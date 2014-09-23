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
  self.HTMLNodes = [[NSMutableDictionary alloc] init];
  self.opaque = NO;
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

- (void)drawRect:(CGRect)rect
{
  if (self.debuggable == NO) {
    return;
  }
  CGContextRef context = UIGraphicsGetCurrentContext();
  
  float left = [[self.embedRect objectForKey:@"left"] floatValue] - self.offsetX;
  float top = [[self.embedRect objectForKey:@"top"] floatValue] - self.offsetY;
  float width = [[self.embedRect objectForKey:@"width"] floatValue];
  float height = [[self.embedRect objectForKey:@"height"] floatValue];
  
  CGRect rectangle = CGRectMake(0, 0, 0, 0);
  
  //---------------------------------
  // Draw the HTML elements region
  //---------------------------------
  CGContextSetRGBFillColor(context, 1.0, 0, 0, 0.4);
  NSDictionary *elemSize;
  for (NSString *domId in self.HTMLNodes) {
    elemSize = [self.HTMLNodes objectForKey:domId];
    left = [[elemSize objectForKey:@"left"] floatValue] - self.offsetX;
    top = [[elemSize objectForKey:@"top"] floatValue] - self.offsetY;
    width = [[elemSize objectForKey:@"width"] floatValue];
    height = [[elemSize objectForKey:@"height"] floatValue];
  
    
    rectangle.origin.x = left;
    rectangle.origin.y = top;
    rectangle.size.width = width;
    rectangle.size.height = height;
    CGContextFillRect(context, rectangle);
  
  }

}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
  [self setNeedsDisplay];
  return [super hitTest:point withEvent:event];
}

@end
