//
//  PluginElevationService.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Cordova/CDV.h>
#import <GoogleMaps/GoogleMaps.h>
#import "PluginUtil.h"

@interface PluginElevationService : CDVPlugin

@property (nonatomic, strong) NSOperationQueue *executeQueue;
- (void)getElevationAlongPath:(CDVInvokedUrlCommand*)command;
- (void)getElevationForLocations:(CDVInvokedUrlCommand*)command;
@end
