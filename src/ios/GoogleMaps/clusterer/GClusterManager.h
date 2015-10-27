#import <Foundation/Foundation.h>
#import <GoogleMaps/GoogleMaps.h>
#import "GClusterAlgorithm.h"
#import "GClusterRenderer.h"
#import "GQTPointQuadTreeItem.h"
#import "GoogleMapsViewController.h"

#import "MyPlgunProtocol.h"
#import "NonHierarchicalDistanceBasedAlgorithm.h"
#import "GDefaultClusterRenderer.h"
#import "LF_Marker.h"

@interface GClusterManager : CDVPlugin <MyPlgunProtocol, GMSMapViewDelegate>

@property(nonatomic, strong) GoogleMapsViewController* mapCtrl;

@property(nonatomic, weak) id<GMSMapViewDelegate> delegate;
@property(nonatomic, strong) id<GClusterAlgorithm> clusterAlgorithm;
@property(nonatomic, strong) id<GClusterRenderer> clusterRenderer;
@property(nonatomic, strong) NSMutableDictionary *items;


- (void)addItem:(id <GClusterItem>) item;
- (void)removeItems;
- (void)removeItemsNotInRectangle:(CGRect)rect;

- (void)cluster:(CDVInvokedUrlCommand*)command;
- (void)initClusterManager:(CDVInvokedUrlCommand*)command;
- (void)reloadMarkers:(CDVInvokedUrlCommand*)command;
- (void)updateCluster;
//convenience

/*+ (instancetype)managerWithMapView:(GMSMapView*)googleMap
                         algorithm:(id<GClusterAlgorithm>)algorithm
                          renderer:(id<GClusterRenderer>)renderer;

+ (instancetype)managerWithMapView:(GoogleMapsViewController*)mapCtrl
                         algorithm:(id<GClusterAlgorithm>)algorithm
                          renderer:(id<GClusterRenderer>)renderer;
*/
@end
