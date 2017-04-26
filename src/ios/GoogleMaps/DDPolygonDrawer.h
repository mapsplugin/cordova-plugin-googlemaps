//
//  DDPolygonDrawer.h
//  DroneDeployBeta
//
//  Created by Pablo Yaniero on 25/4/17.
//
//

#import <Foundation/Foundation.h>
#import <GoogleMaps/GoogleMaps.h>

@class DDPolygon;

@interface DDPolygonDrawer : NSObject

- (instancetype)initWithMapView:(GMSMapView *)mapView;

- (void)pushCoordinate:(CLLocationCoordinate2D)coordinate;

- (void)draw;

- (DDPolygon *)print;

@end
