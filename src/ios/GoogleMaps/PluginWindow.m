//
//  PluginWindow.m
//  DevApp
//
//  Created by masashi on 8/12/14.
//
//

#import "PluginWindow.h"

@implementation PluginWindow


- (void)sendEvent:(UIEvent *)event {
  NSSet *touches = [event allTouches];
  UITouch *touch = touches.anyObject;
  CGPoint point = [touch locationInView:self];
  
  //NSLog(@"x=%f, y=%f", point.x, point.y);
  
  
  float left = [[self.embedRect objectForKey:@"left"] floatValue];
  float top = [[self.embedRect objectForKey:@"top"] floatValue];
  float width = [[self.embedRect objectForKey:@"width"] floatValue];
  float height = [[self.embedRect objectForKey:@"height"] floatValue];
  
  if (touch.phase == UITouchPhaseBegan) {
    if (point.x >= left && point.x <= (left + width) &&
        point.y >= top && point.y <= (top + height)) {
      self.isMapAction = YES;
    } else {
      self.isMapAction = NO;
    }
  }
  
  if (self.isMapAction == NO) {
  NSLog(@"---sendEvent");
    [super sendEvent:event];
    return;
  }
  NSLog(@"---map");
  
}

@end
