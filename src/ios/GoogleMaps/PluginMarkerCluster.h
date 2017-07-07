//
//  Marker.h
//  cordova-googlemaps-plugin
//
//  Created by masashi on 11/8/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"
#import "PluginMarker.h"
#import "NSData+Base64.h"

@interface PluginMarkerCluster : CDVPlugin<MyPlgunProtocol>

@property (nonatomic, strong) NSCache* imgCache;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic) NSMutableDictionary *objects;
@property (nonatomic) NSMutableDictionary *pluginMarkers;
@property (nonatomic) NSMutableDictionary *resolutions;
@property (nonatomic) int waitCnt;
@property (nonatomic) NSMutableDictionary *_pluginResults;

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;
- (void)redrawClusters:(CDVInvokedUrlCommand*)command;
@end
