//
//  PluginMap.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginMap.h"

@implementation PluginMap

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

- (void)pluginUnload
{
  if (self.executeQueue != nil){
    self.executeQueue.suspended = YES;
    [self.executeQueue cancelAllOperations];
    self.executeQueue.suspended = NO;
    self.executeQueue = nil;
  }

  // Plugin destroy
  self.isRemoved = YES;

  // Load the GoogleMap.m
  //CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  //CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];

  [self clear:nil];

  dispatch_async(dispatch_get_main_queue(), ^{
    [self.mapCtrl.map removeFromSuperview];
    [self.mapCtrl.view removeFromSuperview];
    [self.mapCtrl.view setFrame:CGRectMake(0, -1000, 100, 100)];
    [self.mapCtrl.view setNeedsDisplay];

    NSArray *keys = [self.mapCtrl.plugins allKeys];
    CDVPlugin<MyPlgunProtocol> *plugin;
    for (int i = 0; i < [keys count]; i++) {
      plugin = [self.mapCtrl.plugins objectForKey:[keys objectAtIndex:i]];
      [plugin pluginUnload];
      plugin = nil;
    }


    [self.mapCtrl.plugins removeAllObjects];
    self.mapCtrl.map = nil;
    self.mapCtrl = nil;
  });

}
- (void)loadPlugin:(CDVInvokedUrlCommand*)command {
  NSString *className = [command.arguments objectAtIndex:0];
  NSString *pluginName = [NSString stringWithFormat:@"%@", className];
  className = [NSString stringWithFormat:@"Plugin%@", className];

  @synchronized (self.mapCtrl.plugins) {

    CDVPluginResult* pluginResult = nil;
    CDVPlugin<MyPlgunProtocol> *plugin;
    NSString *pluginId = [NSString stringWithFormat:@"%@-%@", self.mapCtrl.mapId, [pluginName lowercaseString]];

    plugin = [self.mapCtrl.plugins objectForKey:pluginId];
    if (!plugin) {
      plugin = [[NSClassFromString(className)alloc] init];

      if (!plugin) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:[NSString stringWithFormat:@"Class not found: %@", className]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
      }

      // Hack:
      // In order to load the plugin instance of the same class but different names,
      // register the map plugin instance into the pluginObjects directly.
      //
      // Don't use the registerPlugin() method of the CDVViewController.
      // Problem is at
      // https://github.com/apache/cordova-ios/blob/582e35776f01ee03f32f0986de181bcf5eb4d232/CordovaLib/Classes/Public/CDVViewController.m#L577
      //
      CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
      if ([plugin respondsToSelector:@selector(setViewController:)]) {
        [plugin setViewController:cdvViewController];
      }
      if ([plugin respondsToSelector:@selector(setCommandDelegate:)]) {
        [plugin setCommandDelegate:cdvViewController.commandDelegate];
      }
      [cdvViewController.pluginObjects setObject:plugin forKey:pluginId];
      [cdvViewController.pluginsMap setValue:pluginId forKey:pluginId];
      [plugin pluginInitialize];

      //NSLog(@"--->loadPlugin : %@ className : %@, plugin : %@", pluginId, className, plugin);
      [self.mapCtrl.plugins setObject:plugin forKey:pluginId];
      [plugin setGoogleMapsViewController:self.mapCtrl];

    }

    //plugin.commandDelegate = self.commandDelegate;


    SEL selector = NSSelectorFromString(@"create:");
    if ([plugin respondsToSelector:selector]){
      [plugin performSelectorInBackground:selector withObject:command];
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                       messageAsString:[NSString stringWithFormat:@"method not found: %@ in %@ class", @"create", className]];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }
}

- (void)getMap:(CDVInvokedUrlCommand*)command {

  self.executeQueue =  [NSOperationQueue new];

  if ([command.arguments count] == 1) {
    //-----------------------------------------------------------------------
    // case: plugin.google.maps.getMap([options]) (no the mapDiv is given)
    //-----------------------------------------------------------------------
    [self setOptions:command];
    //[self.mapCtrl.view setHidden:true];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    return;
  }

  NSDictionary *initOptions = [command.arguments objectAtIndex:1];
  [initOptions setValue:@"getMap" forKeyPath:@"method"];
  if ([initOptions valueForKey:@"camera"]) {
    double delayInSeconds = 1;
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
    dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
      [self _setOptions:initOptions command:command];
    });
  } else {
    [self _setOptions:initOptions command:command];
  }

}


- (void)setDiv:(CDVInvokedUrlCommand *)command {
  [self.executeQueue addOperationWithBlock:^{

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];

    // Detach the map view
    if ([command.arguments count] == 0 && self.mapCtrl.mapDivId) {
      [googlemaps.pluginLayer removeMapView:self.mapCtrl];
    }

    if ([command.arguments count] == 1) {
      NSString *mapDivId = [command.arguments objectAtIndex:0];
      self.mapCtrl.mapDivId = mapDivId;
      [googlemaps.pluginLayer addMapView:self.mapCtrl];
      [self resizeMap:command];
    }
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


- (void)resizeMap:(CDVInvokedUrlCommand *)command {
  [self.executeQueue addOperationWithBlock:^{

    NSString *mapDivId = self.mapCtrl.mapDivId;
    if (!mapDivId) {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      return;
    }

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];


    // Save the map rectangle.
    if (![googlemaps.pluginLayer.pluginScrollView.debugView.HTMLNodes objectForKey:self.mapCtrl.mapDivId]) {
      NSMutableDictionary *dummyInfo = [[NSMutableDictionary alloc] init];;
      [dummyInfo setObject:@"{{0,-3000} - {50,50}}" forKey:@"size"];
      [dummyInfo setObject:[NSNumber numberWithDouble:-999] forKey:@"depth"];
      [googlemaps.pluginLayer.pluginScrollView.debugView.HTMLNodes setObject:dummyInfo forKey:self.mapCtrl.mapDivId];
    }

    //[googlemaps.pluginLayer updateViewPosition:self.mapCtrl];
  }];
}

-(void)setClickable:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{
    Boolean isClickable = [[command.arguments objectAtIndex:0] boolValue];
    self.mapCtrl.clickable = isClickable;
    //self.debugView.clickable = isClickable;
    //[self.pluginScrollView.debugView setNeedsDisplay];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Clear all markups
 */
- (void)clear:(CDVInvokedUrlCommand *)command {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.mapCtrl.map clear];
  });


  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  CDVPlugin<MyPlgunProtocol> *plugin;
  NSString *pluginName;
  NSArray *keys = [self.mapCtrl.plugins allKeys];
  for (int j = 0; j < [keys count]; j++) {
    pluginName = [keys objectAtIndex:j];
    plugin = [self.mapCtrl.plugins objectForKey:pluginName];
    [plugin pluginUnload];

    [cdvViewController.pluginObjects removeObjectForKey:pluginName];
    [cdvViewController.pluginsMap setValue:nil forKey:pluginName];
    //plugin = nil;
  }

  [self.mapCtrl.plugins removeAllObjects];

  if (command != nil) {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  }
}

/**
 * Move the center of the map
 */
- (void)setCameraTarget:(CDVInvokedUrlCommand *)command {

  float latitude = [[command.arguments objectAtIndex:0] floatValue];
  float longitude = [[command.arguments objectAtIndex:1] floatValue];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    [self.mapCtrl.map animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    self.mapCtrl.map.settings.myLocationButton = isEnabled;
    self.mapCtrl.map.myLocationEnabled = isEnabled;
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setIndoorEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    self.mapCtrl.map.settings.indoorPicker = isEnabled;
    self.mapCtrl.map.indoorEnabled = isEnabled;
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTrafficEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    self.mapCtrl.map.trafficEnabled = isEnabled;
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCompassEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    self.mapCtrl.map.settings.compassButton = isEnabled;
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
- (void)setVisible:(CDVInvokedUrlCommand *)command {
  BOOL isVisible = [[command.arguments objectAtIndex:0] boolValue];
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    [self.mapCtrl.view setHidden:!isVisible];
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCameraTilt:(CDVInvokedUrlCommand *)command {
  double angle = [[command.arguments objectAtIndex:0] doubleValue];
  if (angle >=0 && angle <= 90) {
    GMSCameraPosition *camera = self.mapCtrl.map.camera;
    camera = [GMSCameraPosition cameraWithLatitude:camera.target.latitude
                                         longitude:camera.target.longitude
                                              zoom:camera.zoom
                                           bearing:camera.bearing
                                      viewingAngle:angle];

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [self.mapCtrl.map setCamera:camera];
    }];
  }


  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCameraBearing:(CDVInvokedUrlCommand *)command {
  double bearing = [[command.arguments objectAtIndex:0] doubleValue];
  GMSCameraPosition *camera = self.mapCtrl.map.camera;
  camera = [GMSCameraPosition cameraWithLatitude:camera.target.latitude
                                       longitude:camera.target.longitude
                                            zoom:camera.zoom
                                         bearing:bearing
                                    viewingAngle:camera.viewingAngle];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    [self.mapCtrl.map setCamera:camera];
  }];


  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
- (void)setAllGesturesEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    [self.mapCtrl.map.settings setAllGesturesEnabled:isEnabled];
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Change the zoom level
 */
- (void)setCameraZoom:(CDVInvokedUrlCommand *)command {
  float zoom = [[command.arguments objectAtIndex:0] floatValue];
  CLLocationCoordinate2D center = [self.mapCtrl.map.projection coordinateForPoint:self.mapCtrl.map.center];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    [self.mapCtrl.map setCamera:[GMSCameraPosition cameraWithTarget:center zoom:zoom]];
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Pan by
 */
- (void)panBy:(CDVInvokedUrlCommand *)command {
  int x = [[command.arguments objectAtIndex:0] intValue];
  int y = [[command.arguments objectAtIndex:1] intValue];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    [self.mapCtrl.map animateWithCameraUpdate:[GMSCameraUpdate scrollByX:x * -1 Y:y * -1]];
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the Map Type
 */
- (void)setMapTypeId:(CDVInvokedUrlCommand *)command {
  [self.executeQueue addOperationWithBlock:^{
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
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        self.mapCtrl.map.mapType = mapType;
      }];
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
      // Error : User specifies unknow map type id
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                       messageAsString:[NSString
                                                        stringWithFormat:@"Unknow MapTypeID is specified:%@", typeStr]];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Move the map camera with animation
 */
-(void)animateCamera:(CDVInvokedUrlCommand *)command
{
  [self updateCameraPosition:@"animateCamera" command:command];
}

/**
 * Move the map camera
 */
-(void)moveCamera:(CDVInvokedUrlCommand *)command
{
  [self updateCameraPosition:@"moveCamera" command:command];
}

-(void)getCameraPosition:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    GMSCameraPosition *camera = self.mapCtrl.map.camera;
    [self.executeQueue addOperationWithBlock:^{
      NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
      [latLng setObject:[NSNumber numberWithFloat:camera.target.latitude] forKey:@"lat"];
      [latLng setObject:[NSNumber numberWithFloat:camera.target.longitude] forKey:@"lng"];

      NSMutableDictionary *json = [NSMutableDictionary dictionary];
      [json setObject:[NSNumber numberWithFloat:camera.zoom] forKey:@"zoom"];
      [json setObject:[NSNumber numberWithDouble:camera.viewingAngle] forKey:@"tilt"];
      [json setObject:latLng forKey:@"target"];
      [json setObject:[NSNumber numberWithFloat:camera.bearing] forKey:@"bearing"];
      [json setObject:[NSNumber numberWithInt:(int)camera.hash] forKey:@"hashCode"];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];


}

-(void)_changeCameraPosition: (NSString*)action params:(NSDictionary *)json command:(CDVInvokedUrlCommand *)command {

  int bearing;
  if ([json valueForKey:@"bearing"]) {
    bearing = (int)[[json valueForKey:@"bearing"] integerValue];
  } else {
    bearing = self.mapCtrl.map.camera.bearing;
  }

  double angle;
  if ([json valueForKey:@"tilt"]) {
    angle = [[json valueForKey:@"tilt"] doubleValue];
  } else {
    angle = self.mapCtrl.map.camera.viewingAngle;
  }

  double zoom;
  if ([json valueForKey:@"zoom"] && [json valueForKey:@"zoom"] != [NSNull null]) {
    zoom = [[json valueForKey:@"zoom"] doubleValue];
  } else {
    zoom = self.mapCtrl.map.camera.zoom;
  }

  double cameraPadding = 20;
  if ([json valueForKey:@"padding"]) {
    cameraPadding = [[json valueForKey:@"padding"] doubleValue];
  }


  NSDictionary *latLng = nil;
  double latitude;
  double longitude;
  GMSCameraPosition *cameraPosition;
  GMSCoordinateBounds *cameraBounds = nil;

  UIEdgeInsets paddingUiEdgeInsets = UIEdgeInsetsMake(cameraPadding, cameraPadding, cameraPadding, cameraPadding);

  if ([json objectForKey:@"target"]) {
    NSString *targetClsName = [[json objectForKey:@"target"] className];
    if ([targetClsName isEqualToString:@"__NSCFArray"] || [targetClsName isEqualToString:@"__NSArrayM"] ) {
      //--------------------------------------------
      //  cameraPosition.target = [
      //    new plugin.google.maps.LatLng(),
      //    ...
      //    new plugin.google.maps.LatLng()
      //  ]
      //---------------------------------------------
      int i = 0;
      NSArray *latLngList = [json objectForKey:@"target"];
      GMSMutablePath *path = [GMSMutablePath path];
      for (i = 0; i < [latLngList count]; i++) {
        latLng = [latLngList objectAtIndex:i];
        latitude = [[latLng valueForKey:@"lat"] doubleValue];
        longitude = [[latLng valueForKey:@"lng"] doubleValue];
        [path addLatitude:latitude longitude:longitude];
      }

      cameraBounds = [[GMSCoordinateBounds alloc] initWithPath:path];
      //CLLocationCoordinate2D center = cameraBounds.center;

      cameraPosition = [self.mapCtrl.map cameraForBounds:cameraBounds insets:paddingUiEdgeInsets];
    } else {
      //------------------------------------------------------------------
      //  cameraPosition.target = new plugin.google.maps.LatLng();
      //------------------------------------------------------------------

      latLng = [json objectForKey:@"target"];
      latitude = [[latLng valueForKey:@"lat"] floatValue];
      longitude = [[latLng valueForKey:@"lng"] floatValue];

      cameraPosition = [GMSCameraPosition cameraWithLatitude:latitude
                                                   longitude:longitude
                                                        zoom:zoom
                                                     bearing:bearing
                                                viewingAngle:angle];
    }
  } else {
    cameraPosition = [GMSCameraPosition cameraWithLatitude:self.mapCtrl.map.camera.target.latitude
                                                 longitude:self.mapCtrl.map.camera.target.longitude
                                                      zoom:zoom
                                                   bearing:bearing
                                              viewingAngle:angle];
  }

  float duration = 5.0f;
  if ([json objectForKey:@"duration"]) {
    duration = [[json objectForKey:@"duration"] floatValue] / 1000;
  }

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    if ([action  isEqual: @"animateCamera"]) {
      [CATransaction begin]; {
        [CATransaction setAnimationDuration: duration];

        //[CATransaction setAnimationTimingFunction:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseIn]];
        [CATransaction setAnimationTimingFunction:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut]];

        [CATransaction setCompletionBlock:^{
          if (self.isRemoved) {
            return;
          }
          if (cameraBounds != nil){

            GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:self.mapCtrl.map.camera.target.latitude
                                                                             longitude:self.mapCtrl.map.camera.target.longitude
                                                                                  zoom:self.mapCtrl.map.camera.zoom
                                                                               bearing:[[json objectForKey:@"bearing"] doubleValue]
                                                                          viewingAngle:[[json objectForKey:@"tilt"] doubleValue]];

            [self.mapCtrl.map setCamera:cameraPosition2];

          } else {
            GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:self.mapCtrl.map.projection.visibleRegion];
            [self.mapCtrl.map cameraForBounds:bounds insets:paddingUiEdgeInsets];
          }
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];

        [self.mapCtrl.map animateToCameraPosition: cameraPosition];
      }[CATransaction commit];
    }

    if ([action  isEqual: @"moveCamera"]) {
      [self.mapCtrl.map setCamera:cameraPosition];

      if (cameraBounds != nil){
        double delayInSeconds = 0.5;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
          GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:self.mapCtrl.map.camera.target.latitude
                                                                           longitude:self.mapCtrl.map.camera.target.longitude
                                                                                zoom:self.mapCtrl.map.camera.zoom
                                                                             bearing:[[json objectForKey:@"bearing"] doubleValue]
                                                                        viewingAngle:[[json objectForKey:@"tilt"] doubleValue]];

          if (self.isRemoved) {
            return;
          }
          [self.mapCtrl.map setCamera:cameraPosition2];
          dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:self.mapCtrl.map.projection.visibleRegion];
            [self.mapCtrl.map cameraForBounds:bounds insets:paddingUiEdgeInsets];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          });

        });

      } else {
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }

    }
  }];

}
-(void)updateCameraPosition: (NSString*)action command:(CDVInvokedUrlCommand *)command {
  [self.executeQueue addOperationWithBlock:^{
    NSDictionary *json = [command.arguments objectAtIndex:0];
    [self _changeCameraPosition:action params:json command:command];
  }];
}


- (void)toDataURL:(CDVInvokedUrlCommand *)command {

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    NSDictionary *opts = [command.arguments objectAtIndex:0];
    BOOL uncompress = NO;
    if ([opts objectForKey:@"uncompress"]) {
      uncompress = [[opts objectForKey:@"uncompress"] boolValue];
    }

    if (uncompress) {
      UIGraphicsBeginImageContextWithOptions(self.mapCtrl.view.frame.size, NO, 0.0f);
    } else {
      UIGraphicsBeginImageContext(self.mapCtrl.view.frame.size);
    }
    [self.mapCtrl.view drawViewHierarchyInRect:self.mapCtrl.map.layer.bounds afterScreenUpdates:NO];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    [self.executeQueue addOperationWithBlock:^{
      NSData *imageData = UIImagePNGRepresentation(image);
      NSString *base64Encoded = nil;
      base64Encoded = [NSString stringWithFormat:@"data:image/png;base64,%@", [imageData base64EncodedStringWithSeparateLines:NO]];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:base64Encoded];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Maps an Earth coordinate to a point coordinate in the map's view.
 */
- (void)fromLatLngToPoint:(CDVInvokedUrlCommand*)command {

  float latitude = [[command.arguments objectAtIndex:0] floatValue];
  float longitude = [[command.arguments objectAtIndex:1] floatValue];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    CGPoint point = [self.mapCtrl.map.projection
                     pointForCoordinate:CLLocationCoordinate2DMake(latitude, longitude)];

    [self.executeQueue addOperationWithBlock:^{
      NSMutableArray *pointJSON = [[NSMutableArray alloc] init];
      [pointJSON addObject:[NSNumber numberWithDouble:point.x]];
      [pointJSON addObject:[NSNumber numberWithDouble:point.y]];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:pointJSON];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];

}

/**
 * Maps a point coordinate in the map's view to an Earth coordinate.
 */
- (void)fromPointToLatLng:(CDVInvokedUrlCommand*)command {

  float pointX = [[command.arguments objectAtIndex:0] floatValue];
  float pointY = [[command.arguments objectAtIndex:1] floatValue];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    CLLocationCoordinate2D latLng = [self.mapCtrl.map.projection
                                     coordinateForPoint:CGPointMake(pointX, pointY)];

    [self.executeQueue addOperationWithBlock:^{
      NSMutableArray *latLngJSON = [[NSMutableArray alloc] init];
      [latLngJSON addObject:[NSNumber numberWithDouble:latLng.latitude]];
      [latLngJSON addObject:[NSNumber numberWithDouble:latLng.longitude]];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:latLngJSON];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

- (void)_setOptions:(NSDictionary *)initOptions command:(CDVInvokedUrlCommand *)command {

  [self.executeQueue addOperationWithBlock:^{

    BOOL isEnabled = NO;
    //controls
    NSDictionary *controls = [initOptions objectForKey:@"controls"];
    if (controls) {
      //compass
      if ([controls valueForKey:@"compass"] != nil) {
        isEnabled = [[controls valueForKey:@"compass"] boolValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          if (isEnabled == true) {
            self.mapCtrl.map.settings.compassButton = YES;
          } else {
            self.mapCtrl.map.settings.compassButton = NO;
          }
        }];
      }
      //myLocationButton
      if ([controls valueForKey:@"myLocationButton"] != nil) {
        isEnabled = [[controls valueForKey:@"myLocationButton"] boolValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          if (isEnabled == true) {
            self.mapCtrl.map.settings.myLocationButton = YES;
            self.mapCtrl.map.myLocationEnabled = YES;
          } else {
            self.mapCtrl.map.settings.myLocationButton = NO;
            self.mapCtrl.map.myLocationEnabled = NO;
          }
        }];
      }
      //indoorPicker
      if ([controls valueForKey:@"indoorPicker"] != nil) {
        isEnabled = [[controls valueForKey:@"indoorPicker"] boolValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          if (isEnabled == true) {
            self.mapCtrl.map.settings.indoorPicker = YES;
          } else {
            self.mapCtrl.map.settings.indoorPicker = NO;
          }
        }];
      }
    } else {
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        self.mapCtrl.map.settings.compassButton = YES;
      }];
    }

    //gestures
    NSDictionary *gestures = [initOptions objectForKey:@"gestures"];
    if (gestures) {
      //rotate
      if ([gestures valueForKey:@"rotate"] != nil) {
        isEnabled = [[gestures valueForKey:@"rotate"] boolValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          self.mapCtrl.map.settings.rotateGestures = isEnabled;
        }];
      }
      //scroll
      if ([gestures valueForKey:@"scroll"] != nil) {
        isEnabled = [[gestures valueForKey:@"scroll"] boolValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          self.mapCtrl.map.settings.scrollGestures = isEnabled;
        }];
      }
      //tilt
      if ([gestures valueForKey:@"tilt"] != nil) {
        isEnabled = [[gestures valueForKey:@"tilt"] boolValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          self.mapCtrl.map.settings.tiltGestures = isEnabled;
        }];
      }
      //zoom
      if ([gestures valueForKey:@"zoom"] != nil) {
        isEnabled = [[gestures valueForKey:@"zoom"] boolValue];

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          self.mapCtrl.map.settings.zoomGestures = isEnabled;
        }];
      }
    }
    //preferences
    NSDictionary *preferences = [initOptions objectForKey:@"preferences"];
    if (preferences) {
      //padding
      if ([preferences valueForKey:@"padding"] != nil) {
        NSDictionary *padding = [preferences valueForKey:@"padding"];
        UIEdgeInsets current = self.mapCtrl.map.padding;
        if ([padding objectForKey:@"left"] != nil) {
          current.left = [[padding objectForKey:@"left"] floatValue];
        }
        if ([padding objectForKey:@"top"] != nil) {
          current.top = [[padding objectForKey:@"top"] floatValue];
        }
        if ([padding objectForKey:@"bottom"] != nil) {
          current.bottom = [[padding objectForKey:@"bottom"] floatValue];
        }
        if ([padding objectForKey:@"right"] != nil) {
          current.right = [[padding objectForKey:@"right"] floatValue];
        }

        UIEdgeInsets newPadding = UIEdgeInsetsMake(current.top, current.left, current.bottom, current.right);
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [self.mapCtrl.map setPadding:newPadding];
        }];
      }
      //zoom
      if ([preferences valueForKey:@"zoom"] != nil) {
        NSDictionary *zoom = [preferences valueForKey:@"zoom"];
        float minZoom = self.mapCtrl.map.minZoom;
        float maxZoom = self.mapCtrl.map.maxZoom;
        if ([zoom objectForKey:@"minZoom"] != nil) {
          minZoom = [[zoom objectForKey:@"minZoom"] doubleValue];
        }
        if ([zoom objectForKey:@"maxZoom"] != nil) {
          maxZoom = [[zoom objectForKey:@"maxZoom"] doubleValue];
        }

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [self.mapCtrl.map setMinZoom:minZoom maxZoom:maxZoom];
        }];
      }
    }

    //styles
    NSString *styles = [initOptions valueForKey:@"styles"];
    if (styles) {
      NSError *error;
      GMSMapStyle *mapStyle = [GMSMapStyle styleWithJSONString:styles error:&error];
      if (mapStyle != nil) {
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          self.mapCtrl.map.mapStyle = mapStyle;
          self.mapCtrl.map.mapType = kGMSTypeNormal;
        }];
      } else {
        NSLog(@"Your specified map style is incorrect : %@", error.description);
      }
    } else {

      //mapType
      NSString *typeStr = [initOptions valueForKey:@"mapType"];
      if (typeStr) {

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

          [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            self.mapCtrl.map.mapType = mapType;
          }];
        }
      }
    }

    // Redraw the map mandatory
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [self.mapCtrl.map setNeedsDisplay];
    }];


    if ([initOptions valueForKey:@"camera"]) {
      //------------------------------------------
      // Case : The camera option is specified.
      //------------------------------------------
      NSDictionary *cameraOpts = [initOptions objectForKey:@"camera"];
      [self _changeCameraPosition:@"moveCamera" params:cameraOpts command:command];
    } else {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }];

};

- (void)setOptions:(CDVInvokedUrlCommand *)command {
  NSDictionary *initOptions = [command.arguments objectAtIndex:0];
  [initOptions setValue:@"setOptions" forKeyPath:@"method"];
  [self _setOptions:initOptions command:command];
}


- (void)setPadding:(CDVInvokedUrlCommand *)command {
  [self.executeQueue addOperationWithBlock:^{
    NSDictionary *paddingJson = [command.arguments objectAtIndex:0];
    float top = [[paddingJson objectForKey:@"top"] floatValue];
    float left = [[paddingJson objectForKey:@"left"] floatValue];
    float right = [[paddingJson objectForKey:@"right"] floatValue];
    float bottom = [[paddingJson objectForKey:@"bottom"] floatValue];

    UIEdgeInsets padding = UIEdgeInsetsMake(top, left, bottom, right);

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [self.mapCtrl.map setPadding:padding];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

- (void)getFocusedBuilding:(CDVInvokedUrlCommand*)command {
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    GMSIndoorBuilding *building = self.mapCtrl.map.indoorDisplay.activeBuilding;
    if (building != nil || [building.levels count] == 0) {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      return;
    }
    GMSIndoorLevel *activeLevel = self.mapCtrl.map.indoorDisplay.activeLevel;

    [self.executeQueue addOperationWithBlock:^{

      NSMutableDictionary *result = [NSMutableDictionary dictionary];

      NSUInteger activeLevelIndex = [building.levels indexOfObject:activeLevel];
      [result setObject:[NSNumber numberWithInteger:activeLevelIndex] forKey:@"activeLevelIndex"];
      [result setObject:[NSNumber numberWithInteger:building.defaultLevelIndex] forKey:@"defaultLevelIndex"];

      GMSIndoorLevel *level;
      NSMutableDictionary *levelInfo;
      NSMutableArray *levels = [NSMutableArray array];
      for (level in building.levels) {
        levelInfo = [NSMutableDictionary dictionary];

        [levelInfo setObject:[NSString stringWithString:level.name] forKey:@"name"];
        [levelInfo setObject:[NSString stringWithString:level.shortName] forKey:@"shortName"];
        [levels addObject:levelInfo];
      }
      [result setObject:levels forKey:@"levels"];


      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
@end
