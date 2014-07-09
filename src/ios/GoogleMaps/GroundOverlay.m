//
//  GroundOverlay.m
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/4/13.
//
//

#import "GroundOverlay.h"

@implementation GroundOverlay

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createGroundOverlay:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];

  NSArray *points = [json objectForKey:@"bounds"];

  GMSGroundOverlay *layer;
  GMSMutablePath *path = [GMSMutablePath path];
  GMSCoordinateBounds *bounds;

  if (points) {
    //Generate a bounds
    int i = 0;
    NSDictionary *latLng;
    for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
    }
  }
  bounds = [[GMSCoordinateBounds alloc] initWithPath:path];
  layer = [GMSGroundOverlay groundOverlayWithBounds:bounds icon:nil];


  if ([[json valueForKey:@"visible"] boolValue]) {
    layer.map = self.mapCtrl.map;
  }
  if ([json valueForKey:@"zIndex"]) {
    layer.zIndex = [[json valueForKey:@"zIndex"] floatValue];
  }

  NSString *urlStr = [json objectForKey:@"url"];
  if (urlStr) {
    [self _setImage:layer urlStr:urlStr];
  }
  if ([json valueForKey:@"opacity"]) {
    CGFloat opacity = [[json valueForKey:@"opacity"] floatValue];
    layer.icon = [layer.icon imageByApplyingAlpha:opacity];
  }
  if ([json valueForKey:@"bearing"]) {
    layer.bearing = [[json valueForKey:@"bearing"] floatValue];
  }


  NSString *id = [NSString stringWithFormat:@"groundOverlay_%lu", (unsigned long)layer.hash];
  [self.mapCtrl.overlayManager setObject:layer forKey: id];


  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:id forKey:@"id"];
  [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)layer.hash] forKey:@"hashCode"];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)_setImage:(GMSGroundOverlay *)layer urlStr:(NSString *)urlStr {
  NSRange range = [urlStr rangeOfString:@"http"];
  if (range.location == NSNotFound) {
    layer.icon = [UIImage imageNamed:urlStr];
  } else {
    dispatch_queue_t gueue = dispatch_queue_create("GoogleMap_createGroundOverlay", NULL);
    dispatch_sync(gueue, ^{
      NSURL *url = [NSURL URLWithString:urlStr];
      NSData *data = [NSData dataWithContentsOfURL:url];
      UIImage *layerImg = [UIImage imageWithData:data];
      layer.icon = layerImg;
    });

  }
}

/**
 * Remove the ground overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  layer.map = nil;
  [self.mapCtrl removeObjectForKey:key];
  layer = nil;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
  
  if (isVisible) {
    layer.map = self.mapCtrl.map;
  } else {
    layer.map = nil;
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * set image
 * @params key
 */
-(void)setImage:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  
  NSString *urlStr = [command.arguments objectAtIndex:2];
  if (urlStr) {
    [self _setImage:layer urlStr:urlStr];
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set bounds
 * @params key
 */
-(void)setBounds:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  GMSMutablePath *path = [GMSMutablePath path];

  NSArray *points = [command.arguments objectAtIndex:2];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }
  GMSCoordinateBounds *bounds;
  bounds = [[GMSCoordinateBounds alloc] initWithPath:path];
  [layer setBounds:bounds];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set opacity
 * @params key
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  CGFloat opacity = [[command.arguments objectAtIndex:2] floatValue];
  layer.icon = [layer.icon imageByApplyingAlpha:opacity];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set bearing
 * @params key
 */
-(void)setBearing:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  layer.bearing = [[command.arguments objectAtIndex:2] floatValue];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
