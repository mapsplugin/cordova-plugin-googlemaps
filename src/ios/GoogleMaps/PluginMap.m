//
//  Map.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
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
  // Plugin destroy
  self.isRemoved = YES;

  // Load the GoogleMap.m
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];
  [googlemaps.pluginLayer.pluginScrollView dettachView];

  [self clear:nil];
  [self.mapCtrl.overlayManager removeAllObjects];
  [self.mapCtrl.map clear];
  [self.mapCtrl.map removeFromSuperview];
  [self.mapCtrl.view removeFromSuperview];
  self.mapCtrl.map = nil;
  self.mapCtrl = nil;

}
- (void)loadPlugin:(CDVInvokedUrlCommand*)command {
  NSString *className = [command.arguments objectAtIndex:0];
  className = [NSString stringWithFormat:@"Plugin%@", className];
  //NSLog(@"--->loadPlugin : %@ className : %@", self.mapId, className);


  [self.loadPluginQueue addOperationWithBlock: ^{
  
      CDVPluginResult* pluginResult = nil;
      CDVPlugin<MyPlgunProtocol> *plugin;

      // Create an instance of the plugin class everytime.
      //#if CORDOVA_VERSION_MIN_REQUIRED >= __CORDOVA_4_0_0
      //  plugin = [(CDVViewController*)self.viewController getCommandInstance:className];
      //#else
        plugin = [[NSClassFromString(className)alloc] init];
      //#endif
    //NSLog(@"--> plugin = %@", plugin);
      if (plugin) {
        plugin.commandDelegate = self.commandDelegate;
        [plugin setGoogleMapsViewController:self.mapCtrl];
        [self.mapCtrl.plugins setObject:plugin forKey:className];

        NSString *pluginId = [NSString stringWithFormat:@"%@-%@", self.mapId, [className lowercaseString]];
    //NSLog(@"--> pluginId = %@", pluginId);

        // Hack:
        // In order to load the plugin instance of the same class but different names,
        // register the map plugin instance into the pluginObjects directly.
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
      }

      if (!plugin) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:[NSString stringWithFormat:@"Class not found: %@", className]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
      }

      SEL selector = NSSelectorFromString(@"create:");
      if ([plugin respondsToSelector:selector]){
          [plugin performSelectorInBackground:selector withObject:command];
      } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                           messageAsString:[NSString stringWithFormat:@"method not found: %@ in %@ class", @"create", className]];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }
  }];
}

- (void)getMap:(CDVInvokedUrlCommand*)command {

    self.loadPluginQueue =  [NSOperationQueue new];

    if ([command.arguments count] == 1) {
        //-----------------------------------------------------------------------
        // case: plugin.google.maps.getMap([options]) (no the mapDiv is given)
        //-----------------------------------------------------------------------
        [self setOptions:command];
        [self.mapCtrl.view setHidden:true];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }


    dispatch_async(dispatch_get_main_queue(), ^{
      [self setOptions:command];

      [self resizeMap:command];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
}


- (void)setDiv:(CDVInvokedUrlCommand *)command {

    // Detach the map view
    if (self.mapCtrl.mapDivId) {
        [self.mapCtrl.view removeFromSuperview];
        self.mapCtrl.mapDivId = nil;
    }

    if ([command.arguments count] == 1) {
        NSString *mapDivId = [command.arguments objectAtIndex:0];
        self.mapCtrl.mapDivId = mapDivId;
        //NSLog(@"---> setDiv : %@", mapDivId);

        // Load the GoogleMap.m
        CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
        CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];

        [googlemaps.pluginLayer addMapView:self.mapId mapCtrl:self.mapCtrl];

        [self resizeMap:command];
    }
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)resizeMap:(CDVInvokedUrlCommand *)command {

    NSString *mapDivId = self.mapCtrl.mapDivId;
    if (!mapDivId) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];
    googlemaps.pluginLayer.needUpdatePosition = YES;


    // Save the map rectangle.
    if (![googlemaps.pluginLayer.pluginScrollView.debugView.HTMLNodes objectForKey:self.mapCtrl.mapDivId]) {
        NSMutableDictionary *dummyInfo = [[NSMutableDictionary alloc] init];
        [dummyInfo setObject:[NSNumber numberWithInt:1] forKey:@"isDummy"];
        [dummyInfo setObject:[NSNumber numberWithDouble:0] forKey:@"offsetX"];
        [dummyInfo setObject:[NSNumber numberWithDouble:-100] forKey:@"offsetY"];
        [dummyInfo setObject:@"{{0,3000} - {50,50}}" forKey:@"size"];
        [dummyInfo setObject:[NSNumber numberWithDouble:-999] forKey:@"depth"];
        [googlemaps.pluginLayer.pluginScrollView.debugView.HTMLNodes setObject:dummyInfo forKey:self.mapCtrl.mapDivId];
    }

    [googlemaps.pluginLayer updateViewPosition:self.mapId];
}

-(void)setClickable:(CDVInvokedUrlCommand *)command
{
    Boolean isClickable = [[command.arguments objectAtIndex:0] boolValue];
    self.mapCtrl.clickable = isClickable;
    //self.debugView.clickable = isClickable;
    //[self.pluginScrollView.debugView setNeedsDisplay];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Remove the map
 */
- (void)remove:(CDVInvokedUrlCommand *)command {

    self.isRemoved = YES;

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];
    //[googlemaps.pluginLayer clearHTMLElement:mapId];
    [googlemaps.pluginLayer.pluginScrollView dettachView];

    [self clear:nil];
    [self.mapCtrl.overlayManager removeAllObjects];
    [self.mapCtrl.map clear];
    [self.mapCtrl.map removeFromSuperview];
    [self.mapCtrl.view removeFromSuperview];
    self.mapCtrl.map = nil;
    self.mapCtrl = nil;

    if (command != nil) {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

/**
 * Clear all markups
 */
- (void)clear:(CDVInvokedUrlCommand *)command {
    [self.mapCtrl.map clear];
    if (self.loadPluginQueue != nil){
        self.loadPluginQueue.suspended = YES;
        [self.loadPluginQueue cancelAllOperations];
        self.loadPluginQueue.suspended = NO;
    }
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        NSArray *keys = [self.mapCtrl.overlayManager allKeys];
        NSString *key;
        for (int i = 0; i < keys.count; i++) {
            key = keys[i];
            if ([key hasPrefix:@"marker_property_"]) {
                NSMutableDictionary *properties = [self.mapCtrl.overlayManager objectForKey:key];
                properties = nil;
                [self.mapCtrl.overlayManager removeObjectForKey:key];
            }
            if ([key hasPrefix:@"marker_"]) {
                GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:key];
                marker = nil;
                [self.mapCtrl.overlayManager removeObjectForKey:key];
            }
        }
        [self.mapCtrl.overlayManager removeAllObjects];
        //[self.mapCtrl.plugins removeAllObjects];

        if (command != nil) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          
        }
    });

}

/**
 * Move the center of the map
 */
- (void)setCenter:(CDVInvokedUrlCommand *)command {

  float latitude = [[command.arguments objectAtIndex:0] floatValue];
  float longitude = [[command.arguments objectAtIndex:1] floatValue];

  [self.mapCtrl.map animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  self.mapCtrl.map.settings.myLocationButton = isEnabled;
  self.mapCtrl.map.myLocationEnabled = isEnabled;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setIndoorEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  self.mapCtrl.map.settings.indoorPicker = isEnabled;
  self.mapCtrl.map.indoorEnabled = isEnabled;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTrafficEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  self.mapCtrl.map.trafficEnabled = isEnabled;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCompassEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  self.mapCtrl.map.settings.compassButton = isEnabled;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
- (void)setVisible:(CDVInvokedUrlCommand *)command {
    BOOL isVisible = [[command.arguments objectAtIndex:0] boolValue];
    [self.mapCtrl.view setHidden:isVisible];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTilt:(CDVInvokedUrlCommand *)command {
  double angle = [[command.arguments objectAtIndex:0] doubleValue];
  if (angle >=0 && angle < 90) {
    GMSCameraPosition *camera = self.mapCtrl.map.camera;
    camera = [GMSCameraPosition cameraWithLatitude:camera.target.latitude
                                          longitude:camera.target.longitude
                                          zoom:camera.zoom
                                          bearing:camera.bearing
                                          viewingAngle:angle];
    [self.mapCtrl.map setCamera:camera];
  }


  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setAllGesturesEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  [self.mapCtrl.map.settings setAllGesturesEnabled:isEnabled];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Change the zoom level
 */
- (void)setZoom:(CDVInvokedUrlCommand *)command {
  float zoom = [[command.arguments objectAtIndex:0] floatValue];
  CLLocationCoordinate2D center = [self.mapCtrl.map.projection coordinateForPoint:self.mapCtrl.map.center];

  [self.mapCtrl.map setCamera:[GMSCameraPosition cameraWithTarget:center zoom:zoom]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Pan by
 */
- (void)panBy:(CDVInvokedUrlCommand *)command {
  int x = [[command.arguments objectAtIndex:0] intValue];
  int y = [[command.arguments objectAtIndex:1] intValue];

  [self.mapCtrl.map animateWithCameraUpdate:[GMSCameraUpdate scrollByX:x * -1 Y:y * -1]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Change the Map Type
 */
- (void)setMapTypeId:(CDVInvokedUrlCommand *)command {
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
  GMSCameraPosition *camera = self.mapCtrl.map.camera;

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
}

-(void)updateCameraPosition: (NSString*)action command:(CDVInvokedUrlCommand *)command {
  NSDictionary *json = [command.arguments objectAtIndex:0];

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
  if ([json valueForKey:@"zoom"]) {
    zoom = [[json valueForKey:@"zoom"] doubleValue];
  } else {
    zoom = self.mapCtrl.map.camera.zoom;
  }


  NSDictionary *latLng = nil;
  float latitude;
  float longitude;
  GMSCameraPosition *cameraPosition;
  GMSCoordinateBounds *cameraBounds = nil;


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
        latitude = [[latLng valueForKey:@"lat"] floatValue];
        longitude = [[latLng valueForKey:@"lng"] floatValue];
        [path addLatitude:latitude longitude:longitude];
      }
      float scale = 1;
      if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)]) {
        scale = [[UIScreen mainScreen] scale];
      }
      [[UIScreen mainScreen] scale];

      cameraBounds = [[GMSCoordinateBounds alloc] initWithPath:path];
      //CLLocationCoordinate2D center = cameraBounds.center;

      cameraPosition = [self.mapCtrl.map cameraForBounds:cameraBounds insets:UIEdgeInsetsMake(10 * scale, 10* scale, 10* scale, 10* scale)];

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

          GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:cameraBounds.center.latitude
                                              longitude:cameraBounds.center.longitude
                                              zoom:self.mapCtrl.map.camera.zoom
                                              bearing:[[json objectForKey:@"bearing"] doubleValue]
                                              viewingAngle:[[json objectForKey:@"tilt"] doubleValue]];

          [self.mapCtrl.map setCamera:cameraPosition2];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

      [self.mapCtrl.map animateToCameraPosition: cameraPosition];
    }[CATransaction commit];
  }

  if ([action  isEqual: @"moveCamera"]) {
    [self.mapCtrl.map setCamera:cameraPosition];

    if (cameraBounds != nil){

      GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:cameraBounds.center.latitude
                                          longitude:cameraBounds.center.longitude
                                          zoom:self.mapCtrl.map.camera.zoom
                                          bearing:[[json objectForKey:@"bearing"] doubleValue]
                                          viewingAngle:[[json objectForKey:@"tilt"] doubleValue]];

      if (self.isRemoved) {
        return;
      }
      [self.mapCtrl.map setCamera:cameraPosition2];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }


}


- (void)toDataURL:(CDVInvokedUrlCommand *)command {

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

  NSData *imageData = UIImagePNGRepresentation(image);
  NSString *base64Encoded = nil;
  base64Encoded = [NSString stringWithFormat:@"data:image/png;base64,%@", [imageData base64EncodedStringWithSeparateLines:NO]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:base64Encoded];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Maps an Earth coordinate to a point coordinate in the map's view.
 */
- (void)fromLatLngToPoint:(CDVInvokedUrlCommand*)command {

  float latitude = [[command.arguments objectAtIndex:0] floatValue];
  float longitude = [[command.arguments objectAtIndex:1] floatValue];

  CGPoint point = [self.mapCtrl.map.projection
                      pointForCoordinate:CLLocationCoordinate2DMake(latitude, longitude)];

  NSMutableArray *pointJSON = [[NSMutableArray alloc] init];
  [pointJSON addObject:[NSNumber numberWithDouble:point.x]];
  [pointJSON addObject:[NSNumber numberWithDouble:point.y]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:pointJSON];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Maps a point coordinate in the map's view to an Earth coordinate.
 */
- (void)fromPointToLatLng:(CDVInvokedUrlCommand*)command {

  float pointX = [[command.arguments objectAtIndex:0] floatValue];
  float pointY = [[command.arguments objectAtIndex:1] floatValue];

  CLLocationCoordinate2D latLng = [self.mapCtrl.map.projection
                      coordinateForPoint:CGPointMake(pointX, pointY)];

  NSMutableArray *latLngJSON = [[NSMutableArray alloc] init];
  [latLngJSON addObject:[NSNumber numberWithDouble:latLng.latitude]];
  [latLngJSON addObject:[NSNumber numberWithDouble:latLng.longitude]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:latLngJSON];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Return the visible region of the map
 * Thanks @fschmidt
 */
- (void)getVisibleRegion:(CDVInvokedUrlCommand*)command {

    dispatch_queue_t gueue = dispatch_queue_create("getVisibleRegion", NULL);

    dispatch_sync(gueue, ^{
        GMSVisibleRegion visibleRegion = self.mapCtrl.map.projection.visibleRegion;
        GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:visibleRegion];

        dispatch_async(gueue, ^{
            NSMutableDictionary *json = [NSMutableDictionary dictionary];
            NSMutableDictionary *northeast = [NSMutableDictionary dictionary];
            [northeast setObject:[NSNumber numberWithFloat:bounds.northEast.latitude] forKey:@"lat"];
            [northeast setObject:[NSNumber numberWithFloat:bounds.northEast.longitude] forKey:@"lng"];
            [json setObject:northeast forKey:@"northeast"];
            NSMutableDictionary *southwest = [NSMutableDictionary dictionary];
            [southwest setObject:[NSNumber numberWithFloat:bounds.southWest.latitude] forKey:@"lat"];
            [southwest setObject:[NSNumber numberWithFloat:bounds.southWest.longitude] forKey:@"lng"];
            [json setObject:southwest forKey:@"southwest"];

            NSMutableArray *latLngArray = [NSMutableArray array];
            [latLngArray addObject:northeast];
            [latLngArray addObject:southwest];
            [json setObject:latLngArray forKey:@"latLngArray"];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                  });
  });
}

- (void)setOptions:(CDVInvokedUrlCommand *)command {
  NSDictionary *initOptions;
  if ([command.arguments count] == 1) {
    // case of setOptions(options); and getMap(options)
    initOptions = [command.arguments objectAtIndex:0];
  } else {
    // case of getMap(div, options);
    initOptions = [command.arguments objectAtIndex:1];
  }


/*
  if ([initOptions valueForKey:@"camera"]) {
    // camera position
    NSDictionary *cameraOpts = [initOptions objectForKey:@"camera"];
    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lng"];

    if (cameraOpts) {
      NSDictionary *latLngJSON = [cameraOpts objectForKey:@"latLng"];
      [latLng setObject:[NSNumber numberWithFloat:[[latLngJSON valueForKey:@"lat"] floatValue]] forKey:@"lat"];
      [latLng setObject:[NSNumber numberWithFloat:[[latLngJSON valueForKey:@"lng"] floatValue]] forKey:@"lng"];
    }
    GMSCameraPosition *camera = [GMSCameraPosition
                                  cameraWithLatitude: [[latLng valueForKey:@"lat"] floatValue]
                                  longitude: [[latLng valueForKey:@"lng"] floatValue]
                                  zoom: [[cameraOpts valueForKey:@"zoom"] floatValue]
                                  bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                  viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];

    self.map.camera = camera;
  }
*/

  if ([initOptions valueForKey:@"camera"]) {
    //------------------------------------------
    // Case : The camera option is specified.
    //------------------------------------------
    NSDictionary *cameraOpts = [initOptions objectForKey:@"camera"];
    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lng"];
    float latitude;
    float longitude;
    GMSCameraPosition *camera;
    GMSCoordinateBounds *cameraBounds = nil;

    //--------------------------
    // MapOptions.camera.target
    //--------------------------
    if ([cameraOpts objectForKey:@"target"]) {
      NSString *targetClsName = [[cameraOpts objectForKey:@"target"] className];
      //-----------------------------------
      // MapOptions.camera.target = [
      //   new plugin.google.maps.LatLng(...),
      //   ...,
      //   new plugin.google.maps.LatLng(...)
      // ];
      //-----------------------------------
      if ([targetClsName isEqualToString:@"__NSCFArray"] || [targetClsName isEqualToString:@"__NSArrayM"] ) {
        int i = 0;
        NSArray *latLngList = [cameraOpts objectForKey:@"target"];
        GMSMutablePath *path = [GMSMutablePath path];
        for (i = 0; i < [latLngList count]; i++) {
          latLng = [latLngList objectAtIndex:i];
          latitude = [[latLng valueForKey:@"lat"] floatValue];
          longitude = [[latLng valueForKey:@"lng"] floatValue];
          [path addLatitude:latitude longitude:longitude];
        }
        float scale = 1;
        if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)]) {
          scale = [[UIScreen mainScreen] scale];
        }
        [[UIScreen mainScreen] scale];

        cameraBounds = [[GMSCoordinateBounds alloc] initWithPath:path];

        CLLocationCoordinate2D center = cameraBounds.center;

        camera = [GMSCameraPosition cameraWithLatitude:center.latitude
                                            longitude:center.longitude
                                            zoom:self.mapCtrl.map.camera.zoom
                                            bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                            viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];

      } else {
        //------------------------------------------------------------------
        // MapOptions.camera.target = new plugin.google.maps.LatLng(...)
        //------------------------------------------------------------------
        latLng = [cameraOpts objectForKey:@"target"];
        latitude = [[latLng valueForKey:@"lat"] floatValue];
        longitude = [[latLng valueForKey:@"lng"] floatValue];

        camera = [GMSCameraPosition cameraWithLatitude:latitude
                                            longitude:longitude
                                            zoom:[[cameraOpts valueForKey:@"zoom"] floatValue]
                                            bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                            viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];
      }
    } else {
      //------------------------------------------------------------------
      // MapOptions.camera = {
      //    lat: ...,
      //    lng: ...,
      //    zoom: ...,
      //    bearing: ...,
      //    tilt: ...
      // }
      //------------------------------------------------------------------
      camera = [GMSCameraPosition
                              cameraWithLatitude: [[latLng valueForKey:@"lat"] floatValue]
                              longitude: [[latLng valueForKey:@"lng"] floatValue]
                              zoom: [[cameraOpts valueForKey:@"zoom"] floatValue]
                              bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                              viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];
    }
    self.mapCtrl.map.camera = camera;

    if (cameraBounds != nil){
      float scale = 1;
      if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)]) {
        scale = [[UIScreen mainScreen] scale];
      }
      [[UIScreen mainScreen] scale];

      [self.mapCtrl.map moveCamera:[GMSCameraUpdate fitBounds:cameraBounds withPadding:10 * scale]];
      GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:cameraBounds.center.latitude
                                          longitude:cameraBounds.center.longitude
                                          zoom:self.mapCtrl.map.camera.zoom
                                          bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                          viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];

      [self.mapCtrl.map setCamera:cameraPosition2];
    }
  }

  BOOL isEnabled = NO;
  //controls
  NSDictionary *controls = [initOptions objectForKey:@"controls"];
  if (controls) {
    //compass
    if ([controls valueForKey:@"compass"] != nil) {
      isEnabled = [[controls valueForKey:@"compass"] boolValue];
      if (isEnabled == true) {
        self.mapCtrl.map.settings.compassButton = YES;
      } else {
        self.mapCtrl.map.settings.compassButton = NO;
      }
    }
    //myLocationButton
    if ([controls valueForKey:@"myLocationButton"] != nil) {
      isEnabled = [[controls valueForKey:@"myLocationButton"] boolValue];
      if (isEnabled == true) {
        self.mapCtrl.map.settings.myLocationButton = YES;
        self.mapCtrl.map.myLocationEnabled = YES;
      } else {
        self.mapCtrl.map.settings.myLocationButton = NO;
        self.mapCtrl.map.myLocationEnabled = NO;
      }
    }
    //indoorPicker
    if ([controls valueForKey:@"indoorPicker"] != nil) {
      isEnabled = [[controls valueForKey:@"indoorPicker"] boolValue];
      if (isEnabled == true) {
        self.mapCtrl.map.settings.indoorPicker = YES;
      } else {
        self.mapCtrl.map.settings.indoorPicker = NO;
      }
    }
  } else {
    self.mapCtrl.map.settings.compassButton = YES;
  }

  //gestures
  NSDictionary *gestures = [initOptions objectForKey:@"gestures"];
  if (gestures) {
    //rotate
    if ([gestures valueForKey:@"rotate"] != nil) {
      isEnabled = [[gestures valueForKey:@"rotate"] boolValue];
      self.mapCtrl.map.settings.rotateGestures = isEnabled;
    }
    //scroll
    if ([gestures valueForKey:@"scroll"] != nil) {
      isEnabled = [[gestures valueForKey:@"scroll"] boolValue];
      self.mapCtrl.map.settings.scrollGestures = isEnabled;
    }
    //tilt
    if ([gestures valueForKey:@"tilt"] != nil) {
      isEnabled = [[gestures valueForKey:@"tilt"] boolValue];
      self.mapCtrl.map.settings.tiltGestures = isEnabled;
    }
    //zoom
    if ([gestures valueForKey:@"zoom"] != nil) {
      isEnabled = [[gestures valueForKey:@"zoom"] boolValue];
      self.mapCtrl.map.settings.zoomGestures = isEnabled;
    }
  }

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
      self.mapCtrl.map.mapType = mapType;
    }
  }

  // Redraw the map mandatory
  [self.mapCtrl.map setNeedsDisplay];
}


- (void)setPadding:(CDVInvokedUrlCommand *)command {
  NSDictionary *paddingJson = [command.arguments objectAtIndex:0];
  float top = [[paddingJson objectForKey:@"top"] floatValue];
  float left = [[paddingJson objectForKey:@"left"] floatValue];
  float right = [[paddingJson objectForKey:@"right"] floatValue];
  float bottom = [[paddingJson objectForKey:@"bottom"] floatValue];

  UIEdgeInsets padding = UIEdgeInsetsMake(top, left, bottom, right);

  [self.mapCtrl.map setPadding:padding];
}

- (void)getFocusedBuilding:(CDVInvokedUrlCommand*)command {
  GMSIndoorBuilding *building = self.mapCtrl.map.indoorDisplay.activeBuilding;
  if (building != nil) {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }
  GMSIndoorLevel *activeLevel = self.mapCtrl.map.indoorDisplay.activeLevel;

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
}
@end
