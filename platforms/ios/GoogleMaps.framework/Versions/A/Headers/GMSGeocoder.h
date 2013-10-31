//
//  GMSGeocoder.h
//  Google Maps SDK for iOS
//
//  Copyright 2012 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>

#import <GoogleMaps/GMSReverseGeocodeOutput.h>

typedef void (^GMSReverseGeocodeCallback)(GMSReverseGeocodeResponse *,
                                          NSError *);

/**
 * Exposes a service for reverse geocoding.  This maps Earth coordinates
 * (latitude and longitude) to a collection of addresses near that coordinate.
 */
@interface GMSGeocoder : NSObject

/* Convenience constructor for GMSGeocoder. */
+ (GMSGeocoder *)geocoder;

/**
 * Reverse geocodes a coordinate on the Earth's surface.
 *
 * @param coordinate The coordinate to reverse geocode.
 * @param handler The callback to invoke with the reverse geocode results.
 *        The callback will be invoked asynchronously from the main thread.
 */
- (void)reverseGeocodeCoordinate:(CLLocationCoordinate2D)coordinate
               completionHandler:(GMSReverseGeocodeCallback)handler;

@end
