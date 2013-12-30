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
  NSDictionary *json = [command.arguments objectAtIndex:1];
  NSDictionary *position = [json objectForKey:@"position"];
  NSString *address = [json objectForKey:@"address"];

  if (!self.geocoder) {
    self.geocoder = [[CLGeocoder alloc] init];
  }
  if (address && position == nil) {
  
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
      
      
      [self.geocoder geocodeAddressString:address inRegion:region completionHandler:^(NSArray *placemarks, NSError *error) {
        CDVPluginResult* pluginResult;
        if (error) {
          if (placemarks.count == 0) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not found"];
          } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
          }
        } else {
          NSArray *results = [self geocoder_callback:placemarks error:error];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:results];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

    } else {
      //No region specified.
      [self.geocoder geocodeAddressString:address completionHandler:^(NSArray *placemarks, NSError *error) {
        CDVPluginResult* pluginResult;
        if (error) {
          if (placemarks.count == 0) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not found"];
          } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
          }
        } else {
          NSArray *results = [self geocoder_callback:placemarks error:error];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:results];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
    }
    return;
  }
  
  // Reverse geocoding
  if (position && address == nil) {
    
    NSDictionary *latLng = [json objectForKey:@"position"];
    double latitude = [[latLng valueForKey:@"lat"] doubleValue];
    double longitude = [[latLng valueForKey:@"lng"] doubleValue];
    CLLocation *position = [[CLLocation alloc] initWithLatitude:latitude longitude:longitude];
    
    [self.geocoder reverseGeocodeLocation:position completionHandler:^(NSArray *placemarks, NSError *error) {
      CDVPluginResult* pluginResult;
      if (error) {
        if (placemarks.count == 0) {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not found"];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
        }
      } else {
        NSArray *results = [self geocoder_callback:placemarks error:error];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:results];
      }
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }
}

- (NSArray *)geocoder_callback:(NSArray *)placemarks error:(NSError *)error
{
  NSMutableArray *results = [[NSMutableArray alloc] init];
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
  return results;
  
}

@end
