//
//  LF_Marker.h
//  Erdgastankstellen
//
//  Created by Christian on 10.04.15.
//
//

#import <Foundation/Foundation.h>
#import "GClusterItem.h"

@interface LF_Marker : NSObject <GClusterItem>

@property (nonatomic) CLLocationCoordinate2D location;

@property (nonatomic, strong) GMSMarker * marker;

//-(id)initWithMarker:(GMSMarker*)marker;

@end
