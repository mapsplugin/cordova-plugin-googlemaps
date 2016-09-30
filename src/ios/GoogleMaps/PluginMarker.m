//
//  Marker.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "PluginMarker.h"
@implementation PluginMarker
-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
    self.mapCtrl = viewCtrl;
}

- (void)pluginInitialize
{
  // Initialize this plugin
  self.imgCache = [[NSCache alloc]init];
  self.imgCache.totalCostLimit = 3 * 1024 * 1024 * 1024; // 3MB = Cache for image
  self.executeQueue =  [NSOperationQueue new];
  self.objects = [[NSMutableDictionary alloc] init];
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
  NSArray *keys = [self.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      if ([key hasPrefix:@"marker_"] &&
        ![key hasPrefix:@"marker_property"]) {
          GMSMarker *marker = (GMSMarker *)[self.objects objectForKey:key];
          [marker.layer removeAllAnimations];
          marker = nil;
      }
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;
  
  [self.imgCache removeAllObjects];
  self.imgCache = nil;
  key = nil;
  keys = nil;
  
  
  NSString *pluginId = [NSString stringWithFormat:@"%@-marker", self.mapCtrl.mapId];
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  [cdvViewController.pluginObjects removeObjectForKey:pluginId];
  //[cdvViewController.pluginsMap setValue:nil forKey:pluginId];
  pluginId = nil;
}

/**
 * @param marker options
 * @return marker key
 */
-(void)create:(CDVInvokedUrlCommand *)command
{
    NSDictionary *json = [command.arguments objectAtIndex:1];
    NSDictionary *latLng = [json objectForKey:@"position"];
    float latitude = [[latLng valueForKey:@"lat"] floatValue];
    float longitude = [[latLng valueForKey:@"lng"] floatValue];

    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);

    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];


    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSMutableDictionary *iconProperty = nil;
        NSString *animation = nil;
        
        // Create a marker
        GMSMarker *marker = [GMSMarker markerWithPosition:position];

        if ([json valueForKey:@"title"]) {
            [marker setTitle: [json valueForKey:@"title"]];
        }
        if ([json valueForKey:@"snippet"]) {
            [marker setSnippet: [json valueForKey:@"snippet"]];
        }
        if ([json valueForKey:@"draggable"]) {
            [marker setDraggable:[[json valueForKey:@"draggable"] boolValue]];
        }
        if ([json valueForKey:@"flat"]) {
            [marker setFlat:[[json valueForKey:@"flat"] boolValue]];
        }
        if ([json valueForKey:@"rotation"]) {
            CLLocationDegrees degrees = [[json valueForKey:@"rotation"] doubleValue];
            [marker setRotation:degrees];
        }
        if ([json valueForKey:@"opacity"]) {
            [marker setOpacity:[[json valueForKey:@"opacity"] floatValue]];
        }
        if ([json valueForKey:@"zIndex"]) {
            [marker setZIndex:[[json valueForKey:@"zIndex"] intValue]];
        }


        NSString *markerId = [NSString stringWithFormat:@"marker_%lu", (unsigned long)marker.hash];
        [self.objects setObject:marker forKey: markerId];
      
        [result setObject:markerId forKey:@"id"];
        [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)marker.hash] forKey:@"hashCode"];

        // Custom properties
        NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
        NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];

        if ([json valueForKey:@"styles"]) {
            NSDictionary *styles = [json valueForKey:@"styles"];
            [properties setObject:styles forKey:@"styles"];
        }

        BOOL disableAutoPan = NO;
        if ([json valueForKey:@"disableAutoPan"] != nil) {
            disableAutoPan = [[json valueForKey:@"disableAutoPan"] boolValue];
        }
        [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
        [self.objects setObject:properties forKey: propertyId];

        // Create icon
        NSObject *icon = [json valueForKey:@"icon"];
        if ([icon isKindOfClass:[NSString class]]) {
            iconProperty = [NSMutableDictionary dictionary];
            [iconProperty setObject:icon forKey:@"url"];

        } else if ([icon isKindOfClass:[NSDictionary class]]) {
            iconProperty = [json valueForKey:@"icon"];

        } else if ([icon isKindOfClass:[NSArray class]]) {
            NSArray *rgbColor = [json valueForKey:@"icon"];
            iconProperty = [NSMutableDictionary dictionary];
            [iconProperty setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
        }

        // Visible property
        if (iconProperty) {
            [iconProperty setObject:[NSNumber numberWithBool:[[json valueForKey:@"visible"] boolValue]] forKey:@"visible"];
        }

        // Animation
        if ([json valueForKey:@"animation"]) {
            animation = [json valueForKey:@"animation"];
            if (iconProperty) {
                [iconProperty setObject:animation forKey:@"animation"];
            }
        }

        if (iconProperty) {
            if ([json valueForKey:@"infoWindowAnchor"]) {
                [iconProperty setObject:[json valueForKey:@"infoWindowAnchor"] forKey:@"infoWindowAnchor"];
            }

            // Load icon in asynchronise
            CDVPluginResult* pluginResult  = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
            [self setIcon_:marker iconProperty:iconProperty pluginResult:pluginResult callbackId:command.callbackId];

        } else {

            dispatch_async(dispatch_get_main_queue(), ^{
                if ([[json valueForKey:@"visible"] boolValue]) {
                    marker.map = self.mapCtrl.map;
                }
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
                if (animation) {
                    [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:command.callbackId];
                } else {
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                }
            });
        }
    }];

}

/**
 * Show the infowindow of the current marker
 * @params markerId
 */
-(void)showInfoWindow:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *hashCode = [command.arguments objectAtIndex:0];

      GMSMarker *marker = [self.objects objectForKey:hashCode];
      if (marker) {
          self.mapCtrl.map.selectedMarker = marker;
      }

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * Hide current infowindow
 * @params markerId
 */
-(void)hideInfoWindow:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      self.mapCtrl.map.selectedMarker = nil;
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * @params markerId
 * @return current marker position with array(latitude, longitude)
 */
-(void)getPosition:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];

      GMSMarker *marker = [self.objects objectForKey:markerId];
      NSNumber *latitude = @0.0;
      NSNumber *longitude = @0.0;
      if (marker) {
          latitude = [NSNumber numberWithFloat: marker.position.latitude];
          longitude = [NSNumber numberWithFloat: marker.position.longitude];
      }
      NSMutableDictionary *json = [NSMutableDictionary dictionary];
      [json setObject:latitude forKey:@"lat"];
      [json setObject:longitude forKey:@"lng"];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set title to the specified marker
 * @params markerId
 */
-(void)setTitle:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      marker.title = [command.arguments objectAtIndex:1];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set title to the specified marker
 * @params markerId
 */
-(void)setSnippet:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      marker.snippet = [command.arguments objectAtIndex:1];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Remove the specified marker
 * @params markerId
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
      marker.map = nil;
      [self.objects removeObjectForKey:markerId];
      [self.objects removeObjectForKey:propertyId];
      marker = nil;
    
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * Set anchor of the marker
 * @params markerId
 */
-(void)setIconAnchor:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      CGFloat anchorX = [[command.arguments objectAtIndex:1] floatValue];
      CGFloat anchorY = [[command.arguments objectAtIndex:2] floatValue];

      if (marker.icon) {
          anchorX = anchorX / marker.icon.size.width;
          anchorY = anchorY / marker.icon.size.height;
          [[NSOperationQueue mainQueue] addOperationWithBlock:^{
              [marker setGroundAnchor:CGPointMake(anchorX, anchorY)];
          }];
      }

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set anchor of the info window
 * @params markerId
 */
-(void)setInfoWindowAnchor:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          float anchorX = [[command.arguments objectAtIndex:1] floatValue];
          float anchorY = [[command.arguments objectAtIndex:2] floatValue];
          CDVPluginResult* pluginResult;
          if (marker.icon) {
              anchorX = anchorX / marker.icon.size.width;
              anchorY = anchorY / marker.icon.size.height;
              [marker setInfoWindowAnchor:CGPointMake(anchorX, anchorY)];
              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          } else {
              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
          }
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

  }];
}


/**
 * Set opacity
 * @params markerId
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      marker.opacity = [[command.arguments objectAtIndex:1] floatValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set zIndex
 * @params markerId
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      marker.zIndex = [[command.arguments objectAtIndex:1] intValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set draggable
 * @params markerId
 */
-(void)setDraggable:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
      [marker setDraggable:isEnabled];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set disable auto pan
 * @params markerId
 */
-(void)setDisableAutoPan:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      BOOL disableAutoPan = [[command.arguments objectAtIndex:1] boolValue];

      NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
      [self.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set visibility
 * @params markerId
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{

      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

      if (isVisible) {
          marker.map = self.mapCtrl.map;
      } else {
          marker.map = nil;
      }
      
      NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"visible"];
      [self.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set position
 * @params key
 */
-(void)setPosition:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];

      float latitude = [[command.arguments objectAtIndex:1] floatValue];
      float longitude = [[command.arguments objectAtIndex:2] floatValue];
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [marker setPosition:position];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
}

/**
 * Set flattable
 * @params markerId
 */
-(void)setFlat:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];
      Boolean isFlat = [[command.arguments objectAtIndex:1] boolValue];
      [marker setFlat: isFlat];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * set icon
 * @params markerId
 */
-(void)setIcon:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];

      // Create icon
      NSMutableDictionary *iconProperty;
      id icon = [command.arguments objectAtIndex:1];
      if ([icon isKindOfClass:[NSString class]]) {
          iconProperty = [[NSMutableDictionary alloc] init];
          [iconProperty setObject:icon forKey:@"url"];
      } else if ([icon isKindOfClass:[NSDictionary class]]) {
          iconProperty = [command.arguments objectAtIndex:1];
      } else if ([icon isKindOfClass:[NSArray class]]) {
          NSArray *rgbColor = icon;
          iconProperty = [[NSMutableDictionary alloc] init];
          [iconProperty setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
      }
    

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self setIcon_:marker iconProperty:iconProperty pluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * set rotation
 */
-(void)setRotation:(CDVInvokedUrlCommand *)command
{
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];

      CLLocationDegrees degrees = [[command.arguments objectAtIndex:1] doubleValue];
      [marker setRotation:degrees];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


-(void)setAnimation:(CDVInvokedUrlCommand *)command
{

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.objects objectForKey:markerId];

      NSString *animation = [command.arguments objectAtIndex:1];

      CDVPluginResult* successResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self setMarkerAnimation_:animation marker:marker pluginResult:successResult callbackId:command.callbackId];
  }];
}

-(void)setMarkerAnimation_:(NSString *)animation marker:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId {
  
    animation = [animation uppercaseString];
    SWITCH(animation) {
        CASE (@"DROP") {
            [self setDropAnimation_:marker pluginResult:pluginResult callbackId:callbackId];
            break;
        }
        CASE (@"BOUNCE") {
            [self setBounceAnimation_:marker pluginResult:pluginResult callbackId:callbackId];
            break;
        }
        DEFAULT {
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
            break;
        }
    }
}

/**
 * set animation
 * (memo) http://stackoverflow.com/a/19316475/697856
 * (memo) http://qiita.com/edo_m18/items/4309d01b67ee42c35b3c
 * (memo) http://stackoverflow.com/questions/12164049/animationdidstop-for-group-animation
 */
-(void)setDropAnimation_:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId {
    int duration = 1;

    CAKeyframeAnimation *longitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"longitude"];
    CAKeyframeAnimation *latitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"latitude"];

    GMSProjection *projection = self.mapCtrl.map.projection;
    CGPoint point = [projection pointForCoordinate:marker.position];
    double distance = point.y ;

    NSMutableArray *latitudePath = [NSMutableArray array];
    NSMutableArray *longitudeath = [NSMutableArray array];
    CLLocationCoordinate2D startLatLng;

    point.y = 0;
    for (double i = 0.75f; i > 0; i-= 0.25f) {
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance;
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance - distance * (i - 0.25f);
    }
    longitudeAnim.values = longitudeath;
    latitudeAnim.values = latitudePath;

    CAAnimationGroup *group = [[CAAnimationGroup alloc] init];
    group.animations = @[longitudeAnim, latitudeAnim];
    group.duration = duration;
    [group setCompletionBlock:^(void){
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }];

    [marker.layer addAnimation:group forKey:@"dropMarkerAnim"];

}
-(void)setBounceAnimation_:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId
{
    /**
     * Marker drop animation
     */
    int duration = 1;

    CAKeyframeAnimation *longitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"longitude"];
    CAKeyframeAnimation *latitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"latitude"];

    GMSProjection *projection = self.mapCtrl.map.projection;
    CGPoint point = [projection pointForCoordinate:marker.position];
    double distance = point.y;

    NSMutableArray *latitudePath = [NSMutableArray array];
    NSMutableArray *longitudeath = [NSMutableArray array];
    CLLocationCoordinate2D startLatLng;

    point.y = distance * 0.5f;

    for (double i = 0.5f; i > 0; i-= 0.15f) {
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance;
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance - distance * (i - 0.15f);
    }
    longitudeAnim.values = longitudeath;
    latitudeAnim.values = latitudePath;

    CAAnimationGroup *group = [[CAAnimationGroup alloc] init];
    group.animations = @[longitudeAnim, latitudeAnim];
    group.duration = duration;
    [group setCompletionBlock:^(void){
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }];

    [marker.layer addAnimation:group forKey:@"bounceMarkerAnim"];
}

/**
 * @private
 * Load the icon; then set to the marker
 */
-(void)setIcon_:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty
   pluginResult:(CDVPluginResult *)pluginResult
     callbackId:(NSString*)callbackId {

    if (self.mapCtrl.debuggable) {
        NSLog(@"---- setIcon_");
    }
    NSString *iconPath = nil;
    CGFloat width = 0;
    CGFloat height = 0;

    // `url` property
    iconPath = [iconProperty valueForKey:@"url"];

    // `size` property
    if ([iconProperty valueForKey:@"size"]) {
        NSDictionary *size = [iconProperty valueForKey:@"size"];
        width = [[size objectForKey:@"width"] floatValue];
        height = [[size objectForKey:@"height"] floatValue];
    }

    // `animation` property
    NSString *animationValue = nil;
    if ([iconProperty valueForKey:@"animation"]) {
        animationValue = [iconProperty valueForKey:@"animation"];
    }
    __block NSString *animation = animationValue;

    if (iconPath) {

        if (self.mapCtrl.debuggable) {
            NSLog(@"iconPath = %@", iconPath);
        }

        NSRange range = [iconPath rangeOfString:@"http"];
        if (range.location != 0) {
            /**
             * Load icon from file or Base64 encoded strings
             */

            UIImage *image;
            if ([iconPath rangeOfString:@"data:image/"].location != NSNotFound &&
                [iconPath rangeOfString:@";base64,"].location != NSNotFound) {

                /**
                 * Base64 icon
                 */
                NSArray *tmp = [iconPath componentsSeparatedByString:@","];

                NSData *decodedData = [NSData dataFromBase64String:tmp[1]];
                image = [[UIImage alloc] initWithData:decodedData];
                if (width && height) {
                    image = [image resize:width height:height];
                }

            } else {
                /**
                 * Load the icon from local path
                 */

                range = [iconPath rangeOfString:@"cdvfile://"];
                if (range.location != NSNotFound) {

                    iconPath = [PluginUtil getAbsolutePathFromCDVFilePath:self.webView cdvFilePath:iconPath];

                    if (iconPath == nil) {
                        if (self.mapCtrl.debuggable) {
                            NSLog(@"(debug)Can not convert '%@' to device full path.", iconPath);
                        }
                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        return;
                    }
                }

                range = [iconPath rangeOfString:@"://"];
                if (range.location == NSNotFound) {

                    range = [iconPath rangeOfString:@"/"];
                    if (range.location != 0) {
                      // Get the current URL, then calculate the relative path.
                      CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
                      NSString *currentURL = ((UIWebView *)(cdvViewController.webView)).request.URL.absoluteString;
                      currentURL = [currentURL stringByDeletingLastPathComponent];
                      currentURL = [currentURL stringByReplacingOccurrencesOfString:@"file:" withString:@""];
                      currentURL = [currentURL stringByReplacingOccurrencesOfString:@"//" withString:@"/"];
                      iconPath = [NSString stringWithFormat:@"file://%@/%@", currentURL, iconPath];
                    } else {
                      iconPath = [NSString stringWithFormat:@"file://%@", iconPath];
                    }
                }


                range = [iconPath rangeOfString:@"file://"];
                if (range.location != NSNotFound) {
                    iconPath = [iconPath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
                    NSFileManager *fileManager = [NSFileManager defaultManager];
                    if (![fileManager fileExistsAtPath:iconPath]) {
                        //if (self.mapCtrl.debuggable) {
                            NSLog(@"(error)There is no file at '%@'.", iconPath);
                        //}
                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        return;
                    }
                }

                image = [UIImage imageNamed:iconPath];

                if (width && height) {
                    image = [image resize:width height:height];
                }
            }

            dispatch_async(dispatch_get_main_queue(), ^{
                marker.icon = image;

                CGFloat anchorX = 0;
                CGFloat anchorY = 0;
                // The `anchor` property for the icon
                if ([iconProperty valueForKey:@"anchor"]) {
                    NSArray *points = [iconProperty valueForKey:@"anchor"];
                    anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                    anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                    marker.groundAnchor = CGPointMake(anchorX, anchorY);
                }

                // The `infoWindowAnchor` property
                if ([iconProperty valueForKey:@"infoWindowAnchor"]) {
                    NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
                    anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                    anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                    marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
                }


                // The `visible` property
                if (iconProperty[@"visible"]) {
                    marker.map = self.mapCtrl.map;
                }

                if (animation) {
                    // Do animation, then send the result
                    [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:callbackId];
                } else {
                    // Send the result
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                }
            });
        } else {
            if (self.mapCtrl.debuggable) {
                NSLog(@"---- Load the icon from over the internet");
            }
            /***
             * Load the icon from over the internet
             */

            dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0ul);
            dispatch_async(queue, ^{

                NSURL *url = [NSURL URLWithString:iconPath];

                [self downloadImageWithURL:url  completionBlock:^(BOOL succeeded, UIImage *image) {

                    if (!succeeded) {

                        // The `visible` property
                        if ([[iconProperty valueForKey:@"visible"] boolValue]) {
                            marker.map = self.mapCtrl.map;
                        }

                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        return;
                    }

                    if (width && height) {
                        image = [image resize:width height:height];
                    }

                    dispatch_async(dispatch_get_main_queue(), ^{
                        marker.icon = image;

                        // The `anchor` property for the icon
                        if ([iconProperty valueForKey:@"anchor"]) {
                            NSArray *points = [iconProperty valueForKey:@"anchor"];
                            CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                            CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                            marker.groundAnchor = CGPointMake(anchorX, anchorY);
                        }


                        // The `infoWindowAnchor` property
                        if ([iconProperty valueForKey:@"infoWindowAnchor"]) {
                            NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
                            CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                            CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                            marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
                        }

                        // The `visible` property
                        if ([[iconProperty valueForKey:@"visible"] boolValue]) {
                            marker.map = self.mapCtrl.map;
                        }


                        if (animation) {
                            // Do animation, then send the result
                            if (self.mapCtrl.debuggable) {
                                NSLog(@"---- do animation animation = %@", animation);
                            }
                            [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:callbackId];
                        } else {
                            // Send the result
                            if (self.mapCtrl.debuggable) {
                                NSLog(@"---- no marker animation");
                            }
                            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        }

                    });


                }];

            });


        }
    } else if ([iconProperty valueForKey:@"iconColor"]) {
        dispatch_async(dispatch_get_main_queue(), ^{
            UIColor *iconColor = [iconProperty valueForKey:@"iconColor"];
            marker.icon = [GMSMarker markerImageWithColor:iconColor];

            // The `visible` property
            if (iconProperty[@"visible"]) {
                marker.map = self.mapCtrl.map;
            }

            if (animation) {
                // Do animation, then send the result
                [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:callbackId];
            } else {
                // Send the result
                [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
            }
        });

    }

}

- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock
{
    [self.executeQueue addOperationWithBlock:^{
  
        NSURLRequest *req = [NSURLRequest requestWithURL:url
                                          cachePolicy:NSURLRequestReturnCacheDataElseLoad
                                          timeoutInterval:5];
        NSCachedURLResponse *cachedResponse = [[NSURLCache sharedURLCache] cachedResponseForRequest:req];
        if (cachedResponse != nil) {
          UIImage *image = [[UIImage alloc] initWithData:cachedResponse.data];
          completionBlock(YES, image);
          return;
        }

        NSString *uniqueKey = url.absoluteString;
        NSData *cache = [self.imgCache objectForKey:uniqueKey];
        if (cache != nil) {
            UIImage *image = [[UIImage alloc] initWithData:cache];
            completionBlock(YES, image);
            return;
        }



        [NSURLConnection sendAsynchronousRequest:req
              queue:self.executeQueue
              completionHandler:^(NSURLResponse *res, NSData *data, NSError *error) {
                if ( !error ) {
                  [self.imgCache setObject:data forKey:uniqueKey cost:data.length];
                  UIImage *image = [UIImage imageWithData:data];
                  completionBlock(YES, image);
                } else {
                  completionBlock(NO, nil);
                }

        }];
    }];
}
@end
