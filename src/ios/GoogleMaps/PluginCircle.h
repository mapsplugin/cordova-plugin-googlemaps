//
//  Circle.h
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface PluginCircle : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) NSMutableDictionary* objects;
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic) NSOperationQueue *executeQueue;
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
