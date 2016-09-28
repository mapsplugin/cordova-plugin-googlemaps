//
//  TileOverlay.h
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "CordovaGoogleMaps.h"

@interface LocalSyncTileLayer : GMSSyncTileLayer
@property (nonatomic) NSString *tileUrlFormat;
@property (nonatomic) NSString *webPageUrl;
@property (nonatomic) CGFloat tile_size;
- (id)initWithOptions:(NSDictionary *) options;
- (UIImage *)tileForX:(NSUInteger)x y:(NSUInteger)y zoom:(NSUInteger)zoom;
@end
