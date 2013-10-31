//
//  GMSReverseGeocodeOutput.h
//  Google Maps SDK for iOS
//
//  Copyright 2012 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//
// Models representing the results of reverse geocode operations.

#import <Foundation/Foundation.h>

/**
 * A result from a reverse geocode request, containing a human-readable address.
 */
@interface GMSReverseGeocodeResult : NSObject<NSCopying>

/** Returns the first line of the address. */
- (NSString *)addressLine1;

/** Returns the second line of the address. */
- (NSString *)addressLine2;

@end

/**
 * A collection of results from a reverse geocode request.  The query methods
 * <tt>firstResult</tt> and <tt>allResults</tt> construct their answers lazily,
 * and are not cached.  If you only need the first result, just use
 * <tt>firstResult</tt>.
 */
@interface GMSReverseGeocodeResponse : NSObject<NSCopying>

/** Returns the first result. */
- (GMSReverseGeocodeResult *)firstResult;

/** Returns an array of all the results (including the first result). */
- (NSArray *)results;

@end
