//
// Created by Colin Edwards on 8/27/14.
//

#import <Foundation/Foundation.h>
#import "GQTPoint.h"

@interface SphericalMercatorProjection : NSObject

- (id)initWithWorldWidth:(double)worldWidth;

- (CLLocationCoordinate2D)pointToCoordinate:(GQTPoint)point;

- (GQTPoint)coordinateToPoint:(CLLocationCoordinate2D)coordinate;

@end