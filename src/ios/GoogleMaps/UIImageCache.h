//
//  UIImageCache
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//
#ifndef UIImageCache_h
#define UIImageCache_h

#import <Foundation/Foundation.h>

@interface UIImageCache : NSObject

+ (UIImageCache*)sharedInstance;
@property (nonatomic) NSMutableDictionary* iconCacheKeys;

// set
- (void)cacheImage:(UIImage*)image forKey:(NSString*)key;
// get
- (UIImage*)getCachedImageForKey:(NSString*)key;
// remove
- (void)removeCachedImageForKey:(NSString*)key;
// remove all
- (void)removeAllCachedImages;
@end


#endif
