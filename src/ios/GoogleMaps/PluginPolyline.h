//
//  Polyline.h
//  SimpleMap
//
//  Created by masashi on 11/14/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface PluginPolyline : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;

- (void)setColor:(CDVInvokedUrlCommand*)command;
- (void)setWidth:(CDVInvokedUrlCommand*)command;
- (void)setPoints:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
- (void)setGeodesic:(CDVInvokedUrlCommand*)command;

@end
