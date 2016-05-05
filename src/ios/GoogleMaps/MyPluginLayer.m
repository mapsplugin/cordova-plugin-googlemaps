//
//  DummyView.m
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import "MyPluginLayer.h"

@implementation MyPluginLayer

- (id)initWithWebView:(UIWebView *)webView {
    self = [super initWithFrame:webView.frame];
    self.drawRects = [[NSMutableDictionary alloc] init];
    self.HTMLNodes = [[NSMutableDictionary alloc] init];
    self.mapCtrls = [[NSMutableDictionary alloc] init];
    self.clickable = YES;
    self.debuggable = NO;
    self.webView = webView;
    [self.webView removeFromSuperview];

    self.pluginScrollView = [[MyPluginScrollView alloc] initWithFrame:self.webView.frame];
    self.pluginScrollView.debugView.webView = self.webView;
    self.pluginScrollView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    self.webView.scrollView.delegate = self;
    [self.pluginScrollView setContentSize:CGSizeMake(320, 960) ];
    
    [self addSubview:self.pluginScrollView];
    [self addSubview:self.webView];

  return self;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    CGPoint offset = self.pluginScrollView.contentOffset;
    offset.x = self.webView.scrollView.contentOffset.x;
    offset.y = self.webView.scrollView.contentOffset.y;
    [self.pluginScrollView setContentOffset:offset];
    [self setNeedsDisplay];
    [self.pluginScrollView.debugView setNeedsDisplay];
}


- (void)putHTMLElement:(NSString *)mapId domId:(NSString *)domId size:(NSDictionary *)size {
    NSMutableDictionary *domDic = [self.HTMLNodes objectForKey:mapId];
    if (!domDic) {
        domDic = [[NSMutableDictionary alloc] init];
        [self.HTMLNodes setObject:domDic forKey:mapId];
    }
    
    [domDic setObject:size forKey:domId];
    
    // invite drawRect();
    [self setNeedsDisplay];
}
- (void)removeHTMLElement:(NSString *)mapId domId:(NSString *)domId {
    NSMutableDictionary *domDic = [self.HTMLNodes objectForKey:mapId];
    if (!domDic) {
        return;
    }
    
    [domDic removeObjectForKey:domId];
    
    // invite drawRect();
    [self setNeedsDisplay];
}
- (void)clearHTMLElement:(NSString *)mapId {
    NSMutableDictionary *domDic = [self.HTMLNodes objectForKey:mapId];
    if (!domDic) {
        return;
    }
    [domDic removeAllObjects];
    [self.HTMLNodes removeObjectForKey:mapId];
    domDic = nil;
    
    // invite drawRect();
    [self setNeedsDisplay];
}

- (void)addMapView:(NSString *)mapId mapCtrl:(GoogleMapsViewController *)mapCtrl {
  
  
  // Hold mapCtrl instance with mapId.
  [self.mapCtrls setObject:mapCtrl forKey:mapId];
  
  // Hold the size and position information of the mapView.
  [self.drawRects setObject:NSStringFromCGRect(mapCtrl.view.frame) forKey:mapId];
  
  // Add the mapView under this view.
  [self.pluginScrollView addSubview: mapCtrl.view];
  
}


- (void)updateViewPosition:(NSString *)mapId {
  
    float offsetX = self.webView.scrollView.contentOffset.x;// + self.mapCtrl.view.frame.origin.x;
    float offsetY = self.webView.scrollView.contentOffset.y;// + self.mapCtrl.view.frame.origin.y;
  
    CGRect embedRect = CGRectFromString([self.drawRects objectForKey:mapId]);
  
    embedRect.origin.x -= offsetX;
    embedRect.origin.y -= offsetY;
  //NSLog(@"mapId = %@, embedRect = %@", mapId, embedRect);

    GoogleMapsViewController *mapCtrl = [self.mapCtrls objectForKey:mapId];
    [mapCtrl.view setFrame:embedRect];
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {

/*
  if (self.clickable == NO ||
      self.mapCtrl.map == nil ||
      self.mapCtrl.map.hidden == YES) {
    return [super hitTest:point withEvent:event];
  }
  */

    float offsetX = self.webView.scrollView.contentOffset.x;
    float offsetY = self.webView.scrollView.contentOffset.y;
    CGRect rect;
    NSEnumerator *mapIDs = [self.drawRects keyEnumerator];
    GoogleMapsViewController *mapCtrl;
    id mapId;
    BOOL isMapAction = NO;
    while(mapId = [mapIDs nextObject]) {
        rect = CGRectFromString([self.drawRects objectForKey:mapId]);
        if (point.x >= rect.origin.x && point.x <= (rect.origin.x + rect.size.width) &&
            point.y >= rect.origin.y && point.y <= (rect.origin.y + rect.size.height)) {
            isMapAction = YES;
        } else {
            continue;
        }

        /*
        NSDictionary *elemSize;
        for (NSString *domId in self.HTMLNodes) {
          elemSize = [self.HTMLNodes objectForKey:domId];
          left = [[elemSize objectForKey:@"left"] floatValue] - offsetX;
          top = [[elemSize objectForKey:@"top"] floatValue] - offsetY;
          width = [[elemSize objectForKey:@"width"] floatValue];
          height = [[elemSize objectForKey:@"height"] floatValue];
          
          if (point.x >= left && point.x <= (left + width) &&
              point.y >= top && point.y <= (top + height)) {
            isMapAction = NO;
            break;
          }
          
        }
        */

        if (isMapAction == NO) {
            continue;
        }
        // The issue #217 is fixed by @YazeedFares. Thank you!
        mapCtrl = [self.mapCtrls objectForKey:mapId];
        offsetX = mapCtrl.view.frame.origin.x - offsetX;
        offsetY = mapCtrl.view.frame.origin.y - offsetY;
        point.x -= offsetX;
        point.y -= offsetY;

        UIView *hitView =[mapCtrl.view hitTest:point withEvent:event];
        NSString *hitClass = [NSString stringWithFormat:@"%@", [hitView class]];
        if ([PluginUtil isIOS7_OR_OVER] &&
            [hitClass isEqualToString:@"UIButton"] &&
            mapCtrl.map.isMyLocationEnabled &&
            (point.x  + offsetX) >= (rect.origin.x + rect.size.width - 50) &&
            (point.y + offsetY) >= (rect.origin.y + rect.size.height - 50)) {

            BOOL retValue = [mapCtrl didTapMyLocationButtonForMapView:mapCtrl.map];
            if (retValue == YES) {
                return nil;
            }
        }
        return hitView;
    }
    return [super hitTest:point withEvent:event];
  
  /*
  float offsetX = self.webView.scrollView.contentOffset.x;// + self.mapCtrl.view.frame.origin.x;
  float offsetY = self.webView.scrollView.contentOffset.y;// + self.mapCtrl.view.frame.origin.y;
  float left = [[self.embedRect objectForKey:@"left"] floatValue] - offsetX;
  float top = [[self.embedRect objectForKey:@"top"] floatValue] - offsetY;
  float width = [[self.embedRect objectForKey:@"width"] floatValue];
  float height = [[self.embedRect objectForKey:@"height"] floatValue];
  
  BOOL isMapAction = NO;
  if (point.x >= left && point.x <= (left + width) &&
      point.y >= top && point.y <= (top + height)) {
    isMapAction = YES;
  } else {
    isMapAction = NO;
  }
  if (isMapAction == YES) {
    NSDictionary *elemSize;
    for (NSString *domId in self.HTMLNodes) {
      elemSize = [self.HTMLNodes objectForKey:domId];
      left = [[elemSize objectForKey:@"left"] floatValue] - offsetX;
      top = [[elemSize objectForKey:@"top"] floatValue] - offsetY;
      width = [[elemSize objectForKey:@"width"] floatValue];
      height = [[elemSize objectForKey:@"height"] floatValue];
      
      if (point.x >= left && point.x <= (left + width) &&
          point.y >= top && point.y <= (top + height)) {
        isMapAction = NO;
        break;
      }
      
    }
  }
  if (isMapAction == YES) {
    // The issue #217 is fixed by @YazeedFares. Thank you!
    offsetX = self.mapCtrl.view.frame.origin.x - offsetX;
    offsetY = self.mapCtrl.view.frame.origin.y - offsetY;
    point.x -= offsetX;
    point.y -= offsetY;
    
    UIView *hitView =[self.mapCtrl.view hitTest:point withEvent:event];
    NSString *hitClass = [NSString stringWithFormat:@"%@", [hitView class]];
    if ([PluginUtil isIOS7_OR_OVER] &&
        [hitClass isEqualToString:@"UIButton"] &&
        self.mapCtrl.map.isMyLocationEnabled &&
        (point.x  + offsetX) >= (left + width - 50) &&
         (point.y + offsetY) >= (top + height - 50)) {
      
      BOOL retValue = [self.mapCtrl didTapMyLocationButtonForMapView:self.mapCtrl.map];
      if (retValue == YES) {
        return nil;
      }
    }
    return hitView;
  }
  
  return [super hitTest:point withEvent:event];
  */
}

- (void)drawRect:(CGRect)rect
{
/*
  if (self.debuggable == NO) {
    return;
  }
  float offsetX = self.webView.scrollView.contentOffset.x;// + self.mapCtrl.view.frame.origin.x;
  float offsetY = self.webView.scrollView.contentOffset.y;// + self.mapCtrl.view.frame.origin.y;
  
  float left = [[self.embedRect objectForKey:@"left"] floatValue] - offsetX;
  float top = [[self.embedRect objectForKey:@"top"] floatValue] - offsetY;
  float width = [[self.embedRect objectForKey:@"width"] floatValue];
  float height = [[self.embedRect objectForKey:@"height"] floatValue];
  
  //-----------------------
  // Draw the HTML region
  //-----------------------
  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextSetRGBFillColor(context, 0, 1.0, 0, 0.4);
  
  CGRect rectangle = CGRectMake(0, 0, rect.size.width, top);
  CGContextFillRect(context, rectangle);
  
  rectangle.origin.x = 0;
  rectangle.origin.y = top;
  rectangle.size.width = left;
  rectangle.size.height = height;
  CGContextFillRect(context, rectangle);
  
  rectangle.origin.x = left + width;
  rectangle.origin.y = top;
  rectangle.size.width = self.webView.scrollView.contentSize.width;
  rectangle.size.height = height;
  CGContextFillRect(context, rectangle);
  
  rectangle.origin.x = 0;
  rectangle.origin.y = top + height;
  rectangle.size.width = self.webView.scrollView.contentSize.width;
  rectangle.size.height = self.webView.scrollView.contentSize.height;
  CGContextFillRect(context, rectangle);
*/
}


@end
