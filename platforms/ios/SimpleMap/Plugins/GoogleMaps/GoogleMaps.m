//
//  GoogleMaps.m
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import "GoogleMaps.h"

@implementation GoogleMaps

GoogleMapsViewController *mapCtrl;

- (void)GoogleMap_getMap:(CDVInvokedUrlCommand *)command {
  // Create a map view
  mapCtrl = [[GoogleMapsViewController alloc] init];
  mapCtrl.webView = self.webView;
  
  // Create a close button
  CGRect screenSize = [[UIScreen mainScreen] bounds];
  CGRect pluginRect = CGRectMake(screenSize.size.width * 0.05, screenSize.size.height * 0.05, screenSize.size.width * 0.9, screenSize.size.height * 0.9);
  UIButton *closeButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
  closeButton.frame = CGRectMake(0, pluginRect.size.height * 0.9, 50, 30);
  [closeButton setTitle:@"Close" forState:UIControlStateNormal];
  [closeButton addTarget:self action:@selector(onCloseBtn_clicked:) forControlEvents:UIControlEventTouchDown];
  [mapCtrl.view addSubview:closeButton];
  
  
  CDVPluginResult* pluginResult = nil;
  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Close the map window
 */
- (void)onCloseBtn_clicked:(UIButton*)button{
  [mapCtrl.view removeFromSuperview];
}

/**
 * Show the map window
 */
- (void)GoogleMap_show:(CDVInvokedUrlCommand *)command {
    [self.webView addSubview:mapCtrl.view];
  
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Move the center of the map
 */
- (void)GoogleMap_setCenter:(CDVInvokedUrlCommand *)command {

    float latitude = [[command.arguments objectAtIndex:0] floatValue];
    float longitude = [[command.arguments objectAtIndex:1] floatValue];
  
    [mapCtrl.map animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.settings.myLocationButton = isEnabled;
    mapCtrl.map.myLocationEnabled = isEnabled;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setIndoorEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.settings.indoorPicker = isEnabled;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setTrafficEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.trafficEnabled = isEnabled;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setCompassEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.settings.compassButton = isEnabled;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setTilt:(CDVInvokedUrlCommand *)command {
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the zoom level
 */
- (void)GoogleMap_setZoom:(CDVInvokedUrlCommand *)command {
    float zoom = [[command.arguments objectAtIndex:0] floatValue];
    CLLocationCoordinate2D center = [mapCtrl.map.projection coordinateForPoint:mapCtrl.map.center];
  
    [mapCtrl.map setCamera:[GMSCameraPosition cameraWithTarget:center zoom:zoom]];
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the Map Type
 */
- (void)GoogleMap_setMapTypeId:(CDVInvokedUrlCommand *)command {
    NSString *typeStr = [command.arguments objectAtIndex:0];
    NSDictionary *mapTypes = [NSDictionary dictionaryWithObjectsAndKeys:
      ^() {return kGMSTypeHybrid; }, @"MAP_TYPE_HYBRID",
      ^() {return kGMSTypeSatellite; }, @"MAP_TYPE_SATELLITE",
      ^() {return kGMSTypeTerrain; }, @"MAP_TYPE_TERRAIN",
      ^() {return kGMSTypeNormal; }, @"MAP_TYPE_NORMAL",
      ^() {return kGMSTypeNone; }, @"MAP_TYPE_NONE",
      nil];
  
    typedef GMSMapViewType (^CaseBlock)();
    GMSMapViewType mapType = kGMSTypeNormal;
    CaseBlock c = mapTypes[typeStr];
    if (c) {
      mapType = c();
    }
    mapCtrl.map.mapType = mapType;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
