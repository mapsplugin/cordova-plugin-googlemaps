//
//  MyPluginLayer.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "MyPluginLayer.h"

@implementation MyPluginLayer

- (id)initWithWebView:(UIView *)webView {
    self.executeQueue = [NSOperationQueue new];

    self = [super initWithFrame:[webView frame]];
    self.webView = webView;
    self.isSuspended = false;
    self.opaque = NO;
    [self.webView removeFromSuperview];
    // prevent webView from bouncing
    if ([self.webView respondsToSelector:@selector(scrollView)]) {
        ((UIScrollView*)[self.webView scrollView]).bounces = NO;
    } else {
        for (id subview in [self.webView subviews]) {
            if ([[subview class] isSubclassOfClass:[UIScrollView class]]) {
                ((UIScrollView*)subview).bounces = NO;
            }
        }
    }

    self.pluginScrollView = [[MyPluginScrollView alloc] initWithFrame:[self.webView frame]];

    self.pluginScrollView.debugView.webView = self.webView;
    self.pluginScrollView.debugView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

    self.pluginScrollView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    UIView *uiview = self.webView;
    uiview.scrollView.delegate = self;
    [self.pluginScrollView setContentSize:self.webView.scrollView.frame.size ];

    [self addSubview:self.pluginScrollView];
    [self addSubview:self.webView];


    self.redrawTimer = [NSTimer scheduledTimerWithTimeInterval:0.1
                                target:self
                                selector:@selector(resizeTask:)
                                userInfo:nil
                                repeats:YES];

    return self;
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
  CGPoint offset = self.webView.scrollView.contentOffset;
  self.pluginScrollView.contentOffset = offset;
}

-(void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate {
  CGPoint offset = self.webView.scrollView.contentOffset;
  self.pluginScrollView.contentOffset = offset;
}
- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
  CGPoint offset = self.webView.scrollView.contentOffset;
  self.pluginScrollView.contentOffset = offset;

  if (self.pluginScrollView.debugView.debuggable) {
      [self.pluginScrollView.debugView setNeedsDisplay];
  }
}
- (void)clearHTMLElements {
    @synchronized(self.pluginScrollView.debugView.HTMLNodes) {
      NSMutableDictionary *domInfo;
      NSString *domId;
      NSArray *keys=[self.pluginScrollView.debugView.HTMLNodes allKeys];
      NSArray *keys2;
      int i, j;
      for (i = 0; i < [keys count]; i++) {
        domId = [keys objectAtIndex:i];
        domInfo = [self.pluginScrollView.debugView.HTMLNodes objectForKey:domId];
        if (domInfo) {
            keys2 = [domInfo allKeys];
            for (j = 0; j < [keys2 count]; j++) {
                [domInfo removeObjectForKey:[keys2 objectAtIndex:j]];
            }
            domInfo = nil;
        }
        [self.pluginScrollView.debugView.HTMLNodes removeObjectForKey:domId];
      }
    }

}

- (void)putHTMLElements:(NSDictionary *)elementsDic {

    [self.executeQueue addOperationWithBlock:^{
        CGRect rect = CGRectMake(0, 0, 0, 0);
        NSMutableDictionary *domInfo, *size;
        NSString *domId;

        @synchronized(self.pluginScrollView.debugView.HTMLNodes) {

          NSArray *keys=[self.pluginScrollView.debugView.HTMLNodes allKeys];
          NSArray *keys2;
          int i, j;
          for (i = 0; i < [keys count]; i++) {
            domId = [keys objectAtIndex:i];
            domInfo = [self.pluginScrollView.debugView.HTMLNodes objectForKey:domId];
            if (domInfo) {
                keys2 = [domInfo allKeys];
                for (j = 0; j < [keys2 count]; j++) {
                    [domInfo removeObjectForKey:[keys2 objectAtIndex:j]];
                }
                domInfo = nil;
            }
            [self.pluginScrollView.debugView.HTMLNodes removeObjectForKey:domId];
          }
          self.pluginScrollView.debugView.HTMLNodes = nil;

          NSMutableDictionary *newBuffer = [[NSMutableDictionary alloc] init];
          for (domId in elementsDic) {
            domInfo = [elementsDic objectForKey:domId];
            size = [domInfo objectForKey:@"size"];
            rect.origin.x = [[size objectForKey:@"left"] doubleValue];
            rect.origin.y = [[size objectForKey:@"top"] doubleValue];
            rect.size.width = [[size objectForKey:@"width"] doubleValue];
            rect.size.height = [[size objectForKey:@"height"] doubleValue];
            [domInfo setValue:NSStringFromCGRect(rect) forKey:@"size"];
            [newBuffer setObject:domInfo forKey:domId];

            domInfo = nil;
            size = nil;
          }

          self.pluginScrollView.debugView.HTMLNodes = newBuffer;
        }
    }];

}
- (void)addMapView:(GoogleMapsViewController *)mapCtrl {

  dispatch_async(dispatch_get_main_queue(), ^{

      // Hold the mapCtrl instance with mapId.
      [self.pluginScrollView.debugView.mapCtrls setObject:mapCtrl forKey:mapCtrl.mapId];

      // Add the mapView under the scroll view.
      [self.pluginScrollView attachView:mapCtrl.view];

      [self updateViewPosition:mapCtrl];
  });
}

- (void)removeMapView:(GoogleMapsViewController *)mapCtrl {
  dispatch_async(dispatch_get_main_queue(), ^{

      // Remove the mapCtrl instance with mapId.
      [self.pluginScrollView.debugView.mapCtrls removeObjectForKey:mapCtrl.mapId];

      // Remove the mapView from the scroll view.
      [mapCtrl.view removeFromSuperview];

      [mapCtrl.view setFrame:CGRectMake(0, -1000, mapCtrl.view.frame.size.width, mapCtrl.view.frame.size.height)];
      [mapCtrl.view setNeedsDisplay];

      if (self.pluginScrollView.debugView.debuggable) {
          [self.pluginScrollView.debugView setNeedsDisplay];
      }
  });

}

- (void)resizeTask:(NSTimer *)timer {
    if (self.isSuspended) {
      // Assumes all touches for the browser
      return;
    }
    if (self.stopFlag) {
        return;
    }
    self.stopFlag = YES;
    NSArray *keys=[self.pluginScrollView.debugView.mapCtrls allKeys];
    NSString *mapId;
    GoogleMapsViewController *mapCtrl;

    for (int i = 0; i < [keys count]; i++) {
        mapId = [keys objectAtIndex:i];
        mapCtrl = [self.pluginScrollView.debugView.mapCtrls objectForKey:mapId];
        [self updateViewPosition:mapCtrl];
    }

    if (self.pluginScrollView.debugView.debuggable == YES) {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^() {

            [self.pluginScrollView.debugView setNeedsDisplay];
        });
    }
    self.stopFlag = NO;
}

- (void)updateViewPosition:(GoogleMapsViewController *)mapCtrl {

    CGFloat zoomScale = self.webView.scrollView.zoomScale;
    [self.pluginScrollView setFrame:self.webView.frame];

    CGPoint offset = self.webView.scrollView.contentOffset;
    offset.x *= zoomScale;
    offset.y *= zoomScale;
    [self.pluginScrollView setContentOffset:offset];

    if (!mapCtrl.mapDivId) {
        return;
    }

    NSDictionary *domInfo = nil;
    @synchronized(self.pluginScrollView.debugView.HTMLNodes) {
      domInfo = [self.pluginScrollView.debugView.HTMLNodes objectForKey:mapCtrl.mapDivId];
      if (domInfo == nil) {
          return;
      }

    }
    NSString *rectStr = [domInfo objectForKey:@"size"];
    if (rectStr == nil || [rectStr  isEqual: @"null"]) {
      return;
    }

    __block CGRect rect = CGRectFromString(rectStr);
    rect.origin.x *= zoomScale;
    rect.origin.y *= zoomScale;
    rect.size.width *= zoomScale;
    rect.size.height *= zoomScale;
    rect.origin.x += offset.x;
    rect.origin.y += offset.y;

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
            [mapCtrl.view removeFromSuperview];
        }
    }

    if (mapCtrl.isRenderedAtOnce == YES &&
        rect.origin.x == mapCtrl.view.frame.origin.x &&
        rect.origin.y == mapCtrl.view.frame.origin.y &&
        rect.size.width == mapCtrl.view.frame.size.width &&
        rect.size.height == mapCtrl.view.frame.size.height) {
        return;
    }

    dispatch_async(dispatch_get_main_queue(), ^{
      if (mapCtrl.isRenderedAtOnce) {

        [UIView animateWithDuration:0.075f animations:^{
          [mapCtrl.view setFrame:rect];
          rect.origin.x = 0;
          rect.origin.y = 0;
          [mapCtrl.map setFrame:rect];
        }];
      } else {
        [mapCtrl.view setFrame:rect];
        rect.origin.x = 0;
        rect.origin.y = 0;
        [mapCtrl.map setFrame:rect];
        mapCtrl.isRenderedAtOnce = YES;
      }


    });

}
- (void)execJS: (NSString *)jsString {
    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
        [self.webView performSelector:@selector(stringByEvaluatingJavaScriptFromString:) withObject:jsString];
    } else if ([self.webView respondsToSelector:@selector(evaluateJavaScript:completionHandler:)]) {
        [self.webView performSelector:@selector(evaluateJavaScript:completionHandler:) withObject:jsString withObject:nil];
    }
}
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    // Check other views of other plugins before this plugin
    // e.g. PhoneGap-Plugin-ListPicker, etc
    for (UIView *subview in [self.webView.superview subviews]) {
      // we only want to check against other views
      if (subview == self.pluginScrollView || subview == self.webView) {
        continue;
      }

      UIView *hit = [subview hitTest:point withEvent:event];

      if (hit) {
        return hit;
      }
    }

    if (self.pluginScrollView.debugView.mapCtrls == nil || self.pluginScrollView.debugView.mapCtrls.count == 0) {
      // Assumes all touches for the browser
      return [self.webView hitTest:point withEvent:event];
    }

    float offsetX = self.webView.scrollView.contentOffset.x;
    float offsetY = self.webView.scrollView.contentOffset.y;
    float offsetX2 = self.webView.scrollView.contentOffset.x;
    float offsetY2 = self.webView.scrollView.contentOffset.y;

    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;


    CGRect rect, htmlElementRect= CGRectMake(0, 0, 0, 0);
    NSEnumerator *mapIDs = [self.pluginScrollView.debugView.mapCtrls keyEnumerator];
    GoogleMapsViewController *mapCtrl;
    id mapId;
    BOOL isMapAction = NO;
    NSString *elementRect;

    CGFloat zoomScale = self.webView.scrollView.zoomScale;
    offsetY *= zoomScale;
    offsetX *= zoomScale;
    webviewWidth *= zoomScale;
    webviewHeight *= zoomScale;

    NSDictionary *domInfo, *mapDivInfo;
    double domDepth, mapDivDepth;

    CGPoint clickPointAsHtml = CGPointMake(point.x * zoomScale, point.y * zoomScale);
@synchronized(self.pluginScrollView.debugView.HTMLNodes) {
    while(mapId = [mapIDs nextObject]) {
        mapCtrl = [self.pluginScrollView.debugView.mapCtrls objectForKey:mapId];
        if (!mapCtrl.mapDivId) {
          continue;
        }
        domInfo =[self.pluginScrollView.debugView.HTMLNodes objectForKey:mapCtrl.mapDivId];

        rect = CGRectFromString([domInfo objectForKey:@"size"]);
        rect.origin.x += offsetX2;
        rect.origin.y += offsetY2;
        rect.origin.x *= zoomScale;
        rect.origin.y *= zoomScale;
        rect.size.width *= zoomScale;
        rect.size.height *= zoomScale;

        // Is the map clickable?
        if (mapCtrl.clickable == NO) {
            //NSLog(@"--> map (%@) is not clickable.", mapCtrl.mapId);
            continue;
        }

        // Is the map displayed?
        if (rect.origin.y + rect.size.height < offsetY ||
            rect.origin.x + rect.size.width < offsetX ||
            rect.origin.y > offsetY + webviewHeight ||
            rect.origin.x > offsetX + webviewWidth ||
            mapCtrl.view.hidden == YES) {
            //NSLog(@"--> map (%@) is not displayed.", mapCtrl.mapId);
            continue;
        }

        // Is the clicked point is in the map rectangle?
        if ((point.x + offsetX2) >= rect.origin.x && (point.x + offsetX2) <= (rect.origin.x + rect.size.width) &&
            (point.y + offsetY2) >= rect.origin.y && (point.y + offsetY2) <= (rect.origin.y + rect.size.height)) {
            isMapAction = YES;
        } else {
            continue;
        }

        // Is the clicked point is on the html elements in the map?
        mapDivInfo = [self.pluginScrollView.debugView.HTMLNodes objectForKey:mapCtrl.mapDivId];

        mapDivDepth = [[mapDivInfo objectForKey:@"depth"] doubleValue];
        for (NSString *domId in self.pluginScrollView.debugView.HTMLNodes) {
            if ([mapCtrl.mapDivId isEqualToString:domId]) {
                continue;
            }

            domInfo = [self.pluginScrollView.debugView.HTMLNodes objectForKey:domId];
            domDepth = [[domInfo objectForKey:@"depth"] doubleValue];
            if (domDepth < mapDivDepth) {
                continue;
            }
            elementRect = [domInfo objectForKey:@"size"];
            htmlElementRect = CGRectFromString(elementRect);
            if (htmlElementRect.size.width == 0 || htmlElementRect.size.height == 0) {
                continue;
            }

            //NSLog(@"----> domId = %@, %f, %f - %f, %f (%@ : %@)",
            //                domId,
            //                htmlElementRect.origin.x, htmlElementRect.origin.y,
            //                htmlElementRect.size.width, htmlElementRect.size.height,
            //                [domInfo objectForKey:@"tagName"],
            //              [domInfo objectForKey:@"position"]);
            if (clickPointAsHtml.x >= htmlElementRect.origin.x && clickPointAsHtml.x <= (htmlElementRect.origin.x + htmlElementRect.size.width) &&
                clickPointAsHtml.y >= htmlElementRect.origin.y && clickPointAsHtml.y <= (htmlElementRect.origin.y + htmlElementRect.size.height)) {
                isMapAction = NO;
                //NSLog(@"--> hit (%@) : (domId: %@, depth: %d) ", mapCtrl.mapId, domId, domDepth);
                break;
            }
        }

        if (isMapAction == YES) {

          // If user click on the map, return the mapCtrl.view.
          offsetX = (mapCtrl.view.frame.origin.x * zoomScale) - offsetX;
          offsetY = (mapCtrl.view.frame.origin.y * zoomScale) - offsetY;
          CGPoint point2 = CGPointMake(point.x * zoomScale, point.y * zoomScale);
          point2.x -= offsetX;
          point2.y -= offsetY;

          UIView *hitView =[mapCtrl.view hitTest:point2 withEvent:event];
          //NSLog(@"--> (hit test) point = %f, %f / hit = %@", clickPointAsHtml.x, clickPointAsHtml.y,  hitView.class);

          return hitView;
        }
    }
}

    UIView *hitView =[self.webView hitTest:point withEvent:event];
    //NSLog(@"--> (hit test) hit = %@", hitView.class);
    return hitView;

}

@end
