//
//  GMSMapLayer.h
//  Google Maps SDK for iOS
//
//  Copyright 2013 Google Inc.
//
//  Usage of this SDK is subject to the Google Maps/Google Earth APIs Terms of
//  Service: https://developers.google.com/maps/terms
//

#import <CoreLocation/CoreLocation.h>
#import <QuartzCore/QuartzCore.h>

/**
 * The following layer properties and constants describe the camera properties
 * that may be animated on the custom model layer of a GMSMapView with Core
 * Animation. For simple camera control and animation, please see the helper
 * methods in GMSMapView+Animation.h, and the camera object definition within
 * GMSCameraPosition.h.
 *
 * Each camera property may be modified via its @property, setValue:forKey: or
 * via addAnimation:forKey:, e.g.:
 *   mapView_.layer.cameraBearing = 20;
 * or:
 *   [mapView_.layer setValue:@(20) forKey:kGMSLayerCameraBearingKey];
 * or:
 *   CAMediaTimingFunction *curve = [CAMediaTimingFunction functionWithName:
 *                                   kCAMediaTimingFunctionEaseInEaseOut];
 *   CABasicAnimation *animation =
 *       [CABasicAnimation animationWithKeyPath:kGMSLayerCameraBearingKey];
 *   animation.duration = 2.0f;
 *   animation.timingFunction = curve;
 *   animation.toValue = @(20);
 *   [mapView_.layer addAnimation:animation forKey:kGMSLayerCameraBearingKey];
 *
 * To perform several animations together, Core Animation's transaction support
 * may be used, e.g.:
 *   [CATransaction begin];
 *   [CATransaction setAnimationDuration:2.0f];
 *   mapView_.layer.cameraBearing = 20;
 *   mapView_.layer.cameraViewingAngle = 30;
 *   [CATransaction commit];
 *
 * Note that these properties are not view-based. Please see "Animating View
 * and Layer Changes Together" in the View Programming Guide for iOS-
 *   http://developer.apple.com/library/ios/#documentation/windowsviews/conceptual/viewpg_iphoneos/AnimatingViews/AnimatingViews.html
 */

/**
 * kGMSLayerCameraLatitudeKey ranges from [-85, 85], and values outside this
 * range will be clamped.
 */
extern NSString *const kGMSLayerCameraLatitudeKey;

/**
 * kGMSLayerCameraLongitudeKey ranges from [-180, 180), and values outside this
 * range will be wrapped to within this range.
 */
extern NSString *const kGMSLayerCameraLongitudeKey;

/**
 * kGMSLayerCameraBearingKey ranges from [0, 360), and values are wrapped.
 */
extern NSString *const kGMSLayerCameraBearingKey;

/**
 * kGMSLayerCameraZoomLevelKey ranges from [kGMSMinZoomLevel, kGMSMaxZoomLevel],
 * and values are clamped.
 */
extern NSString *const kGMSLayerCameraZoomLevelKey;

/**
 * kGMSLayerCameraViewingAngleKey ranges from zero (i.e., facing straight down)
 * and to between 30 and 45 degrees towards the horizon, depending on the model
 * zoom level.
 */
extern NSString *const kGMSLayerCameraViewingAngleKey;

/**
 * GMSMapLayer is a custom subclass of CALayer, provided as the layer class on
 * GMSMapView. This layer should not be instantiated directly. It provides
 * model access to the camera normally defined on GMSMapView.
 *
 * Modifying or animating these properties will typically interrupt any current
 * gesture on GMSMapView, e.g., a user's pan or rotation. Similarly, if a user
 * performs an enabled gesture during an animation, the animation will stop
 * 'in-place' (at the current presentation value).
 */
@interface GMSMapLayer : CALayer

/** Camera latitude, as per kGMSLayerCameraLatitudeKey. */
@property(nonatomic, assign) CLLocationDegrees cameraLatitude;

/** Camera longitude, as per kGMSLayerCameraLongitudeKey. */
@property(nonatomic, assign) CLLocationDegrees cameraLongitude;

/** Camera bearing, as per kGMSLayerCameraBearingKey. */
@property(nonatomic, assign) CLLocationDirection cameraBearing;

/** Camera zoom, as per kGMSLayerCameraZoomLevelKey. */
@property(nonatomic, assign) CGFloat cameraZoomLevel;

/** Camera viewing angle, as per kGMSLayerCameraViewingAngleKey. */
@property(nonatomic, assign) double cameraViewingAngle;

@end
