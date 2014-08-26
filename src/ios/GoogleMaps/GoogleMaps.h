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
#import "R9HTTPRequest.h"
#import "MyPluginLayer.h"

@interface GoogleMaps : CDVPlugin<CLLocationManagerDelegate, UIScrollViewDelegate>

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic) UIView *licenseLayer;
@property (nonatomic) UIView *footer;
@property (nonatomic) UIButton *closeButton;
@property (nonatomic) UIButton *licenseButton;
@property (nonatomic, strong) CLLocationManager *locationManager;
@property (nonatomic, strong) NSMutableArray *locationCommandQueue;
@property (nonatomic) UIScrollView *pluinScrollView;
@property (nonatomic) UIView *root;
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
@end
