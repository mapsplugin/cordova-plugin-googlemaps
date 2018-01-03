//
//  PluginObjects
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//
#import "PluginObjects.h"


@implementation PluginObjects

static NSObject *_lock;

- (instancetype)init {
  self = [super init];
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    _lock = [NSObject alloc];
  });
  self.objects = [NSMutableDictionary dictionary];
  return self;
}

- (void)setObject:(id)objId forKey:(NSString*)key {
  @synchronized(_lock) {
    [self.objects setObject:objId forKey:key];
  }
}

- (id)objectForKey:(NSString*)key {
  @synchronized(_lock) {
    return [self.objects objectForKey:key];
  }
}
- (void)removeObjectForKey:(NSString*)key {
  @synchronized(_lock) {
    [self.objects removeObjectForKey:key];
  }
}

- (void)removeAllObjects {
  @synchronized(_lock) {
    [self.objects removeAllObjects];
  }
}

- (NSArray<NSString *>*)allKeys {
  @synchronized(_lock) {
    return [self.objects allKeys];
  }
}


@end
