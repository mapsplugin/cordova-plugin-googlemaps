//
//  MyPluginScrollView.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "MyPluginScrollView.h"

@implementation MyPluginScrollView


-  (id)initWithFrame:(CGRect)aRect
{
  self = [super initWithFrame:aRect];
  self.debugView = [[MyPluginLayerDebugView alloc] initWithFrame:CGRectMake(0, 0, aRect.size.width, 5000)];
  // Avoid the white bar that appears at the top of the map with iPhone iOS 11
  // See problem description here: https://github.com/mapsplugin/cordova-plugin-googlemaps/issues/1909
  //
  // https://github.com/vidinoti/cordova-plugin-googlemaps/commit/0894072be260223f4ee833e422adf011af9740dd
  if (@available(iOS 11, *)) {
    self.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever;
  }
  return self;
}

- (void)attachView:(UIView *)view {
  [self.debugView removeFromSuperview];
  [self addSubview:view];
  [self addSubview:self.debugView];
}
- (void)detachView:(UIView *)view {
  [self.debugView removeFromSuperview];
  [view removeFromSuperview];
}
@end
