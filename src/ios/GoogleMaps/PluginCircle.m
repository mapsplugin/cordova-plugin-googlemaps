//
//  Circle.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "PluginCircle.h"

@implementation PluginCircle

- (void)pluginInitialize
{
  // Initialize this plugin
  self.objects = [[NSMutableDictionary alloc] init];
}

- (void)pluginUnload
{
  // Plugin destroy
  NSArray *keys = [self.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      GMSCircle *circle = (GMSCircle *)[self.objects objectForKey:key];
      circle.map = nil;
      circle = nil;
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;
  
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
  
    // Since this plugin uses own touch-detection,
    // set NO to the tappable property.
    circle.tappable = NO;

    // Store the circle instance into self.objects
    NSString *circleId = [NSString stringWithFormat:@"circle_%lu", (unsigned long)circle.hash];
    circle.title = circleId;
    [self.objects setObject:circle forKey: circleId];

    dispatch_sync(dispatch_get_main_queue(), ^{
      

        if ([[json valueForKey:@"visible"] boolValue]) {
            circle.map = self.mapCtrl.map;
        }
      
        NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
        [result setObject:circleId forKey:@"id"];
        [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)circle.hash] forKey:@"hashCode"];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      
    });
}
/**
 * Set center
 * @params key
 */
-(void)setCenter:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{

        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.objects objectForKey:circleId];

        float latitude = [[command.arguments objectAtIndex:1] floatValue];
        float longitude = [[command.arguments objectAtIndex:2] floatValue];
        CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
        [circle setPosition:center];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

/**
 * Set fill color
 * @params key
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.objects objectForKey:circleId];

        NSArray *rgbColor = [command.arguments objectAtIndex:1];
        [circle setFillColor:[rgbColor parsePluginColor]];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}


/**
 * Set stroke color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.objects objectForKey:circleId];
      
        NSArray *rgbColor = [command.arguments objectAtIndex:1];
        [circle setStrokeColor:[rgbColor parsePluginColor]];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

/**
 * Set stroke width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.objects objectForKey:circleId];
      
        float width = [[command.arguments objectAtIndex:1] floatValue];
        [circle setStrokeWidth:width];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

/**
 * Set radius
 * @params key
 */
-(void)setRadius:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.objects objectForKey:circleId];
        float radius = [[command.arguments objectAtIndex:1] floatValue];
        [circle setRadius:radius];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}
/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.objects objectForKey:circleId];
        NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
        [circle setZIndex:(int)zIndex];

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

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *circleId = [command.arguments objectAtIndex:0];
        GMSCircle *circle = [self.objects objectForKey:circleId];
        Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];
        if (isVisible) {
          circle.map = self.mapCtrl.map;
        } else {
          circle.map = nil;
        }

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
        GMSCircle *circle = [self.objects objectForKey:circleId];
        circle.map = nil;
        [self.objects removeObjectForKey:circleId];
      
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];

}

@end
