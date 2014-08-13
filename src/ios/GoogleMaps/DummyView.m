//
//  DummyView.m
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import "DummyView.h"

@implementation DummyView
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
  float left = [[self.embedRect objectForKey:@"left"] floatValue] - self.webView.scrollView.contentOffset.x;
  float top = [[self.embedRect objectForKey:@"top"] floatValue] - self.webView.scrollView.contentOffset.y;
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
  NSLog(@"--map");

    return self.map;
  }
  NSLog(@"--browser");

  
  return [super hitTest:point withEvent:event];
/*
  UIView *result = [super hitTest:point withEvent:event];
  CGPoint buttonPoint = [underButton convertPoint:point fromView:self];
  if ([underButton pointInside:buttonPoint withEvent:event]) {
    return underButton;
  }
  return result;
  */
}

@end
