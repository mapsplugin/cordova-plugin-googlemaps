//
//  Polyline.m
//  SimpleMap
//
//  Created by masashi on 11/14/13.
//
//

#import "Polyline.h"

@implementation Polyline

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createPolyline:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  
  GMSMutablePath *path = [GMSMutablePath path];
  
  NSArray *points = [json objectForKey:@"points"];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    NSLog(@"%d -> %f,%f", i, [[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue]);
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }

  // Create the Polyline, and assign it to the map.
  GMSPolyline *polyline = [GMSPolyline polylineWithPath:path];

  if ([[json valueForKey:@"visible"] boolValue]) {
    polyline.map = self.mapCtrl.map;
  }
  if ([[json valueForKey:@"geodesic"] boolValue]) {
    polyline.geodesic = YES;
  }
  NSArray *rgbColor = [json valueForKey:@"color"];
  polyline.strokeColor = [rgbColor parsePluginColor];
  
  polyline.strokeWidth = [[json valueForKey:@"width"] floatValue];
  polyline.zIndex = [[json valueForKey:@"zIndex"] floatValue];
  
  NSString *key = [NSString stringWithFormat:@"polyline%d", polyline.hash];
  [self.mapCtrl.overlayManager setObject:polyline forKey: key];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: key];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
