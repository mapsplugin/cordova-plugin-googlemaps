//
//  GroundOverlay.m
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/4/13.
//
//

#import "PluginGroundOverlay.h"

@implementation PluginGroundOverlay
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
        if ([key hasPrefix:@"groundoverlay_property"]) {
          key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
          GMSGroundOverlay *groundoverlay = (GMSGroundOverlay *)[self.objects objectForKey:key];
          groundoverlay.map = nil;
          groundoverlay = nil;
        }
        [self.objects removeObjectForKey:key];
    }
    self.objects = nil;
  
  [self.imgCache removeAllObjects];
  self.imgCache = nil;
  
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
      
        NSString *groundOverlayId = [NSString stringWithFormat:@"groundoverlay_%lu", (unsigned long)groundOverlay.hash];
        [self.objects setObject:groundOverlay forKey: groundOverlayId];
        groundOverlay.title = groundOverlayId;

        if ([json valueForKey:@"zIndex"]) {
            groundOverlay.zIndex = [[json valueForKey:@"zIndex"] floatValue];
        }

        if ([json valueForKey:@"bearing"]) {
            groundOverlay.bearing = [[json valueForKey:@"bearing"] floatValue];
        }
      
        BOOL isVisible = NO;
        if ([[json valueForKey:@"visible"] boolValue]) {
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
  

        MYCompletionHandler callback = ^(NSError *error) {
            if (error) {
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;
            }

            NSString *imgId = [NSString stringWithFormat:@"groundoverlay_image_%lu", (unsigned long)groundOverlay.hash];
            [self.imgCache setObject:groundOverlay.icon forKey:imgId];
          
            if ([json valueForKey:@"opacity"]) {
                CGFloat opacity = [[json valueForKey:@"opacity"] floatValue];
                groundOverlay.icon = [groundOverlay.icon imageByApplyingAlpha:opacity];
            }

            
            //---------------------------
            // Keep the properties
            //---------------------------
            NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%lu", (unsigned long)groundOverlay.hash];
        
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
            [self.objects setObject:properties forKey:propertyId];

            //---------------------------
            // Result for JS
            //---------------------------
            NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
            [result setObject:groundOverlayId forKey:@"id"];
            [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)groundOverlay.hash] forKey:@"hashCode"];

            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
            [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

        };

        // Load image
        [self.executeQueue addOperationWithBlock:^{
            NSString *urlStr = [json objectForKey:@"url"];
            if (urlStr) {
                [self _setImage:groundOverlay urlStr:urlStr completionHandler: callback];
            } else {
                callback(nil);
            }
        }];
    });

}

- (void)_setImage:(GMSGroundOverlay *)groundOverlay urlStr:(NSString *)urlStr completionHandler:(MYCompletionHandler)completionHandler {

    NSError *error;
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
                    if (self.mapCtrl.debuggable) {
                        NSLog(@"(debug)Can not convert '%@' to device full path.", urlStr);
                    }
                    NSMutableDictionary* details = [NSMutableDictionary dictionary];
                    [details setValue:[NSString stringWithFormat:@"Can not convert '%@' to device full path.", urlStr] forKey:NSLocalizedDescriptionKey];
                    error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
                    completionHandler(error);
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
                  NSURL *url = [webview URL];
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
                    //if (self.mapCtrl.debuggable) {
                        NSLog(@"(error)There is no file at '%@'.", urlStr);
                    //}
                    NSMutableDictionary* details = [NSMutableDictionary dictionary];
                    [details setValue:[NSString stringWithFormat:@"Can not convert '%@' to device full path.", urlStr] forKey:NSLocalizedDescriptionKey];
                    error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
                    completionHandler(error);
                    return;
                }
            }

            image = [UIImage imageNamed:urlStr];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            groundOverlay.icon = [UIImage imageNamed:urlStr];
            completionHandler(nil);
        });
  
      
    } else {
        NSURL *url = [NSURL URLWithString:urlStr];
        [self downloadImageWithURL:url  completionBlock:^(NSError *error, UIImage *image) {

            if (error) {
              completionHandler(error);
              return;
            }

            dispatch_async(dispatch_get_main_queue(), ^{
                groundOverlay.icon = image;
                completionHandler(nil);
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
    [self.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
      
        [self.imgCache removeObjectForKey:groundOverlayId];
        
        NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%lu", (unsigned long)groundOverlay.hash];
        [self.objects removeObjectForKey:propertyId];
        [self.objects removeObjectForKey:groundOverlayId];

        groundOverlay.map = nil;
        groundOverlay = nil;
        [self.objects removeObjectForKey:groundOverlayId];

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

    [self.executeQueue addOperationWithBlock:^{

        NSString *key = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:key];
        Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];
      
        // Update the property
        NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%lu", (unsigned long)groundOverlay.hash];
        NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                           [self.objects objectForKey:propertyId]];
        [properties setObject:[NSNumber numberWithBool:isVisible] forKey:@"isVisible"];
        [self.objects setObject:properties forKey:propertyId];

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

    [self.executeQueue addOperationWithBlock:^{
    
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
        NSString *urlStr = [command.arguments objectAtIndex:1];
        if (urlStr) {
            __block PluginGroundOverlay *self_ = self;
            [self _setImage:groundOverlay urlStr:urlStr completionHandler:^(NSError *error) {
                CDVPluginResult* pluginResult;
                if (error) {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                } else {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
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
    [self.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
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

    [self.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
        CGFloat opacity = [[command.arguments objectAtIndex:1] floatValue];
      
        
        NSString *imgId = [NSString stringWithFormat:@"groundoverlay_image_%lu", (unsigned long)groundOverlay.hash];
        UIImage *image = [self.imgCache objectForKey:imgId];
        UIImage *prevImage = groundOverlay.icon;
        prevImage = nil;
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            groundOverlay.icon = [image imageByApplyingAlpha:opacity];

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

    [self.executeQueue addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
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
  [self.executeQueue addOperationWithBlock:^{

      NSString *key = [command.arguments objectAtIndex:0];
      GMSGroundOverlay *groundOverlay = (GMSGroundOverlay *)[self.objects objectForKey:key];
      Boolean isClickable = [[command.arguments objectAtIndex:1] boolValue];
    
      // Update the property
      NSString *propertyId = [NSString stringWithFormat:@"groundoverlay_property_%lu", (unsigned long)groundOverlay.hash];
      NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                         [self.objects objectForKey:propertyId]];
      [properties setObject:[NSNumber numberWithBool:isClickable] forKey:@"isClickable"];
      [self.objects setObject:properties forKey:propertyId];

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
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
      
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
    [self.executeQueue addOperationWithBlock:^{
  
        NSURLRequest *req = [NSURLRequest requestWithURL:url
                                          cachePolicy:NSURLRequestReturnCacheDataElseLoad
                                          timeoutInterval:5];
        NSCachedURLResponse *cachedResponse = [[NSURLCache sharedURLCache] cachedResponseForRequest:req];
        if (cachedResponse != nil) {
          UIImage *image = [[UIImage alloc] initWithData:cachedResponse.data];
          completionBlock(YES, image);
          return;
        }

        NSString *uniqueKey = url.absoluteString;
        NSData *cache = [self.imgCache objectForKey:uniqueKey];
        if (cache != nil) {
            UIImage *image = [[UIImage alloc] initWithData:cache];
            completionBlock(YES, image);
            return;
        }



        [NSURLConnection sendAsynchronousRequest:req
              queue:self.executeQueue
              completionHandler:^(NSURLResponse *res, NSData *data, NSError *error) {
                if ( !error ) {
                  [self.imgCache setObject:data forKey:uniqueKey cost:data.length];
                  UIImage *image = [UIImage imageWithData:data];
                  completionBlock(YES, image);
                } else {
                  completionBlock(NO, nil);
                }

        }];
    }];
}

@end
