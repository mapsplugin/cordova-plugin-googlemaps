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

- (void)Marker_showInfoWindow:(CDVInvokedUrlCommand*)command;
- (void)Marker_hideInfoWindow:(CDVInvokedUrlCommand*)command;
- (void)Marker_getPosition:(CDVInvokedUrlCommand*)command;
- (void)Marker_setSnippet:(CDVInvokedUrlCommand*)command;
- (void)Marker_setTitle:(CDVInvokedUrlCommand*)command;
- (void)Marker_remove:(CDVInvokedUrlCommand*)command;

//@http://cocoamatic.blogspot.com/2010/07/uicolor-macro-with-hex-values.html
//RGB color macro
#define UIColorFromRGB(rgbValue) [UIColor \
colorWithRed:((float)((rgbValue & 0xFF0000) >> 16))/255.0 \
green:((float)((rgbValue & 0xFF00) >> 8))/255.0 \
blue:((float)(rgbValue & 0xFF))/255.0 alpha:1.0]
 
//RGB color macro with alpha
#define UIColorFromRGBWithAlpha(rgbValue,a) [UIColor \
colorWithRed:((float)((rgbValue & 0xFF0000) >> 16))/255.0 \
green:((float)((rgbValue & 0xFF00) >> 8))/255.0 \
blue:((float)(rgbValue & 0xFF))/255.0 alpha:a]
@end
