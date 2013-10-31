//
//  GMSPolyline.h
//  Google Maps SDK for iOS
//
//  Copyright 2012 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <GoogleMaps/GMSOverlay.h>

@class GMSPath;

/**
 * GMSPolyline specifies the available options for a polyline that exists on the
 * Earth's surface. It is drawn as a physical line between the points specified
 * in |path|.
 */
@interface GMSPolyline : GMSOverlay

/** The path that describes this polyline. */
@property(nonatomic, copy) GMSPath *path;

/** The width of the line in screen points. Defaults to 1. */
@property(nonatomic, assign) CGFloat strokeWidth;

/** The UIColor used to render the polyline. Defaults to blueColor. */
@property(nonatomic, strong) UIColor *strokeColor;

/** If this line should be rendered with geodesic correction. */
@property(nonatomic, assign) BOOL geodesic;

/**
 * Convenience constructor for GMSPolyline for a particular path.
 * Other properties will have default values.
 */
+ (instancetype)polylineWithPath:(GMSPath *)path;

@end
