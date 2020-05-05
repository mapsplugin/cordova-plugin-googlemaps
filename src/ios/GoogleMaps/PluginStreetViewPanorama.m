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
  NSDictionary *meta = [command.arguments objectAtIndex:0];
  self.panoramaCtrl.viewDepth = [[meta objectForKey:@"depth"] integerValue];
  NSDictionary *initOptions = [command.arguments objectAtIndex:1];

  if ([initOptions objectForKey:@"camera"]) {
    [self _setPosition:[initOptions objectForKey:@"camera"]];
    [self _setPov:[initOptions objectForKey:@"camera"]];
  }
  if ([initOptions objectForKey:@"gestures"]) {
    NSDictionary *gestures = [initOptions objectForKey:@"gestures"];
    if ([gestures objectForKey:@"panning"]) {
      self.panoramaCtrl.panoramaView.orientationGestures = [[gestures objectForKey:@"panning"] boolValue];
    }
    if ([gestures objectForKey:@"zoom"]) {
      self.panoramaCtrl.panoramaView.zoomGestures = [[gestures objectForKey:@"zoom"] boolValue];
    }
  }
  if ([initOptions objectForKey:@"controls"]) {
    NSDictionary *controls = [initOptions objectForKey:@"controls"];
    if ([controls objectForKey:@"navigation"]) {
      self.panoramaCtrl.panoramaView.navigationLinksHidden = ![[controls objectForKey:@"navigation"] boolValue];
    }
    if ([controls objectForKey:@"streetNames"]) {
      self.panoramaCtrl.panoramaView.streetNamesHidden = ![[controls objectForKey:@"streetNames"] boolValue];
    }
  }
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)setPov:(CDVInvokedUrlCommand *)command {
  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSDictionary *cameraOpts = [command.arguments objectAtIndex:0];
      [self _setPov:cameraOpts];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}

- (void)setPosition:(CDVInvokedUrlCommand *)command {
  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      NSDictionary *cameraOpts = [command.arguments objectAtIndex:0];
      [self _setPosition:cameraOpts];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
- (void)_setPosition:(NSDictionary *)cameraOpts {

  if ([cameraOpts valueForKey:@"target"] && [cameraOpts valueForKey:@"target"] != [NSNull null]) {
    NSObject *target = [cameraOpts objectForKey:@"target"];
    if ([target isKindOfClass:NSString.class]) {
      [self.panoramaCtrl.panoramaView moveToPanoramaID:(NSString *)target];
    } else if ([target isKindOfClass:NSDictionary.class]) {
      NSDictionary *latLng = (NSDictionary *)target;
      double latitude = [[latLng valueForKey:@"lat"] doubleValue];
      double longitude = [[latLng valueForKey:@"lng"] doubleValue];
      CLLocationCoordinate2D location = CLLocationCoordinate2DMake(latitude, longitude);

      if ([cameraOpts objectForKey:@"source"] && [cameraOpts valueForKey:@"source"] != [NSNull null]) {
        GMSPanoramaSource source = [@"OUTDOOR" isEqualToString:[cameraOpts objectForKey:@"source"]] ?
          kGMSPanoramaSourceOutside : kGMSPanoramaSourceDefault;
        if ([cameraOpts objectForKey:@"radius"]) {
          int radius = [[cameraOpts objectForKey:@"radius"] intValue];
          [self.panoramaCtrl.panoramaView moveNearCoordinate:location radius:radius source:source];
        } else {
          [self.panoramaCtrl.panoramaView moveNearCoordinate:location source:source];
        }
      } else {
        if ([cameraOpts objectForKey:@"radius"]) {
          int radius = [[cameraOpts objectForKey:@"radius"] intValue];
          [self.panoramaCtrl.panoramaView moveNearCoordinate:location radius:radius];
        } else {
          [self.panoramaCtrl.panoramaView moveNearCoordinate:location];
        }
      }
    }
  }


}

- (void)_setPov:(NSDictionary *)cameraOpts {


  double bearing = self.panoramaCtrl.panoramaView.camera.orientation.heading;
  if ([cameraOpts valueForKey:@"bearing"] && [cameraOpts valueForKey:@"bearing"] != [NSNull null]) {
    bearing = [[cameraOpts valueForKey:@"bearing"] doubleValue];
  }

  double angle = self.panoramaCtrl.panoramaView.camera.orientation.pitch;
  if ([cameraOpts valueForKey:@"tilt"] && [cameraOpts valueForKey:@"tilt"] != [NSNull null]) {
    angle = [[cameraOpts valueForKey:@"tilt"] doubleValue];
  }

  float zoom = self.panoramaCtrl.panoramaView.camera.zoom;
  if ([cameraOpts valueForKey:@"zoom"] && [cameraOpts valueForKey:@"zoom"] != [NSNull null]) {
    zoom = [[cameraOpts valueForKey:@"zoom"] doubleValue];
  }

  self.panoramaCtrl.panoramaView.camera = [GMSPanoramaCamera cameraWithHeading:bearing pitch:angle zoom:zoom FOV:90];

}


- (void)setPanningGesturesEnabled:(CDVInvokedUrlCommand*)command{

  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      Boolean boolValue = [[command.arguments objectAtIndex:0] boolValue];
      [self.panoramaCtrl.panoramaView setOrientationGestures:boolValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
- (void)setZoomGesturesEnabled:(CDVInvokedUrlCommand*)command {

  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      Boolean boolValue = [[command.arguments objectAtIndex:0] boolValue];
      [self.panoramaCtrl.panoramaView setZoomGestures:boolValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
- (void)setNavigationEnabled:(CDVInvokedUrlCommand*)command {

  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      Boolean boolValue = ![[command.arguments objectAtIndex:0] boolValue];
      [self.panoramaCtrl.panoramaView setNavigationLinksHidden:boolValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
- (void)setStreetNamesEnabled:(CDVInvokedUrlCommand*)command {

  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      Boolean boolValue = ![[command.arguments objectAtIndex:0] boolValue];
      [self.panoramaCtrl.panoramaView setStreetNamesHidden:boolValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
- (void)setVisible:(CDVInvokedUrlCommand*)command {
  [self.panoramaCtrl.executeQueue addOperationWithBlock:^{
    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
      Boolean boolValue = ![[command.arguments objectAtIndex:0] boolValue];
      [self.panoramaCtrl.view setHidden:boolValue];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  }];
}
- (void)remove:(CDVInvokedUrlCommand*)command {
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{

    // Load the GoogleMap.m
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
    [googlemaps.pluginLayer removePluginOverlay:self.panoramaCtrl];
    self.panoramaCtrl.attached = NO;
    self.isRemoved = YES;
    self.panoramaCtrl.view = nil;
    self.panoramaCtrl = nil;

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

- (void)pluginUnload {

  // Plugin destroy
  self.isRemoved = YES;
  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    self.panoramaCtrl.view = nil;
    self.panoramaCtrl.panoramaView = nil;
    self.panoramaCtrl = nil;
  }];
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
