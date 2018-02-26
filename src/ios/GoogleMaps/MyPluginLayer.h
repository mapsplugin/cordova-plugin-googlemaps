//
//  MyPluginLayer.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <UIKit/UIKit.h>
#import "GoogleMapsViewController.h"
#import "MyPluginScrollView.h"

@interface OverflowCSS : NSObject
@property BOOL cropX;
@property BOOL cropY;
@property CGRect rect;
@end

@interface MyPluginLayer : UIView<UIScrollViewDelegate>

@property (nonatomic) UIView *webView;
@property (nonatomic) MyPluginScrollView *pluginScrollView;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (atomic, strong) NSObject *_lockObject;

- (id)initWithWebView:(UIView *)webView;
- (void)clearHTMLElements;
- (void)putHTMLElements:(NSDictionary *)elementsDic;
- (void)addMapView:(GoogleMapsViewController *)mapCtrl;
- (void)removeMapView:(GoogleMapsViewController *)mapCtrl;
- (void)updateViewPosition:(GoogleMapsViewController *)mapCtrl;
@end
