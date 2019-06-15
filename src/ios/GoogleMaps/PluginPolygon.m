//
//  PluginPolygon.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginPolygon.h"

@implementation PluginPolygon

-(void)setPluginViewController:(PluginViewController *)viewCtrl
{
  self.mapCtrl = (PluginMapViewController *)viewCtrl;
}

- (void)pluginUnload
{
  // Plugin destroy
  NSArray *keys = [self.mapCtrl.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      if ([key hasPrefix:@"polygon_property"]) {
        key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
        GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:key];
        polygon.map = nil;
        polygon = nil;
      }
      [self.mapCtrl.objects removeObjectForKey:key];
  }

  key = nil;
  keys = nil;

  NSString *pluginId = [NSString stringWithFormat:@"%@-polygon", self.mapCtrl.overlayId];
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  [cdvViewController.pluginObjects removeObjectForKey:pluginId];
  [cdvViewController.pluginsMap setValue:nil forKey:pluginId];
  pluginId = nil;
}


- (void)pluginInitialize
{
  if (self.initialized) {
    return;
  }
  self.initialized = YES;
  [super pluginInitialize];
}

-(void)create:(CDVInvokedUrlCommand *)command
{

  // Parse the polygonOptions
  NSDictionary *json = [command.arguments objectAtIndex:1];
  NSString *idBase = [command.arguments objectAtIndex:2];

  GMSMutablePath *mutablePath = [GMSMutablePath path];
  NSArray *points = [json objectForKey:@"points"];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [mutablePath addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue])];
  }

  // Create paths of the hole property if specified.
  NSMutableArray *holePaths = [NSMutableArray array];
  if ([json valueForKey:@"holes"] && [json valueForKey:@"holes"] != [NSNull null]) {
      NSArray *holes = [json objectForKey:@"holes"];
      NSArray *latLngArray;
      NSDictionary *latLng;
      int j;

      for (i = 0; i < holes.count; i++) {
          latLngArray = [holes objectAtIndex:i];
          GMSMutablePath *holePath = [GMSMutablePath path];
          for (j = 0; j < latLngArray.count; j++) {
              latLng = [latLngArray objectAtIndex:j];
              [holePath addLatitude:[[latLng objectForKey:@"lat"] doubleValue] longitude:[[latLng objectForKey:@"lng"] doubleValue]];
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

      BOOL isVisible = YES;

      // Visible property
      NSString *visibleValue = [NSString stringWithFormat:@"%@",  json[@"visible"]];
      if ([@"0" isEqualToString:visibleValue]) {
        // false
        isVisible = NO;
        polygon.map = nil;
      } else {
        // true or default
        polygon.map = self.mapCtrl.map;
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
      NSString *id = [NSString stringWithFormat:@"polygon_%@", idBase];
      [self.mapCtrl.objects setObject:polygon forKey: id];
      polygon.title = id;

      // Run the below code on background thread.
      [self.mapCtrl.executeQueue addOperationWithBlock:^{

          //---------------------------
          // Keep the properties
          //---------------------------
          NSString *propertyId = [NSString stringWithFormat:@"polygon_property_%@", idBase];

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
          [properties setObject:[NSNumber numberWithDouble:polygon.zIndex] forKey:@"zIndex"];;
          [self.mapCtrl.objects setObject:properties forKey:propertyId];


          //---------------------------
          // Result for JS
          //---------------------------
          NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
          [result setObject:id forKey:@"__pgmId"];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];

  }];


}

-(void)insertHoleAt:(CDVInvokedUrlCommand *)command
{

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];
      NSArray *holes = [command.arguments objectAtIndex:2];


      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];


      // Insert a hole into the holes property
      NSArray *latLngArray;
      NSDictionary *latLng;
      int i;

      GMSMutablePath *path = [GMSMutablePath path];
      for (i = 0; i < [holes count]; i++) {
        latLng = [latLngArray objectAtIndex:i];
        [path addLatitude:[[latLng objectForKey:@"lat"] doubleValue] longitude:[[latLng objectForKey:@"lng"] doubleValue]];
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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger holeIndex = [[command.arguments objectAtIndex:1] integerValue];
      NSInteger pointIndex = [[command.arguments objectAtIndex:2] integerValue];
      //GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];


      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger holeIndex = [[command.arguments objectAtIndex:1] integerValue];
      NSInteger pointIndex = [[command.arguments objectAtIndex:2] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];
      NSDictionary *latLng = [command.arguments objectAtIndex:3];


      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];

      // Insert a point into the specified hole
      GMSMutablePath *path = [holePaths objectAtIndex:holeIndex];
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue]);
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

-(void)setHoles:(CDVInvokedUrlCommand *)command
{

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSArray *holeList = [command.arguments objectAtIndex:1];
      GMSMutablePath *hole;

      GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];
      NSDictionary *latLng;


      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      // Remove current holes
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];
      for (int i = 0; i < holePaths.count; i++) {
        hole = [holePaths objectAtIndex:i];
        [hole removeAllCoordinates];
      }
      [holePaths removeAllObjects];

      // update all holes
      GMSMutablePath *path;
      NSArray *holePositions;
      for (int i = 0; i < holeList.count; i++) {
        path = [GMSMutablePath path];
        holePositions = [holeList objectAtIndex:i];
        for (int j = 0; j < holePositions.count; j++) {
          latLng = [holePositions objectAtIndex:j];
          [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue])];
        }
        [holePaths addObject:path];
      }

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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger holeIndex = [[command.arguments objectAtIndex:1] integerValue];
      NSInteger pointIndex = [[command.arguments objectAtIndex:2] integerValue];
      GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];
      NSDictionary *latLng = [command.arguments objectAtIndex:3];


      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
      NSMutableArray *holePaths = (NSMutableArray *)[properties objectForKey:@"holePaths"];

      // Insert a point into the specified hole
      GMSMutablePath *path = [holePaths objectAtIndex:holeIndex];
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue]);
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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      //GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];

      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];

      [mutablePath removeCoordinateAtIndex:index];

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      if ([mutablePath count] == 0) {
          [properties removeObjectForKey:@"bounds"];
      } else {
          [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      }
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}


-(void)setPoints:(CDVInvokedUrlCommand *)command
{

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSArray *positionList = [command.arguments objectAtIndex:1];
      NSDictionary *latLng;
      GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];

      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];
      [mutablePath removeAllCoordinates];
      for (int i = 0; i < positionList.count; i++) {
        latLng = [positionList objectAtIndex:i];
        [mutablePath addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue])];
      }

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polygon setPath:mutablePath];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];

}


-(void)insertPointAt:(CDVInvokedUrlCommand *)command
{

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:2];
      GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];

      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];

      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue]);
      [mutablePath insertCoordinate:position atIndex:index];

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [polygon setPath:mutablePath];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  }];

}

-(void)setPointAt:(CDVInvokedUrlCommand *)command
{

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      NSInteger index = [[command.arguments objectAtIndex:1] integerValue];
      NSDictionary *latLng = [command.arguments objectAtIndex:2];
      GMSPolygon *polygon = (GMSPolygon *)[self.mapCtrl.objects objectForKey:polygonKey];

      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      GMSMutablePath *mutablePath = (GMSMutablePath *)[properties objectForKey:@"mutablePath"];

      CLLocationCoordinate2D position = CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] doubleValue], [[latLng objectForKey:@"lng"] doubleValue]);
      [mutablePath replaceCoordinateAtIndex:index withCoordinate:position];

      // update the property
      [properties setObject:mutablePath forKey:@"mutablePath"];
      [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];

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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];

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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{
      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];
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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];
      int zIndex = [[command.arguments objectAtIndex:1] intValue];

      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      // update the property
      [properties setObject:[NSNumber numberWithInt:zIndex] forKey:@"zIndex"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      //GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];
      Boolean isClickable = [[command.arguments objectAtIndex:1] boolValue];

      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      // update the property
      [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];
      Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

      // Get properties
      NSString *propertyId = [polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];

      // update the property
      [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

      // Apply to the polygon on UI thread.
      [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          if (isVisible) {
            polygon.map = self.mapCtrl.map;
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

  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *polygonKey = [command.arguments objectAtIndex:0];
      GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];
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


    // Apply to the polygon on UI thread.
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *polygonKey = [command.arguments objectAtIndex:0];
        GMSPolygon *polygon = [self.mapCtrl.objects objectForKey:polygonKey];
        [self.mapCtrl.objects removeObjectForKey:[polygonKey stringByReplacingOccurrencesOfString:@"polygon_" withString:@"polygon_property_"]];
        [self.mapCtrl.objects removeObjectForKey:polygonKey];
        polygon.map = nil;
        polygon = nil;

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];

}

@end
