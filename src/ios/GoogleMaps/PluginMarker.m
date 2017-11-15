//
//  PluginMarker.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
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
  if (self.initialized) {
    return;
  }
  self.initialized = YES;
  [super pluginInitialize];
  // Initialize this plugin
}

- (void)pluginUnload
{

  // Plugin destroy
  NSArray *keys = [self.mapCtrl.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
    key = [keys objectAtIndex:i];
    if ([key hasPrefix:@"marker_"] &&
        ![key hasPrefix:@"marker_icon_"] &&
        ![key hasPrefix:@"marker_property"]) {
      GMSMarker *marker = (GMSMarker *)[self.mapCtrl.objects objectForKey:key];
      if (marker) {
        marker.userData = nil;
        marker.map = nil;
        [marker.layer removeAllAnimations];
      }
      marker = nil;
    }
    [self.mapCtrl.objects removeObjectForKey:key];
  }

  [[UIImageCache sharedInstance] removeAllCachedImages];
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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSDictionary *json = [command.arguments objectAtIndex:1];

    __block NSMutableDictionary *createResult = [[NSMutableDictionary alloc] init];
    NSString *markerId = [NSString stringWithFormat:@"marker_%lu%d", command.hash, arc4random() % 100000];
    [createResult setObject:markerId forKey:@"id"];

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      CDVCommandDelegateImpl *cmdDelegate = (CDVCommandDelegateImpl *)self.commandDelegate;
      [self _create:markerId markerOptions:json callbackBlock:^(BOOL successed, id result) {
        CDVPluginResult* pluginResult;
        if (successed == NO) {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:result];
        } else {
          pluginResult  = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:createResult ];
        }
        [cmdDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

    }];
  }];

}
- (void)_create:(NSString *)markerId markerOptions:(NSDictionary *)json callbackBlock:(void (^)(BOOL successed, id resultObj)) callbackBlock {

  if ([NSThread isMainThread] == NO) {
    callbackBlock(NO, @"PluginMarker._create() must run on the main thread.");
    return;
  }

  NSMutableDictionary *iconProperty = nil;
  NSString *animation = nil;
  NSDictionary *latLng = [json objectForKey:@"position"];
  float latitude = [[latLng valueForKey:@"lat"] floatValue];
  float longitude = [[latLng valueForKey:@"lng"] floatValue];

  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);

  // Create a marker
  GMSMarker *marker = [GMSMarker markerWithPosition:position];
  marker.tracksViewChanges = NO;
  marker.tracksInfoWindowChanges = NO;

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

  [self.mapCtrl.objects setObject:marker forKey: markerId];
  marker.userData = markerId;

  // Custom properties
  NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
  NSString *propertyId = [NSString stringWithFormat:@"marker_property_%@", markerId];

  if ([json valueForKey:@"styles"]) {
    NSDictionary *styles = [json valueForKey:@"styles"];
    [properties setObject:styles forKey:@"styles"];
  }

  BOOL disableAutoPan = NO;
  if ([json valueForKey:@"disableAutoPan"] != nil) {
    disableAutoPan = [[json valueForKey:@"disableAutoPan"] boolValue];
  }
  [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];

  [self.mapCtrl.objects setObject:properties forKey: propertyId];

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
  if (json[@"visible"]) {
    [iconProperty setObject:json[@"visible"] forKey:@"visible"];
  } else {
    [iconProperty setObject:[NSNumber numberWithBool:true] forKey:@"visible"];
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
    [self setIcon_:marker iconProperty:iconProperty callbackBlock:callbackBlock];
  } else {
    if (json[@"visible"]) {
      marker.map = self.mapCtrl.map;
    }


    if (animation) {
      [self setIcon_:marker iconProperty:iconProperty callbackBlock:callbackBlock];
    } else {
      callbackBlock(YES, marker);
    }
  }
}

/**
 * Show the infowindow of the current marker
 * @params markerId
 */
-(void)showInfoWindow:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *hashCode = [command.arguments objectAtIndex:0];

      GMSMarker *marker = [self.mapCtrl.objects objectForKey:hashCode];
      if (marker) {
        self.mapCtrl.map.selectedMarker = marker;
        self.mapCtrl.activeMarker = marker;
      }

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
/**
 * Hide current infowindow
 * @params markerId
 */
-(void)hideInfoWindow:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      self.mapCtrl.map.selectedMarker = nil;
      self.mapCtrl.activeMarker = nil;
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
/**
 * @params markerId
 * @return current marker position with array(latitude, longitude)
 */
-(void)getPosition:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *markerId = [command.arguments objectAtIndex:0];

    GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
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
    [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set title to the specified marker
 * @params markerId
 */
-(void)setTitle:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      marker.title = [command.arguments objectAtIndex:1];

      NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.userData];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];



      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}


/**
 * Set title to the specified marker
 * @params markerId
 */
-(void)setSnippet:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      marker.snippet = [command.arguments objectAtIndex:1];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Remove the specified marker
 * @params markerId
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      [self.mapCtrl.objects removeObjectForKey:markerId];
      [self _removeMarker:marker];
      marker = nil;

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

-(void)_removeMarker:(GMSMarker *)marker {
  if (marker == nil || marker.userData == nil) {
    return;
  }

  @synchronized (self.mapCtrl.objects) {
    NSString *iconCacheKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];
    NSString *propertyId = [NSString stringWithFormat:@"marker_property_%@", marker.userData];
    [self.mapCtrl.objects objectForKey: marker.userData];
    //[self.mapCtrl.objects removeObjectForKey:iconCacheKey];
    [self.mapCtrl.objects removeObjectForKey:propertyId];
    marker.map = nil;
    marker = nil;

    [self.mapCtrl.executeQueue addOperationWithBlock:^{
      @synchronized(self.mapCtrl.objects) {
        if ([self.mapCtrl.objects objectForKey:iconCacheKey]) {

          NSString *cacheKey = [self.mapCtrl.objects objectForKey:iconCacheKey];

          if ([[UIImageCache sharedInstance].iconCacheKeys objectForKey:cacheKey]) {
            int count = [[[UIImageCache sharedInstance].iconCacheKeys objectForKey:cacheKey] intValue];
            count--;
            if (count < 1) {
              [[UIImageCache sharedInstance] removeCachedImageForKey:cacheKey];
              [[UIImageCache sharedInstance].iconCacheKeys removeObjectForKey:cacheKey];
            } else {
              [[UIImageCache sharedInstance].iconCacheKeys setObject:[NSNumber numberWithInt:count] forKey:cacheKey];
            }
          }

          if ([self.mapCtrl.objects objectForKey:iconCacheKey]) {
            [self.mapCtrl.objects removeObjectForKey:iconCacheKey];
          }
        }
      }
    }];
  }
}

/**
 * Set anchor of the marker
 * @params markerId
 */
-(void)setIconAnchor:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *markerId = [command.arguments objectAtIndex:0];
    GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
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
    [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set anchor of the info window
 * @params markerId
 */
-(void)setInfoWindowAnchor:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *markerId = [command.arguments objectAtIndex:0];
    GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];

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
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];

  }];
}


/**
 * Set opacity
 * @params markerId
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      marker.opacity = [[command.arguments objectAtIndex:1] floatValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set zIndex
 * @params markerId
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      marker.zIndex = [[command.arguments objectAtIndex:1] intValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set draggable
 * @params markerId
 */
-(void)setDraggable:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
      [marker setDraggable:isEnabled];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set disable auto pan
 * @params markerId
 */
-(void)setDisableAutoPan:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *markerId = [command.arguments objectAtIndex:0];
    GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
    BOOL disableAutoPan = [[command.arguments objectAtIndex:1] boolValue];

    NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.userData];
    NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                       [self.mapCtrl.objects objectForKey:propertyId]];
    [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
    [self.mapCtrl.objects setObject:properties forKey:propertyId];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set visibility
 * @params markerId
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{

      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

      if (isVisible) {
        marker.map = self.mapCtrl.map;
      } else {
        marker.map = nil;
      }

      NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.userData];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"visible"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set position
 * @params key
 */
-(void)setPosition:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *markerId = [command.arguments objectAtIndex:0];
    GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];

    float latitude = [[command.arguments objectAtIndex:1] floatValue];
    float longitude = [[command.arguments objectAtIndex:2] floatValue];
    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [marker setPosition:position];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set flattable
 * @params markerId
 */
-(void)setFlat:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      Boolean isFlat = [[command.arguments objectAtIndex:1] boolValue];
      [marker setFlat: isFlat];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * set icon
 * @params markerId
 */
-(void)setIcon:(CDVInvokedUrlCommand *)command
{

  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *markerId = [command.arguments objectAtIndex:0];
    GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
    if (marker == nil) {
      NSLog(@"--> can not find the maker : %@", markerId);
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                        messageAsString:[NSString stringWithFormat:@"Cannot find the marker : %@", markerId]];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      return;
    }

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

    CDVCommandDelegateImpl *cmdDelegate = (CDVCommandDelegateImpl *)self.commandDelegate;
    [self setIcon_:marker iconProperty:iconProperty callbackBlock:^(BOOL successed, id resultObj) {
      CDVPluginResult* pluginResult;
      if (successed == NO) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:resultObj];
      } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      }
      [cmdDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
/**
 * set rotation
 */
-(void)setRotation:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];

      CLLocationDegrees degrees = [[command.arguments objectAtIndex:1] doubleValue];
      [marker setRotation:degrees];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}


-(void)setAnimation:(CDVInvokedUrlCommand *)command
{

  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSString *markerId = [command.arguments objectAtIndex:0];
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];

      NSString *animation = [command.arguments objectAtIndex:1];
      CDVCommandDelegateImpl *cmdDelegate = (CDVCommandDelegateImpl *)self.commandDelegate;

      [self setMarkerAnimation_:animation marker:marker callbackBlock:^(void) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [cmdDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
    }];
  }];
}

-(void)setMarkerAnimation_:(NSString *)animation marker:(GMSMarker *)marker callbackBlock:(void (^)()) callbackBlock {

  animation = [animation uppercaseString];
  SWITCH(animation) {
    CASE (@"DROP") {
      [self setDropAnimation_:marker callbackBlock:callbackBlock];
      break;
    }
    CASE (@"BOUNCE") {
      [self setBounceAnimation_:marker callbackBlock:callbackBlock];
      break;
    }
    DEFAULT {
      callbackBlock();
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
-(void)setDropAnimation_:(GMSMarker *)marker callbackBlock:(void (^)()) callbackBlock {
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
  [group setCompletionBlock: callbackBlock];

  [marker.layer addAnimation:group forKey:@"dropMarkerAnim"];

}
-(void)setBounceAnimation_:(GMSMarker *)marker callbackBlock:(void (^)()) callbackBlock {
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
  [group setCompletionBlock: callbackBlock];

  [marker.layer addAnimation:group forKey:@"bounceMarkerAnim"];
}

/**
 * @private
 * Load the icon; then set to the marker
 */

-(void)setIcon_:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty callbackBlock:(void (^)(BOOL successed, id resultObj)) callbackBlock {

  if (marker == nil) {
    callbackBlock(NO, @"marker is null");
    return;
  }

  NSString *iconPath = nil;
  CGFloat width = -1;
  CGFloat height = -1;

  // `url` property
  iconPath = [iconProperty valueForKey:@"url"];
  if (self.mapCtrl.debuggable) {
    NSLog(@"---- setIcon_ : %@", iconPath);
  }

  // `animation` property
  NSString *animationValue = nil;
  if ([iconProperty valueForKey:@"animation"]) {
    animationValue = [iconProperty valueForKey:@"animation"];
  }
  __block NSString *animation = animationValue;

  //--------------------------------
  // the icon property is color name
  //--------------------------------
  if ([iconProperty valueForKey:@"iconColor"]) {
    dispatch_async(dispatch_get_main_queue(), ^{
      UIColor *iconColor = [iconProperty valueForKey:@"iconColor"];
      marker.icon = [GMSMarker markerImageWithColor:iconColor];

      // The `visible` property
      if (iconProperty[@"visible"]) {
        marker.map = self.mapCtrl.map;
      }

      if (animation) {
        // Do animation, then send the result
        [self setMarkerAnimation_:animation marker:marker callbackBlock:^(void) {
          callbackBlock(YES, marker);
        }];
      } else {
        // Send the result
        callbackBlock(YES, marker);
      }
    });
    return;
  }

  if (iconPath == nil) {
    callbackBlock(YES, marker);
    //callbackBlock(NO, @"icon property is null");
    return;
  }

  // `size` property
  if ([iconProperty valueForKey:@"size"]) {
    NSDictionary *size = [iconProperty valueForKey:@"size"];
    width = [[size objectForKey:@"width"] floatValue];
    height = [[size objectForKey:@"height"] floatValue];
  }





  if (self.mapCtrl.debuggable) {
    NSLog(@"iconPath = %@", iconPath);
  }
  NSString *iconCacheKey = [NSString stringWithFormat:@"%lu/%d,%d", (unsigned long)iconPath.hash, (int)width, (int)height];
  UIImage *image = [[UIImageCache sharedInstance] getCachedImageForKey:iconCacheKey];



  //---------------------------------------
  // If the image is cached, return it
  //---------------------------------------
  if (image != nil) {

    NSString *iconKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];

    // Cache the icon image
    if ([self.mapCtrl.objects objectForKey:iconKey]) {
      NSString *currentCacheKey = [self.mapCtrl.objects objectForKey:iconKey];
      if (currentCacheKey && ![iconCacheKey isEqualToString:currentCacheKey]) {

        int count = [[[UIImageCache sharedInstance].iconCacheKeys objectForKey:currentCacheKey] intValue];
        count--;
        if (count < 1) {
          [[UIImageCache sharedInstance] removeCachedImageForKey:currentCacheKey];
          [[UIImageCache sharedInstance].iconCacheKeys removeObjectForKey:currentCacheKey];
        } else {
          [[UIImageCache sharedInstance].iconCacheKeys setObject:[NSNumber numberWithInt:count] forKey:currentCacheKey];
        }
        //NSLog(@"---> currentKey = %@, count = %d", iconKey, count);
      }

    }

    [self.mapCtrl.objects setObject:iconCacheKey forKey:iconKey];
    int count = [[[UIImageCache sharedInstance].iconCacheKeys objectForKey:iconCacheKey] intValue];
    count++;
    [[UIImageCache sharedInstance].iconCacheKeys setObject:[NSNumber numberWithInt:count] forKey:iconCacheKey];

    //NSLog(@"--->iconCacheKey = %@, count = %d", iconCacheKey, count);

    if ([iconProperty objectForKey:@"label"]) {
      image = [self drawLabel:image labelOptions:[iconProperty objectForKey:@"label"]];
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
        [self setMarkerAnimation_:animation marker:marker callbackBlock:^(void) {
          callbackBlock(YES, marker);
        }];
      } else {
        // Send the result
        callbackBlock(YES, marker);
      }
    });

    return;
  }

  //
  // Load icon from file or Base64 encoded strings
  //

  if ([iconPath rangeOfString:@"data:image/"].location != NSNotFound &&
      [iconPath rangeOfString:@";base64,"].location != NSNotFound) {

    NSArray *tmp = [iconPath componentsSeparatedByString:@","];

    NSData *decodedData = [NSData dataFromBase64String:tmp[1]];
    image = [[UIImage alloc] initWithData:decodedData];
    if (width && height) {
      image = [image resize:width height:height];
    }
  }

  NSRange range;
  if (image == nil && ![iconPath hasPrefix:@"http"]) {

    if (![iconPath containsString:@"://"] &&
        ![iconPath hasPrefix:@"/"] &&
        ![iconPath hasPrefix:@"www"] &&
        ![iconPath hasPrefix:@"./"] &&
        ![iconPath hasPrefix:@"../"]) {
      iconPath = [NSString stringWithFormat:@"./%@", iconPath];
    }

    if ([iconPath hasPrefix:@"./"] || [iconPath hasPrefix:@"../"]) {
      NSError *error = nil;

      // replace repeated "./" (i.e ./././test.png)
      NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"(\\.\\/)+" options:NSRegularExpressionCaseInsensitive error:&error];
      iconPath = [regex stringByReplacingMatchesInString:iconPath options:0 range:NSMakeRange(0, [iconPath length]) withTemplate:@"./"];


      // Get the current URL, then calculate the relative path.
      CDVViewController *cdvViewController = (CDVViewController*)self.viewController;

      id webview = cdvViewController.webView;
      NSString *clsName = [webview className];
      NSURL *url;
      if ([clsName isEqualToString:@"UIWebView"]) {
        url = ((UIWebView *)cdvViewController.webView).request.URL;
        NSString *currentURL = url.absoluteString;

        // remove page unchor (i.e index.html#page=test, index.html?key=value)
        regex = [NSRegularExpression regularExpressionWithPattern:@"[#\\?].*$" options:NSRegularExpressionCaseInsensitive error:&error];
        currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];

        // remove file name (i.e /index.html)
        regex = [NSRegularExpression regularExpressionWithPattern:@"\\/[^\\/]+\\.[^\\/]+$" options:NSRegularExpressionCaseInsensitive error:&error];
        currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];

        if (![currentURL hasSuffix:@"/"]) {
          currentURL = [NSString stringWithFormat:@"%@/", currentURL];
        }
        iconPath = [NSString stringWithFormat:@"%@%@", currentURL, iconPath];

        // remove file name (i.e /index.html)
        regex = [NSRegularExpression regularExpressionWithPattern:@"(\\/\\.\\/+)+" options:NSRegularExpressionCaseInsensitive error:&error];
        iconPath = [regex stringByReplacingMatchesInString:iconPath options:0 range:NSMakeRange(0, [iconPath length]) withTemplate:@"/"];

        iconPath = [iconPath stringByReplacingOccurrencesOfString:@"%20" withString:@" "];

        if (self.mapCtrl.debuggable) {
          NSLog(@"iconPath = %@", iconPath);
        }
      } else {
        //------------------------------------------
        // WKWebView URL is use http:// always
        //------------------------------------------

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          NSURL *url = [webview URL];
          NSString *currentURL = url.absoluteString;
          if (![[url lastPathComponent] isEqualToString:@"/"]) {
            currentURL = [currentURL stringByDeletingLastPathComponent];
          }
          //url = [NSURL URLWithString:[NSString stringWithFormat:@"%@/%@", currentURL, iconPath]];

          //
          // Load the icon from over the internet
          //
          [self.mapCtrl.executeQueue addOperationWithBlock:^{

            NSURL *url = [NSURL URLWithString:[NSString stringWithFormat:@"%@/%@", currentURL, iconPath]];

            [self downloadImageWithURL:url  completionBlock:^(BOOL succeeded, UIImage *image) {

              if (!succeeded) {
                NSLog(@"[fail] url = %@", url);
                // The `visible` property
                if (iconProperty[@"visible"]) {
                  marker.map = self.mapCtrl.map;
                }
                if ([[UIImageCache sharedInstance].iconCacheKeys objectForKey:iconCacheKey]) {
                  [[UIImageCache sharedInstance].iconCacheKeys removeObjectForKey:iconCacheKey];
                }

                callbackBlock(NO, [NSString stringWithFormat:@"Can not load image from '%@'.", url]);
                return;
              }


              if (self.mapCtrl.debuggable) {
                NSLog(@"[success] url = %@", url);
              }

              if (width && height) {
                image = [image resize:width height:height];
              }

              // Cache the icon image
              NSString *iconKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];
              [[UIImageCache sharedInstance] cacheImage:image forKey:iconCacheKey];
              [self.mapCtrl.objects setObject:iconCacheKey forKey:iconKey];
              [[UIImageCache sharedInstance].iconCacheKeys setObject:[NSNumber numberWithInt:1] forKey:iconCacheKey];;
              //NSLog(@"--->confirm: key: %@, iconCacheKey : %@", iconKey, [self.mapCtrl.objects objectForKey:iconKey]);

              // Draw label
              if ([iconProperty objectForKey:@"label"]) {
                image = [self drawLabel:image labelOptions:[iconProperty objectForKey:@"label"]];
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
                if (iconProperty[@"visible"]) {
                  marker.map = self.mapCtrl.map;
                }


                if (animation) {
                  // Do animation, then send the result
                  [self setMarkerAnimation_:animation marker:marker callbackBlock:^(void) {
                    callbackBlock(YES, marker);
                  }];
                } else {
                  // Send the result
                  callbackBlock(YES, marker);
                }

              });


            }];

          }];




        }];

        return;
      }
    }


    range = [iconPath rangeOfString:@"cdvfile://"];
    if ([iconPath hasPrefix:@"cdvfile://"]) {

      iconPath = [PluginUtil getAbsolutePathFromCDVFilePath:self.webView cdvFilePath:iconPath];

      if (iconPath == nil) {
        if (self.mapCtrl.debuggable) {
          NSLog(@"(debug)Can not convert '%@' to device full path.", iconPath);
        }
        //[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        callbackBlock(NO, [NSString stringWithFormat:@"Can not convert '%@' to device full path.", iconPath]);
        return;
      }
    }


    range = [iconPath rangeOfString:@"://"];

    if (range.location == 0) {
      /**
       * Load the icon from local path
       */

      if ([iconPath hasPrefix:@"/"]) {
        // Get the current URL, then calculate the relative path.
        CDVViewController *cdvViewController = (CDVViewController*)self.viewController;

        id webview = cdvViewController.webView;
        NSString *clsName = [webview className];
        NSURL *url;
        if ([clsName isEqualToString:@"UIWebView"]) {
          url = ((UIWebView *)cdvViewController.webView).request.URL;
        } else {
          url = [webview URL];
        }
        NSString *currentURL = url.absoluteString;
        currentURL = [currentURL stringByDeletingLastPathComponent];
        currentURL = [currentURL stringByReplacingOccurrencesOfString:@"file:" withString:@""];
        currentURL = [currentURL stringByReplacingOccurrencesOfString:@"//" withString:@"/"];
        currentURL = [currentURL stringByReplacingOccurrencesOfString:@"%20" withString:@" "];
        iconPath = [NSString stringWithFormat:@"file://%@/%@", currentURL, iconPath];
      } else {
        iconPath = [NSString stringWithFormat:@"file://%@", iconPath];
      }
    }

    if ([iconPath hasPrefix:@"file://"]) {
      iconPath = [iconPath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
      NSFileManager *fileManager = [NSFileManager defaultManager];
      if (![fileManager fileExistsAtPath:iconPath]) {
        //if (self.mapCtrl.debuggable) {
        NSLog(@"(error)There is no file at '%@'.", iconPath);
        //}
        //[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        callbackBlock(NO, [NSString stringWithFormat:@"There is no file at '%@'.", iconPath]);
        return;
      }
    }

    image = [UIImage imageNamed:iconPath];
  }

  if (image != nil) {

    if (width && height) {
      image = [image resize:width height:height];
    }



    // Cache the icon image
    NSString *iconKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];
    if ([self.mapCtrl.objects objectForKey:iconKey]) {
      NSString *currentCacheKey = [self.mapCtrl.objects objectForKey:iconKey];
      if (currentCacheKey && ![iconCacheKey isEqualToString:currentCacheKey]) {

        int count = [[[UIImageCache sharedInstance].iconCacheKeys objectForKey:currentCacheKey] intValue];
        count--;
        if (count < 1) {
          [[UIImageCache sharedInstance] removeCachedImageForKey:currentCacheKey];
          [[UIImageCache sharedInstance].iconCacheKeys removeObjectForKey:currentCacheKey];
        } else {
          [[UIImageCache sharedInstance].iconCacheKeys setObject:[NSNumber numberWithInt:count] forKey:currentCacheKey];
        }
        //NSLog(@"---> currentKey = %@, count = %d", iconKey, count);
      }

    }

    [[UIImageCache sharedInstance] cacheImage:image forKey:iconCacheKey];
    [self.mapCtrl.objects setObject:iconCacheKey forKey:iconKey];
    [[UIImageCache sharedInstance].iconCacheKeys setObject:[NSNumber numberWithInt:1] forKey:iconCacheKey];
    //NSLog(@"--->(save)iconCacheKey: key: %@ = %d", iconCacheKey, [[[UIImageCache sharedInstance].iconCacheKeys objectForKey:iconCacheKey] intValue]);


    //NSString *iconKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];
    //[self.mapCtrl.objects setObject:iconCacheKey forKey:iconKey];




    if ([iconProperty objectForKey:@"label"]) {
      image = [self drawLabel:image labelOptions:[iconProperty objectForKey:@"label"]];
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
        [self setMarkerAnimation_:animation marker:marker callbackBlock:^(void) {
          callbackBlock(YES, marker);
        }];
      } else {
        // Send the result
        callbackBlock(YES, marker);
      }
    });

    return;
  }




  if (self.mapCtrl.debuggable) {
    NSLog(@"---- Load the icon from over the internet");
  }

  //
  // Load the icon from over the internet
  //

  dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0ul);
  dispatch_async(queue, ^{

    NSURL *url = [NSURL URLWithString:iconPath];

    [self downloadImageWithURL:url  completionBlock:^(BOOL succeeded, UIImage *image) {

      if (!succeeded) {
        NSLog(@"[fail] url = %@", url);
        // The `visible` property
        if (iconProperty[@"visible"]) {
          marker.map = self.mapCtrl.map;
        }
        if ([[UIImageCache sharedInstance].iconCacheKeys objectForKey:iconCacheKey]) {
          [[UIImageCache sharedInstance].iconCacheKeys removeObjectForKey:iconCacheKey];
        }

        callbackBlock(NO, [NSString stringWithFormat:@"Can not load image from '%@'.", url]);
        return;
      }


      if (self.mapCtrl.debuggable) {
        NSLog(@"[success] url = %@", url);
      }

      if (width && height) {
        image = [image resize:width height:height];
      }

      // Cache the icon image
      NSString *iconKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];
      [[UIImageCache sharedInstance] cacheImage:image forKey:iconCacheKey];
      [self.mapCtrl.objects setObject:iconCacheKey forKey:iconKey];
      [[UIImageCache sharedInstance].iconCacheKeys setObject:[NSNumber numberWithInt:1] forKey:iconCacheKey];;
      //NSLog(@"--->confirm: key: %@, iconCacheKey : %@", iconKey, [self.mapCtrl.objects objectForKey:iconKey]);

      // Draw label
      if ([iconProperty objectForKey:@"label"]) {
        image = [self drawLabel:image labelOptions:[iconProperty objectForKey:@"label"]];
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
        if (iconProperty[@"visible"]) {
          marker.map = self.mapCtrl.map;
        }


        if (animation) {
          // Do animation, then send the result
          [self setMarkerAnimation_:animation marker:marker callbackBlock:^(void) {
            callbackBlock(YES, marker);
          }];
        } else {
          // Send the result
          callbackBlock(YES, marker);
        }

      });


    }];

  });

}

- (UIImage *)drawLabel:(UIImage *) image labelOptions:(NSDictionary *)labelOptions {
  NSString *text = [NSString stringWithFormat:@"%@", [labelOptions objectForKey:@"text"]];
  if (text == nil) {
    return image;
  }

  UIFont *font;
  float fontSize = 10;
  BOOL isBold = NO;
  BOOL isItalic = NO;
  UIColor *textColor = UIColor.blackColor;

  if ([labelOptions objectForKey:@"fontSize"]) {
    fontSize = [[labelOptions objectForKey:@"fontSize"] floatValue];
  }
  if ([labelOptions objectForKey:@"bold"]) {
    isBold = [[labelOptions objectForKey:@"bold"] boolValue];
  }
  if ([labelOptions objectForKey:@"italic"]) {
    isItalic = [[labelOptions objectForKey:@"italic"] boolValue];
  }

  if (isBold == YES && isItalic == YES) {
    // ref: http://stackoverflow.com/questions/4713236/how-do-i-set-bold-and-italic-on-uilabel-of-iphone-ipad#21777132
    font = [UIFont systemFontOfSize:17.0f];
    UIFontDescriptor *fontDescriptor = [font.fontDescriptor
                                        fontDescriptorWithSymbolicTraits:UIFontDescriptorTraitBold | UIFontDescriptorTraitItalic];
    font = [UIFont fontWithDescriptor:fontDescriptor size:fontSize];
  } else if (isBold == TRUE && isItalic == FALSE) {
    font = [UIFont boldSystemFontOfSize:fontSize];
  } else if (isBold == TRUE && isItalic == FALSE) {
    font = [UIFont italicSystemFontOfSize:fontSize];
  } else {
    font = [UIFont systemFontOfSize:fontSize];
  }


  if ([labelOptions objectForKey:@"color"]) {
    textColor = [[labelOptions objectForKey:@"color"] parsePluginColor];
  }

  // Calculate the size for the title strings
  CGRect rect = [text
                 boundingRectWithSize: CGSizeMake(image.size.width, image.size.height)
                 options:(NSStringDrawingUsesLineFragmentOrigin|NSStringDrawingUsesFontLeading)
                 attributes:[NSDictionary
                             dictionaryWithObject:font
                             forKey:NSFontAttributeName]
                 context:nil];

  NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
  style.lineBreakMode = NSLineBreakByWordWrapping;
  style.alignment = NSTextAlignmentCenter;


  NSDictionary *attributes = @{
                               NSForegroundColorAttributeName : textColor,
                               NSFontAttributeName : font,
                               NSParagraphStyleAttributeName : style
                               };

  UIGraphicsBeginImageContextWithOptions(image.size, NO, 0.0);
  [image drawInRect:CGRectMake(0,0, image.size.width, image.size.height)];
  [text drawInRect:CGRectMake((image.size.width - rect.size.width) /2 , (image.size.height - rect.size.height) /2, rect.size.width, rect.size.height) withAttributes:attributes];
  UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
  /*
   //(debug)
   CGContextRef context = UIGraphicsGetCurrentContext();
   CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 0.5);
   CGContextFillRect(context, rect);
   */
  UIGraphicsEndImageContext();
  image = nil;


  return newImage;
}

- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{

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
    UIImage *image = [[UIImageCache sharedInstance] getCachedImageForKey:uniqueKey];
    if (image != nil) {
      completionBlock(YES, image);
      return;
    }



    //-------------------------------------------------------------
    // Use NSURLSessionDataTask instead of [NSURLConnection sendAsynchronousRequest]
    // https://stackoverflow.com/a/20871647
    //-------------------------------------------------------------
    NSURLSessionConfiguration *sessionConfiguration = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfiguration];
    NSURLSessionDataTask *getTask = [session dataTaskWithRequest:req
                                               completionHandler:^(NSData *data, NSURLResponse *res, NSError *error) {
                                                 [session finishTasksAndInvalidate];
                                                 if ( !error ) {
                                                   UIImage *image = [UIImage imageWithData:data];
                                                   [[UIImageCache sharedInstance] cacheImage:image forKey:uniqueKey];
                                                   completionBlock(YES, image);
                                                 } else {
                                                   completionBlock(NO, nil);
                                                 }

                                               }];
    [getTask resume];
    //-------------------------------------------------------------
    // NSURLConnection sendAsynchronousRequest is deprecated.
    //-------------------------------------------------------------
    /*
     [NSURLConnection sendAsynchronousRequest:req
     queue:self.mapCtrl.executeQueue
     completionHandler:^(NSURLResponse *res, NSData *data, NSError *error) {
     if ( !error ) {
     [self.icons setObject:data forKey:uniqueKey cost:data.length];
     UIImage *image = [UIImage imageWithData:data];
     completionBlock(YES, image);
     } else {
     completionBlock(NO, nil);
     }

     }];
     */


  }];
}
@end
