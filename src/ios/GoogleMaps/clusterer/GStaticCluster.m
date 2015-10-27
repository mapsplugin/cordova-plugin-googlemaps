#import "GStaticCluster.h"
#import "SphericalMercatorProjection.h"
#import "GQTPoint.h"

@implementation GStaticCluster {
    CLLocationCoordinate2D _position;
    NSMutableSet *_items;
    SphericalMercatorProjection *_projection;
}

- (id)initWithCoordinate:(CLLocationCoordinate2D)coordinate andMarker:(GMSMarker *)marker{
    if (self = [super init]) {
        _position = coordinate;
        _items = [[NSMutableSet alloc] init];
        _marker = marker;
        _projection = [[SphericalMercatorProjection alloc] initWithWorldWidth:1];
    }
    return self;
}

- (void)add:(GQuadItem*)item {
    [_items addObject:item];
}

- (void)remove:(GQuadItem*)item {
    [_items removeObject:item];
}

- (NSSet*)items {
    return _items;
}

- (CLLocationCoordinate2D)position {
    return _position;
}

- (CLLocationCoordinate2D)averagePosition {
    return [self getAveragePosition_2];
}

- (CLLocationCoordinate2D)getAveragePosition_2 {
    
    double x = 0;
    double y = 0;
    
    for (GQuadItem *item in _items) {
        x += item.point.x;
        y += item.point.y;
    }
    
    x /= _items.count;
    y /= _items.count;

    return [_projection pointToCoordinate:(GQTPoint){x, y}];
}

- (CLLocationCoordinate2D)getAveragePosition {
    
    double x_max = 0;
    double x_min = 100;
    double y_max = 0;
    double y_min = 100;
    
    NSArray *result = [[_items allObjects] sortedArrayUsingComparator:^NSComparisonResult(GQuadItem *obj1, GQuadItem *obj2) {
        if (obj1.point.x > obj2.point.x && obj1.point.y > obj2.point.y) {
            return (NSComparisonResult)NSOrderedDescending;
        }
        if (obj1.point.x < obj2.point.x && obj1.point.y < obj2.point.y) {
            return (NSComparisonResult)NSOrderedAscending;
        }
        return (NSComparisonResult)NSOrderedSame;
    }];
    
    if (result.count % 2 != 0) {
        return ((GQuadItem*)[result objectAtIndex:result.count / 2 + 1]).position;
    }
    else {
        return ((GQuadItem*)[result objectAtIndex:result.count / 2]).position;
    }
    
    for (GQuadItem *item in _items) {
        NSLog(@"POSITIONS: %f", item.point.x);
        if (item.point.x > x_max) {
            x_max = item.point.x;
        }
        if (item.point.x < x_min) {
            x_min = item.point.x;
        }
        if (item.point.y > y_max) {
            y_max = item.point.y;
        }
        if (item.point.y < y_min) {
            y_min = item.point.y;
        }
    }
    
    double x = (x_max + x_min) / 2;
    double y = (y_max + y_min) / 2;
    
    return [_projection pointToCoordinate:(GQTPoint){x, y}];
}

@end
