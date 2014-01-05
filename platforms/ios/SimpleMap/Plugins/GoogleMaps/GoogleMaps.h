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

@interface GoogleMaps : CDVPlugin

@property (nonatomic, readwrite, strong) NSMutableDictionary* plugins;
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;

- (void)exec:(CDVInvokedUrlCommand*)command;
- (void)showDialog:(CDVInvokedUrlCommand*)command;
- (void)closeDialog:(CDVInvokedUrlCommand*)command;
- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;
- (void)getMyLocation:(CDVInvokedUrlCommand*)command;
@end
