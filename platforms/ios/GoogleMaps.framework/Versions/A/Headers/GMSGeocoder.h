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

@class GMSReverseGeocodeResponse;
@class GMSReverseGeocodeResult;

/** GMSGeocoder error codes, embedded in NSError. */
typedef NS_ENUM(NSInteger, GMSGeocoderErrorCode) {
  kGMSGeocoderErrorInvalidCoordinate = 1,
  kGMSGeocoderErrorInternal,
};

/** Handler that reports a reverse geocoding response, or error. */
typedef void (^GMSReverseGeocodeCallback)(GMSReverseGeocodeResponse *, NSError *);

/**
 * Exposes a service for reverse geocoding.  This maps Earth coordinates (latitude and longitude) to
 * a collection of addresses near that coordinate.
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

/** A result from a reverse geocode request, containing a human-readable address. */
@interface GMSReverseGeocodeResult : NSObject<NSCopying>

/** Returns the first line of the address. */
- (NSString *)addressLine1;

/** Returns the second line of the address. */
- (NSString *)addressLine2;

@end

/** A collection of results from a reverse geocode request. */
@interface GMSReverseGeocodeResponse : NSObject<NSCopying>

/** Returns the first result, or nil if no results were available. */
- (GMSReverseGeocodeResult *)firstResult;

/**
 * Returns an array of all the results (contains GMSReverseGeocodeResult), including the first
 * result.
 */
- (NSArray *)results;

@end
