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
    

    float offsetX = self.webView.scrollView.contentOffset.x;
    float offsetY = self.webView.scrollView.contentOffset.y;
    
    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;
    
    
    CGRect rect;
    NSEnumerator *mapIDs = [self.drawRects keyEnumerator];
    GoogleMapsViewController *mapCtrl;
    id mapId;
    //NSLog(@"--> point = %f, %f", point.x, point.y);
    while(mapId = [mapIDs nextObject]) {
        rect = CGRectFromString([self.drawRects objectForKey:mapId]);
        mapCtrl = [self.mapCtrls objectForKey:mapId];
      
        // Is the map is displayed?
        if (rect.origin.y + rect.size.height < offsetY ||
            rect.origin.x + rect.size.width < offsetX ||
            rect.origin.y > offsetY + webviewHeight ||
            rect.origin.x > offsetX + webviewWidth ||
            mapCtrl.view.hidden == YES) {
          
            // Detach from the parent view
            [mapCtrl.view removeFromSuperview];
          
        } else {
            // Attach the map view to the parent.
            [mapCtrl.map layoutIfNeeded];
            [mapCtrl.map updateFocusIfNeeded];
            [mapCtrl.map updateConstraintsIfNeeded];
            [self.pluginScrollView addSubview:mapCtrl.view];
        }
    }

}


- (void)putHTMLElement:(NSString *)mapId domId:(NSString *)domId size:(NSDictionary *)size {
    NSMutableDictionary *domDic = [self.HTMLNodes objectForKey:mapId];
    if (domDic == nil) {
        domDic = [[NSMutableDictionary alloc] init];
    }
    
    NSString *rectString = [NSString stringWithFormat:@"{{%@, %@}, {%@, %@}}",
        [size objectForKey:@"left"],
        [size objectForKey:@"top"],
        [size objectForKey:@"width"],
        [size objectForKey:@"height"],
        nil
    ];
    [domDic setObject:rectString forKey:domId];
    [self.HTMLNodes setObject:domDic forKey:mapId];
    
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
  
    CGPoint offset = self.pluginScrollView.contentOffset;
    offset.x = self.webView.scrollView.contentOffset.x;
    offset.y = self.webView.scrollView.contentOffset.y;
    [self.pluginScrollView setContentOffset:offset];
  
    GoogleMapsViewController *mapCtrl = [self.mapCtrls objectForKey:mapId];
  
    CGRect rect = CGRectFromString([self.drawRects objectForKey:mapId]);
  
    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;
  
    // Is the map is displayed?
    if (rect.origin.y + rect.size.height < offset.y ||
        rect.origin.x + rect.size.width < offset.x ||
        rect.origin.y > offset.y + webviewHeight ||
        rect.origin.x > offset.x + webviewWidth ||
        mapCtrl.view.hidden == YES) {
      
        // Detach from the parent view
        [mapCtrl.view removeFromSuperview];
      
    } else {
        // Attach the map view to the parent.
        // invite drawRect();
        [mapCtrl.map layoutIfNeeded];
        [mapCtrl.map updateFocusIfNeeded];
        [mapCtrl.map updateConstraintsIfNeeded];
        [self.pluginScrollView addSubview:mapCtrl.view];
    }

    [mapCtrl.view setFrame:rect];
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {

    float offsetX = self.webView.scrollView.contentOffset.x;
    float offsetY = self.webView.scrollView.contentOffset.y;
    
    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;
    
    
    CGRect rect, htmlElementRect;
    NSEnumerator *mapIDs = [self.drawRects keyEnumerator];
    GoogleMapsViewController *mapCtrl;
    id mapId;
    BOOL isMapAction = NO;
    NSDictionary *elements;
    NSString *elemSize;
    //NSLog(@"--> point = %f, %f", point.x, point.y);
    while(mapId = [mapIDs nextObject]) {
        rect = CGRectFromString([self.drawRects objectForKey:mapId]);
        mapCtrl = [self.mapCtrls objectForKey:mapId];
        
        // Is the map clickable?
        if (mapCtrl.clickable == NO) {
          continue;
        }

        // Is the map is displayed?
        if (rect.origin.y + rect.size.height < offsetY ||
            rect.origin.x + rect.size.width < offsetX ||
            rect.origin.y > offsetY + webviewHeight ||
            rect.origin.x > offsetX + webviewWidth ||
            mapCtrl.view.hidden == YES) {
            continue;
        }
      
        // Is the clicked point is in the map rectangle?
        if ((point.x + offsetX) >= rect.origin.x && (point.x + offsetX) <= (rect.origin.x + rect.size.width) &&
            (point.y + offsetY) >= rect.origin.y && (point.y + offsetY) <= (rect.origin.y + rect.size.height)) {
            isMapAction = YES;
        } else {
            //NSLog(@"--> point = %f, %f are not in the map.", point.x, point.y);
            continue;
        }

        // Is the clicked point is on the html elements in the map?
        elements = [self.HTMLNodes objectForKey:mapId];
        for (NSString *domId in elements) {
            elemSize = [elements objectForKey:domId];
            htmlElementRect = CGRectFromString(elemSize);
            htmlElementRect.origin.x -= offsetX;
            htmlElementRect.origin.y -= offsetY;
          
            if (point.x >= htmlElementRect.origin.x && point.x <= (htmlElementRect.origin.x + htmlElementRect.size.width) &&
                point.y >= htmlElementRect.origin.y && point.y <= (htmlElementRect.origin.y + htmlElementRect.size.height)) {
                isMapAction = NO;
                break;
            }
          
        }

        if (isMapAction == NO) {
            continue;
        }
        
        // If user click on the map, return the mapCtrl.view.
        offsetX = mapCtrl.view.frame.origin.x - offsetX;
        offsetY = mapCtrl.view.frame.origin.y - offsetY;
        point.x -= offsetX;
        point.y -= offsetY;

        UIView *hitView =[mapCtrl.view hitTest:point withEvent:event];
        //NSLog(@"--> (hit test) point = %f, %f / hit = %@", point.x, point.y,  hitView.class);
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
