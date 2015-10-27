//
//  ClusterRenderer.h
//  Parkingmobility
//
//  Created by Colin Edwards on 1/18/14.
//  Copyright (c) 2014 Colin Edwards. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <GoogleMaps/GoogleMaps.h>

@protocol GClusterRenderer <NSObject>

@property (nonatomic, strong) GMSMapView * map;
@property (nonatomic, strong) NSDictionary *overlayManager; // To strore the IconDate of the marker

- (void)clustersChanged:(NSSet*)clusters;

- (void)clustersChanged:(NSSet *)clusters inRegion:(GMSVisibleRegion)region;

- (void)clustersChanged:(NSSet *)clusters inRegion:(GMSVisibleRegion)region withZoom:(int)currentDiscreteZoom;

- (void)updateViewPortInRegion:(GMSVisibleRegion)region;

@end
