//
//  GoogleMaps.h
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import <Cordova/CDV.h>
#import <GoogleMaps/GoogleMaps.h>

@interface GoogleMaps : CDVPlugin

- (void)GoogleMap_getMap:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_show:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setCenter:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setMyLocationEnabled:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setIndoorEnabled:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setTrafficEnabled:(CDVInvokedUrlCommand*)command;
- (void)GoogleMap_setCompassEnabled:(CDVInvokedUrlCommand*)command;

@end
