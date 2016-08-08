//
//  MyPluginLayerDebugView.m
//  DevApp
//
//  Created by Katsumata Masashi on 9/22/14.
//
//

#import "MyPluginLayerDebugView.h"

@implementation MyPluginLayerDebugView


-  (id)initWithFrame:(CGRect)aRect
{
  self = [super initWithFrame:aRect];
  self.drawRects = [[NSMutableDictionary alloc] init];
  self.HTMLNodes = [[NSMutableDictionary alloc] init];
  self.mapCtrls = [[NSMutableDictionary alloc] init];
  self.opaque = NO;
  self.debuggable = YES;

  return self;
}

- (void)putHTMLElement:(NSString *)domId size:(NSDictionary *)size {
  [self.HTMLNodes setObject:size forKey:domId];
  [self setNeedsDisplay];
}
- (void)removeHTMLElement:(NSString *)domId {
  [self.HTMLNodes removeObjectForKey:domId];
  [self setNeedsDisplay];
}
- (void)clearHTMLElement {
  [self.HTMLNodes removeAllObjects];
  [self setNeedsDisplay];
}

- (void)drawRect:(CGRect)rect
{
    if (!self.debuggable) {
      return;
    }

    float offsetX = self.webView.scrollView.contentOffset.x;
    float offsetY = self.webView.scrollView.contentOffset.y;
    
    float webviewWidth = self.webView.frame.size.width;
    float webviewHeight = self.webView.frame.size.height;
  
  
  
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextClearRect(context, rect);
    CGContextSetRGBFillColor(context, 0, 1.0, 0, 0.4);
    CGFloat zoomScale = self.webView.scrollView.zoomScale;
  
    offsetY *= zoomScale;
    offsetX *= zoomScale;
    webviewWidth *= zoomScale;
    webviewHeight *= zoomScale;
  
    CGRect htmlElementRect;
    NSEnumerator *mapIDs = [self.drawRects keyEnumerator];
    GoogleMapsViewController *mapCtrl;
    id mapId;
    NSDictionary *elements;
    NSString *elemSize, *rectStr;
    //NSLog(@"--> point = %f, %f", point.x, point.y);
    while(mapId = [mapIDs nextObject]) {
        rectStr = [self.drawRects objectForKey:mapId];
        //NSLog(@"%@ = %@", mapId, rectStr);
        rect = CGRectFromString(rectStr);
        rect.origin.x *= zoomScale;
        rect.origin.y *= zoomScale;
        rect.size.width *= zoomScale;
        rect.size.height *= zoomScale;
        mapCtrl = [self.mapCtrls objectForKey:mapId];
      
        // Is the map is displayed?
        if (rect.origin.y + rect.size.height < offsetY ||
            rect.origin.x + rect.size.width < offsetX ||
            rect.origin.y > offsetY + webviewHeight ||
            rect.origin.x > offsetX + webviewWidth ||
            mapCtrl.view.hidden == YES) {
            continue;
        }
        CGContextFillRect(context, rect);
  
      
      
      /*

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

/*
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

        }*/
    }



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

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
  NSLog(@"--->hitTest");
  return [super hitTest:point withEvent:event];
}

@end
