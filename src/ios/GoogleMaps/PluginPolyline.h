//
//  PluginPolyline.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface PluginPolyline : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic) NSMutableDictionary* objects;
@property (nonatomic) NSOperationQueue *executeQueue;
- (void)create:(CDVInvokedUrlCommand*)command;

- (void)setStrokeColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeWidth:(CDVInvokedUrlCommand*)command;
- (void)removePointAt:(CDVInvokedUrlCommand*)command;
- (void)setPointAt:(CDVInvokedUrlCommand*)command;
- (void)insertPointAt:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setClickable:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
- (void)setGeodesic:(CDVInvokedUrlCommand*)command;

@end
