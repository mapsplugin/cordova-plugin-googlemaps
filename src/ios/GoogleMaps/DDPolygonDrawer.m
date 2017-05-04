//
//  DDPolygonDrawer.m
//  DroneDeployBeta
//
//  Created by Pablo Yaniero on 25/4/17.
//
//

#import "DDPolygonDrawer.h"
#import "DDPolygon.h"
#import "DDPolyline.h"
#import "GMUtils.h"

#define InvalidStrokeColor [UIColor redColor]
#define ValidStrokeColor [UIColor blueColor]
#define InvalidFillColor [UIColor colorWithRed:1 green:0 blue:0 alpha:0.3]
#define ValidFillColor [UIColor colorWithRed:0 green:0 blue:1 alpha:0.3]

@interface DDPolygonDrawer ()

@property (nonatomic, strong) GMSMutablePath *currentPath;
@property (nonatomic, strong) GMSMapView *mapView;
@property (nonatomic, strong) DDPolyline *templatePolyline;
@property (nonatomic, strong) GMSPolygon *templatePolygon;
@property (nonatomic, strong) GMSPolyline *invalidSegmentPolyline;

@property (nonatomic, strong) CLLocation *invalidPosition;
@end

@implementation DDPolygonDrawer

- (instancetype)initWithMapView:(GMSMapView *)mapView
{
    self = [super init];
    if (self) {
        self.currentPath = [GMSMutablePath path];
        self.mapView = mapView;
    }
    return self;
}

- (void)pushCoordinate:(CLLocationCoordinate2D)coordinate
{
    BOOL intersects = [GMUtils segmentToPoint:coordinate intersectsWithPath:self.currentPath];
    
    if (intersects) {
        self.invalidPosition = [[CLLocation alloc] initWithLatitude:coordinate.latitude longitude:coordinate.longitude];
    } else {
        self.invalidPosition = nil;
        [self.currentPath addCoordinate:coordinate];
    }
    
}

- (void)draw
{
    [self drawBorder];
    
    [self drawFill];
    
    if (self.invalidPosition) {
        [self drawInvalidSegment];
    } else {
        [self deleteInvalidSegment];
        
        BOOL intersects = [GMUtils checkNonConvexHullPath:self.currentPath];
        
        if (intersects) {
            [self drawInvalidPolygon];
        }
    }
}

- (DDPolygon *)print
{
    [self deleteInvalidSegment];
    
    BOOL intersects = [GMUtils checkNonConvexHullPath:self.currentPath];
    
    if (intersects) {
        [self drawInvalidPolygon];
        
        return nil;
    } else {
        [self removeTemplates];
        
        DDPolygon *polygon = [[DDPolygon alloc] initPolygonWithWithGMSPath:self.currentPath andMapView:self.mapView];
        polygon.map = self.mapView;
        [polygon setPolygonEditable:YES];
        
        return polygon;
    }
}

- (void)deleteLastDrawnVertex{
    
    [self.currentPath removeLastCoordinate];
    
    [self draw];
};

- (void)cancelPolygonDrawing{

    if (self.templatePolyline)
    {
        [self.templatePolyline removeFromMap];
        self.templatePolyline.map = nil;
        self.templatePolyline = nil;
    }
    
    if (self.templatePolygon)
    {
        self.templatePolygon.map = nil;
        self.templatePolygon = nil;
    }
    
    [self.currentPath removeAllCoordinates];
    
    self.currentPath = nil;
    
};

#pragma mark - helper methods

- (void)removeTemplates
{
    [self.templatePolyline removeFromMap];
    self.templatePolyline.map = nil;
    self.templatePolyline = nil;
    self.templatePolygon.map = nil;
    self.templatePolygon = nil;
}

- (void)drawBorder
{
    if ([self.currentPath count] >= 1) {
        if (!self.templatePolyline) {
            self.templatePolyline = [[DDPolyline alloc] initPolylineWithWithGMSPath:self.currentPath andMapView:self.mapView];
            self.templatePolyline.map = self.mapView;
            [self.templatePolyline setPolylineDrawable:YES];
            
        } else {
            [self.templatePolyline updatePath:self.currentPath];
            [self.templatePolyline setPolylineDrawable:YES];
        }
        
        if (self.currentPath.count == 1) {
            self.templatePolyline.map = nil;
        } else {
            self.templatePolyline.map = self.mapView;
        }
        
        self.templatePolyline.strokeColor = ValidStrokeColor;
    } else {
        [self.templatePolyline removeFromMap];
        self.templatePolyline.map = nil;
        self.templatePolyline = nil;
    }
}

- (void)drawFill
{
    if ([self.currentPath count] >= 3) {
        if (!self.templatePolygon) {
            self.templatePolygon = [GMSPolygon polygonWithPath:self.currentPath];
            self.templatePolygon.map = self.mapView;
        } else {
            [self.templatePolygon setPath:self.currentPath];
        }
        self.templatePolygon.fillColor = ValidFillColor;
    } else {
        self.templatePolygon.map = nil;
        self.templatePolygon = nil;
    }
}

- (void)drawInvalidSegment
{
    self.templatePolyline.strokeColor = InvalidStrokeColor;
    
    GMSMutablePath *path = [GMSMutablePath path];
    [path addCoordinate:[self.currentPath coordinateAtIndex:self.currentPath.count - 1]];
    [path addCoordinate:self.invalidPosition.coordinate];
    
    if (self.invalidSegmentPolyline) {
        self.invalidSegmentPolyline.map = nil;
    }
    self.invalidSegmentPolyline = [GMSPolyline polylineWithPath:path];
    
    NSArray *styles = @[[GMSStrokeStyle solidColor:InvalidStrokeColor],
                        [GMSStrokeStyle solidColor:[UIColor clearColor]]];
    NSArray *lengths = @[@2, @2];
    
    
    self.invalidSegmentPolyline.spans = GMSStyleSpans(path, styles, lengths, kGMSLengthRhumb);
    self.invalidSegmentPolyline.map = self.mapView;
}

- (void)deleteInvalidSegment
{
    self.templatePolyline.strokeColor = self.templatePolygon.strokeColor;
    
    self.invalidSegmentPolyline.map = nil;
    self.invalidSegmentPolyline = nil;
}

- (void)drawInvalidPolygon
{
    self.templatePolygon.fillColor = InvalidFillColor;
    self.templatePolyline.strokeColor = InvalidStrokeColor;
}

@end
