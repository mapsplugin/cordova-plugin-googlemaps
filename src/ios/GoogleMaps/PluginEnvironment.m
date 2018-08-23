//
//  PluginEnvironment.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginEnvironment.h"

@implementation PluginEnvironment

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
  dispatch_async(queue, ^{
      //-------------------------------
      // Check the Google Maps API key
      //-------------------------------
      NSString *errorMsg = nil;
      NSString *APIKey = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"Google Maps API Key"];
      if (!APIKey) {
        errorMsg = [PluginUtil PGM_LOCALIZATION:@"APIKEY_IS_UNDEFINED_MESSAGE"];
      } else {

        /*---------------------------------------------------------------------------------------
         * If CFBundleExecutable is not English, the Google Maps SDK for iOS will crash.
         * So must be English.
         *
         * If you want to use non-english name for your app, you need to change your config.xml like this.
         *
         * <?xml version='1.0' encoding='utf-8'?>
         * <widget id="(package name)" version="0.0.1" xmlns="http://www.w3.org/ns/widgets" xmlns:cdv="http://cordova.apache.org/ns/1.0">
         *   <name short="(non-english app name)">(english app name)</name>
         *---------------------------------------------------------------------------------------*/

        NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
        NSString *CFBundleExecutable = [info objectForKey:@"CFBundleExecutable"];

        NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"^[a-zA-Z0-9$@$!%*?&#^\\-_.\\s+]+$" options:NSRegularExpressionCaseInsensitive error:nil];
        if ([regex numberOfMatchesInString:CFBundleExecutable options:0 range:NSMakeRange(0, CFBundleExecutable.length)] == 0) {
          errorMsg = [PluginUtil PGM_LOCALIZATION:@"APP_NAME_ERROR_MESSAGE"];
        }
      }
      CDVPluginResult* pluginResult;
      if (errorMsg) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMsg];
      } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      }
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  });

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

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
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

@end
