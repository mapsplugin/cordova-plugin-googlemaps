//
//  GMSPanoramaLayer.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>
#import <QuartzCore/QuartzCore.h>

/** kGMSLayerPanoramaHeadingKey ranges from [0, 360). */
extern NSString *const kGMSLayerPanoramaHeadingKey;

/** kGMSLayerPanoramaPitchKey ranges from [-90, 90]. */
extern NSString *const kGMSLayerPanoramaPitchKey;

/** kGMSLayerCameraZoomLevelKey ranges from [1, 5], default 1. */
extern NSString *const kGMSLayerPanoramaZoomKey;

/** kGMSLayerPanoramaFOVKey ranges from [1, 160] (in degrees), default 90. */
extern NSString *const kGMSLayerPanoramaFOVKey;

/**
 * GMSPanoramaLayer is a custom subclass of CALayer, provided as the layer
 * class on GMSPanoramaView. This layer should not be instantiated directly.
 */
@interface GMSPanoramaLayer : CALayer
@property(nonatomic, assign) CGFloat cameraHeading;
@property(nonatomic, assign) CGFloat cameraPitch;
@property(nonatomic, assign) CGFloat cameraZoom;
@property(nonatomic, assign) CGFloat cameraFOV;
@end
