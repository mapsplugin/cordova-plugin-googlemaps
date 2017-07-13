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

@interface PluginMarkerCluster : PluginMarker<MyPlgunProtocol>
@property (atomic, strong) NSMutableDictionary *pluginMarkers;
@property (atomic, strong) NSMutableDictionary *waitCntManager;
@property (atomic, strong) NSMutableDictionary *_pluginResults;

- (void)create:(CDVInvokedUrlCommand*)command;
- (void)redrawClusters:(CDVInvokedUrlCommand*)command;
@end
