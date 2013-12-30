//
//  Geocoder.h
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/29/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"
#import <CoreLocation/CoreLocation.h>

@interface Geocoder : CDVPlugin<MyPlgunProtocol>

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)createGeocoder:(CDVInvokedUrlCommand*)command;
@property (nonatomic, strong) CLGeocoder *geocoder;

@end
