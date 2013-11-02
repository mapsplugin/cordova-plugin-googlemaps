//
//  GoogleMaps.m
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import "GoogleMaps.h"

@implementation GoogleMaps
- (void)GoogleMap_getMap:(CDVInvokedUrlCommand *)command {
    GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:-33.86
                                                          longitude:151.20
                                                               zoom:6];
    CGRect rect = CGRectMake(0, 0, 300, 300);
    [GMSMapView mapWithFrame:rect camera:camera];
    GMSMapView *mapView_ = [GMSMapView mapWithFrame:rect camera:camera];
    [self.webView addSubview:mapView_];


    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
 
 
}
@end
