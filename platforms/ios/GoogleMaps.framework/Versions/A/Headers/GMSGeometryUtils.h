//
//  GMSGeometryUtils.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>

#define OVERLOADABLE __attribute__((overloadable))

@class GMSPath;

/**
 * Returns whether |point| lies inside of path. The path is always cosidered
 * closed, regardless of whether the last point equals the first or not.
 * Inside is defined as not containing the South Pole -- the South Pole is
 * always outside.
 * |path| describes great circle segments if |geodesic| is YES, and rhumb
 * (loxodromic) segments otherwise.
 * If |point| is exactly equal to one of the vertices, the result is YES.
 * A point that is not equal to a vertex is on one side or the other of any path
 * segment -- it can never be "exactly on the border".
 * See GMSGeometryIsLocationOnPath() for a border test with tolerance.
 */
FOUNDATION_EXPORT
BOOL GMSGeometryContainsLocation(CLLocationCoordinate2D point, GMSPath *path,
                                 BOOL geodesic);

/**
 * Returns whether |point| lies on or near |path|, within the specified
 * |tolerance| in meters.
 * |path| is composed of great circle segments if |geodesic| is YES, and of
 * rhumb (loxodromic) segments if |geodesic| is NO.
 * See also GMSGeometryIsLocationOnPath(point, path, geodesic).
 *
 * The tolerance, in meters, is relative to the spherical radius of the Earth.
 * If you need to work on a sphere of different radius,
 * you may compute the equivalent tolerance from the desired tolerance on the
 * sphere of radius R: tolerance = toleranceR * (RadiusEarth / R),
 * with RadiusEarth==6371009.
 */
FOUNDATION_EXPORT OVERLOADABLE
BOOL GMSGeometryIsLocationOnPath(CLLocationCoordinate2D point, GMSPath *path,
                                 BOOL geodesic, CLLocationDistance tolerance);

/**
 * Same as GMSGeometryIsLocationOnPath(point, path, geodesic, tolerance),
 * with a default tolerance of 0.1 meters.
 */
FOUNDATION_EXPORT OVERLOADABLE
BOOL GMSGeometryIsLocationOnPath(CLLocationCoordinate2D point, GMSPath *path,
                                 BOOL geodesic);

/**
 * Returns the great circle distance between two coordinates, in meters,
 * on Earth.
 * This is the shortest distance between the two coordinates on the sphere.
 * Both coordinates must be valid.
 */
FOUNDATION_EXPORT OVERLOADABLE
CLLocationDistance GMSGeometryDistance(CLLocationCoordinate2D from,
                                       CLLocationCoordinate2D to);

/**
 * Returns the great circle length of |path|, in meters, on Earth.
 * This is the sum of GMSGeometryDistance() over the path segments.
 * All the coordinates of the path must be valid.
 */
FOUNDATION_EXPORT OVERLOADABLE
CLLocationDistance GMSGeometryLength(GMSPath *path);

/**
 * Returns the area of a geodesic polygon defined by |path| on Earth.
 * The "inside" of the polygon is defined as not containing the South pole.
 * If |path| is not closed, it is implicitly treated as a closed path
 * nevertheless and the result is the same.
 * All coordinates of the path must be valid.
 * If any segment of the path is a pair of antipodal points, the
 * result is undefined -- because two antipodal points do not form a
 * unique great circle segment on the sphere.
 * The polygon must be simple (not self-overlapping) and may be concave.
 */
FOUNDATION_EXPORT OVERLOADABLE
double GMSGeometryArea(GMSPath *path);

/**
 * Returns the signed area of a geodesic polygon defined by |path| on Earth.
 * The result has the same absolute value as GMSGeometryArea(); it is positive
 * if the points of path are in counter-clockwise order, and negative otherwise.
 * The same restrictions as on GMSGeometryArea() apply.
 */
FOUNDATION_EXPORT OVERLOADABLE
double GMSGeometrySignedArea(GMSPath *path);

/**
 * Returns the initial heading (degrees clockwise of North) at |from|
 * of the shortest path to |to|.
 * Returns 0 if the two coordinates are the same.
 * Both coordinates must be valid.
 * The returned value is in the range [0, 360).
 *
 * To get the final heading at |to| one may use
 * (GMSGeometryHeading(|to|, |from|) + 180) modulo 360.
 */
FOUNDATION_EXPORT
CLLocationDirection GMSGeometryHeading(CLLocationCoordinate2D from,
                                       CLLocationCoordinate2D to);

/**
 * Returns the destination coordinate, when starting at |from|
 * with initial |heading|, travelling |distance| meters along a great circle
 * arc, on Earth.
 * The resulting longitude is in the range [-180, 180).
 * Both coordinates must be valid.
 */
FOUNDATION_EXPORT OVERLOADABLE
CLLocationCoordinate2D GMSGeometryOffset(CLLocationCoordinate2D from,
                                         CLLocationDistance distance,
                                         CLLocationDirection heading);

/**
 * Returns the coordinate that lies the given |fraction| of the way between
 * the |from| and |to| coordinates on the shortest path between the two.
 * The resulting longitude is in the range [-180, 180).
 */
FOUNDATION_EXPORT
CLLocationCoordinate2D GMSGeometryInterpolate(CLLocationCoordinate2D from,
                                              CLLocationCoordinate2D to,
                                              double fraction);
