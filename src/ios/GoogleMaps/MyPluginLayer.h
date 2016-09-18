//
//  MyPluginLayer.h
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import <UIKit/UIKit.h>
#import "GoogleMapsViewController.h"
#import "MyPluginScrollView.h"

@interface MyPluginLayer : UIView<UIScrollViewDelegate>

@property (nonatomic) UIWebView *webView;
@property (nonatomic) MyPluginScrollView *pluginScrollView;
@property (nonatomic) BOOL stopFlag;
@property (nonatomic) BOOL needUpdatePosition;

- (id)initWithWebView:(UIWebView *)webView;
- (void)updateViewPosition:(GoogleMapsViewController *)mapCtrl;
- (void)putHTMLElements:(NSDictionary *)elementsDic;
- (void)addMapView:(GoogleMapsViewController *)mapCtrl;
- (void)removeMapView:(GoogleMapsViewController *)mapCtrl;
@end
