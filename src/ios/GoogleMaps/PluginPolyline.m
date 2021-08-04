//
//  PluginPolyline.m
//  cordova-googlemaps-plugin v2
//
//  Created by masashi.
//
//

#import "PluginPolyline.h"

@implementation PluginPolyline

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
      if ([key hasPrefix:@"polyline_property"]) {
        key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
        GMSPolyline *polyline = (GMSPolyline *)[self.mapCtrl.objects objectForKey:key];
        polyline.map = nil;
        polyline = nil;
      }
      [self.mapCtrl.objects removeObjectForKey:key];
  }

  key = nil;
  keys = nil;
  
  NSString *pluginId = [NSString stringWithFormat:@"%@-polyline", self.mapCtrl.overlayId];
  [self.mapCtrl.plugins removeObjectForKey:pluginId];
}

-(PluginPolyline *)_getInstance: (NSString *)mapId {
  NSString *pluginId = [NSString stringWithFormat:@"%@-polyline", mapId];
  PluginMap *mapInstance = [CordovaGoogleMaps getViewPlugin:mapId];
  return [mapInstance.mapCtrl.plugins objectForKey:pluginId];
}

-(void)create:(CDVInvokedUrlCommand *)command
{


  NSDictionary *json = [command.arguments objectAtIndex:2];
  NSString *idBase = [command.arguments objectAtIndex:3];
  GMSMutablePath *mutablePath = [GMSMutablePath path];

  NSArray *points = [json objectForKey:@"points"];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [mutablePath
        addCoordinate:
          CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue])];
  }

  dispatch_async(dispatch_get_main_queue(), ^{

      // Create the Polyline, and assign it to the map.
      GMSPolyline *polyline = [GMSPolyline polylineWithPath:mutablePath];


      BOOL isVisible = YES;
      // Visible property
      NSString *visibleValue = [NSString stringWithFormat:@"%@",  json[@"visible"]];
      if ([@"0" isEqualToString:visibleValue]) {
        // false
        isVisible = NO;
        polyline.map = nil;
      } else {
        // true or default
        polyline.map = self.mapCtrl.map;
      }
      BOOL isClickable = NO;
      if ([[json valueForKey:@"clickable"] boolValue]) {
        isClickable = YES;
      }
      if ([[json valueForKey:@"geodesic"] boolValue]) {
        polyline.geodesic = YES;
      }
      NSArray *rgbColor = [json valueForKey:@"color"];
      polyline.strokeColor = [rgbColor parsePluginColor];

      polyline.strokeWidth = [[json valueForKey:@"width"] floatValue];
      polyline.zIndex = [[json valueForKey:@"zIndex"] floatValue];

      // Since this plugin provide own click detection,
      // disable default clickable feature.
      polyline.tappable = NO;

      NSString *id = [NSString stringWithFormat:@"polyline_%@", idBase];
      [self.mapCtrl.objects setObject:polyline forKey: id];
      polyline.title = id;

      // Run the below code on background thread.
      [self.mapCtrl.executeQueue addOperationWithBlock:^{

          //---------------------------
          // Result for JS
          //---------------------------
          NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
          [result setObject:id forKey:@"__pgmId"];

          //---------------------------
          // Keep the properties
          //---------------------------
          NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%@", idBase];

          // points
          NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
          [properties setObject:mutablePath forKey:@"mutablePath"];
          // bounds (pre-calculate for click detection)
          [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
          // isVisible
          [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
          // isClickable
          [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
          // geodesic
          [properties setObject:[NSNumber numberWithBool:polyline.geodesic] forKey:@"geodesic"];
          // zIndex
          [properties setObject:[NSNumber numberWithFloat:polyline.zIndex] forKey:@"zIndex"];
          [self.mapCtrl.objects setObject:properties forKey:propertyId];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  });
}



-(void)removePointAt:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polylineKey = [command.arguments objectAtIndex:1];
      NSInteger index = [[command.arguments objectAtIndex:2] integerValue];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];

      // Get properties
      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];

      [mutablePath removeCoordinateAtIndex:index];

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setPath:mutablePath];
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  }];

}
-(void)setPoints:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polylineKey = [command.arguments objectAtIndex:1];
      NSArray *positionList = [command.arguments objectAtIndex:2];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];

      // Get properties
      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];
      [mutablePath removeAllCoordinates];

      CLLocationCoordinate2D position;
      NSDictionary *latLng;
      for (int i = 0; i < positionList.count; i++) {
          latLng = [positionList objectAtIndex:i];
          position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue]);
          [mutablePath addCoordinate:position];
      }

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setPath:mutablePath];
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}

-(void)insertPointAt:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polylineKey = [command.arguments objectAtIndex:1];
      NSInteger index = [[command.arguments objectAtIndex:2] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:3];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];

      // Get properties
      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];

      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue]);
      [mutablePath insertCoordinate:position atIndex:index];

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setPath:mutablePath];
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}

-(void)setPointAt:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polylineKey = [command.arguments objectAtIndex:1];
      NSInteger index = [[command.arguments objectAtIndex:2] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:3];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];

      // Get properties
      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];

      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue]);
      [mutablePath replaceCoordinateAtIndex:index withCoordinate:position];

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setPath:mutablePath];
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}

/**
 * Set color
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:1];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];

      NSArray *rgbColor = [command.arguments objectAtIndex:2];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setStrokeColor:[rgbColor parsePluginColor]];
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}

/**
 * Set width
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:1];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];
      float width = [[command.arguments objectAtIndex:2] floatValue];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setStrokeWidth:width];
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set z-index
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:1];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];
      int zIndex = [[command.arguments objectAtIndex:2] intValue];

      // Update the property
      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithInt:zIndex] forKey:@"zIndex"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];

      // Run on the UI thread
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setZIndex:(int)zIndex];
      }];


      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set clickable
 */
-(void)setClickable:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polylineKey = [command.arguments objectAtIndex:1];
      //GMSPolyline *polyline = (GMSPolyline *)[self.mapCtrl.objects objectForKey:polylineKey];
      Boolean isClickable = [[command.arguments objectAtIndex:2] boolValue];

      // Update the property
      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * Set visibility
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:1];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];
      Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];

      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];


      // Run on the UI thread
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          if (isVisible) {
            polyline.map = self.mapCtrl.map;
          } else {
            polyline.map = nil;
          }
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * Set geodesic
 */
-(void)setGeodesic:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:1];
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];
      Boolean isGeodisic = [[command.arguments objectAtIndex:2] boolValue];

      // Update the property
      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [polylineInstance.mapCtrl.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isGeodisic] forKey:@"isGeodisic"];
      [polylineInstance.mapCtrl.objects setObject:properties forKey:propertyId];


      // Run on the UI thread
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setGeodesic:isGeodisic];
      }];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Remove the polyline
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginPolyline *polylineInstance = [self _getInstance:mapId];
  
  [polylineInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *polylineKey = [command.arguments objectAtIndex:1];
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      GMSPolyline *polyline = (GMSPolyline *)[polylineInstance.mapCtrl.objects objectForKey:polylineKey];
      [polylineInstance.mapCtrl.objects removeObjectForKey:polylineKey];

      NSString *propertyId = [polylineKey stringByReplacingOccurrencesOfString:@"polyline_" withString:@"polyline_property_"];
      [polylineInstance.mapCtrl.objects removeObjectForKey:propertyId];
      polyline.map = nil;
      polyline = nil;
    }];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [polylineInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}

@end
