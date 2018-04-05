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
      NSString *errorTitle = [PluginUtil PGM_LOCALIZATION:@"APIKEY_IS_UNDEFINED_TITLE"];
      NSString *errorMsg = [PluginUtil PGM_LOCALIZATION:@"APIKEY_IS_UNDEFINED_MESSAGE"];

      UIAlertController* alert = [UIAlertController alertControllerWithTitle:errorTitle
                                                                     message:errorMsg
                                                              preferredStyle:UIAlertControllerStyleAlert];

      NSString *closeBtnLabel = [PluginUtil PGM_LOCALIZATION:@"CLOSE_BUTTON"];
      UIAlertAction* ok = [UIAlertAction actionWithTitle:closeBtnLabel
                                                   style:UIAlertActionStyleDefault
                                                 handler:^(UIAlertAction* action)
                           {
                             [alert dismissViewControllerAnimated:YES completion:nil];
                           }];

      [alert addAction:ok];

      [self.viewController presentViewController:alert
                                        animated:YES
                                      completion:nil];
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
  self.viewPlugins = [[NSMutableDictionary alloc] init];

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

  NSArray *keys = [self.viewPlugins allKeys];
  NSString *key;
  CDVPlugin<IPluginProtocol, IPluginView> *viewPlugin;
  PluginMap *pluginMap;
  for (int i = 0; i < keys.count; i++) {
    key = [keys objectAtIndex:i];
    if ([self.viewPlugins objectForKey:key]) {
      viewPlugin = [self.viewPlugins objectForKey:key];
      if ([viewPlugin isKindOfClass:[PluginMap class]]) {
        pluginMap = (PluginMap *)viewPlugin;
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
    NSArray *keys=[self.viewPlugins allKeys];
    for (int i = 0; i < [keys count]; i++) {
      mapId = [keys objectAtIndex:i];
      [self _destroyMap:mapId];
    }
    [self.viewPlugins removeAllObjects];

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
  if (![self.viewPlugins objectForKey:mapId]) {
    return;
  }
  CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
  
  CDVPlugin<IPluginView> *pluginView = [self.viewPlugins objectForKey:mapId];
  if ([mapId hasPrefix:@"streetview_"]) {
    PluginStreetViewPanorama *pluginSV = (PluginStreetViewPanorama *)pluginView;
    pluginSV.isRemoved = YES;
    //[pluginSV clear:nil];
    [pluginSV pluginUnload];
    [cdvViewController.pluginObjects setObject:pluginView forKey:mapId];
    [cdvViewController.pluginsMap setValue:mapId forKey:mapId];

    [self.pluginLayer removePluginOverlay:pluginSV.panoramaCtrl];
    pluginSV.panoramaCtrl.view = nil;
    pluginSV = nil;
  } else {
    PluginMap *pluginMap = (PluginMap *)pluginView;
    pluginMap.isRemoved = YES;
    //[pluginMap clear:nil];
    [pluginMap pluginUnload];
    
    [cdvViewController.pluginObjects setObject:pluginView forKey:mapId];
    [cdvViewController.pluginsMap setValue:mapId forKey:mapId];

    [self.pluginLayer removePluginOverlay:pluginMap.mapCtrl];
    
    pluginMap.mapCtrl.view = nil;
    [pluginMap.mapCtrl.plugins removeAllObjects];
    pluginMap.mapCtrl.plugins = nil;
    pluginMap.mapCtrl.view = nil;
    pluginMap.mapCtrl = nil;
    pluginMap = nil;
  }


  [self.viewPlugins removeObjectForKey:mapId];

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
    [self.pluginLayer startRedrawTimer];
  }

  /*---------------------------------------------------------------------------------------
   * If CFBundleExecutable is not English, the Google Maps SDK for iOS will crash.
   * So must be English.
   *
   * If you want to use non-english name for your app, you need to change your config.xml like this.
   *
   * <?xml version='1.0' encoding='utf-8'?>
   * <widget id="(package name)" version="0.0.1" xmlns="http://www.w3.org/ns/widgets" xmlns:cdv="http://cordova.apache.org/ns/1.0">
   *   <name short="(non-english app name)">(english app name)</name>
   *---------------------------------------------------------------------------------------*/

  NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
  NSString *CFBundleExecutable = [info objectForKey:@"CFBundleExecutable"];

  NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"^[a-zA-Z0-9$@$!%*?&#^\\-_.\\s+]+$" options:NSRegularExpressionCaseInsensitive error:nil];
  if ([regex numberOfMatchesInString:CFBundleExecutable options:0 range:NSMakeRange(0, CFBundleExecutable.length)] == 0) {

    NSString *APP_NAME_ERROR_TITLE = [PluginUtil PGM_LOCALIZATION:@"APP_NAME_ERROR_TITLE"];
    NSString *APP_NAME_ERROR_MESSAGE = [PluginUtil PGM_LOCALIZATION:@"APP_NAME_ERROR_MESSAGE"];

    UIAlertController* alert = [UIAlertController alertControllerWithTitle:APP_NAME_ERROR_TITLE
                                                                   message:APP_NAME_ERROR_MESSAGE
                                                            preferredStyle:UIAlertControllerStyleAlert];

    NSString *closeBtnLabel = [PluginUtil PGM_LOCALIZATION:@"CLOSE_BUTTON"];
    UIAlertAction* ok = [UIAlertAction actionWithTitle:closeBtnLabel
                                                 style:UIAlertActionStyleDefault
                                               handler:^(UIAlertAction* action)
                         {
                           [alert dismissViewControllerAnimated:YES completion:nil];

                           CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Can not use '%@' for CFBundleExecutable", CFBundleExecutable]];
                           [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

                         }];

    [alert addAction:ok];

    [self.viewController presentViewController:alert
                       animated:YES
                     completion:nil];
    return;
  }

  dispatch_async(dispatch_get_main_queue(), ^{

    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    NSString *mapId = [command.arguments objectAtIndex:0];
    NSDictionary *initOptions = [command.arguments objectAtIndex:1];

    // Wrapper view
    PluginMapViewController* viewCtrl = [[PluginMapViewController alloc] initWithOptions:nil];
    viewCtrl.webView = self.webView;
    viewCtrl.isFullScreen = YES;
    viewCtrl.overlayId = mapId;
    viewCtrl.divId = nil;
    [viewCtrl.view setHidden:YES];

    // Create an instance of the Map class everytime.
    PluginMap *pluginMap = [[PluginMap alloc] init];
    [pluginMap pluginInitialize];
    pluginMap.mapCtrl = viewCtrl;

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

    [self.viewPlugins setObject:pluginMap forKey:mapId];

    CGRect rect = CGRectZero;
    // Sets the map div id.
    if ([command.arguments count] == 3) {
      pluginMap.mapCtrl.divId = [command.arguments objectAtIndex:2];
      if (pluginMap.mapCtrl.divId != nil) {
        NSDictionary *domInfo = [self.pluginLayer.pluginScrollView.debugView.HTMLNodes objectForKey:pluginMap.mapCtrl.divId];
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

    viewCtrl.map = [GMSMapView mapWithFrame:rect camera:camera];
    viewCtrl.view = viewCtrl.map;

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
          ((GMSMapView *)(viewCtrl.view)).mapType = mapType;
        }];
      }
    }
    viewCtrl.view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;


    //indoor display
    ((GMSMapView *)(viewCtrl.view)).delegate = viewCtrl;
    ((GMSMapView *)(viewCtrl.view)).indoorDisplay.delegate = viewCtrl;
    [self.pluginLayer addPluginOverlay:viewCtrl];

    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
      [pluginMap getMap:command];
    });

  });
}

/**
 * Intialize the panorama
 */
- (void)getPanorama:(CDVInvokedUrlCommand *)command {
  if (self.pluginLayer != nil) {
    self.pluginLayer.isSuspended = false;
    [self.pluginLayer startRedrawTimer];
  }

  /*---------------------------------------------------------------------------------------
   * If CFBundleExecutable is not English, the Google Maps SDK for iOS will crash.
   * So must be English.
   *
   * If you want to use non-english name for your app, you need to change your config.xml like this.
   *
   * <?xml version='1.0' encoding='utf-8'?>
   * <widget id="(package name)" version="0.0.1" xmlns="http://www.w3.org/ns/widgets" xmlns:cdv="http://cordova.apache.org/ns/1.0">
   *   <name short="(non-english app name)">(english app name)</name>
   *---------------------------------------------------------------------------------------*/

  NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
  NSString *CFBundleExecutable = [info objectForKey:@"CFBundleExecutable"];

  NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"^[a-zA-Z0-9$@$!%*?&#^\\-_.\\s+]+$" options:NSRegularExpressionCaseInsensitive error:nil];
  if ([regex numberOfMatchesInString:CFBundleExecutable options:0 range:NSMakeRange(0, CFBundleExecutable.length)] == 0) {

    NSString *APP_NAME_ERROR_TITLE = [PluginUtil PGM_LOCALIZATION:@"APP_NAME_ERROR_TITLE"];
    NSString *APP_NAME_ERROR_MESSAGE = [PluginUtil PGM_LOCALIZATION:@"APP_NAME_ERROR_MESSAGE"];

    UIAlertController* alert = [UIAlertController alertControllerWithTitle:APP_NAME_ERROR_TITLE
                                                                   message:APP_NAME_ERROR_MESSAGE
                                                            preferredStyle:UIAlertControllerStyleAlert];

    NSString *closeBtnLabel = [PluginUtil PGM_LOCALIZATION:@"CLOSE_BUTTON"];
    UIAlertAction* ok = [UIAlertAction actionWithTitle:closeBtnLabel
                                                 style:UIAlertActionStyleDefault
                                               handler:^(UIAlertAction* action)
                         {
                           [alert dismissViewControllerAnimated:YES completion:nil];

                           CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Can not use '%@' for CFBundleExecutable", CFBundleExecutable]];
                           [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

                         }];

    [alert addAction:ok];

    [self.viewController presentViewController:alert
                                      animated:YES
                                    completion:nil];
    return;
  }

  dispatch_async(dispatch_get_main_queue(), ^{

    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    NSString *panoramaId = [command.arguments objectAtIndex:0];
    NSString *divId = [command.arguments objectAtIndex:2];

    // Wrapper view
    PluginStreetViewPanoramaController* panoramaCtrl = [[PluginStreetViewPanoramaController alloc] initWithOptions:nil];
    panoramaCtrl.webView = self.webView;
    panoramaCtrl.isFullScreen = YES;
    panoramaCtrl.overlayId = panoramaId;
    panoramaCtrl.divId = divId;
    panoramaCtrl.title = @"test";
    //[mapCtrl.view setHidden:YES];

    // Create an instance of the PluginStreetViewPanorama class everytime.
    PluginStreetViewPanorama *pluginStreetView = [[PluginStreetViewPanorama alloc] init];
    [pluginStreetView pluginInitialize];

    // Hack:
    // In order to load the plugin instance of the same class but different names,
    // register the street view plugin instance into the pluginObjects directly.
    if ([pluginStreetView respondsToSelector:@selector(setViewController:)]) {
      [pluginStreetView setViewController:cdvViewController];
    }
    if ([pluginStreetView respondsToSelector:@selector(setCommandDelegate:)]) {
      [pluginStreetView setCommandDelegate:cdvViewController.commandDelegate];
    }
    [cdvViewController.pluginObjects setObject:pluginStreetView forKey:panoramaId];
    [cdvViewController.pluginsMap setValue:panoramaId forKey:panoramaId];
    [pluginStreetView pluginInitialize];

    [self.viewPlugins setObject:pluginStreetView forKey:panoramaId];

    CGRect rect = CGRectZero;
    // Sets the panorama div id.
    pluginStreetView.panoramaCtrl = panoramaCtrl;
    pluginStreetView.panoramaCtrl.divId = divId;
    if (pluginStreetView.panoramaCtrl.divId != nil) {
      NSDictionary *domInfo = [self.pluginLayer.pluginScrollView.debugView.HTMLNodes objectForKey:pluginStreetView.panoramaCtrl.divId];
      if (domInfo != nil) {
        rect = CGRectFromString([domInfo objectForKey:@"size"]);
      }
    }

    panoramaCtrl.panoramaView = [GMSPanoramaView panoramaWithFrame:rect nearCoordinate: CLLocationCoordinate2DMake(0, 0)];
    panoramaCtrl.view = panoramaCtrl.panoramaView;
    panoramaCtrl.view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    ((GMSPanoramaView *)(panoramaCtrl.view)).delegate = panoramaCtrl;
    [self.pluginLayer addPluginOverlay:panoramaCtrl];

    [pluginStreetView getPanorama:command];

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
    self.pluginLayer.isSuspended = NO;
    [self.pluginLayer startRedrawTimer];
  }
  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}
- (void)pause:(CDVInvokedUrlCommand *)command {
  if (self.pluginLayer != nil) {
    if (!self.pluginLayer.isSuspended) {
      self.pluginLayer.isSuspended = YES;
      // cancel the timer
      [self.pluginLayer stopRedrawTimer];

      [self.pluginLayer resizeTask:nil];
    }
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
          [self.pluginLayer.pluginScrollView.debugView.HTMLNodes setObject:currentDomInfo forKey:domId];
        }
      }

      self.pluginLayer.isSuspended = false;
      [self.pluginLayer startRedrawTimer];
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
     NSArray *keys=[self.viewPlugins allKeys];
     NSString *mapId;
     PluginMap *pluginMap;

     for (int i = 0; i < [keys count]; i++) {
     mapId = [keys objectAtIndex:i];
     pluginMap = [self.viewPlugins objectForKey:mapId];
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
