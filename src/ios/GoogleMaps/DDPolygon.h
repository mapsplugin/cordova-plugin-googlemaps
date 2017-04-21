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

/**
 *  This method is called when a polygon's border marked was dragged
 *
 *  @param markerMoved The border marked that was moved
 *
 */
- (void)movePolygonBorderMarker:(GMSMarker *)markerMoved;

- (void)movePolygonMidMarker:(GMSMarker *)markerMoved;

- (void)startDraggingBorderMarker:(GMSMarker *)marker;

/**
 *  This method is called when a polygon's center marked was dragged
 *
 *  @param toCenter coordinates of the polygon's center marker
 *
 */
- (void)moveToCenter:(CLLocationCoordinate2D)toCenter;

- (void)polygonEdited;


#pragma mark - Public Methods

- (id)initPolygonWithWithGMSPath:(GMSMutablePath *)path andMapView:(GMSMapView *)map;

- (void)updateZIndex:(int)newZIndex;

- (void)updateDDPolygonVisibilityInMap:(GMSMapView *)mapView;

- (void)removeFromMap;

- (void)updatePath:(GMSMutablePath *)newPath;

- (void)setPolygonEditable:(BOOL)editable;

@end
