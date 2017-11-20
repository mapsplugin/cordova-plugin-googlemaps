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

@interface MyPluginLayer : UIView<UIScrollViewDelegate>

@property (nonatomic) UIView *webView;
@property (nonatomic) MyPluginScrollView *pluginScrollView;
@property (nonatomic) NSTimer *redrawTimer;
@property (nonatomic) BOOL isSuspended;
@property (nonatomic) BOOL stopFlag;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic) dispatch_semaphore_t semaphore;

- (id)initWithWebView:(UIView *)webView;
- (void)resizeTask:(NSTimer *)timer;
- (void)clearHTMLElements;
- (void)putHTMLElements:(NSDictionary *)elementsDic;
- (void)addMapView:(GoogleMapsViewController *)mapCtrl;
- (void)removeMapView:(GoogleMapsViewController *)mapCtrl;
- (void)updateViewPosition:(GoogleMapsViewController *)mapCtrl;
@end
