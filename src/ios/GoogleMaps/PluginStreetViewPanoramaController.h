//
//  PluginStreetViewPanoramaController.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>
#import <math.h>
#import "PluginUtil.h"
#import "NSData+Base64.h"
#import "IPluginProtocol.h"
#import "PluginObjects.h"
#import "PluginViewController.h"
#import <GoogleMaps/GoogleMaps.h>

@interface PluginStreetViewPanoramaController : PluginViewController<GMSPanoramaViewDelegate>

@property (nonatomic) GMSPanoramaView* panoramaView;

//----------------------------------------------------
// In order to keep the compatibility with Android,
// some delegate methods are commented-out.
//----------------------------------------------------

//// Called when starting a move to another panorama.
//- (void)panoramaView:(GMSPanoramaView *)view willMoveToPanoramaID:(NSString *)panoramaID;

//This is invoked every time the view.panorama property changes.
- (void)panoramaView:(GMSPanoramaView *)view didMoveToPanorama:(nullable GMSPanorama *)panorama;

// Called when the panorama change was caused by invoking moveToPanoramaNearCoordinate:.
- (void)panoramaView:(GMSPanoramaView *)view didMoveToPanorama:(nonnull GMSPanorama *)panorama nearCoordinate:(CLLocationCoordinate2D)coordinate;

// Called when moveNearCoordinate: produces an error.
- (void)panoramaView:(GMSPanoramaView *)view error:(nonnull NSError *)error onMoveNearCoordinate:(CLLocationCoordinate2D)coordinate;

// Called when moveToPanoramaID: produces an error.
- (void)panoramaView:(GMSPanoramaView *)view error:(nonnull NSError *)error onMoveToPanoramaID:(nonnull NSString *)panoramaID;

// Called repeatedly during changes to the camera on GMSPanoramaView.
- (void)panoramaView:(GMSPanoramaView *)view didMoveCamera:(nonnull GMSPanoramaCamera *)camera;

// Called when a user has tapped on the GMSPanoramaView, but this tap was not consumed (taps may be consumed by e.g., tapping on a navigation arrow).
- (void)panoramaView:(GMSPanoramaView *)view didTap:(CGPoint)point;


//// Called after a marker has been tapped.
//- (void)panoramaView:(GMSPanoramaView *)view didTapMarker:(nonnull GMSMarker *)marker;
//
//// Called when the panorama tiles for the current view have just been requested and are beginning to load.
//- (void)panoramaViewDidStartRendering:(GMSPanoramaView *)panoramaView;
//
//// Called when the panorama tiles have been loaded (or permanently failed to load) and rendered on screen.
//- (void)panoramaViewDidFinishRendering:(GMSPanoramaView *)panoramaView;

@end
