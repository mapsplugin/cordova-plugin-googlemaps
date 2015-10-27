//
// Created by Colin Edwards on 8/27/14.
//

#import <CoreLocation/CoreLocation.h>
#import "SphericalMercatorProjection.h"

@implementation SphericalMercatorProjection {
    double _worldWidth;
}

- (id)initWithWorldWidth:(double)worldWidth {
    if (self = [super init]) {
        _worldWidth = worldWidth;
    }
    return self;
}

- (id)init {
    return [self initWithWorldWidth:1];
}

- (CLLocationCoordinate2D)pointToCoordinate:(GQTPoint)point {
    double x = point.x / _worldWidth - 0.5;
    double lng = x * 360;

    double y = .5 - (point.y / _worldWidth);
    double lat = 90 - ((atan(exp(-y * 2 * M_PI)) * 2) * (180.0 / M_PI));

    return CLLocationCoordinate2DMake(lat, lng);
}

- (GQTPoint)coordinateToPoint:(CLLocationCoordinate2D)coordinate {
    double x = coordinate.longitude / 360 + .5;
    double siny = sin(coordinate.latitude / 180.0 * M_PI);
    double y = 0.5 * log((1 + siny) / (1 - siny)) / -(2 * M_PI) + .5;

    return (GQTPoint){x * _worldWidth, y * _worldWidth};
}

@end
