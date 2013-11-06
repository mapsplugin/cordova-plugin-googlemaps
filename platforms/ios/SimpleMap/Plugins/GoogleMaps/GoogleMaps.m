//
//  GoogleMaps.m
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import "GoogleMaps.h"

@implementation GoogleMaps
GMSMapView *mapView_;
UIView *pluginView;

- (void)GoogleMap_getMap:(CDVInvokedUrlCommand *)command {
    [GMSServices provideAPIKey:@"AIzaSyADns99mO62aBapBN4_cnCJQnYdh-j6-ug"];
  
  
  
    GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:0
                                longitude:0
                                zoom:0];
    CGRect screenSize = [[UIScreen mainScreen] bounds];
    CGRect pluginRect = CGRectMake(screenSize.size.width * 0.05, screenSize.size.height * 0.05, screenSize.size.width * 0.9, screenSize.size.height * 0.9);
    CGRect mapRect = CGRectMake(pluginRect.size.width * 0.05, pluginRect.size.height * 0.05, pluginRect.size.width * 0.9, pluginRect.size.height * 0.9 - 30);
  
    pluginView = [[UIView alloc] initWithFrame:pluginRect];
    pluginView.backgroundColor = [UIColor lightGrayColor];
  
    [GMSMapView mapWithFrame:mapRect camera:camera];
    mapView_ = [GMSMapView mapWithFrame:mapRect camera:camera];
    mapView_.settings.myLocationButton = YES;
    [pluginView addSubview:mapView_];

    UIButton *closeButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    closeButton.frame = CGRectMake(0, pluginRect.size.height * 0.9, 50, 30);
    [closeButton setTitle:@"Close" forState:UIControlStateNormal];
    [closeButton addTarget:self action:@selector(onCloseBtn_clicked:) forControlEvents:UIControlEventTouchDown];
    [pluginView addSubview:closeButton];
  
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}
- (void)onCloseBtn_clicked:(UIButton*)button{
  [pluginView removeFromSuperview];
}

- (void)GoogleMap_show:(CDVInvokedUrlCommand *)command {
    [self.webView addSubview:pluginView];
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setCenter:(CDVInvokedUrlCommand *)command {

    float latitude = [[command.arguments objectAtIndex:0] floatValue];
    float longitude = [[command.arguments objectAtIndex:1] floatValue];
  
    [mapView_ animateToLocation:CLLocationCoordinate2DMake(latitude, longitude)];
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setMyLocationEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnable = [[command.arguments objectAtIndex:0] boolValue];
    mapView_.settings.myLocationButton = isEnable;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setIndoorEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnable = [[command.arguments objectAtIndex:0] boolValue];
    mapView_.settings.indoorPicker = isEnable;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setTrafficEnabled:(CDVInvokedUrlCommand *)command {
    NSLog(@"setTrafficEnabled is ignored in iOS");
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)GoogleMap_setCompassEnabled:(CDVInvokedUrlCommand *)command {
    Boolean isEnable = [[command.arguments objectAtIndex:0] boolValue];
    mapView_.settings.compassButton = isEnable;
  
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
