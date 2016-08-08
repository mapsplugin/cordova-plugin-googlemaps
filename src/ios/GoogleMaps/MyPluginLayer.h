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
@property (nonatomic) NSMutableDictionary *drawRects;
@property (nonatomic) NSMutableDictionary *HTMLNodes;
@property (nonatomic) NSMutableDictionary *mapCtrls;
@property (nonatomic) MyPluginScrollView *pluginScrollView;

- (id)initWithWebView:(UIWebView *)webView;
- (void)putHTMLElement:(NSString *)mapId domId:(NSString *)domId size:(NSDictionary *)size;
- (void)removeHTMLElement:(NSString *)mapId domId:(NSString *)domId;
- (void)clearHTMLElement:(NSString *)mapId;
- (void)updateViewPosition:(NSString *)mapId;
- (void)addMapView:(NSString *)mapId mapCtrl:(GoogleMapsViewController *)mapCtrl;

@end
