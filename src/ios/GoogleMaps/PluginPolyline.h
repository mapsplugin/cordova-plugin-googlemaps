//
//  PluginPolyline.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"

@interface PluginPolyline : CDVPlugin<IPluginProtocol>
@property (nonatomic, strong) PluginMapViewController* mapCtrl;
@property (nonatomic) BOOL initialized;

- (void)create:(CDVInvokedUrlCommand*)command;
- (void)setStrokeColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeWidth:(CDVInvokedUrlCommand*)command;
- (void)setPoints:(CDVInvokedUrlCommand*)command;
- (void)removePointAt:(CDVInvokedUrlCommand*)command;
- (void)setPointAt:(CDVInvokedUrlCommand*)command;
- (void)insertPointAt:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setClickable:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
- (void)setGeodesic:(CDVInvokedUrlCommand*)command;

@end
