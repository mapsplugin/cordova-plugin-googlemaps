#import <Foundation/Foundation.h>
#import "GCluster.h"
#import "GQuadItem.h"

@interface GStaticCluster : NSObject <GCluster> 

- (id)initWithCoordinate:(CLLocationCoordinate2D)coordinate andMarker:(GMSMarker *)marker;

- (void)add:(GQuadItem*)item;

- (void)remove:(GQuadItem*)item;

@property (nonatomic, strong) GMSMarker *marker;

@end
