//
//  PluginGroundOverlay.m
//  cordova-googlemaps-plugin v2
//
//  Created by Katsumata Masashi.
//
//

#import "PluginGroundOverlay.h"

@implementation PluginGroundOverlay
- (void)pluginInitialize
{
  if (self.initialized) {
    return;
  }
  [super pluginInitialize];
  // Initialize this plugin
  //self.imgCache = [[NSCache alloc]init];
  //self.imgCache.totalCostLimit = 3 * 1024 * 1024 * 1024; // 3MB = Cache for image
}

- (void)pluginUnload
{


    // Plugin destroy
    NSArray *keys = [self.mapCtrl.objects allKeys];
    NSString *key;
    for (int i = 0; i < [keys count]; i++) {
        key = [keys objectAtIndex:i];
        if ([key hasPrefix:@"groundoverlay_property"]) {
          key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
          GMSGroundOverlay *groundoverlay = (GMSGroundOverlay *)[self.mapCtrl.objects objectForKey:key];
          groundoverlay.map = nil;
          groundoverlay = nil;
        }
        [self.mapCtrl.objects removeObjectForKey:key];
    }

  //[self.imgCache removeAllObjects];
  //self.imgCache = nil;

  key = nil;
  keys = nil;

  NSString *pluginId = [NSString stringWithFormat:@"%@-groundoverlay", self.mapCtrl.mapId];
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
    PluginGroundOverlay *self_ = self;

    NSDictionary *json = [command.arguments objectAtIndex:1];
    NSArray *points = [json objectForKey:@"bounds"];

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

    dispatch_async(dispatch_get_main_queue(), ^{
        GMSGroundOverlay *groundOverlay = [GMSGroundOverlay groundOverlayWithBounds:bounds icon:nil];

        NSString *idBase = [NSString stringWithFormat:@"%lu%d", command.hash, arc4random() % 100000];
        NSString *groundOverlayId = [NSString stringWithFormat:@"groundoverlay_%@", idBase];
        [self.mapCtrl.objects setObject:groundOverlay forKey: groundOverlayId];
        groundOverlay.title = groundOverlayId;

        if ([json valueForKey:@"zIndex"]) {
            groundOverlay.zIndex = [[json valueForKey:@"zIndex"] floatValue];
        }

        if ([json valueForKey:@"bearing"]) {
            groundOverlay.bearing = [[json valueForKey:@"bearing"] floatValue];
        }

        BOOL isVisible = NO;
        if (json[@"visible"]) {
            groundOverlay.map = self.mapCtrl.map;
            isVisible = YES;
        }
        BOOL isClickable = NO;
        if ([[json valueForKey:@"clickable"] boolValue]) {
            isClickable = YES;
        }


        // Since this plugin uses own touch-detection,
        // set NO to the tappable property.
        groundOverlay.tappable = NO;

        __block PluginGroundOverlay *me = self;

        // Load image
        [self.mapCtrl.executeQueue addOperationWithBlock:^{
            NSString *urlStr = [json objectForKey:@"url"];
            if (urlStr) {
                [self _setImage:groundOverlay urlStr:urlStr completionHandler:^(BOOL successed) {
                  if (!successed) {
                      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                      [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                      return;
                  }

                  //NSString *imgId = [NSString stringWithFormat:@"groundoverlay_image_%lu", (unsigned long)groundOverlay.hash];
                  //[me.imgCache setObject:groundOverlay.icon forKey:imgId];

                  if ([json valueForKey:@"opacity"]) {
                      CGFloat opacity = [[json valueForKey:@"opacity"] floatValue];
                      groundOverlay.icon = [groundOverlay.icon imageByApplyingAlpha:opacity];
                  }


                  //---------------------------
                  // Keep the properties
                  //---------------------------
                  NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%@", idBase];

                  // points
                  NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
                  // bounds (pre-calculate for click detection)
                  [properties setObject:groundOverlay.bounds  forKey:@"bounds"];
                  // isVisible
                  [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
                  // isClickable
                  [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
                  // zIndex
                  [properties setObject:[NSNumber numberWithFloat:groundOverlay.zIndex] forKey:@"zIndex"];;
                  [me.mapCtrl.objects setObject:properties forKey:propertyId];

                  //---------------------------
                  // Result for JS
                  //---------------------------
                  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
                  [result setObject:groundOverlayId forKey:@"id"];

                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
                  [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

              }];
            } else {
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Error: The url property is not specified."];
                [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
        }];
    });

}

- (void)_setImage:(GMSGroundOverlay *)groundOverlay urlStr:(NSString *)urlStr completionHandler:(void (^)(BOOL succeeded))completionHandler {

    NSRange range = [urlStr rangeOfString:@"http"];

    if (range.location != 0) {
        /**
         * Load icon from file or Base64 encoded strings
         */

        UIImage *image;
        if ([urlStr rangeOfString:@"data:image/"].location != NSNotFound &&
            [urlStr rangeOfString:@";base64,"].location != NSNotFound) {

            /**
             * Base64 icon
             */
            NSArray *tmp = [urlStr componentsSeparatedByString:@","];

            NSData *decodedData = [NSData dataFromBase64String:tmp[1]];
            image = [[UIImage alloc] initWithData:decodedData];

        } else {
            /**
             * Load the icon from local path
             */
            range = [urlStr rangeOfString:@"cdvfile://"];
            if (range.location != NSNotFound) {

                urlStr = [PluginUtil getAbsolutePathFromCDVFilePath:self.webView cdvFilePath:urlStr];

                if (urlStr == nil) {
                    NSLog(@"(error)Can not convert '%@' to device full path.", urlStr);
                    completionHandler(NO);
                    return;
                }
            }

            range = [urlStr rangeOfString:@"://"];
            if (range.location == NSNotFound) {

                range = [urlStr rangeOfString:@"/"];
                if (range.location != 0) {
                  // Get the current URL, then calculate the relative path.
                  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
                  id webview = cdvViewController.webView;
                  NSString *clsName = [webview className];
                  NSURL *url;
                  if ([clsName isEqualToString:@"UIWebView"]) {
                    url = ((UIWebView *)cdvViewController.webView).request.URL;
                  } else {
                    url = [webview URL];
                  }
                  NSString *currentURL = url.absoluteString;
                  currentURL = [currentURL stringByDeletingLastPathComponent];
                  currentURL = [currentURL stringByReplacingOccurrencesOfString:@"file:" withString:@""];
                  currentURL = [currentURL stringByReplacingOccurrencesOfString:@"//" withString:@"/"];
                  urlStr = [NSString stringWithFormat:@"file://%@/%@", currentURL, urlStr];
                } else {
                  urlStr = [NSString stringWithFormat:@"file://%@", urlStr];
                }
            }


            range = [urlStr rangeOfString:@"file://"];
            if (range.location != NSNotFound) {
                urlStr = [urlStr stringByReplacingOccurrencesOfString:@"file://" withString:@""];
                NSFileManager *fileManager = [NSFileManager defaultManager];
                if (![fileManager fileExistsAtPath:urlStr]) {
                    NSLog(@"(error)There is no file at '%@'.", urlStr);
                    completionHandler(NO);
                    return;
                }
            }

            image = [UIImage imageNamed:urlStr];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            groundOverlay.icon = [UIImage imageNamed:urlStr];
            completionHandler(YES);
        });


    } else {
        NSURL *url = [NSURL URLWithString:urlStr];
        [self downloadImageWithURL:url  completionBlock:^(BOOL succeeded, UIImage *image) {

            if (!succeeded) {
              completionHandler(NO);
              return;
            }

            dispatch_async(dispatch_get_main_queue(), ^{
                groundOverlay.icon = image;
                completionHandler(YES);
            });

        }];
    }


}

/**
 * Remove the ground overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.mapCtrl.objects objectForKey:groundOverlayId];

        //[self.imgCache removeObjectForKey:groundOverlayId];

        NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%lu", (unsigned long)groundOverlay.hash];
        [self.mapCtrl.objects removeObjectForKey:propertyId];
        [self.mapCtrl.objects removeObjectForKey:groundOverlayId];

        groundOverlay.map = nil;
        groundOverlay = nil;
        [self.mapCtrl.objects removeObjectForKey:groundOverlayId];

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

    [self.mapCtrl.executeQueue addOperationWithBlock:^{

        NSString *key = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.mapCtrl.objects objectForKey:key];
        Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

        // Update the property
        NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%lu", (unsigned long)groundOverlay.hash];
        NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                           [self.mapCtrl.objects objectForKey:propertyId]];
        [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
        [self.mapCtrl.objects setObject:properties forKey:propertyId];

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            if (isVisible) {
              groundOverlay.map = self.mapCtrl.map;
            } else {
              groundOverlay.map = nil;
            }

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];

    }];
}

/**
 * set image
 * @params key
 */
-(void)setImage:(CDVInvokedUrlCommand *)command
{

    [self.mapCtrl.executeQueue addOperationWithBlock:^{

        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.mapCtrl.objects objectForKey:groundOverlayId];
        NSString *urlStr = [command.arguments objectAtIndex:1];
        if (urlStr) {
            __block PluginGroundOverlay *self_ = self;
            [self _setImage:groundOverlay urlStr:urlStr completionHandler:^(BOOL successed) {
                CDVPluginResult* pluginResult;
                if (successed) {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                } else {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                }

                [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }];
        } else {

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }

    }];

}

/**
 * Set bounds
 * @params key
 */
-(void)setBounds:(CDVInvokedUrlCommand *)command
{
    [self.mapCtrl.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.mapCtrl.objects objectForKey:groundOverlayId];
        GMSMutablePath *path = [GMSMutablePath path];

        NSArray *points = [command.arguments objectAtIndex:1];
        int i = 0;
        NSDictionary *latLng;
        for (i = 0; i < points.count; i++) {
            latLng = [points objectAtIndex:i];
            [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
        }
        GMSCoordinateBounds *bounds;
        bounds = [[GMSCoordinateBounds alloc] initWithPath:path];

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [groundOverlay setBounds:bounds];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }];
}

/**
 * Set opacity
 * @params key
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{

    [self.mapCtrl.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.mapCtrl.objects objectForKey:groundOverlayId];
        CGFloat opacity = [[command.arguments objectAtIndex:1] floatValue];

        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            groundOverlay.opacity = opacity;

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }];
}

/**
 * Set bearing
 * @params key
 */
-(void)setBearing:(CDVInvokedUrlCommand *)command
{

    [self.mapCtrl.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.mapCtrl.objects objectForKey:groundOverlayId];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            groundOverlay.bearing = [[command.arguments objectAtIndex:1] floatValue];
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];

    }];

}


/**
 * Set clickable
 * @params key
 */
-(void)setClickable:(CDVInvokedUrlCommand *)command
{
  [self.mapCtrl.executeQueue addOperationWithBlock:^{

      NSString *key = [command.arguments objectAtIndex:0];
      GMSGroundOverlay *groundOverlay = (GMSGroundOverlay *)[self.mapCtrl.objects objectForKey:key];
      Boolean isClickable = [[command.arguments objectAtIndex:1] boolValue];

      // Update the property
      NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%lu", (unsigned long)groundOverlay.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.mapCtrl.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
      [self.mapCtrl.objects setObject:properties forKey:propertyId];

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

    [self.mapCtrl.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.mapCtrl.objects objectForKey:groundOverlayId];

        NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            [groundOverlay setZIndex:(int)zIndex];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    }];
}



- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock
{
    [self.mapCtrl.executeQueue addOperationWithBlock:^{

        NSURLRequest *req = [NSURLRequest requestWithURL:url
                                          cachePolicy:NSURLRequestReturnCacheDataElseLoad
                                          timeoutInterval:5];
        NSCachedURLResponse *cachedResponse = [[NSURLCache sharedURLCache] cachedResponseForRequest:req];
        if (cachedResponse != nil) {
          UIImage *image = [[UIImage alloc] initWithData:cachedResponse.data];
          completionBlock(YES, image);
          return;
        }


        [NSURLConnection sendAsynchronousRequest:req
              queue:self.mapCtrl.executeQueue
              completionHandler:^(NSURLResponse *res, NSData *data, NSError *error) {
                if ( !error ) {
                  UIImage *image = [UIImage imageWithData:data];
                  completionBlock(YES, image);
                } else {
                  completionBlock(NO, nil);
                }

        }];
    }];
}

@end
