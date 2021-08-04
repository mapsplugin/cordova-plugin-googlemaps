//
//  PluginCircle.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginCircle.h"

@implementation PluginCircle

- (void)pluginInitialize
{
  if (self.initialized) {
    return;
  }
  self.initialized = YES;
  [super pluginInitialize];
}

- (void)pluginUnload
{
  // Plugin destroy
  NSArray *keys = [self.mapCtrl.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
    key = [keys objectAtIndex:i];
    if ([key hasPrefix:@"circle_property"]) {
      key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
      GMSCircle *circle = (GMSCircle *)[self.mapCtrl.objects objectForKey:key];
      if (circle != nil) {
        circle.map = nil;
      }
      circle = nil;
    }
    [self.mapCtrl.objects removeObjectForKey:key];
  }

  key = nil;
  keys = nil;


  NSString *pluginId = [NSString stringWithFormat:@"%@-circle", self.mapCtrl.overlayId];
  [self.mapCtrl.plugins removeObjectForKey:pluginId];
}
-(void)setPluginViewController:(PluginViewController *)viewCtrl
{
    self.mapCtrl = (PluginMapViewController *)viewCtrl;
}


-(PluginCircle *)_getInstance: (NSString *)mapId {
  NSString *pluginId = [NSString stringWithFormat:@"%@-circle", mapId];
  PluginMap *mapInstance = [CordovaGoogleMaps getViewPlugin:mapId];
  return [mapInstance.mapCtrl.plugins objectForKey:pluginId];
}

-(void)create:(CDVInvokedUrlCommand *)command
{

    NSDictionary *json = [command.arguments objectAtIndex:2];
    NSString *idBase = [command.arguments objectAtIndex:3];
  
  
    NSDictionary *latLng = [json objectForKey:@"center"];
    double latitude = [[latLng valueForKey:@"lat"] doubleValue];
    double longitude = [[latLng valueForKey:@"lng"] doubleValue];
    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);

    float radius = [[json valueForKey:@"radius"] floatValue];

    dispatch_async(dispatch_get_main_queue(), ^{

        GMSCircle *circle = [GMSCircle circleWithPosition:position radius:radius];
        if ([json valueForKey:@"fillColor"] && [json valueForKey:@"fillColor"] != [NSNull null]) {
            circle.fillColor = [[json valueForKey:@"fillColor"] parsePluginColor];
        }
        if ([json valueForKey:@"strokeColor"] && [json valueForKey:@"strokeColor"] != [NSNull null]) {
            circle.strokeColor = [[json valueForKey:@"strokeColor"] parsePluginColor];
        }
        if ([json valueForKey:@"strokeWidth"] && [json valueForKey:@"strokeWidth"] != [NSNull null]) {
            circle.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
        }
        if ([json valueForKey:@"zIndex"] && [json valueForKey:@"zIndex"] != [NSNull null]) {
            circle.zIndex = [[json valueForKey:@"zIndex"] floatValue];
        }

        BOOL isVisible = YES;

        // Visible property
        NSString *visibleValue = [NSString stringWithFormat:@"%@",  json[@"visible"]];
        if ([@"0" isEqualToString:visibleValue]) {
          // false
          isVisible = NO;
          circle.map = nil;
        } else {
          // true or default
          circle.map = ((GMSMapView *)(self.mapCtrl.view));
        }
        BOOL isClickable = NO;
        if ([json valueForKey:@"clickable"] != [NSNull null] && [[json valueForKey:@"clickable"] boolValue]) {
            isClickable = YES;
        }


        // Since this plugin uses own touch-detection,
        // set NO to the tappable property.
        circle.tappable = NO;

        // Store the circle instance into self.objects
        NSString *circleId = [NSString stringWithFormat:@"circle_%@", idBase];
        circle.title = circleId;
        [self.mapCtrl.objects setObject:circle forKey: circleId];


        [self.mapCtrl.executeQueue addOperationWithBlock:^{
            //---------------------------
            // Keep the properties
            //---------------------------
            NSString *propertyId = [NSString stringWithFormat:@"circle_property_%@", idBase];

            // points
            NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
            GMSMutablePath *mutablePath = [PluginUtil getMutablePathFromCircle:circle.position radius:circle.radius];
            //[properties setObject:mutablePath forKey:@"mutablePath"];
            // bounds (pre-calculate for click detection)
            [properties setObject:[[GMSCoordinateBounds alloc] initWithPath:mutablePath] forKey:@"bounds"];
            // isVisible
            [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
            // isClickable
            [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
            // zIndex
            [properties setObject:[NSNumber numberWithFloat:circle.zIndex] forKey:@"zIndex"];;
            [self.mapCtrl.objects setObject:properties forKey:propertyId];

            //---------------------------
            // Result for JS
            //---------------------------
            NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
            [result setObject:circleId forKey:@"__pgmId"];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];

    });
}
/**
 * Set center
 * 
 */
-(void)setCenter:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *circleId = [command.arguments objectAtIndex:1];
    GMSCircle *circle = [circleInstance.mapCtrl.objects objectForKey:circleId];

    NSDictionary *latLngPairs = [command.arguments objectAtIndex:2];
    double latitude = [[latLngPairs objectForKey:@"lat"] doubleValue];
    double longitude = [[latLngPairs objectForKey:@"lng"] doubleValue];
    CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [circle setPosition:center];
    }];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set fill color
 * 
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *circleId = [command.arguments objectAtIndex:1];
    GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];

    NSArray *rgbColor = [command.arguments objectAtIndex:2];
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        [circle setFillColor:[rgbColor parsePluginColor]];
    }];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


/**
 * Set stroke color
 * 
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *circleId = [command.arguments objectAtIndex:1];
    GMSCircle *circle = [circleInstance.mapCtrl.objects objectForKey:circleId];

    NSArray *rgbColor = [command.arguments objectAtIndex:2];
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        [circle setStrokeColor:[rgbColor parsePluginColor]];
    }];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set stroke width
 * 
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *circleId = [command.arguments objectAtIndex:1];
    GMSCircle *circle = [circleInstance.mapCtrl.objects objectForKey:circleId];

    float width = [[command.arguments objectAtIndex:2] floatValue];
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      [circle setStrokeWidth:width];
    }];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set radius
 * 
 */
-(void)setRadius:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *circleId = [command.arguments objectAtIndex:1];
    GMSCircle *circle = [circleInstance.mapCtrl.objects objectForKey:circleId];
    float radius = [[command.arguments objectAtIndex:2] floatValue];
    
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        [circle setRadius:radius];
    }];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * Set z-index
 * 
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{
    NSString *circleId = [command.arguments objectAtIndex:1];
    GMSCircle *circle = [circleInstance.mapCtrl.objects objectForKey:circleId];
    NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
    
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        [circle setZIndex:(int)zIndex];
    }];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Set visibility
 * 
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{

    NSString *key = [command.arguments objectAtIndex:1];
    GMSCircle *circle = (GMSCircle *)[circleInstance.mapCtrl.objects objectForKey:key];
    Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];

    // Update the property
    NSString *propertyId = [key stringByReplacingOccurrencesOfString:@"circle_" withString:@"circle_property_"];
    NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                       [circleInstance.mapCtrl.objects objectForKey:propertyId]];
    [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
    [circleInstance.mapCtrl.objects setObject:properties forKey:propertyId];

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        if (isVisible) {
          circle.map = ((GMSMapView *)(circleInstance.mapCtrl.view));
        } else {
          circle.map = nil;
        }
    }];


    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


/**
 * Set clickable
 * 
 */
-(void)setClickable:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  [circleInstance.mapCtrl.executeQueue addOperationWithBlock:^{

    NSString *key = [command.arguments objectAtIndex:1];
    //GMSCircle *circle = (GMSCircle *)[self.mapCtrl.objects objectForKey:key];
    Boolean isClickable = [[command.arguments objectAtIndex:2] boolValue];

    // Update the property
    NSString *propertyId = [key stringByReplacingOccurrencesOfString:@"circle_" withString:@"circle_property_"];
    NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                       [circleInstance.mapCtrl.objects objectForKey:propertyId]];
    [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
    [circleInstance.mapCtrl.objects setObject:properties forKey:propertyId];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Remove the circle
 * 
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginCircle *circleInstance = [self _getInstance:mapId];

  NSString *circleId = [command.arguments objectAtIndex:1];
  GMSCircle *circle = [circleInstance.mapCtrl.objects objectForKey:circleId];

  NSString *propertyId = [circleId stringByReplacingOccurrencesOfString:@"circle_" withString:@"circle_property_"];
  [circleInstance.mapCtrl.objects removeObjectForKey:propertyId];

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    circle.map = nil;
    [circleInstance.mapCtrl.objects removeObjectForKey:circleId];
  }];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [circleInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

@end
