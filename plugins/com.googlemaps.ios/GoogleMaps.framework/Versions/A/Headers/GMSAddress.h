//
//  GMSAddress.h
//  Google Maps SDK for iOS
//
//  Copyright 2014 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>

#import <GoogleMaps/GMSMapView.h>

/**
 * A result from a reverse geocode request, containing a human-readable address. This class is
 * immutable and should be obtained via GMSGeocoder.
 *
 * Some of the fields may be nil, indicating they are not present.
 */
@interface GMSAddress : NSObject<NSCopying>

/** Location, or kLocationCoordinate2DInvalid if unknown. */
@property(nonatomic, readonly) CLLocationCoordinate2D coordinate;

/** Street number and name. */
@property(nonatomic, copy, readonly) NSString *thoroughfare;

/** Locality or city. */
@property(nonatomic, copy, readonly) NSString *locality;

/** Subdivision of locality, district or park. */
@property(nonatomic, copy, readonly) NSString *subLocality;

/** Region/State/Administrative area. */
@property(nonatomic, copy, readonly) NSString *administrativeArea;

/** Postal/Zip code. */
@property(nonatomic, copy, readonly) NSString *postalCode;

/** The country name. */
@property(nonatomic, copy, readonly) NSString *country;

/** An array of NSString containing formatted lines of the address. May be nil. */
@property(nonatomic, copy, readonly) NSArray *lines;

/** Returns the first line of the address. */
- (NSString *)addressLine1 __GMS_AVAILABLE_BUT_DEPRECATED;

/** Returns the second line of the address. */
- (NSString *)addressLine2 __GMS_AVAILABLE_BUT_DEPRECATED;

@end

/**
 * The former type of geocode results (pre-1.7). This remains here for migration and will be
 * removed in future releases.
 */
@compatibility_alias GMSReverseGeocodeResult GMSAddress;
