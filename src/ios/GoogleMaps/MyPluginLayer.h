//
//  MyPluginLayer.h
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import <UIKit/UIKit.h>
#import <GoogleMaps/GoogleMaps.h>
#import "MyPluginScrollView.h"

@interface MyPluginLayer : UIView<UIScrollViewDelegate>

@property (nonatomic) UIWebView *webView;
@property (nonatomic) NSMutableDictionary *drawRects;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) BOOL clickable;
@property (nonatomic) NSMutableDictionary *HTMLNodes;
@property (nonatomic) NSMutableDictionary *mapViews;
@property (nonatomic) MyPluginScrollView *pluginScrollView;

- (id)initWithWebView:(UIWebView *)webView;
- (void)putHTMLElement:(NSString *)mapId domId:(NSString *)domId size:(NSDictionary *)size;
- (void)removeHTMLElement:(NSString *)mapId domId:(NSString *)domId;
- (void)clearHTMLElement:(NSString *)mapId;
- (void)updateViewPosition:(NSString *)mapId;
- (void)addMapView:(NSString *)mapId map:(GMSMapView *)map;

@end
