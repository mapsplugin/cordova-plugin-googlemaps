//
//  Environment.h
//  cordova-googlemaps-plugin v2
//
//  Created by masashi.
//
//

#import <Cordova/CDV.h>
#import "CordovaGoogleMaps.h"

@interface Environment : CDVPlugin

- (void)isAvailable:(CDVInvokedUrlCommand*)command;
- (void)setBackGroundColor:(CDVInvokedUrlCommand*)command;
- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;
- (void)setDebuggable:(CDVInvokedUrlCommand*)command;
@end
