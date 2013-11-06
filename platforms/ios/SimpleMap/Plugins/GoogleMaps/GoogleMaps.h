//
//  GoogleMaps.h
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import <Cordova/CDV.h>
#import <GoogleMaps/GoogleMaps.h>
#import "GoogleMapsViewController.h"

@class MyViewController;
@interface GoogleMaps : CDVPlugin

- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setTilt:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_getMap:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setCenter:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setZoom:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setMapTypeId:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_addMarker:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_addCircle:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_show:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_animateCamera:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_moveCamera:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setMyLocationEnabled:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setIndoorEnabled:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setTrafficEnabled:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setCompassEnabled:(CDVInvokedUrlCommand*)command;
@end
