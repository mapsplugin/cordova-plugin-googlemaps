//
//  Geocoder.m
//  cordova-googlemaps-plugin v2
//
//  Created by Katsumata Masashi.
//
//

#import "PluginGeocoder.h"

@implementation PluginGeocoder

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
  NSArray *countryCodes = [NSLocale ISOCountryCodes];
  NSMutableArray *countries = [NSMutableArray arrayWithCapacity:[countryCodes count]];
  NSString *currentLanguage = [[NSLocale preferredLanguages] objectAtIndex:0];

  for (NSString *countryCode in countryCodes)
  {
      NSString *identifier = [NSLocale localeIdentifierFromComponents: [NSDictionary dictionaryWithObject: countryCode forKey: NSLocaleCountryCode]];
      NSString *country = [[[NSLocale alloc] initWithLocaleIdentifier:currentLanguage] displayNameForKey: NSLocaleIdentifier value: identifier];
      [countries addObject: country];
  }

  self.codeForCountryDictionary = [[NSDictionary alloc] initWithObjects:countryCodes forKeys:countries];
  self.executeQueue =  [NSOperationQueue new];
  self.executeQueue.maxConcurrentOperationCount = 6;
  
}

-(void)geocode:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{
      NSDictionary *json = [command.arguments objectAtIndex:0];
      NSDictionary *position = [json objectForKey:@"position"];
      NSString *address = [json objectForKey:@"address"];
      int idx = 0;
    
      if ([json objectForKey:@"idx"]) {
          idx = [[json objectForKey:@"idx"] integerValue];
      }

      if (address && position == nil) {
          __block CLGeocoder *geocoder = [CLGeocoder new];

          NSArray *points = [json objectForKey:@"bounds"];

          if (points) {
              //center
              int i = 0;
              NSDictionary *latLng;
              GMSMutablePath *path = [GMSMutablePath path];
              GMSCoordinateBounds *bounds;
              for (i = 0; i < points.count; i++) {
                  latLng = [points objectAtIndex:i];
                  [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
              }
              bounds = [[GMSCoordinateBounds alloc] initWithPath:path];


              CLLocationCoordinate2D southWest = bounds.southWest;
              CLLocationCoordinate2D northEast = bounds.northEast;
              float latitude = (southWest.latitude + northEast.latitude) / 2.0;
              float longitude = (southWest.longitude + northEast.longitude) / 2.0;
              CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);

              //distance
              CLLocation *locA = [[CLLocation alloc] initWithLatitude:center.latitude longitude:center.latitude];
              CLLocation *locB = [[CLLocation alloc] initWithLatitude:southWest.latitude longitude:southWest.longitude];
              CLLocationDistance distance = [locA distanceFromLocation:locB];

              CLCircularRegion *region =  [[CLCircularRegion alloc] initWithCenter:center radius:distance/2 identifier:@"geocoder"];


              [geocoder geocodeAddressString:address inRegion:region completionHandler:^(NSArray *placemarks, NSError *error) {

                  CDVPluginResult* pluginResult;
                  
                  if (error) {
                      if (placemarks.count == 0) {
                          NSMutableDictionary *resultJson = [[NSMutableDictionary alloc] init];
                          [resultJson setObject:[NSNumber numberWithInt:idx] forKey:@"idx"];
                          [resultJson setObject:[[NSArray alloc] init] forKey:@"results"];
                          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultJson];
                        
                      } else {
                          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
                      }
                  } else {
                      NSArray *results = [self geocoder_callback:placemarks error:error];
                      NSMutableDictionary *resultJson = [[NSMutableDictionary alloc] init];
                      [resultJson setObject:[NSNumber numberWithInt:idx] forKey:@"idx"];
                      [resultJson setObject:results forKey:@"results"];
                      
                      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultJson];
                  }
                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
              }];

          } else {
              //No region specified.
              [geocoder geocodeAddressString:address completionHandler:^(NSArray *placemarks, NSError *error) {
                  CDVPluginResult* pluginResult;
                  if (error) {
                      if (placemarks.count == 0) {
                          NSMutableDictionary *resultJson = [[NSMutableDictionary alloc] init];
                          [resultJson setObject:[NSNumber numberWithInt:idx] forKey:@"idx"];
                          [resultJson setObject:[[NSArray alloc] init] forKey:@"results"];
                          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultJson];
                        
                      } else {
                          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
                      }
                  } else {
                      NSArray *results = [self geocoder_callback:placemarks error:error];
                      NSMutableDictionary *resultJson = [[NSMutableDictionary alloc] init];
                      [resultJson setObject:[NSNumber numberWithInt:idx] forKey:@"idx"];
                      [resultJson setObject:results forKey:@"results"];
                      
                      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultJson];
                  }
                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
              }];
          }
          return;
      }

      // Reverse geocoding
      if (position && address == nil) {

          [[NSOperationQueue mainQueue] addOperationWithBlock:^{
              GMSGeocoder *reverseGeocoder = [GMSGeocoder geocoder];

              NSDictionary *latLng = [json objectForKey:@"position"];
              CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue]);

              [reverseGeocoder reverseGeocodeCoordinate:position completionHandler:^(GMSReverseGeocodeResponse *response, NSError *error) {
                
                  [self.executeQueue addOperationWithBlock:^{
                      CDVPluginResult* pluginResult;
                      if (error) {
                          if ([response.results count] == 0) {
                              NSMutableDictionary *resultJson = [[NSMutableDictionary alloc] init];
                              [resultJson setObject:[json objectForKey:@"idx"] forKey:@"idx"];
                              [resultJson setObject:[[NSArray alloc] init] forKey:@"results"];
                              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultJson];
                          } else {
                              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
                          }
                      } else {

                          NSMutableArray *results = [NSMutableArray array];
                          GMSAddress *address;
                          NSString *countryCode;
                          int i;
                          for (i = 0; i < [response.results count]; i++) {
                              address = [response.results objectAtIndex:i];

                              NSMutableDictionary *result = [NSMutableDictionary dictionary];
                              [result setObject:[NSNumber numberWithDouble:address.coordinate.latitude] forKey:@"lat"];
                              [result setObject:[NSNumber numberWithDouble:address.coordinate.longitude] forKey:@"lng"];

                              NSMutableDictionary *position = [NSMutableDictionary dictionary];
                              [position setObject:[NSNumber numberWithDouble:address.coordinate.latitude] forKey:@"lat"];
                              [position setObject:[NSNumber numberWithDouble:address.coordinate.longitude] forKey:@"lng"];
                              [result setObject:position forKey:@"position"];

                              [result setObject:[NSString stringWithFormat:@"%@", address.locality] forKey:@"locality"];
                              [result setObject:[NSString stringWithFormat:@"%@", address.administrativeArea] forKey:@"adminArea"];
                              [result setObject:[NSString stringWithFormat:@"%@", address.country] forKey:@"country"];
                              countryCode = [self.codeForCountryDictionary objectForKey:address.country];
                              [result setObject:[NSString stringWithFormat:@"%@", countryCode] forKey:@"countryCode"];
                              [result setObject:@"" forKey:@"locale"];
                              [result setObject:[NSString stringWithFormat:@"%@", address.postalCode] forKey:@"postalCode"];
                              [result setObject:@"" forKey:@"subAdminArea"];
                              [result setObject:[NSString stringWithFormat:@"%@", address.subLocality] forKey:@"subLocality"];
                              [result setObject:@"" forKey:@"subThoroughfare"];
                              [result setObject:[NSString stringWithFormat:@"%@", address.thoroughfare] forKey:@"thoroughfare"];


                              NSMutableDictionary *extra = [NSMutableDictionary dictionary];
                              [extra setObject:address.lines forKey:@"lines"];
                              [result setObject:extra forKey:@"extra"];

                              [results addObject:result];
                          }
                          NSMutableDictionary *resultJson = [[NSMutableDictionary alloc] init];
                          [resultJson setObject:[json objectForKey:@"idx"] forKey:@"idx"];
                          [resultJson setObject:results forKey:@"results"];
                          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultJson];
                      }
                      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                  }];
              }];
          }];
      }
  }];
}

- (NSArray *)geocoder_callback:(NSArray *)placemarks error:(NSError *)error
{
  NSMutableArray *results = [[NSMutableArray alloc] init];
  if ([placemarks count] > 0) {

    CLPlacemark *placemark;
    CLLocation *location;
    CLLocationCoordinate2D coordinate;
    for (int i = 0; i < placemarks.count; i++) {
      NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

      NSMutableDictionary *position = [[NSMutableDictionary alloc] init];
      placemark = [placemarks objectAtIndex:i];
      location = placemark.location;
      coordinate = location.coordinate;
      [position setObject:[NSNumber numberWithDouble:coordinate.latitude] forKey:@"lat"];
      [position setObject:[NSNumber numberWithDouble:coordinate.longitude] forKey:@"lng"];
      [result setObject:position forKey:@"position"];


      if (placemark.administrativeArea) {
        [result setObject:placemark.administrativeArea forKey:@"adminArea"];
      }
      if (placemark.subAdministrativeArea) {
        [result setObject:placemark.subAdministrativeArea forKey:@"subAdminArea"];
      }

      if (placemark.locality) {
        [result setObject:placemark.locality forKey:@"locality"];
      }

      if (placemark.country) {
        [result setObject:placemark.country forKey:@"country"];
      }
      if (placemark.postalCode) {
        [result setObject:placemark.postalCode forKey:@"postalCode"];
      }
      if (placemark.subLocality) {
        [result setObject:placemark.subLocality forKey:@"subLocality"];
      }
      if (placemark.subThoroughfare) {
        [result setObject:placemark.subThoroughfare forKey:@"subThoroughfare"];
      }
      if (placemark.thoroughfare) {
        [result setObject:placemark.thoroughfare forKey:@"thoroughfare"];
      }

      NSMutableDictionary *extra = [[NSMutableDictionary alloc] init];
      if (placemark.ocean) {
        [extra setObject:placemark.ocean forKey:@"ocean"];
      }
      if (placemark.addressDictionary) {
        [extra setObject:placemark.addressDictionary forKey:@"address"];
      }
      if (placemark.description) {
        [extra setObject:placemark.description forKey:@"description"];
      }
      if (placemark.inlandWater) {
        [extra setObject:placemark.inlandWater forKey:@"inlandWater"];
      }
      if (placemark.region) {
        // iOS8 SDK still uses CLRegion inside CLPlaceMarker though,
        // to supress the warning, cast to CLCircularRegion.
        // http://www.4byte.cn/question/288833/deprecated-clregion-methods-how-to-get-radius.html
        #ifdef __IPHONE_7_0
          CLCircularRegion *circularRegion = (CLCircularRegion *)placemark.region;
          [extra setObject:[NSString stringWithFormat:@"%f", circularRegion.radius] forKey:@"radius"];
        #else
          [extra setObject:[NSString stringWithFormat:@"%f", placemark.region.radius] forKey:@"radius"];
        #endif


        [extra setObject:placemark.region.identifier forKey:@"identifier"];
      }
      if (placemark.name) {
        [extra setObject:placemark.name forKey:@"name"];
      }
      [result setObject:extra forKey:@"extra"];

      [results addObject:result];
    }
  }
  return results;

}

@end
