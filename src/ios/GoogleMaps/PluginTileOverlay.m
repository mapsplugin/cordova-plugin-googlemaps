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
   #ifdef PGM_PLATFORM_CAPACITOR
    CDVCommandDelegateImpl *delegate = self.commandDelegate;
    [delegate.manager.pluginObjects removeObjectForKey:pluginId];
    [delegate.manager.pluginsMap setValue:nil forKey:pluginId];
   #endif
   #ifdef PGM_PLATFORM_CORDOVA
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    [cdvViewController.pluginObjects removeObjectForKey:pluginId];
    [cdvViewController.pluginsMap setValue:nil forKey:pluginId];
   #endif
  pluginId = nil;
}

-(void)create:(CDVInvokedUrlCommand *)command
{


  dispatch_async(dispatch_get_main_queue(), ^{

      NSDictionary *json = [command.arguments objectAtIndex:1];
      NSString *idBase = [command.arguments objectAtIndex:2];
      //NSString *tileUrlFormat = [json objectForKey:@"tileUrlFormat"];


      GMSTileLayer *layer;
      NSString *_id = [NSString stringWithFormat:@"tileoverlay_%@", idBase];

      //NSRange range = [tileUrlFormat rangeOfString:@"http"];
      //if (range.location != 0) {
          NSMutableDictionary *options = [[NSMutableDictionary alloc] init];

          NSURL *url;
          id webview;
            #ifdef PGM_PLATFORM_CORDOVA
            CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
            webview = cdvViewController.webView;
            NSString *clsName = [webview className];
            if ([clsName isEqualToString:@"UIWebView"]) {
              url = ((UIWebView *)cdvViewController.webView).request.URL;
            } else {
              url = [webview URL];
            }
            #endif
            #ifdef PGM_PLATFORM_CAPACITOR
            url = [(WKWebView *)self.webView URL];
            webview = self.webView;
            #endif
          NSString *webPageUrl = url.absoluteString;
          [options setObject:webPageUrl forKey:@"webPageUrl"];
          [options setObject:self.mapCtrl.overlayId forKey:@"mapId"];
          [options setObject:idBase forKey:@"pluginId"];

          ///[options setObject:tileUrlFormat forKey:@"tileUrlFormat"];
          [options setObject:[json objectForKey:@"tileSize"] forKey:@"tileSize"];
          [options setObject:[json objectForKey:@"debug"] forKey:@"debug"];

          layer = [[PluginTileProvider alloc] initWithOptions:options webView:webview];
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

  [self.executeQueue addOperationWithBlock:^{
    NSString *_id = [command.arguments objectAtIndex:0];
    NSString *urlKey = [command.arguments objectAtIndex:1];
    NSString *tileUrl = [command.arguments objectAtIndex:2];
    NSString *pluginId = [NSString stringWithFormat:@"tileoverlay_%@", _id];
    GMSTileLayer *tileLayer = [self.mapCtrl.objects objectForKey:pluginId];


    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    if (tileLayer) {
        PluginTileProvider *localLayer =(PluginTileProvider *)tileLayer;
        [localLayer onGetTileUrlFromJS:urlKey tileUrl:tileUrl];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  }];
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{

  [self.executeQueue addOperationWithBlock:^{
      NSString *tileLayerKey = [command.arguments objectAtIndex:0];
      GMSTileLayer *layer = (GMSTileLayer *)[self.mapCtrl.objects objectForKey:tileLayerKey];
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
          GMSTileLayer *layer = (GMSTileLayer *)[self.mapCtrl.objects objectForKey:tileLayerKey];
          layer.map = nil;
          [layer clearTileCache];
          [self.mapCtrl.objects removeObjectForKey:tileLayerKey];
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
      GMSTileLayer *layer = (GMSTileLayer *)[self.mapCtrl.objects objectForKey:tileLayerKey];
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
      GMSTileLayer *layer = (GMSTileLayer *)[self.mapCtrl.objects objectForKey:tileLayerKey];
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
      GMSTileLayer *layer = (GMSTileLayer *)[self.mapCtrl.objects objectForKey:tileLayerKey];
      double opacity = [[command.arguments objectAtIndex:1] doubleValue];
      dispatch_async(dispatch_get_main_queue(), ^{
          [layer setOpacity:opacity];

          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });

  }];
}
@end
