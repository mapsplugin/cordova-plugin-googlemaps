//
//  MyPluginLayer.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <UIKit/UIKit.h>
#import "PluginMapViewController.h"
#import "MyPluginScrollView.h"

@interface OverflowCSS : NSObject
@property BOOL cropX;
@property BOOL cropY;
@property CGRect rect;
@end

@interface MyPluginLayer : UIView<UIScrollViewDelegate>

@property (nonatomic) UIView *webView;
@property (nonatomic) MyPluginScrollView *pluginScrollView;
@property (atomic) NSTimer *redrawTimer;
@property (nonatomic) BOOL isSuspended;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (atomic, strong) NSObject *_lockObject;

- (id)initWithWebView:(UIView *)webView;
- (void)resizeTask:(NSTimer *)timer;
- (void)clearHTMLElements;
- (void)putHTMLElements:(NSDictionary *)elementsDic;
- (void)addPluginOverlay:(PluginViewController *)mapCtrl;
- (void)removePluginOverlay:(PluginViewController *)mapCtrl;
- (void)updateViewPosition:(PluginViewController *)mapCtrl;
- (void)startRedrawTimer;
- (void)stopRedrawTimer;
@end
