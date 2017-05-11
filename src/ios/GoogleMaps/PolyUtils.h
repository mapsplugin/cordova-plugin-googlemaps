//
//  PolyUtils.h
//  HelloWorld
//
//  Created by Nicolas Torres on 1/12/17.
//
//

#import <Foundation/Foundation.h>
#import <GoogleMaps/GoogleMaps.h>

@interface PolyUtils : NSObject

+ (instancetype)sharedInstance;

- (GMSMutablePath *) generatePathWithParameters:(NSArray *)params;

- (NSMutableArray *)getLatLngsWithPath:(GMSPath *)path;

- (NSMutableArray *)getLatLngsForGeoJSONWithPath:(GMSPath *)path;

- (GMSMutablePath *)pathByAddingCoordinates:(NSArray *)params toPath:(GMSPath *)path;

- (NSString *)getStrokeColorStringFromColor:(UIColor *)color;

- (void)createBorderMarkerForOverlay:(GMSOverlay *)overlay withCoordinates:(CLLocationCoordinate2D)coordinates borderMarkersAray:(NSMutableArray *)borderMarkersAray andIsEditMode:(BOOL)editMode andMap:(GMSMapView *)mapView;
- (void)createMidMarkerForOverlay:(GMSOverlay *)overlay withCoordinates:(CLLocationCoordinate2D)coordinates midMarkersAray:(NSMutableArray *)midMarkersAray andIsEditMode:(BOOL)editMode andMap:(GMSMapView *)mapView;

@end
