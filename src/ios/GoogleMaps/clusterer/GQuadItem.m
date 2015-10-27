#import "GQuadItem.h"
#import "SphericalMercatorProjection.h"

@implementation GQuadItem{
    id <GClusterItem> _item;
    GQTPoint _point;
    CLLocationCoordinate2D _position;
}

- (id)initWithItem:(id <GClusterItem>)clusterItem {
    if (self = [super init]) {
        SphericalMercatorProjection *projection = [[SphericalMercatorProjection alloc] initWithWorldWidth:1];

        _position = clusterItem.position;
        _point = [projection coordinateToPoint:_position];
        _item = clusterItem;
        _marker = clusterItem.marker;
    }
    return self;
}

- (GQTPoint)point {
    return _point;
}

- (id)copyWithZone:(NSZone *)zone {
    GQuadItem *newGQuadItem = [[self class] allocWithZone:zone];
    newGQuadItem->_point = _point;
    newGQuadItem->_item = _item;
    newGQuadItem->_position = _position;
    return newGQuadItem;
}

- (CLLocationCoordinate2D)position {
    return _position;
}

- (NSSet*)items {
    return [NSSet setWithObject:_item];
}

- (BOOL)isEqualToQuadItem:(GQuadItem *)other {
    return [_item isEqual:other->_item]
            && _point.x == other->_point.x
            && _point.y == other->_point.y;
}

#pragma mark - NSObject

- (BOOL)isEqual:(id)other {
    if (other == self)
        return YES;
    if (!other || ![[other class] isEqual:[self class]])
        return NO;

    return [self isEqualToQuadItem:other];
}

- (NSUInteger)hash {
    return [_item hash];
}

@end
