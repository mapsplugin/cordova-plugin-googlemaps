//
//  LF_Marker.m
//  Erdgastankstellen
//
//  Created by Christian on 10.04.15.
//
//

#import "LF_Marker.h"

@implementation LF_Marker
/*
-(id)initWithMarker:(GMSMarker *)marker {
    if (self = [super init]) {
        self.marker = marker;
        self.location = marker.position;
    }
    return self;
}
*/


- (CLLocationCoordinate2D)position {
    return self.location;
}

@end
