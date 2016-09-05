//
//  Circle.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "PluginCircle.h"

@implementation PluginCircle

- (void)pluginUnload
{
}
-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}
-(void)create:(CDVInvokedUrlCommand *)command
{
/*
  // Initialize this plugin
  if (self.mapCtrl == nil) {
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];
    self.mapCtrl = googlemaps.mapCtrl;
    [self.mapCtrl.plugins setObject:self forKey:@"Circle"];
  }

  NSDictionary *json = [command.arguments objectAtIndex:0];
  NSDictionary *latLng = [json objectForKey:@"center"];
  __block GMSCircle *circle;

  dispatch_queue_t gueue = dispatch_queue_create("createCircle", NULL);
  dispatch_async(gueue, ^{

      float latitude = [[latLng valueForKey:@"lat"] floatValue];
      float longitude = [[latLng valueForKey:@"lng"] floatValue];

      float radius = [[json valueForKey:@"radius"] floatValue];
      CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);


      dispatch_sync(dispatch_get_main_queue(), ^{
          circle = [GMSCircle circleWithPosition:position radius:radius];

          //if ([[json valueForKey:@"visible"] boolValue]) {
          //    circle.map = self.mapCtrl.map;
          //}

          NSArray *rgbColor = [json valueForKey:@"fillColor"];
          circle.fillColor = [rgbColor parsePluginColor];

          rgbColor = [json valueForKey:@"strokeColor"];
          circle.strokeColor = [rgbColor parsePluginColor];

          circle.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
          circle.zIndex = [[json valueForKey:@"zIndex"] floatValue];

          circle.tappable = YES;

          NSString *id = [NSString stringWithFormat:@"circle_%lu", (unsigned long)circle.hash];
          [self.mapCtrl.overlayManager setObject:circle forKey: id];
          circle.title = id;

          dispatch_async(gueue, ^{
              NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
              [result setObject:id forKey:@"id"];
              [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)circle.hash] forKey:@"hashCode"];

              CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
              [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          });
      });
  });

  */

}
/**
 * Set center
 * @params key
 */
-(void)setCenter:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];

  float latitude = [[command.arguments objectAtIndex:1] floatValue];
  float longitude = [[command.arguments objectAtIndex:2] floatValue];
  CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
  [circle setPosition:center];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set fill color
 * @params key
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];

  NSArray *rgbColor = [command.arguments objectAtIndex:1];
  [circle setFillColor:[rgbColor parsePluginColor]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set stroke color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];

  NSArray *rgbColor = [command.arguments objectAtIndex:1];
  [circle setStrokeColor:[rgbColor parsePluginColor]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set stroke width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  float width = [[command.arguments objectAtIndex:1] floatValue];
  [circle setStrokeWidth:width];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set radius
 * @params key
 */
-(void)setRadius:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  float radius = [[command.arguments objectAtIndex:1] floatValue];
  [circle setRadius:radius];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
  [circle setZIndex:(int)zIndex];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
/*
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];
  if (isVisible) {
    circle.map = self.mapCtrl.map;
  } else {
    circle.map = nil;
  }

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
*/
}

/**
 * Remove the circle
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *circleKey = [command.arguments objectAtIndex:0];
  GMSCircle *circle = [self.mapCtrl getCircleByKey: circleKey];
  circle.map = nil;
  [self.mapCtrl removeObjectForKey:circleKey];
  circle = nil;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
