//
//  GMSPanorama.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>

/**
 * Metadata for a specific panorama.
 * You can't instantiate this class directly;
 * use GMSPanoramaService to obtain an instance.
 */
@interface GMSPanorama : NSObject

/** The precise location of this panorama. */
@property(nonatomic, readonly) CLLocationCoordinate2D coordinate;

/**
 * The ID of this panorama.
 * The panorama ID may change so please do not store this persistently.
 */
@property(nonatomic, readonly) NSString *panoramaID;

/** An array of |GMSPanoramaLink| containing the neighboring panoramas. */
@property(nonatomic, readonly) NSArray *links;

@end
