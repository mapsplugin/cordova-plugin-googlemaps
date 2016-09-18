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
  // Plugin destroy
  NSArray *keys = [self.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      GMSCircle *circle = (GMSCircle *)[self.objects objectForKey:key];
      circle.map = nil;
      circle = nil;
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;
  
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

    NSDictionary *json = [command.arguments objectAtIndex:0];
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
    GMSGroundOverlay *groundOverlay = [GMSGroundOverlay groundOverlayWithBounds:bounds icon:nil];
  
    NSString *groundGverlayId = [NSString stringWithFormat:@"groundOverlay_%lu", (unsigned long)groundOverlay.hash];
    [self.objects setObject:groundOverlay forKey: groundGverlayId];
    groundOverlay.title = groundGverlayId;

    if ([json valueForKey:@"zIndex"]) {
        groundOverlay.zIndex = [[json valueForKey:@"zIndex"] floatValue];
    }

    if ([json valueForKey:@"bearing"]) {
        groundOverlay.bearing = [[json valueForKey:@"bearing"] floatValue];
    }
  
            if ([[json valueForKey:@"visible"] boolValue]) {
                //layer.map = self.mapCtrl.map;
            }

    MYCompletionHandler callback = ^(NSError *error) {
        if (error) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
            [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }

        if ([json valueForKey:@"opacity"]) {
            CGFloat opacity = [[json valueForKey:@"opacity"] floatValue];
            groundOverlay.icon = [groundOverlay.icon imageByApplyingAlpha:opacity];
        }

        groundOverlay.tappable = NO;


        NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    };

    // Load image
    NSString *urlStr = [json objectForKey:@"url"];
    if (urlStr) {
        [self _setImage:groundOverlay urlStr:urlStr completionHandler: callback];
    } else {
        callback(nil);
    }

}

- (void)_setImage:(GMSGroundOverlay *)groundOverlay urlStr:(NSString *)urlStr completionHandler:(MYCompletionHandler)completionHandler {

    NSError *error;
    NSRange range = [urlStr rangeOfString:@"http"];
    if (range.location != 0) {
    
        range = [urlStr rangeOfString:@"://"];
        
        if (range.location == NSNotFound) {
            range = [urlStr rangeOfString:@"www/"];
            if (range.location == NSNotFound) {
                range = [urlStr rangeOfString:@"/"];
                if (range.location != 0) {
                    urlStr = [NSString stringWithFormat:@"./%@", urlStr];
                }
            }
        }


        range = [urlStr rangeOfString:@"://"];
        if (range.location == NSNotFound) {
            range = [urlStr rangeOfString:@"www/"];
            if (range.location == NSNotFound) {
                urlStr = [NSString stringWithFormat:@"www/%@", urlStr];
            }

            range = [urlStr rangeOfString:@"/"];
            if (range.location != 0) {
              // Get the absolute path of the www folder.
              // https://github.com/apache/cordova-plugin-file/blob/1e2593f42455aa78d7fff7400a834beb37a0683c/src/ios/CDVFile.m#L506
              NSString *applicationDirectory = [[NSURL fileURLWithPath:[[NSBundle mainBundle] resourcePath]] absoluteString];
              urlStr = [NSString stringWithFormat:@"%@%@", applicationDirectory, urlStr];
            } else {
              urlStr = [NSString stringWithFormat:@"file://%@", urlStr];
            }
        }


        range = [urlStr rangeOfString:@"cdvfile://"];
        if (range.location != NSNotFound) {
            urlStr = [PluginUtil getAbsolutePathFromCDVFilePath:self.webView cdvFilePath:urlStr];
            if (urlStr == nil) {
                NSMutableDictionary* details = [NSMutableDictionary dictionary];
                [details setValue:[NSString stringWithFormat:@"Can not convert '%@' to device full path.", urlStr] forKey:NSLocalizedDescriptionKey];
                error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
            }
        }

        range = [urlStr rangeOfString:@"file://"];
        if (range.location != NSNotFound) {
            urlStr = [urlStr stringByReplacingOccurrencesOfString:@"file://" withString:@""];
            NSFileManager *fileManager = [NSFileManager defaultManager];
            if (![fileManager fileExistsAtPath:urlStr]) {
                NSMutableDictionary* details = [NSMutableDictionary dictionary];
                [details setValue:[NSString stringWithFormat:@"There is no file at '%@'.", urlStr] forKey:NSLocalizedDescriptionKey];
                error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
            }
        }
        // If there is an error, return
        if (error) {
            completionHandler(error);
            return;
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
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
        Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

        if (isVisible) {
            groundOverlay.map = self.mapCtrl.map;
        } else {
            groundOverlay.map = nil;
        }

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
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
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
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
        [groundOverlay setBounds:bounds];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

/**
 * Set opacity
 * @params key
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
        CGFloat opacity = [[command.arguments objectAtIndex:1] floatValue];
        groundOverlay.icon = [groundOverlay.icon imageByApplyingAlpha:opacity];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

/**
 * Set bearing
 * @params key
 */
-(void)setBearing:(CDVInvokedUrlCommand *)command
{

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
        groundOverlay.bearing = [[command.arguments objectAtIndex:1] floatValue];
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

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
        NSString *groundOverlayId = [command.arguments objectAtIndex:0];
        GMSGroundOverlay *groundOverlay = [self.objects objectForKey:groundOverlayId];
      
        NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
        [groundOverlay setZIndex:(int)zIndex];
      
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
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
