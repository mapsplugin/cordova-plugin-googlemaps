//
//  GMSMapView+Overlays.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <GoogleMaps/GMSMapView.h>

/**
 * GMSMapView (Overlays) offers several methods to obtain specific overlay types
 * currently attached to the map.
 *
 * NOTE: In 1.2 of the Google Maps SDK for iOS, these methods are deprecated.
 * They may not appear in later releases of the SDK. Instead of using these
 * methods, we suggest that you maintain your own references to overlays that
 * you have added to a GMSMapView.
 */
@interface GMSMapView (Overlays)

/** Returns every GMSMarker attached to this GMSMapView. */
- (NSArray *)markers __GMS_AVAILABLE_BUT_DEPRECATED;

/** Returns every GMSGroundOverlay attached to this GMSMapView. */
- (NSArray *)groundOverlays __GMS_AVAILABLE_BUT_DEPRECATED;

/** Returns every GMSPolyline attached to this GMSMapView. */
- (NSArray *)polylines __GMS_AVAILABLE_BUT_DEPRECATED;

@end
