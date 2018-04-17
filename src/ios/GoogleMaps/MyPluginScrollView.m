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
  // Avoid the white bar that appears at the top of the map with iPhone iOS 11
  // See problem description here: https://github.com/mapsplugin/cordova-plugin-googlemaps/issues/1909
  //
  // https://github.com/vidinoti/cordova-plugin-googlemaps/commit/0894072be260223f4ee833e422adf011af9740dd
  if (@available(iOS 11, *)) {
    self.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever;
  }
  return self;
}

- (void)attachView:(UIView *)view depth:(NSInteger)depth {
  NSArray *subviews = [self subviews];
  UIView *subview;
  NSInteger tag;
  int viewCnt = (int)[subviews count];
  int index = viewCnt;
  for (int i = 0; i < viewCnt; i++) {
    subview = [subviews objectAtIndex: i];
    tag = subview.tag;
    if (tag == 0) {
      continue;
    }
    if (tag > depth) {
      index = i;
      break;
    }
  }
  
  [self insertSubview:view atIndex:index];
}
- (void)detachView:(UIView *)view {
  [view removeFromSuperview];
  
}
@end
