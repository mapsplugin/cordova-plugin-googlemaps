//
//  Polygon.h
//  HelloWorld
//
//  Created by Nicolas Torres on 10/11/16.
//
//

#import <GoogleMaps/GoogleMaps.h>

@interface DDPolygon : GMSPolygon

#pragma mark - Map Events

- (void)polygonEdited;

#pragma mark - Mid Markers

- (void)polygonMidMarkerDragging:(GMSMarker *)markerMoved;

- (void)polygonMidMarkerWasDragged:(GMSMarker *)markerMoved;

- (void)startDraggingPolygonMidMarker:(GMSMarker *)marker;

#pragma mark - Border Markers

- (void)movePolygonBorderMarker:(GMSMarker *)markerMoved;

- (void)startDraggingBorderMarker:(GMSMarker *)marker;

#pragma mark - Center Marker

- (void)startDraggingPolygonCenterMarker:(GMSMarker *)marker;

- (void)moveToCenter:(CLLocationCoordinate2D)toCenter;



#pragma mark - Public Methods

- (id)initPolygonWithWithGMSPath:(GMSMutablePath *)path andMapView:(GMSMapView *)map;

- (void)updateZIndex:(int)newZIndex;

- (void)updateDDPolygonVisibilityInMap:(GMSMapView *)mapView;

- (void)removeFromMap;

- (void)updatePath:(GMSMutablePath *)newPath;

- (void)setPolygonEditable:(BOOL)editable;

@end
