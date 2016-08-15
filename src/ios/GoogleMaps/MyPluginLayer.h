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

- (id)initWithWebView:(UIWebView *)webView;
- (void)putHTMLElements:(NSDictionary *)elementsDic;
- (void)putHTMLElement:(NSString *)domId domInfo:(NSDictionary *)domInfo;
- (void)removeHTMLElement:(NSString *)domId;
- (void)clearHTMLElements;
- (void)updateViewPosition:(NSString *)mapId;
- (void)addMapView:(NSString *)mapId mapCtrl:(GoogleMapsViewController *)mapCtrl;

@end
