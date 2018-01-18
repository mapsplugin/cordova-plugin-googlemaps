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
          circle.map = nil;
          circle = nil;
        }
        [self.mapCtrl.objects removeObjectForKey:key];
    }

    key = nil;
    keys = nil;

    NSString *pluginId = [NSString stringWithFormat:@"%@-circle", self.mapCtrl.mapId];
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    [cdvViewController.pluginObjects removeObjectForKey:pluginId];
    [cdvViewController.pluginsMap setValue:nil forKey:pluginId];
    pluginId = nil;
}
-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
    self.mapCtrl = viewCtrl;
}
-(void)create:(CDVInvokedUrlCommand *)command
{

    NSDictionary *json = [command.arguments objectAtIndex:1];
    NSDictionary *latLng = [json objectForKey:@"center"];
    float latitude = [[latLng valueForKey:@"lat"] floatValue];
    float longitude = [[latLng valueForKey:@"lng"] floatValue];
    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);

    float radius = [[json valueForKey:@"radius"] floatValue];

    dispatch_async(dispatch_get_main_queue(), ^{

        GMSCircle *circle = [GMSCircle circleWithPosition:position radius:radius];
        if ([json valueForKey:@"fillColor"]) {
            circle.fillColor = [[json valueForKey:@"fillColor"] parsePluginColor];
        }
        if ([json valueForKey:@"strokeColor"]) {
            circle.strokeColor = [[json valueForKey:@"strokeColor"] parsePluginColor];
        }
        if ([json valueForKey:@"strokeWidth"]) {
            circle.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
        }
        if ([json valueForKey:@"zIndex"]) {
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
          circle.map = self.mapCtrl.map;
        }
        BOOL isClickable = NO;
        if ([[json valueForKey:@"clickable"] boolValue]) {
            isClickable = YES;
        }


        // Since this plugin uses own touch-detection,
        // set NO to the tappable property.
        circle.tappable = NO;

        // Store the circle instance into self.objects
        NSString *idBase = [NSString stringWithFormat:@"%lu%d", command.hash, arc4random() % 100000];
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
            [result setObject:circleId forKey:@"id"];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];

    });
}
/**
 * Set center
 * @params key
 */
-(void)setCenter:(CDVInvokedUrlCommand *)command
{

    [self.mapCtrl.executeQueue addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];

        float latitude = [[command.arguments objectAtIndex:1] floatValue];
        float longitude = [[command.arguments objectAtIndex:2] floatValue];
        CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [circle setPosition:center];

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
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];

        NSArray *rgbColor = [command.arguments objectAtIndex:1];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [circle setFillColor:[rgbColor parsePluginColor]];

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
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];

        NSArray *rgbColor = [command.arguments objectAtIndex:1];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [circle setStrokeColor:[rgbColor parsePluginColor]];

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
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];

        float width = [[command.arguments objectAtIndex:1] floatValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [circle setStrokeWidth:width];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }];
}

/**
 * Set radius
 * @params key
 */
-(void)setRadius:(CDVInvokedUrlCommand *)command
{
    [self.mapCtrl.executeQueue addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];
        float radius = [[command.arguments objectAtIndex:1] floatValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [circle setRadius:radius];

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
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];
        NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [circle setZIndex:(int)zIndex];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }];
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{

    [self.mapCtrl.executeQueue addOperationWithBlock:^{

        NSString *key = [command.arguments objectAtIndex:0];
        GMSCircle *circle = (GMSCircle *)[self.mapCtrl.objects objectForKey:key];
        Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

        // Update the property
        NSString *propertyId = [key stringByReplacingOccurrencesOfString:@"circle_" withString:@"circle_property_"];
        NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                           [self.mapCtrl.objects objectForKey:propertyId]];
        [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
        [self.mapCtrl.objects setObject:properties forKey:propertyId];

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            if (isVisible) {
              circle.map = self.mapCtrl.map;
            } else {
              circle.map = nil;
            }

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

      NSString *key = [command.arguments objectAtIndex:0];
      GMSCircle *circle = (GMSCircle *)[self.mapCtrl.objects objectForKey:key];
      Boolean isClickable = [[command.arguments objectAtIndex:1] boolValue];

      // Update the property
      NSString *propertyId = [key stringByReplacingOccurrencesOfString:@"circle_" withString:@"circle_property_"];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

/**
 * Remove the circle
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.mapCtrl.objects objectForKey:circleId];

        NSString *propertyId = [circleId stringByReplacingOccurrencesOfString:@"circle_" withString:@"circle_property_"];
        [self.mapCtrl.objects removeObjectForKey:propertyId];

        circle.map = nil;
        [self.mapCtrl.objects removeObjectForKey:circleId];


        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];

}

@end
