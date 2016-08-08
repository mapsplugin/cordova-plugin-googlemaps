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
#import "Map.h"
#import "MyPluginLayer.h"
#import "MyPlgunProtocol.h"

@interface GoogleMaps : CDVPlugin<CLLocationManagerDelegate>

@property (nonatomic) MyPluginLayer *pluginLayer;
@property (nonatomic, strong) CLLocationManager *locationManager;
@property (nonatomic, strong) NSMutableArray *locationCommandQueue;
@property (nonatomic) NSMutableDictionary *mapPlugins;

- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)getMyLocation:(CDVInvokedUrlCommand*)command;
- (void)unload:(CDVInvokedUrlCommand*)command;
@end
