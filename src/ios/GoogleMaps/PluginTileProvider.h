//
//  PluginTileProvider.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"

@interface PluginTileProvider : GMSTileLayer
@property (nonatomic) NSString *tileUrlFormat;
@property (nonatomic) NSString *webPageUrl;
@property (nonatomic) UIView *webView;
@property (nonatomic) CGFloat tile_size;
@property (nonatomic) NSString *mapId;
@property (nonatomic) NSString *pluginId;
@property (nonatomic) NSString *_tileUrl;
@property (nonatomic, strong) NSCache* imgCache;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic) dispatch_semaphore_t semaphore;
- (id)initWithOptions:(NSDictionary *) options webView:(UIView *)webView;
- (void)requestTileForX:(NSUInteger)x   y:(NSUInteger)y    zoom:(NSUInteger)z    receiver:(id<GMSTileReceiver>)receiver;
- (void)onGetTileUrlFromJS:(NSString *)tileUrl;
- (void)downloadImageWithX:(NSUInteger)x y:(NSUInteger)y  zoom:(NSUInteger)zoom  url:(NSURL *)url receiver: (id<GMSTileReceiver>) receiver;
@end
