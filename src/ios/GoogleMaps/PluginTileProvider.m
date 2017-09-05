//
//  PluginTileProvider.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginTileProvider.h"

@implementation PluginTileProvider

CGRect tileRect;
CGRect debugRect;
UIFont *debugFont;
NSDictionary *debugAttributes;

- (id)initWithOptions:(NSDictionary *) options webView:(UIView *)webView{
    self = [super init];
    self.webView  = webView;
    //self.tileUrlFormat = [options objectForKey:@"tileUrlFormat"];
    self.webPageUrl = [options objectForKey:@"webPageUrl"];
    if ([options objectForKey:@"tileSize"]) {
        self.tileSize = [[options objectForKey:@"tileSize"] floatValue];
    } else {
        self.tileSize = 512.0f;
    }
    self.mapId = [options objectForKey:@"mapId"];
    self.pluginId = [options objectForKey:@"pluginId"];
    self.semaphore = dispatch_semaphore_create(0);
    self.tileUrlMap = [NSMutableDictionary dictionary];


    self.isDebug = NO;
    if ([[options objectForKey:@"debug"] boolValue]) {
      self.isDebug = YES;
      tileRect = CGRectMake(0, 0 , self.tileSize, self.tileSize);
      debugRect = CGRectMake(30, 30 , self.tileSize - 30, self.tileSize - 30);

      debugFont = [UIFont systemFontOfSize:30.0f];
      NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
      style.lineBreakMode = NSLineBreakByWordWrapping;

      debugAttributes = @{
          NSForegroundColorAttributeName : [UIColor redColor],
          NSFontAttributeName : debugFont,
          NSParagraphStyleAttributeName : style
      };
    }

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

- (void)onGetTileUrlFromJS:(NSString *)urlKey tileUrl:(NSString *)tileUrl {
    @synchronized (self.tileUrlMap) {
      [self.tileUrlMap setObject:tileUrl forKey:urlKey];
      dispatch_semaphore_signal(self.semaphore);
    }
}



- (void)requestTileForX:(NSUInteger)x   y:(NSUInteger)y    zoom:(NSUInteger)zoom    receiver:(id<GMSTileReceiver>)receiver {


  NSString *urlKey = [NSString stringWithFormat:@"%@-%@-%d-%d-%d",
                                self.mapId, self.pluginId, (int)x, (int)y, (int)zoom];

  @synchronized (self.semaphore) {

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{

          // Execute the getTile() callback
          NSString* jsString = [NSString
                                stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@-%@-tileoverlay', {key: \"%@\", x: %d, y: %d, zoom: %d});",
                                self.mapId, self.pluginId, urlKey, (int)x, (int)y, (int)zoom];

          [self execJS:jsString];


      }];
      dispatch_semaphore_wait(self.semaphore, dispatch_time(DISPATCH_TIME_NOW, 10 * 1000 * 1000 * 1000)); // Maximum wait 10sec

  }
  NSString *urlStr = nil;
  @synchronized (self.tileUrlMap) {
    urlStr = [self.tileUrlMap objectForKey:urlKey];
    [self.tileUrlMap removeObjectForKey:urlKey];
  }  NSString *originalUrlStr = urlStr;

  if (urlStr == nil || [urlStr isEqualToString:@"(null)"]) {
    //-------------------------
    // No image tile
    //-------------------------
     if (self.isDebug) {
       UIImage *image = [self drawDebugInfoWithImage:nil
                                           x:x
                                           y:y
                                           zoom:zoom
                                           url: originalUrlStr];
       [receiver receiveTileWithX:x y:y zoom:zoom image:image];
     } else {
       [receiver receiveTileWithX:x y:y zoom:zoom image:kGMSTileLayerNoTile];
     }
  }

  NSRange range = [urlStr rangeOfString:@"http"];
  if (range.location == 0) {
      //-------------------------
      // http:// or https://
      //-------------------------
      [self downloadImageWithX:x y:y zoom:zoom url:[NSURL URLWithString:urlStr] receiver:receiver];
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
             if (self.isDebug) {
               UIImage *image = [self drawDebugInfoWithImage:nil
                                                   x:x
                                                   y:y
                                                   zoom:zoom
                                                   url: originalUrlStr];
               [receiver receiveTileWithX:x y:y zoom:zoom image:image];
             } else {
               [receiver receiveTileWithX:x y:y zoom:zoom image:kGMSTileLayerNoTile];
             }
             return;
          }
      }


      NSData *data = [[NSFileManager defaultManager] contentsAtPath:urlStr];
      CGFloat screenScale = [[UIScreen mainScreen] scale];
      UIImage *image = [UIImage imageWithData:data scale:screenScale];
      if (image != nil &&
          (image.size.width != self.tileSize || image.size.height != self.tileSize)) {

          image = [image resize:self.tileSize height:self.tileSize];
      }

      if (image != nil) {
         if (self.isDebug) {
           image = [self drawDebugInfoWithImage:image
                                             x:x
                                             y:y
                                             zoom:zoom
                                             url: originalUrlStr];
           [receiver receiveTileWithX:x y:y zoom:zoom image:image];
         } else {
           [receiver receiveTileWithX:x y:y zoom:zoom image:image];
         }
      } else {
         if (self.isDebug) {
           UIImage *image = [self drawDebugInfoWithImage:nil
                                               x:x
                                               y:y
                                               zoom:zoom
                                               url: originalUrlStr];
           [receiver receiveTileWithX:x y:y zoom:zoom image:image];
         } else {
           [receiver receiveTileWithX:x y:y zoom:zoom image:kGMSTileLayerNoTile];
         }
      }
      return;

  }

}

- (UIImage*)drawDebugInfoWithImage:(UIImage*)image x:(NSInteger)x y:(NSUInteger)y  zoom:(NSUInteger)zoom url:(NSString *)url {

      CGSize rectSize = CGSizeMake(self.tileSize, self.tileSize);
      UIGraphicsBeginImageContextWithOptions(rectSize, NO, 0.0f);
      CGContextRef context = UIGraphicsGetCurrentContext();

      if (image != nil) {
        [image drawInRect:tileRect];
        //CGContextDrawImage(context, tileRect, image.CGImage);
      }

      CGContextSetAllowsAntialiasing(context, true);
      CGContextSetStrokeColorWithColor(context, [UIColor redColor].CGColor);
      CGContextSetLineWidth(context, 2);
      CGContextMoveToPoint(context, self.tileSize, 0);
      CGContextAddLineToPoint(context, 0, 0);
      CGContextAddLineToPoint(context, 0, self.tileSize);
      CGContextStrokePath(context);

      if (url) {
        url = [NSString stringWithFormat:@"\n%@", url];
      } else {
        url = @"";
      }

      NSString *debugInfo = [NSString stringWithFormat:@"x = %d, y = %d, zoom = %d%@", (int)x, (int)y, (int)zoom, url];

      [debugInfo drawInRect:debugRect withAttributes:debugAttributes];

      UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
      UIGraphicsEndImageContext();
      image = nil;
      return newImage;
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
      if (self.isDebug) {
        image = [self drawDebugInfoWithImage:image
                                           x:x
                                           y:y
                                           zoom:zoom
                                           url: url.absoluteString];
      }
      [receiver receiveTileWithX:x y:y zoom:zoom image:image];
      return;
    }

    NSString *uniqueKey = url.absoluteString;
    NSData *cache = [self.imgCache objectForKey:uniqueKey];
    if (cache != nil) {
      UIImage *image = [[UIImage alloc] initWithData:cache];
      if (self.isDebug) {
        image = [self drawDebugInfoWithImage:image
                                           x:x
                                           y:y
                                           zoom:zoom
                                           url: url.absoluteString];
      }
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
                                 if (self.isDebug) {
                                   image = [self drawDebugInfoWithImage:image
                                                                       x:x
                                                                       y:y
                                                                       zoom:zoom
                                                                       url: url.absoluteString];
                                 }
                                 [receiver receiveTileWithX:x y:y zoom:zoom image:image];
                               } else {
                                 if (self.isDebug) {
                                   UIImage *image = [self drawDebugInfoWithImage:nil
                                                                       x:x
                                                                       y:y
                                                                       zoom:zoom
                                                                       url: url.absoluteString];
                                   [receiver receiveTileWithX:x y:y zoom:zoom image:image];
                                 } else {
                                   [receiver receiveTileWithX:x y:y zoom:zoom image:kGMSTileLayerNoTile];
                                 }
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
