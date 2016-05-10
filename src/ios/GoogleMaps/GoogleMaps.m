//
//  GoogleMaps.m
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import "GoogleMaps.h"

@implementation GoogleMaps

- (void)pluginInitialize
{
#if CORDOVA_VERSION_MIN_REQUIRED >= __CORDOVA_4_0_0
    self.webView.backgroundColor = [UIColor clearColor];
    self.webView.opaque = NO;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pageDidLoad) name:CDVPageDidLoadNotification object:nil];
#endif
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
    
    //-------------------------------
    // Plugin initialization
    //-------------------------------
    [GMSServices provideAPIKey:APIKey];
    self.mapPlugins = [[NSMutableDictionary alloc] init];
    
    self.licenseLayer = nil;
    
    self.locationCommandQueue = [[NSMutableArray alloc] init];


    self.pluginLayer = [[MyPluginLayer alloc] initWithWebView:(UIWebView *)self.webView];
    self.pluginLayer.backgroundColor = [UIColor whiteColor];
    self.pluginLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;


    NSArray *subViews = self.viewController.view.subviews;
    UIView *view;
    for (int i = 0; i < [subViews count]; i++) {
        view = [subViews objectAtIndex:i];
        NSLog(@"remove i=%d class=%@", i, view.class);
        [view removeFromSuperview];
        [self.pluginLayer addSubview: view];
    }
    [self.viewController.view addSubview:self.pluginLayer];

    
    //[self.mapCtrl.view removeFromSuperview];
    //self.mapCtrl.isFullScreen = NO;
    
    //[self.pluginLayer.pluginScrollView attachView:self.mapCtrl.view];
 

}

-(void)viewDidLayoutSubviews {
//TODO
    //[mapCtrl.pluginLayer.pluginScrollView setContentSize: self.webView.scrollView.contentSize];
    //[mapCtrl.pluginLayer.pluginScrollView flashScrollIndicators];
}

-(void)pageDidLoad {
    self.webView.backgroundColor = [UIColor clearColor];
    self.webView.opaque = NO;
}

/**
 * Intialize the map
 */
- (void)getMap:(CDVInvokedUrlCommand *)command {
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    NSString *mapId = [command.arguments objectAtIndex:0];
    
    // Wrapper view
    GoogleMapsViewController* mapCtrl = [[GoogleMapsViewController alloc] init];
    mapCtrl.webView = self.webView;
    mapCtrl.isFullScreen = YES;
    
    // Create an instance of the Map class everytime.
    Map *mapPlugin = [[Map alloc] init];
    [mapPlugin pluginInitialize];
    mapPlugin.mapId = mapId;
    mapPlugin.mapCtrl = mapCtrl;
  
    // Hack:
    // In order to load the plugin instance of the same class but different names,
    // register the map plugin instance into the pluginObjects directly.
    if ([mapPlugin respondsToSelector:@selector(setViewController:)]) {
        [mapPlugin setViewController:cdvViewController];
    }
    if ([mapPlugin respondsToSelector:@selector(setCommandDelegate:)]) {
        [mapPlugin setCommandDelegate:cdvViewController.commandDelegate];
    }
    [cdvViewController.pluginObjects setObject:mapPlugin forKey:mapId];
    [cdvViewController.pluginsMap setValue:mapId forKey:mapId];
    [mapPlugin pluginInitialize];
  
    [self.mapPlugins setObject:mapPlugin forKey:mapId];
  
  
    // Generate an instance of GMSMapView;
    GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:0
                                        longitude:0
                                        zoom:0
                                        bearing:0
                                        viewingAngle:0];

    CGRect pluginRect = mapCtrl.view.frame;
    int marginBottom = 0;
    //if ([PluginUtil isIOS7] == false) {
    //  marginBottom = 20;
    //}
    CGRect mapRect = CGRectMake(0, 0, pluginRect.size.width, pluginRect.size.height  - marginBottom);
    //NSLog(@"mapRect=%f,%f - %f,%f", mapRect.origin.x, mapRect.origin.y, mapRect.size.width, mapRect.size.height);
    //NSLog(@"mapRect=%@", camera);
    mapPlugin.mapCtrl.map = [GMSMapView mapWithFrame:mapRect camera:camera];

    mapPlugin.mapCtrl.map.delegate = mapCtrl;
    mapPlugin.mapCtrl.map.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  
    //indoor display
    mapPlugin.mapCtrl.map.indoorDisplay.delegate = mapCtrl;
    
    [mapCtrl.view addSubview:mapPlugin.mapCtrl.map];
    [self.pluginLayer addMapView:mapPlugin.mapId mapCtrl:mapCtrl];

  
    [mapPlugin getMap:command];
}


-(void)setVisible:(CDVInvokedUrlCommand *)command
{/*
    Boolean isVisible = [[command.arguments objectAtIndex:0] boolValue];
    if (self.mapCtrl.isFullScreen == NO) {
        if (isVisible == YES) {
            self.mapCtrl.map.hidden = NO;
        } else {
            self.mapCtrl.map.hidden = YES;
        }
    }
*/
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)exec:(CDVInvokedUrlCommand *)command {


    [self.commandDelegate runInBackground:^{

        CDVPluginResult* pluginResult = nil;
        NSString *classAndMethod = [command.arguments objectAtIndex:0];

        NSArray *target = [classAndMethod componentsSeparatedByString:@"."];
        NSString *className = [target objectAtIndex:0];
        CDVPlugin<MyPlgunProtocol> *pluginClass = nil;
        NSString *methodName;
        //if (self.mapCtrl.debuggable) {
        //    NSLog(@"(debug)%@", classAndMethod);
        //}

        if ([classAndMethod isEqualToString:@"Map.setOptions"]) {
            NSDictionary *options = [command.arguments objectAtIndex:1];
            if ([options objectForKey:@"backgroundColor"]) {
                NSArray *rgbColor = [options objectForKey:@"backgroundColor"];
                self.pluginLayer.backgroundColor = [rgbColor parsePluginColor];
            }
        }

        if ([target count] == 2) {
            methodName = [NSString stringWithFormat:@"%@:", [target objectAtIndex:1]];
/*
            pluginClass = [self.mapCtrl.plugins objectForKey:className];
            if (!pluginClass) {
#if CORDOVA_VERSION_MIN_REQUIRED >= __CORDOVA_4_0_0
                pluginClass = [(CDVViewController*)self.viewController getCommandInstance:className];
#else
                pluginClass = [[NSClassFromString(className)alloc] initWithWebView:self.webView];
#endif
                if (pluginClass) {
                    pluginClass.commandDelegate = self.commandDelegate;
                    [pluginClass setGoogleMapsViewController:self.mapCtrl];
                    [self.mapCtrl.plugins setObject:pluginClass forKey:className];
                }
            }
            if (!pluginClass) {

                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                 messageAsString:[NSString stringWithFormat:@"Class not found: %@", className]];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;

            } else {
*/
                SEL selector = NSSelectorFromString(methodName);
                if ([pluginClass respondsToSelector:selector]){
                    [pluginClass performSelectorOnMainThread:selector withObject:command waitUntilDone:YES];
                } else {

                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                     messageAsString:[NSString stringWithFormat:@"method not found: %@ in %@ class", [target objectAtIndex:1], className]];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

                }
/*
            }
*/
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:[NSString stringWithFormat:@"class not found: %@", className]];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

/**
 * Get license information
 */
-(void)getLicenseInfo:(CDVInvokedUrlCommand *)command
{
    NSString *txt = [GMSServices openSourceLicenseInfo];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:txt];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Close the map window
 */
- (void)onCloseBtn_clicked:(UIButton*)button{
    [self _removeMapView];
}

- (void)_removeMapView{
/*
    if (self.mapCtrl.isFullScreen == YES) {
        [self.mapCtrl.view removeFromSuperview];
        [self.mapCtrl.pluginScrollView dettachView];
        [self.footer removeFromSuperview];
        //[self.pluginLayer clearHTMLElement];
        //[self.mapCtrl.pluginScrollView.debugView clearHTMLElement];
        self.mapCtrl.isFullScreen = NO;
        self.mapCtrl.view.autoresizingMask = UIViewAutoresizingNone;
    }

    float width = [[self.mapCtrl.embedRect objectForKey:@"width"] floatValue];
    float height = [[self.mapCtrl.embedRect objectForKey:@"height"] floatValue];
    if (width > 0.0f && height > 0.0f) {
        //[self.webView.scrollView addSubview:self.mapCtrl.view];
        [self.mapCtrl.pluginScrollView attachView:self.mapCtrl.view];
        [self.mapCtrl updateMapViewLayout];
        return;
    }
 
    //[self.mapCtrl.view removeFromSuperview];
    //[self.footer removeFromSuperview];
    //self.mapCtrl.isFullScreen = NO;
    //self.mapCtrl.view.autoresizingMask = UIViewAutoresizingNone;
 
    //Notify to the JS
    NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onMapEvent('map_close');"];
    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
        [self.webView performSelector:@selector(stringByEvaluatingJavaScriptFromString:) withObject:jsString];
    } else if ([self.webView respondsToSelector:@selector(evaluateJavaScript:completionHandler:)]) {
        [self.webView performSelector:@selector(evaluateJavaScript:completionHandler:) withObject:jsString withObject:nil];
    }
*/
}

/**
 * Show the licenses
 */
- (void)onLicenseBtn_clicked:(UIButton*)button{
/*
    if (self.licenseLayer == nil) {
        //Create the dialog
        CGRect dialogRect = self.mapCtrl.view.frame;
        dialogRect.origin.x = dialogRect.size.width / 10;
        dialogRect.origin.y = dialogRect.size.height / 10;
        dialogRect.size.width -= dialogRect.origin.x * 2;
        dialogRect.size.height -= dialogRect.origin.y * 2;
        if ([PluginUtil isIOS7_OR_OVER] == false) {
            dialogRect.size.height -= 20;
        }

        self.licenseLayer = [[UIView alloc] initWithFrame:self.mapCtrl.view.frame];
        self.licenseLayer.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self.licenseLayer setBackgroundColor:[UIColor colorWithHue:0 saturation:0 brightness:0 alpha:0.25f]];

        UIView *licenseDialog = [[UIView alloc] initWithFrame:dialogRect];
        [licenseDialog setBackgroundColor:[UIColor whiteColor]];
        [licenseDialog.layer setBorderColor:[UIColor blackColor].CGColor];
        [licenseDialog.layer setBorderWidth:1.0];
        [licenseDialog.layer setCornerRadius:10];
        licenseDialog.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleHeight;
        [self.licenseLayer addSubview:licenseDialog];

        UIScrollView *scrollView = [[UIScrollView alloc] init];
        [scrollView setFrameWithInt:5 top:5 width:dialogRect.size.width - 10 height:dialogRect.size.height - 30];
        [scrollView.layer setBorderColor:[UIColor blackColor].CGColor];
        scrollView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [licenseDialog addSubview:scrollView];


        UIWebView *webView = [[UIWebView alloc] initWithFrame:[scrollView bounds]];
        [webView setBackgroundColor:[UIColor whiteColor]];
        webView.scalesPageToFit = NO;
        webView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

        int fontSize = 13;
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad){
            fontSize = 18;
        }
        NSMutableString *licenceTxt = [NSMutableString
                                       stringWithFormat:@"<html><body style='font-size:%dpx;white-space:pre-line'>%@</body></html>",
                                       fontSize,
                                       [GMSServices openSourceLicenseInfo]];

        [webView loadHTMLString:licenceTxt baseURL:nil];
        scrollView.contentSize = [webView bounds].size;
        [scrollView addSubview:webView];

        //close button
        UIButton *closeButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
        [closeButton setFrameWithInt:0 top:dialogRect.size.height - 30 width:dialogRect.size.width height:30];
        closeButton.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewContentModeTopLeft;
        [closeButton setTitle:@"Close" forState:UIControlStateNormal];
        [closeButton addTarget:self action:@selector(onLicenseCloseBtn_clicked:) forControlEvents:UIControlEventTouchDown];
        [licenseDialog addSubview:closeButton];
    }

    [self.mapCtrl.view addSubview:self.licenseLayer];
*/
}

/**
 * Close the map window
 */
- (void)onLicenseCloseBtn_clicked:(UIButton*)button{
    [self.licenseLayer removeFromSuperview];
}

- (void)setDiv:(CDVInvokedUrlCommand *)command {
/*
    if ([command.arguments count] == 2) {
        //[self.mapCtrl.view removeFromSuperview];
        [self.mapCtrl.pluginScrollView dettachView];
        [self.pluginLayer clearHTMLElement];
        [self.mapCtrl.pluginScrollView.debugView clearHTMLElement];
        self.mapCtrl.isFullScreen = NO;
        self.pluginLayer.mapCtrl = self.mapCtrl;
        self.pluginLayer.webView = self.webView;

        [self.mapCtrl.pluginScrollView attachView:self.mapCtrl.view];
        [self resizeMap:command];
    } else {
        //[self.mapCtrl.view removeFromSuperview];
        [self.mapCtrl.pluginScrollView dettachView];
    }
*/
}


- (void)resizeMap:(CDVInvokedUrlCommand *)command {
/*
    NSInteger argCnt = [command.arguments count];
    self.mapCtrl.embedRect = [command.arguments objectAtIndex:(argCnt - 2)];
    self.pluginLayer.embedRect = self.mapCtrl.embedRect;
    self.mapCtrl.pluginScrollView.debugView.embedRect = self.mapCtrl.embedRect;
    [self.pluginLayer clearHTMLElement];
    [self.mapCtrl.pluginScrollView.debugView clearHTMLElement];

    NSArray *HTMLs = [command.arguments objectAtIndex:(argCnt - 1)];
    NSString *elemId;
    NSDictionary *elemSize, *elemInfo;
    for (int i = 0; i < [HTMLs count]; i++) {
        elemInfo = [HTMLs objectAtIndex:i];
        elemSize = [elemInfo objectForKey:@"size"];
        elemId = [elemInfo objectForKey:@"id"];
        [self.pluginLayer putHTMLElement:elemId size:elemSize];
        [self.mapCtrl.pluginScrollView.debugView putHTMLElement:elemId size:elemSize];
    }

    [self.mapCtrl updateMapViewLayout];
*/
}


/**
 * Show the map window
 */
- (void)showDialog:(CDVInvokedUrlCommand *)command {
/*
    if (self.mapCtrl.isFullScreen == YES) {
        return;
    }
    
    dispatch_queue_t gueue = dispatch_queue_create("plugins.google.maps.showDialog", NULL);
    dispatch_async(gueue, ^{
        if (self.footer == nil) {
            // Create the footer background
            self.footer = [[UIView alloc]init];
            self.footer.backgroundColor = [UIColor lightGrayColor];

            self.footer.autoresizingMask = UIViewAutoresizingFlexibleTopMargin |
            UIViewAutoresizingFlexibleWidth;

            // Create the close button
            self.closeButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
            CGRect frame = [self.closeButton frame];
            frame.origin.x = 10;
            frame.origin.y = 0;
            frame.size.width = 50;
            frame.size.height = 40;
            [self.closeButton setFrame:frame];
            self.closeButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin |
            UIViewAutoresizingFlexibleHeight;
            [self.closeButton setTitle:@"Close" forState:UIControlStateNormal];
            [self.closeButton addTarget:self action:@selector(onCloseBtn_clicked:) forControlEvents:UIControlEventTouchUpInside];
            [self.footer addSubview:self.closeButton];

            // Create the legal notices button
            self.licenseButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
            self.licenseButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin |
            UIViewAutoresizingFlexibleLeftMargin |
            UIViewAutoresizingFlexibleHeight;
            [self.licenseButton setTitle:@"Legal Notices" forState:UIControlStateNormal];
            [self.licenseButton addTarget:self action:@selector(onLicenseBtn_clicked:) forControlEvents:UIControlEventTouchUpInside];
            [self.footer addSubview:self.licenseButton];
        }

        // remove the map view from the parent
        dispatch_sync(dispatch_get_main_queue(), ^{
            float width = [[self.mapCtrl.embedRect objectForKey:@"width"] floatValue];
            float height = [[self.mapCtrl.embedRect objectForKey:@"height"] floatValue];
            if (width > 0.0f && height > 0.0f) {
                [self.mapCtrl.view removeFromSuperview];
            }

            self.mapCtrl.isFullScreen = YES;

            self.mapCtrl.view.autoresizingMask =  UIViewAutoresizingFlexibleWidth |
                                                  UIViewAutoresizingFlexibleHeight |
                                                  UIViewAutoresizingFlexibleLeftMargin |
                                                  UIViewAutoresizingFlexibleRightMargin |
                                                  UIViewAutoresizingFlexibleBottomMargin;
            
            
            int footerHeight = 40;
            int footerAdjustment = 0;
            if ([PluginUtil isIOS7_OR_OVER] == false) {
                footerAdjustment = 20;
            }
            
            
            // Calculate the full screen size
            CGRect screenSize = self.mapCtrl.screenSize;
            CGRect pluginRect = self.mapCtrl.view.frame;
            pluginRect.origin.x = 0;
            pluginRect.origin.y = 0;
            int direction;
            
#if !defined(__IPHONE_8_0)
            // iOS 7
            direction = self.mapCtrl.interfaceOrientation;
#else
            // iOS8 or above
            direction = [UIDevice currentDevice].orientation;
#endif
            
            
            if (direction == UIInterfaceOrientationLandscapeLeft ||
                direction == UIInterfaceOrientationLandscapeRight) {
                pluginRect.size.width = screenSize.size.height;
                pluginRect.size.height = screenSize.size.width - footerHeight - footerAdjustment;
            } else {
                pluginRect.size.width = screenSize.size.width;
                pluginRect.size.height = screenSize.size.height - footerHeight - footerAdjustment;
            }
            [self.mapCtrl.view setFrame:pluginRect];
            
            //self.mapCtrl.view.frame = pluginRect;
            [self.footer setFrameWithInt:0 top:pluginRect.size.height width:pluginRect.size.width height:footerHeight];
            [self.licenseButton setFrameWithInt:pluginRect.size.width - 110 top:0 width:100 height:footerHeight];
            [self.closeButton setFrameWithInt:10 top:0 width:50 height:footerHeight];
            
            // Show the map
            [self.webView addSubview:self.mapCtrl.view];
            
            // Add the footer
            [self.webView addSubview:self.footer];
            
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            
        });
        
    });
*/
}

/**
 * Show the map window
 */
- (void)closeDialog:(CDVInvokedUrlCommand *)command {
/*
    [self _removeMapView];
    self.mapCtrl.isFullScreen = NO;

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
*/
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
#ifdef __IPHONE_8_0
        if ([PluginUtil isIOS8_OR_OVER]) {
            // iOS8
            [self.locationManager requestWhenInUseAuthorization];
        }
#endif
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

/**
 * Return always success
 */
-(void)isAvailable:(CDVInvokedUrlCommand *)command
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Clear all markups
 */
- (void)clear:(CDVInvokedUrlCommand *)command {
/*
    [self.mapCtrl.overlayManager removeAllObjects];
    [self.mapCtrl.map clear];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
*/
}

- (void)pluginLayer_pushHtmlElement:(CDVInvokedUrlCommand *)command {
    NSString *mapId = [command.arguments objectAtIndex:0];
    NSString *domId = [command.arguments objectAtIndex:1];
    NSDictionary *size = [command.arguments objectAtIndex:2];
    [self.pluginLayer putHTMLElement:mapId domId:domId size:size];
    //[self.mapCtrl.pluginScrollView.debugView putHTMLElement:domId size:size];
}

- (void)pluginLayer_removeHtmlElement:(CDVInvokedUrlCommand *)command {
    NSString *mapId = [command.arguments objectAtIndex:0];
    NSString *domId = [command.arguments objectAtIndex:1];
    [self.pluginLayer removeHTMLElement:mapId domId:domId];
    //[self.mapCtrl.pluginScrollView.debugView removeHTMLElement:domId];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


-(void)pluginLayer_setDebuggable:(CDVInvokedUrlCommand *)command
{
/*
    Boolean isDebuggable = [[command.arguments objectAtIndex:0] boolValue];
    self.pluginLayer.debuggable = isDebuggable;
    self.pluginLayer.pluginScrollView.debugView.debuggable = isDebuggable;
    self.mapCtrl.debuggable = isDebuggable;
    [self.pluginLayer.pluginScrollView.debugView setNeedsDisplay];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
*/
}


-(void)pluginLayer_setClickable:(CDVInvokedUrlCommand *)command
{
    Boolean isClickable = [[command.arguments objectAtIndex:0] boolValue];
    self.pluginLayer.clickable = isClickable;
    self.pluginLayer.pluginScrollView.debugView.clickable = isClickable;
    [self.pluginLayer.pluginScrollView.debugView setNeedsDisplay];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set background color
 * @params key
 */
-(void)pluginLayer_setBackGroundColor:(CDVInvokedUrlCommand *)command
{
    NSArray *rgbColor = [command.arguments objectAtIndex:0];
    self.pluginLayer.backgroundColor = [rgbColor parsePluginColor];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the map
 */
- (void)remove:(CDVInvokedUrlCommand *)command {
/*
    NSString *mapId = [command.arguments objectAtIndex:0];
    [self.pluginLayer clearHTMLElement:mapId];
    [self.mapCtrl.pluginScrollView.debugView clearHTMLElement];
    [self.mapCtrl.overlayManager removeAllObjects];
    [self.mapCtrl.map clear];
    [self.mapCtrl.map removeFromSuperview];
    [self.mapCtrl.view removeFromSuperview];
    self.mapCtrl.map = nil;
    self.mapCtrl = nil;
    self.licenseLayer = nil;
    self.footer = nil;
    self.closeButton = nil;
    self.locationManager = nil;
    //self.locationCommandQueue = nil;  // issue 437


    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
*/
}
@end
