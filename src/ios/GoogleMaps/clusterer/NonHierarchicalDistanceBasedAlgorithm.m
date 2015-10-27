#import <GoogleMaps/GoogleMaps.h>
#import "NonHierarchicalDistanceBasedAlgorithm.h"
#import "GQTBounds.h"
#import "GQTPoint.h"
#import "GStaticCluster.h"
#import "GQuadItem.h"

@implementation NonHierarchicalDistanceBasedAlgorithm {
    GQTPointQuadTree *_quadTree;
    NSInteger _maxDistanceAtZoom;
    BOOL _firstCall;
}

- (id)initWithMaxDistanceAtZoom:(NSInteger)aMaxDistanceAtZoom {
    if (self = [super init]) {
        _items = [[NSMutableArray alloc] init];
        _quadTree = [[GQTPointQuadTree alloc] initWithBounds:(GQTBounds){0,0,1,1}];
        _maxDistanceAtZoom = aMaxDistanceAtZoom;
        _firstCall = YES;
    }
    return self;
}

- (id)init {
    return [self initWithMaxDistanceAtZoom:150];
}

- (void)addItem:(id <GClusterItem>) item {
    if (_firstCall == YES) {
        [self removeItems];
        _firstCall = NO;
    }
    
    GQuadItem *quadItem = [[GQuadItem alloc] initWithItem:item];
    [_items addObject:quadItem];
    [_quadTree add:quadItem];
}

- (void)removeItems
{
  [_items removeAllObjects];
  [_quadTree clear];
}

- (void)removeItemsNotInRectangle:(CGRect)rect
{
    NSMutableArray *newItems = [[NSMutableArray alloc] init];
    [_quadTree clear];
    
    for (GQuadItem *item in _items) {

        if (CGRectContainsPoint(rect, CGPointMake(item.position.latitude, item.position.longitude)))
        {
            [newItems addObject:item];
            [_quadTree add:item];
        }
    }
    _items = newItems;
}

- (NSSet*)getClusters:(float)zoom {
    _firstCall = YES;
    int discreteZoom = (int) zoom;
    
    double zoomSpecificSpan = _maxDistanceAtZoom / pow(2, discreteZoom) / 256;
    
    NSMutableSet *visitedCandidates = [[NSMutableSet alloc] init];
    NSMutableSet *results = [[NSMutableSet alloc] init];
    NSMutableDictionary *distanceToCluster = [[NSMutableDictionary alloc] init];
    NSMutableDictionary *itemToCluster = [[NSMutableDictionary alloc] init];
    
    for (GQuadItem* candidate in _items) {
        if (candidate.hidden) continue;
        
        if ([visitedCandidates containsObject:candidate]) {
            // Candidate is already part of another cluster.
            continue;
        }
        
        GQTBounds bounds = [self createBoundsFromSpan:candidate.point span:zoomSpecificSpan];
        NSArray *clusterItems  = [_quadTree searchWithBounds:bounds];
        if ([clusterItems count] == 1) {
            // Only the current marker is in range. Just add the single item to the results.
            [results addObject:candidate];
            [visitedCandidates addObject:candidate];
            [distanceToCluster setObject:[NSNumber numberWithDouble:0] forKey:candidate];
            continue;
        }
        
        GStaticCluster *cluster = [[GStaticCluster alloc] initWithCoordinate:candidate.position andMarker:candidate.marker];
        [results addObject:cluster];
        
        for (GQuadItem* clusterItem in clusterItems) {
            if (clusterItem.hidden) continue;
            NSNumber *existingDistance = [distanceToCluster objectForKey:clusterItem];
            double distance = [self distanceSquared:clusterItem.point :candidate.point];
            if (existingDistance != nil) {
                // Item already belongs to another cluster. Check if it's closer to this cluster.
                if ([existingDistance doubleValue] < distance) {
                    continue;
                }
                
                // Move item to the closer cluster.
                GStaticCluster *oldCluster = [itemToCluster objectForKey:clusterItem];
                [oldCluster remove:clusterItem];
            }
            [distanceToCluster setObject:[NSNumber numberWithDouble:distance] forKey:clusterItem];
            [cluster add:clusterItem];
            [itemToCluster setObject:cluster forKey:clusterItem];
        }
        [visitedCandidates addObjectsFromArray:clusterItems];
    }
  
    return results;
}

- (void)setMaxDistanceAtZoom:(int)maxDistanceAtZoom {
    _maxDistanceAtZoom = maxDistanceAtZoom;
}

- (double)distanceSquared:(GQTPoint) a :(GQTPoint) b {
    return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
}

- (GQTBounds) createBoundsFromSpan:(GQTPoint) point span:(double) span {
    double halfSpan = span / 2;
    GQTBounds bounds;
    bounds.minX = point.x - halfSpan;
    bounds.maxX = point.x + halfSpan;
    bounds.minY = point.y - halfSpan;
    bounds.maxY = point.y + halfSpan;

    return bounds;
}

@end
