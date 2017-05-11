//
//  MyPluginScrollView.h
//  DevApp
//
//  Created by masashi on 9/22/14.
//
//

#import <UIKit/UIKit.h>
#import "MyPluginLayerDebugView.h"
#import <WebKit/WKWebView.h>

@interface MyPluginScrollView : UIScrollView
@property (nonatomic) MyPluginLayerDebugView *debugView;
- (void)attachView:(WKWebView *)view;
- (void)dettachView;
@end
