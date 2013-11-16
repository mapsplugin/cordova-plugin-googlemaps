//
//  Map.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "Map.h"

@implementation Map


-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

/**
 * Move the center of the map
 */
- (void)setCenter:(CDVInvokedUrlCommand *)command {
  
  float latitude = [[command.arguments objectAtIndex:1] floatValue];
  float longitude = [[command.arguments objectAtIndex:2] floatValue];
  
  [self.mapCtrl.map animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.mapCtrl.map.settings.myLocationButton = isEnabled;
  self.mapCtrl.map.myLocationEnabled = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setIndoorEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.mapCtrl.map.settings.indoorPicker = isEnabled;
  self.mapCtrl.map.indoorEnabled = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTrafficEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.mapCtrl.map.trafficEnabled = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCompassEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.mapCtrl.map.settings.compassButton = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTilt:(CDVInvokedUrlCommand *)command {
  
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the zoom level
 */
- (void)setZoom:(CDVInvokedUrlCommand *)command {
  float zoom = [[command.arguments objectAtIndex:1] floatValue];
  CLLocationCoordinate2D center = [self.mapCtrl.map.projection coordinateForPoint:self.mapCtrl.map.center];
  
  [self.mapCtrl.map setCamera:[GMSCameraPosition cameraWithTarget:center zoom:zoom]];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the Map Type
 */
- (void)setMapTypeId:(CDVInvokedUrlCommand *)command {
  CDVPluginResult* pluginResult = nil;
  
  NSString *typeStr = [command.arguments objectAtIndex:1];
  NSDictionary *mapTypes = [NSDictionary dictionaryWithObjectsAndKeys:
                            ^() {return kGMSTypeHybrid; }, @"MAP_TYPE_HYBRID",
                            ^() {return kGMSTypeSatellite; }, @"MAP_TYPE_SATELLITE",
                            ^() {return kGMSTypeTerrain; }, @"MAP_TYPE_TERRAIN",
                            ^() {return kGMSTypeNormal; }, @"MAP_TYPE_NORMAL",
                            ^() {return kGMSTypeNone; }, @"MAP_TYPE_NONE",
                            nil];
  
  typedef GMSMapViewType (^CaseBlock)();
  GMSMapViewType mapType;
  CaseBlock caseBlock = mapTypes[typeStr];
  if (caseBlock) {
    // Change the map type
    mapType = caseBlock();
    self.mapCtrl.map.mapType = mapType;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  } else {
    // Error : User specifies unknow map type id
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                     messageAsString:[NSString
                                                      stringWithFormat:@"Unknow MapTypeID is specified:%@", typeStr]];
  }
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Move the map camera with animation
 */
-(void)animateCamera:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  
  NSLog(@"AnimateCamera");
  float latitude = [[json valueForKey:@"lat"] floatValue];
  float longitude = [[json valueForKey:@"lng"] floatValue];
  int bearing = [[json valueForKey:@"bearing"] integerValue];
  double angle = [[json valueForKey:@"tilt"] doubleValue];
  int zoom = [[json valueForKey:@"zoom"] integerValue];
  
  float duration = 1.0f;
  if (command.arguments.count == 3) {
    duration = [[command.arguments objectAtIndex:2] floatValue] / 1000;
  }
  
  GMSCameraPosition *cameraPosition = [GMSCameraPosition cameraWithLatitude:latitude
                                                                  longitude:longitude
                                                                       zoom:zoom
                                                                    bearing:bearing
                                                               viewingAngle:angle];
  
  [CATransaction begin]; {
    [CATransaction setAnimationDuration: duration];
    
    [CATransaction setCompletionBlock:^{
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
    
    [self.mapCtrl.map animateToCameraPosition: cameraPosition];
  }[CATransaction commit];
}

/**
 * Move the map camera
 */
-(void)moveCamera:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  
  float latitude = [[json valueForKey:@"lat"] floatValue];
  float longitude = [[json valueForKey:@"lng"] floatValue];
  int bearing = [[json valueForKey:@"bearing"] integerValue];
  double angle = [[json valueForKey:@"tilt"] doubleValue];
  int zoom = [[json valueForKey:@"zoom"] integerValue];
  
  float duration = 1.0f;
  if (command.arguments.count == 3) {
    duration = [[command.arguments objectAtIndex:2] floatValue] / 1000;
  }
  
  GMSCameraPosition *cameraPosition = [GMSCameraPosition cameraWithLatitude:latitude
                                                                  longitude:longitude
                                                                       zoom:zoom
                                                                    bearing:bearing
                                                               viewingAngle:angle];
  
  [self.mapCtrl.map setCamera:cameraPosition];
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void)getCameraPosition:(CDVInvokedUrlCommand *)command
{
  GMSCameraPosition *camera = self.mapCtrl.map.camera;
  
  NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
  [latLng setObject:[NSNumber numberWithFloat:camera.target.latitude] forKey:@"lat"];
  [latLng setObject:[NSNumber numberWithFloat:camera.target.longitude] forKey:@"lng"];
  
  NSMutableDictionary *json = [NSMutableDictionary dictionary];
  [json setObject:[NSNumber numberWithFloat:camera.zoom] forKey:@"zoom"];
  [json setObject:[NSNumber numberWithDouble:camera.viewingAngle] forKey:@"tilt"];
  [json setObject:latLng forKey:@"target"];
  [json setObject:[NSNumber numberWithFloat:camera.bearing] forKey:@"bearing"];
  [json setObject:[NSNumber numberWithInt:camera.hash] forKey:@"hashCode"];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}



@end
