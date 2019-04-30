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
#import "MyPluginLayer.h"
#import "IPluginProtocol.h"
#import "IPluginView.h"

@interface CordovaGoogleMaps : CDVPlugin

@property (nonatomic) BOOL isSdkAvailable;
@property (nonatomic) MyPluginLayer *pluginLayer;
@property (nonatomic) NSMutableDictionary *viewPlugins;
@property (nonatomic) NSOperationQueue *executeQueue;

- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)getPanorama:(CDVInvokedUrlCommand*)command;
- (void)clearHtmlElements:(CDVInvokedUrlCommand *)command;
- (void)putHtmlElements:(CDVInvokedUrlCommand *)command;
- (void)removeMap:(CDVInvokedUrlCommand *)command;
- (void)pause:(CDVInvokedUrlCommand *)command;
- (void)resume:(CDVInvokedUrlCommand *)command;
@end
