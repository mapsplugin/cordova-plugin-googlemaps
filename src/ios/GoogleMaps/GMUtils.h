//
//  Utils.h
//  DroneDeploy
//
//  Created by Nicolas Torres on 12/30/16.
//
//
#import <GoogleMaps/GoogleMaps.h>
#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

@interface GMUtils : NSObject

+ (instancetype)sharedInstance;

+ (NSString *)getMethodNameStringFromSelector:(SEL)selector;

- (int)getMaxZIndexValue;

- (void)updateMaxZIndexValue:(int)value;

+ (CLLocationCoordinate2D)getLocationFromDictionary:(NSDictionary *)locationDictionary;

+ (CLLocationCoordinate2D)getMidPointBetweenCoordinate:(CLLocationCoordinate2D)cordA andCoordinate:(CLLocationCoordinate2D)cordB;

+ (CLLocationCoordinate2D)calculateCenterCoordinate:(NSMutableArray *)arrayOfMarkers;

+ (BOOL)segmentToPoint:(CLLocationCoordinate2D)point intersectsWithPath:(GMSPath *)path;

+ (BOOL)checkNonConvexHullPath:(GMSPath *)path;

@end
