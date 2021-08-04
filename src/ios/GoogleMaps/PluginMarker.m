//
//  PluginMarker.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginMarker.h"
@implementation PluginMarker
-(void)setPluginViewController:(PluginViewController *)viewCtrl
{
  self.mapCtrl = (PluginMapViewController *)viewCtrl;
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


  NSString *pluginId = [NSString stringWithFormat:@"%@-marker", self.mapCtrl.overlayId];
  [self.mapCtrl.plugins removeObjectForKey:pluginId];
}

-(id)_getInstance: (NSString *)mapId markerId:(NSString *)markerId {
  
  PluginMap *mapInstance = [CordovaGoogleMaps getViewPlugin:mapId];
  NSString *pluginId;
  if ([markerId containsString:@"-marker_"]) {
    pluginId = [NSString stringWithFormat:@"%@-markercluster", mapId];
  } else {
    pluginId = [NSString stringWithFormat:@"%@-marker", mapId];
  }
  return [mapInstance.mapCtrl.plugins objectForKey:pluginId];
}
/*
 * Create a marker instance
 */
-(void)create:(CDVInvokedUrlCommand *)command
{
  

  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSDictionary *json = [command.arguments objectAtIndex:2];
    NSString *hashCode = [command.arguments objectAtIndex:3];

    __block NSMutableDictionary *createResult = [[NSMutableDictionary alloc] init];
    NSString *markerId = [NSString stringWithFormat:@"marker_%@", hashCode];
    [createResult setObject:markerId forKey:@"__pgmId"];

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      CDVCommandDelegateImpl *cmdDelegate = (CDVCommandDelegateImpl *)self.commandDelegate;
      [self _create:markerId markerOptions:json callbackBlock:^(BOOL successed, id result) {
        CDVPluginResult* pluginResult;

        if (successed == NO) {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:result];
        } else {
          GMSMarker *marker = result;
          NSString *iconCacheKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];
          UIImage *image = [[UIImageCache sharedInstance] getCachedImageForKey:iconCacheKey];
          if (image != nil) {
            [createResult setObject:[NSNumber numberWithInt: (int)image.size.width] forKey:@"width"];
            [createResult setObject:[NSNumber numberWithInt: (int)image.size.height] forKey:@"height"];
          } else {
            [createResult setObject:[NSNumber numberWithInt: 24] forKey:@"width"];
            [createResult setObject:[NSNumber numberWithInt: 40] forKey:@"height"];
          }

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
  double latitude = [[latLng valueForKey:@"lat"] doubleValue];
  double longitude = [[latLng valueForKey:@"lng"] doubleValue];

  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);

  // Create a marker
  GMSMarker *marker = [GMSMarker markerWithPosition:position];
  marker.tracksViewChanges = NO;
  marker.tracksInfoWindowChanges = NO;

  if ([json valueForKey:@"title"] && [json valueForKey:@"title"] != [NSNull null]) {
    [marker setTitle: [json valueForKey:@"title"]];
  }
  if ([json valueForKey:@"snippet"] && [json valueForKey:@"snippet"] != [NSNull null]) {
    [marker setSnippet: [json valueForKey:@"snippet"]];
  }
  if ([json valueForKey:@"draggable"] && [json valueForKey:@"draggable"] != [NSNull null]) {
    [marker setDraggable:[[json valueForKey:@"draggable"] boolValue]];
  }
  if ([json valueForKey:@"flat"] && [json valueForKey:@"flat"] != [NSNull null]) {
    [marker setFlat:[[json valueForKey:@"flat"] boolValue]];
  }
  if ([json valueForKey:@"rotation"] && [json valueForKey:@"rotation"] != [NSNull null]) {
    CLLocationDegrees degrees = [[json valueForKey:@"rotation"] doubleValue];
    [marker setRotation:degrees];
  }
  if ([json valueForKey:@"opacity"] && [json valueForKey:@"opacity"] != [NSNull null]) {
    [marker setOpacity:[[json valueForKey:@"opacity"] floatValue]];
  }
  if ([json valueForKey:@"zIndex"] && [json valueForKey:@"zIndex"] != [NSNull null]) {
    [marker setZIndex:[[json valueForKey:@"zIndex"] intValue]];
  }

  [self.mapCtrl.objects setObject:marker forKey: markerId];
  marker.userData = markerId;

  // Custom properties
  NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
  NSString *propertyId = [NSString stringWithFormat:@"marker_property_%@", markerId];

  if ([json valueForKey:@"styles"] && [json valueForKey:@"styles"] != [NSNull null]) {
    NSDictionary *styles = [json valueForKey:@"styles"];
    [properties setObject:styles forKey:@"styles"];
  }

  BOOL disableAutoPan = NO;
  if ([json valueForKey:@"disableAutoPan"] != nil && [json valueForKey:@"disableAutoPan"] != [NSNull null]) {
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

    id url = [iconProperty objectForKey:@"url"];
    if ([url isKindOfClass:[NSArray class]]) {
      NSArray *rgbColor = url;
      iconProperty = [[NSMutableDictionary alloc] init];
      [iconProperty setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
    }

  } else if ([icon isKindOfClass:[NSArray class]]) {
    NSArray *rgbColor = [json valueForKey:@"icon"];
    iconProperty = [NSMutableDictionary dictionary];
    [iconProperty setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
  }

  // Visible property
  NSString *visibleValue = [NSString stringWithFormat:@"%@",  json[@"visible"]];
  BOOL visible = YES;
  if ([@"0" isEqualToString:visibleValue]) {
    // false
    visible = NO;
    if (iconProperty == nil) {
      iconProperty = [NSMutableDictionary dictionary];
    }
    [iconProperty setObject:[NSNumber numberWithBool:false] forKey:@"visible"];
  } else {
    // default or true
    if (iconProperty == nil) {
      iconProperty = [NSMutableDictionary dictionary];
    }
    [iconProperty setObject:[NSNumber numberWithBool:true] forKey:@"visible"];
  }

  // Animation
  if ([json valueForKey:@"animation"] && [json valueForKey:@"animation"] != [NSNull null]) {
    animation = [json valueForKey:@"animation"];
    if (iconProperty == nil) {
      iconProperty = [NSMutableDictionary dictionary];
    }
    [iconProperty setObject:animation forKey:@"animation"];
    //NSLog(@"--->animation = %@", animation);
  }

  if ([json valueForKey:@"infoWindowAnchor"] && [json valueForKey:@"infoWindowAnchor"] != [NSNull null]) {
    [iconProperty setObject:[json valueForKey:@"infoWindowAnchor"] forKey:@"infoWindowAnchor"];
  }
  if (iconProperty && ([iconProperty objectForKey:@"url"] || [iconProperty objectForKey:@"iconColor"])) {

    // Load icon in asynchronise
    [self _setIcon:marker iconProperty:iconProperty callbackBlock:callbackBlock];
  } else {
    if (visible) {
      marker.map = self.mapCtrl.map;
    } else {
      marker.map = nil;
    }


    if (animation) {
      [self _setMarkerAnimation:animation marker:marker callbackBlock:^(void) {
        callbackBlock(YES, marker);
      }];
    } else {
      callbackBlock(YES, marker);
    }
  }
}

/**
 * Show the infowindow of the current marker
 * params markerId
 */
-(void)showInfoWindow:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{

      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      if (marker) {
        markerInstance.mapCtrl.map.selectedMarker = marker;
        markerInstance.mapCtrl.activeMarker = marker;
      }

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
/**
 * Hide current infowindow
 * params markerId
 */
-(void)hideInfoWindow:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      markerInstance.mapCtrl.map.selectedMarker = nil;
      markerInstance.mapCtrl.activeMarker = nil;
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set title to the specified marker
 * params markerId
 */
-(void)setTitle:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      marker.title = [command.arguments objectAtIndex:2];

      NSString *propertyId = [NSString stringWithFormat:@"marker_property_%@", markerId];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [markerInstance.mapCtrl.objects objectForKey:propertyId]];
      [markerInstance.mapCtrl.objects setObject:properties forKey:propertyId];
      
      markerInstance.mapCtrl.map.selectedMarker = nil;
      markerInstance.mapCtrl.activeMarker = nil;

      markerInstance.mapCtrl.map.selectedMarker = marker;
      markerInstance.mapCtrl.activeMarker = marker;
    }];

    NSLog(@"--->commandDelegate : %@", markerInstance.commandDelegate);
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


/**
 * Set title to the specified marker
 * params markerId
 */
-(void)setSnippet:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      marker.snippet = [command.arguments objectAtIndex:2];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Remove the specified marker
 * params markerId
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      [markerInstance.mapCtrl.objects removeObjectForKey:markerId];
      [markerInstance _removeMarker:marker];
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
 * params markerId
 */
-(void)setIconAnchor:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
    
    NSDictionary *anchorParams = [command.arguments objectAtIndex:2];
    CGFloat anchorX = [[anchorParams objectForKey:@"x"] floatValue];
    CGFloat anchorY = [[anchorParams objectForKey:@"y"] floatValue];

    if (marker.icon) {
      anchorX = anchorX / marker.icon.size.width;
      anchorY = anchorY / marker.icon.size.height;
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        [marker setGroundAnchor:CGPointMake(anchorX, anchorY)];
      }];
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set anchor of the info window
 * params markerId
 */
-(void)setInfoWindowAnchor:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
    
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSDictionary *anchorParams = [command.arguments objectAtIndex:2];
      float anchorX = [[anchorParams objectForKey:@"x"] floatValue];
      float anchorY = [[anchorParams objectForKey:@"y"] floatValue];

      anchorX = anchorX / marker.icon.size.width;
      anchorY = anchorY / marker.icon.size.height;
      [marker setInfoWindowAnchor:CGPointMake(anchorX, anchorY)];
      
      CDVPluginResult* pluginResult;
      if (marker.icon) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
      }
      [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    }];
    
  }];
}


/**
 * Set opacity
 * params markerId
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [self.mapCtrl.objects objectForKey:markerId];
      marker.opacity = [[command.arguments objectAtIndex:2] floatValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set zIndex
 * params markerId
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      marker.zIndex = [[command.arguments objectAtIndex:2] intValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set draggable
 * params markerId
 */
-(void)setDraggable:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      Boolean isEnabled = [[command.arguments objectAtIndex:2] boolValue];
      [marker setDraggable:isEnabled];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

/**
 * Set disable auto pan
 * params markerId
 */
-(void)setDisableAutoPan:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    BOOL disableAutoPan = [[command.arguments objectAtIndex:2] boolValue];

    NSString *propertyId = [NSString stringWithFormat:@"marker_property_%@",markerId];
    NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                       [markerInstance.mapCtrl.objects objectForKey:propertyId]];
    [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
    [markerInstance.mapCtrl.objects setObject:properties forKey:propertyId];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set visibility
 * params markerId
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
    Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
    
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{

      if (isVisible) {
        marker.map = markerInstance.mapCtrl.map;
      } else {
        marker.map = nil;
      }
    }];

    NSString *propertyId = [NSString stringWithFormat:@"marker_property_%@", markerId];
    NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                       [markerInstance.mapCtrl.objects objectForKey:propertyId]];
    [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"visible"];
    [markerInstance.mapCtrl.objects setObject:properties forKey:propertyId];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
  }];
}

/**
 * Set position
 * 
 */
-(void)setPosition:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];

    NSDictionary *latLngParams = [command.arguments objectAtIndex:2];
    double latitude = [[latLngParams objectForKey:@"lat"] doubleValue];
    double longitude = [[latLngParams objectForKey:@"lng"] doubleValue];
    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
    
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [marker setPosition:position];
    }];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set flattable
 * params markerId
 */
-(void)setFlat:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      Boolean isFlat = [[command.arguments objectAtIndex:2] boolValue];
      [marker setFlat: isFlat];
    }];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * set icon
 * params markerId
 */
-(void)setIcon:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  

  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];

    // Create icon
    NSMutableDictionary *iconProperty;
    id icon = [command.arguments objectAtIndex:2];
    if ([icon isKindOfClass:[NSString class]]) {
      iconProperty = [[NSMutableDictionary alloc] init];
      [iconProperty setObject:icon forKey:@"url"];
    } else if ([icon isKindOfClass:[NSDictionary class]]) {
      iconProperty = [command.arguments objectAtIndex:2];

      id url = [iconProperty objectForKey:@"url"];
      if ([url isKindOfClass:[NSArray class]]) {
        NSArray *rgbColor = url;
        iconProperty = [[NSMutableDictionary alloc] init];
        [iconProperty setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
      }
    } else if ([icon isKindOfClass:[NSArray class]]) {
      NSArray *rgbColor = icon;
      iconProperty = [[NSMutableDictionary alloc] init];
      [iconProperty setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
    }
    

    CDVCommandDelegateImpl *cmdDelegate = (CDVCommandDelegateImpl *)markerInstance.commandDelegate;
    [markerInstance _setIcon:marker iconProperty:iconProperty callbackBlock:^(BOOL successed, id resultObj) {
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
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];
      CLLocationDegrees degrees = [[command.arguments objectAtIndex:2] doubleValue];
      [marker setRotation:degrees];

    }];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)markerInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


-(void)setAnimation:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  NSString *markerId = [command.arguments objectAtIndex:1];
  PluginMarker *markerInstance = [self _getInstance:mapId markerId:markerId];
  
  [markerInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSMarker *marker = [markerInstance.mapCtrl.objects objectForKey:markerId];

      NSString *animation = [command.arguments objectAtIndex:2];
      CDVCommandDelegateImpl *cmdDelegate = (CDVCommandDelegateImpl *)markerInstance.commandDelegate;
      
      [markerInstance _setMarkerAnimation:animation marker:marker callbackBlock:^(void) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [cmdDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
    }];
  }];
}

-(void)_setMarkerAnimation:(NSString *)animation marker:(GMSMarker *)marker callbackBlock:(void (^)(void)) callbackBlock {

  animation = [animation uppercaseString];
  SWITCH(animation) {
    CASE (@"DROP") {
      [self _setDropAnimation:marker callbackBlock:callbackBlock];
      break;
    }
    CASE (@"BOUNCE") {
      [self _setBounceAnimation:marker callbackBlock:callbackBlock];
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
-(void)_setDropAnimation:(GMSMarker *)marker callbackBlock:(void (^)(void)) callbackBlock {
  /**
   * Marker drop animation
   */
  int duration = 0.1f;

  CAKeyframeAnimation *longitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"longitude"];
  CAKeyframeAnimation *latitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"latitude"];

  GMSProjection *projection = self.mapCtrl.map.projection;
  CGPoint point = [projection pointForCoordinate:marker.position];
  double distance = point.y ;

  NSMutableArray *latitudePath = [NSMutableArray array];
  NSMutableArray *longitudeath = [NSMutableArray array];
  CLLocationCoordinate2D startLatLng;

  point.y = 0;
  startLatLng = [projection coordinateForPoint:point];
  [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
  [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

  point.y = distance;
  startLatLng = [projection coordinateForPoint:point];
  [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
  [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

  longitudeAnim.values = longitudeath;
  latitudeAnim.values = latitudePath;

  CAAnimationGroup *group = [[CAAnimationGroup alloc] init];
  group.animations = @[longitudeAnim, latitudeAnim];
  group.duration = duration;
  [group setCompletionBlock: callbackBlock];

  [marker.layer addAnimation:group forKey:@"dropMarkerAnim"];

}
-(void)_setBounceAnimation:(GMSMarker *)marker callbackBlock:(void (^)(void)) callbackBlock {
  /**
   * Marker bounce animation
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

-(void)_setIcon:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty callbackBlock:(void (^)(BOOL successed, id resultObj)) callbackBlock {

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
    NSLog(@"---- _setIcon : %@", iconPath);
  }

  // `animation` property
  NSString *animationValue = nil;
  if ([iconProperty valueForKey:@"animation"] && [iconProperty valueForKey:@"animation"] != [NSNull null]) {
    animationValue = [iconProperty valueForKey:@"animation"];
  }
  __block NSString *animation = animationValue;

  //--------------------------------
  // the icon property is color name
  //--------------------------------
  if ([iconProperty valueForKey:@"iconColor"] && [iconProperty valueForKey:@"iconColor"] != [NSNull null]) {
    dispatch_async(dispatch_get_main_queue(), ^{
      UIColor *iconColor = [iconProperty valueForKey:@"iconColor"];
      marker.icon = [GMSMarker markerImageWithColor:iconColor];

      // The `visible` property
      if (iconProperty[@"visible"] == [NSNumber numberWithBool:true]) {
        marker.map = self.mapCtrl.map;
      } else if (iconProperty[@"visible"] == [NSNumber numberWithBool:false]) {
        marker.map = nil;
      }

      if (animation) {
        // Do animation, then send the result
        [self _setMarkerAnimation:animation marker:marker callbackBlock:^(void) {
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
  if ([iconProperty valueForKey:@"size"] && [iconProperty valueForKey:@"size"] != [NSNull null]) {
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
      if ([iconProperty valueForKey:@"anchor"] && [iconProperty valueForKey:@"anchor"] != [NSNull null]) {
        NSArray *points = [iconProperty valueForKey:@"anchor"];
        anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.groundAnchor = CGPointMake(anchorX, anchorY);
      }

      // The `infoWindowAnchor` property
      if ([iconProperty valueForKey:@"infoWindowAnchor"] && [iconProperty valueForKey:@"infoWindowAnchor"] != [NSNull null]) {
        NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
        anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
      }


      // The `visible` property
      if (iconProperty[@"visible"] == [NSNumber numberWithBool:true]) {
        marker.map = self.mapCtrl.map;
      } else if (iconProperty[@"visible"] == [NSNumber numberWithBool:false]) {
        marker.map = nil;
      }

      if (animation) {
        // Do animation, then send the result
        [self _setMarkerAnimation:animation marker:marker callbackBlock:^(void) {
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
    NSData *decodedData = [[NSData alloc] initWithBase64EncodedString:[tmp objectAtIndex:1] options:0];

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
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSURL *url = [(WKWebView *)self.webView URL];
        NSString *currentURL = url.absoluteString;
        //NSLog(@"currentURL = %@", url);
        if (![[url lastPathComponent] isEqualToString:@"/"]) {
          currentURL = [currentURL stringByDeletingLastPathComponent];
        }
        // remove page unchor (i.e index.html#page=test, index.html?key=value)
        NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"[#\\?].*$" options:NSRegularExpressionCaseInsensitive error:nil];
        currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];

        // remove file name (i.e /index.html)
        regex = [NSRegularExpression regularExpressionWithPattern:@"\\/[^\\/]+\\.[^\\/]+$" options:NSRegularExpressionCaseInsensitive error:nil];
        currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];

        //url = [NSURL URLWithString:[NSString stringWithFormat:@"%@/%@", currentURL, iconPath]];
        currentURL = [NSString stringWithFormat:@"%@/%@", currentURL, iconPath];
        currentURL = [currentURL regReplace:@"\\/\\.\\/" replaceTxt:@"/" options:0];
        currentURL = [currentURL regReplace:@"\\/+" replaceTxt:@"/" options:0];
        currentURL = [currentURL stringByReplacingOccurrencesOfString:@":/" withString:@"://"];
        currentURL = [currentURL stringByReplacingOccurrencesOfString:@":///" withString:@"://"];
        //NSLog(@"currentURL = %@", currentURL);
        url = [NSURL URLWithString:currentURL];

        //
        // Load the icon from over the internet
        //
        [PluginUtil downloadImageWithURL:url  completionBlock:^(BOOL succeeded, UIImage *image) {

          if (!succeeded) {
            dispatch_async(dispatch_get_main_queue(), ^{
              NSLog(@"[fail] url = %@", url);
              // The `visible` property
              if (iconProperty[@"visible"] == [NSNumber numberWithBool:true]) {
                marker.map = self.mapCtrl.map;
              } else if (iconProperty[@"visible"] == [NSNumber numberWithBool:false]) {
                marker.map = nil;
              }
              if ([[UIImageCache sharedInstance].iconCacheKeys objectForKey:iconCacheKey]) {
                [[UIImageCache sharedInstance].iconCacheKeys removeObjectForKey:iconCacheKey];
              }

              callbackBlock(NO, [NSString stringWithFormat:@"Can not load image from '%@'.", url]);
            });
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
            if ([iconProperty valueForKey:@"anchor"] && [iconProperty valueForKey:@"anchor"] != [NSNull null]) {
              NSArray *points = [iconProperty valueForKey:@"anchor"];
              CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
              CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
              marker.groundAnchor = CGPointMake(anchorX, anchorY);
            }


            // The `infoWindowAnchor` property
            if ([iconProperty valueForKey:@"infoWindowAnchor"] && [iconProperty valueForKey:@"infoWindowAnchor"] != [NSNull null]) {
              NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
              CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
              CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
              marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
            }

            // The `visible` property
            if (iconProperty[@"visible"] == [NSNumber numberWithBool:true]) {
              marker.map = self.mapCtrl.map;
            } else if (iconProperty[@"visible"] == [NSNumber numberWithBool:false]) {
              marker.map = nil;
            }


            if (animation) {
              // Do animation, then send the result
              [self _setMarkerAnimation:animation marker:marker callbackBlock:^(void) {
                callbackBlock(YES, marker);
              }];
            } else {
              // Send the result
              callbackBlock(YES, marker);
            }

          });
        }];
      }];
      return;
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
        NSURL *url = [(WKWebView *)self.webView URL];
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
      if ([iconProperty valueForKey:@"anchor"] && [iconProperty valueForKey:@"anchor"] != [NSNull null]) {
        NSArray *points = [iconProperty valueForKey:@"anchor"];
        anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.groundAnchor = CGPointMake(anchorX, anchorY);
      }

      // The `infoWindowAnchor` property
      if ([iconProperty valueForKey:@"infoWindowAnchor"] && [iconProperty valueForKey:@"infoWindowAnchor"] != [NSNull null]) {
        NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
        anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
      }


      // The `visible` property
      if (iconProperty[@"visible"] == [NSNumber numberWithBool:true]) {
        marker.map = self.mapCtrl.map;
      } else if (iconProperty[@"visible"] == [NSNumber numberWithBool:false]) {
        marker.map = nil;
      }

      if (animation) {
        // Do animation, then send the result
        [self _setMarkerAnimation:animation marker:marker callbackBlock:^(void) {
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

  iconPath = [iconPath regReplace:@"\\/\\.\\/" replaceTxt:@"/" options:0];
  iconPath = [iconPath regReplace:@"\\/+" replaceTxt:@"/" options:0];
  iconPath = [iconPath stringByReplacingOccurrencesOfString:@":/" withString:@"://"];
  NSURL *url = [NSURL URLWithString:iconPath];

  [PluginUtil downloadImageWithURL:url  completionBlock:^(BOOL succeeded, UIImage *image) {

    if (!succeeded) {
      dispatch_async(dispatch_get_main_queue(), ^{
        NSLog(@"[fail] url = %@", url);
        // The `visible` property
        if (iconProperty[@"visible"] == [NSNumber numberWithBool:true]) {
          marker.map = self.mapCtrl.map;
        } else if (iconProperty[@"visible"] == [NSNumber numberWithBool:false]) {
          marker.map = nil;
        }
        if ([[UIImageCache sharedInstance].iconCacheKeys objectForKey:iconCacheKey]) {
          [[UIImageCache sharedInstance].iconCacheKeys removeObjectForKey:iconCacheKey];
        }

        callbackBlock(NO, [NSString stringWithFormat:@"Can not load image from '%@'.", url]);
      });
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
      if ([iconProperty valueForKey:@"anchor"] && [iconProperty valueForKey:@"anchor"] != [NSNull null]) {
        NSArray *points = [iconProperty valueForKey:@"anchor"];
        CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.groundAnchor = CGPointMake(anchorX, anchorY);
      }


      // The `infoWindowAnchor` property
      if ([iconProperty valueForKey:@"infoWindowAnchor"] && [iconProperty valueForKey:@"infoWindowAnchor"] != [NSNull null]) {
        NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
        CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
      }

      // The `visible` property
      if (iconProperty[@"visible"] == [NSNumber numberWithBool:true]) {
        marker.map = self.mapCtrl.map;
      } else if (iconProperty[@"visible"] == [NSNumber numberWithBool:false]) {
        marker.map = nil;
      }


      if (animation) {
        // Do animation, then send the result
        [self _setMarkerAnimation:animation marker:marker callbackBlock:^(void) {
          callbackBlock(YES, marker);
        }];
      } else {
        // Send the result
        callbackBlock(YES, marker);
      }

    });


  }];

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

@end
