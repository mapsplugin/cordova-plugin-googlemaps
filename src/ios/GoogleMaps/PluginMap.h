//
//  Map.h
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "NSData+Base64.h"

@interface PluginMap : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic) BOOL isRemoved;
@property (nonatomic) NSOperationQueue *loadPluginQueue;
@property (nonatomic) NSOperationQueue *executeQueue;

- (void)clear:(CDVInvokedUrlCommand*)command;
- (void)setClickable:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setCameraTilt:(CDVInvokedUrlCommand*)command;
- (void)setCameraTarget:(CDVInvokedUrlCommand*)command;
- (void)setCameraBearing:(CDVInvokedUrlCommand *)command;
- (void)setCameraZoom:(CDVInvokedUrlCommand*)command;
- (void)setDiv:(CDVInvokedUrlCommand *)command;
- (void)setMapTypeId:(CDVInvokedUrlCommand*)command;
- (void)animateCamera:(CDVInvokedUrlCommand*)command;
- (void)loadPlugin:(CDVInvokedUrlCommand*)command;
- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)moveCamera:(CDVInvokedUrlCommand*)command;
- (void)setMyLocationEnabled:(CDVInvokedUrlCommand*)command;
- (void)setIndoorEnabled:(CDVInvokedUrlCommand*)command;
- (void)setTrafficEnabled:(CDVInvokedUrlCommand*)command;
- (void)setCompassEnabled:(CDVInvokedUrlCommand*)command;
- (void)getCameraPosition:(CDVInvokedUrlCommand*)command;
- (void)toDataURL:(CDVInvokedUrlCommand*)command;
- (void)getVisibleRegion:(CDVInvokedUrlCommand*)command;
- (void)setOptions:(CDVInvokedUrlCommand*)command;
- (void)setAllGesturesEnabled:(CDVInvokedUrlCommand*)command;
- (void)setPadding:(CDVInvokedUrlCommand*)command;
- (void)panBy:(CDVInvokedUrlCommand*)command;
- (void)getFocusedBuilding:(CDVInvokedUrlCommand*)command;

@end
