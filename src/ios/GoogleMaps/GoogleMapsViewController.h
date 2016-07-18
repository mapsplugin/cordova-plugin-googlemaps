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
#import "NSData+Base64.h"
#import <GoogleMaps/GoogleMaps.h>

@interface GoogleMapsViewController : UIViewController<GMSMapViewDelegate, GMSIndoorDisplayDelegate>

@property (nonatomic, strong) UIView* webView;
@property (nonatomic, strong) NSMutableDictionary* overlayManager;
@property (nonatomic, readwrite, strong) NSMutableDictionary* plugins;
@property (nonatomic) BOOL isFullScreen;
@property (nonatomic) CGRect screenSize;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) NSString *mapId;
@property (nonatomic, strong) GMSMapView* map;
@property (nonatomic) BOOL clickable;
@property (nonatomic) BOOL isRenderedAtOnce;


//- (UIView *)mapView:(GMSMapView *)mapView markerInfoWindow:(GMSMarker *)marker;
- (id)initWithOptions:(NSDictionary *) options;

- (GMSCircle *)getCircleByKey: (NSString *)key;
- (GMSMarker *)getMarkerByKey: (NSString *)key;
- (GMSPolygon *)getPolygonByKey: (NSString *)key;
- (GMSPolyline *)getPolylineByKey: (NSString *)key;
- (GMSTileLayer *)getTileLayerByKey: (NSString *)key;
- (GMSGroundOverlay *)getGroundOverlayByKey: (NSString *)key;
- (UIImage *)getUIImageByKey: (NSString *)key;

- (void)removeObjectForKey: (NSString *)key;
- (BOOL)didTapMyLocationButtonForMapView:(GMSMapView *)mapView;

- (void) didChangeActiveBuilding: (GMSIndoorBuilding *)building;
- (void) didChangeActiveLevel: (GMSIndoorLevel *)level;
@end