//
//  PluginUtil.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#if __has_include("CapacitorCordova.h")
  #define PGM_PLATFORM_CAPACITOR
#else
  #define PGM_PLATFORM_CORDOVA
#endif


#ifndef MIN
  #import <NSObjCRuntime.h>
#endif
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>

#ifdef PGM_PLATFORM_CORDOVA
  #import "MainViewController.h"
#endif

#import <QuartzCore/QuartzCore.h>
#import <objc/runtime.h>
#import <math.h>
#import "IPluginProtocol.h"
#import "PluginViewController.h"
#import <Cordova/CDVCommandDelegate.h>
#import <Cordova/CDVCommandDelegateImpl.h>
#import "UIImageCache.h"

typedef void (^MYCompletionHandler)(NSError *error);

// Switch statement in Objective-C
//http://qiita.com/GeneralD/items/5a05f176ac2321e7a51b
#define CASE(str) if ([__s__ isEqualToString:(str)])
#define SWITCH(s) for (NSString *__s__ = (s); __s__; __s__ = nil)
#define DEFAULT

@interface UIView (GoogleMapsPlugin)
- (void)setFrameWithDictionary:(NSDictionary *) params;
- (void)setFrameWithInt:(int)left top:(int)top width:(int)width height:(int)height;
@end

@interface NSArray (GoogleMapsPlugin)
- (UIColor*)parsePluginColor;
@end

@interface NSString (GoogleMapsPlugin)
- (NSString *)urlencode;
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
typedef void (^TIFAnimationGroupCompletionBlock)(void);
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
+ (void)getJsonWithURL:(NSString *)baseUrlStr params:(NSDictionary *)params completionBlock:(void (^)(BOOL succeeded, NSDictionary *response, NSString *error))completionBlock;
+ (void)getJsonWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, NSDictionary *response, NSString *error))completionBlock;
+ (double)getZoomFromBounds:(GMSCoordinateBounds *)bounds mapWidth:(double)mapWidth mapHeight:(double)mapHeight;
+ (double)_zoom:(double)mapPx worldPx:(double)worldPx fraction:(double)fraction;
+ (double)_latRad:(double)lat;
+ (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock;
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
