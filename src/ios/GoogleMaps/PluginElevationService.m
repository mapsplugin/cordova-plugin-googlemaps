//
//  PluginElevationService.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginElevationService.h"

@implementation PluginElevationService

- (void)pluginUnload
{
  if (self.executeQueue != nil){
      self.executeQueue.suspended = YES;
      [self.executeQueue cancelAllOperations];
      self.executeQueue.suspended = NO;
      self.executeQueue = nil;
  }
}

- (void)pluginInitialize
{
  self.executeQueue = [NSOperationQueue new];
  self.executeQueue.maxConcurrentOperationCount = 3;

}

- (void)getElevationAlongPath:(CDVInvokedUrlCommand*)command {
  NSDictionary *json = [command.arguments objectAtIndex:0];
  
  NSArray *path = [json objectForKey:@"path"];
  int i = 0;
  NSDictionary *latLng;
  GMSMutablePath *mutablePath = [GMSMutablePath path];
  for (i = 0; i < path.count; i++) {
      latLng = [path objectAtIndex:i];
      [mutablePath
        addCoordinate:
          CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue])];
  }
  
  NSString *encodedPoint = [mutablePath encodedPath];
  encodedPoint = [encodedPoint stringByAddingPercentEncodingWithAllowedCharacters:NSCharacterSet.URLHostAllowedCharacterSet];
  encodedPoint = [NSString stringWithFormat:@"enc:%@", encodedPoint];
  
  NSMutableDictionary *params = [NSMutableDictionary dictionary];
  [params setObject:encodedPoint forKey:@"path"];
  [params setObject:[json objectForKey:@"samples"] forKey:@"samples"];
  
  [self httpGetWithDictionary:params command:command];
}

-(void)getElevationForLocations:(CDVInvokedUrlCommand *)command
{
  
  NSDictionary *json = [command.arguments objectAtIndex:0];
  
  NSArray *path = [json objectForKey:@"locations"];
  int i = 0;
  NSDictionary *latLng;
  GMSMutablePath *mutablePath = [GMSMutablePath path];
  for (i = 0; i < path.count; i++) {
      latLng = [path objectAtIndex:i];
      [mutablePath
        addCoordinate:
          CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue])];
  }
  
  NSString *encodedPoint = [mutablePath encodedPath];
  encodedPoint = [encodedPoint stringByAddingPercentEncodingWithAllowedCharacters:NSCharacterSet.URLHostAllowedCharacterSet];
  encodedPoint = [NSString stringWithFormat:@"enc:%@", encodedPoint];
  
  NSMutableDictionary *params = [NSMutableDictionary dictionary];
  [params setObject:encodedPoint forKey:@"locations"];
  
  [self httpGetWithDictionary:params command:command];
}

- (void)httpGetWithDictionary:(NSDictionary *)params command:(CDVInvokedUrlCommand *)command
{
  
  NSString *urlStr = @"https://maps.googleapis.com/maps/api/elevation/json?";
  
  NSUserDefaults *myDefaults = [[NSUserDefaults alloc] initWithSuiteName:@"cordova.plugin.googlemaps"];
  NSString *DEFAULT_APIKey = [myDefaults objectForKey:@"GOOGLE_MAPS_API_KEY"];
  urlStr = [urlStr stringByAppendingFormat:@"key=%@&", DEFAULT_APIKey];

  [self.executeQueue addOperationWithBlock:^{
    [PluginUtil getJsonWithURL:urlStr params:params completionBlock:^(BOOL succeeded, NSDictionary *response, NSString *error) {

      CDVPluginResult* pluginResult;
      if (!succeeded) {
        NSLog(@"[elevation] %@", error);
        pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_ERROR messageAsString:error];
      } else {
        NSMutableDictionary *results = [NSMutableDictionary dictionary];
        [results setObject: [response objectForKey:@"results"] forKey:@"results"];
        pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK messageAsDictionary:results];
      }
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

@end
