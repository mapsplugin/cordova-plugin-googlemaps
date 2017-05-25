//
//  MyPluginScrollView.h
//  DevApp
//
//  Created by masashi.
//
//

#import <UIKit/UIKit.h>
#import "MyPluginLayerDebugView.h"

@interface MyPluginScrollView : UIScrollView

@property (nonatomic) MyPluginLayerDebugView *debugView;

- (void)attachView:(UIView *)view;
@end
