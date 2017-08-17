//
//  UIImageCache
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//
#import "UIImageCache.h"

static UIImageCache *sharedInstance;

@interface UIImageCache ()
@property (nonatomic, strong) NSCache *imageCache;
@end

@implementation UIImageCache

+ (UIImageCache*)sharedInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[UIImageCache alloc] init];
    });
    return sharedInstance;
}
- (instancetype)init {
    self = [super init];
    if (self) {
        self.imageCache = [[NSCache alloc] init];
        self.imageCache.totalCostLimit = 3 * 1024 * 1024 * 1024; // 3MB = Cache for image
        self.iconCacheKeys = [NSMutableDictionary dictionary];
    }
    return self;
}

- (void)cacheImage:(UIImage*)image forKey:(NSString*)key {
    NSData *imgData = UIImagePNGRepresentation(image);
    [self.imageCache setObject:image forKey:key cost:[imgData length]];
    imgData = nil;
}

- (UIImage*)getCachedImageForKey:(NSString*)key {
    return [self.imageCache objectForKey:key];
}
- (void)removeCachedImageForKey:(NSString*)key {
    [self.imageCache removeObjectForKey:key];
}

- (void)removeAllCachedImages {
    [self.imageCache removeAllObjects];
}


@end
