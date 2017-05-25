//
//  External.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
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
  
  from = [from stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
  to = [to stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
  
  
  NSURL *googleMapsURLScheme = [NSURL URLWithString:@"comgooglemaps-x-callback://"];
  if ([[UIApplication sharedApplication] canOpenURL:googleMapsURLScheme]) {
  
    NSMutableArray *params = [NSMutableArray array];
    [params addObject:[NSString stringWithFormat:@"saddr=%@", from, nil]];
    [params addObject:[NSString stringWithFormat:@"daddr=%@", to, nil]];
    if ([json objectForKey:@"travelMode"] != nil) {
      [params addObject:[NSString stringWithFormat:@"directionsmode=%@", [json objectForKey:@"travelMode"], nil]];
    }
    NSString *bundleIdentifier = [[NSBundle mainBundle] bundleIdentifier];
    [params addObject:[NSString stringWithFormat:@"x-success=%@://?resume=true", bundleIdentifier, nil]];
    
    NSString *appName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    appName = [appName stringByReplacingOccurrencesOfString:@" " withString:@""];
    [params addObject:[NSString stringWithFormat:@"x-source=%@", appName, nil]];
    
    directionsRequest =
      [NSString stringWithFormat: @"comgooglemaps-x-callback://?%@", [params componentsJoinedByString: @"&"], nil];
  } else {
    directionsRequest =
      [NSString stringWithFormat: @"https://maps.apple.com/?saddr=%@&daddr=%@",
        from, to, nil];
  }
  NSURL *directionsURL = [NSURL URLWithString:directionsRequest];
  [[UIApplication sharedApplication] openURL:directionsURL];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
