//
//  Geocoder.m
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/29/13.
//
//

#import "Geocoder.h"

@implementation Geocoder

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createGeocoder:(CDVInvokedUrlCommand *)command
{

  if (!self.geocoder) {
    self.geocoder = [[CLGeocoder alloc] init];
  }

  NSDictionary *json = [command.arguments objectAtIndex:1];
  NSLog(@"%@", json);
  NSMutableArray *results = [[NSMutableArray alloc] init];
  
  NSString *address = [json objectForKey:@"address"];
  if (address) {
    [self.geocoder geocodeAddressString:address completionHandler:^(NSArray *placemarks, NSError *error) {
      if ([placemarks count] > 0) {
        
        CLPlacemark *placemark;
        CLLocation *location;
        CLLocationCoordinate2D coordinate = location.coordinate;
        for (int i = 0; i < placemarks.count; i++) {
          NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
          
          NSMutableDictionary *position = [[NSMutableDictionary alloc] init];
          placemark = [placemarks objectAtIndex:i];
          location = placemark.location;
          coordinate = location.coordinate;
          [position setObject:[NSNumber numberWithDouble:coordinate.latitude] forKey:@"lat"];
          [position setObject:[NSNumber numberWithDouble:coordinate.longitude] forKey:@"lng"];
          [result setObject:position forKey:@"position"];
          
          [result setObject:placemark.administrativeArea forKey:@"adminArea"];
          [result setObject:placemark.subAdministrativeArea forKey:@"subAdminArea"];
          [result setObject:placemark.locality forKey:@"locality"];
          [result setObject:placemark.country forKey:@"country"];
          [result setObject:placemark.postalCode forKey:@"postalCode"];
          [result setObject:placemark.subLocality forKey:@"subLocality"];
          [result setObject:placemark.subThoroughfare forKey:@"subThoroughfare"];
          [result setObject:placemark.thoroughfare forKey:@"thoroughfare"];
          
          
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
            [extra setObject:[NSString stringWithFormat:@"%f", placemark.region.radius] forKey:@"radius"];
            
            [extra setObject:placemark.region.identifier forKey:@"identifier"];
          }
          if (placemark.name) {
            [extra setObject:placemark.name forKey:@"name"];
          }
          [result setObject:extra forKey:@"extra"];

          [results addObject:result];
        }
      }
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:results];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      
    }];
  }
}

@end
