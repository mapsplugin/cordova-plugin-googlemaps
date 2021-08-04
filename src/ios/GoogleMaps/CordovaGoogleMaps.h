//
//  CordovaGoogleMaps.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Cordova/CDV.h>
#import <GoogleMaps/GoogleMaps.h>
#import "PluginMapViewController.h"
#import "PluginStreetViewPanoramaController.h"
#import "PluginUtil.h"
#import "PluginMap.h"
#import "PluginStreetViewPanorama.h"
#import "PgmPluginLayer.h"
#import "IPluginProtocol.h"

static NSMutableDictionary *viewPlugins;

@interface CordovaGoogleMaps : CDVPlugin

@property (nonatomic) BOOL isSdkAvailable;
@property (nonatomic) PgmPluginLayer *pluginLayer;
@property (nonatomic) NSOperationQueue *executeQueue;

+ (id)getViewPlugin:(NSString *)pluginId;

- (void)updateMapPositionOnly:(CDVInvokedUrlCommand *)command;
- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)getPanorama:(CDVInvokedUrlCommand*)command;
- (void)clearHtmlElements:(CDVInvokedUrlCommand *)command;
- (void)putHtmlElements:(CDVInvokedUrlCommand *)command;
- (void)removeMap:(CDVInvokedUrlCommand *)command;
- (void)pause:(CDVInvokedUrlCommand *)command;
- (void)resume:(CDVInvokedUrlCommand *)command;
- (void)setDiv:(CDVInvokedUrlCommand *)command;
- (void)attachToWebView:(CDVInvokedUrlCommand*)command;
- (void)detachFromWebView:(CDVInvokedUrlCommand*)command;
- (void)resizeMap:(CDVInvokedUrlCommand *)command;
- (void)setBackGroundColor:(CDVInvokedUrlCommand *)command;
- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;
- (void)setEnv:(CDVInvokedUrlCommand*)command;
@end
