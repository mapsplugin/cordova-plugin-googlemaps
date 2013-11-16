//
//  Polygon.m
//  SimpleMap
//
//  Created by masashi on 11/13/13.
//
//

#import "Polygon.h"

@implementation Polygon

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createPolygon:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  
  GMSMutablePath *path = [GMSMutablePath path];
  
  NSArray *points = [json objectForKey:@"points"];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }

  // Create the polygon, and assign it to the map.
  GMSPolygon *polygon = [GMSPolygon polygonWithPath:path];

  if ([[json valueForKey:@"visible"] boolValue]) {
    polygon.map = self.mapCtrl.map;
  }
  if ([[json valueForKey:@"geodesic"] boolValue]) {
    polygon.geodesic = YES;
  }
  NSArray *rgbColor = [json valueForKey:@"fillColor"];
  polygon.fillColor = [rgbColor parsePluginColor];
  
  rgbColor = [json valueForKey:@"strokeColor"];
  polygon.strokeColor = [rgbColor parsePluginColor];
  
  polygon.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
  polygon.zIndex = [[json valueForKey:@"zIndex"] floatValue];
  
  NSString *key = [NSString stringWithFormat:@"polygon%d", polygon.hash];
  [self.mapCtrl.overlayManager setObject:polygon forKey: key];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: key];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
