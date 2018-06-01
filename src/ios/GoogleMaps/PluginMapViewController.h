//
//  PluginMapViewController.h
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

@interface PluginMapViewController : PluginViewController<GMSMapViewDelegate, GMSIndoorDisplayDelegate>

@property (nonatomic) GMSMarker* activeMarker;
@property (nonatomic) GMSMapView* map;

- (BOOL)didTapMyLocationButtonForMapView:(GMSMapView *)mapView;
- (void)didChangeActiveBuilding: (GMSIndoorBuilding *)building;
- (void)didChangeActiveLevel: (GMSIndoorLevel *)level;
@end
