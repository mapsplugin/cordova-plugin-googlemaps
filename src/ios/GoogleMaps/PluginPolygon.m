//
//  Polygon.m
//  SimpleMap
//
//  Created by masashi on 11/13/13.
//
//

#import "PluginPolygon.h"

@implementation PluginPolygon

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
  NSArray *keys = [self.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      if ([key hasPrefix:@"polygon_property"]) {
        key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
        GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:key];
        polygon.map = nil;
        polygon = nil;
      }
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;
  
  key = nil;
  keys = nil;
  
  NSString *pluginId = [NSString stringWithFormat:@"%@-polygon", self.mapCtrl.mapId];
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  [cdvViewController.pluginObjects removeObjectForKey:pluginId];
  [cdvViewController.pluginsMap setValue:nil forKey:pluginId];
  pluginId = nil;
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

-(void)create:(CDVInvokedUrlCommand *)command
{

  // Parse the polygonOptions
  NSDictionary *json = [command.arguments objectAtIndex:1];

  GMSMutablePath *mutablePath = [GMSMutablePath path];
  NSArray *points = [json objectForKey:@"points"];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [mutablePath addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }

  // Create paths of the hole property if specified.
  NSMutableArray *holePaths = [NSMutableArray array];
  if ([json valueForKey:@"holes"]) {
      NSArray *holes = [json objectForKey:@"holes"];
      NSArray *latLngArray;
      NSDictionary *latLng;
      int j;

      for (i = 0; i < holes.count; i++) {
          latLngArray = [holes objectAtIndex:i];
          GMSMutablePath *holePath = [GMSMutablePath path];
          for (j = 0; j < latLngArray.count; j++) {
              latLng = [latLngArray objectAtIndex:j];
              [holePath addLatitude:[[latLng objectForKey:@"lat"] floatValue] longitude:[[latLng objectForKey:@"lng"] floatValue]];
          }
          [holePaths addObject:holePath];
      }
  }

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{

      // Create the polygon, and assign it to the map on UI thread.
      GMSPolygon *polygon = [GMSPolygon polygonWithPath:mutablePath];

      if (holePaths != nil) {
        polygon.holes = holePaths;
      }

      BOOL isVisible = NO;
      if ([[json valueForKey:@"visible"] boolValue]) {
        polygon.map = self.mapCtrl.map;
        isVisible = YES;
      }
      BOOL isClickable = NO;
      if ([[json valueForKey:@"clickable"] boolValue]) {
        isClickable = YES;
      }
      if ([[json valueForKey:@"geodesic"] boolValue]) {
        polygon.geodesic = YES;
      }
      NSArray *rgbColor = [json valueForKey:@"fillColor"];
      polygon.fillColor = [rgbColor parsePluginColor];

      rgbColor = [json valueForKey:@"strokeColor"];
      polygon.strokeColor = [rgbColor parsePluginColor];

      polygon.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
      polygon.zIndex = [[json valueForKey:@"zIndex"] floatValue];

      // Since this plugin provide own click detection,
      // disable default clickable feature.
      polygon.tappable = NO;

      // Register polygon to the overlayManager.
      NSString *id = [NSString stringWithFormat:@"polygon_%lu", (unsigned long)polygon.hash];
      [self.objects setObject:polygon forKey: id];

      // Run the below code on background thread.
      [self.executeQueue addOperationWithBlock:^{
          polygon.title = id;
        
          //---------------------------
          // Keep the properties
          //---------------------------
          NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
        
          // points
          NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
          [properties setObject:mutablePath forKey:@"mutablePath"];
          // holes
          [properties setObject:holePaths forKey:@"holePaths"];
          // bounds (pre-calculate for click detection)
          [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
          // isVisible
          [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
          // isClickable
          [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
          // geodesic
          [properties setObject:[NSNumber numberWithBool:polygon.geodesic] forKey:@"geodesic"];
          // zIndex
          [properties setObject:[NSNumber numberWithFloat:polygon.zIndex] forKey:@"zIndex"];;
          [self.objects setObject:properties forKey:propertyId];
        

          //---------------------------
          // Result for JS
          //---------------------------
          NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
          [result setObject:id forKey:@"id"];
          [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)polygon.hash] forKey:@"hashCode"];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

  }];


}

-(void)insertHoleAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:polygonKey];
      NSArray *holes = [command.arguments objectAtIndex:2];
    
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];
    
    
      // Insert a hole into the holes property
      NSArray *latLngArray;
      NSDictionary *latLng;
      int i;

      GMSMutablePath *path = [GMSMutablePath path];
      for (i = 0; i < [holes count]; i++) {
        latLng = [latLngArray objectAtIndex:i];
        [path addLatitude:[[latLng objectForKey:@"lat"] floatValue] longitude:[[latLng objectForKey:@"lng"] floatValue]];
      }
      [holePaths insertObject:path atIndex:index];
    
      // Save the property
      [properties setObject:holePaths forKey:@"holePaths"];
    

      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        polygon.holes = holePaths;

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

    }];

}



-(void)removePointOfHoleAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger holeIndex = [[command.arguments objectAtIndex:1] integerValue];
      NSInteger pointIndex = [[command.arguments objectAtIndex:2] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:polygonKey];
    
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];
    
      // Insert a point into the specified hole
      GMSMutablePath *path = [holePaths objectAtIndex:holeIndex];
      [path removeCoordinateAtIndex:pointIndex];
      [holePaths setObject:path atIndexedSubscript:holeIndex];
    
      // Save the property
      [properties setObject:holePaths forKey:@"holePaths"];
    
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

-(void)setPointOfHoleAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger holeIndex = [[command.arguments objectAtIndex:1] integerValue];
      NSInteger pointIndex = [[command.arguments objectAtIndex:2] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:polygonKey];
      NSDictionary *latLng = [command.arguments objectAtIndex:3];
    
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];
    
      // Insert a point into the specified hole
      GMSMutablePath *path = [holePaths objectAtIndex:holeIndex];
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue]);
      [path replaceCoordinateAtIndex:pointIndex withCoordinate:position];
      [holePaths setObject:path atIndexedSubscript:holeIndex];
    
      // Save the property
      [properties setObject:holePaths forKey:@"holePaths"];
    

      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        polygon.holes = holePaths;

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

  }];
}

-(void)insertPointOfHoleAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger holeIndex = [[command.arguments objectAtIndex:1] integerValue];
      NSInteger pointIndex = [[command.arguments objectAtIndex:2] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:polygonKey];
      NSDictionary *latLng = [command.arguments objectAtIndex:3];
    
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];
    
      // Insert a point into the specified hole
      GMSMutablePath *path = [holePaths objectAtIndex:holeIndex];
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue]);
      [path insertCoordinate:position atIndex:pointIndex];
      [holePaths setObject:path atIndexedSubscript:holeIndex];
    
      // Save the property
      [properties setObject:holePaths forKey:@"holePaths"];
    

      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          polygon.holes = holePaths;

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

  }];
}



-(void)removePointAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:polygonKey];
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
    
      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];
    
      [mutablePath removeCoordinateAtIndex:index];
    
      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      if ([mutablePath count] == 0) {
          [properties removeObjectForKey:@"bounds"];
      } else {
          [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      }
      [self.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}


-(void)insertPointAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:2];
      GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:polygonKey];
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
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
          [polygon setPath:mutablePath];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];

}

-(void)setPointAt:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
  
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:2];
      GMSPolygon *polygon = (GMSPolygon *)[self.objects objectForKey:polygonKey];
    
      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
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
          [polygon setPath:mutablePath];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];

}
/**
 * Set fill color
 * @params key
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{

    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.objects objectForKey:polygonKey];

    NSArray *rgbColor = [command.arguments objectAtIndex:1];

    // Apply to the polygon on UI thread.
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        [polygon setFillColor:[rgbColor parsePluginColor]];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];

}


/**
 * Set stroke color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.objects objectForKey:polygonKey];

    NSArray *rgbColor = [command.arguments objectAtIndex:1];

    // Apply to the polygon on UI thread.
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        [polygon setStrokeColor:[rgbColor parsePluginColor]];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];

}

/**
 * Set stroke width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.objects objectForKey:polygonKey];
      float width = [[command.arguments objectAtIndex:1] floatValue];

    
      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polygon setStrokeWidth:width];
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

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.objects objectForKey:polygonKey];
      NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];

      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];

      // update the property
      [properties setObject:[NSNumber numberWithInt:zIndex] forKey:@"zIndex"];
      [self.objects setObject:properties forKey:propertyId];
      
      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polygon setZIndex:(int)zIndex];

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

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.objects objectForKey:polygonKey];
      Boolean isClickable = [[command.arguments objectAtIndex:1] boolValue];

      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];

      // update the property
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

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.objects objectForKey:polygonKey];
      Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

      // Get properties
      NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%lu", (unsigned long)polygon.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];

      // update the property
      [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
      [self.objects setObject:properties forKey:propertyId];
      
      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          if (isVisible) {
            //polygon.map = self.mapCtrl.map;
          } else {
            polygon.map = nil;
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

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.objects objectForKey:polygonKey];
      Boolean isGeodisic = [[command.arguments objectAtIndex:1] boolValue];

      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polygon setGeodesic:isGeodisic];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
}

/**
 * Remove the polygon
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.objects objectForKey:polygonKey];

      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          polygon.map = nil;
          [self.objects removeObjectForKey:polygonKey];
          [self.objects removeObjectForKey:[polygonKey stringByReplacingOccurrencesOfString:@"_" withString:@"_holePaths_"]];
          [self.objects removeObjectForKey:[polygonKey stringByReplacingOccurrencesOfString:@"_" withString:@"_property_"]];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];
  
}

@end
