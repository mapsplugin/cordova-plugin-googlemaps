//
//  LatLngBounds.h
//  SimpleMap
//
//  Created by masashi on 11/14/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface LatLngBounds : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;

- (void)contains:(CDVInvokedUrlCommand*)command;

@end
