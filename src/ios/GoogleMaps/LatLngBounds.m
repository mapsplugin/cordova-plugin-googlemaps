//
//  LatLngBounds.m
//  SimpleMap
//
//  Created by masashi on 11/14/13.
//
//

#import "LatLngBounds.h"

@implementation LatLngBounds

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)contains:(CDVInvokedUrlCommand *)command
{
  GMSMutablePath *path = [GMSMutablePath path];
  NSArray *points = [command.arguments objectAtIndex:1];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }
  
  NSDictionary *latLngJSON = [command.arguments objectAtIndex:2];
  float latitude = [[latLngJSON objectForKey:@"lat"] floatValue];
  float longitude = [[latLngJSON objectForKey:@"lng"] floatValue];
  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
  
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithPath:path];
  BOOL isContain = [bounds containsCoordinate:position];
  NSString *isContainStr = @"false";
  if (isContain) {
    isContainStr = @"true";
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:isContainStr];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
