//
//  GMSPath.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>
#import <Foundation/Foundation.h>

/**
 * GMSPath encapsulates an immutable array of CLLocationCooordinate2D. All the coordinates of a
 * GMSPath must be valid. The mutable counterpart is GMSMutablePath.
 */
@interface GMSPath : NSObject <NSCopying, NSMutableCopying>

/** Convenience constructor for an empty path. */
+ (instancetype)path;

/** Initializes a newly allocated path with the contents of another GMSPath. */
- (id)initWithPath:(GMSPath *)path;

/** Get size of path. */
- (NSUInteger)count;

/** Returns kCLLocationCoordinate2DInvalid if |index| >= count. */
- (CLLocationCoordinate2D)coordinateAtIndex:(NSUInteger)index;

/**
 * Initializes a newly allocated path from |encodedPath|. This format is described at:
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
+ (instancetype)pathFromEncodedPath:(NSString *)encodedPath;

/** Returns an encoded string of the path in the format described above. */
- (NSString *)encodedPath;

/**
 * Returns a new path obtained by adding |deltaLatitude| and |deltaLongitude| to each coordinate
 * of the current path. Does not modify the current path.
 */
- (instancetype)pathOffsetByLatitude:(CLLocationDegrees)deltaLatitude
                           longitude:(CLLocationDegrees)deltaLongitude;

@end
