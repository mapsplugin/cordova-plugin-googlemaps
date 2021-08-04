//
//  PluginPluginTileOverlay.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginTileOverlay.h"

@implementation PluginTileOverlay

-(void)setPluginViewController:(PluginViewController *)viewCtrl
{
  self.mapCtrl = (PluginMapViewController *)viewCtrl;
}
- (void)pluginInitialize
{
  if (self.initialized) {
    return;
  }
  self.initialized = YES;
  [super pluginInitialize];
  // Initialize this plugin
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
  NSArray *keys = [self.mapCtrl.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      if ([key hasPrefix:@"tileoverlay_property"]) {
        key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
        GMSTileLayer *tileoverlay = (GMSTileLayer *)[self.mapCtrl.objects objectForKey:key];
        tileoverlay.map = nil;
        tileoverlay = nil;
      }
      [self.mapCtrl.objects removeObjectForKey:key];
  }

  [self.imgCache removeAllObjects];
  self.imgCache = nil;

  key = nil;
  keys = nil;
  
  NSString *pluginId = [NSString stringWithFormat:@"%@-tileoverlay", self.mapCtrl.overlayId];
  [self.mapCtrl.plugins removeObjectForKey:pluginId];
}

-(PluginTileOverlay *)_getInstance: (NSString *)mapId {
  NSString *pluginId = [NSString stringWithFormat:@"%@-tileoverlay", mapId];
  PluginMap *mapInstance = [CordovaGoogleMaps getViewPlugin:mapId];
  return [mapInstance.mapCtrl.plugins objectForKey:pluginId];
}
-(void)create:(CDVInvokedUrlCommand *)command
{
  
  dispatch_async(dispatch_get_main_queue(), ^{

      NSDictionary *json = [command.arguments objectAtIndex:2];
      NSString *idBase = [command.arguments objectAtIndex:3];
      //NSString *tileUrlFormat = [json objectForKey:@"tileUrlFormat"];


      GMSTileLayer *layer;
      NSString *_id = [NSString stringWithFormat:@"tileoverlay_%@", idBase];

      //NSRange range = [tileUrlFormat rangeOfString:@"http"];
      //if (range.location != 0) {
          NSMutableDictionary *options = [[NSMutableDictionary alloc] init];
    
          NSURL *url = [(WKWebView *)self.webView URL];
          NSString *webPageUrl = url.absoluteString;
          [options setObject:webPageUrl forKey:@"webPageUrl"];
          [options setObject:self.mapCtrl.overlayId forKey:@"mapId"];
          [options setObject:idBase forKey:@"pluginId"];

          ///[options setObject:tileUrlFormat forKey:@"tileUrlFormat"];
          [options setObject:[json objectForKey:@"tileSize"] forKey:@"tileSize"];
          [options setObject:[json objectForKey:@"debug"] forKey:@"debug"];

          layer = [[PluginTileProvider alloc] initWithOptions:options webView:self.webView];
      /*
  } else {
          GMSTileURLConstructor constructor = ^(NSUInteger x, NSUInteger y, NSUInteger zoom) {
              NSString *urlStr = [tileUrlFormat stringByReplacingOccurrencesOfString:@"<x>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)x]];
              urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<y>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)y]];
              urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<zoom>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)zoom]];
              return [NSURL URLWithString:urlStr];
          };
          layer = [GMSURLTileLayer tileLayerWithURLConstructor:constructor];
      }
       */



      // Visible property
      NSString *visibleValue = [NSString stringWithFormat:@"%@",  json[@"visible"]];
      if ([@"0" isEqualToString:visibleValue]) {
        // false
        layer.map = nil;
      } else {
        // true or default
        layer.map = self.mapCtrl.map;
      }
      if ([json valueForKey:@"zIndex"] && [json valueForKey:@"zIndex"] != [NSNull null]) {
        layer.zIndex = [[json valueForKey:@"zIndex"] floatValue];
      }
      if ([json valueForKey:@"tileSize"] && [json valueForKey:@"tileSize"] != [NSNull null]) {
        layer.tileSize = [[json valueForKey:@"tileSize"] integerValue];
      }
      if ([json valueForKey:@"opacity"] && [json valueForKey:@"opacity"] != [NSNull null]) {
        layer.opacity = [[json valueForKey:@"opacity"] floatValue];
      }


      [self.executeQueue addOperationWithBlock:^{

          [self.mapCtrl.objects setObject:layer forKey:_id];

          NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
          [result setObject:_id forKey:@"__pgmId"];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      }];
  });

}


-(void)onGetTileUrlFromJS:(CDVInvokedUrlCommand *)command
{
  
  NSString *pluginId = [command.arguments objectAtIndex:0];
  PluginTileOverlay *tOverlayInstance = [self _getInstance:pluginId];

  [tOverlayInstance.executeQueue addOperationWithBlock:^{
    NSString *_id = [command.arguments objectAtIndex:1];
    NSString *urlKey = [command.arguments objectAtIndex:2];
    NSString *tileUrl = [command.arguments objectAtIndex:3];
    NSString *pluginId = [NSString stringWithFormat:@"tileoverlay_%@", _id];
    GMSTileLayer *tileLayer = [tOverlayInstance.mapCtrl.objects objectForKey:pluginId];


    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    if (tileLayer) {
        PluginTileProvider *localLayer =(PluginTileProvider *)tileLayer;
        [localLayer onGetTileUrlFromJS:urlKey tileUrl:tileUrl];
    }
    [tOverlayInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  }];
}

/**
 * Set visibility
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginTileOverlay *tOverlayInstance = [self _getInstance:mapId];

  [tOverlayInstance.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:1];
      GMSTileLayer *layer = (GMSTileLayer *)[tOverlayInstance.mapCtrl.objects objectForKey:tileLayerKey];
    
      Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          if (isVisible) {
            layer.map = self.mapCtrl.map;
          } else {
            layer.map = nil;
          }
      });

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [tOverlayInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}


/**
 * Remove the tile overlay
 * 
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginTileOverlay *tOverlayInstance = [self _getInstance:mapId];

  [tOverlayInstance.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:1];
      dispatch_async(dispatch_get_main_queue(), ^{
          GMSTileLayer *layer = (GMSTileLayer *)[tOverlayInstance.mapCtrl.objects objectForKey:tileLayerKey];
          layer.map = nil;
          [layer clearTileCache];
          [tOverlayInstance.mapCtrl.objects removeObjectForKey:tileLayerKey];
          layer = nil;
      });

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [tOverlayInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}


/**
 * Set z-index
 * 
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginTileOverlay *tOverlayInstance = [self _getInstance:mapId];

  [tOverlayInstance.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:1];
      GMSTileLayer *layer = (GMSTileLayer *)[tOverlayInstance.mapCtrl.objects objectForKey:tileLayerKey];
      NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          [layer setZIndex:(int)zIndex];
      });

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [tOverlayInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}
/**
 * Set fadeIn
 * 
 */
-(void)setFadeIn:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginTileOverlay *tOverlayInstance = [self _getInstance:mapId];

  [tOverlayInstance.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:1];
      GMSTileLayer *layer = (GMSTileLayer *)[tOverlayInstance.mapCtrl.objects objectForKey:tileLayerKey];
      Boolean isEnabled = [[command.arguments objectAtIndex:2] boolValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          [layer setFadeIn:isEnabled];
      });

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [tOverlayInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}


/**
 * Set opacity
 * 
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginTileOverlay *tOverlayInstance = [self _getInstance:mapId];

  [tOverlayInstance.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:1];
      GMSTileLayer *layer = (GMSTileLayer *)[tOverlayInstance.mapCtrl.objects objectForKey:tileLayerKey];
      double opacity = [[command.arguments objectAtIndex:2] doubleValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          [layer setOpacity:opacity];
      });

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [tOverlayInstance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  }];
}
@end
