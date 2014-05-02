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
  self.licenseLayer = nil;
  self.mapCtrl.isFullScreen = YES;
}

/**
 * Intialize the map
 */
- (void)getMap:(CDVInvokedUrlCommand *)command {
NSLog(@"action=getMap");

  NSString *APIKey = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"Google Maps API Key"];
  if ([APIKey isEqualToString:@"API_KEY_FOR_IOS"]) {
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
  
  if (!self.mapCtrl) {
    dispatch_queue_t gueue = dispatch_queue_create("plugins.google.maps.init", NULL);
    
    // Create a map view
    dispatch_async(gueue, ^{
      NSDictionary *options = [command.arguments objectAtIndex:0];
      self.mapCtrl = [[GoogleMapsViewController alloc] initWithOptions:options];
      self.mapCtrl.webView = self.webView;
    });
    
    // Create an instance of Map Class
    dispatch_async(gueue, ^{
      Map *mapClass = [[NSClassFromString(@"Map")alloc] initWithWebView:self.webView];
      mapClass.commandDelegate = self.commandDelegate;
      [mapClass setGoogleMapsViewController:self.mapCtrl];
      [self.mapCtrl.plugins setObject:mapClass forKey:@"Map"];
      
      
      dispatch_sync(dispatch_get_main_queue(), ^{
      
        if ([command.arguments count] == 2) {
          self.mapCtrl.isFullScreen = NO;
          NSDictionary* divSize = [command.arguments objectAtIndex:1];
          float left = [[divSize valueForKeyPath:@"left"] floatValue];
          float top = [[divSize valueForKeyPath:@"top"] floatValue];
          float width = [[divSize valueForKeyPath:@"width"] floatValue];
          float height = [[divSize valueForKeyPath:@"height"] floatValue];
          
          
          self.mapCtrl.embedRect = CGRectMake(left, top, width, height);
          [self.mapCtrl.view setFrame:self.mapCtrl.embedRect];
          self.mapCtrl.view.autoresizingMask = UIViewAutoresizingNone;
          [self.webView.scrollView addSubview:self.mapCtrl.view];
        }
        

      });
    });
    
    // Create the dialog footer
    dispatch_async(gueue, ^{
      dispatch_sync(dispatch_get_main_queue(), ^{
        
        // Create the footer background
        self.footer = [[UIView alloc]init];
        self.footer.backgroundColor = [UIColor lightGrayColor];
        
        self.footer.autoresizingMask = UIViewAutoresizingFlexibleTopMargin |
                                  UIViewAutoresizingFlexibleWidth;
        
        // Create the close button
        self.closeButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
        self.closeButton.frame = CGRectMake(10, 0, 50, 40);
        self.closeButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin;
        [self.closeButton setTitle:@"Close" forState:UIControlStateNormal];
        [self.closeButton addTarget:self action:@selector(onCloseBtn_clicked:) forControlEvents:UIControlEventTouchUpInside];
        [self.footer addSubview:self.closeButton];
      
        // Create the legal notices button
        self.licenseButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
        self.licenseButton.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleLeftMargin;
        [self.licenseButton setTitle:@"Legal Notices" forState:UIControlStateNormal];
        [self.licenseButton addTarget:self action:@selector(onLicenseBtn_clicked:) forControlEvents:UIControlEventTouchUpInside];
        [self.footer addSubview:self.licenseButton];
        
    
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });
    });
  }
}



- (void)exec:(CDVInvokedUrlCommand *)command {
  
  [self.commandDelegate runInBackground:^{
    
    CDVPluginResult* pluginResult = nil;
    NSString *classAndMethod = [command.arguments objectAtIndex:0];
    NSLog(@"action=%@", classAndMethod);
    
    NSArray *target = [classAndMethod componentsSeparatedByString:@"."];
    NSString *className = [target objectAtIndex:0];
    CDVPlugin<MyPlgunProtocol> *pluginClass = nil;
    NSString *methodName;
    
    if ([target count] == 2) {
      methodName = [NSString stringWithFormat:@"%@:", [target objectAtIndex:1]];
      
      pluginClass = [self.mapCtrl.plugins objectForKey:className];
      if (!pluginClass) {
        pluginClass = [[NSClassFromString(className)alloc] initWithWebView:self.webView];
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
  [self.mapCtrl.view removeFromSuperview];
  [self.footer removeFromSuperview];
  self.mapCtrl.isFullScreen = NO;
  self.mapCtrl.view.autoresizingMask = UIViewAutoresizingNone;
  
  if (self.mapCtrl.embedRect.size.width != 0 &&
      self.mapCtrl.embedRect.size.height != 0) {
    [self.webView.scrollView addSubview:self.mapCtrl.view];
    [self.mapCtrl updateMapViewLayout];
  }
  
  //Notify to the JS
  NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onMapEvent('map_close');"];
  [self.webView stringByEvaluatingJavaScriptFromString:jsString];
}

/**
 * Show the licenses
 */
- (void)onLicenseBtn_clicked:(UIButton*)button{

  if (self.licenseLayer == nil) {
    //Create the dialog
    CGRect dialogRect = self.mapCtrl.view.frame;
    dialogRect.origin.x = dialogRect.size.width / 10;
    dialogRect.origin.y = dialogRect.size.height / 10;
    dialogRect.size.width -= dialogRect.origin.x * 2;
    dialogRect.size.height -= dialogRect.origin.y * 2;
    if ([PluginUtil isIOS7] == false) {
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

    CGRect scrollViewRect = CGRectMake(5, 5, dialogRect.size.width - 10, dialogRect.size.height - 30);
    UIScrollView *scrollView = [[UIScrollView alloc] initWithFrame: scrollViewRect];
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
    closeButton.frame = CGRectMake(0, dialogRect.size.height - 30, dialogRect.size.width, 30);
    closeButton.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewContentModeTopLeft;
    [closeButton setTitle:@"Close" forState:UIControlStateNormal];
    [closeButton addTarget:self action:@selector(onLicenseCloseBtn_clicked:) forControlEvents:UIControlEventTouchDown];
    [licenseDialog addSubview:closeButton];
  }
  
  [self.mapCtrl.view addSubview:self.licenseLayer];
}

/**
 * Close the map window
 */
- (void)onLicenseCloseBtn_clicked:(UIButton*)button{
  [self.licenseLayer removeFromSuperview];
}

- (void)setDiv:(CDVInvokedUrlCommand *)command {
  if (self.mapCtrl.embedRect.size.width > 0 &&
      self.mapCtrl.embedRect.size.height > 0) {
    [self.mapCtrl.view removeFromSuperview];
  }
  
  if ([command.arguments count] == 1) {
    [self.webView.scrollView addSubview:self.mapCtrl.view];
    [self resizeMap:command];
  }
}


- (void)resizeMap:(CDVInvokedUrlCommand *)command {
NSLog(@"action=resizeMap");

  dispatch_queue_t gueue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0);
  dispatch_sync(gueue, ^{

    NSDictionary* divSize = [command.arguments objectAtIndex:0];
    float left = [[divSize valueForKeyPath:@"left"] floatValue];
    float top = [[divSize valueForKeyPath:@"top"] floatValue];
    float width = [[divSize valueForKeyPath:@"width"] floatValue];
    float height = [[divSize valueForKeyPath:@"height"] floatValue];

    self.mapCtrl.embedRect = CGRectMake(left, top, width, height);
    [self.mapCtrl updateMapViewLayout];
  });
}


/**
 * Show the map window
 */
- (void)showDialog:(CDVInvokedUrlCommand *)command {
  if (self.mapCtrl.isFullScreen == YES) {
    return;
  }
  
  // remove the map view from the parent
  if (self.mapCtrl.embedRect.size.width != 0 &&
      self.mapCtrl.embedRect.size.height != 0) {
    [self.mapCtrl.view removeFromSuperview];
  }
  
  self.mapCtrl.isFullScreen = YES;
  
  self.mapCtrl.view.autoresizingMask =  UIViewAutoresizingFlexibleWidth |
                                        UIViewAutoresizingFlexibleHeight |
                                        UIViewAutoresizingFlexibleLeftMargin |
                                        UIViewAutoresizingFlexibleRightMargin |
                                        UIViewAutoresizingFlexibleBottomMargin;
  
  dispatch_queue_t gueue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0);
  dispatch_sync(gueue, ^{
  
    int footerHeight = 40;
  
    // Calculate the full screen size
    CGRect pluginRect;
    CGRect screenSize = self.mapCtrl.screenSize;
    int direction = self.mapCtrl.interfaceOrientation;
    if (direction == UIInterfaceOrientationLandscapeLeft ||
      direction == UIInterfaceOrientationLandscapeRight) {
      pluginRect = CGRectMake(0, 0, screenSize.size.height, screenSize.size.width - footerHeight);
    } else {
      pluginRect = CGRectMake(0, 0, screenSize.size.width, screenSize.size.height - footerHeight);
    }
    self.mapCtrl.view.frame = pluginRect;
    
    //self.mapCtrl.view.frame = pluginRect;
    self.footer.frame = CGRectMake(0, pluginRect.size.height, pluginRect.size.width, footerHeight);
    self.licenseButton.frame = CGRectMake(pluginRect.size.width - 110, 0, 100, footerHeight);
    self.closeButton.frame = CGRectMake(10, 0, 50, footerHeight);
    
    // Add the footer
    [self.webView addSubview:self.footer];
    
    // Show the map
    [self.webView addSubview:self.mapCtrl.view];
    
    [self.mapCtrl updateMapViewLayout];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  });
}

/**
 * Show the map window
 */
- (void)closeDialog:(CDVInvokedUrlCommand *)command {
  [self _removeMapView];
  self.mapCtrl.isFullScreen = NO;
  
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
  
  NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
  [latLng setObject:[NSNumber numberWithFloat:locationManager.location.coordinate.latitude] forKey:@"lat"];
  [latLng setObject:[NSNumber numberWithFloat:locationManager.location.coordinate.longitude] forKey:@"lng"];

  NSMutableDictionary *json = [NSMutableDictionary dictionary];
  [json setObject:latLng forKey:@"latLng"];
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
