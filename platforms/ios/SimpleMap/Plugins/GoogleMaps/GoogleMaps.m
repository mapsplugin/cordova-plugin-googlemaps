//
//  GoogleMaps.m
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import "GoogleMaps.h"

@implementation GoogleMaps
- (void)GoogleMap_getMap:(CDVInvokedUrlCommand *)command {
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Hello"];
    NSLog(@"GoogleMaps!");

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
