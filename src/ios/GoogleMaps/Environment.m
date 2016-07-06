//
//  Environment.m
//  SimpleMap
//
//  Created by masashi on 06/26/16.
//
//

#import "Environment.h"

@implementation Environment

- (void)isAvailable:(CDVInvokedUrlCommand *)command {
  // Return true always in iOS.
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)setBackGroundColor:(CDVInvokedUrlCommand *)command {

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    GoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];
  
    NSArray *rgbColor = [command.arguments objectAtIndex:0];
    googlemaps.pluginLayer.backgroundColor = [rgbColor parsePluginColor];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getLicenseInfo:(CDVInvokedUrlCommand *)command {


  // TODO: stub
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"stub"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setDebuggable:(CDVInvokedUrlCommand *)command {

  // TODO: stub
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
