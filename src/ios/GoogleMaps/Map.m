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

- (void)getMap:(CDVInvokedUrlCommand*)command {
    
      
        //NSDictionary *options = [command.arguments objectAtIndex:1];
        
        //if ([options objectForKey:@"backgroundColor"]) {
        //    NSArray *rgbColor = [options objectForKey:@"backgroundColor"];
        //    self.pluginLayer.backgroundColor = [rgbColor parsePluginColor];
        //}
/*
        GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:0
                                            longitude:0
                                            zoom:0
                                            bearing:0
                                            viewingAngle:0];

        CGRect pluginRect = self.mapCtrl.view.frame;
        int marginBottom = 0;
        //if ([PluginUtil isIOS7] == false) {
        //  marginBottom = 20;
        //}
        CGRect mapRect = CGRectMake(0, 0, pluginRect.size.width, pluginRect.size.height  - marginBottom);
        //NSLog(@"mapRect=%f,%f - %f,%f", mapRect.origin.x, mapRect.origin.y, mapRect.size.width, mapRect.size.height);
        //NSLog(@"mapRect=%@", camera);
        self.map = [GMSMapView mapWithFrame:mapRect camera:camera];
  
        //self.map.delegate = self;
        self.map.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
      
        //indoor display
        //self.map.indoorDisplay.delegate = self;
  
        [self.pluginLayer addMapView:self.mapId map:self.map];
*/
        if ([command.arguments count] != 4) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }
  
        [self resizeMap:command];
            
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
 

}


- (void)resizeMap:(CDVInvokedUrlCommand *)command {
    NSInteger argCnt = [command.arguments count];
    self.embedRect = [command.arguments objectAtIndex:(argCnt - 2)];
    [self.mapCtrl.pluginLayer.drawRects setObject:self.embedRect forKey:self.mapId];
  
  
    //self.mapCtrl.pluginScrollView.debugView.drawRects = self.mapCtrl.embedRect;
    [self.mapCtrl.pluginLayer clearHTMLElement:self.mapId];
    //[self.mapCtrl.pluginScrollView.debugView clearHTMLElement];

    NSArray *HTMLs = [command.arguments objectAtIndex:(argCnt - 1)];
    NSString *elemId;
    NSDictionary *elemSize, *elemInfo;
    for (int i = 0; i < [HTMLs count]; i++) {
        elemInfo = [HTMLs objectAtIndex:i];
        elemSize = [elemInfo objectForKey:@"size"];
        elemId = [elemInfo objectForKey:@"id"];
        [self.mapCtrl.pluginLayer putHTMLElement:self.mapId domId:elemId size:elemSize];
        //[self.mapCtrl.pluginScrollView.debugView putHTMLElement:elemId size:elemSize];
    }

    [self.mapCtrl updateMapViewLayout];
}


/**
 * Move the center of the map
 */
- (void)setCenter:(CDVInvokedUrlCommand *)command {
  
  float latitude = [[command.arguments objectAtIndex:1] floatValue];
  float longitude = [[command.arguments objectAtIndex:2] floatValue];
  
  [self.map animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.map.settings.myLocationButton = isEnabled;
  self.map.myLocationEnabled = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setIndoorEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.map.settings.indoorPicker = isEnabled;
  self.map.indoorEnabled = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTrafficEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.map.trafficEnabled = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setCompassEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  self.map.settings.compassButton = isEnabled;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setTilt:(CDVInvokedUrlCommand *)command {
  
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setAllGesturesEnabled:(CDVInvokedUrlCommand *)command {
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  [self.map.settings setAllGesturesEnabled:isEnabled];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Change the zoom level
 */
- (void)setZoom:(CDVInvokedUrlCommand *)command {
  float zoom = [[command.arguments objectAtIndex:1] floatValue];
  CLLocationCoordinate2D center = [self.map.projection coordinateForPoint:self.map.center];
  
  [self.map setCamera:[GMSCameraPosition cameraWithTarget:center zoom:zoom]];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Pan by
 */
- (void)panBy:(CDVInvokedUrlCommand *)command {
  int x = [[command.arguments objectAtIndex:1] intValue];
  int y = [[command.arguments objectAtIndex:2] intValue];
  
  [self.map animateWithCameraUpdate:[GMSCameraUpdate scrollByX:x * -1 Y:y * -1]];
  
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
    self.map.mapType = mapType;
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
  GMSCameraPosition *camera = self.map.camera;
  
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
  NSDictionary *json = [command.arguments objectAtIndex:1];
  
  int bearing = (int)[[json valueForKey:@"bearing"] integerValue];
  double angle = [[json valueForKey:@"tilt"] doubleValue];
  double zoom = [[json valueForKey:@"zoom"] doubleValue];
  
  
  NSDictionary *latLng = nil;
  float latitude;
  float longitude;
  GMSCameraPosition *cameraPosition;
  GMSCoordinateBounds *cameraBounds = nil;
  
  
  if ([json objectForKey:@"target"]) {
    NSString *targetClsName = [[json objectForKey:@"target"] className];
    if ([targetClsName isEqualToString:@"__NSCFArray"] || [targetClsName isEqualToString:@"__NSArrayM"] ) {
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
      
      cameraPosition = [self.map cameraForBounds:cameraBounds insets:UIEdgeInsetsMake(10 * scale, 10* scale, 10* scale, 10* scale)];
    
    } else {
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
    cameraPosition = [GMSCameraPosition cameraWithLatitude:self.map.camera.target.latitude
                                        longitude:self.map.camera.target.longitude
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
        if (cameraBounds != nil){
        
          GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:cameraBounds.center.latitude
                                              longitude:cameraBounds.center.longitude
                                              zoom:self.map.camera.zoom
                                              bearing:[[json objectForKey:@"bearing"] doubleValue]
                                              viewingAngle:[[json objectForKey:@"tilt"] doubleValue]];
        
          [self.map setCamera:cameraPosition2];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
      
      [self.map animateToCameraPosition: cameraPosition];
    }[CATransaction commit];
  }
  
  if ([action  isEqual: @"moveCamera"]) {
    [self.map setCamera:cameraPosition];

    if (cameraBounds != nil){
    
      GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:cameraBounds.center.latitude
                                          longitude:cameraBounds.center.longitude
                                          zoom:self.map.camera.zoom
                                          bearing:[[json objectForKey:@"bearing"] doubleValue]
                                          viewingAngle:[[json objectForKey:@"tilt"] doubleValue]];
    
      [self.map setCamera:cameraPosition2];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }

  
}


- (void)toDataURL:(CDVInvokedUrlCommand *)command {

  NSDictionary *opts = [command.arguments objectAtIndex:1];
  BOOL uncompress = NO;
  if ([opts objectForKey:@"uncompress"]) {
      uncompress = [[opts objectForKey:@"uncompress"] boolValue];
  }
  
  if (uncompress) {
    UIGraphicsBeginImageContextWithOptions(self.mapCtrl.view.frame.size, NO, 0.0f);
  } else {
    UIGraphicsBeginImageContext(self.mapCtrl.view.frame.size);
  }
  [self.mapCtrl.view drawViewHierarchyInRect:self.map.layer.bounds afterScreenUpdates:NO];
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
  
  float latitude = [[command.arguments objectAtIndex:1] floatValue];
  float longitude = [[command.arguments objectAtIndex:2] floatValue];
  
  CGPoint point = [self.map.projection
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
  
  float pointX = [[command.arguments objectAtIndex:1] floatValue];
  float pointY = [[command.arguments objectAtIndex:2] floatValue];
  
  CLLocationCoordinate2D latLng = [self.map.projection
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
  GMSVisibleRegion visibleRegion = self.map.projection.visibleRegion;
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:visibleRegion];

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
}

- (void)setOptions:(CDVInvokedUrlCommand *)command {
  NSDictionary *initOptions = [command.arguments objectAtIndex:1];
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
    NSDictionary *cameraOpts = [initOptions objectForKey:@"camera"];
    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lng"];
    float latitude;
    float longitude;
    GMSCameraPosition *camera;
    GMSCoordinateBounds *cameraBounds = nil;

    if ([cameraOpts objectForKey:@"target"]) {
      NSString *targetClsName = [[cameraOpts objectForKey:@"target"] className];
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
                                            zoom:self.map.camera.zoom
                                            bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                            viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];
        
      } else {
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
      camera = [GMSCameraPosition
                              cameraWithLatitude: [[latLng valueForKey:@"lat"] floatValue]
                              longitude: [[latLng valueForKey:@"lng"] floatValue]
                              zoom: [[cameraOpts valueForKey:@"zoom"] floatValue]
                              bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                              viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];
    }
    self.map.camera = camera;

    if (cameraBounds != nil){
      float scale = 1;
      if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)]) {
        scale = [[UIScreen mainScreen] scale];
      }
      [[UIScreen mainScreen] scale];
      
      [self.map moveCamera:[GMSCameraUpdate fitBounds:cameraBounds withPadding:10 * scale]];
      GMSCameraPosition *cameraPosition2 = [GMSCameraPosition cameraWithLatitude:cameraBounds.center.latitude
                                          longitude:cameraBounds.center.longitude
                                          zoom:self.map.camera.zoom
                                          bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                          viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];
    
      [self.map setCamera:cameraPosition2];
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
        self.map.settings.compassButton = YES;
      } else {
        self.map.settings.compassButton = NO;
      }
    }
    //myLocationButton
    if ([controls valueForKey:@"myLocationButton"] != nil) {
      isEnabled = [[controls valueForKey:@"myLocationButton"] boolValue];
      if (isEnabled == true) {
        self.map.settings.myLocationButton = YES;
        self.map.myLocationEnabled = YES;
      } else {
        self.map.settings.myLocationButton = NO;
        self.map.myLocationEnabled = NO;
      }
    }
    //indoorPicker
    if ([controls valueForKey:@"indoorPicker"] != nil) {
      isEnabled = [[controls valueForKey:@"indoorPicker"] boolValue];
      if (isEnabled == true) {
        self.map.settings.indoorPicker = YES;
      } else {
        self.map.settings.indoorPicker = NO;
      }
    }
  } else {
    self.map.settings.compassButton = YES;
  }

  //gestures
  NSDictionary *gestures = [initOptions objectForKey:@"gestures"];
  if (gestures) {
    //rotate
    if ([gestures valueForKey:@"rotate"] != nil) {
      isEnabled = [[gestures valueForKey:@"rotate"] boolValue];
      self.map.settings.rotateGestures = isEnabled;
    }
    //scroll
    if ([gestures valueForKey:@"scroll"] != nil) {
      isEnabled = [[gestures valueForKey:@"scroll"] boolValue];
      self.map.settings.scrollGestures = isEnabled;
    }
    //tilt
    if ([gestures valueForKey:@"tilt"] != nil) {
      isEnabled = [[gestures valueForKey:@"tilt"] boolValue];
      self.map.settings.tiltGestures = isEnabled;
    }
    //zoom
    if ([gestures valueForKey:@"zoom"] != nil) {
      isEnabled = [[gestures valueForKey:@"zoom"] boolValue];
      self.map.settings.zoomGestures = isEnabled;
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
      self.map.mapType = mapType;
    }
  }
}


- (void)setPadding:(CDVInvokedUrlCommand *)command {
  NSDictionary *paddingJson = [command.arguments objectAtIndex:1];
  float top = [[paddingJson objectForKey:@"top"] floatValue];
  float left = [[paddingJson objectForKey:@"left"] floatValue];
  float right = [[paddingJson objectForKey:@"right"] floatValue];
  float bottom = [[paddingJson objectForKey:@"bottom"] floatValue];
  
  UIEdgeInsets padding = UIEdgeInsetsMake(top, left, bottom, right);
  
  [self.map setPadding:padding];
}

- (void)getFocusedBuilding:(CDVInvokedUrlCommand*)command {
  GMSIndoorBuilding *building = self.map.indoorDisplay.activeBuilding;
  if (building != nil) {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }
  GMSIndoorLevel *activeLevel = self.map.indoorDisplay.activeLevel;
  
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
