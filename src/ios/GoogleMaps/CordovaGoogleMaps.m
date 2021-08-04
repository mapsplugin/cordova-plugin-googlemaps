//
//  CordovaGoogleMaps.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"

@implementation CordovaGoogleMaps

- (instancetype)init {
  self = [super init];
  if (viewPlugins == nil) {
    viewPlugins = [NSMutableDictionary dictionary];
  }
  return self;
}

+ (id)getViewPlugin:(NSString *)pluginId {
  return [viewPlugins objectForKey:pluginId];
}
- (void)pluginInitialize
{
  self.isSdkAvailable = NO;

  self.webView.backgroundColor = [UIColor clearColor];
  self.webView.opaque = NO;
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pageDidLoad) name:CDVPageDidLoadNotification object:nil];
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    self.executeQueue =  [NSOperationQueue new];
    self.executeQueue.maxConcurrentOperationCount = 10;
  });

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    //-------------------------------
    // Check the Google Maps API key
    //-------------------------------
    NSString *APIKey = nil;
    #ifdef PGM_PLATFORM_CAPACITOR
      NSBundle *mainBundle = [NSBundle mainBundle];
      NSString *path = [NSString stringWithFormat:@"%@/capacitor.config.json", [mainBundle bundlePath]];
      NSString *fileContents = [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:nil];
      NSData *data = [fileContents dataUsingEncoding:NSUTF8StringEncoding];
      NSDictionary *json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
      if (json != nil) {
        NSDictionary *properties = [json objectForKey:@"googlemaps"];
        if (properties != nil) {
          APIKey = [properties objectForKey:@"GOOGLE_MAPS_IOS_API_KEY"];
        }
      }
    #endif

    #ifdef PGM_PLATFORM_CORDOVA
      CDVViewController *viewCtrl = (CDVViewController *)self.viewController;
      APIKey = [viewCtrl.settings objectForKey:@"google_maps_ios_api_key"];
    #endif

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
    self.isSdkAvailable = YES;

    NSUserDefaults *myDefaults = [[NSUserDefaults alloc] initWithSuiteName:@"cordova.plugin.googlemaps"];
    [myDefaults setObject:APIKey forKey:@"GOOGLE_MAPS_API_KEY"];
    [myDefaults synchronize];
  }];

  //-------------------------------
  // Plugin initialization
  //-------------------------------

  self.pluginLayer = [[PgmPluginLayer alloc] initWithWebView:self.webView];
  self.pluginLayer.backgroundColor = [UIColor clearColor];
  self.pluginLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  self.webView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

  #ifdef PGM_PLATFORM_CAPACITOR
    // TODO: other plugin's view have to move under the pluginLayer
    [UIApplication sharedApplication].windows[0].rootViewController.view = self.pluginLayer;

  #endif
  #ifdef PGM_PLATFORM_CORDOVA
    NSArray *subViews1 = self.viewController.view.subviews;
/*
    UIView *view1;
    for (int i = 0; i < [subViews1 count]; i++) {
      view1 = [subViews1 objectAtIndex:i];
      //NSLog(@"--->remove i=%d class=%@", i, view.class);
      [view1 removeFromSuperview];
      [self.pluginLayer addSubview: view1];
    }
    [self.viewController.view addSubview:self.pluginLayer]
 */
  #endif



  NSArray *subViews = self.viewController.view.subviews;
  //NSLog(@"--->subViews count=%lu", subViews.count);
  UIView *view;
  for (int i = 0; i < [subViews count]; i++) {
    view = [subViews objectAtIndex:i];
    //NSLog(@"--->remove i=%d class=%@", i, view.class);
    [view removeFromSuperview];
    [self.pluginLayer addSubview: view];
  }
  #ifdef PGM_PLATFORM_CORDOVA
    [self.viewController.view addSubview:self.pluginLayer];
  #endif
}
- (void) didRotate:(id)sender
{

  NSArray *keys = [viewPlugins allKeys];
  NSString *key;
  CDVPlugin<IPluginProtocol> *viewPlugin;
  PluginMap *pluginMap;
  for (int i = 0; i < keys.count; i++) {
    key = [keys objectAtIndex:i];
    if ([viewPlugins objectForKey:key]) {
      viewPlugin = [viewPlugins objectForKey:key];
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
  self.pluginLayer.backgroundColor = [UIColor clearColor];

  dispatch_async(dispatch_get_main_queue(), ^{

    // Remove all url caches
    [[NSURLCache sharedURLCache] removeAllCachedResponses];

    // Remove old plugins that are used in the previous html.
    NSString *mapId;
    NSArray *keys=[viewPlugins allKeys];
    for (int i = 0; i < [keys count]; i++) {
      mapId = [keys objectAtIndex:i];
      [self _destroyMap:mapId];
    }
    [viewPlugins removeAllObjects];

    @synchronized(self.pluginLayer.pluginScrollView.HTMLNodes) {
      [self.pluginLayer.pluginScrollView.HTMLNodes removeAllObjects];
      self.pluginLayer.pluginScrollView.HTMLNodes = nil;
    }
    [self.pluginLayer.pluginScrollView.mapCtrls removeAllObjects];

  });

}
-(void)pageDidLoad {
  self.webView.backgroundColor = [UIColor clearColor];
  self.webView.opaque = NO;

}

- (void)_destroyMap:(NSString *)mapId {
  if (![viewPlugins objectForKey:mapId]) {
    return;
  }

  CDVPlugin *pluginView = [viewPlugins objectForKey:mapId];
  if ([mapId hasPrefix:@"streetview_"]) {
    PluginStreetViewPanorama *pluginSV = (PluginStreetViewPanorama *)pluginView;
    pluginSV.isRemoved = YES;
    //[pluginSV clear:nil];
    [pluginSV pluginUnload];


    [self.pluginLayer removePluginOverlay:pluginSV.panoramaCtrl];
    pluginSV.panoramaCtrl.view = nil;
    pluginSV = nil;
  } else {
    PluginMap *pluginMap = (PluginMap *)pluginView;
    pluginMap.isRemoved = YES;
    //[pluginMap clear:nil];
    [pluginMap pluginUnload];

    [self.pluginLayer removePluginOverlay:pluginMap.mapCtrl];

    pluginMap.mapCtrl.view = nil;
    [pluginMap.mapCtrl.plugins removeAllObjects];
    pluginMap.mapCtrl.plugins = nil;
    pluginMap.mapCtrl.view = nil;
    pluginMap.mapCtrl = nil;
    pluginMap = nil;
  }


  [viewPlugins removeObjectForKey:mapId];

}


- (void)resizeMap:(CDVInvokedUrlCommand *)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];

  [instance.mapCtrl.executeQueue addOperationWithBlock:^{

    NSString *mapDivId = instance.mapCtrl.divId;
    if (!mapDivId) {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      return;
    }

    // Load the GoogleMap.m

    // Save the map rectangle.
    if (![self.pluginLayer.pluginScrollView.HTMLNodes objectForKey:instance.mapCtrl.divId]) {
      NSMutableDictionary *dummyInfo = [[NSMutableDictionary alloc] init];;
      [dummyInfo setObject:@"{{0,-3000} - {50,50}}" forKey:@"size"];
      [dummyInfo setObject:[NSNumber numberWithDouble:-999] forKey:@"depth"];
      [self.pluginLayer.pluginScrollView.HTMLNodes setObject:dummyInfo forKey:instance.mapCtrl.divId];
    }


    dispatch_async(dispatch_get_main_queue(), ^{
      [self.pluginLayer updateViewPosition:instance.mapCtrl];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });

  }];
}



- (void)setDiv:(CDVInvokedUrlCommand *)command {

  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];


  // Detach the map view
  if ([command.arguments count] == 1) {
    [self.pluginLayer removePluginOverlay:instance.mapCtrl];
    instance.mapCtrl.attached = NO;
    instance.mapCtrl.view = nil;
  } else {
    instance.mapCtrl.view = instance.mapCtrl.map;
    [self.pluginLayer addPluginOverlay:instance.mapCtrl];
    NSString *mapDivId = [command.arguments objectAtIndex:1];
    instance.mapCtrl.divId = mapDivId;
    instance.mapCtrl.attached = YES;
    instance.mapCtrl.isRenderedAtOnce = NO; //prevent unexpected animation
    [self.pluginLayer updateViewPosition:instance.mapCtrl];
  }
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)attachToWebView:(CDVInvokedUrlCommand*)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];

  [instance.mapCtrl.executeQueue addOperationWithBlock:^{

    [self.pluginLayer addPluginOverlay:instance.mapCtrl];
    instance.mapCtrl.attached = YES;

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];
}

- (void)detachFromWebView:(CDVInvokedUrlCommand*)command {
  NSString *mapId = [command.arguments objectAtIndex:0];
  PluginMap *instance = [CordovaGoogleMaps getViewPlugin:mapId];


  [instance.mapCtrl.executeQueue addOperationWithBlock:^{

    [self.pluginLayer removePluginOverlay:instance.mapCtrl];
    instance.mapCtrl.attached = NO;

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [instance.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }];

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

- (void)setBackGroundColor:(CDVInvokedUrlCommand *)command {
  NSArray *rgbColor = [command.arguments objectAtIndex:0];

  NSString *clsName;
  for (UIView *subView in [self.webView subviews]) {
    clsName = [NSString stringWithFormat:@"%@", subView.class];
    if ([@"WKScrollView" isEqualToString:clsName]) {
      subView.backgroundColor = [UIColor clearColor];
      subView.opaque = NO;

      for (UIView *view in [subView subviews]) {
        clsName = [NSString stringWithFormat:@"%@", view.class];
        if ([@"WKContentView" isEqualToString:clsName]) {
          view.backgroundColor = [UIColor clearColor];
          view.opaque = NO;
        }
      }
    }
  }
  self.pluginLayer.backgroundColor = [rgbColor parsePluginColor];


  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getLicenseInfo:(CDVInvokedUrlCommand *)command {

  NSString *txt = [GMSServices openSourceLicenseInfo];
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:txt];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)setEnv:(CDVInvokedUrlCommand *)command {
  // TODO:

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Intialize the map
 */
- (void)getMap:(CDVInvokedUrlCommand *)command {
  if (!self.isSdkAvailable) {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Can not use Google Maps SDK"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    return;
  }
  if (self.pluginLayer != nil) {
    self.pluginLayer.isSuspended = false;
  }



  NSDictionary *meta = [command.arguments objectAtIndex:0];
  NSString *mapId = [meta objectForKey:@"__pgmId"];
  NSDictionary *initOptions = [command.arguments objectAtIndex:1];

  // Wrapper view
  PluginMapViewController* viewCtrl = [[PluginMapViewController alloc] initWithOptions:nil];
  viewCtrl.webView = self.webView;
  viewCtrl.isFullScreen = YES;
  viewCtrl.overlayId = mapId;
  viewCtrl.title = mapId;
  viewCtrl.divId = nil;
  [viewCtrl.view setHidden:YES];


  // Create an instance of the Map class everytime.
  PluginMap *pluginMap = [[PluginMap alloc] init];

  [pluginMap setCommandDelegate:self.commandDelegate];
  [pluginMap setViewController:viewCtrl];

  pluginMap.mapCtrl = viewCtrl;

  [pluginMap pluginInitialize];

  [viewPlugins setObject:pluginMap forKey:mapId];

  CGRect rect = CGRectZero;
  // Sets the map div id.
  if ([command.arguments count] == 3) {
    pluginMap.mapCtrl.divId = [command.arguments objectAtIndex:2];
    if (pluginMap.mapCtrl.divId != nil) {
      NSDictionary *domInfo = [self.pluginLayer.pluginScrollView.HTMLNodes objectForKey:pluginMap.mapCtrl.divId];
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

    if ([cameraOptions valueForKey:@"bearing"] && [cameraOptions valueForKey:@"bearing"] != [NSNull null]) {
      bearing = (int)[[cameraOptions valueForKey:@"bearing"] integerValue];
    } else {
      bearing = 0;
    }

    if ([cameraOptions valueForKey:@"tilt"] && [cameraOptions valueForKey:@"tilt"] != [NSNull null]) {
      angle = [[cameraOptions valueForKey:@"tilt"] doubleValue];
    } else {
      angle = 0;
    }

    if ([cameraOptions valueForKey:@"zoom"] && [cameraOptions valueForKey:@"zoom"] != [NSNull null]) {
      zoom = [[cameraOptions valueForKey:@"zoom"] doubleValue];
    } else {
      zoom = 0;
    }
    if ([cameraOptions objectForKey:@"target"] && [cameraOptions valueForKey:@"target"] != [NSNull null]) {
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

        CLLocationCoordinate2D center = GMSGeometryInterpolate(cameraBounds.northEast, cameraBounds.southWest, 0.5);
        latitude = center.latitude;
        longitude = center.longitude;
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

    typedef GMSMapViewType (^CaseBlock)(void);
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

}

/**
 * Intialize the panorama
 */
- (void)getPanorama:(CDVInvokedUrlCommand *)command {
  if (!self.isSdkAvailable) {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Can not use Google Maps SDK"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    return;
  }
  if (self.pluginLayer != nil) {
    self.pluginLayer.isSuspended = false;
  }

  dispatch_async(dispatch_get_main_queue(), ^{

    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    NSDictionary *meta = [command.arguments objectAtIndex:0];
    NSString *panoramaId = [meta objectForKey:@"__pgmId"];
    NSString *divId = [command.arguments objectAtIndex:2];

    // Wrapper view
    PluginStreetViewPanoramaController* panoramaCtrl = [[PluginStreetViewPanoramaController alloc] initWithOptions:nil];
    panoramaCtrl.webView = self.webView;
    panoramaCtrl.isFullScreen = YES;
    panoramaCtrl.overlayId = panoramaId;
    panoramaCtrl.divId = divId;
    panoramaCtrl.title = panoramaId;
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
       #ifdef PGM_PLATFORM_CAPACITOR
        [pluginStreetView setCommandDelegate:self.commandDelegate];
        pluginStreetView.webView = self.webView;
       #endif
       #ifdef PGM_PLATFORM_CORDOVA
        [pluginStreetView setCommandDelegate:cdvViewController.commandDelegate];
       #endif
    }


    [pluginStreetView pluginInitialize];

    [viewPlugins setObject:pluginStreetView forKey:panoramaId];

    CGRect rect = CGRectZero;
    // Sets the panorama div id.
    pluginStreetView.panoramaCtrl = panoramaCtrl;
    pluginStreetView.panoramaCtrl.divId = divId;
    if (pluginStreetView.panoramaCtrl.divId != nil) {
      NSDictionary *domInfo = [self.pluginLayer.pluginScrollView.HTMLNodes objectForKey:pluginStreetView.panoramaCtrl.divId];
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

      //[self.pluginLayer resizeTask:nil];
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
      @synchronized(self.pluginLayer.pluginScrollView.HTMLNodes) {
        for (domId in elementsDic) {

          domInfo = [elementsDic objectForKey:domId];
          size = [domInfo objectForKey:@"size"];
          rect.origin.x = [[size objectForKey:@"left"] doubleValue];
          rect.origin.y = [[size objectForKey:@"top"] doubleValue];
          rect.size.width = [[size objectForKey:@"width"] doubleValue];
          rect.size.height = [[size objectForKey:@"height"] doubleValue];

          currentDomInfo = [self.pluginLayer.pluginScrollView.HTMLNodes objectForKey:domId];
          if (currentDomInfo == nil) {
            currentDomInfo = domInfo;
          }
          [currentDomInfo setValue:NSStringFromCGRect(rect) forKey:@"size"];
          [self.pluginLayer.pluginScrollView.HTMLNodes setObject:currentDomInfo forKey:domId];
        }
      }

      self.pluginLayer.isSuspended = false;
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
     NSArray *keys=[viewPlugins allKeys];
     NSString *mapId;
     PluginMap *pluginMap;

     for (int i = 0; i < [keys count]; i++) {
     mapId = [keys objectAtIndex:i];
     pluginMap = [viewPlugins objectForKey:mapId];
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
