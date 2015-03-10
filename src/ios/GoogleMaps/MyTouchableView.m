//
//  MyTouchableView.m
//  DevApp
//
//  Created by Masashi Katsumata on 3/10/15.
//
//


#import "MyTouchableView.h"

@implementation MyTouchableView

-  (id)initWithFrame:(CGRect)aRect
{
  self = [super initWithFrame:aRect];
  return self;
}


/**
 http://abi.exdream.com/post/2010/03/18/iPhone-How-to-pass-touch-events-from-UIScrollView-to-the-parent-UIViewController.aspx
 */
- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];
  
  
  NSLog(@"touchBegan : %f, %f", point.x, point.y);
  
  [self.mapCtrl.view touchesEnded:touches withEvent:event];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];
  
  
  NSLog(@"touchesMoved : %f, %f", point.x, point.y);
  [self.mapCtrl.view touchesEnded:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];
  
  NSLog(@"touchesEnded : %f, %f", point.x, point.y);
  [self.mapCtrl.view touchesEnded:touches withEvent:event];
}
- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
  UITouch *touch = [touches anyObject];
  CGPoint point = [touch locationInView:self];
  
  NSLog(@"touchesCancelled : %f, %f", point.x, point.y);
  [self.mapCtrl.view touchesEnded:touches withEvent:event];
}

@end
