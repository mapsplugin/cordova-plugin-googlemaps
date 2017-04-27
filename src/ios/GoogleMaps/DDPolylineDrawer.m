//
//  DDPolylineDrawer.m
//  DroneDeployBeta
//
//  Created by Nicolas Torres on 4/26/17.
//
//

#import "DDPolylineDrawer.h"
#import "DDPolyline.h"

@interface DDPolylineDrawer ()

@property (nonatomic, strong) GMSMutablePath *currentPath;
@property (nonatomic, strong) GMSMapView *mapView;
@property (nonatomic, strong) DDPolyline *templatePolyline;
@end

@implementation DDPolylineDrawer

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
    if (!self.templatePolyline) {
        self.templatePolyline = [[DDPolyline alloc] initPolylineWithWithGMSPath:self.currentPath andMapView:self.mapView];
        self.templatePolyline.map = self.mapView;
        [self.templatePolyline setPolylineDrawable:YES];
        
    } else {
        [self.templatePolyline updatePath:self.currentPath];
        [self.templatePolyline setPolylineDrawable:YES];
    }
}

- (DDPolyline *)print
{
    [self.templatePolyline removeFromMap];
    self.templatePolyline.map = nil;
    self.templatePolyline = nil;
    
    DDPolyline *polyline = [[DDPolyline alloc] initPolylineWithWithGMSPath:self.currentPath andMapView:self.mapView];
    [polyline setPolylineEditable:YES];
    polyline.map = self.mapView;
    
    return polyline;
}

@end
