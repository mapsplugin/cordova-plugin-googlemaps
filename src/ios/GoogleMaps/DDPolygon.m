//
//  Polygon.m
//  HelloWorld
//
//  Created by Nicolas Torres on 10/11/16.
//
//

#import "DDPolygon.h"
#import "GMUtils.h"
#import "PolyUtils.h"

@interface DDPolygon()

@property (nonatomic, strong) NSMutableArray *polygonMarkers;
@property (nonatomic, strong) NSMutableArray *midMarkers;
@property (nonatomic, strong) GMSMarker *centerMarker;
@property (nonatomic, assign) CLLocationCoordinate2D currentCenter;
@property (nonatomic, assign) BOOL editMode;
@property (nonatomic, strong) GMSMapView *mapview;

@end

@implementation DDPolygon

#pragma mark - Public Methods

// TODO: review the map parameter
- (id)initPolygonWithWithGMSPath:(GMSMutablePath *)path andMapView:(GMSMapView *)map{
    
    if (self = [super init])
    {
        self.mapview = map;
        self.editMode = NO;
        self.path = path;
        self.polygonMarkers = [[NSMutableArray alloc] init];
        self.midMarkers = [[NSMutableArray alloc] init];
        [self addBorderMarkersToPolygon];
        [self addCenterMarkerToPolygon];
        
        [self addMidMarkers];
    }
    return self;
}

- (void)removeFromMap{
    
    
    for (int i = 0; i < self.polygonMarkers.count; i++) {
        
        GMSMarker *mark = [self.polygonMarkers objectAtIndex:i];
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

- (void)updateZIndex:(int)newZIndex{
    [self setZIndex:newZIndex];
    
    for (int i = 0; i < self.polygonMarkers.count; i++) {
        
        GMSMarker *mark = [self.polygonMarkers objectAtIndex:i];
        mark.zIndex = newZIndex;
    }
    
    for (int i = 0; i < self.midMarkers.count; i++) {
        
        GMSMarker *mark = [self.midMarkers objectAtIndex:i];
        mark.zIndex = newZIndex;
    }
    
    self.centerMarker.zIndex = newZIndex;
}

- (void)updateDDPolygonVisibilityInMap:(GMSMapView *)mapView{
    
    self.map = mapView;
    
    for (int i = 0; i < self.polygonMarkers.count; i++) {
        
        GMSMarker *mark = [self.polygonMarkers objectAtIndex:i];
        mark.map = mapView;
    }
    
    for (int i = 0; i < self.midMarkers.count; i++) {
        
        GMSMarker *mark = [self.midMarkers objectAtIndex:i];
        mark.map = mapView;
    }
    
    self.centerMarker.map = mapView;
};

#pragma mark - Map Events

- (void)polygonEdited{
    
    [self clearMidMarkers];
    [self addMidMarkers];
    
}

#pragma mark - Mid Markers

- (void)startDraggingPolygonMidMarker:(GMSMarker *)marker{
    
    int currentIndex = (int)[self.midMarkers indexOfObject:marker];
    
    marker.opacity = 1;
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    
    [newPath insertCoordinate:marker.position atIndex:currentIndex + 1];
    
    self.path = newPath;
    
};

- (void)polygonMidMarkerDragging:(GMSMarker *)markerMoved {
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    
    int currentIndex = (int)[self.midMarkers indexOfObject:markerMoved];
    
    [newPath replaceCoordinateAtIndex:(currentIndex + 1) withCoordinate:markerMoved.position];
    
    self.path = newPath;
    
}

- (void)polygonMidMarkerWasDragged:(GMSMarker *)markerMoved{
    
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
- (void)startDraggingBorderMarker:(GMSMarker *)marker{
    
    [self clearMidMarkers];
}

- (void)movePolygonBorderMarker:(GMSMarker *)markerMoved {
    
    [self updatePolygonPathWithBorderMarker:markerMoved];
    
    [self updateCenterCoordinates];
}

#pragma mark - Center Marker

- (void)startDraggingPolygonCenterMarker:(GMSMarker *)marker{
    
    [self clearMidMarkers];
}

// Dragging Center marker
- (void) moveToCenter:(CLLocationCoordinate2D)toCenter {
    
    CLLocationCoordinate2D centerOffset, newPosition;
    centerOffset.latitude =  toCenter.latitude - self.currentCenter.latitude;
    centerOffset.longitude = toCenter.longitude - self.currentCenter.longitude;
    
    for (int i = 0; i < self.polygonMarkers.count; i++)
    {
        GMSMarker *mark = [self.polygonMarkers objectAtIndex:i];
        mark.map = nil;
        newPosition.latitude = mark.position.latitude + centerOffset.latitude;
        newPosition.longitude = mark.position.longitude + centerOffset.longitude;
        [mark setPosition:newPosition];
        
        [self updatePolygonPathWithBorderMarker:mark];
        
        mark.map = self.mapview;
    }
    
    [self reloadBorderMarkers];

    self.currentCenter = toCenter;
    self.centerMarker.position = self.currentCenter;
    
}

#pragma mark - Private Methods

- (void)addMidMarkers{

    if (self.polygonMarkers && self.polygonMarkers.count)
    {
        for (int i = 0; i < self.polygonMarkers.count; i++)
        {
            GMSMarker *markerA = (GMSMarker *)[self.polygonMarkers objectAtIndex:i];
            GMSMarker *markerB;
            
            if (i == self.polygonMarkers.count - 1)
            {
                markerB = (GMSMarker *)[self.polygonMarkers objectAtIndex:0];
            }
            else
            {
                markerB = (GMSMarker *)[self.polygonMarkers objectAtIndex:i+1];
            }
            
            CLLocationCoordinate2D midCoordinate = [GMUtils getMidPointBetweenCoordinate:markerA.position andCoordinate:markerB.position];
            
            [[PolyUtils sharedInstance] createMidMarkerForOverlay:self withCoordinates:midCoordinate midMarkersAray:self.midMarkers andIsEditMode:self.editMode andMap:self.mapview];
            
            
        }
    }
  
}

- (void)createBorderMarkerWithCoordinates:(CLLocationCoordinate2D)coordinates{
    
    [[PolyUtils sharedInstance] createBorderMarkerForOverlay:self withCoordinates:coordinates borderMarkersAray:self.polygonMarkers andIsEditMode:self.editMode andMap:self.mapview];
    
}

- (void)addBorderMarkersToMap{
    
    for (int i = 0; i < self.polygonMarkers.count; i++)
    {
        GMSMarker *borderMarker = (GMSMarker *)[self.polygonMarkers objectAtIndex:i];
        
        borderMarker.map = self.mapview;
    }
}

- (void)setPolygonEditable:(BOOL)editable{

    // Enable or disable DRAG feature for all polygons marker (center manker and border markers)
    
    self.editMode = editable;
    
    [self.centerMarker setDraggable:editable];
    
    self.centerMarker.opacity = editable;
    
    for (int i = 0; i < self.polygonMarkers.count; i++)
    {
        GMSMarker *marker = [self.polygonMarkers objectAtIndex:i];
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

- (void)updateCenterCoordinates{
    
    CLLocationCoordinate2D center = [GMUtils calculateCenterCoordinate:self.polygonMarkers];
    
    self.currentCenter = center;
    self.centerMarker.position = self.currentCenter;
}

- (void)clearMidMarkers{
    
    [self clearPolygonMarkers:NO];
}

- (void)clearBorderMarkers{

    [self clearPolygonMarkers:YES];
    
}

- (void)clearPolygonMarkers:(BOOL)isBorderMarker{

    NSMutableArray *array;
    
    if (isBorderMarker)
    {
        array = self.polygonMarkers;
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

- (void)addBorderMarkersToPolygon{
    GMSPath *path = self.path;
    
    for (int i = 0; i < path.count; i++)
    {
        CLLocationCoordinate2D coordinate = [path coordinateAtIndex:i];
        
        [self createBorderMarkerWithCoordinates:coordinate];
    }
}

- (void)addCenterMarkerToPolygon{
    
    self.centerMarker = [[GMSMarker alloc] init];
    [self.centerMarker setTitle:@"Center Marker"];
    self.centerMarker.groundAnchor = CGPointMake(0.5, 0.5);
    self.centerMarker.icon = [UIImage imageNamed:@"move-icon"];
    [self.centerMarker setDraggable:NO];
    self.centerMarker.opacity = self.editMode;
    self.centerMarker.map = self.mapview;
    [self.centerMarker setTappable:NO];
    
    self.centerMarker.userData = self;
    
    [self updateCenterCoordinates];
}

- (void)reloadBorderMarkers{

    [self clearBorderMarkers];
    [self addBorderMarkersToPolygon];
    [self addBorderMarkersToMap];
    
}


- (void)updatePolygonPathWithBorderMarker:(GMSMarker *)marker{
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:self.path];
    GMSMarker *markItem;
    
    for (int i = 0; i < self.polygonMarkers.count; i++)
    {
        markItem = (GMSMarker *)[self.polygonMarkers objectAtIndex:i];
        
        if ([markItem.title isEqualToString:marker.title])
        {
            [newPath replaceCoordinateAtIndex:i withCoordinate:marker.position];
        }
    }
    
    self.path = newPath;
}

@end
