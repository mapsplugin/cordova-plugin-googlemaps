//
//  PluginMarkerCluster.h
//  cordova-googlemaps-plugin
//
//  Created by masashi.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"
#import "PluginUtil.h"
#import "PluginMarker.h"

@interface PluginMarkerCluster : PluginMarker<IPluginProtocol>
@property (atomic, strong) NSMutableDictionary *debugFlags;
@property (atomic, strong) NSMutableDictionary *pluginMarkers;
@property (atomic, strong) NSMutableDictionary *waitCntManager;
@property (atomic, strong) NSMutableDictionary *allResults;
@property (atomic, strong) NSMutableArray *deleteMarkers;
@property (atomic, strong) dispatch_semaphore_t semaphore;
@property (atomic, strong) dispatch_semaphore_t deleteThreadLock;
@property (nonatomic) BOOL stopFlag;

- (void)create:(CDVInvokedUrlCommand*)command;
- (void)redrawClusters:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
@end
