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
  NSDictionary *initOptions = [command.arguments objectAtIndex:1];

  if ([initOptions objectForKey:@"camera"]) {
    [self movePanoramaCamera:[initOptions objectForKey:@"camera"]];
  }
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)moveCamera:(CDVInvokedUrlCommand *)command {
  NSDictionary *cameraOpts = [command.arguments objectAtIndex:0];
  [self movePanoramaCamera:cameraOpts];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)movePanoramaCamera:(NSDictionary *)cameraOpts {

  if ([cameraOpts valueForKey:@"target"]) {
    NSDictionary *latLng = [cameraOpts objectForKey:@"target"];
    double latitude = [[latLng valueForKey:@"lat"] doubleValue];
    double longitude = [[latLng valueForKey:@"lng"] doubleValue];
    [self.panoramaCtrl.panoramaView moveNearCoordinate:CLLocationCoordinate2DMake(latitude, longitude)];
  }

  double bearing = self.panoramaCtrl.panoramaView.camera.orientation.heading;
  if ([cameraOpts valueForKey:@"bearing"]) {
    bearing = [[cameraOpts valueForKey:@"bearing"] doubleValue];
  }

  double angle = self.panoramaCtrl.panoramaView.camera.orientation.pitch;
  if ([cameraOpts valueForKey:@"tilt"]) {
    angle = [[cameraOpts valueForKey:@"tilt"] doubleValue];
  }
  
  float zoom = self.panoramaCtrl.panoramaView.camera.zoom;
  if ([cameraOpts valueForKey:@"zoom"]) {
    zoom = [[cameraOpts valueForKey:@"zoom"] floatValue];
  }
  
  double fov = self.panoramaCtrl.panoramaView.camera.FOV;
  if ([cameraOpts valueForKey:@"fov"]) {
    fov = [[cameraOpts valueForKey:@"fov"] doubleValue];
  }
  
  self.panoramaCtrl.panoramaView.camera = [GMSPanoramaCamera cameraWithHeading:bearing pitch:angle zoom:zoom FOV:fov];

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


@end
