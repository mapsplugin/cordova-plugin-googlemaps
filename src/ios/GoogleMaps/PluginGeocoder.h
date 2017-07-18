//
//  PluginGeocoder.h
//  cordova-googlemaps-plugin v2
//
//  Created by Katsumata Masashi.
//
//

#import "CordovaGoogleMaps.h"
#import "PluginUtil.h"
#import <CoreLocation/CoreLocation.h>

@interface PluginGeocoder : CDVPlugin
@property (nonatomic, strong) NSDictionary *codeForCountryDictionary;
@property (nonatomic, strong) NSOperationQueue *executeQueue;
- (void)geocode:(CDVInvokedUrlCommand*)command;
- (NSArray *)geocoder_callback:(NSArray *)placemarks error:(NSError *)error;

@end
