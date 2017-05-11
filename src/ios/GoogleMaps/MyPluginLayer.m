//
//  DummyView.m
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import "MyPluginLayer.h"
#import "UIView+ColorOfPoint.h"

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
    if (self.mapCtrl.map == nil) {
        return [self.webView.scrollView hitTest:point withEvent:event];
    }

    [self.webView setAlpha:1.0];
    [self.webView setBackgroundColor:[UIColor clearColor]];
    [self.webView setFrame:[UIScreen mainScreen].bounds];
    unsigned char alpha = [self.webView.scrollView colorAlphaPoint:point];

    
    if (alpha != 0) {
        return [self.webView.scrollView hitTest:point withEvent:event];
    } else {
        return [self.mapCtrl.view hitTest:point withEvent:event];
    }

    return nil;
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


@end
