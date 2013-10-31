//
//  GMSCameraPosition.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>


/**
 * An immutable class that aggregates all camera position parameters.
 */
@interface GMSCameraPosition : NSObject

/**
 * Designated initializer. Configures this GMSCameraPosition with all available
 * camera properties. Building a GMSCameraPosition via this initializer (or by
 * the following convenience constructors) will implicitly clamp camera values.
 *
 * @param target location on the earth which the camera points
 * @param zoom the zoom level near the center of the screen
 * @param bearing of the camera in degrees from true north
 * @param viewingAngle in degrees, of the camera angle from the nadir
 */
- (id)initWithTarget:(CLLocationCoordinate2D)target
                zoom:(CGFloat)zoom
             bearing:(CLLocationDirection)bearing
        viewingAngle:(double)viewingAngle;

/**
 * Convenience constructor for GMSCameraPosition for a particular target and
 * zoom level. This will set the bearing and viewingAngle properties of this
 * camera to zero defaults. i.e., directly facing the Earth's surface, with the
 * top of the screen pointing north.
 */
+ (GMSCameraPosition *)cameraWithTarget:(CLLocationCoordinate2D)target
                                   zoom:(CGFloat)zoom;

/**
 * Convenience constructor for GMSCameraPosition, as per cameraWithTarget:zoom:.
 */
+ (GMSCameraPosition *)cameraWithLatitude:(CLLocationDegrees)latitude
                                longitude:(CLLocationDegrees)longitude
                                     zoom:(CGFloat)zoom;

/**
 * Convenience constructor for GMSCameraPosition, with all camera properties as
 * per initWithTarget:zoom:bearing:viewingAngle:.
 */
+ (GMSCameraPosition *)cameraWithTarget:(CLLocationCoordinate2D)target
                                   zoom:(CGFloat)zoom
                                bearing:(CLLocationDirection)bearing
                           viewingAngle:(double)viewingAngle;

/**
 * Convenience constructor for GMSCameraPosition, with latitude/longitude and
 * all other camera properties as per initWithTarget:zoom:bearing:viewingAngle:.
 */
+ (GMSCameraPosition *)cameraWithLatitude:(CLLocationDegrees)latitude
                                longitude:(CLLocationDegrees)longitude
                                     zoom:(CGFloat)zoom
                                  bearing:(CLLocationDirection)bearing
                             viewingAngle:(double)viewingAngle;

/**
 * Get the zoom level at which |meters| distance, at given |coord| on Earth,
 * correspond to the specified number of screen |points|.
 *
 * For extremely large or small distances the returned zoom level may be
 * smaller or larger than the minimum or maximum zoom level
 * allowed on the camera.
 *
 * This helper method is useful for building camera positions that contain
 * specific physical areas on Earth.
 */
+ (CGFloat)zoomAtCoordinate:(CLLocationCoordinate2D)cooord
                  forMeters:(CGFloat)meters
                  perPoints:(CGFloat)points;

/** Location on the Earth towards which the camera points. */
@property(nonatomic, readonly, getter=targetAsCoordinate) CLLocationCoordinate2D target;

/**
 * Zoom level. Zoom uses an exponentional scale, where zoom 0 represents the
 * entire world as a 256 x 256 square. Each successive zoom level increases
 * magnification by a factor of 2. At zoom 10, the entire world is a 256k x
 * 256k square, and so on.
 */
@property(nonatomic, readonly) CGFloat zoom;

/**
 * Bearing of the camera, in degrees clockwise from true north.
 */
@property(nonatomic, readonly) CLLocationDirection bearing;

/**
 * The angle, in degrees, of the camera angle from the nadir (directly facing
 * the Earth). 0 is straight down, 90 is parallel to the ground. Note that the
 * maximum angle allowed is 45 degrees.
 */
@property(nonatomic, readonly) double viewingAngle;

@end

/**
 * The maximum zoom (closest to the Earth's surface) permitted by the map
 * camera.
 */
FOUNDATION_EXTERN const CGFloat kGMSMaxZoomLevel;

/**
 * The minimum zoom (farthest from the Earth's surface) permitted by the map
 * camera.
 */
FOUNDATION_EXTERN const CGFloat kGMSMinZoomLevel;
