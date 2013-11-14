//
//  GoogleMaps.m
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import "GoogleMaps.h"

@implementation GoogleMaps

UIButton *closeButton;

- (void)pluginInitialize
{
  self.plugins = [NSMutableDictionary dictionary];
}

/**
 * Intialize the map
 */
- (void)getMap:(CDVInvokedUrlCommand *)command {
  
  if (!self.mapCtrl) {
    dispatch_queue_t gueue = dispatch_queue_create("plugins.google.maps.init", NULL);
    
    
    
    
    // Create a map view
    dispatch_sync(gueue, ^{
      //self.mapCtrl = [[GoogleMapsViewController alloc] init];
      NSDictionary *options = [command.arguments objectAtIndex:0];
      self.mapCtrl = [[GoogleMapsViewController alloc] initWithOptions:options];
      self.mapCtrl.webView = self.webView;
    });
    
    // Create an instance of Map Class
    dispatch_sync(gueue, ^{
      Map *mapClass = [[NSClassFromString(@"Map")alloc] initWithWebView:self.webView];
      mapClass.commandDelegate = self.commandDelegate;
      [mapClass setGoogleMapsViewController:self.mapCtrl];
      [self.plugins setObject:mapClass forKey:@"Map"];
    });
    
    // Create a close button
    dispatch_sync(gueue, ^{
      CGRect screenSize = [[UIScreen mainScreen] bounds];
      CGRect pluginRect;
      int direction = self.viewController.interfaceOrientation;
      if (direction == UIInterfaceOrientationLandscapeLeft ||
          direction == UIInterfaceOrientationLandscapeRight) {
          pluginRect = CGRectMake(screenSize.size.height * 0.05, screenSize.size.width * 0.05, screenSize.size.height * 0.9, screenSize.size.width * 0.9);
      
      } else {
        pluginRect = CGRectMake(screenSize.size.width * 0.05, screenSize.size.height * 0.05, screenSize.size.width * 0.9, screenSize.size.height * 0.9);
      }
      
      
      closeButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
      closeButton.frame = CGRectMake(0, pluginRect.size.height - 30, 50, 30);
      closeButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin;
      [closeButton setTitle:@"Close" forState:UIControlStateNormal];
      [closeButton addTarget:self action:@selector(onCloseBtn_clicked:) forControlEvents:UIControlEventTouchDown];
      [self.mapCtrl.view addSubview:closeButton];
    });
    
    dispatch_release(gueue);
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}



- (void)exec:(CDVInvokedUrlCommand *)command {
  
    NSLog(@"exec");
  
  [self.commandDelegate runInBackground:^{
    
    CDVPluginResult* pluginResult = nil;
    NSString *classAndMethod = [command.arguments objectAtIndex:0];
    NSLog(@"exec: %@",classAndMethod);
    
    NSArray *target = [classAndMethod componentsSeparatedByString:@"."];
    NSString *className = [target objectAtIndex:0];
    CDVPlugin<MyPlgunProtocol> *pluginClass = nil;
    NSString *methodName;
    
    if ([target count] == 2) {
      methodName = [NSString stringWithFormat:@"%@:", [target objectAtIndex:1]];
      
      pluginClass = [self.plugins objectForKey:className];
      if (!pluginClass) {
        pluginClass = [[NSClassFromString(className)alloc] initWithWebView:self.webView];
        if (pluginClass) {
          pluginClass.commandDelegate = self.commandDelegate;
          [pluginClass setGoogleMapsViewController:self.mapCtrl];
          [self.plugins setObject:pluginClass forKey:className];
        }
      }
      if (!pluginClass) {
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:[NSString stringWithFormat:@"Class not found: %@", className]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
        
      } else {
        SEL selector = NSSelectorFromString(methodName);
        if ([pluginClass respondsToSelector:selector]){
          [pluginClass performSelectorOnMainThread:selector withObject:command waitUntilDone:YES];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                           messageAsString:[NSString stringWithFormat:@"method not found: %@ in %@ class", [target objectAtIndex:1], className]];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
      }
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                       messageAsString:[NSString stringWithFormat:@"class not found: %@", className]];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  }];
}
- (void)dealloc
{
  NSLog(@"dealloc");
}

/**
 * Get license information
 */
-(void)getLicenseInfo:(CDVInvokedUrlCommand *)command
{
  NSLog(@"GetLicenseInfo");
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Hello"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Close the map window
 */
- (void)onCloseBtn_clicked:(UIButton*)button{
  [self.mapCtrl.view removeFromSuperview];
}

/**
 * Show the map window
 */
- (void)showDialog:(CDVInvokedUrlCommand *)command {
  [self.webView addSubview:self.mapCtrl.view];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Return the current position based on GPS
 */
-(void)getMyLocation:(CDVInvokedUrlCommand *)command
{
  
  CLLocationManager *locationManager = [[CLLocationManager alloc] init];
  locationManager.distanceFilter = kCLDistanceFilterNone;
  

  NSMutableDictionary *json = [NSMutableDictionary dictionary];
  [json setObject:[NSArray
      arrayWithObjects:[NSNumber numberWithFloat:locationManager.location.coordinate.latitude],
                       [NSNumber numberWithFloat:locationManager.location.coordinate.longitude],
                       nil] forKey:@"latLng"];
  [json setObject:[NSNumber numberWithFloat:[locationManager.location speed]] forKey:@"speed"];
  [json setObject:[NSNumber numberWithFloat:[locationManager.location altitude]] forKey:@"altitude"];
  
  //todo: calcurate the correct accuracy based on horizontalAccuracy and verticalAccuracy
  [json setObject:[NSNumber numberWithFloat:[locationManager.location horizontalAccuracy]] forKey:@"accuracy"];
  [json setObject:[NSNumber numberWithDouble:[locationManager.location.timestamp timeIntervalSince1970]] forKey:@"time"];
  [json setObject:[NSNumber numberWithInteger:[locationManager.location hash]] forKey:@"hashCode"];

  locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters;
  [locationManager startUpdatingLocation];
    
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
