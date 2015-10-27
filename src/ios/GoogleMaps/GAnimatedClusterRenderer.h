//
//  GAnimatedClusterRenderer.h
//  Erdgastankstellen
//
//  Created by Christian on 26.05.15.
//
//

#import "GDefaultClusterRenderer.h"

@interface GAnimatedClusterRenderer : GDefaultClusterRenderer//NSObject<GClusterRenderer>

//-(id)initWithMapView:(GMSMapView*)googleMap;
-(void)freeClusterIconCache;

-(void)clustersChanged:(NSSet *)clusters inRegion:(GMSVisibleRegion)region withZoom:(int)currentDiscreteZoom;

-(void)updateViewPortInRegion:(GMSVisibleRegion)region;

-(void) updateCluster:(NSSet *)clusters inRegion:(GMSVisibleRegion)region;
@end
