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

  NSString *pluginId = [NSString stringWithFormat:@"%@-groundoverlay", self.mapCtrl.overlayId];
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  [cdvViewController.pluginObjects removeObjectForKey:pluginId];
  [cdvViewController.pluginsMap setValue:nil forKey:pluginId];
  pluginId = nil;
}

-(void)setPluginViewController:(PluginViewController *)viewCtrl
{
    self.mapCtrl = (PluginMapViewController *)viewCtrl;
}

-(void)create:(CDVInvokedUrlCommand *)command
{
    PluginGroundOverlay *self_ = self;

    NSDictionary *json = [command.arguments objectAtIndex:1];
    NSString *idBase = [command.arguments objectAtIndex:2];
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

        NSString *groundOverlayId = [NSString stringWithFormat:@"groundoverlay_%@", idBase];
        [self.mapCtrl.objects setObject:groundOverlay forKey: groundOverlayId];
        groundOverlay.title = groundOverlayId;
        groundOverlay.anchor = CGPointMake(0.5f, 0.5f);

        if ([json valueForKey:@"zIndex"] && [json valueForKey:@"zIndex"] != [NSNull null]) {
            groundOverlay.zIndex = [[json valueForKey:@"zIndex"] floatValue];
        }

        if ([json valueForKey:@"bearing"] && [json valueForKey:@"bearing"] != [NSNull null]) {
            groundOverlay.bearing = [[json valueForKey:@"bearing"] floatValue];
        }
        if ([json valueForKey:@"anchor"] && [json valueForKey:@"anchor"] != [NSNull null]) {
            NSArray *anchor = [json valueForKey:@"anchor"];
            groundOverlay.anchor = CGPointMake([[anchor objectAtIndex:0] floatValue], [[anchor objectAtIndex:1] floatValue]);
        }

        BOOL isVisible = YES;

        // Visible property
        NSString *visibleValue = [NSString stringWithFormat:@"%@",  json[@"visible"]];
        if ([@"0" isEqualToString:visibleValue]) {
          // false
          isVisible = NO;
          groundOverlay.map = nil;
        } else {
          // true or default
          groundOverlay.map = self.mapCtrl.map;
        }
        BOOL isClickable = NO;
        if ([json valueForKey:@"clickable"] != [NSNull null] && [[json valueForKey:@"clickable"] boolValue]) {
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

                  if ([json valueForKey:@"opacity"] && [json valueForKey:@"opacity"] != [NSNull null]) {
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
                  [result setObject:groundOverlayId forKey:@"__pgmId"];

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
            NSData *decodedData = [[NSData alloc] initWithBase64EncodedString:[tmp objectAtIndex:1] options:0];

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
                  dispatch_sync(dispatch_get_main_queue(), ^{
                       NSURL *url = [webview URL];
                       NSString *currentURL = url.absoluteString;
                       if (![[url lastPathComponent] isEqualToString:@"/"]) {
                         currentURL = [currentURL stringByDeletingLastPathComponent];
                       }

                       // remove page unchor (i.e index.html#page=test, index.html?key=value)
                       NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"[#\\?].*$" options:NSRegularExpressionCaseInsensitive error:nil];
                       currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];

                       // remove file name (i.e /index.html)
                       regex = [NSRegularExpression regularExpressionWithPattern:@"\\/[^\\/]+\\.[^\\/]+$" options:NSRegularExpressionCaseInsensitive error:nil];
                       currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];


                       NSString *urlStr2 = [NSString stringWithFormat:@"%@/%@", currentURL, urlStr];
                       urlStr2 = [urlStr2 stringByReplacingOccurrencesOfString:@":/" withString:@"://"];
                       urlStr2 = [urlStr2 stringByReplacingOccurrencesOfString:@":///" withString:@"://"];
                       url = [NSURL URLWithString:urlStr2];

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
                   });
                   return;
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

        NSString *propertyId = [groundOverlayId stringByReplacingOccurrencesOfString:@"groundoverlay_" withString:@"groundoverlay_property"];
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
        NSString *propertyId = [key stringByReplacingOccurrencesOfString:@"groundoverlay_" withString:@"groundoverlay_property_"];
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
      //GMSGroundOverlay *groundOverlay = (GMSGroundOverlay *)[self.mapCtrl.objects objectForKey:key];
      Boolean isClickable = [[command.arguments objectAtIndex:1] boolValue];

      // Update the property
      NSString *propertyId = [key stringByReplacingOccurrencesOfString:@"groundoverlay_" withString:@"groundoverlay_property_"];
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

    NSString *iconPath = url.absoluteString;

    // Since ionic local server declines HTTP access for some reason,
    // replace URL with file path
    NSBundle *mainBundle = [NSBundle mainBundle];
    NSString *wwwPath = [mainBundle pathForResource:@"www/cordova" ofType:@"js"];
    wwwPath = [wwwPath stringByReplacingOccurrencesOfString:@"/cordova.js" withString:@""];
    if ([iconPath containsString:@"assets/"]) {
      iconPath = [iconPath regReplace:@"^.*assets/" replaceTxt:[NSString stringWithFormat:@"%@/assets/", wwwPath] options:NSRegularExpressionCaseInsensitive];
    }
    // iconPath = [iconPath stringByReplacingOccurrencesOfString:wwwPath withString: @""];
    
    // ionic 4
    iconPath = [iconPath stringByReplacingOccurrencesOfString:@"http://localhost:8080" withString: wwwPath];

    // ionic 5
    iconPath = [iconPath stringByReplacingOccurrencesOfString:@"ionic://localhost" withString: wwwPath];
    
    if ([iconPath hasPrefix:@"file://"] || [iconPath hasPrefix:@"/"]) {
      iconPath = [iconPath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
      if (![iconPath hasPrefix:@"/"]) {
        iconPath = [NSString stringWithFormat:@"/%@", iconPath];
      }
      NSFileManager *fileManager = [NSFileManager defaultManager];
      if (![fileManager fileExistsAtPath:iconPath]) {
        //if (self.mapCtrl.debuggable) {
        NSLog(@"(error)There is no file at '%@'.", iconPath);
        //}
        //[self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        completionBlock(NO, nil);
        return;
      }

      UIImage *image = [UIImage imageNamed:iconPath];
      if (image) {
        completionBlock(YES, image);
        return;
      }
    }



    NSURLRequest *req = [NSURLRequest requestWithURL:url
                                         cachePolicy:NSURLRequestReturnCacheDataElseLoad
                                     timeoutInterval:5];
    NSCachedURLResponse *cachedResponse = [[NSURLCache sharedURLCache] cachedResponseForRequest:req];
    if (cachedResponse != nil) {
      UIImage *image = [[UIImage alloc] initWithData:cachedResponse.data];
      if (image) {
        completionBlock(YES, image);
        return;
      }
    }


    //-------------------------------------------------------------
    // Use NSURLSessionDataTask instead of [NSURLConnection sendAsynchronousRequest]
    // https://stackoverflow.com/a/20871647
    //-------------------------------------------------------------
    NSURLSessionConfiguration *sessionConfiguration = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfiguration];
    NSURLSessionDataTask *getTask = [session dataTaskWithRequest:req
                                               completionHandler:^(NSData *data, NSURLResponse *res, NSError *error) {
                                                 [session finishTasksAndInvalidate];

                                                 UIImage *image = [UIImage imageWithData:data];
                                                 if (image) {
                                                   completionBlock(YES, image);
                                                   return;
                                                 }

                                                 completionBlock(NO, nil);

                                               }];
    [getTask resume];


  }];
}

@end
