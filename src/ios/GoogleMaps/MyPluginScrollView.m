//
//  MyPluginScrollView.m
//  DevApp
//
//  Created by masashi on 9/22/14.
//
//

#import "MyPluginScrollView.h"

@implementation MyPluginScrollView

UIView *myView = nil;

-  (id)initWithFrame:(CGRect)aRect
{
  self = [super initWithFrame:aRect];
  self.debugView = [[MyPluginLayerDebugView alloc] initWithFrame:aRect];
  self.touchableView = [[MyTouchableView alloc] initWithFrame:aRect];
  return self;
}

- (void)attachView:(UIView *)view {
  myView = view;
  [myView addSubview:self.touchableView];
  [self addSubview:view];
  [self addSubview:self.debugView];
}
- (void)dettachView {
  [self.touchableView removeFromSuperview];
  [myView removeFromSuperview];
  [self.debugView removeFromSuperview];
}
@end
