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

#pragma mark - Mid Markers

- (void)polylineMidMarkerDragging:(GMSMarker *)markerMoved;

- (void)polylineMidMarkerWasDragged:(GMSMarker *)markerMoved;

- (void)startDraggingPolylineMidMarker:(GMSMarker *)marker;

#pragma mark - Border Markers

- (void)movePolylineBorderMarker:(GMSMarker *)markerMoved;

- (void)startDraggingPolylineBorderMarker:(GMSMarker *)marker;

#pragma mark - Center Marker

- (void)startDraggingPolylineCenterMarker:(GMSMarker *)marker;

- (void)movePolylineToCenter:(CLLocationCoordinate2D)toCenter;

#pragma mark - Public Methods

- (id)initPolylineWithWithGMSPath:(GMSMutablePath *)path andMapView:(GMSMapView *)map;

- (void)updateZIndex:(int)newZIndex;

- (void)updateDDPolylineVisibilityInMap:(GMSMapView *)mapView;

- (void)removeFromMap;

- (void)updatePath:(GMSMutablePath *)newPath;

- (void)setPolylineEditable:(BOOL)editable;

- (void)setPolylineDrawable:(BOOL)drawable;

@end
