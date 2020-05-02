//
//  PluginElevationService.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginDirectionsService.h"

@implementation PluginDirectionsService

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

- (NSString *)_decode_DirectionsRequestLocation: (NSDictionary *)position {
  NSString *positionType = [position objectForKey:@"type"];
  
  if ([@"string" isEqualToString: positionType]) {
    return [[position objectForKey:@"value"] stringValue];
  }
  
  NSDictionary *value = [position objectForKey:@"value"];
  if ([@"location" isEqualToString:positionType]) {
    return [NSString stringWithFormat:@"%f,%f",
              [[value objectForKey:@"lat"] doubleValue],
              [[value objectForKey:@"lng"] doubleValue]
            ];
  }
  if ([value objectForKey:@"placeId"]) {
    return [NSString stringWithFormat:@"place_id:%@",
              [[value objectForKey:@"placeId"] stringValue]
            ];
  }
  
  if ([value objectForKey:@"location"]) {
    NSDictionary *location = [value objectForKey:@"location"];
    return [NSString stringWithFormat:@"%f,%f",
              [[location objectForKey:@"lat"] doubleValue],
              [[location objectForKey:@"lng"] doubleValue]
            ];
  }
  if ([value objectForKey:@"query"]) {
    return [value objectForKey:@"query"];
  }
  return @"";
}

- (void)route:(CDVInvokedUrlCommand*)command {
  NSDictionary *opts = [command.arguments objectAtIndex:0];
  NSLog(@"route = %@", opts);
  

  NSMutableDictionary *params = [NSMutableDictionary dictionary];
  //-----------------------
  // required parameters
  //-----------------------
  [params setObject:[self _decode_DirectionsRequestLocation:[opts objectForKey:@"origin"]] forKey:@"origin"];
  [params setObject:[self _decode_DirectionsRequestLocation:[opts objectForKey:@"destination"]] forKey:@"destination"];
  
  //-----------------------
  // mode parameter
  //-----------------------
  if ([opts objectForKey:@"travelMode"]) {
    // Default: driving
    NSString *travelMode = [opts objectForKey:@"travelMode"];
    travelMode = [travelMode lowercaseString];
    if (![@"driving" isEqualToString:travelMode]) {
      [params setObject:travelMode forKey:@"mode"];
      
      //-----------------------
      // transitOptions parameter
      //-----------------------
      if ([@"transit" isEqualToString:travelMode] &&
          [opts objectForKey:@"transitOptions"]) {
        NSDictionary *transitOptions = [opts objectForKey:@"transitOptions"];
        if ([transitOptions objectForKey:@"arrivalTime"]) {
          [params setObject:[NSString stringWithFormat:@"%@", [transitOptions objectForKey:@"arrivalTime"]] forKey:@"arrival_time"];
        }
        if ([transitOptions objectForKey:@"depatureTime"]) {
          [params setObject:[NSString stringWithFormat:@"%@", [transitOptions objectForKey:@"depatureTime"]] forKey:@"depature_time"];
        }
      }
    } else {
       //-----------------------
       // DrivingOptions parameter
       //-----------------------
       if ([opts objectForKey:@"drivingOptions"]) {
         NSDictionary *drivingOptions = [opts objectForKey:@"drivingOptions"];
         if ([drivingOptions objectForKey:@"depatureTime"]) {
           [params setObject:[NSString stringWithFormat:@"%@", [drivingOptions objectForKey:@"depatureTime"]] forKey:@"depature_time"];
         }
         if ([drivingOptions objectForKey:@"trafficModel"]) {
           NSString *trafficModel = [[[opts objectForKey:@"trafficModel"] stringValue] lowercaseString];
           [params setObject:trafficModel forKey:@"traffic_model"];
         }
       }
    }

    //-------------------
    // transitOptions parameter
    //-------------------
    if ([opts objectForKey:@"transitOptions"]) {
      NSDictionary *transitOptions = [opts objectForKey:@"transitOptions"];
      //-------------------
      // transitOptions.modes parameter
      //-------------------
      if ([transitOptions objectForKey:@"modes"]) {
        NSArray *modes = [transitOptions objectForKey:@"modes"];
        NSString *buffer = @"";
        for (int i = 0; i < modes.count; i++) {
          if (i > 0) {
            buffer = [buffer stringByAppendingString:@"|"];
          }
          buffer = [buffer stringByAppendingString:[[modes objectAtIndex:i] lowercaseString]];
        }
        [params setObject:buffer forKey:@"transit_mode"];
      }
      //-------------------
      // transitOptions.routingPreference parameter
      //-------------------
      if ([transitOptions objectForKey:@"routingPreference"]) {
        NSString *routingPreference = [[[transitOptions objectForKey:@"routingPreference"] stringValue] lowercaseString];
        [params setObject:routingPreference forKey:@"transit_routing_preference"];
      }
    }
  }
  //-------------------
  // waypoints parameter
  //-------------------
  if ([opts objectForKey:@"waypoints"]) {
    NSArray *waypoints = [opts objectForKey:@"waypoints"];
    NSString *buffer = @"";
    int cnt = 0;
    if ([opts objectForKey:@"optimizeWayPoints"]) {
      cnt = 1;
      buffer = @"optimize:true";
    }
    for (int i = 0; i < waypoints.count; i++) {
      NSDictionary *point = [waypoints objectAtIndex:i];
      bool stopOver = false;
      if ([point objectForKey:@"location"]) {
        if ([point objectForKey:@"stopover"]) {
          stopOver = [point objectForKey:@"stopover"];
        }
        if (cnt > 0) {
          buffer = [buffer stringByAppendingString:@"%7C"];
        }
        if (!stopOver) {
          buffer = [buffer stringByAppendingString:@"via:"];
        }
        buffer = [buffer stringByAppendingString:[self _decode_DirectionsRequestLocation:[point objectForKey:@"location"]]];
        cnt++;
      }
    }
    if (cnt > 0) {
      [params setObject:buffer forKey:@"waypoints"];
    }
  }
  //-------------------
  // alternatives parameter
  //-------------------
  if ([opts objectForKey:@"provideRouteAlternatives"]) {
    [params setObject:@"true" forKey:@"alternatives"];
  }
  
  //-------------------
  // avoid parameter
  //-------------------
  bool avoidFerries = false;
  bool avoidHighways = false;
  bool avoidTolls = false;
  if ([opts objectForKey:@"avoideFerries"]) {
    avoidFerries = [[opts objectForKey:@"avoideFerries"] boolValue];
  }
  if ([opts objectForKey:@"avoidHighways"]) {
    avoidHighways = [[opts objectForKey:@"avoidHighways"] boolValue];
  }
  if ([opts objectForKey:@"avoidTolls"]) {
    avoidTolls = [[opts objectForKey:@"avoidTolls"] boolValue];
  }
  if (avoidFerries || avoidHighways || avoidTolls) {
    NSString *buffer = @"";
    int cnt = 0;
    if (avoidFerries) {
      buffer = @"ferries";
      cnt = 1;
    }
    if (avoidTolls) {
      if (cnt > 0) {
        buffer = [buffer stringByAppendingString:@"|"];
      }
      buffer = [buffer stringByAppendingString:@"tolls"];
      cnt++;
    }
    if (avoidHighways) {
      if (cnt > 0) {
        buffer = [buffer stringByAppendingString:@"|"];
      }
      buffer = [buffer stringByAppendingString:@"highways"];
    }
    [params setObject:buffer forKey:@"avoid"];
  }
  
  //-------------------
  // language parameter
  //-------------------
  NSArray<NSString *> *preferredLanguages = [NSLocale preferredLanguages];
  NSString *localeCode = [preferredLanguages objectAtIndex:0];
  NSString *languageCode = [[localeCode componentsSeparatedByString:@"-"] firstObject];
  [params setObject:languageCode forKey:@"language"];

  //-------------------
  // units parameter
  //-------------------
  if ([opts objectForKey:@"unitSystem"]) {
    [params setObject:[[[opts objectForKey:@"unitSystem"] stringValue] lowercaseString] forKey:@"units"];
//  } else if ([@"en_US" isEqualToString:localeCode]) {
//    [params setObject:@"imperial" forKey:@"units"];
//  } else {
//    [params setObject:@"metric" forKey:@"units"];
  }
  
  //-------------------
  // region parameter
  //-------------------
  if ([opts objectForKey:@"region"]) {
    [params setObject:[[[opts objectForKey:@"region"] stringValue] lowercaseString] forKey:@"region"];
  }
  
  [self httpGetWithDictionary:params command:command];
}

- (void)httpGetWithDictionary:(NSDictionary *)params command:(CDVInvokedUrlCommand *)command
{
  
  NSString *urlStr = @"https://maps.googleapis.com/maps/api/directions/json?";
  
  NSUserDefaults *myDefaults = [[NSUserDefaults alloc] initWithSuiteName:@"cordova.plugin.googlemaps"];
  NSString *DEFAULT_APIKey = [myDefaults objectForKey:@"GOOGLE_MAPS_API_KEY"];
  urlStr = [urlStr stringByAppendingFormat:@"key=%@&", DEFAULT_APIKey];

  [self.executeQueue addOperationWithBlock:^{
    [PluginUtil getJsonWithURL:urlStr params:params completionBlock:^(BOOL succeeded, NSDictionary *result, NSString *error) {

      CDVPluginResult* pluginResult;
      if (!succeeded) {
        NSLog(@"[directions] %@", error);
        pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_ERROR messageAsString:error];
      } else {
        pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK messageAsDictionary:result];
      }
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

@end
