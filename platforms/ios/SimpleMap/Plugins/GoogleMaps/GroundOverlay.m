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
  NSDictionary *json = [command.arguments objectAtIndex:1];  NSLog(@"%@", json);
  
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
      dispatch_release(gueue);
      
    }
  }
  if ([json valueForKey:@"opacity"]) {
    CGFloat opacity = [[json valueForKey:@"opacity"] floatValue];
    layer.icon = [layer.icon imageByApplyingAlpha:opacity];
  }
  
  
  NSString *key = [NSString stringWithFormat:@"groundOverlay%d", layer.hash];
  [self.mapCtrl.overlayManager setObject:layer forKey: key];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: key];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


@end
