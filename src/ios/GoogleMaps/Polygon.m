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
  NSString *idPrefix = @"";
  if ([command.arguments count] == 3) {
    idPrefix = [command.arguments objectAtIndex:2];
  }
  
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
    polygon.geodesic = true;
  }
  NSArray *rgbColor = [json valueForKey:@"fillColor"];
  polygon.fillColor = [rgbColor parsePluginColor];
  
  rgbColor = [json valueForKey:@"strokeColor"];
  polygon.strokeColor = [rgbColor parsePluginColor];
  
  polygon.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
  polygon.zIndex = [[json valueForKey:@"zIndex"] floatValue];
  
  NSString *key = [NSString stringWithFormat:@"%@polygon%d", idPrefix, polygon.hash];
  [self.mapCtrl.overlayManager setObject:polygon forKey: key];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: key];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set points
 * @params key
 */
-(void)setPoints:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  GMSMutablePath *path = [GMSMutablePath path];
  
  NSArray *points = [command.arguments objectAtIndex:2];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }
  [polygon setPath:path];
  
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set fill color
 * @params key
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
 
  NSArray *rgbColor = [command.arguments objectAtIndex:2];
  [polygon setFillColor:[rgbColor parsePluginColor]];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set stroke color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
 
  NSArray *rgbColor = [command.arguments objectAtIndex:2];
  [polygon setStrokeColor:[rgbColor parsePluginColor]];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set stroke width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  float width = [[command.arguments objectAtIndex:2] floatValue];
  [polygon setStrokeWidth:width];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
  [polygon setZIndex:zIndex];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
  if (isVisible) {
    polygon.map = self.mapCtrl.map;
  } else {
    polygon.map = nil;
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
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  Boolean isGeodisic = [[command.arguments objectAtIndex:2] boolValue];
  [polygon setGeodesic:isGeodisic];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the polygon
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  polygon.map = nil;
  [self.mapCtrl removeObjectForKey:polygonKey];
  polygon = nil;
    
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
