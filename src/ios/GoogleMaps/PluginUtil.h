//
//  PluginUtil.h
//  SimpleMap
//
//  Created by masashi on 11/15/13.
//
//

#ifndef MIN
#import <NSObjCRuntime.h>
#endif
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "MainViewController.h"
#import <QuartzCore/QuartzCore.h>
#import <objc/runtime.h>
//#import "MFGoogleMapAdditions/GMSCoordinateBounds+Geometry.h"
#import "GMSCoordinateBounds+Geometry.h"

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
- (NSString*)regReplace:(NSString*)pattern replaceTxt:(NSString*)replaceTxt options:(NSRegularExpressionOptions)options;
@end


@interface UIImage (GoogleMapsPlugin)
- (UIImage*)imageByApplyingAlpha:(CGFloat) alpha;
- (UIImage *)resize:(CGFloat)width height:(CGFloat)height;
@end

//
// Override the webViewDidFinishLoad
// http://stackoverflow.com/questions/5272451/overriding-methods-using-categories-in-objective-c#5272612
//
@interface MainViewController (CDVViewController)
#if CORDOVA_VERSION_MIN_REQUIRED < __CORDOVA_4_0_0
- (void)webViewDidFinishLoad:(UIWebView*)theWebView;
#endif
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
+ (BOOL)isInDebugMode;
+ (NSString *)getAbsolutePathFromCDVFilePath:(UIView*)theWebView cdvFilePath:(NSString *)cdvFilePath;
@end



@implementation UIGestureRecognizer (Cancel)
- (void)cancel {
    self.enabled = NO;
    self.enabled = YES;
}
@end
