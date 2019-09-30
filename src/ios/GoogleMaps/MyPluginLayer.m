//
//  MyPluginLayer.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "MyPluginLayer.h"

@implementation OverflowCSS : NSObject
  BOOL cropX;
  BOOL cropY;
  CGRect rect;
@end


@implementation CGDOMRect : NSObject
  CGFloat left;
  CGFloat right;
  CGFloat top;
  CGFloat bottom;
@end

@implementation MyPluginLayer

- (id)initWithWebView:(UIView *)webView {
    self.executeQueue = [NSOperationQueue new];
    self._lockObject = [[NSObject alloc] init];
    self.CACHE_FIND_DOM = [NSMutableDictionary dictionary];

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

    self.pluginScrollView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

    self.pluginScrollView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    UIView *uiview = self.webView;
    uiview.scrollView.delegate = self;
    [self.pluginScrollView setContentSize:self.webView.scrollView.frame.size ];

    [self addSubview:self.pluginScrollView];

    [self addSubview:self.webView];


//    dispatch_queue_t q_background = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0);
//
//    dispatch_async(q_background, ^{
//      [self startRedrawTimer];
//    });

    return self;
}

- (void)startRedrawTimer {
  @synchronized(self._lockObject) {
    if (!self.redrawTimer) {

      self.redrawTimer = [NSTimer scheduledTimerWithTimeInterval:0.1
                                  target:self
                                  selector:@selector(resizeTask:)
                                  userInfo:nil
                                  repeats:YES];
    }
  }
}
- (void)stopRedrawTimer {
  @synchronized(self._lockObject) {
    if (self.redrawTimer) {
      [self.redrawTimer invalidate];
      self.redrawTimer = nil;
    }
  }
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
}
- (void)clearHTMLElements {
    @synchronized(self.pluginScrollView.HTMLNodes) {
      NSMutableDictionary *domInfo;
      NSString *domId;
      NSArray *keys=[self.pluginScrollView.HTMLNodes allKeys];
      NSArray *keys2;
      int i, j;
      for (i = 0; i < [keys count]; i++) {
        domId = [keys objectAtIndex:i];
        domInfo = [self.pluginScrollView.HTMLNodes objectForKey:domId];
        if (domInfo) {
            keys2 = [domInfo allKeys];
            for (j = 0; j < [keys2 count]; j++) {
                [domInfo removeObjectForKey:[keys2 objectAtIndex:j]];
            }
            domInfo = nil;
        }
        [self.pluginScrollView.HTMLNodes removeObjectForKey:domId];
      }
    }

}

- (void)putHTMLElements:(NSDictionary *)elementsDic {

    [self.executeQueue addOperationWithBlock:^{
        CGRect rect = CGRectMake(0, 0, 0, 0);
        NSMutableDictionary *domInfo, *size;
        NSString *domId;

        @synchronized(self._lockObject) {

          if (self.pluginScrollView.HTMLNodes != nil) {
            NSArray *keys=[self.pluginScrollView.HTMLNodes allKeys];
            NSArray *keys2;
            int i, j;
            for (i = 0; i < [keys count]; i++) {
              domId = [keys objectAtIndex:i];
              domInfo = [self.pluginScrollView.HTMLNodes objectForKey:domId];
              if (domInfo) {
                  keys2 = [domInfo allKeys];
                  for (j = 0; j < [keys2 count]; j++) {
                      [domInfo removeObjectForKey:[keys2 objectAtIndex:j]];
                  }
                  domInfo = nil;
              }
              [self.pluginScrollView.HTMLNodes removeObjectForKey:domId];
            }
          }
          self.pluginScrollView.HTMLNodes = nil;

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

          self.pluginScrollView.HTMLNodes = newBuffer;
        }
    }];

}
- (void)addPluginOverlay:(PluginViewController *)pluginViewCtrl {

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      // Hold the mapCtrl instance with mapId.
      [self.pluginScrollView.mapCtrls setObject:pluginViewCtrl forKey:pluginViewCtrl.overlayId];

      //When showing this plugin multiple times, a UIView from other plugin might be overlayed on
      //top of this plugin so we must switch the view with the view that is right before the webview
      //and update the background color for the webview to prevent issues with non transparent webview
      [self exchangeSubviewAtIndex:[self.subviews indexOfObject:self.webView]-1 withSubviewAtIndex:[self.subviews indexOfObject:self.pluginScrollView]];
      self.webView.backgroundColor = [UIColor clearColor];

      // Add the mapView under the scroll view.
      [pluginViewCtrl.view setTag:pluginViewCtrl.viewDepth];
      [self.pluginScrollView attachView:pluginViewCtrl.view depth:pluginViewCtrl.viewDepth];
      pluginViewCtrl.attached = YES;

      [self updateViewPosition:pluginViewCtrl];
  }];
}

- (void)removePluginOverlay:(PluginViewController *)pluginViewCtrl {
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      pluginViewCtrl.attached = NO;

      // Remove the mapCtrl instance with mapId.
      [self.pluginScrollView.mapCtrls removeObjectForKey:pluginViewCtrl.overlayId];

      // Remove the mapView from the scroll view.
      [pluginViewCtrl willMoveToParentViewController:nil];
      [pluginViewCtrl.view removeFromSuperview];
      [pluginViewCtrl removeFromParentViewController];
      [self.pluginScrollView detachView:pluginViewCtrl.view];

      //[pluginViewCtrl.view setFrame:CGRectMake(0, -pluginViewCtrl.view.frame.size.height, pluginViewCtrl.view.frame.size.width, pluginViewCtrl.view.frame.size.height)];
      //[pluginViewCtrl.view setNeedsDisplay];
  }];

}

- (void)resizeTask:(NSTimer *)timer {
  dispatch_async(dispatch_get_main_queue(), ^{
    NSArray *keys=[self.pluginScrollView.mapCtrls allKeys];
    NSString *mapId;
    PluginMapViewController *mapCtrl;


    for (int i = 0; i < [keys count]; i++) {
        mapId = [keys objectAtIndex:i];
        mapCtrl = [self.pluginScrollView.mapCtrls objectForKey:mapId];
        [self updateViewPosition:mapCtrl];
    }
  });
}

- (void)updateViewPosition:(PluginViewController *)pluginViewCtrl {
    CGFloat zoomScale = self.webView.scrollView.zoomScale;
    [self.pluginScrollView setFrame:self.webView.frame];

    CGPoint offset = self.webView.scrollView.contentOffset;
    offset.x *= zoomScale;
    offset.y *= zoomScale;
    [self.pluginScrollView setContentOffset:offset];

    if (!pluginViewCtrl.divId) {
      return;
    }

    NSDictionary *domInfo = nil;
    @synchronized(self.pluginScrollView.HTMLNodes) {
      domInfo = [self.pluginScrollView.HTMLNodes objectForKey:pluginViewCtrl.divId];
      if (domInfo == nil) {
          return;
      }

    }
    NSString *rectStr = [domInfo objectForKey:@"size"];
    if (rectStr == nil || [rectStr  isEqual: @"null"]) {
      return;
    }

    __block CGRect rect = CGRectFromString(rectStr);
    if (rect.origin.x == 0 &&
        rect.origin.y == 0 &&
        rect.size.width == 0 &&
        rect.size.height == 0) {
      return;
    }
    rect.origin.x *= zoomScale;
    rect.origin.y *= zoomScale;
    rect.size.width *= zoomScale;
    rect.size.height *= zoomScale;
    rect.origin.x += offset.x;
    rect.origin.y += offset.y;

    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;


    // Is the map is displayed?
    if (pluginViewCtrl.view.hidden == NO) {
      if (pluginViewCtrl.isRenderedAtOnce == YES ||
          ([pluginViewCtrl.overlayId hasPrefix:@"panorama_"] ||
            ([pluginViewCtrl.overlayId hasPrefix:@"map_"] &&
            ((GMSMapView *)pluginViewCtrl.view).mapType != kGMSTypeSatellite &&
            ((GMSMapView *)pluginViewCtrl.view).mapType != kGMSTypeHybrid)
          )) {

        if (rect.origin.y + rect.size.height >= offset.y &&
            rect.origin.x + rect.size.width >= offset.x &&
            rect.origin.y < offset.y + webviewHeight &&
            rect.origin.x < offset.x + webviewWidth) {

          // Attach the map view to the parent.
          [pluginViewCtrl.view setTag:pluginViewCtrl.viewDepth];
          [self.pluginScrollView attachView:pluginViewCtrl.view depth:pluginViewCtrl.viewDepth];

        } else {

          // Detach from the parent view
          [pluginViewCtrl.view removeFromSuperview];
        }
      }
    }


    if (pluginViewCtrl.isRenderedAtOnce == YES &&
        rect.origin.x == pluginViewCtrl.view.frame.origin.x &&
        rect.origin.y == pluginViewCtrl.view.frame.origin.y &&
        rect.size.width == pluginViewCtrl.view.frame.size.width &&
        rect.size.height == pluginViewCtrl.view.frame.size.height) {
        return;
    }

    if (pluginViewCtrl.attached) {
//      if (pluginViewCtrl.isRenderedAtOnce) {
//
//        __block int zPosition = pluginViewCtrl.view.layer.zPosition;
//        [UIView animateWithDuration:0.075f animations:^{
//          [pluginViewCtrl.view setFrame:rect];
//          pluginViewCtrl.view.layer.zPosition = zPosition;
//          //rect.origin.x = 0;
//          //rect.origin.y = 0;
//        }];
//      } else {
        [pluginViewCtrl.view setFrame:rect];
        //rect.origin.x = 0;
        //rect.origin.y = 0;
        pluginViewCtrl.isRenderedAtOnce = YES;
//      }
    } else {
      [pluginViewCtrl.view setFrame:CGRectMake(0, -pluginViewCtrl.view.frame.size.height, pluginViewCtrl.view.frame.size.width, pluginViewCtrl.view.frame.size.height)];
    }

}
- (void)execJS: (NSString *)jsString {
    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
        [self.webView performSelector:@selector(stringByEvaluatingJavaScriptFromString:) withObject:jsString];
    } else if ([self.webView respondsToSelector:@selector(evaluateJavaScript:completionHandler:)]) {
        [self.webView performSelector:@selector(evaluateJavaScript:completionHandler:) withObject:jsString withObject:nil];
    }
}
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
  CGPoint browserClickPoint = CGPointMake(point.x - self.webView.frame.origin.x, point.y - self.webView.frame.origin.y);
  //NSLog(@"-->zoomScale = %f", self.webView.scrollView.zoomScale);

  [self.CACHE_FIND_DOM removeAllObjects];

  // Check other views of other plugins before this plugin
  // e.g. PhoneGap-Plugin-ListPicker, etc
  UIView *subview;
  NSArray *subviews = [self.webView.superview subviews];
  //CGRect statusBarFrame = [UIApplication sharedApplication].statusBarFrame;
  //CGPoint subviewPoint = CGPointMake(browserClickPoint.x, browserClickPoint.y - statusBarFrame.size.height);

  for (int i = ((int)[subviews count] - 1); i >= 0; i--) {
    subview = [subviews objectAtIndex: i];
    //NSLog(@"--->subview[%d] = %@", i, subview);
    // we only want to check against other views
    if (subview == self.pluginScrollView) {
      continue;
    }

    if (subview.isHidden || !subview.isUserInteractionEnabled) {
      continue;
    }

    CGPoint subviewPoint = CGPointMake(point.x, point.y - subview.frame.origin.y);
    UIView *hit = [subview hitTest:subviewPoint withEvent:event];

    if (hit) {
      if (subview == self.webView) {
        break;
      }
      return hit;
    }
  }
  if (self.pluginScrollView.mapCtrls == nil || self.pluginScrollView.mapCtrls.count == 0) {
    // Assumes all touches for the browser
    //NSLog(@"--->browser!");
    return [self.webView hitTest:browserClickPoint withEvent:event];
  }

  float offsetX = self.webView.scrollView.contentOffset.x;
  float offsetY = self.webView.scrollView.contentOffset.y;

  float webviewWidth = self.webView.frame.size.width;
  float webviewHeight = self.webView.frame.size.height;


  CGRect rect;
  NSEnumerator *mapIDs = [self.pluginScrollView.mapCtrls keyEnumerator];
  PluginMapViewController *mapCtrl;
  id mapId;
  NSString *clickedDomId;

  CGFloat zoomScale = [[UIScreen mainScreen] scale];
  offsetY *= zoomScale;
  offsetX *= zoomScale;
  webviewWidth *= zoomScale;
  webviewHeight *= zoomScale;

  NSDictionary *domInfo;

  @synchronized(self.pluginScrollView.HTMLNodes) {
    //NSLog(@"--->browserClickPoint = %f, %f", browserClickPoint.x, browserClickPoint.y);
    clickedDomId = [self findClickedDom:@"root" withPoint:browserClickPoint isMapChild:NO overflow:nil];
    //NSLog(@"--->clickedDomId = %@", clickedDomId);

    while(mapId = [mapIDs nextObject]) {
      mapCtrl = [self.pluginScrollView.mapCtrls objectForKey:mapId];
      if (!mapCtrl.divId) {
        continue;
      }
      domInfo =[self.pluginScrollView.HTMLNodes objectForKey:mapCtrl.divId];

      rect = CGRectFromString([domInfo objectForKey:@"size"]);

      // Is the map clickable?
      if (mapCtrl.clickable == NO) {
        //NSLog(@"--> map (%@) is not clickable.", mapCtrl.overlayId);
        continue;
      }

      // Is the map displayed?
      if (rect.origin.y + rect.size.height < 0 ||
          rect.origin.x + rect.size.width < 0 ||
          rect.origin.y > offsetY + webviewHeight ||
          rect.origin.x > offsetX + webviewWidth ||
          mapCtrl.view.hidden == YES) {
        //NSLog(@"--> map (%@) is not displayed.", mapCtrl.overlayId);
        continue;
      }

      // Is the clicked point is in the map rectangle?
      if (CGRectContainsPoint(rect, browserClickPoint)) {
      //NSLog(@"--->in map");

        //NSLog(@"--->clickedDomId = %@, mapCtrl.divId = %@", clickedDomId, mapCtrl.divId);
        if ([mapCtrl.divId isEqualToString:clickedDomId]) {
          // If user click on the map, return the mapCtrl.view.
          CGPoint mapPoint = CGPointMake(point.x - mapCtrl.view.frame.origin.x - self.webView.frame.origin.x, point.y - mapCtrl.view.frame.origin.y - self.webView.frame.origin.y);

          UIView *hitView =[mapCtrl.view hitTest:mapPoint withEvent:event];
          //[mapCtrl mapView:mapCtrl.map didTapAtPoint:point2];

          return hitView;
        }
      }

    }
  }

    //NSLog(@"--->in browser!");
    return [self.webView hitTest:browserClickPoint withEvent:event];
}

- (NSString *)findClickedDom:(NSString *)domId withPoint:(CGPoint)clickPoint isMapChild:(BOOL)isMapChild overflow:(OverflowCSS *)overflow {

  if ([self.CACHE_FIND_DOM objectForKey:domId]) {
    NSString *cacheResult = [self.CACHE_FIND_DOM objectForKey:domId];
    if ([@"(null)" isEqualToString:cacheResult]) {
      cacheResult = nil;
    }
    return cacheResult;
  }

  OverflowCSS *overflow1 = nil;
  if (overflow) {
    overflow1 = [[OverflowCSS alloc] init];
    overflow1.cropX = overflow.cropX;
    overflow1.cropY = overflow.cropY;
    overflow1.rect = CGRectMake(overflow.rect.origin.x, overflow.rect.origin.y, overflow.rect.size.width, overflow.rect.size.height);
  }

  NSDictionary *domInfo = [self.pluginScrollView.HTMLNodes objectForKey:domId];
  NSDictionary *domInfoChild;
  NSString *maxDomId = nil;
  CGRect rect, tmpRect;
  CGDOMRect *rect2, *rect3;
  NSDictionary *zIndexProp;
  unsigned long containMapCnt = 0;

  domInfo = [self.pluginScrollView.HTMLNodes objectForKey:domId];
  if ([domInfo objectForKey:@"containMapIDs"]) {
    NSDictionary *containMapIDs = [domInfo objectForKey:@"containMapIDs"];
    containMapCnt = [[containMapIDs allKeys] count];
  }
  isMapChild = isMapChild || [[domInfo objectForKey:@"isMap"] boolValue] || false;
  //NSLog(@"---- domId = %@, containMapCnt = %ld, isMapChild = %@", domId, containMapCnt, isMapChild ? @"YES":@"NO");

  NSString *pointerEvents = [domInfo objectForKey:@"pointerEvents"];
  NSString *overflowX = [domInfo objectForKey:@"overflowX"];
  NSString *overflowY = [domInfo objectForKey:@"overflowY"];
  if ([@"hidden" isEqualToString:overflowX] || [@"scroll" isEqualToString:overflowX] ||
    [@"hidden" isEqualToString:overflowY] || [@"scroll" isEqualToString:overflowY]) {
    overflow1 = [[OverflowCSS alloc] init];
    overflow1.cropX = [@"hidden" isEqualToString:overflowX] || [@"scroll" isEqualToString:overflowX];
    overflow1.cropY = [@"hidden" isEqualToString:overflowY] || [@"scroll" isEqualToString:overflowY];
    overflow1.rect = CGRectFromString([domInfo objectForKey:@"size"]);
  } else if ([@"visible" isEqualToString:overflowX] || [@"visible" isEqualToString:overflowX]) {
    if (overflow1 != nil) {
      overflow1.cropX = ![@"visible" isEqualToString:overflowX];
      overflow1.cropY = ![@"visible" isEqualToString:overflowY];
    }
  }

  zIndexProp = [domInfo objectForKey:@"zIndex"];
  NSArray *children = [domInfo objectForKey:@"children"];
  int maxZIndex = -1215752192;  //  -1 * pow(2, 32) + 1;
  if ((containMapCnt > 0 || isMapChild || [@"none" isEqualToString:pointerEvents] ||
       [[zIndexProp objectForKey:@"isInherit"] boolValue]) && children != nil && children.count > 0) {

    int zIndexValue;
    NSString *childId, *grandChildId;
    NSArray *grandChildren;

    for (int i = (int)children.count - 1; i >= 0; i--) {
      childId = [children objectAtIndex:i];
      domInfo = [self.pluginScrollView.HTMLNodes objectForKey:childId];
      if (!domInfo) {
        continue;
      }

      zIndexProp = [domInfo objectForKey:@"zIndex"];
      zIndexValue = [[zIndexProp objectForKey:@"z"] intValue];
      if (maxZIndex < zIndexValue || [[zIndexProp objectForKey:@"isInherit"] boolValue]) {

        grandChildren = [domInfo objectForKey:@"children"];
        if (grandChildren == nil || grandChildren.count == 0) {
          tmpRect = CGRectFromString([domInfo objectForKey:@"size"]);
          rect2 = [[CGDOMRect alloc] init];
          rect2.left = tmpRect.origin.x;
          rect2.top = tmpRect.origin.y;
          rect2.right = rect2.left + tmpRect.size.width;
          rect2.bottom = rect2.top + tmpRect.size.height;

          if (overflow1 && ![@"root" isEqualToString:domId]) {

            overflow1.rect = CGRectFromString([domInfo objectForKey:@"size"]);
            rect3 = [[CGDOMRect alloc] init];
            rect3.left = overflow1.rect.origin.x;
            rect3.top = overflow1.rect.origin.y;
            rect3.right = rect3.left + overflow1.rect.size.width;
            rect3.bottom = rect3.top + overflow1.rect.size.height;


            if (overflow1.cropX) {
              rect2.left = MAX(rect2.left, rect3.left);
              rect2.right = MIN(rect2.right, rect3.right);
            }
            if (overflow1.cropY) {
              rect2.top = MAX(rect2.top, rect3.top);
              rect2.bottom = MIN(rect2.bottom, rect3.bottom);
            }

          }

          if (clickPoint.x < rect2.left ||
              clickPoint.y < rect2.top ||
              clickPoint.x > rect2.right ||
              clickPoint.y > rect2.bottom) {
            continue;
          }

          if ([@"none" isEqualToString:[domInfo objectForKey:@"pointerEvents"]]) {
            continue;
          }

          if (maxZIndex < zIndexValue) {
            maxZIndex = zIndexValue;
            maxDomId = childId;
          }

        } else {
          if (zIndexValue < maxZIndex) {
            continue;
          }
          grandChildId = [self findClickedDom:childId withPoint:clickPoint isMapChild: isMapChild overflow:overflow1];
          //NSLog(@"-----[570] childId = %@ grandChildId = %@", childId, grandChildId);
          if (grandChildId == nil) {
            grandChildId = childId;
          } else {
            domInfoChild = [self.pluginScrollView.HTMLNodes objectForKey:grandChildId];
            zIndexProp = [domInfoChild objectForKey:@"zIndex"];
            zIndexValue = [[zIndexProp objectForKey:@"z"] intValue];
          }
          domInfoChild = [self.pluginScrollView.HTMLNodes objectForKey:grandChildId];
          rect = CGRectFromString([domInfoChild objectForKey:@"size"]);


          rect2 = [[CGDOMRect alloc] init];
          rect2.left = rect.origin.x;
          rect2.top = rect.origin.y;
          rect2.right = rect2.left + rect.size.width;
          rect2.bottom = rect2.top + rect.size.height;

          /*
          if (overflow1 && ![@"root" isEqualToString:domId]) {

            NSLog(@"-----[519] grandChildId = %@, size = %@", grandChildId, [domInfo objectForKey:@"size"]);
            overflow1.rect = CGRectFromString([domInfo objectForKey:@"size"]);
            rect3 = [[CGDOMRect alloc] init];
            rect3.left = overflow1.rect.origin.x;
            rect3.top = overflow1.rect.origin.y;
            rect3.right = rect3.left + overflow1.rect.size.width;
            rect3.bottom = rect3.top + overflow1.rect.size.height;


            if (overflow1.cropX) {
              rect2.left = MAX(rect2.left, rect3.left);
              rect2.right = MIN(rect2.right, rect3.right);
            }
            if (overflow1.cropY) {
              rect2.top = MAX(rect2.top, rect3.top);
              rect2.bottom = MIN(rect2.bottom, rect3.bottom);
            }

          }
          */
          if (clickPoint.x < rect2.left ||
              clickPoint.y < rect2.top ||
              clickPoint.x > rect2.right ||
              clickPoint.y > rect2.bottom) {
            continue;
          }


          if ([@"none" isEqualToString:[domInfoChild objectForKey:@"pointerEvents"]]) {
            continue;
          }
          if (maxZIndex < zIndexValue) {
            maxZIndex = zIndexValue;
            maxDomId = grandChildId;
          }
        }

      }
    }
  }

  if (maxDomId == nil && ![@"root" isEqualToString:domId]) {
    if ([@"none" isEqualToString:pointerEvents]) {
      [self.CACHE_FIND_DOM setValue:@"(null)" forKey:domId];
      return nil;
    }

    domInfo = [self.pluginScrollView.HTMLNodes objectForKey:domId];
    rect = CGRectFromString([domInfo objectForKey:@"size"]);

    rect2 = [[CGDOMRect alloc] init];
    rect2.left = rect.origin.x;
    rect2.top = rect.origin.y;
    rect2.right = rect2.left + rect.size.width;
    rect2.bottom = rect2.top + rect.size.height;


//    if (overflow1) {
//
//      overflow1.rect = CGRectFromString([domInfo objectForKey:@"size"]);
//      rect3 = [[CGDOMRect alloc] init];
//      rect3.left = overflow1.rect.origin.x;
//      rect3.top = overflow1.rect.origin.y;
//      rect3.right = rect3.left + overflow1.rect.size.width;
//      rect3.bottom = rect3.top + overflow1.rect.size.height;
//
//
//      if (overflow1.cropX) {
//        rect2.left = MAX(rect2.left, rect3.left);
//        rect2.right = MIN(rect2.right, rect3.right);
//      }
//      if (overflow1.cropY) {
//        rect2.top = MAX(rect2.top, rect3.top);
//        rect2.bottom = MIN(rect2.bottom, rect3.bottom);
//      }
//
//    }

    if (clickPoint.x < rect2.left ||
        clickPoint.y < rect2.top ||
        clickPoint.x > rect2.right ||
        clickPoint.y > rect2.bottom) {
      [self.CACHE_FIND_DOM setValue:@"(null)" forKey:domId];
      return nil;
    }

    maxDomId = domId;
  }

  if (!maxDomId) {
    [self.CACHE_FIND_DOM setValue:@"(null)" forKey:domId];
  } else {
    [self.CACHE_FIND_DOM setValue:maxDomId forKey:domId];
  }

  return maxDomId;
}


@end
