//
//  PluginMap.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginMap.h"

@implementation PluginMap


-(void)setPluginViewController:(PluginViewController *)viewCtrl
{
  self.mapCtrl = (PluginMapViewController *)viewCtrl;
}

- (void)pluginInitialize
{
  // Initialize this plugin
  if (self.initialized) return;
  self.initialized = YES;
  
}

- (void)pluginUnload
{
  // Plugin destroy
  self.isRemoved = YES;

  [self clear:nil];

  dispatch_async(dispatch_get_main_queue(), ^{
    [self.mapCtrl.map removeFromSuperview];
    [self.mapCtrl.view removeFromSuperview];
    [self.mapCtrl.view setFrame:CGRectMake(0, -1000, 100, 100)];
    [self.mapCtrl.view setNeedsDisplay];

    NSArray *keys = [self.mapCtrl.plugins allKeys];
    CDVPlugin<IPluginProtocol> *plugin;
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
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];

  NSString *className = [command.arguments objectAtIndex:1];
  NSString *pluginName = [NSString stringWithFormat:@"%@", className];
  className = [NSString stringWithFormat:@"Plugin%@", className];

  @synchronized (instance.mapCtrl.plugins) {

    CDVPluginResult* pluginResult = nil;
    CDVPlugin<IPluginProtocol> *plugin;
    NSString *pluginId = [NSString stringWithFormat:@"%@-%@", instance.mapCtrl.overlayId, [pluginName lowercaseString]];
    


    plugin = [instance.mapCtrl.plugins objectForKey:pluginId];
    if (!plugin) {
      SEL selInitWith = NSSelectorFromString(@"initWithWebViewEngine:");
      plugin = [[NSClassFromString(className)alloc] performSelector:selInitWith withObject:self.webViewEngine];

      if (!plugin) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:[NSString stringWithFormat:@"Class not found: %@", className]];
        [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
      }
      
      [plugin setViewController: instance.mapCtrl];
      [plugin setCommandDelegate: instance.commandDelegate];
      [plugin pluginInitialize];

//      NSLog(@"--->loadPlugin : %@ className : %@, plugin : %@", pluginId, className, plugin);
      [instance.mapCtrl.plugins setObject:plugin forKey: pluginId];
      [plugin setPluginViewController: instance.mapCtrl];

      plugin.commandDelegate = instance.commandDelegate;
    }



    SEL selector = NSSelectorFromString(@"create:");
    if ([plugin respondsToSelector:selector]){
      [plugin performSelectorInBackground:selector withObject:command];
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                       messageAsString:[NSString stringWithFormat:@"method not found: %@ in %@ class", @"create", className]];
      [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }
}

- (void)getMap:(CDVInvokedUrlCommand*)command {


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
  NSDictionary *meta = [command.arguments objectAtIndex:0];
  self.mapCtrl.viewDepth = [[meta objectForKey:@"depth"] integerValue];

  NSDictionary *initOptions = [command.arguments objectAtIndex:1];
  if ([initOptions valueForKey:@"camera"] && [initOptions valueForKey:@"camera"] != [NSNull null]) {
    double delayInSeconds = 1;
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
    dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
      [self _setOptions:initOptions requestMethod:@"getMap" command:command];
    });
  } else {
    [self _setOptions:initOptions requestMethod:@"getMap" command:command];
  }

}

-(void)setClickable:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance.mapCtrl.executeQueue addOperationWithBlock:^{
    Boolean isClickable = [[command.arguments objectAtIndex:0] boolValue];
    instance.mapCtrl.clickable = isClickable;
    //self.debugView.clickable = isClickable;
    //[self.pluginScrollView.debugView setNeedsDisplay];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Clear all markups
 */
- (void)clear:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  dispatch_async(dispatch_get_main_queue(), ^{
    CDVPlugin<IPluginProtocol> *plugin;
    NSString *pluginName;
    NSArray *keys = [instance.mapCtrl.plugins allKeys];
    for (int j = 0; j < [keys count]; j++) {
      pluginName = [keys objectAtIndex:j];
      plugin = [instance.mapCtrl.plugins objectForKey:pluginName];
      [plugin pluginUnload];

    }

    [instance.mapCtrl.plugins removeAllObjects];

    [instance.mapCtrl.map clear];
  });

  if (command != (id)[NSNull null]) {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  }
}

/**
 * Move the center of the map
 */
- (void)setCameraTarget:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  

  [instance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSDictionary *params = [command.arguments objectAtIndex:1];
    
    float latitude = [[params objectForKey:@"lat"] floatValue];
    float longitude = [[params objectForKey:@"lng"] floatValue];

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [instance.mapCtrl.map animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];
    }];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

- (void)setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  NSDictionary *params =[command.arguments objectAtIndex:1];

  instance.mapCtrl.map.settings.myLocationButton = [[params valueForKey:@"myLocationButton"] boolValue];
  instance.mapCtrl.map.myLocationEnabled = [[params valueForKey:@"myLocation"] boolValue];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setIndoorEnabled:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  instance.mapCtrl.map.settings.indoorPicker = isEnabled;
  instance.mapCtrl.map.indoorEnabled = isEnabled;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTrafficEnabled:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  instance.mapCtrl.map.trafficEnabled = isEnabled;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCompassEnabled:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];

  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  instance.mapCtrl.map.settings.compassButton = isEnabled;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
- (void)setVisible:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  BOOL isVisible = [[command.arguments objectAtIndex:1] boolValue];
  [instance.mapCtrl.view setHidden:!isVisible];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCameraTilt:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];

  double angle = [[command.arguments objectAtIndex:1] doubleValue];
  if (angle >=0 && angle <= 90) {
    GMSCameraPosition *camera = instance.mapCtrl.map.camera;
    camera = [GMSCameraPosition cameraWithLatitude:camera.target.latitude
                                         longitude:camera.target.longitude
                                              zoom:camera.zoom
                                           bearing:camera.bearing
                                      viewingAngle:angle];

      [instance.mapCtrl.map setCamera:camera];
  }


  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCameraBearing:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  double bearing = [[command.arguments objectAtIndex:1] doubleValue];
  GMSCameraPosition *camera = instance.mapCtrl.map.camera;
  camera = [GMSCameraPosition cameraWithLatitude:camera.target.latitude
                                       longitude:camera.target.longitude
                                            zoom:camera.zoom
                                         bearing:bearing
                                    viewingAngle:camera.viewingAngle];


  [instance.mapCtrl.map setCamera:camera];


  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
- (void)setAllGesturesEnabled:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  Boolean isEnabled = [[command.arguments objectAtIndex:0] boolValue];
  [instance.mapCtrl.map.settings setAllGesturesEnabled:isEnabled];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Change the zoom level
 */
- (void)setCameraZoom:(CDVInvokedUrlCommand *)command {
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  float zoom = [[command.arguments objectAtIndex:0] floatValue];
  CLLocationCoordinate2D center = [instance.mapCtrl.map.projection coordinateForPoint:instance.mapCtrl.map.center];
  [instance.mapCtrl.map setCamera:[GMSCameraPosition cameraWithTarget:center zoom:zoom]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Pan by
 */
- (void)panBy:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSDictionary *params = [command.arguments objectAtIndex:1];
    int x = [[params objectForKey:@"x"] intValue];
    int y = [[params objectForKey:@"y"] intValue];
    
    [instance.mapCtrl.map animateWithCameraUpdate:[GMSCameraUpdate scrollByX:x * -1 Y:y * -1]];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Change the Map Type
 */
- (void)setMapTypeId:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance.mapCtrl.executeQueue addOperationWithBlock:^{
    CDVPluginResult* pluginResult = nil;

    NSString *typeStr = [command.arguments objectAtIndex:0];
    NSDictionary *mapTypes = [NSDictionary dictionaryWithObjectsAndKeys:
                              ^() {return kGMSTypeHybrid; }, @"MAP_TYPE_HYBRID",
                              ^() {return kGMSTypeSatellite; }, @"MAP_TYPE_SATELLITE",
                              ^() {return kGMSTypeTerrain; }, @"MAP_TYPE_TERRAIN",
                              ^() {return kGMSTypeNormal; }, @"MAP_TYPE_NORMAL",
                              ^() {return kGMSTypeNone; }, @"MAP_TYPE_NONE",
                              nil];

    typedef GMSMapViewType (^CaseBlock)(void);
    GMSMapViewType mapType;
    CaseBlock caseBlock = mapTypes[typeStr];
    if (caseBlock) {
      // Change the map type
      mapType = caseBlock();
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        instance.mapCtrl.map.mapType = mapType;
      }];
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
      // Error : User specifies unknow map type id
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                       messageAsString:[NSString
                                                        stringWithFormat:@"Unknow MapTypeID is specified:%@", typeStr]];
    }
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Move the map camera with animation
 */
-(void)animateCamera:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance updateCameraPosition:@"animateCamera" command:command];
}

/**
 * Move the map camera
 */
-(void)moveCamera:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance updateCameraPosition:@"moveCamera" command:command];
}


-(void)_changeCameraPosition: (NSString*)action requestMethod:(NSString *)requestMethod params:(NSDictionary *)json command:(CDVInvokedUrlCommand *)command {

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    __block double bearing;
    if ([json valueForKey:@"bearing"] && [json valueForKey:@"bearing"] != [NSNull null]) {
      bearing = [[json valueForKey:@"bearing"] doubleValue];
    } else {
      bearing = self.mapCtrl.map.camera.bearing;
    }

    double angle;
    if ([json valueForKey:@"tilt"] && [json valueForKey:@"tilt"] != [NSNull null]) {
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
    if ([json valueForKey:@"padding"] && [json valueForKey:@"zoom"] != [NSNull null]) {
      cameraPadding = [[json valueForKey:@"padding"] doubleValue];
    }


    NSDictionary *latLng = nil;
    double latitude;
    double longitude;
    GMSCameraPosition *cameraPosition;
    GMSCoordinateBounds *cameraBounds = nil;
    CGFloat scale = self.mapCtrl.screenScale;

    UIEdgeInsets paddingUiEdgeInsets = UIEdgeInsetsMake(cameraPadding / scale, cameraPadding / scale, cameraPadding / scale, cameraPadding / scale);

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
        latitude = [[latLng valueForKey:@"lat"] doubleValue];
        longitude = [[latLng valueForKey:@"lng"] doubleValue];

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

        //[CATransaction setAnimationTimingFunction:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionDefault]];
        [CATransaction setAnimationTimingFunction:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut]];

        [CATransaction setCompletionBlock:^{
          if (self.isRemoved) {
            return;
          }
          if (cameraBounds != (id)[NSNull null]){

            GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:self.mapCtrl.map.camera.target.latitude
                                                                             longitude:self.mapCtrl.map.camera.target.longitude
                                                                                  zoom:self.mapCtrl.map.camera.zoom
                                                                               bearing:bearing
                                                                          viewingAngle:angle];

            [self.mapCtrl.map setCamera:cameraPosition2];

          } else {
            if (bearing == 0) {
              GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:self.mapCtrl.map.projection.visibleRegion];
              [self.mapCtrl.map cameraForBounds:bounds insets:paddingUiEdgeInsets];
            }
          }
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];

        [self.mapCtrl.map animateToCameraPosition: cameraPosition];
      }[CATransaction commit];
    }
    if ([action  isEqual: @"moveCamera"]) {
      [self.mapCtrl.map setCamera:cameraPosition];

      if (cameraBounds != (id)[NSNull null]){
        double delayInSeconds = 0.5;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
          GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:self.mapCtrl.map.camera.target.latitude
                                                                           longitude:self.mapCtrl.map.camera.target.longitude
                                                                                zoom:self.mapCtrl.map.camera.zoom
                                                                             bearing:bearing
                                                                        viewingAngle:angle];

          if (self.isRemoved) {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
            return;
          }
          [self.mapCtrl.map setCamera:cameraPosition2];
          dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:self.mapCtrl.map.projection.visibleRegion];
            [self.mapCtrl.map cameraForBounds:bounds insets:paddingUiEdgeInsets];
            [self.mapCtrl.view setHidden:NO];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

          });

        });

      } else {
        [self.mapCtrl.view setHidden:NO];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }

    }
  }];

}
-(void)updateCameraPosition: (NSString*)action command:(CDVInvokedUrlCommand *)command {
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSDictionary *json = [command.arguments objectAtIndex:1];
    [instance _changeCameraPosition:action requestMethod:@"updateCameraPosition" params:json command:command];
  }];
}

- (void)setActiveMarkerId:(CDVInvokedUrlCommand*)command {
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  

  NSString *markerId = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [instance.mapCtrl.objects objectForKey:markerId];
  if (marker != (id)[NSNull null]) {
    instance.mapCtrl.map.selectedMarker = marker;
    instance.mapCtrl.activeMarker = marker;
  }

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [(CDVCommandDelegateImpl *)instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)toDataURL:(CDVInvokedUrlCommand *)command {
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance.mapCtrl.executeQueue addOperationWithBlock:^{

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSDictionary *opts = [command.arguments objectAtIndex:1];
      BOOL uncompress = NO;
      if ([opts objectForKey:@"uncompress"]) {
        uncompress = [[opts objectForKey:@"uncompress"] boolValue];
      }

      if (uncompress) {
        UIGraphicsBeginImageContextWithOptions(instance.mapCtrl.view.frame.size, NO, 0.0f);
      } else {
        UIGraphicsBeginImageContext(instance.mapCtrl.view.frame.size);
      }
      [instance.mapCtrl.view drawViewHierarchyInRect:instance.mapCtrl.map.layer.bounds afterScreenUpdates:NO];
      UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
      UIGraphicsEndImageContext();

      [instance.mapCtrl.executeQueue addOperationWithBlock:^{
        NSData *imageData = UIImagePNGRepresentation(image);
        NSString* base64Encoded = [imageData base64EncodedStringWithOptions:0];
        NSString* base64EncodedWithData = [@"data:image/png;base64," stringByAppendingString:base64Encoded];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:base64EncodedWithData];
        [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
    }];
    
  }];
  
}

/**
 * Maps an Earth coordinate to a point coordinate in the map's view.
 */
- (void)fromLatLngToPoint:(CDVInvokedUrlCommand*)command {
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];

  NSDictionary *params = [command.arguments objectAtIndex:1];
  
  float latitude = [[params objectForKey:@"lat"] floatValue];
  float longitude = [[params objectForKey:@"lng"] floatValue];
  CGPoint point = [instance.mapCtrl.map.projection
                   pointForCoordinate:CLLocationCoordinate2DMake(latitude, longitude)];

  NSMutableArray *pointJSON = [[NSMutableArray alloc] init];
  [pointJSON addObject:[NSNumber numberWithDouble:point.x]];
  [pointJSON addObject:[NSNumber numberWithDouble:point.y]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:pointJSON];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  
}

/**
 * Maps a point coordinate in the map's view to an Earth coordinate.
 */
- (void)fromPointToLatLng:(CDVInvokedUrlCommand*)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  NSDictionary *params = [command.arguments objectAtIndex:1];
  
  float pointX = [[params objectForKey:@"x"] floatValue];
  float pointY = [[params objectForKey:@"y"] floatValue];
  
  CLLocationCoordinate2D latLng = [instance.mapCtrl.map.projection
                                   coordinateForPoint:CGPointMake(pointX, pointY)];

  NSMutableArray *latLngJSON = [[NSMutableArray alloc] init];
  [latLngJSON addObject:[NSNumber numberWithDouble:latLng.latitude]];
  [latLngJSON addObject:[NSNumber numberWithDouble:latLng.longitude]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:latLngJSON];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)_setOptions:(NSDictionary *)initOptions requestMethod:(NSString *)requestMethod command:(CDVInvokedUrlCommand *)command {

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    BOOL isEnabled = NO;
    //controls
    NSDictionary *controls = [initOptions objectForKey:@"controls"];
    if (controls) {
      //compass
      if ([controls valueForKey:@"compass"] != (id)[NSNull null]) {
        isEnabled = [[controls valueForKey:@"compass"] boolValue];
        if (isEnabled == true) {
          self.mapCtrl.map.settings.compassButton = YES;
        } else {
          self.mapCtrl.map.settings.compassButton = NO;
        }
      }
      //myLocationButton
      if ([controls valueForKey:@"myLocationButton"] != (id)[NSNull null]) {
        isEnabled = [[controls valueForKey:@"myLocationButton"] boolValue];
        if (isEnabled == true) {
          self.mapCtrl.map.settings.myLocationButton = YES;
        } else {
          self.mapCtrl.map.settings.myLocationButton = NO;
        }
      }
      //myLocation
      if ([controls valueForKey:@"myLocation"] != (id)[NSNull null]) {
        isEnabled = [[controls valueForKey:@"myLocation"] boolValue];
        if (isEnabled == true) {
          self.mapCtrl.map.myLocationEnabled = YES;
        } else {
          self.mapCtrl.map.myLocationEnabled = NO;
        }
      }
      //indoorPicker
      if ([controls valueForKey:@"indoorPicker"] != (id)[NSNull null]) {
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
      if ([gestures valueForKey:@"rotate"] != (id)[NSNull null]) {
        isEnabled = [[gestures valueForKey:@"rotate"] boolValue];
        self.mapCtrl.map.settings.rotateGestures = isEnabled;
      }
      //scroll
      if ([gestures valueForKey:@"scroll"] != (id)[NSNull null]) {
        isEnabled = [[gestures valueForKey:@"scroll"] boolValue];
        self.mapCtrl.map.settings.scrollGestures = isEnabled;
      }
      //tilt
      if ([gestures valueForKey:@"tilt"] != (id)[NSNull null]) {
        isEnabled = [[gestures valueForKey:@"tilt"] boolValue];
        self.mapCtrl.map.settings.tiltGestures = isEnabled;
      }
      //zoom
      if ([gestures valueForKey:@"zoom"] != (id)[NSNull null]) {
        isEnabled = [[gestures valueForKey:@"zoom"] boolValue];
        self.mapCtrl.map.settings.zoomGestures = isEnabled;
      }
    }
    //preferences
    NSDictionary *preferences = [initOptions objectForKey:@"preferences"];
    if (preferences) {
      //padding
      if ([preferences valueForKey:@"padding"] != (id)[NSNull null]) {
        NSDictionary *padding = [preferences valueForKey:@"padding"];
        UIEdgeInsets current = self.mapCtrl.map.padding;
        if ([padding objectForKey:@"left"] != (id)[NSNull null]) {
          current.left = [[padding objectForKey:@"left"] floatValue];
        }
        if ([padding objectForKey:@"top"] != (id)[NSNull null]) {
          current.top = [[padding objectForKey:@"top"] floatValue];
        }
        if ([padding objectForKey:@"bottom"] != (id)[NSNull null]) {
          current.bottom = [[padding objectForKey:@"bottom"] floatValue];
        }
        if ([padding objectForKey:@"right"] != (id)[NSNull null]) {
          current.right = [[padding objectForKey:@"right"] floatValue];
        }

        UIEdgeInsets newPadding = UIEdgeInsetsMake(current.top, current.left, current.bottom, current.right);
        [self.mapCtrl.map setPadding:newPadding];
      }
      //zoom
      if ([preferences valueForKey:@"zoom"] != (id)[NSNull null]) {
        NSDictionary *zoom = [preferences valueForKey:@"zoom"];
        float minZoom = self.mapCtrl.map.minZoom;
        float maxZoom = self.mapCtrl.map.maxZoom;
        if ([zoom objectForKey:@"minZoom"] != (id)[NSNull null]) {
          minZoom = [[zoom objectForKey:@"minZoom"] doubleValue];
        }
        if ([zoom objectForKey:@"maxZoom"] != (id)[NSNull null]) {
          maxZoom = [[zoom objectForKey:@"maxZoom"] doubleValue];
        }

        [self.mapCtrl.map setMinZoom:minZoom maxZoom:maxZoom];
      }

      // restriction
      NSDictionary *restriction = [preferences valueForKey:@"restriction"];
      if (restriction == (id)[NSNull null]) {
        [self _setCameraRestriction:nil];
      } else {
        NSDictionary *restriction = [preferences objectForKey:@"restriction"];
        [self _setCameraRestriction:restriction];
      }

      // building
      if ([preferences valueForKey:@"building"] != nil) {
        self.mapCtrl.map.buildingsEnabled = [[preferences valueForKey:@"building"] boolValue];
      }
    }

    //styles
    NSString *styles = [initOptions valueForKey:@"styles"];
    if (styles) {
      NSError *error;
      GMSMapStyle *mapStyle = [GMSMapStyle styleWithJSONString:styles error:&error];
      if (mapStyle != (id)[NSNull null]) {
        self.mapCtrl.map.mapStyle = mapStyle;
        self.mapCtrl.map.mapType = kGMSTypeNormal;
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

        typedef GMSMapViewType (^CaseBlock)(void);
        GMSMapViewType mapType;
        CaseBlock caseBlock = mapTypes[typeStr];
        if (caseBlock) {
          // Change the map type
          mapType = caseBlock();
          self.mapCtrl.map.mapType = mapType;
        }
      }
    }

    // Redraw the map mandatory
    [self.mapCtrl.map setNeedsDisplay];

    if ([initOptions valueForKey:@"camera"] && [initOptions valueForKey:@"camera"] != [NSNull null]) {
      //------------------------------------------
      // Case : The camera option is specified.
      //------------------------------------------
      NSDictionary *cameraOpts = [initOptions objectForKey:@"camera"];
      [self _changeCameraPosition:@"moveCamera" requestMethod:requestMethod params:cameraOpts command:command];
    } else {
      [self.mapCtrl mapView:self.mapCtrl.map didChangeCameraPosition:self.mapCtrl.map.camera];
      [self.mapCtrl.view setHidden:NO];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }];

};
- (void)_setCameraRestriction:(NSDictionary *)params {

  if (params == (id)[NSNull null] || params == nil) {
    self.mapCtrl.map.cameraTargetBounds = nil;
    double minZoom = 0;
    double maxZoom = 23;
    [self.mapCtrl.map setMinZoom:minZoom maxZoom:maxZoom];

  } else {

    GMSMutablePath *path = [GMSMutablePath path];
    [path
      addCoordinate: CLLocationCoordinate2DMake([[params objectForKey:@"south"] doubleValue], [[params objectForKey:@"west"] doubleValue])];
    [path
      addCoordinate: CLLocationCoordinate2DMake([[params objectForKey:@"north"] doubleValue], [[params objectForKey:@"east"] doubleValue])];

    GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithPath:path];
    self.mapCtrl.map.cameraTargetBounds = bounds;

    double minZoom = [[params objectForKey:@"minZoom"] doubleValue];
    double maxZoom = [[params objectForKey:@"maxZoom"] doubleValue];
    [self.mapCtrl.map setMinZoom:minZoom maxZoom:maxZoom];
  }
}


- (void)setOptions:(CDVInvokedUrlCommand *)command {
  
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  [instance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSDictionary *initOptions = [command.arguments objectAtIndex:1];
    [initOptions setValue:@"setOptions" forKeyPath:@"method"];
    [instance _setOptions:initOptions requestMethod:@"setOptions" command:command];
  }];
}


- (void)setPadding:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  [instance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSDictionary *paddingJson = [command.arguments objectAtIndex:1];
    float top = [[paddingJson objectForKey:@"top"] floatValue];
    float left = [[paddingJson objectForKey:@"left"] floatValue];
    float right = [[paddingJson objectForKey:@"right"] floatValue];
    float bottom = [[paddingJson objectForKey:@"bottom"] floatValue];

    UIEdgeInsets padding = UIEdgeInsetsMake(top, left, bottom, right);

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [instance.mapCtrl.map setPadding:padding];
    }];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

- (void)getFocusedBuilding:(CDVInvokedUrlCommand*)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];
  
  
  GMSIndoorBuilding *building = instance.mapCtrl.map.indoorDisplay.activeBuilding;
  if (building != (id)[NSNull null] || [building.levels count] == 0) {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    return;
  }
  GMSIndoorLevel *activeLevel = instance.mapCtrl.map.indoorDisplay.activeLevel;

  [instance.mapCtrl.executeQueue addOperationWithBlock:^{

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
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


- (void)stopAnimation:(CDVInvokedUrlCommand*)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];

  [instance.mapCtrl.map.layer removeAllAnimations];
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}
@end
