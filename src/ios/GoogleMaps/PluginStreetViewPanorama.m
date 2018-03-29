//
//  PluginStreetViewPanorama.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginStreetViewPanorama.h"

@implementation PluginStreetViewPanorama


- (void)getPanorama:(CDVInvokedUrlCommand *)command {
  dispatch_async(dispatch_get_main_queue(), ^{
    [self.panoramaCtrl.panorama moveNearCoordinate:CLLocationCoordinate2DMake(-33.87365, 151.20689)];


    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  });

}

- (void)pluginUnload {

}

- (void)attachToWebView:(CDVInvokedUrlCommand*)command {
  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
    [googlemaps.pluginLayer addPluginOverlay:self.panoramaCtrl];
    self.panoramaCtrl.attached = YES;

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

- (void)detachFromWebView:(CDVInvokedUrlCommand*)command {

  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
    [googlemaps.pluginLayer removePluginOverlay:self.panoramaCtrl];
    self.panoramaCtrl.attached = NO;

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

}


//
//- (void)attachMap:(CDVInvokedUrlCommand*)command {
//  [self.mapCtrl.executeQueue addOperationWithBlock:^{
//
//    // Load the GoogleMap.m
//    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
//    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
//    [googlemaps.pluginLayer addMapView:self.mapCtrl];
//    self.mapCtrl.attached = YES;
//
//    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
//    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
//  }];
//}
//
//- (void)detachMap:(CDVInvokedUrlCommand*)command {
//
//  [self.mapCtrl.executeQueue addOperationWithBlock:^{
//
//    // Load the GoogleMap.m
//    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
//    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
//    [googlemaps.pluginLayer removeMapView:self.mapCtrl];
//    self.mapCtrl.attached = NO;
//
//    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
//    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
//  }];
//
//}
//

@end
