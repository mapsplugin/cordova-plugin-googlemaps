//
//  External.m
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "External.h"

@implementation External
-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}
/**
 */
-(void)launchNavigation:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  NSString *from = [json objectForKey:@"from"];
  NSString *to = [json objectForKey:@"to"];
  NSString *directionsRequest = nil;
  
  NSURL *googleMapsURLScheme = [NSURL URLWithString:@"comgooglemaps-x-callback://"];
  if ([[UIApplication sharedApplication] canOpenURL:googleMapsURLScheme]) {
    NSString *bundleIdentifier = [[NSBundle mainBundle] bundleIdentifier];
    directionsRequest =
      [NSString stringWithFormat: @"comgooglemaps-x-callback://?saddr=%@&daddr=%@&x-success=sourceapp://?resume=true&x-source=%@",
        from, to, bundleIdentifier, nil];
  } else {
    directionsRequest =
      [NSString stringWithFormat: @"http://maps.apple.com/?saddr=%@&daddr=%@",
        from, to, nil];
  }
  NSURL *directionsURL = [NSURL URLWithString:directionsRequest];
  [[UIApplication sharedApplication] openURL:directionsURL];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
