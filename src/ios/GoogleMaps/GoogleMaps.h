//
//  GoogleMaps.h
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import <Cordova/CDV.h>
#import <GoogleMaps/GoogleMaps.h>
#import "GoogleMapsViewController.h"
#import "Map.h"
#import "PluginUtil.h"
#import "MyPluginLayer.h"
#import "MyReachability.h"
#import "MyPluginScrollView.h"

@interface GoogleMaps : CDVPlugin<CLLocationManagerDelegate, UIScrollViewDelegate>

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic) UIView *licenseLayer;
@property (nonatomic) UIView *footer;
@property (nonatomic) UIButton *closeButton;
@property (nonatomic) UIButton *licenseButton;
@property (nonatomic, strong) CLLocationManager *locationManager;
@property (nonatomic, strong) NSMutableArray *locationCommandQueue;
@property (nonatomic) MyPluginScrollView *pluginScrollView;
@property (nonatomic) MyPluginLayer *pluginLayer;

- (void)exec:(CDVInvokedUrlCommand*)command;
- (void)showDialog:(CDVInvokedUrlCommand*)command;
- (void)closeDialog:(CDVInvokedUrlCommand*)command;
- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;
- (void)getMyLocation:(CDVInvokedUrlCommand*)command;
- (void)resizeMap:(CDVInvokedUrlCommand *)command;
- (void)setDiv:(CDVInvokedUrlCommand *)command;
- (void)isAvailable:(CDVInvokedUrlCommand *)command;
- (void)clear:(CDVInvokedUrlCommand*)command;
- (void)pluginLayer_pushHtmlElement:(CDVInvokedUrlCommand*)command;
- (void)pluginLayer_removeHtmlElement:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
@end
