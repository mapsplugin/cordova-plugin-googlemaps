//
//  CordovaGoogleMaps.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
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
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
      self.executeQueue =  [NSOperationQueue new];
      self.executeQueue.maxConcurrentOperationCount = 10;
    });

    [[NSOperationQueue mainQueue] addOperationWithBlock:^{
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

      [GMSServices provideAPIKey:APIKey];
    }];

    //-------------------------------
    // Plugin initialization
    //-------------------------------
    self.pluginMaps = [[NSMutableDictionary alloc] init];

    self.pluginLayer = [[MyPluginLayer alloc] initWithWebView:self.webView];
    self.pluginLayer.backgroundColor = [UIColor whiteColor];
    self.pluginLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;


    NSArray *subViews = self.viewController.view.subviews;
    //NSLog(@"--->subViews count=%lu", subViews.count);
    UIView *view;
    for (int i = 0; i < [subViews count]; i++) {
      view = [subViews objectAtIndex:i];
      //NSLog(@"--->remove i=%d class=%@", i, view.class);
      [view removeFromSuperview];
      [self.pluginLayer addSubview: view];
    }
    [self.viewController.view addSubview:self.pluginLayer];
}
- (void) didRotate:(id)sender
{

  NSArray *keys = [self.pluginMaps allKeys];
  NSString *key;
  PluginMap *pluginMap;
  for (int i = 0; i < keys.count; i++) {
    key = [keys objectAtIndex:i];
    if ([self.pluginMaps objectForKey:key]) {
      pluginMap = [self.pluginMaps objectForKey:key];
      if (pluginMap.mapCtrl.map) {
         // Trigger the CAMERA_MOVE_END mandatory
        [pluginMap.mapCtrl mapView:pluginMap.mapCtrl.map idleAtCameraPosition:pluginMap.mapCtrl.map.camera];
      }
    }
  }

}


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

      @synchronized(self.pluginLayer.pluginScrollView.debugView.HTMLNodes) {
        [self.pluginLayer.pluginScrollView.debugView.HTMLNodes removeAllObjects];
        self.pluginLayer.pluginScrollView.debugView.HTMLNodes = nil;
      }
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
    //[pluginMap clear:nil];
    [pluginMap pluginUnload];

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

    [cdvViewController.pluginObjects removeObjectForKey:mapId];
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
    if (self.pluginLayer != nil) {
      self.pluginLayer.isSuspended = false;
    }

    dispatch_async(dispatch_get_main_queue(), ^{

        CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
        NSString *mapId = [command.arguments objectAtIndex:0];
        NSDictionary *initOptions = [command.arguments objectAtIndex:1];

        // Wrapper view
        GoogleMapsViewController* mapCtrl = [[GoogleMapsViewController alloc] initWithOptions:nil];
        mapCtrl.webView = self.webView;
        mapCtrl.isFullScreen = YES;
        mapCtrl.mapId = mapId;
        mapCtrl.mapDivId = nil;
        [mapCtrl.view setHidden:YES];

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
        GMSCameraPosition *camera = nil;
        int bearing = 0;
        double angle = 0, zoom = 0;
        NSDictionary *latLng = nil;
        double latitude = 0;
        double longitude = 0;
        GMSCoordinateBounds *cameraBounds = nil;
        NSDictionary *cameraOptions = [initOptions valueForKey:@"camera"];
        if (cameraOptions) {

          if ([cameraOptions valueForKey:@"bearing"]) {
            bearing = (int)[[cameraOptions valueForKey:@"bearing"] integerValue];
          } else {
            bearing = 0;
          }

          if ([cameraOptions valueForKey:@"tilt"]) {
            angle = [[cameraOptions valueForKey:@"tilt"] doubleValue];
          } else {
            angle = 0;
          }

          if ([cameraOptions valueForKey:@"zoom"] && [cameraOptions valueForKey:@"zoom"] != [NSNull null]) {
            zoom = [[cameraOptions valueForKey:@"zoom"] doubleValue];
          } else {
            zoom = 0;
          }
          if ([cameraOptions objectForKey:@"target"]) {
            NSString *targetClsName = [[cameraOptions objectForKey:@"target"] className];
            if ([targetClsName isEqualToString:@"__NSCFArray"] || [targetClsName isEqualToString:@"__NSArrayM"] ) {
              //--------------------------------------------
              //  cameraPosition.target = [
              //    new plugin.google.maps.LatLng(),
              //    ...
              //    new plugin.google.maps.LatLng()
              //  ]
              //---------------------------------------------
              int i = 0;
              NSArray *latLngList = [cameraOptions objectForKey:@"target"];
              GMSMutablePath *path = [GMSMutablePath path];
              for (i = 0; i < [latLngList count]; i++) {
                latLng = [latLngList objectAtIndex:i];
                latitude = [[latLng valueForKey:@"lat"] doubleValue];
                longitude = [[latLng valueForKey:@"lng"] doubleValue];
                [path addLatitude:latitude longitude:longitude];
              }

              cameraBounds = [[GMSCoordinateBounds alloc] initWithPath:path];
              //CLLocationCoordinate2D center = cameraBounds.center;

              latitude = cameraBounds.center.latitude;
              longitude = cameraBounds.center.longitude;
            } else {
              //------------------------------------------------------------------
              //  cameraPosition.target = new plugin.google.maps.LatLng();
              //------------------------------------------------------------------

              latLng = [cameraOptions objectForKey:@"target"];
              latitude = [[latLng valueForKey:@"lat"] floatValue];
              longitude = [[latLng valueForKey:@"lng"] floatValue];

            }
          }
          //[pluginMap.mapCtrl.view setHidden:YES];
        }
        camera = [GMSCameraPosition cameraWithLatitude:latitude
                                          longitude:longitude
                                          zoom: zoom
                                          bearing: bearing
                                          viewingAngle: angle];

        mapCtrl.map = [GMSMapView mapWithFrame:rect camera:camera];

        //mapType
        NSString *typeStr = [initOptions valueForKey:@"mapType"];
        if (typeStr) {

          NSDictionary *mapTypes = [NSDictionary dictionaryWithObjectsAndKeys:
                                    ^() {return kGMSTypeHybrid; }, @"MAP_TYPE_HYBRID",
                                    ^() {return kGMSTypeSatellite; }, @"MAP_TYPE_SATELLITE",
                                    ^() {return kGMSTypeTerrain; }, @"MAP_TYPE_TERRAIN",
                                    ^() {return kGMSTypeNormal; }, @"MAP_TYPE_NORMAL",
                                    ^() {return kGMSTypeNone; }, @"MAP_TYPE_NONE",
                                    nil];

          typedef GMSMapViewType (^CaseBlock)();
          GMSMapViewType mapType;
          CaseBlock caseBlock = mapTypes[typeStr];
          if (caseBlock) {
            // Change the map type
            mapType = caseBlock();

            [[NSOperationQueue mainQueue] addOperationWithBlock:^{
              pluginMap.mapCtrl.map.mapType = mapType;
            }];
          }
        }
        pluginMap.mapCtrl.map.delegate = mapCtrl;
        pluginMap.mapCtrl.map.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;


        //indoor display
        pluginMap.mapCtrl.map.indoorDisplay.delegate = mapCtrl;
        [mapCtrl.view addSubview:mapCtrl.map];
        [self.pluginLayer addMapView:mapCtrl];


        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [pluginMap getMap:command];
        });

    });
}


- (void)clearHtmlElements:(CDVInvokedUrlCommand *)command {
    [self.executeQueue addOperationWithBlock:^{
        if (self.pluginLayer != nil) {
          [self.pluginLayer clearHTMLElements];
        }
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)resume:(CDVInvokedUrlCommand *)command {
    if (self.pluginLayer != nil) {
      self.pluginLayer.isSuspended = false;
      dispatch_semaphore_signal(self.pluginLayer.semaphore);
    }
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}
- (void)pause:(CDVInvokedUrlCommand *)command {
    if (self.pluginLayer != nil) {
      self.pluginLayer.isSuspended = true;
    }
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
- (void)updateMapPositionOnly:(CDVInvokedUrlCommand *)command {
  [self.executeQueue addOperationWithBlock:^{
      if (self.pluginLayer != nil) {

        NSDictionary *elementsDic = [command.arguments objectAtIndex:0];
        NSString *domId;
        CGRect rect = CGRectMake(0, 0, 0, 0);
        NSMutableDictionary *domInfo, *size, *currentDomInfo;
        @synchronized(self.pluginLayer.pluginScrollView.debugView.HTMLNodes) {
          for (domId in elementsDic) {

            domInfo = [elementsDic objectForKey:domId];
            size = [domInfo objectForKey:@"size"];
            rect.origin.x = [[size objectForKey:@"left"] doubleValue];
            rect.origin.y = [[size objectForKey:@"top"] doubleValue];
            rect.size.width = [[size objectForKey:@"width"] doubleValue];
            rect.size.height = [[size objectForKey:@"height"] doubleValue];

            currentDomInfo = [self.pluginLayer.pluginScrollView.debugView.HTMLNodes objectForKey:domId];
            if (currentDomInfo == nil) {
              currentDomInfo = domInfo;
            }
            [currentDomInfo setValue:NSStringFromCGRect(rect) forKey:@"size"];
            [currentDomInfo setObject:[domInfo objectForKey:@"zIndex"] forKey:@"zIndex"];
            [self.pluginLayer.pluginScrollView.debugView.HTMLNodes setObject:currentDomInfo forKey:domId];
          }
        }

        self.pluginLayer.isSuspended = false;
        dispatch_semaphore_signal(self.pluginLayer.semaphore);
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [self.pluginLayer resizeTask:nil];
        }];
      }
      CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

- (void)putHtmlElements:(CDVInvokedUrlCommand *)command {
    [self.executeQueue addOperationWithBlock:^{

        NSDictionary *elements = [command.arguments objectAtIndex:0];

        if (self.pluginLayer != nil) {
          [self.pluginLayer putHTMLElements:elements];
        }
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
