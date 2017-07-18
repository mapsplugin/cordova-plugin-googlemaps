//
//  PluginTileProvider.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginTileProvider.h"

@implementation PluginTileProvider

- (id)initWithOptions:(NSDictionary *) options webView:(UIView *)webView{
    self = [super init];
    self.webView  = webView;
    //self.tileUrlFormat = [options objectForKey:@"tileUrlFormat"];
    self.webPageUrl = [options objectForKey:@"webPageUrl"];
    if ([options objectForKey:@"tileSize"]) {
        self.tile_size = [[options objectForKey:@"tileSize"] floatValue];
    } else {
        self.tile_size = 256.0f;
    }
    self.mapId = [options objectForKey:@"mapId"];
    self.pluginId = [options objectForKey:@"pluginId"];
    self.semaphore = dispatch_semaphore_create(0);


    self.imgCache = [[NSCache alloc]init];
    self.imgCache.totalCostLimit = 3 * 1024 * 1024 * 1024; // 3MB = Cache for image
    self.executeQueue =  [NSOperationQueue new];

    return self;
}

- (void)execJS: (NSString *)jsString {
  if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
      [self.webView performSelector:@selector(stringByEvaluatingJavaScriptFromString:) withObject:jsString];
  } else if ([self.webView respondsToSelector:@selector(evaluateJavaScript:completionHandler:)]) {
      [self.webView performSelector:@selector(evaluateJavaScript:completionHandler:) withObject:jsString withObject:nil];
  }
}

- (void)onGetTileUrlFromJS:(NSString *)tileUrl {
    self._tileUrl = tileUrl;
    dispatch_semaphore_signal(self.semaphore);
}


- (void)requestTileForX:(NSUInteger)x   y:(NSUInteger)y    zoom:(NSUInteger)zoom    receiver:(id<GMSTileReceiver>)receiver {

  NSUInteger orginalZoom = zoom;

  if (floor(self.map.camera.zoom) < zoom) {
    zoom = zoom - 1;  // Why does Google provides different zoom level?
  }

  @synchronized (self.semaphore) {

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{

          // Execute the getTile() callback
          NSString* jsString = [NSString
                                stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@-%@-tileoverlay', {x: %lu, y: %lu, zoom: %lu});",
                                self.mapId, self.pluginId, (unsigned long)x, (unsigned long)y, (unsigned long)zoom];

          [self execJS:jsString];


      }];
      dispatch_semaphore_wait(self.semaphore, DISPATCH_TIME_FOREVER);

  }
  NSString *urlStr = self._tileUrl;

  if ([urlStr isEqualToString:@"(null)"]) {
    //-------------------------
    // No image tile
    //-------------------------
    return [receiver receiveTileWithX:x y:y zoom:orginalZoom image:kGMSTileLayerNoTile];
  }

  NSRange range = [urlStr rangeOfString:@"http"];
  if (range.location == 0) {
      //-------------------------
      // http:// or https://
      //-------------------------
      [self downloadImageWithX:x y:y zoom:orginalZoom url:[NSURL URLWithString:urlStr] receiver:receiver];
      return;
  }

  range = [urlStr rangeOfString:@"://"];
  if (range.location == NSNotFound) {

      range = [urlStr rangeOfString:@"/"];
      if (range.location != 0) {
          //-------------------------------------------------------
          // Get the current URL, then calculate the relative path.
          //-------------------------------------------------------
          NSString *currentURL = [NSString stringWithString:self.webPageUrl];
          currentURL = [currentURL stringByDeletingLastPathComponent];
          currentURL = [currentURL stringByReplacingOccurrencesOfString:@"file:" withString:@""];
          currentURL = [currentURL stringByReplacingOccurrencesOfString:@"//" withString:@"/"];
          urlStr = [NSString stringWithFormat:@"file://%@/%@", currentURL, urlStr];
      } else {
          //-------------------------
          // Absolute file path
          //-------------------------
          urlStr = [NSString stringWithFormat:@"file://%@", urlStr];
      }

      range = [urlStr rangeOfString:@"file://"];
      if (range.location != NSNotFound) {
          //-------------------------
          // file path
          //-------------------------
          urlStr = [urlStr stringByReplacingOccurrencesOfString:@"file://" withString:@""];

          NSFileManager *fileManager = [NSFileManager defaultManager];
          if (![fileManager fileExistsAtPath:urlStr]) {
              [receiver receiveTileWithX:x y:y zoom:orginalZoom image:kGMSTileLayerNoTile];
              return;
          }
      }


      NSData *data = [[NSFileManager defaultManager] contentsAtPath:urlStr];
      CGFloat screenScale = [[UIScreen mainScreen] scale];
      UIImage *image = [UIImage imageWithData:data scale:screenScale];
      if (image != nil &&
          (image.size.width != self.tile_size || image.size.height != self.tile_size)) {

          image = [image resize:self.tile_size height:self.tile_size];
      }

      if (image != nil) {
          [receiver receiveTileWithX:x y:y zoom:orginalZoom image:image];
      } else {
          [receiver receiveTileWithX:x y:y zoom:orginalZoom image:kGMSTileLayerNoTile];
      }
      return;

  }

}


- (void)downloadImageWithX:(NSUInteger)x y:(NSUInteger)y  zoom:(NSUInteger)zoom  url:(NSURL *)url receiver: (id<GMSTileReceiver>) receiver
{
  [self.executeQueue addOperationWithBlock:^{

    NSURLRequest *req = [NSURLRequest requestWithURL:url
                                         cachePolicy:NSURLRequestReturnCacheDataElseLoad
                                     timeoutInterval:5];
    NSCachedURLResponse *cachedResponse = [[NSURLCache sharedURLCache] cachedResponseForRequest:req];
    if (cachedResponse != nil) {
      UIImage *image = [[UIImage alloc] initWithData:cachedResponse.data];
      [receiver receiveTileWithX:x y:y zoom:zoom image:image];
      return;
    }

    NSString *uniqueKey = url.absoluteString;
    NSData *cache = [self.imgCache objectForKey:uniqueKey];
    if (cache != nil) {
      UIImage *image = [[UIImage alloc] initWithData:cache];
      [receiver receiveTileWithX:x y:y zoom:zoom image:image];
      return;
    }

    //-------------------------------------------------------------
    // Use NSURLSessionDataTask instead of [NSURLConnection sendAsynchronousRequest]
    // https://stackoverflow.com/a/20871647
    //-------------------------------------------------------------
    NSURLSessionConfiguration *sessionConfiguration = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfiguration];
    NSURLSessionDataTask *getTask = [session dataTaskWithRequest:req
                             completionHandler:^(NSData *data, NSURLResponse *res, NSError *error) {
                               if ( !error ) {
                                 [self.imgCache setObject:data forKey:uniqueKey cost:data.length];
                                 UIImage *image = [UIImage imageWithData:data];
                                 [receiver receiveTileWithX:x y:y zoom:zoom image:image];
                               } else {
                                 [receiver receiveTileWithX:x y:y zoom:zoom image:kGMSTileLayerNoTile];
                               }

                            }];
    [getTask resume];
    //-------------------------------------------------------------
    // NSURLConnection sendAsynchronousRequest is deprecated.
    //-------------------------------------------------------------
/*
    [NSURLConnection sendAsynchronousRequest:req
                                       queue:self.executeQueue
                           completionHandler:^(NSURLResponse *res, NSData *data, NSError *error) {
                             if ( !error ) {
                               [self.imgCache setObject:data forKey:uniqueKey cost:data.length];
                               UIImage *image = [UIImage imageWithData:data];
                               [receiver receiveTileWithX:x y:y zoom:zoom image:image];
                             } else {
                               [receiver receiveTileWithX:x y:y zoom:zoom image:kGMSTileLayerNoTile];
                             }

                           }];
*/
  }];

}

@end
