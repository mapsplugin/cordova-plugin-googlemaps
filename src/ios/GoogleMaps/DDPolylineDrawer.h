//
//  DDPolylineDrawer.h
//  DroneDeployBeta
//
//  Created by Nicolas Torres on 4/26/17.
//
//

#import <Foundation/Foundation.h>
#import <GoogleMaps/GoogleMaps.h>

@class DDPolyline;

@interface DDPolylineDrawer : NSObject

- (instancetype)initWithMapView:(GMSMapView *)mapView;

- (void)pushCoordinate:(CLLocationCoordinate2D)coordinate;

- (void)draw;

- (DDPolyline *)print;

@end
