//
//  TileOverlay.m
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "LocalSyncTileLayer.h"

@implementation LocalSyncTileLayer

- (id)initWithOptions:(NSDictionary *) options {
    self = [super init];
    self.tileUrlFormat = [options objectForKey:@"tileUrlFormat"];
    self.webPageUrl = [options objectForKey:@"webPageUrl"];
    if ([options objectForKey:@"tileSize"]) {
        self.tile_size = [[options objectForKey:@"tileSize"] floatValue];
    } else {
        self.tile_size = 256.0f;
    }
    return self;
}

- (UIImage *)tileForX:(NSUInteger)x y:(NSUInteger)y zoom:(NSUInteger)zoom {

  /**
   * Load the icon from local path
   */
  NSString *urlStr = [self.tileUrlFormat stringByReplacingOccurrencesOfString:@"<x>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)x]];
  urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<y>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)y]];
  urlStr = [urlStr stringByReplacingOccurrencesOfString:@"<zoom>" withString:[NSString stringWithFormat:@"%lu", (unsigned long)zoom]];

  NSRange range = [urlStr rangeOfString:@"://"];
  if (range.location == NSNotFound) {

      range = [urlStr rangeOfString:@"/"];
      if (range.location != 0) {
        // Get the current URL, then calculate the relative path.
        NSString *currentURL = [NSString stringWithString:self.webPageUrl];
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
          return kGMSTileLayerNoTile;
      }
  }
  
  
  //UIImage *image = [UIImage imageWithContentsOfFile:urlStr];
  NSData *data = [[NSFileManager defaultManager] contentsAtPath:urlStr];
  CGFloat screenScale = [[UIScreen mainScreen] scale];
  UIImage *image = [UIImage imageWithData:data scale:screenScale];
  if (image != nil &&
      (image.size.width != self.tile_size || image.size.height != self.tile_size)) {
    
      image = [image resize:self.tile_size height:self.tile_size];
  }

  if (image != nil) {
      return image;
  } else {
      return kGMSTileLayerNoTile;
  }
}
@end
