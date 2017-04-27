//
//  Polyline.m
//  HelloWorld
//
//  Created by Nicolas Torres on 10/11/16.
//
//

#import "DDPolyline.h"
#import "GMUtils.h"
#import "PolyUtils.h"

NSString * const kGeoJSONPolylineKey = @"LineString";

@interface DDPolyline()

@property (nonatomic, strong) NSMutableArray *polylineMarkers;
@property (nonatomic, strong) NSMutableArray *midMarkers;
@property (nonatomic, strong) GMSMarker *centerMarker;
@property (nonatomic, assign) CLLocationCoordinate2D currentCenter;
@property (nonatomic, assign) BOOL editMode;
@property (nonatomic, assign) BOOL drawMode;
@property (nonatomic, strong) GMSMapView *mapView;

@end

@implementation DDPolyline


#pragma mark - Public Methods

- (id)initPolylineWithWithGMSPath:(GMSMutablePath *)path andMapView:(GMSMapView *)map{

    if (self = [super init])
    {
        self.mapView = map;
        self.editMode = NO;
        self.path = path;
        self.polylineMarkers = [[NSMutableArray alloc] init];
        self.midMarkers = [[NSMutableArray alloc] init];
        [self addBorderMarkersToPolyline];
        [self addCenterMarkerToPolyline];
        
        [self addMidMarkers];
    }
    return self;
    
};

- (void)updateZIndex:(int)newZIndex{

    [self setZIndex:newZIndex];
    
    for (int i = 0; i < self.polylineMarkers.count; i++) {
        
        GMSMarker *mark = [self.polylineMarkers objectAtIndex:i];
        mark.zIndex = newZIndex;
    }
    
    for (int i = 0; i < self.midMarkers.count; i++) {
        
        GMSMarker *mark = [self.midMarkers objectAtIndex:i];
        mark.zIndex = newZIndex;
    }
    
    self.centerMarker.zIndex = newZIndex;
    
};

- (void)updateDDPolylineVisibilityInMap:(GMSMapView *)mapView{

    self.map = mapView;
    
    for (int i = 0; i < self.polylineMarkers.count; i++) {
        
        GMSMarker *mark = [self.polylineMarkers objectAtIndex:i];
        mark.map = mapView;
    }
    
    for (int i = 0; i < self.midMarkers.count; i++) {
        
        GMSMarker *mark = [self.midMarkers objectAtIndex:i];
        mark.map = mapView;
    }
    
    self.centerMarker.map = mapView;
};

- (void)removeFromMap{
    
    for (int i = 0; i < self.polylineMarkers.count; i++) {
        
        GMSMarker *mark = [self.polylineMarkers objectAtIndex:i];
        mark.map = nil;
    }
    
    for (int i = 0; i < self.midMarkers.count; i++) {
        
        GMSMarker *mark = [self.midMarkers objectAtIndex:i];
        mark.map = nil;
    }
    
    self.centerMarker.map = nil;

};

- (void)updatePath:(GMSMutablePath *)newPath{

    self.path = newPath;
    
    [self reloadBorderMarkers];
    
    [self clearMidMarkers];
    
    [self addMidMarkers];
    
    // TODO: GMS review this commented line
    //[[PolygonManager sharedInstance] addPolygonToMapView:self];
    
    [self updateCenterCoordinates];
};

#pragma mark - Map Events

- (void)polylineEdited{
    
    [self clearMidMarkers];
    [self addMidMarkers];
}

#pragma mark - Mid Markers

- (void)startDraggingPolylineMidMarker:(GMSMarker *)marker{
    
    int currentIndex = (int)[self.midMarkers indexOfObject:marker];
    
    marker.opacity = 1;
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    
    [newPath insertCoordinate:marker.position atIndex:currentIndex + 1];
    
    self.path = newPath;
    
};

- (void)polylineMidMarkerDragging:(GMSMarker *)markerMoved {
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    
    int currentIndex = (int)[self.midMarkers indexOfObject:markerMoved];
    
    [newPath replaceCoordinateAtIndex:(currentIndex + 1) withCoordinate:markerMoved.position];
    
    self.path = newPath;
    
}

- (void)polylineMidMarkerWasDragged:(GMSMarker *)markerMoved{
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    int currentIndex = (int)[self.midMarkers indexOfObject:markerMoved];
    [self.midMarkers removeObject:markerMoved];
    
    markerMoved.map = nil;
    
    [newPath replaceCoordinateAtIndex:(currentIndex + 1) withCoordinate:markerMoved.position];
    
    self.path = newPath;
    
    [self reloadBorderMarkers];
    
    [self clearMidMarkers];
    [self addMidMarkers];
    
    [self updateCenterCoordinates];
    
};

#pragma mark - Border Markers

// Dragstart border marker
- (void)startDraggingPolylineBorderMarker:(GMSMarker *)marker{
    
    [self clearMidMarkers];
}

- (void)movePolylineBorderMarker:(GMSMarker *)markerMoved {
    
    [self updatePolylinePathWithBorderMarker:markerMoved];
    
    [self updateCenterCoordinates];
}

#pragma mark - Center Marker

- (void)startDraggingPolylineCenterMarker:(GMSMarker *)marker{
    
    [self clearMidMarkers];
}

// Dragging Center marker
- (void) movePolylineToCenter:(CLLocationCoordinate2D)toCenter {
    
    CLLocationCoordinate2D centerOffset, newPosition;
    centerOffset.latitude =  toCenter.latitude - self.currentCenter.latitude;
    centerOffset.longitude = toCenter.longitude - self.currentCenter.longitude;
    
    for (int i = 0; i < self.polylineMarkers.count; i++)
    {
        GMSMarker *mark = [self.polylineMarkers objectAtIndex:i];
        mark.map = nil;
        newPosition.latitude = mark.position.latitude + centerOffset.latitude;
        newPosition.longitude = mark.position.longitude + centerOffset.longitude;
        [mark setPosition:newPosition];
        
        [self updatePolylinePathWithBorderMarker:mark];
        
        mark.map = self.mapView;
    }
    
    [self reloadBorderMarkers];
    
    self.currentCenter = toCenter;
    self.centerMarker.position = self.currentCenter;
    
}

#pragma mark - Private Methods

- (void)setPolylineEditable:(BOOL)editable{
    
    // Enable or disable DRAG feature for all polylines marker (center manker and border markers)
    
    self.editMode = editable;
    
    [self.centerMarker setDraggable:editable];
    
    self.centerMarker.opacity = editable;
    
    for (int i = 0; i < self.polylineMarkers.count; i++)
    {
        GMSMarker *marker = [self.polylineMarkers objectAtIndex:i];
        [marker setDraggable:editable];
        marker.opacity = editable;
    }
    
    for (int i = 0; i < self.midMarkers.count; i++)
    {
        GMSMarker *marker = [self.midMarkers objectAtIndex:i];
        [marker setDraggable:editable];
        marker.opacity = 0.5 * editable;
    }
}

- (void)setPolylineDrawable:(BOOL)drawable
{
    
    if (self.editMode) {
        return;
    }
    
    self.drawMode = drawable;
    
    for (int i = 0; i < self.polylineMarkers.count; i++)
    {
        GMSMarker *marker = [self.polylineMarkers objectAtIndex:i];
        [marker setDraggable:NO];
        marker.opacity = drawable;
    }
}

- (void)updateCenterCoordinates{
    
    CLLocationCoordinate2D center = [GMUtils calculateCenterCoordinate:self.polylineMarkers];
    
    self.currentCenter = center;
    self.centerMarker.position = self.currentCenter;
}

- (void)clearMidMarkers{
    
    [self clearPolylineMarkers:NO];
}

- (void)clearBorderMarkers{
    
    [self clearPolylineMarkers:YES];
    
}

- (void)clearPolylineMarkers:(BOOL)isBorderMarker{
    
    NSMutableArray *array;
    
    if (isBorderMarker)
    {
        array = self.polylineMarkers;
    }
    else
    {
        array = self.midMarkers;
    }
    
    // Remove all markers from Map
    for (int i = 0; i < array.count; i++)
    {
        GMSMarker *marker = [array objectAtIndex:i];
        marker.map = nil;
    }
    
    // Clean the markers array
    [array removeAllObjects];
}

- (void)addMidMarkers{
    
    if (self.polylineMarkers && self.polylineMarkers.count)
    {
        for (int i = 0; i < self.polylineMarkers.count - 1; i++)
        {
            GMSMarker *markerA = (GMSMarker *)[self.polylineMarkers objectAtIndex:i];
            GMSMarker *markerB = (GMSMarker *)[self.polylineMarkers objectAtIndex:i+1];
            
            CLLocationCoordinate2D midCoordinate = [GMUtils getMidPointBetweenCoordinate:markerA.position andCoordinate:markerB.position];
            
            [[PolyUtils sharedInstance] createMidMarkerForOverlay:self withCoordinates:midCoordinate midMarkersAray:self.midMarkers andIsEditMode:self.editMode andMap:self.mapView];
            
        }
    }
    
}

- (void)addBorderMarkersToPolyline{
    GMSPath *path = self.path;
    
    for (int i = 0; i < path.count; i++)
    {
        CLLocationCoordinate2D coordinate = [path coordinateAtIndex:i];
        
        [[PolyUtils sharedInstance] createBorderMarkerForOverlay:self withCoordinates:coordinate borderMarkersAray:self.polylineMarkers andIsEditMode:self.editMode andMap:self.mapView];
    }
}

- (void)addCenterMarkerToPolyline{
    
    self.centerMarker = [[GMSMarker alloc] init];
    [self.centerMarker setTitle:@"Center Marker"];
    self.centerMarker.groundAnchor = CGPointMake(0.5, 0.5);
    self.centerMarker.icon = [UIImage imageNamed:@"move-icon"];
    [self.centerMarker setDraggable:NO];
    self.centerMarker.opacity = self.editMode;
    [self.centerMarker setTappable:NO];
    
    // TODO: this is a patch that we should change, we need a link between the border markers and the polyline
    self.centerMarker.userData = self;
    
    [self updateCenterCoordinates];
    
    self.centerMarker.map = self.mapView;
    
}

- (void)addBorderMarkersToMap{
    
    for (int i = 0; i < self.polylineMarkers.count; i++)
    {
        GMSMarker *borderMarker = (GMSMarker *)[self.polylineMarkers objectAtIndex:i];
        
        borderMarker.map = self.mapView;
    }
}

- (void)reloadBorderMarkers{
    
    [self clearBorderMarkers];
    [self addBorderMarkersToPolyline];
    [self addBorderMarkersToMap];
    
}

- (void)updatePolylineBorderMarker:(GMSMarker *)marker{
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    GMSMarker *markItem;
    
    for (int i = 0; i < self.polylineMarkers.count; i++)
    {
        markItem = (GMSMarker *)[self.polylineMarkers objectAtIndex:i];
        
        if ([markItem.title isEqualToString:marker.title])
        {
            [newPath replaceCoordinateAtIndex:i withCoordinate:marker.position];
        }
    }
    
    self.path = newPath;
}

- (void)updatePolylinePathWithBorderMarker:(GMSMarker *)marker{
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    GMSMarker *markItem;
    
    for (int i = 0; i < self.polylineMarkers.count; i++)
    {
        markItem = (GMSMarker *)[self.polylineMarkers objectAtIndex:i];
        
        if ([markItem.title isEqualToString:marker.title])
        {
            [newPath replaceCoordinateAtIndex:i withCoordinate:marker.position];
        }
    }
    
    self.path = newPath;
}

@end
