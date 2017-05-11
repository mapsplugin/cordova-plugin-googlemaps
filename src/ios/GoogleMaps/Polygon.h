//
//  Polygon.h
//  SimpleMap
//
//  Created by masashi on 11/13/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface Polygon : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)createPolygon:(CDVInvokedUrlCommand*)command;
- (void)setFillColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeColor:(CDVInvokedUrlCommand*)command;
- (void)setStrokeWidth:(CDVInvokedUrlCommand*)command;
- (void)setPoints:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
- (void)setGeodesic:(CDVInvokedUrlCommand*)command;
- (void)setEditable:(CDVInvokedUrlCommand *)command;
@end
