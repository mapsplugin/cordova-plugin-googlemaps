//
//  TileOverlay.m
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "PluginTileOverlay.h"

@implementation PluginTileOverlay

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}
- (void)pluginUnload
{
}

-(void)create:(CDVInvokedUrlCommand *)command
{

  // Initialize this plugin
  if (self.mapCtrl == nil) {
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];
    //self.mapCtrl = googlemaps.mapCtrl;
    [self.mapCtrl.plugins setObject:self forKey:@"TileOverlay"];
  }

  NSDictionary *json = [command.arguments objectAtIndex:0];
  NSString *tileUrlFormat = [json objectForKey:@"tileUrlFormat"];

  dispatch_queue_t gueue = dispatch_queue_create("createTileOverlay", NULL);
  dispatch_async(gueue, ^{


      dispatch_sync(dispatch_get_main_queue(), ^{

          GMSTileURLConstructor constructor = ^(NSUInteger x, NSUInteger y, NSUInteger zoom) {
            NSString *urlStr = [tileUrlFormat stringByReplacingOccurrencesOfString:@"<x>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)x]];
            urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<y>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)y]];
            urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<zoom>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)zoom]];

            if (self.mapCtrl.debuggable) {
              NSLog(@"%@", urlStr);
            }
            return [NSURL URLWithString:urlStr];
          };
          GMSTileLayer *layer = [GMSURLTileLayer tileLayerWithURLConstructor:constructor];

          if ([[json valueForKey:@"visible"] boolValue]) {
            //layer.map = self.mapCtrl.map;
          }
          if ([json valueForKey:@"zIndex"]) {
            layer.zIndex = [[json valueForKey:@"zIndex"] floatValue];
          }
          if ([json valueForKey:@"tileSize"]) {
            layer.tileSize = [[json valueForKey:@"tileSize"] integerValue];
          }
          if ([json valueForKey:@"opacity"]) {
            layer.opacity = [[json valueForKey:@"opacity"] floatValue];
          }


          dispatch_async(gueue, ^{

              NSString *id = [NSString stringWithFormat:@"tileOverlay_%lu", (unsigned long)layer.hash];
              [self.mapCtrl.overlayManager setObject:layer forKey: id];


              NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
              [result setObject:id forKey:@"id"];
              [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)layer.hash] forKey:@"hashCode"];

              CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
              [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          });
      });
  });
}


/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:0];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];
  if (isVisible) {
    //layer.map = self.mapCtrl.map;
  } else {
    layer.map = nil;
  }

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Remove the tile overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:0];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  layer.map = nil;
  [layer clearTileCache];
  [self.mapCtrl removeObjectForKey:tileLayerKey];
  layer = nil;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Clear tile cache
 * @params key
 */
-(void)clearTileCache:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:0];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  [layer clearTileCache];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:0];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
  [layer setZIndex:(int)zIndex];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set fadeIn
 * @params key
 */
-(void)setFadeIn:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:0];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
  [layer setFadeIn:isEnabled];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set opacity
 * @params key
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:0];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  double opacity = [[command.arguments objectAtIndex:1] doubleValue];
  [layer setOpacity:opacity];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
