//
//  Circle.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "Circle.h"

@implementation Circle

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}
-(void)createCircle:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  NSDictionary *latLng = [json objectForKey:@"center"];
  float latitude = [[latLng valueForKey:@"lat"] floatValue];
  float longitude = [[latLng valueForKey:@"lng"] floatValue];

  float radius = [[json valueForKey:@"radius"] floatValue];
  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
  GMSCircle *circle = [GMSCircle circleWithPosition:position radius:radius];

  if ([[json valueForKey:@"visible"] boolValue]) {
    circle.map = self.mapCtrl.map;
  }
  NSArray *rgbColor = [json valueForKey:@"fillColor"];
  circle.fillColor = [rgbColor parsePluginColor];

  rgbColor = [json valueForKey:@"strokeColor"];
  circle.strokeColor = [rgbColor parsePluginColor];

  circle.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
  circle.zIndex = [[json valueForKey:@"zIndex"] floatValue];

  circle.tappable = YES;
  
  NSString *id = [NSString stringWithFormat:@"circle_%lu", (unsigned long)circle.hash];
  [self.mapCtrl.overlayManager setObject:circle forKey: id];
  circle.title = id;

  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:id forKey:@"id"];
  [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)circle.hash] forKey:@"hashCode"];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set center
 * @params key
 */
-(void)setCenter:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];

  float latitude = [[command.arguments objectAtIndex:2] floatValue];
  float longitude = [[command.arguments objectAtIndex:3] floatValue];
  CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
  [circle setPosition:center];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set fill color
 * @params key
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];

  NSArray *rgbColor = [command.arguments objectAtIndex:2];
  [circle setFillColor:[rgbColor parsePluginColor]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set stroke color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];

  NSArray *rgbColor = [command.arguments objectAtIndex:2];
  [circle setStrokeColor:[rgbColor parsePluginColor]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set stroke width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  float width = [[command.arguments objectAtIndex:2] floatValue];
  [circle setStrokeWidth:width];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set radius
 * @params key
 */
-(void)setRadius:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  float radius = [[command.arguments objectAtIndex:2] floatValue];
  [circle setRadius:radius];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
  [circle setZIndex:(int)zIndex];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
  if (isVisible) {
    circle.map = self.mapCtrl.map;
  } else {
    circle.map = nil;
  }

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the circle
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  circle.map = nil;
  [self.mapCtrl removeObjectForKey:circleKey];
  circle = nil;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end

