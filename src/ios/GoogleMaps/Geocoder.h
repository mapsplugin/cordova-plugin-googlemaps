//
//  Geocoder.h
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/29/13.
//
//

#import "GoogleMaps.h"
#import "PluginUtil.h"
#import <CoreLocation/CoreLocation.h>

@interface Geocoder : CDVPlugin

@property (nonatomic, strong) CLGeocoder *geocoder;
@property (nonatomic, strong) GMSGeocoder *reverseGeocoder;
- (void)geocode:(CDVInvokedUrlCommand*)command;
- (NSArray *)geocoder_callback:(NSArray *)placemarks error:(NSError *)error;

@end
