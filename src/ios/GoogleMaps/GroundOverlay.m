//
//  GroundOverlay.m
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/4/13.
//
//

#import "GroundOverlay.h"

@implementation GroundOverlay

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
    self.mapCtrl = viewCtrl;
}

-(void)create:(CDVInvokedUrlCommand *)command
{
    // Initialize this plugin
    if (self.mapCtrl == nil) {
        CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
        GoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"GoogleMaps"];
        self.mapCtrl = googlemaps.mapCtrl;
        [self.mapCtrl.plugins setObject:self forKey:@"GroundOverlay"];
    }
  
    __block GMSGroundOverlay *layer;
    __block GroundOverlay *self_ = self;
  
    dispatch_queue_t gueue = dispatch_queue_create("createGroundOverlay", NULL);
    dispatch_async(gueue, ^{
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
        
        dispatch_sync(dispatch_get_main_queue(), ^{
            layer = [GMSGroundOverlay groundOverlayWithBounds:bounds icon:nil];

            if ([[json valueForKey:@"visible"] boolValue]) {
                layer.map = self.mapCtrl.map;
            }
            if ([json valueForKey:@"zIndex"]) {
                layer.zIndex = [[json valueForKey:@"zIndex"] floatValue];
            }

            dispatch_async(gueue, ^{
            
                MYCompletionHandler callback = ^(NSError *error) {
                    if (error) {
                        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                        [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                        return;
                    }

                    if ([json valueForKey:@"opacity"]) {
                        CGFloat opacity = [[json valueForKey:@"opacity"] floatValue];
                        layer.icon = [layer.icon imageByApplyingAlpha:opacity];
                    }
                    if ([json valueForKey:@"bearing"]) {
                        layer.bearing = [[json valueForKey:@"bearing"] floatValue];
                    }

                    layer.tappable = YES;

                    NSString *id = [NSString stringWithFormat:@"groundOverlay_%lu", (unsigned long)layer.hash];
                    [self_.mapCtrl.overlayManager setObject:layer forKey: id];
                    layer.title = id;

                    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
                    [result setObject:id forKey:@"id"];
                    [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)layer.hash] forKey:@"hashCode"];

                    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
                    [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

                };

                NSString *urlStr = [json objectForKey:@"url"];
                if (urlStr) {
                    [self _setImage:layer urlStr:urlStr completionHandler: callback];
                } else {
                    callback(nil);
                }

            });
        });
    });
}

- (void)_setImage:(GMSGroundOverlay *)layer urlStr:(NSString *)urlStr completionHandler:(MYCompletionHandler)completionHandler {

    __block NSString *urlStrStatic = urlStr;

    dispatch_queue_t gueue = dispatch_queue_create("_setImgForGroundOverlay", NULL);
    dispatch_async(gueue, ^{
        NSString *id = [NSString stringWithFormat:@"groundOverlay_icon_%lu", (unsigned long)layer.hash];

        NSError *error;
        NSRange range = [urlStrStatic rangeOfString:@"://"];
        if (range.location == NSNotFound) {
            range = [urlStrStatic rangeOfString:@"www/"];
            if (range.location == NSNotFound) {
                range = [urlStrStatic rangeOfString:@"/"];
                if (range.location != 0) {
                    urlStrStatic = [NSString stringWithFormat:@"./%@", urlStrStatic];
                }
            }
        }

        range = [urlStrStatic rangeOfString:@"./"];
        if (range.location != NSNotFound) {
            SEL requestSelector = NSSelectorFromString(@"request");
            SEL urlSelector = NSSelectorFromString(@"URL");
            NSString *currentPath = @"";
            if ([self.webView respondsToSelector:requestSelector]) {
              NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[[self.webView class] instanceMethodSignatureForSelector:requestSelector]];
              [invocation setSelector:requestSelector];
              [invocation setTarget:self.webView];
              [invocation invoke];
              NSURLRequest *request;
              [invocation getReturnValue:&request];
              currentPath = [request.URL absoluteString];
            } else if ([self.webView respondsToSelector:urlSelector]) {
              NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[[self.webView class] instanceMethodSignatureForSelector:urlSelector]];
              [invocation setSelector:urlSelector];
              [invocation setTarget:self.webView];
              [invocation invoke];
              NSURL *URL;
              [invocation getReturnValue:&URL];
              currentPath = [URL absoluteString];
            }
            NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"[^\\/]*$" options:NSRegularExpressionCaseInsensitive error:&error];
            currentPath= [regex stringByReplacingMatchesInString:currentPath options:0 range:NSMakeRange(0, [currentPath length]) withTemplate:@""];
            urlStrStatic = [urlStrStatic stringByReplacingOccurrencesOfString:@"./" withString:currentPath];
        }

        range = [urlStrStatic rangeOfString:@"cdvfile://"];
        if (range.location != NSNotFound) {
            urlStrStatic = [PluginUtil getAbsolutePathFromCDVFilePath:self.webView cdvFilePath:urlStrStatic];
            if (urlStrStatic == nil) {
                NSMutableDictionary* details = [NSMutableDictionary dictionary];
                [details setValue:[NSString stringWithFormat:@"Can not convert '%@' to device full path.", urlStrStatic] forKey:NSLocalizedDescriptionKey];
                error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
            }
        }

        range = [urlStrStatic rangeOfString:@"file://"];
        if (range.location != NSNotFound) {
            urlStrStatic = [urlStrStatic stringByReplacingOccurrencesOfString:@"file://" withString:@""];
            NSFileManager *fileManager = [NSFileManager defaultManager];
            if (![fileManager fileExistsAtPath:urlStrStatic]) {
                NSMutableDictionary* details = [NSMutableDictionary dictionary];
                [details setValue:[NSString stringWithFormat:@"There is no file at '%@'.", urlStrStatic] forKey:NSLocalizedDescriptionKey];
                error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
            }
        }

        // If there is an error, return
        if (error) {
            completionHandler(error);
            return;
        }



        if ([urlStrStatic hasPrefix:@"http://"] || [urlStrStatic hasPrefix:@"https://"]) {
            
            NSURL *url = [NSURL URLWithString:urlStrStatic];
            [self downloadImageWithURL:url  completionBlock:^(NSError *error, UIImage *image) {
            
                if (error) {
                  completionHandler(error);
                  return;
                }
              
                dispatch_sync(dispatch_get_main_queue(), ^{
                    NSURL *url = [NSURL URLWithString:urlStrStatic];
                    NSData *data = [NSData dataWithContentsOfURL:url];
                    UIImage *layerImg = [UIImage imageWithData:data];
                    layer.icon = layerImg;
                    [self.mapCtrl.overlayManager setObject:layerImg forKey: id];
                    completionHandler(nil);
                });
              
            }];

        } else {
            dispatch_sync(dispatch_get_main_queue(), ^{
                layer.icon = [UIImage imageNamed:urlStrStatic];
                [self.mapCtrl.overlayManager setObject:layer.icon forKey: id];
                completionHandler(nil);
            });
        }
    });
}

/**
 * Remove the ground overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
    NSString *key = [command.arguments objectAtIndex:0];
    GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
    NSString *id = [NSString stringWithFormat:@"groundOverlay_icon_%lu", (unsigned long)layer.hash];
    layer.map = nil;
    [self.mapCtrl removeObjectForKey:key];
    [self.mapCtrl removeObjectForKey:id];
    layer = nil;

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
    NSString *key = [command.arguments objectAtIndex:0];
    GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
    Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

    if (isVisible) {
        layer.map = self.mapCtrl.map;
    } else {
        layer.map = nil;
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * set image
 * @params key
 */
-(void)setImage:(CDVInvokedUrlCommand *)command
{
    NSString *key = [command.arguments objectAtIndex:0];
    GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];

    NSString *urlStr = [command.arguments objectAtIndex:1];
    if (urlStr) {
        __block GroundOverlay *self_ = self;
        [self _setImage:layer urlStr:urlStr completionHandler:^(NSError *error) {
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

}

/**
 * Set bounds
 * @params key
 */
-(void)setBounds:(CDVInvokedUrlCommand *)command
{
    NSString *key = [command.arguments objectAtIndex:0];
    GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
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
    [layer setBounds:bounds];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set opacity
 * @params key
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{


    NSString *key = [command.arguments objectAtIndex:0];
    GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
    CGFloat opacity = [[command.arguments objectAtIndex:1] floatValue];

    NSString *id = [NSString stringWithFormat:@"groundOverlay_icon_%lu", (unsigned long)layer.hash];
    UIImage *icon = [self.mapCtrl getUIImageByKey:id];

    layer.icon = [icon imageByApplyingAlpha:opacity];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set bearing
 * @params key
 */
-(void)setBearing:(CDVInvokedUrlCommand *)command
{
    NSString *key = [command.arguments objectAtIndex:0];
    GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
    layer.bearing = [[command.arguments objectAtIndex:1] floatValue];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
    NSString *key = [command.arguments objectAtIndex:0];
    GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
    NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];
    [layer setZIndex:(int)zIndex];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(NSError *error, UIImage *image))completionBlock
{

    NSURLRequest *req = [NSURLRequest requestWithURL:url
                                      cachePolicy:NSURLRequestReturnCacheDataElseLoad
                                      timeoutInterval:5];
    NSCachedURLResponse *cachedResponse = [[NSURLCache sharedURLCache] cachedResponseForRequest:req];
    if (cachedResponse != nil) {
      UIImage *image = [[UIImage alloc] initWithData:cachedResponse.data];
      completionBlock(nil, image);
      return;
    }
  
    [NSURLConnection sendAsynchronousRequest:req
          queue:[NSOperationQueue mainQueue]
          completionHandler:^(NSURLResponse *res, NSData *data, NSError *error) {
            if ( !YES ) {
              UIImage *image = [UIImage imageWithData:data];
              completionBlock(nil, image);
            } else {
              completionBlock(error, nil);
            }
        
    }];
}

@end
