//
//  GMSCoordinateBounds+Geometry.h
//
//  Created by Marius Feldmann on 8/12/14.
//  Copyright (c) 2014 Marius Feldmann. All rights reserved.
//

#import <GoogleMaps/GoogleMaps.h>

@interface GMSCoordinateBounds (MFAdditions)

/** The North-West corner of these bounds. */
- (CLLocationCoordinate2D)southEast;

/** The South-East corner of these bounds. */
- (CLLocationCoordinate2D)northWest;

/** The center coordinate of these bounds. */
- (CLLocationCoordinate2D)center;

/** Return the path of the rect. */
- (GMSPath *)path;

/**
 * Returns an NSArray of GMSCoordinateBounds
 * Divides the current rectangular bounding box
 * into |numberOfRects| smaller boxes.
 */
- (NSArray *)divideIntoNumberOfBoxes:(NSInteger)numberOfBoxesl;

@end
