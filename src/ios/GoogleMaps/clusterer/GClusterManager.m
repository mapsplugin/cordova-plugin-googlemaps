#import "GClusterManager.h"
#import "GCluster.h"
@interface GClusterManager()
@property (nonatomic) BOOL isClustering;
@end

@implementation GClusterManager {
    GMSCameraPosition *previousCameraPosition;
}

-(void)setGoogleMapsViewController:(id)viewCtrl {
    self.mapCtrl = viewCtrl;
}

/*
- (void)setMapView:(GMSMapView*)mapView {
    previousCameraPosition = nil;
    _mapView = mapView;
}
*/

- (void)setClusterAlgorithm:(id <GClusterAlgorithm>)clusterAlgorithm {
    previousCameraPosition = nil;
    _clusterAlgorithm = clusterAlgorithm;
}

- (void)setClusterRenderer:(id <GClusterRenderer>)clusterRenderer {
    previousCameraPosition = nil;
    _clusterRenderer = clusterRenderer;
}

- (void)addItem:(id <GClusterItem>) item {
    [_clusterAlgorithm addItem:item];
//    [self updateCluster];                   //TEST_
}

- (void)removeItems {
  [_clusterAlgorithm removeItems];
}

- (void)removeItemsNotInRectangle:(CGRect)rect {
    [_clusterAlgorithm removeItemsNotInRectangle:rect];
}

- (void)initClusterManager:(CDVInvokedUrlCommand*)command {
    
    self.clusterAlgorithm = [[NonHierarchicalDistanceBasedAlgorithm alloc]init];
    self.clusterRenderer = [[GDefaultClusterRenderer alloc]initWithMapView:self.mapCtrl.map];
    self.clusterRenderer.overlayManager = self.mapCtrl.overlayManager;
    
    self.mapCtrl.map.delegate = self;
    self.delegate = self.mapCtrl;
    
    CDVPluginResult * pluginResult = nil;
    if (self.clusterAlgorithm == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Could not initialise clusterAlgorithm."];
        return;
    }
    else if (self.clusterRenderer == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Could not initialise clusterRenderer."];
        return;
    }
    else if (self.delegate == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No MapDelegate set."];
        return;
    }
    else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)reloadMarkers:(CDVInvokedUrlCommand*)command {
    CDVPluginResult * pluginResult = nil;
    [self removeItems];
    self.clusterRenderer.overlayManager = self.mapCtrl.overlayManager;
    int counter = 0;

    for (GMSMarker *item in self.mapCtrl.overlayManager) {
        
        if ([[self.mapCtrl.overlayManager objectForKey:item]class] == [GMSMarker class]) {
            
//            GMSMarker * marker = [[GMSMarker alloc]init];
            GMSMarker * marker = [self.mapCtrl.overlayManager objectForKey:item];
            
            LF_Marker *lfmarker = [[LF_Marker alloc]init];
            lfmarker.location = marker.position;
            lfmarker.marker = marker;
            [self addItem:lfmarker];
            counter++;
        }
    }
    if (counter <= 0) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT
                                         messageAsString:@"ClusterManager has no items to cluster."];
    }
    else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
        
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)cluster:(CDVInvokedUrlCommand*)command {
    
    CDVPluginResult * pluginResult = nil;
    self.isClustering = true;
    
    if (self.clusterAlgorithm && self.clusterRenderer) {
        [self.mapCtrl.map clear];
        [self updateCluster];
        [self.mapCtrl.view addSubview:_clusterRenderer.map];
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
        messageAsString:@"ClusterAlgorithm and ClusterRenderer are not instansiated.\nCreate Clustermanager first."];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)updateCluster {
//        [self.mapCtrl.map clear]; // TEST_
    NSSet *clusters = [_clusterAlgorithm getClusters:self.mapCtrl.map.camera.zoom];
    [_clusterRenderer clustersChanged:clusters];
}

- (CGRect)getVisibleRegion {
    
    CGPoint topRightPoint = CGPointMake(self.mapCtrl.map.frame.size.width, 0);
    CLLocationCoordinate2D topRightCoordinate = [self.mapCtrl.map.projection coordinateForPoint:topRightPoint];
    
    CGPoint bottomLeftPoint = CGPointMake(0, self.mapCtrl.map.frame.size.height);
    CLLocationCoordinate2D bottomLeftCoordinate = [self.mapCtrl.map.projection coordinateForPoint:bottomLeftPoint];
    NSLog(@"BL: \nlat: %f\nlng: %f",bottomLeftCoordinate.latitude, bottomLeftCoordinate.longitude);
    
    double width = topRightCoordinate.latitude - bottomLeftCoordinate.latitude;
    
    double height = topRightCoordinate.longitude - bottomLeftCoordinate.longitude;
    
    NSLog(@"W = %f   H = %f", width, height);

    return CGRectMake(bottomLeftCoordinate.latitude, bottomLeftCoordinate.longitude, width, height);
}

#pragma mark convenience
/*
+ (instancetype)managerWithMapView:(GMSMapView*)googleMap algorithm:(id<GClusterAlgorithm>)algorithm renderer:(id<GClusterRenderer>)renderer {
 GClusterManager *mgr = [[[self class] alloc] init];
 if(mgr) {
 mgr.mapView = googleMap;
 mgr.clusterAlgorithm = algorithm;
 mgr.clusterRenderer = renderer;
 }
 return mgr;
}
*/


#pragma mark mapview delegate

-(void)mapView:(GMSMapView *)mapView willMove:(BOOL)gesture {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:willMove:)]) {
        [self.delegate mapView:mapView willMove:gesture];
    }
//    NSLog(@"GClusterManager.m - mapView willMove");
}

- (void)mapView:(GMSMapView *)mapView didChangeCameraPosition:(GMSCameraPosition *)position {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didChangeCameraPosition:)]) {
        [self.delegate mapView:mapView didChangeCameraPosition:position];
    }
//    NSLog(@"GClusterManager.m - mapView didChangeCameraPosition");
}

- (void)mapView:(GMSMapView *)mapView idleAtCameraPosition:(GMSCameraPosition *)cameraPosition {
    assert(mapView == self.mapCtrl.map);
    
    // Don't re-compute clusters if the map has just been panned/tilted/rotated.
    GMSCameraPosition *position = [mapView camera];
    if (previousCameraPosition != nil && previousCameraPosition.zoom == position.zoom) {
        return;
    }
    previousCameraPosition = [mapView camera];
    
    if (self.isClustering) {
        [self updateCluster];
    }
    
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:idleAtCameraPosition:)]) {
        [self.delegate mapView:mapView idleAtCameraPosition:cameraPosition];
    }
}

-(void)mapView:(GMSMapView *)mapView didTapAtCoordinate:(CLLocationCoordinate2D)coordinate {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didTapAtCoordinate:)]) {
        [self.delegate mapView:mapView didTapAtCoordinate:coordinate];
    }
//    NSLog(@"GClusterManager.m - mapView didTapAtCoordinate");
}

-(void)mapView:(GMSMapView *)mapView didLongPressAtCoordinate:(CLLocationCoordinate2D)coordinate {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didLongPressAtCoordinate:)]) {
        [self.delegate mapView:mapView didLongPressAtCoordinate:coordinate];
    }
//    NSLog(@"GClusterManager.m - mapView didLongPressAtCoordinate");
}

- (BOOL)mapView:(GMSMapView *)mapView didTapMarker:(GMSMarker *)marker {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didTapMarker:)]) {
        return [self.delegate mapView:mapView didTapMarker:marker];
    }

    return true;
}

- (void)mapView:(GMSMapView *)mapView didTapInfoWindowOfMarker:(GMSMarker *)marker {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didTapInfoWindowOfMarker:)]) {
        [self.delegate mapView:mapView didTapInfoWindowOfMarker:marker];
        NSLog(@"GClusterManager.m - mapView didTapInfoWindowOfMarker");
    }
    
}

- (void)mapView:(GMSMapView *)mapView didTapOverlay:(GMSOverlay *)overlay {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didTapOverlay:)]) {
        [self.delegate mapView:mapView didTapOverlay:overlay];
    }
//    NSLog(@"GClusterManager.m - mapView didTapOverlay");
}

- (UIView *)mapView:(GMSMapView *)mapView markerInfoWindow:(GMSMarker *)marker {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:markerInfoWindow:)]) {

        NSLog(@"GClusterManager.m - mapView MarkerInfoWindow\n%@",marker.userData);
//        self.delegate
        return [self.delegate mapView:mapView markerInfoWindow:marker];
    }
    NSLog(@"GClusterManager.m - mapView MarkerInfoWindow - NIL");
    return nil;
}

- (UIView *)mapView:(GMSMapView *)mapView markerInfoContents:(GMSMarker *)marker {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:markerInfoContents:)]) {
        
        NSLog(@"GClusterManager.m - mapView MarkerInfoContent");
        
        return [self.delegate mapView:mapView markerInfoContents:marker];
    }
    
    return nil;
}

- (void)mapView:(GMSMapView *)mapView didBeginDraggingMarker:(GMSMarker *)marker {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didBeginDraggingMarker:)]) {
        [self.delegate mapView:mapView didBeginDraggingMarker:marker];
    }
}

-(void)mapView:(GMSMapView *)mapView didEndDraggingMarker:(GMSMarker *)marker {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didEndDraggingMarker:)]) {
        [self.delegate mapView:mapView didEndDraggingMarker:marker];
    }
}

- (void)mapView:(GMSMapView *)mapView didDragMarker:(GMSMarker *)marker {
    if ([self delegate] != nil
        && [self.delegate respondsToSelector:@selector(mapView:didDragMarker:)]) {
        [self.delegate mapView:mapView didDragMarker:marker];
    }
}

@end
