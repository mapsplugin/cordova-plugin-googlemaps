//
//  Polygon.h
//  SimpleMap
//
//  Created by masashi on 11/13/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
@interface PluginPolygon : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic, strong) NSMutableDictionary* objects;
@property (nonatomic) NSOperationQueue *executeQueue;

- (void)create:(CDVInvokedUrlCommand*)command;
- (void)setFillColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeWidth:(CDVInvokedUrlCommand*)command;
- (void)setHoles:(CDVInvokedUrlCommand*)command;
- (void)setPoints:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand*)command;
- (void)setClickable:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
- (void)setGeodesic:(CDVInvokedUrlCommand*)command;
@end
