//
//  External.m
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "External.h"

@implementation External

/**
 * Launch the Google Maps App if it is installed.
 * Otherwise launch the Apple Map.
 */
-(void)launchNavigation:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:0];
  NSString *from = [json objectForKey:@"from"];
  NSString *to = [json objectForKey:@"to"];
  NSString *directionsRequest = nil;
  
  NSURL *googleMapsURLScheme = [NSURL URLWithString:@"comgooglemaps-x-callback://"];
  if ([[UIApplication sharedApplication] canOpenURL:googleMapsURLScheme]) {
    NSString *appName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    directionsRequest =
      [NSString stringWithFormat: @"comgooglemaps-x-callback://?saddr=%@&daddr=%@&x-success=sourceapp://?resume=true&x-source=%@",
        from, to, appName, nil];
  } else {
    directionsRequest =
      [NSString stringWithFormat: @"http://maps.apple.com/?saddr=%@&daddr=%@",
        from, to, nil];
  }
  NSURL *directionsURL = [NSURL URLWithString:directionsRequest];
  [[UIApplication sharedApplication] openURL:directionsURL];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
