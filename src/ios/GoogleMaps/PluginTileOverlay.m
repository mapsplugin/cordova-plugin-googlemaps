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
- (void)pluginInitialize
{
  // Initialize this plugin
  self.objects = [[NSMutableDictionary alloc] init];
  self.imgCache = [[NSCache alloc]init];
  self.imgCache.totalCostLimit = 3 * 1024 * 1024 * 1024; // 3MB = Cache for image
  self.executeQueue =  [NSOperationQueue new];
}

- (void)pluginUnload
{

  if (self.executeQueue != nil){
      self.executeQueue.suspended = YES;
      [self.executeQueue cancelAllOperations];
      self.executeQueue.suspended = NO;
      self.executeQueue = nil;
  }


  // Plugin destroy
  NSArray *keys = [self.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      if ([key hasPrefix:@"tileoverlay_property"]) {
        key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
        GMSTileLayer *tileoverlay = (GMSTileLayer *)[self.objects objectForKey:key];
        tileoverlay.map = nil;
        tileoverlay = nil;
      }
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;
  
  [self.imgCache removeAllObjects];
  self.imgCache = nil;
  
  key = nil;
  keys = nil;

  NSString *pluginId = [NSString stringWithFormat:@"%@-tileoverlay", self.mapCtrl.mapId];
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  [cdvViewController.pluginObjects removeObjectForKey:pluginId];
  [cdvViewController.pluginsMap setValue:nil forKey:pluginId];
  pluginId = nil;
}

-(void)create:(CDVInvokedUrlCommand *)command
{


  dispatch_async(dispatch_get_main_queue(), ^{

      NSDictionary *json = [command.arguments objectAtIndex:1];
      NSString *tileUrlFormat = [json objectForKey:@"tileUrlFormat"];
  
    
      GMSTileLayer *layer;
    
      NSRange range = [tileUrlFormat rangeOfString:@"http"];
      if (range.location != 0) {
          NSMutableDictionary *options = [[NSMutableDictionary alloc] init];
          
          CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
          id webview = cdvViewController.webView;
          NSURL *url = [webview URL];
          NSString *webPageUrl = url.absoluteString;
          [options setObject:webPageUrl forKey:@"webPageUrl"];
          
          [options setObject:tileUrlFormat forKey:@"tileUrlFormat"];
          [options setObject:[json objectForKey:@"tileSize"] forKey:@"tileSize"];
          
          layer = [[LocalSyncTileLayer alloc] initWithOptions:options];
      } else {
          GMSTileURLConstructor constructor = ^(NSUInteger x, NSUInteger y, NSUInteger zoom) {
              NSString *urlStr = [tileUrlFormat stringByReplacingOccurrencesOfString:@"<x>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)x]];
              urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<y>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)y]];
              urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<zoom>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)zoom]];
              return [NSURL URLWithString:urlStr];
          };
        
          layer = [GMSURLTileLayer tileLayerWithURLConstructor:constructor];
      }
    
    
    

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


      [self.executeQueue addOperationWithBlock:^{

          NSString *id = [NSString stringWithFormat:@"tileoverlay_%lu", (unsigned long)layer.hash];
          [self.objects setObject:layer forKey:id];

          NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
          [result setObject:id forKey:@"id"];
          [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)layer.hash] forKey:@"hashCode"];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  });
}


/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:0];
      GMSTileLayer *layer = (GMSTileLayer *)[self.objects objectForKey:tileLayerKey];
      Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          if (isVisible) {
            layer.map = self.mapCtrl.map;
          } else {
            layer.map = nil;
          }

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });
  }];
}


/**
 * Remove the tile overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:0];
      dispatch_async(dispatch_get_main_queue(), ^{
          GMSTileLayer *layer = (GMSTileLayer *)[self.objects objectForKey:tileLayerKey];
          layer.map = nil;
          [layer clearTileCache];
          [self.objects removeObjectForKey:tileLayerKey];
          layer = nil;
      });

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
  [self.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:0];
      GMSTileLayer *layer = (GMSTileLayer *)[self.objects objectForKey:tileLayerKey];
      NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          [layer setZIndex:(int)zIndex];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });
  }];
}
/**
 * Set fadeIn
 * @params key
 */
-(void)setFadeIn:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:0];
      GMSTileLayer *layer = (GMSTileLayer *)[self.objects objectForKey:tileLayerKey];
      Boolean isEnabled = [[command.arguments objectAtIndex:1] boolValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          [layer setFadeIn:isEnabled];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });
  }];

}


/**
 * Set opacity
 * @params key
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:0];
      GMSTileLayer *layer = (GMSTileLayer *)[self.objects objectForKey:tileLayerKey];
      double opacity = [[command.arguments objectAtIndex:1] doubleValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          [layer setOpacity:opacity];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });

  }];
}
@end
