//
//  PluginEnvironment.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Cordova/CDV.h>
#import "CordovaGoogleMaps.h"

@interface PluginEnvironment : CDVPlugin

- (void)isAvailable:(CDVInvokedUrlCommand*)command;
- (void)setBackGroundColor:(CDVInvokedUrlCommand*)command;
- (void)getLicenseInfo:(CDVInvokedUrlCommand*)command;
@end
