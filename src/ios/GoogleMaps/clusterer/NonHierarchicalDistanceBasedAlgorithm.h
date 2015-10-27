#import <Foundation/Foundation.h>
#import "GClusterAlgorithm.h"
#import "GQTPointQuadTree.h"

@interface NonHierarchicalDistanceBasedAlgorithm : NSObject<GClusterAlgorithm> 

@property (nonatomic, strong) NSMutableArray *items;

- (id)initWithMaxDistanceAtZoom:(NSInteger)maxDistanceAtZoom;

@end
