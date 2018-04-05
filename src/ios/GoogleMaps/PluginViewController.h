//
//  PluginViewController.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>
#import <math.h>
#import "PluginUtil.h"
#import "NSData+Base64.h"
#import "IPluginProtocol.h"
#import "PluginObjects.h"
#import <GoogleMaps/GoogleMaps.h>

@interface PluginViewController : UIViewController

@property (nonatomic, strong) UIView* webView;
@property (nonatomic) NSMutableDictionary* plugins;
@property (nonatomic) BOOL attached;
@property (nonatomic) BOOL isFullScreen;
@property (nonatomic) BOOL isDragging;
@property (nonatomic) CGRect screenSize;
@property (nonatomic) CGFloat screenScale;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) NSString *overlayId;
@property (nonatomic) BOOL clickable;
@property (nonatomic) BOOL isRenderedAtOnce;
@property (nonatomic, readwrite, strong) NSString *divId;
@property (nonatomic, strong) PluginObjects *objects;
@property (atomic, strong) NSOperationQueue *executeQueue;

- (id)initWithOptions:(NSDictionary *) options;
- (void)execJS: (NSString *)jsString;
@end
