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
    self.webView = webView;
    self.opaque = NO;
    [self.webView removeFromSuperview];

    self.pluginScrollView = [[MyPluginScrollView alloc] initWithFrame:self.webView.frame];
  
    self.pluginScrollView.debugView.webView = self.webView;
    self.pluginScrollView.debugView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  
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
  
    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;
    
    CGFloat zoomScale = self.webView.scrollView.zoomScale;
    
    CGRect rect;
    NSEnumerator *mapIDs = [self.drawRects keyEnumerator];
    GoogleMapsViewController *mapCtrl;
    id mapId;
    //NSLog(@"--> point = %f, %f", point.x, point.y);
    while(mapId = [mapIDs nextObject]) {
        rect = CGRectFromString([self.drawRects objectForKey:mapId]);
        rect.origin.x *= zoomScale;
        rect.origin.y *= zoomScale;
        rect.size.width *= zoomScale;
        rect.size.height *= zoomScale;
        mapCtrl = [self.mapCtrls objectForKey:mapId];
      
          
        // Is the map is displayed?
        if (rect.origin.y + rect.size.height >= offset.y &&
            rect.origin.x + rect.size.width >= offset.x &&
            rect.origin.y < offset.y + webviewHeight &&
            rect.origin.x < offset.x + webviewWidth &&
            mapCtrl.view.hidden == NO) {
          
            // Attach the map view to the parent.
            if (mapCtrl.isRenderedAtOnce == YES ||
                (mapCtrl.map.mapType != kGMSTypeSatellite &&
                mapCtrl.map.mapType != kGMSTypeHybrid)) {
                [self.pluginScrollView attachView:mapCtrl.view];
            }
          
        } else {
            // Detach from the parent view
            if (mapCtrl.isRenderedAtOnce == YES ||
                (mapCtrl.map.mapType != kGMSTypeSatellite &&
                mapCtrl.map.mapType != kGMSTypeHybrid)) {
                
                //[self.pluginScrollView addSubview:mapCtrl.view];
                [mapCtrl.view removeFromSuperview];
            }
        }
      
    }
    if (self.pluginScrollView.debugView.debuggable) {
        [self.pluginScrollView.debugView setNeedsDisplay];
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
  NSString *rectStr = NSStringFromCGRect(mapCtrl.view.frame);
  [self.drawRects setObject:rectStr forKey:mapId];
  [self.pluginScrollView.debugView.drawRects setObject:rectStr forKey:mapId];
  
  // Add the mapView under this view.
  //[self.pluginScrollView addSubview: mapCtrl.view];
  [self.pluginScrollView attachView:mapCtrl.view];
  
}


- (void)updateViewPosition:(NSString *)mapId {
    CGFloat zoomScale = self.webView.scrollView.zoomScale;
  
    CGPoint offset = self.pluginScrollView.contentOffset;
    offset.x = self.webView.scrollView.contentOffset.x;
    offset.y = self.webView.scrollView.contentOffset.y;
    [self.pluginScrollView setContentOffset:offset];
  
    GoogleMapsViewController *mapCtrl = [self.mapCtrls objectForKey:mapId];
  
    NSString *rectStr = [self.drawRects objectForKey:mapId];
    [self.pluginScrollView.debugView.drawRects setObject:rectStr forKey:mapId];
  
    CGRect rect = CGRectFromString(rectStr);
    rect.origin.x *= zoomScale;
    rect.origin.y *= zoomScale;
    rect.size.width *= zoomScale;
    rect.size.height *= zoomScale;
  
    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;
  
    // Is the map is displayed?
    if (rect.origin.y + rect.size.height >= offset.y &&
        rect.origin.x + rect.size.width >= offset.x &&
        rect.origin.y < offset.y + webviewHeight &&
        rect.origin.x < offset.x + webviewWidth &&
        mapCtrl.view.hidden == NO) {
      
        // Attach the map view to the parent.
        if (mapCtrl.isRenderedAtOnce == YES ||
            (mapCtrl.map.mapType != kGMSTypeSatellite &&
            mapCtrl.map.mapType != kGMSTypeHybrid)) {
            
            [self.pluginScrollView attachView:mapCtrl.view];
        }
      
    } else {
        // Detach from the parent view
        if (mapCtrl.isRenderedAtOnce == YES ||
            (mapCtrl.map.mapType != kGMSTypeSatellite &&
            mapCtrl.map.mapType != kGMSTypeHybrid)) {
            
            //[self.pluginScrollView addSubview:mapCtrl.view];
            [mapCtrl.view removeFromSuperview];
        }
    }

    mapCtrl.isRenderedAtOnce = YES;

    [mapCtrl.view setFrame:rect];
    
    if (self.pluginScrollView.debugView.debuggable) {
        [self.pluginScrollView.debugView setNeedsDisplay];
    }
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {

    float offsetX = self.webView.scrollView.contentOffset.x;
    float offsetY = self.webView.scrollView.contentOffset.y;
    float offsetX2 = self.webView.scrollView.contentOffset.x;
    float offsetY2 = self.webView.scrollView.contentOffset.y;
  
    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;
  
    
    CGRect rect, rect2, htmlElementRect;
    NSEnumerator *mapIDs = [self.drawRects keyEnumerator];
    GoogleMapsViewController *mapCtrl;
    id mapId;
    BOOL isMapAction = NO;
    NSDictionary *elements;
    NSString *elemSize;
  
    CGFloat zoomScale = self.webView.scrollView.zoomScale;
    offsetY *= zoomScale;
    offsetX *= zoomScale;
    webviewWidth *= zoomScale;
    webviewHeight *= zoomScale;
  
    while(mapId = [mapIDs nextObject]) {
        rect2 = CGRectFromString([self.drawRects objectForKey:mapId]);
        rect = CGRectFromString([self.drawRects objectForKey:mapId]);
        rect.origin.x *= zoomScale;
        rect.origin.y *= zoomScale;
        rect.size.width *= zoomScale;
        rect.size.height *= zoomScale;
        mapCtrl = [self.mapCtrls objectForKey:mapId];
        //NSLog(@"--> rect = %f, %f - %f, %f", rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
        
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
        if ((point.x + offsetX2) >= rect.origin.x && (point.x + offsetX2) <= (rect.origin.x + rect.size.width) &&
            (point.y + offsetY2) >= rect.origin.y && (point.y + offsetY2) <= (rect.origin.y + rect.size.height)) {
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
            htmlElementRect.origin.x *= zoomScale;
            htmlElementRect.origin.y *= zoomScale;
            htmlElementRect.size.width *= zoomScale;
            htmlElementRect.size.height *= zoomScale;
            htmlElementRect.origin.x -= offsetX2;
            htmlElementRect.origin.y -= offsetY2;
          
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
        offsetX = (mapCtrl.view.frame.origin.x * zoomScale) - offsetX;
        offsetY = (mapCtrl.view.frame.origin.y * zoomScale) - offsetY;
        CGPoint point2 = CGPointMake(point.x * zoomScale, point.y * zoomScale);
        point2.x -= offsetX;
        point2.y -= offsetY;

        UIView *hitView =[mapCtrl.view hitTest:point2 withEvent:event];
        //NSLog(@"--> (hit test) point = %f, %f / hit = %@", point2.x, point2.y,  hitView.class);
        NSString *hitClass = [NSString stringWithFormat:@"%@", [hitView class]];
        if ([PluginUtil isIOS7_OR_OVER] &&
            [hitClass isEqualToString:@"UIButton"] &&
            mapCtrl.map.isMyLocationEnabled &&
            (point.x  + offsetX2) >= (rect.origin.x + rect.size.width - 50) &&
            (point.y + offsetY2) >= (rect.origin.y + rect.size.height - 50)) {

            BOOL retValue = [mapCtrl didTapMyLocationButtonForMapView:mapCtrl.map];
            if (retValue == YES) {
                return nil;
            }
        }
        return hitView;
    }
    return [super hitTest:point withEvent:event];
}


@end
