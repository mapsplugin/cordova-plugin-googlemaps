//
//  PluginMarkerCluster.h
//  cordova-googlemaps-plugin
//
//  Created by masashi.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"
#import "PluginMarker.h"
#import "NSData+Base64.h"

@interface PluginMarkerCluster : CDVPlugin<MyPlgunProtocol>

@property (nonatomic) NSMutableDictionary *objects;
@property (nonatomic) NSMutableDictionary *pluginMarkers;
@property (nonatomic) NSMutableDictionary *waitCntManager;
@property (nonatomic) NSMutableDictionary *_pluginResults;

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;
- (void)redrawClusters:(CDVInvokedUrlCommand*)command;
@end
