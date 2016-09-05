//
//  Environment.h
//  SimpleMap
//
//  Created by masashi on 06/26/16.
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
