//
//  GoogleMapsViewController.h
//  SimpleMap
//
//  Created by masashi on 11/6/13.
//
//

#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>
#import "PluginUtil.h"
#import "MyPluginLayer.h"
#import "NSData+Base64.h"
#import "MyPluginLayer.h"

@interface GoogleMapsViewController : UIViewController<GMSMapViewDelegate, GMSIndoorDisplayDelegate>

@property (nonatomic, strong) UIView* webView;
@property (nonatomic, strong) NSMutableDictionary* overlayManager;
@property (nonatomic, readwrite, strong) NSMutableDictionary* plugins;
@property (nonatomic) BOOL isFullScreen;
@property (nonatomic) CGRect screenSize;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) MyPluginLayer *pluginLayer;


//- (UIView *)mapView:(GMSMapView *)mapView markerInfoWindow:(GMSMarker *)marker;
- (id)initWithOptions:(NSDictionary *) options;

- (GMSCircle *)getCircleByKey: (NSString *)key;
- (GMSMarker *)getMarkerByKey: (NSString *)key;
- (GMSPolygon *)getPolygonByKey: (NSString *)key;
- (GMSPolyline *)getPolylineByKey: (NSString *)key;
- (GMSTileLayer *)getTileLayerByKey: (NSString *)key;
- (GMSGroundOverlay *)getGroundOverlayByKey: (NSString *)key;
- (UIImage *)getUIImageByKey: (NSString *)key;
- (void)updateMapViewLayout;

- (void)removeObjectForKey: (NSString *)key;
- (BOOL)didTapMyLocationButtonForMapView:(GMSMapView *)mapView;
- (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator;

- (void) didChangeActiveBuilding: (GMSIndoorBuilding *)building;
- (void) didChangeActiveLevel: (GMSIndoorLevel *)level;
@end