//
//  Map.h
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"
//#import "NSData-Base64/NSData+Base64.h"
#import "NSData+Base64.h"

@interface Map : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)setTilt:(CDVInvokedUrlCommand*)command;
- (void)setCenter:(CDVInvokedUrlCommand*)command;
- (void)setZoom:(CDVInvokedUrlCommand*)command;
- (void)setMapTypeId:(CDVInvokedUrlCommand*)command;
- (void)animateCamera:(CDVInvokedUrlCommand*)command;
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

- (void)drawMarker:(CDVInvokedUrlCommand*)command;
- (void)drawMarkerCallbackCalled:(GMSMarker *)marker;

- (void)drawPolygon:(CDVInvokedUrlCommand*)command;
- (void)completeDrawnShape:(CDVInvokedUrlCommand*)command;

- (void)deleteLastDrawnVertex:(CDVInvokedUrlCommand*)command;

- (void)cancelDrawing:(CDVInvokedUrlCommand*)command;

@end
