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
@property (nonatomic) NSMutableDictionary* objects;
@property (atomic, strong) NSMutableDictionary *debugFlags;
@property (atomic, strong) NSMutableDictionary *pluginMarkers;
@property (atomic, strong) NSMutableDictionary *waitCntManager;
@property (atomic, strong) NSMutableArray *deleteMarkers;
@property (atomic, strong) dispatch_semaphore_t semaphore;
@property (nonatomic) BOOL stopFlag;

- (void)create:(CDVInvokedUrlCommand*)command;
- (void)redrawClusters:(CDVInvokedUrlCommand*)command;
@end
