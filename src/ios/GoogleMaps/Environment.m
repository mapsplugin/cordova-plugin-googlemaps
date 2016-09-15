//
//  Environment.m
//  SimpleMap
//
//  Created by masashi on 06/26/16.
//
//

#import "Environment.h"

@implementation Environment

dispatch_queue_t queue;

- (void)pluginInitialize
{
  queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0ul);
}

- (void)pluginUnload
{
  queue = nil;
}
- (void)isAvailable:(CDVInvokedUrlCommand *)command {
  // Return true always in iOS.
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)setBackGroundColor:(CDVInvokedUrlCommand *)command {

  dispatch_async(queue, ^{
      // Load the GoogleMap.m
      CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
      CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
    
      NSArray *rgbColor = [command.arguments objectAtIndex:0];
      dispatch_async(dispatch_get_main_queue(), ^{
          googlemaps.pluginLayer.backgroundColor = [rgbColor parsePluginColor];
      });

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  });
}

- (void)getLicenseInfo:(CDVInvokedUrlCommand *)command {

  dispatch_async(queue, ^{
      dispatch_async(dispatch_get_main_queue(), ^{
        NSString *txt = [GMSServices openSourceLicenseInfo];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:txt];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });
  });
}

- (void)setDebuggable:(CDVInvokedUrlCommand *)command {
    Boolean isDebuggable = [[command.arguments objectAtIndex:0] boolValue];
  
    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
  
    googlemaps.pluginLayer.pluginScrollView.debugView.debuggable = isDebuggable;
    [googlemaps.pluginLayer.pluginScrollView.debugView setNeedsDisplay];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
