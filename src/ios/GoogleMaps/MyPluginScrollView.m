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
  self.HTMLNodes = [[NSMutableDictionary alloc] init];
  self.mapCtrls = [[NSMutableDictionary alloc] init];
  return self;
}

- (void)attachView:(UIView *)view depth:(NSInteger)depth {
  NSArray *subviews = [self subviews];
  int viewCnt = (int)[subviews count];
  int index = viewCnt;

  NSArray *sortedArray;
  sortedArray = [subviews sortedArrayUsingComparator:^NSComparisonResult(id a, id b) {
    NSInteger first = ((UIView*)a).tag;
    NSInteger second = ((UIView*)b).tag;
    return first - second;
  }];


  for (int i = 0; i < sortedArray.count; i++) {
    NSInteger tag = ((UIView *)[sortedArray objectAtIndex:i]).tag;
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

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    return [super hitTest:point withEvent:event];
}
@end
