//
//  Marker.h
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"
#import "PluginMarker.h"
#import "NSData+Base64.h"

@interface PluginMarkerClusterer : CDVPlugin<MyPlgunProtocol>

@property (nonatomic, strong) NSCache* imgCache;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic) NSMutableDictionary *objects;
@property (nonatomic) NSMutableDictionary *pluginMarkers;
@property (nonatomic) NSMutableDictionary *resolutions;
@property (nonatomic) int waitCnt;
@property (nonatomic) NSMutableDictionary *_pluginResults;

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;
- (void)setResolution:(CDVInvokedUrlCommand*)command;
- (void)deleteClusters:(CDVInvokedUrlCommand*)command;
- (void)redrawClusters:(CDVInvokedUrlCommand*)command;
@end
