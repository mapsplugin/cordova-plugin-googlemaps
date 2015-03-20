//
//  DummyView.m
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import "MyPluginLayer.h"

@implementation MyPluginLayer

-  (id)initWithFrame:(CGRect)aRect
{
  self = [super initWithFrame:aRect];
  self.HTMLNodes = [[NSMutableDictionary alloc] init];
  self.clickable = YES;
  self.debuggable = NO;
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

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
  if (self.clickable == NO ||
      self.mapCtrl.map == nil ||
      self.mapCtrl.map.hidden == YES) {
    return [super hitTest:point withEvent:event];
  }
  
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
    
    NSLog(@"hitClass = %@", hitView);
    return self;
    //return hitView;
  }
  
  return [super hitTest:point withEvent:event];
}

- (void)drawRect:(CGRect)rect
{
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
}

/**
http://abi.exdream.com/post/2010/03/18/iPhone-How-to-pass-touch-events-from-UIScrollView-to-the-parent-UIViewController.aspx
*/
- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];

  
  NSLog(@"touchBegan : %f, %f", point.x, point.y);
  
  for (UIView *child in self.mapCtrl.view.subviews) {
  NSLog(@"child = %@", child);
    [child touchesEnded:touches withEvent:event];
    }
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];
  
    
  NSLog(@"touchesMoved : %f, %f", point.x, point.y);
  
  for (UIView *child in self.mapCtrl.view.subviews)
    [child touchesEnded:touches withEvent:event];

  //[hitView touchesBegan:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];
    
  NSLog(@"touchesEnded : %f, %f", point.x, point.y);
  
  for (UIView *child in self.mapCtrl.view.subviews)
    [child touchesEnded:touches withEvent:event];
}
- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];
    
  NSLog(@"touchesCancelled : %f, %f", point.x, point.y);
  
  for (UIView *child in self.mapCtrl.view.subviews)
    [child touchesEnded:touches withEvent:event];
}

@end
