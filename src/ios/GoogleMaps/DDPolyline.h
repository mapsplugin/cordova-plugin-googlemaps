//
//  Polyline.h
//  HelloWorld
//
//  Created by Nicolas Torres on 10/11/16.
//
//

#import <GoogleMaps/GoogleMaps.h>

@interface DDPolyline : GMSPolyline

#pragma mark - Map Events

- (void)polylineEdited;

- (void)movePolylineMarker:(GMSMarker *)markerMoved;

- (void)movePolylineMidMarker:(GMSMarker *)markerMoved;

- (void)startDraggingPolylineBorderMarker:(GMSMarker *)marker;

- (void)movePolylineToCenter:(CLLocationCoordinate2D)toCenter;

#pragma mark - Public Methods

- (id)initPolylineWithWithGMSPath:(GMSMutablePath *)path andMapView:(GMSMapView *)map;

- (void)updateZIndex:(int)newZIndex;

- (void)updateDDPolylineVisibilityInMap:(GMSMapView *)mapView;

- (void)removeFromMap;

- (void)updatePath:(GMSMutablePath *)newPath;

- (void)setPolylineEditable:(BOOL)editable;

@end
