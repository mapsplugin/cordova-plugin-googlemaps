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
  self.debugView = [[MyPluginLayerDebugView alloc] initWithFrame:CGRectMake(0, 0, aRect.size.width, 5000)];
  
  return self;
}

- (void)attachView:(UIView *)view {
  [self.debugView removeFromSuperview];
  [self addSubview:view];
  [self addSubview:self.debugView];
}
- (void)dettachView {
  [myView removeFromSuperview];
  [self.debugView removeFromSuperview];
}
@end
