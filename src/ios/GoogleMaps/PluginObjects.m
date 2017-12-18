//
//  PluginObjects
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//
#import "PluginObjects.h"

static PluginObjects *sharedInstance;


@implementation PluginObjects

+ (PluginObjects*)sharedInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
      sharedInstance = [[PluginObjects alloc] init];
      sharedInstance.objects = [NSMutableDictionary dictionary];
    });
    return sharedInstance;
}
- (instancetype)init {
    self = [super init];
    return self;
}

- (void)setObject:(id)objId forKey:(NSString*)key {
    @synchronized(self) {
      [self.objects setObject:objId forKey:key];
    }
}

- (UIImage*)objectForKey:(NSString*)key {
    return [self.objects objectForKey:key];
}
- (void)removeObjectForKey:(NSString*)key {
    [self.objects removeObjectForKey:key];
}

- (void)removeAllObjects {
    [self.objects removeAllObjects];
}

- (NSArray<NSString *>*)allKeys {
    return [self.objects allKeys];
}


@end
