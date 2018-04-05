//
//  PluginCircle.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"

@interface PluginCircle : CDVPlugin<IPluginProtocol>
@property (nonatomic, strong) PluginMapViewController* mapCtrl;
@property (nonatomic) BOOL initialized;

- (void)create:(CDVInvokedUrlCommand*)command;
- (void)setCenter:(CDVInvokedUrlCommand*)command;
- (void)setFillColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeWidth:(CDVInvokedUrlCommand*)command;
- (void)setRadius:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
@end
