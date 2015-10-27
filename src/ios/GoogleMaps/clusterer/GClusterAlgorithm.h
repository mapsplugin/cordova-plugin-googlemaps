#import <Foundation/Foundation.h>
#import "GClusterItem.h"

@protocol GClusterAlgorithm <NSObject>

- (void)addItem:(id <GClusterItem>) item;
- (void)removeItems;
- (void)removeItemsNotInRectangle:(CGRect)rect;
- (void)setMaxDistanceAtZoom:(int)maxDistanceAtZoom;

- (NSSet*)getClusters:(float)zoom;

@end
