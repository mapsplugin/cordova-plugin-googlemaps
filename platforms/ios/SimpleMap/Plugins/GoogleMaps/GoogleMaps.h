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
#import "Map.h"

@interface GoogleMaps : CDVPlugin

@property (nonatomic, readwrite, strong) NSMutableDictionary* plugins;
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;

- (void)exec:(CDVInvokedUrlCommand*)command;
- (void)showMap:(CDVInvokedUrlCommand*)command;
- (void)getMap:(CDVInvokedUrlCommand*)command;
- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;

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
