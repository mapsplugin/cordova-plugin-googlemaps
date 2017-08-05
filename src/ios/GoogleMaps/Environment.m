//
//  Environment.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
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
  dispatch_async(queue, ^{
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  });

}

- (void)setBackGroundColor:(CDVInvokedUrlCommand *)command {

  dispatch_async(dispatch_get_main_queue(), ^{
      // Load the GoogleMap.m
      CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
      CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];

      id webViewId = self.webView;
      NSString *clsName = [webViewId className];
      NSURL *url;
      if ([clsName isEqualToString:@"UIWebView"]) {
        url = ((UIWebView *)webViewId).request.URL;
      } else {
        url = [webViewId URL];
      }
      NSString *currentURL = url.absoluteString;
      currentURL = [currentURL stringByDeletingLastPathComponent];
      currentURL = [currentURL stringByReplacingOccurrencesOfString:@"file:" withString:@""];
      currentURL = [currentURL stringByReplacingOccurrencesOfString:@"//" withString:@"/"];
      NSRange range = [currentURL rangeOfString:cdvViewController.wwwFolderName];
      if (range.location != NSNotFound) {
        currentURL = [currentURL substringToIndex:range.location];
      }
      NSString *iconPath = [NSString stringWithFormat:@"file://%@%@/images/starbucks.gif",currentURL, cdvViewController.wwwFolderName];

      //NSLog(@"--->currentUrl = %@", iconPath);

      NSString *jsString = [NSString stringWithFormat:@"javascript:onSetBackgroundCSS(\"url('%@')\");", iconPath];


      if ([googlemaps.pluginLayer.backgroundWebview respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
        [googlemaps.pluginLayer.backgroundWebview performSelector:@selector(stringByEvaluatingJavaScriptFromString:) withObject:jsString];
      } else if ([googlemaps.pluginLayer.backgroundWebview respondsToSelector:@selector(evaluateJavaScript:completionHandler:)]) {
        [googlemaps.pluginLayer.backgroundWebview performSelector:@selector(evaluateJavaScript:completionHandler:) withObject:jsString withObject:nil];
      }

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  });
/*
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
*/
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
