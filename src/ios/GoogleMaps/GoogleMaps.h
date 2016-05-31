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
#import "PluginUtil.h"
#import "MyReachability.h"
#import "Map.h"
#import "MyPluginLayer.h"

@interface GoogleMaps : CDVPlugin<CLLocationManagerDelegate>

@property (nonatomic) MyPluginLayer *pluginLayer;
@property (nonatomic) UIView *licenseLayer;
@property (nonatomic) UIView *footer;
@property (nonatomic) UIButton *closeButton;
@property (nonatomic) UIButton *licenseButton;
@property (nonatomic, strong) CLLocationManager *locationManager;
@property (nonatomic, strong) NSMutableArray *locationCommandQueue;
@property (nonatomic) NSMutableDictionary *mapPlugins;

- (void)exec:(CDVInvokedUrlCommand*)command;
- (void)showDialog:(CDVInvokedUrlCommand*)command;
- (void)closeDialog:(CDVInvokedUrlCommand*)command;
- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;
- (void)getMyLocation:(CDVInvokedUrlCommand*)command;
- (void)resizeMap:(CDVInvokedUrlCommand *)command;
- (void)isAvailable:(CDVInvokedUrlCommand *)command;
- (void)pluginLayer_pushHtmlElement:(CDVInvokedUrlCommand*)command;
- (void)pluginLayer_removeHtmlElement:(CDVInvokedUrlCommand*)command;
@end
