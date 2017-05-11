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
    
  NSString *id = [[[NSUUID alloc] init] UUIDString];
    
    if (!self.tileLayerFormats) {
        self.tileLayerFormats = [NSMutableDictionary dictionary];
    }
    
  [self.tileLayerFormats setObject:tileUrlFormat forKey:id];
  GMSTileURLConstructor constructor = ^(NSUInteger x, NSUInteger y, NSUInteger zoom) {
    NSString *urlStr = [[self.tileLayerFormats objectForKey:id] stringByReplacingOccurrencesOfString:@"<x>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)x]];
    urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<y>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)y]];
    urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<zoom>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)zoom]];

    urlStr = [urlStr stringByAddingPercentEncodingWithAllowedCharacters: [NSCharacterSet URLFragmentAllowedCharacterSet]];
    urlStr = [urlStr stringByReplacingOccurrencesOfString:@"https%253A%252F%252F"
                                             withString:@"https://"];
    NSURL *url = [NSURL URLWithString:urlStr];

      if (true) {
          NSLog(@"%@", url);
      }
    return url;
  };
    
  GMSTileLayer *layer = [GMSURLTileLayer tileLayerWithURLConstructor:constructor];

  if ([[json valueForKey:@"visible"] boolValue]) {
    layer.map = self.mapCtrl.map;
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

-(void)setTileUrlFormat:(CDVInvokedUrlCommand *)command
{
    NSString *tileLayerKey = [command.arguments objectAtIndex:1];
    NSString *newTileLayerFormat = [[command.arguments objectAtIndex:2] stringValue];
    
    [self.tileLayerFormats setObject:newTileLayerFormat forKey:tileLayerKey];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
    
/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *tileLayerKey = [command.arguments objectAtIndex:1];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
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
  NSString *tileLayerKey = [command.arguments objectAtIndex:1];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  Boolean isEnabled = [[command.arguments objectAtIndex:2] boolValue];
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
  NSString *tileLayerKey = [command.arguments objectAtIndex:1];
  GMSTileLayer *layer = [self.mapCtrl getTileLayerByKey:tileLayerKey];
  double opacity = [[command.arguments objectAtIndex:2] doubleValue];
  [layer setOpacity:opacity];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
