//
//  TileOverlay.m
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "TileOverlay.h"

@implementation TileOverlay

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createTileOverlay:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  NSString *tileUrlFormat = [json objectForKey:@"tileUrlFormat"];


  GMSTileURLConstructor constructor = ^(NSUInteger x, NSUInteger y, NSUInteger zoom) {
    NSString *urlStr = [tileUrlFormat stringByReplacingOccurrencesOfString:@"<x>" withString:[NSString stringWithFormat:@"%d", x]];
    urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<y>" withString:[NSString stringWithFormat:@"%d", y]];
    urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<zoom>" withString:[NSString stringWithFormat:@"%d", zoom]];
    NSLog(@"%@", urlStr);
    return [NSURL URLWithString:urlStr];
  };
  GMSTileLayer *layer = [GMSURLTileLayer tileLayerWithURLConstructor:constructor];

  if ([[json valueForKey:@"visible"] boolValue]) {
    layer.map = self.mapCtrl.map;
  }
  if ([json valueForKey:@"zIndex"]) {
    layer.zIndex = [[json valueForKey:@"zIndex"] floatValue];
  }

  NSString *id = [NSString stringWithFormat:@"tileOverlay_%lu", (unsigned long)layer.hash];
  [self.mapCtrl.overlayManager setObject:layer forKey: id];


  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:id forKey:@"id"];
  [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)layer.hash] forKey:@"hashCode"];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:1];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
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
 * Remove the tile overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:1];
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
  NSString *tileLayerKey = [command.arguments objectAtIndex:1];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  [layer clearTileCache];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
