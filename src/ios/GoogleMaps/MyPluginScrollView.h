//
//  MyPluginScrollView.h
//  DevApp
//
//  Created by masashi on 9/22/14.
//
//

#import <UIKit/UIKit.h>
#import "MyPluginLayerDebugView.h"

@interface MyPluginScrollView : UIScrollView

@property (nonatomic) MyPluginLayerDebugView *debugView;

- (void)attachView:(UIView *)view;
- (void)dettachView;
@end
