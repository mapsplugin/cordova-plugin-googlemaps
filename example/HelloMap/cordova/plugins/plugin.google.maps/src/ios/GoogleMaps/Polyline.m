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



/**
 * Set points
 * @params key
 */
-(void)setPoints:(CDVInvokedUrlCommand *)command
{
  NSString *polylineKey = [command.arguments objectAtIndex:1];
  GMSPolyline *polyline = [self.mapCtrl getPolylineByKey: polylineKey];
  GMSMutablePath *path = [GMSMutablePath path];
  
  NSArray *points = [command.arguments objectAtIndex:2];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }
  [polyline setPath:path];
  
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set color
 * @params key
 */
-(void)setColor:(CDVInvokedUrlCommand *)command
{
  NSString *polylineKey = [command.arguments objectAtIndex:1];
  GMSPolyline *polyline = [self.mapCtrl getPolylineByKey: polylineKey];
 
  NSArray *rgbColor = [command.arguments objectAtIndex:2];
  [polyline setStrokeColor:[rgbColor parsePluginColor]];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set width
 * @params key
 */
-(void)setWidth:(CDVInvokedUrlCommand *)command
{
  NSString *polylineKey = [command.arguments objectAtIndex:1];
  GMSPolyline *polyline = [self.mapCtrl getPolylineByKey: polylineKey];
  float width = [[command.arguments objectAtIndex:2] floatValue];
  [polyline setStrokeWidth:width];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *polylineKey = [command.arguments objectAtIndex:1];
  GMSPolyline *polyline = [self.mapCtrl getPolylineByKey: polylineKey];
  NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
  [polyline setZIndex:zIndex];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *polylineKey = [command.arguments objectAtIndex:1];
  GMSPolyline *polyline = [self.mapCtrl getPolylineByKey: polylineKey];
  Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
  if (isVisible) {
    polyline.map = self.mapCtrl.map;
  } else {
    polyline.map = nil;
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set geodesic
 * @params key
 */
-(void)setGeodesic:(CDVInvokedUrlCommand *)command
{
  NSString *polylineKey = [command.arguments objectAtIndex:1];
  GMSPolyline *polyline = [self.mapCtrl getPolylineByKey: polylineKey];
  Boolean isGeodisic = [[command.arguments objectAtIndex:2] boolValue];
  [polyline setGeodesic:isGeodisic];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the polyline
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *polylineKey = [command.arguments objectAtIndex:1];
  GMSPolyline *polyline = [self.mapCtrl getPolylineByKey: polylineKey];
  polyline.map = nil;
  [self.mapCtrl removeObjectForKey:polylineKey];
  polyline = nil;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
