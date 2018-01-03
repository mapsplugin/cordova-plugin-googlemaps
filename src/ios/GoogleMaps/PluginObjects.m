//
//  PluginObjects
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//
#import "PluginObjects.h"


@implementation PluginObjects

- (instancetype)init {
  self = [super init];
  self.objects = [NSMutableDictionary dictionary];
  return self;
}

- (void)setObject:(id)objId forKey:(NSString*)key {
  @synchronized(self) {
    [self.objects setObject:objId forKey:key];
  }
}

- (UIImage*)objectForKey:(NSString*)key {
  @synchronized(self) {
    return [self.objects objectForKey:key];
  }
}
- (void)removeObjectForKey:(NSString*)key {
  @synchronized(self) {
    [self.objects removeObjectForKey:key];
  }
}

- (void)removeAllObjects {
  @synchronized(self) {
    [self.objects removeAllObjects];
  }
}

- (NSArray<NSString *>*)allKeys {
  @synchronized(self) {
    return [self.objects allKeys];
  }
}


@end
