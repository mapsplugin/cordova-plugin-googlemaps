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
@property (nonatomic, strong) NSMutableDictionary* objects;
@property (nonatomic) NSOperationQueue *executeQueue;
- (void)create:(CDVInvokedUrlCommand*)command;

- (void)setColor:(CDVInvokedUrlCommand*)command;
- (void)setWidth:(CDVInvokedUrlCommand*)command;
- (void)removePointAt:(CDVInvokedUrlCommand*)command;
- (void)setPointAt:(CDVInvokedUrlCommand*)command;
- (void)insertPointAt:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setClickable:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
- (void)setGeodesic:(CDVInvokedUrlCommand*)command;

@end
