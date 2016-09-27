//
//  Polyline.m
//  SimpleMap
//
//  Created by masashi on 11/14/13.
//
//

#import "PluginPolyline.h"

@implementation PluginPolyline

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

- (void)pluginInitialize
{
  // Initialize this plugin
  self.objects = [[NSMutableDictionary alloc] init];
  self.executeQueue =  [NSOperationQueue new];
  
  // In order to keep the statement order,
  // the queue must be FIFO.
  // (especially for moderating the points and the holes)
  self.executeQueue.maxConcurrentOperationCount = 1;
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
      if ([key hasPrefix:@"polyline_property"]) {
        key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
        GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:key];
        polyline.map = nil;
        polyline = nil;
      }
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;
  
  key = nil;
  keys = nil;
  
  NSString *pluginId = [NSString stringWithFormat:@"%@-polyline", self.mapCtrl.mapId];
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  [cdvViewController.pluginObjects removeObjectForKey:pluginId];
  [cdvViewController.pluginsMap setValue:nil forKey:pluginId];
  pluginId = nil;
}

-(void)create:(CDVInvokedUrlCommand *)command
{


  NSDictionary *json = [command.arguments objectAtIndex:1];
  GMSMutablePath *mutablePath = [GMSMutablePath path];

  NSArray *points = [json objectForKey:@"points"];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [mutablePath
        addCoordinate:
          CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }

  dispatch_async(dispatch_get_main_queue(), ^{

      // Create the Polyline, and assign it to the map.
      GMSPolyline *polyline = [GMSPolyline polylineWithPath:mutablePath];

      BOOL isVisible = NO;
      if ([[json valueForKey:@"visible"] boolValue]) {
        polyline.map = self.mapCtrl.map;
        isVisible = YES;
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

      NSString *id = [NSString stringWithFormat:@"polyline_%lu", (unsigned long)polyline.hash];
      [self.objects setObject:polyline forKey: id];
      polyline.title = id;
      
      // Run the below code on background thread.
      [self.executeQueue addOperationWithBlock:^{

          //---------------------------
          // Result for JS
          //---------------------------
          NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
          [result setObject:id forKey:@"id"];
          [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)polyline.hash] forKey:@"hashCode"];
        
          //---------------------------
          // Keep the properties
          //---------------------------
          NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
        
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
          [properties setObject:[NSNumber numberWithFloat:polyline.zIndex] forKey:@"zIndex"];;
          [self.objects setObject:properties forKey:propertyId];
        
          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  });
}



-(void)removePointAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
    
      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];
    
      [mutablePath removeCoordinateAtIndex:index];
    
      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [self.objects setObject:properties forKey:propertyId];
    
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  }];

}
-(void)insertPointAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:2];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
    
      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];
    
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue]);
      [mutablePath insertCoordinate:position atIndex:index];
    
      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [self.objects setObject:properties forKey:propertyId];
    
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setPath:mutablePath];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];

}

-(void)setPointAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:2];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
    
      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];
    
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue]);
      [mutablePath replaceCoordinateAtIndex:index withCoordinate:position];
    
      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [self.objects setObject:properties forKey:propertyId];
    
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setPath:mutablePath];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];

}

/**
 * Set color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];

      NSArray *rgbColor = [command.arguments objectAtIndex:1];
      
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setStrokeColor:[rgbColor parsePluginColor]];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
  
}

/**
 * Set width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
      float width = [[command.arguments objectAtIndex:1] floatValue];
      
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setStrokeWidth:width];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
}

/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
      NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
      
      // Update the property
      NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithInt:zIndex] forKey:@"zIndex"];
      [self.objects setObject:properties forKey:propertyId];

      // Run on the UI thread
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setZIndex:(int)zIndex];
        

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
}

/**
 * Set clickable
 * @params key
 */
-(void)setClickable:(CDVInvokedUrlCommand *)command
{
  [self.executeQueue addOperationWithBlock:^{

      NSString *polylineKey = [command.arguments objectAtIndex:0];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
      Boolean isClickable = [[command.arguments objectAtIndex:1] boolValue];
    
      // Update the property
      NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
      [self.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
      Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];
      
      NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
      [self.objects setObject:properties forKey:propertyId];
    
    
      // Run on the UI thread
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          if (isVisible) {
            polyline.map = self.mapCtrl.map;
          } else {
            polyline.map = nil;
          }

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
}
/**
 * Set geodesic
 * @params key
 */
-(void)setGeodesic:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *polylineKey = [command.arguments objectAtIndex:0];
      GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
      Boolean isGeodisic = [[command.arguments objectAtIndex:1] boolValue];
    
      // Update the property
      NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isGeodisic] forKey:@"isGeodisic"];
      [self.objects setObject:properties forKey:propertyId];
      
      
      // Run on the UI thread
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polyline setGeodesic:isGeodisic];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
}

/**
 * Remove the polyline
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{

  NSString *polylineKey = [command.arguments objectAtIndex:0];
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    GMSPolyline *polyline = (GMSPolyline *)[self.objects objectForKey:polylineKey];
    [self.objects removeObjectForKey:polylineKey];
    
    NSString *propertyId = [NSString stringWithFormat:@"polyline_property_%lu", (unsigned long)polyline.hash];
    [self.objects removeObjectForKey:propertyId];
    polyline.map = nil;
    polyline = nil;
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}

@end
