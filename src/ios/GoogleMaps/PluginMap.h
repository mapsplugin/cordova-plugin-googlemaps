//
//  PluginMap.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"
#import "NSData+Base64.h"
#import "IPluginView.h"

@interface PluginMap : CDVPlugin<IPluginProtocol, IPluginView>
@property (nonatomic, strong) PluginMapViewController* mapCtrl;
@property (nonatomic) BOOL isRemoved;
@property (nonatomic) BOOL initialized;

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
- (void)setMyLocationButtonEnabled:(CDVInvokedUrlCommand*)command;
- (void)setIndoorEnabled:(CDVInvokedUrlCommand*)command;
- (void)setTrafficEnabled:(CDVInvokedUrlCommand*)command;
- (void)setCompassEnabled:(CDVInvokedUrlCommand*)command;
- (void)attachToWebView:(CDVInvokedUrlCommand*)command;
- (void)detachFromWebView:(CDVInvokedUrlCommand*)command;
- (void)toDataURL:(CDVInvokedUrlCommand*)command;
- (void)setOptions:(CDVInvokedUrlCommand*)command;
- (void)setAllGesturesEnabled:(CDVInvokedUrlCommand*)command;
- (void)setPadding:(CDVInvokedUrlCommand*)command;
- (void)panBy:(CDVInvokedUrlCommand*)command;
- (void)getFocusedBuilding:(CDVInvokedUrlCommand*)command;
- (void)setActiveMarkerId:(CDVInvokedUrlCommand*)command;

@end


