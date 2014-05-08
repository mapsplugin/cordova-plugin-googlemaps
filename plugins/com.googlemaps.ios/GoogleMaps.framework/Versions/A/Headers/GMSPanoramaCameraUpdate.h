//
//  GMSPanoramaCameraUpdate.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

@interface GMSPanoramaCameraUpdate : NSObject

/** Returns an update that increments the camera heading with |deltaHeading|. */
+ (GMSPanoramaCameraUpdate *)rotateBy:(CGFloat)deltaHeading;

/** Returns an update that sets the camera heading to the given value. */
+ (GMSPanoramaCameraUpdate *)setHeading:(CGFloat)heading;

/** Returns an update that sets the camera pitch to the given value. */
+ (GMSPanoramaCameraUpdate *)setPitch:(CGFloat)pitch;

/** Returns an update that sets the camera zoom to the given value. */
+ (GMSPanoramaCameraUpdate *)setZoom:(CGFloat)zoom;

@end
