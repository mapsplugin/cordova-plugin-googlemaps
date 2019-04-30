//
//  PluginUtil.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#ifndef MIN
#import <NSObjCRuntime.h>
#endif
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <QuartzCore/QuartzCore.h>
#import <objc/runtime.h>
//#import "MFGoogleMapAdditions/GMSCoordinateBounds+Geometry.h"
#import "GMSCoordinateBounds+Geometry.h"
#import <math.h>
#import "IPluginProtocol.h"
#import "PluginViewController.h"
#import <Cordova/CDVCommandDelegateImpl.h>

typedef void (^MYCompletionHandler)(NSError *error);

// Switch statement in Objective-C
//http://qiita.com/GeneralD/items/5a05f176ac2321e7a51b
#define CASE(str) if ([__s__ isEqualToString:(str)])
#define SWITCH(s) for (NSString *__s__ = (s); __s__; __s__ = nil)
#define DEFAULT


#if __has_include(<CordovaPluginsStatic_vers.c>)
  #define PGM_PLATFORM_CAPACITOR

  @class CDVViewController;
  @class CDVCommandQueue;

  @interface CDVCommandDelegateImpl (GoogleMapsPlugin)
    @property (nonatomic,retain) CDVPluginManager *manager;
    @property (nonatomic,retain) WKWebView *webView;
    @property (nonatomic,retain) NSRegularExpression *callbackIdPattern;

    - (id)initWithWebView:(WKWebView*)webView pluginManager:(CDVPluginManager *)manager;
    - (void)flushCommandQueueWithDelayedJs;

    - (NSRegularExpression *)callbackIdPattern;
    - (WKWebView *)webView;
    - (CDVPluginManager *)manager;
    - (NSString*)pathForResource:(NSString*)resourcepath;
    - (void)evalJsHelper2:(NSString*)js;
    - (BOOL)isValidCallbackId:(NSString*)callbackId;
    - (void)sendPluginResult:(CDVPluginResult*)result callbackId:(NSString*)callbackId;
    - (void)evalJs:(NSString*)js;
    - (void)evalJs:(NSString*)js scheduledOnRunLoop:(BOOL)scheduledOnRunLoop;
    - (id)getCommandInstance:(NSString*)pluginName;
    - (void)runInBackground:(void (^)())block;
    - (NSString*)userAgent;
    - (NSDictionary*)settings;
  @end
#else
  #define PGM_PLATFORM_CORDOVA
#endif


@interface UIView (GoogleMapsPlugin)
- (void)setFrameWithDictionary:(NSDictionary *) params;
- (void)setFrameWithInt:(int)left top:(int)top width:(int)width height:(int)height;
@end

@interface NSArray (GoogleMapsPlugin)
- (UIColor*)parsePluginColor;
@end

@interface NSString (GoogleMapsPlugin)
- (NSString*)regReplace:(NSString*)pattern replaceTxt:(NSString*)replaceTxt options:(NSRegularExpressionOptions)options;
@end


@interface UIImage (GoogleMapsPlugin)
- (UIImage*)imageByApplyingAlpha:(CGFloat) alpha;
- (UIImage *)resize:(CGFloat)width height:(CGFloat)height;
@end

//
// animationDidStop for group animation
// http://stackoverflow.com/a/28051909/697856
//
typedef void (^TIFAnimationGroupCompletionBlock)();
@interface CAAnimationGroup (Blocks)
- (void)setCompletionBlock:(TIFAnimationGroupCompletionBlock)handler;
@end


@interface PluginUtil : NSObject
+ (BOOL)isPolygonContains:(GMSPath *)path coordinate:(CLLocationCoordinate2D)coordinate projection:(GMSProjection *)projection;
+ (CLLocationCoordinate2D)isPointOnTheLine:(GMSPath *)path coordinate:(CLLocationCoordinate2D)coordinate projection:(GMSProjection *)projection;
+ (CLLocationCoordinate2D)isPointOnTheGeodesicLine:(GMSPath *)path coordinate:(CLLocationCoordinate2D)coordinate threshold:(double)threshold projection:(GMSProjection *)projection;
+ (BOOL)isCircleContains:(GMSCircle *)circle coordinate:(CLLocationCoordinate2D)point;
+ (BOOL)isInDebugMode;
+ (GMSMutablePath *)getMutablePathFromCircle:(CLLocationCoordinate2D)center radius:(double)radius;
+ (NSString *)getAbsolutePathFromCDVFilePath:(UIView*)theWebView cdvFilePath:(NSString *)cdvFilePath;
+ (NSString *)PGM_LOCALIZATION:(NSString *)key;
@end



@implementation UIGestureRecognizer (Cancel)
- (void)cancel {
    self.enabled = NO;
    self.enabled = YES;
}
@end



@interface CDVPlugin (GoogleMapsPlugin)
- (void)setPluginViewController: (PluginViewController*)viewCtrl;
@end
