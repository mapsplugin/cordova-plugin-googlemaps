//
//  GoogleMaps.m
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import "CordovaGoogleMaps.h"

@implementation CordovaGoogleMaps

- (void)pluginInitialize
{
#if CORDOVA_VERSION_MIN_REQUIRED >= __CORDOVA_4_0_0
    self.webView.backgroundColor = [UIColor clearColor];
    self.webView.opaque = NO;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pageDidLoad) name:CDVPageDidLoadNotification object:nil];
#endif
    self.executeQueue =  [NSOperationQueue new];
    //-------------------------------
    // Check the Google Maps API key
    //-------------------------------
    NSString *APIKey = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"Google Maps API Key"];
    if (APIKey == nil) {
        NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
        NSString *bundleName = [NSString stringWithFormat:@"%@", [info objectForKey:@"CFBundleDisplayName"]];
        NSString *message = [NSString stringWithFormat:@"Please replace 'API_KEY_FOR_IOS' in the platforms/ios/%@/%@-Info.plist with your API Key!", bundleName, bundleName];

        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"API key is not setted."
                                                        message:message
                                                       delegate:self
                                              cancelButtonTitle:@"CLOSE"
                                              otherButtonTitles:nil];
        [alert show];
        return;
    }

    [[NSNotificationCenter defaultCenter] addObserver:self
                                               selector:@selector(didRotate:)
                                                   name:UIDeviceOrientationDidChangeNotification object:nil];

    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];

    //-------------------------------
    // Plugin initialization
    //-------------------------------
    [GMSServices provideAPIKey:APIKey];
    self.pluginMaps = [[NSMutableDictionary alloc] init];
    self.locationCommandQueue = [[NSMutableArray alloc] init];

    self.pluginLayer = [[MyPluginLayer alloc] initWithWebView:self.webView];
    self.pluginLayer.backgroundColor = [UIColor whiteColor];
    self.pluginLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;


    NSArray *subViews = self.viewController.view.subviews;
    UIView *view;
    for (int i = 0; i < [subViews count]; i++) {
        view = [subViews objectAtIndex:i];
        //NSLog(@"remove i=%d class=%@", i, view.class);
        [view removeFromSuperview];
        [self.pluginLayer addSubview: view];
    }
    [self.viewController.view addSubview:self.pluginLayer];

}

- (void) didRotate:(id)sender
{}

-(void)viewDidLayoutSubviews {
    [self.pluginLayer.pluginScrollView setContentSize: self.webView.scrollView.contentSize];
    [self.pluginLayer.pluginScrollView flashScrollIndicators];
}

- (void)onReset
{
  [super onReset];

  // Reset the background color
  self.pluginLayer.backgroundColor = [UIColor whiteColor];

  dispatch_async(dispatch_get_main_queue(), ^{
    
      // Remove all url caches
      [[NSURLCache sharedURLCache] removeAllCachedResponses];
    
      // Remove old plugins that are used in the previous html.
      NSString *mapId;
      NSArray *keys=[self.pluginMaps allKeys];
      for (int i = 0; i < [keys count]; i++) {
        mapId = [keys objectAtIndex:i];
        [self _destroyMap:mapId];
      }
      [self.pluginMaps removeAllObjects];

      [self.pluginLayer.pluginScrollView.debugView.HTMLNodes removeAllObjects];
      self.pluginLayer.pluginScrollView.debugView.HTMLNodes = nil;
      [self.pluginLayer.pluginScrollView.debugView.mapCtrls removeAllObjects];
    
  });

}
-(void)pageDidLoad {
    self.webView.backgroundColor = [UIColor clearColor];
    self.webView.opaque = NO;
  
}

- (void)_destroyMap:(NSString *)mapId {
    PluginMap *pluginMap = [self.pluginMaps objectForKey:mapId];
    if (pluginMap == nil) {
        return;
    }

    pluginMap.isRemoved = YES;
    [pluginMap clear:nil];

    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    [cdvViewController.pluginObjects setObject:pluginMap forKey:mapId];
    [cdvViewController.pluginsMap setValue:mapId forKey:mapId];
  
    [self.pluginLayer removeMapView:pluginMap.mapCtrl];

    pluginMap.mapCtrl.view = nil;
    [pluginMap.mapCtrl.plugins removeAllObjects];
    pluginMap.mapCtrl.plugins = nil;
    pluginMap.mapCtrl.map.delegate = nil;
    pluginMap.mapCtrl.map = nil;
    pluginMap.mapCtrl = nil;
    [self.pluginMaps removeObjectForKey:mapId];
    pluginMap = nil;
}
/**
 * Remove the map
 */
- (void)removeMap:(CDVInvokedUrlCommand *)command {
    NSString *mapId = [command.arguments objectAtIndex:0];
    [self _destroyMap:mapId];
  
    if (command != nil) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

/**
 * Intialize the map
 */
- (void)getMap:(CDVInvokedUrlCommand *)command {

    dispatch_async(dispatch_get_main_queue(), ^{

        CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
        NSString *mapId = [command.arguments objectAtIndex:0];

        // Wrapper view
        GoogleMapsViewController* mapCtrl = [[GoogleMapsViewController alloc] initWithOptions:nil];
        mapCtrl.webView = self.webView;
        mapCtrl.isFullScreen = YES;
        mapCtrl.mapId = mapId;
        mapCtrl.mapDivId = nil;

        // Create an instance of the Map class everytime.
        PluginMap *pluginMap = [[PluginMap alloc] init];
        [pluginMap pluginInitialize];
        pluginMap.mapCtrl = mapCtrl;

        // Hack:
        // In order to load the plugin instance of the same class but different names,
        // register the map plugin instance into the pluginObjects directly.
        if ([pluginMap respondsToSelector:@selector(setViewController:)]) {
            [pluginMap setViewController:cdvViewController];
        }
        if ([pluginMap respondsToSelector:@selector(setCommandDelegate:)]) {
            [pluginMap setCommandDelegate:cdvViewController.commandDelegate];
        }
        [cdvViewController.pluginObjects setObject:pluginMap forKey:mapId];
        [cdvViewController.pluginsMap setValue:mapId forKey:mapId];
        [pluginMap pluginInitialize];

        [self.pluginMaps setObject:pluginMap forKey:mapId];

        CGRect rect = CGRectZero;
        // Sets the map div id.
        if ([command.arguments count] == 3) {
          pluginMap.mapCtrl.mapDivId = [command.arguments objectAtIndex:2];
          if (pluginMap.mapCtrl.mapDivId != nil) {
            NSDictionary *domInfo = [self.pluginLayer.pluginScrollView.debugView.HTMLNodes objectForKey:pluginMap.mapCtrl.mapDivId];
            if (domInfo != nil) {
              rect = CGRectFromString([domInfo objectForKey:@"size"]);
            }
          }
        }


        // Generate an instance of GMSMapView;
        GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:0
                                            longitude:0
                                            zoom:0
                                            bearing:0
                                            viewingAngle:0];

        pluginMap.mapCtrl.map = [GMSMapView mapWithFrame:rect camera:camera];
        pluginMap.mapCtrl.map.delegate = mapCtrl;
        pluginMap.mapCtrl.map.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;


        //indoor display
        pluginMap.mapCtrl.map.indoorDisplay.delegate = mapCtrl;
        [mapCtrl.view addSubview:pluginMap.mapCtrl.map];

        [self.pluginLayer addMapView:mapCtrl];

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [pluginMap getMap:command];
        });

    });
}


/**
 * Return the current position based on GPS
 */
-(void)getMyLocation:(CDVInvokedUrlCommand *)command
{
    // Obtain the authorizationStatus
    CLAuthorizationStatus status = [CLLocationManager authorizationStatus];
    if (status == kCLAuthorizationStatusDenied ||
        status == kCLAuthorizationStatusRestricted) {
        //----------------------------------------------------
        // kCLAuthorizationStatusDenied
        // kCLAuthorizationStatusRestricted
        //----------------------------------------------------
        UIAlertView *alertView = [[UIAlertView alloc]
                                  initWithTitle:@"Location Services disabled"
                                  message:@"This app needs access to your location. Please turn on Location Services in your device settings."
                                  delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [alertView show];

        NSString *error_code = @"service_denied";
        NSString *error_message = @"This app has rejected to use Location Services.";

        NSMutableDictionary *json = [NSMutableDictionary dictionary];
        [json setObject:[NSNumber numberWithBool:NO] forKey:@"status"];
        [json setObject:[NSString stringWithString:error_message] forKey:@"error_message"];
        [json setObject:[NSString stringWithString:error_code] forKey:@"error_code"];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:json];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {

        if (self.locationManager == nil) {
            self.locationManager = [[CLLocationManager alloc] init];
        }
        self.locationManager.delegate = self;


        //----------------------------------------------------
        // kCLAuthorizationStatusNotDetermined
        // kCLAuthorizationStatusAuthorized
        // kCLAuthorizationStatusAuthorizedAlways
        // kCLAuthorizationStatusAuthorizedWhenInUse
        //----------------------------------------------------
        CLLocationAccuracy locationAccuracy = kCLLocationAccuracyNearestTenMeters;
        NSDictionary *opts = [command.arguments objectAtIndex:0];
        BOOL isEnabledHighAccuracy = NO;
        if ([opts objectForKey:@"enableHighAccuracy"]) {
            isEnabledHighAccuracy = [[opts objectForKey:@"enableHighAccuracy"] boolValue];
        }

        if (isEnabledHighAccuracy == YES) {
            locationAccuracy = kCLLocationAccuracyBestForNavigation;
            self.locationManager.distanceFilter = 5;
        } else {
            self.locationManager.distanceFilter = 10;
        }
        self.locationManager.desiredAccuracy = locationAccuracy;

        //http://stackoverflow.com/questions/24268070/ignore-ios8-code-in-xcode-5-compilation
        [self.locationManager requestWhenInUseAuthorization];
        
        [self.locationManager stopUpdatingLocation];
        [self.locationManager startUpdatingLocation];
        [self.locationCommandQueue addObject:command];

        //CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
        //[pluginResult setKeepCallbackAsBool:YES];
        //[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

-(void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations {
    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithFloat:self.locationManager.location.coordinate.latitude] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithFloat:self.locationManager.location.coordinate.longitude] forKey:@"lng"];

    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    [json setObject:[NSNumber numberWithBool:YES] forKey:@"status"];

    [json setObject:latLng forKey:@"latLng"];
    [json setObject:[NSNumber numberWithFloat:[self.locationManager.location speed]] forKey:@"speed"];
    [json setObject:[NSNumber numberWithFloat:[self.locationManager.location altitude]] forKey:@"altitude"];

    //todo: calcurate the correct accuracy based on horizontalAccuracy and verticalAccuracy
    [json setObject:[NSNumber numberWithFloat:[self.locationManager.location horizontalAccuracy]] forKey:@"accuracy"];
    [json setObject:[NSNumber numberWithDouble:[self.locationManager.location.timestamp timeIntervalSince1970]] forKey:@"time"];
    [json setObject:[NSNumber numberWithInteger:[self.locationManager.location hash]] forKey:@"hashCode"];

    for (CDVInvokedUrlCommand *command in self.locationCommandQueue) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

    [self.locationCommandQueue removeAllObjects];
    [self.locationManager stopUpdatingLocation];
    //self.locationManager.delegate = nil;
    //self.locationManager = nil;
}
- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    [json setObject:[NSNumber numberWithBool:NO] forKey:@"status"];
    NSString *error_code = @"error";
    NSString *error_message = @"Cannot get your location.";
    if (error.code == kCLErrorDenied) {
        error_code = @"service_denied";
        error_message = @"This app has rejected to use Location Services.";
    }

    [json setObject:[NSString stringWithString:error_message] forKey:@"error_message"];
    [json setObject:[NSString stringWithString:error_code] forKey:@"error_code"];

    for (CDVInvokedUrlCommand *command in self.locationCommandQueue) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:json];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

- (void)putHtmlElements:(CDVInvokedUrlCommand *)command {
    [self.executeQueue addOperationWithBlock:^{
    
        NSDictionary *elements = [command.arguments objectAtIndex:0];

        [self.pluginLayer putHTMLElements:elements];
/*
        if (self.pluginLayer.needUpdatePosition) {
            self.pluginLayer.needUpdatePosition = NO;
            NSArray *keys=[self.pluginMaps allKeys];
            NSString *mapId;
            PluginMap *pluginMap;

            for (int i = 0; i < [keys count]; i++) {
              mapId = [keys objectAtIndex:i];
              pluginMap = [self.pluginMaps objectForKey:mapId];
              [self.pluginLayer updateViewPosition:pluginMap.mapCtrl];
            }
        }
*/
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        pluginResult = nil;
        elements = nil;
    }];
}

@end
