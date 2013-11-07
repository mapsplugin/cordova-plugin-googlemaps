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
  dispatch_queue_t gueue = dispatch_queue_create("GoogleMap_getMap", NULL);
  
  // Create a map view
  dispatch_sync(gueue, ^{
    mapCtrl = [[GoogleMapsViewController alloc] init];
    mapCtrl.webView = self.webView;
  });
  
  // Create a close button
  dispatch_sync(gueue, ^{
    CGRect screenSize = [[UIScreen mainScreen] bounds];
    CGRect pluginRect = CGRectMake(screenSize.size.width * 0.05, screenSize.size.height * 0.05, screenSize.size.width * 0.9, screenSize.size.height * 0.9);
    UIButton *closeButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    closeButton.frame = CGRectMake(0, pluginRect.size.height * 0.9, 50, 30);
    [closeButton setTitle:@"Close" forState:UIControlStateNormal];
    [closeButton addTarget:self action:@selector(onCloseBtn_clicked:) forControlEvents:UIControlEventTouchDown];
    [mapCtrl.view addSubview:closeButton];
  });
  dispatch_release(gueue);
  
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
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
  
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Move the center of the map
 */
- (void)GoogleMap_setCenter:(CDVInvokedUrlCommand *)command {

    float latitude = [[command.arguments objectAtIndex:0] floatValue];
    float longitude = [[command.arguments objectAtIndex:1] floatValue];
  
    [mapCtrl.map animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.settings.myLocationButton = isEnabled;
    mapCtrl.map.myLocationEnabled = isEnabled;
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setIndoorEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.settings.indoorPicker = isEnabled;
    mapCtrl.map.indoorEnabled = isEnabled;
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setTrafficEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.trafficEnabled = isEnabled;
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setCompassEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
    mapCtrl.map.settings.compassButton = isEnabled;
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setTilt:(CDVInvokedUrlCommand *)command {

  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the zoom level
 */
- (void)GoogleMap_setZoom:(CDVInvokedUrlCommand *)command {
    float zoom = [[command.arguments objectAtIndex:0] floatValue];
    CLLocationCoordinate2D center = [mapCtrl.map.projection coordinateForPoint:mapCtrl.map.center];
  
    [mapCtrl.map setCamera:[GMSCameraPosition cameraWithTarget:center zoom:zoom]];
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the Map Type
 */
- (void)GoogleMap_setMapTypeId:(CDVInvokedUrlCommand *)command {
    CDVPluginResult* pluginResult = nil;

    NSString *typeStr = [command.arguments objectAtIndex:0];
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
      mapCtrl.map.mapType = mapType;
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
-(void)GoogleMap_animateCamera:(CDVInvokedUrlCommand *)command
{
    NSDictionary *json = [command.arguments objectAtIndex:0];
  
    float latitude = [[json valueForKey:@"lat"] floatValue];
    float longitude = [[json valueForKey:@"lng"] floatValue];
    int bearing = [[json valueForKey:@"bearing"] integerValue];
    double angle = [[json valueForKey:@"tilt"] doubleValue];
    int zoom = [[json valueForKey:@"zoom"] integerValue];
  
    float duration = 1.0f;
    if (command.arguments.count == 2) {
      duration = [[command.arguments objectAtIndex:1] floatValue] / 1000;
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
      
      [mapCtrl.map animateToCameraPosition: cameraPosition];
    }[CATransaction commit];
}

/**
 * Move the map camera
 */
-(void)GoogleMap_moveCamera:(CDVInvokedUrlCommand *)command
{
    NSDictionary *json = [command.arguments objectAtIndex:0];
  
    float latitude = [[json valueForKey:@"lat"] floatValue];
    float longitude = [[json valueForKey:@"lng"] floatValue];
    int bearing = [[json valueForKey:@"bearing"] integerValue];
    double angle = [[json valueForKey:@"tilt"] doubleValue];
    int zoom = [[json valueForKey:@"zoom"] integerValue];
  
    float duration = 1.0f;
    if (command.arguments.count == 2) {
      duration = [[command.arguments objectAtIndex:1] floatValue] / 1000;
    }
  
    GMSCameraPosition *cameraPosition = [GMSCameraPosition cameraWithLatitude:latitude
                                                           longitude:longitude
                                                           zoom:zoom
                                                           bearing:bearing
                                                           viewingAngle:angle];
  
    [mapCtrl.map setCamera:cameraPosition];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Get license information
 */
-(void)getLicenseInfo:(CDVInvokedUrlCommand *)command
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * @param marker options
 * @return marker key
 */
-(void)GoogleMap_addMarker:(CDVInvokedUrlCommand *)command
{
    NSDictionary *json = [command.arguments objectAtIndex:0];
    float latitude = [[json valueForKey:@"lat"] floatValue];
    float longitude = [[json valueForKey:@"lng"] floatValue];
    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
    GMSMarker *marker = [GMSMarker markerWithPosition:position];
    if ([[json valueForKey:@"visible"] boolValue]) {
      marker.map = mapCtrl.map;
    }

    marker.title = [json valueForKey:@"title"];
    marker.snippet = [json valueForKey:@"snippet"];
    marker.draggable = [[json valueForKey:@"draggable"] boolValue];
    marker.flat = [[json valueForKey:@"flat"] boolValue];
    marker.rotation = [[json valueForKey:@"flat"] floatValue];
    
    NSString *key = [NSString stringWithFormat:@"marker%d", marker.hash];
    [mapCtrl.markerManager setObject:marker forKey: key];
  
    // Create icon
    NSString *iconPath = [json valueForKey:@"icon"];
    if (iconPath) {
      NSRange range = [iconPath rangeOfString:@"http"];
      if (range.location == NSNotFound) {
        marker.icon  = [UIImage imageNamed:iconPath];
      } else {
        dispatch_queue_t gueue = dispatch_queue_create("GoogleMap_addMarker", NULL);
        dispatch_sync(gueue, ^{
          NSURL *url = [NSURL URLWithString:iconPath];
          NSData *data = [NSData dataWithContentsOfURL:url];
          marker.icon = [UIImage imageWithData:data];
        });
        dispatch_release(gueue);
      }
    }
  
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: key];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Show the infowindow of the current marker
 * @params MarkerKey
 */
-(void)Marker_showInfoWindow:(CDVInvokedUrlCommand *)command
{
  
    NSString *hashCode = [command.arguments objectAtIndex:0];
  
    GMSMarker *marker = [mapCtrl.markerManager objectForKey:hashCode];
    if (marker) {
      mapCtrl.map.selectedMarker = marker;
    }
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  
}
/**
 * Hide current infowindow
 * @params MarkerKey
 */
-(void)Marker_hideInfoWindow:(CDVInvokedUrlCommand *)command
{
  mapCtrl.map.selectedMarker = nil;
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * @params MarkerKey
 * @return current marker position with array(latitude, longitude)
 */
-(void)Marker_getPosition:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:0];
  
  GMSMarker *marker = [mapCtrl.markerManager objectForKey:markerKey];
  NSNumber *latitude = @0.0;
  NSNumber *longitude = @0.0;
  if (marker) {
    latitude = [NSNumber numberWithFloat: marker.position.latitude];
    longitude = [NSNumber numberWithFloat: marker.position.longitude];
  }
  NSArray *latlng = @[latitude, longitude];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:latlng];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)Marker_setTitle:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:0];
  GMSMarker *marker = [mapCtrl.markerManager objectForKey:markerKey];
  marker.title = [command.arguments objectAtIndex:1];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)Marker_setSnippet:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:0];
  GMSMarker *marker = [mapCtrl.markerManager objectForKey:markerKey];
  marker.snippet = [command.arguments objectAtIndex:1];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the specified marker
 * @params MarkerKey
 */
-(void)Marker_remove:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:0];
  GMSMarker *marker = [mapCtrl.markerManager objectForKey:markerKey];
  marker.map = nil;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
