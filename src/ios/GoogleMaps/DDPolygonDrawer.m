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

@interface DDPolygonDrawer ()

@property (nonatomic, strong) GMSMutablePath *currentPath;
@property (nonatomic, strong) GMSMapView *mapView;
@property (nonatomic, strong) DDPolyline *templatePolyline;
@property (nonatomic, strong) GMSPolygon *templatePolygon;
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
    [self.currentPath addCoordinate:coordinate];
}

- (void)draw
{
    if ([self.currentPath count] > 0) {
        if (!self.templatePolyline) {
            self.templatePolyline = [[DDPolyline alloc] initPolylineWithWithGMSPath:self.currentPath andMapView:self.mapView];
            self.templatePolyline.map = self.mapView;
            [self.templatePolyline setPolylineDrawable:YES];
            
        } else {
            [self.templatePolyline updatePath:self.currentPath];
            [self.templatePolyline setPolylineDrawable:YES];
        }
    }
    
    if ([self.currentPath count] >= 3) {
        if (!self.templatePolygon) {
            self.templatePolygon = [GMSPolygon polygonWithPath:self.currentPath];
            self.templatePolygon.map = self.mapView;
        } else {
            [self.templatePolygon setPath:self.currentPath];
        }
    } else {
        self.templatePolygon.map = nil;
        self.templatePolygon = nil;
    }
    
}

- (DDPolygon *)print
{
    self.templatePolyline.map = nil;
    self.templatePolygon.map = nil;
    self.templatePolygon = nil;
    self.templatePolyline = nil;
    
    DDPolygon *polygon = [[DDPolygon alloc] initPolygonWithWithGMSPath:self.currentPath andMapView:self.mapView];
    
    return polygon;
}

@end
