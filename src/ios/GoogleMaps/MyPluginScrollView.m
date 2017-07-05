//
//  MyPluginScrollView.m
//  cordova-googlemaps-plugin v2
//
//  Created by masashi.
//
//

#import "MyPluginScrollView.h"

@implementation MyPluginScrollView


-  (id)initWithFrame:(CGRect)aRect
{
  self = [super initWithFrame:aRect];
  self.debugView = [[MyPluginLayerDebugView alloc] initWithFrame:CGRectMake(0, 0, aRect.size.width, 5000)];
  
  return self;
}

- (void)attachView:(UIView *)view {
  [self.debugView removeFromSuperview];
  [self addSubview:view];
  [self addSubview:self.debugView];
}
@end
