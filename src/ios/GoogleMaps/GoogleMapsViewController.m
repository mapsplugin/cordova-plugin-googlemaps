//
//  GoogleMapsViewController.m
//  SimpleMap
//
//  Created by masashi on 11/6/13.
//
//

#import "GoogleMapsViewController.h"
#import <Cordova/CDVJSON.h>


@implementation GoogleMapsViewController
NSDictionary *initOptions;

- (id)initWithOptions:(NSDictionary *) options {
    self = [super init];
    initOptions = [[NSDictionary alloc] initWithDictionary:options];
    self.plugins = [NSMutableDictionary dictionary];
    self.isFullScreen = NO;
    self.embedRect = nil;
    self.screenSize = [[UIScreen mainScreen] bounds];

    return self;
}

- (void)loadView {
  [super loadView];
  [self updateMapViewLayout:NO];
  
}
- (void)updateMapViewLayout:(BOOL) animated {
  
  if (self.isFullScreen == NO) {
    if (animated == NO) {
      self.view.hidden = YES;
      [self.view setFrameWithDictionary:self.embedRect];
      self.view.hidden = NO;
    } else {
      [UIView animateWithDuration:0.5f animations:^{
        [self.view setFrameWithDictionary:self.embedRect];
      }];
    }
  }
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.view.backgroundColor = [[NSArray arrayWithObjects:[NSNumber numberWithInt:128],
                                                            [NSNumber numberWithInt:128],
                                                            [NSNumber numberWithInt:128],
                                                            [NSNumber numberWithInt:128], nil] parsePluginColor];
  
    //------------
    // Initialize
    //------------
    self.overlayManager = [NSMutableDictionary dictionary];
  
    //------------------
    // Create a map view
    //------------------
    NSString *APIKey = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"Google Maps API Key"];
    [GMSServices provideAPIKey:APIKey];
  
    //Intial camera position
    NSDictionary *cameraOpts = [initOptions objectForKey:@"camera"];
    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lng"];
    
    if (cameraOpts) {
      NSDictionary *latLngJSON = [cameraOpts objectForKey:@"latLng"];
      [latLng setObject:[NSNumber numberWithFloat:[[latLngJSON valueForKey:@"lat"] floatValue]] forKey:@"lat"];
      [latLng setObject:[NSNumber numberWithFloat:[[latLngJSON valueForKey:@"lng"] floatValue]] forKey:@"lng"];
    }
    GMSCameraPosition *camera = [GMSCameraPosition
                                  cameraWithLatitude: [[latLng valueForKey:@"lat"] floatValue]
                                  longitude: [[latLng valueForKey:@"lng"] floatValue]
                                  zoom: [[cameraOpts valueForKey:@"zoom"] floatValue]
                                  bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                  viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];
  
    CGRect pluginRect = self.view.frame;
    int marginBottom = 0;
    if ([PluginUtil isIOS7] == false) {
      marginBottom = 20;
    }
    CGRect mapRect = CGRectMake(0, 0, pluginRect.size.width, pluginRect.size.height  - marginBottom);
    self.map = [GMSMapView mapWithFrame:mapRect camera:camera];
    self.map.delegate = self;
    //self.map.autoresizingMask = UIViewAutoresizingNone;
    self.map.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  
  
    Boolean isEnabled = false;
    //controls
    NSDictionary *controls = [initOptions objectForKey:@"controls"];
    if (controls) {
      //compass
      if ([controls valueForKey:@"compass"]) {
        isEnabled = [[controls valueForKey:@"compass"] boolValue];
        self.map.settings.compassButton = isEnabled;
      }
      //myLocationButton
      if ([controls valueForKey:@"myLocationButton"]) {
        isEnabled = [[controls valueForKey:@"myLocationButton"] boolValue];
        self.map.settings.myLocationButton = isEnabled;
        self.map.myLocationEnabled = isEnabled;
      }
      //indoorPicker
      if ([controls valueForKey:@"indoorPicker"]) {
        isEnabled = [[controls valueForKey:@"indoorPicker"] boolValue];
        self.map.settings.indoorPicker = isEnabled;
        self.map.indoorEnabled = isEnabled;
      }
    } else {
      self.map.settings.compassButton = TRUE;
    }

  
    //gestures
    NSDictionary *gestures = [initOptions objectForKey:@"gestures"];
    if (gestures) {
      //rotate
      if ([gestures valueForKey:@"rotate"]) {
        isEnabled = [[gestures valueForKey:@"rotate"] boolValue];
        self.map.settings.rotateGestures = isEnabled;
      }
      //scroll
      if ([gestures valueForKey:@"scroll"]) {
        isEnabled = [[gestures valueForKey:@"scroll"] boolValue];
        self.map.settings.scrollGestures = isEnabled;
      }
      //tilt
      if ([gestures valueForKey:@"tilt"]) {
        isEnabled = [[gestures valueForKey:@"tilt"] boolValue];
        self.map.settings.tiltGestures = isEnabled;
      }
      //zoom
      if ([gestures valueForKey:@"zoom"]) {
        isEnabled = [[gestures valueForKey:@"zoom"] boolValue];
        self.map.settings.zoomGestures = isEnabled;
      }
    }
  
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
        self.map.mapType = mapType;
      }
    }
  
  
    [self.view addSubview: self.map];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

/**
 * Called when the My Location button is tapped.
 *
 * @return YES if the listener has consumed the event (i.e., the default behavior should not occur),
 *         NO otherwise (i.e., the default behavior should occur). The default behavior is for the
 *         camera to move such that it is centered on the user location.
 */
- (BOOL)didTapMyLocationButtonForMapView:(GMSMapView *)mapView {

  [self.webView stringByEvaluatingJavaScriptFromString:@"plugin.google.maps.Map._onMapEvent('my_location_button_click');"];

  return NO;
}

#pragma mark - GMSMapViewDelegate

/**
 * @callback the my location button is clicked.
 */
- (void)mapView:(GMSMapView *)mapView didTapAtCoordinate:(CLLocationCoordinate2D)coordinate {
  [self triggerMapEvent:@"click" coordinate:coordinate];
}
/**
 * @callback map long_click
 */
- (void) mapView:(GMSMapView *)mapView didLongPressAtCoordinate:(CLLocationCoordinate2D)coordinate {
  [self triggerMapEvent:@"long_click" coordinate:coordinate];
}

/**
 * @callback map will_move
 */
- (void) mapView:(GMSMapView *)mapView willMove:(BOOL)gesture
{
  dispatch_queue_t gueue = dispatch_queue_create("plugin.google.maps.Map._onMapEvent", NULL);
  dispatch_sync(gueue, ^{
  
    NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onMapEvent('will_move', %hhd);", gesture];
    [self.webView stringByEvaluatingJavaScriptFromString:jsString];
  });
}


/**
 * @callback map camera_change
 */
- (void)mapView:(GMSMapView *)mapView didChangeCameraPosition:(GMSCameraPosition *)position {
  [self triggerCameraEvent:@"camera_change" position:position];
}
/**
 * @callback map camera_idle
 */
- (void) mapView:(GMSMapView *)mapView idleAtCameraPosition:(GMSCameraPosition *)position
{
  [self triggerCameraEvent:@"camera_idle" position:position];
}


/**
 * @callback marker info_click
 */
- (void) mapView:(GMSMapView *)mapView didTapInfoWindowOfMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"info_click" marker:marker];
}
/**
 * @callback marker drag_start
 */
- (void) mapView:(GMSMapView *) mapView didBeginDraggingMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"drag_start" marker:marker];
}
/**
 * @callback marker drag_end
 */
- (void) mapView:(GMSMapView *) mapView didEndDraggingMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"drag_end" marker:marker];
}
/**
 * @callback marker drag
 */
- (void) mapView:(GMSMapView *) mapView didDragMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"drag" marker:marker];
}

/**
 * @callback marker click
 */
- (BOOL)mapView:(GMSMapView *)mapView didTapMarker:(GMSMarker *)marker {
  [self triggerMarkerEvent:@"click" marker:marker];

	return NO;
}

- (void)mapView:(GMSMapView *)mapView didTapOverlay:(GMSOverlay *)overlay {
    NSString *overlayClass = NSStringFromClass([overlay class]);
    if([overlayClass isEqualToString:@"GMSPolygon"]) {
        [self triggerOverlayEvent:@"overlay_click" type:@"polygon" overlay:overlay];
    }
    if([overlayClass isEqualToString:@"GMSPolyline"]) {
        [self triggerOverlayEvent:@"overlay_click" type:@"polyline" overlay:overlay];
    }
    if([overlayClass isEqualToString:@"GMSCircle"]) {
        [self triggerOverlayEvent:@"overlay_click" type:@"circle" overlay:overlay];
    }
    if([overlayClass isEqualToString:@"GMSGroundOverlay"]) {
        [self triggerOverlayEvent:@"overlay_click" type:@"groundOverlay" overlay:overlay];
    }
}

/**
 * Involve App._onMapEvent
 */
- (void)triggerMapEvent: (NSString *)eventName coordinate:(CLLocationCoordinate2D)coordinate
{
  NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onMapEvent('%@', new window.plugin.google.maps.LatLng(%f,%f));",
                                      eventName, coordinate.latitude, coordinate.longitude];
  [self.webView stringByEvaluatingJavaScriptFromString:jsString];
}
/**
 * Involve App._onCameraEvent
 */
- (void)triggerCameraEvent: (NSString *)eventName position:(GMSCameraPosition *)position
{

  NSMutableDictionary *target = [NSMutableDictionary dictionary];
  [target setObject:[NSNumber numberWithDouble:position.target.latitude] forKey:@"lat"];
  [target setObject:[NSNumber numberWithDouble:position.target.longitude] forKey:@"lng"];

  NSMutableDictionary *json = [NSMutableDictionary dictionary];
  [json setObject:[NSNumber numberWithFloat:position.bearing] forKey:@"bearing"];
  [json setObject:target forKey:@"target"];
  [json setObject:[NSNumber numberWithDouble:position.viewingAngle] forKey:@"tilt"];
  [json setObject:[NSNumber numberWithInt:position.hash] forKey:@"hashCode"];
  
  
  NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onCameraEvent('%@', %@);", eventName, [json JSONString]];
  [self.webView stringByEvaluatingJavaScriptFromString:jsString];
}


/**
 * Involve App._onMarkerEvent
 */
- (void)triggerMarkerEvent: (NSString *)eventName marker:(GMSMarker *)marker
{
  NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onMarkerEvent('%@', 'marker_%lu');",
                                      eventName, (unsigned long)marker.hash];
  [self.webView stringByEvaluatingJavaScriptFromString:jsString];
}

/**
 * Involve App._onOverlayEvent
 */
- (void)triggerOverlayEvent: (NSString *)eventName type:(NSString *) type overlay:(GMSOverlay *)overlay
{
    NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onOverlayEvent('%@', '%@_%lu');",
                          eventName, type, (unsigned long)overlay.hash];
    [self.webView stringByEvaluatingJavaScriptFromString:jsString];
}

//future support: custom info window
-(UIView *)mapView:(GMSMapView *)mapView markerInfoWindow:(GMSMarker*)marker
{
  CGSize rectSize;
  CGSize textSize;
  CGSize snippetSize;
  UIFont *titleFont;
  UIFont *snippetFont;
  UIImage *base64Image;
  
  Boolean isTextMode = false;
  NSString *title = marker.title;
  NSString *snippet = marker.snippet;
  
  if (title == nil) {
    return NULL;
  }
  
  // Load images
  UIImage *leftImg = [self loadImageFromGoogleMap:@"bubble_left"];
  UIImage *rightImg = [self loadImageFromGoogleMap:@"bubble_right"];
  
  //-------------------------------------
  // Calculate the size for the contents
  //-------------------------------------
  if ([title rangeOfString:@"data:image/"].location != NSNotFound &&
      [title rangeOfString:@";base64,"].location != NSNotFound) {
    
    isTextMode = false;
    NSArray *tmp = [title componentsSeparatedByString:@","];
    NSData *decodedData;
    if ([PluginUtil isIOS7]) {
      decodedData = [[NSData alloc] initWithBase64EncodedString:tmp[1] options:0];
    } else {
      decodedData = [NSData dataFromBase64String:tmp[1]];
    }
    
    base64Image = [[UIImage alloc] initWithData:decodedData];
    rectSize = CGSizeMake(base64Image.size.width + leftImg.size.width, base64Image.size.height + leftImg.size.height / 2);
    
  } else {
  
    if ([title rangeOfString:@"\n"].location == NSNotFound) {
      return NULL;
    }
    isTextMode = true;
    
    // Calculate the size for the title strings
    titleFont = [UIFont systemFontOfSize:17.0f];
    textSize = [title sizeWithFont:titleFont constrainedToSize: CGSizeMake(mapView.frame.size.width - 20, mapView.frame.size.height - 20)];
    rectSize = CGSizeMake(textSize.width, textSize.height);
    //rectSize.width += leftImg.size.width;
    rectSize.height += 16;
    
    // Calculate the size for the snippet strings
    if (snippet) {
      snippetFont = [UIFont systemFontOfSize:12.0f];
      snippet = [snippet stringByReplacingOccurrencesOfString:@"\n" withString:@""];
      snippetSize = [snippet sizeWithFont:snippetFont constrainedToSize: CGSizeMake(mapView.frame.size.width - 20, mapView.frame.size.height - 20)];
      rectSize.height += snippetSize.height + 2;
      if (rectSize.width < snippetSize.width + leftImg.size.width) {
        rectSize.width = snippetSize.width + leftImg.size.width;
      }
    }
  }
  
  //-------------------------------------
  // Drawing
  //-------------------------------------
  UIGraphicsBeginImageContextWithOptions(rectSize, NO, 0.0f);
  
  // Draw the upper side
  CGRect trimArea = CGRectMake(15, 0, 5, 45);
  if (leftImg.scale > 1.0f) {
    trimArea = CGRectMake(trimArea.origin.x * leftImg.scale,
                      trimArea.origin.y * leftImg.scale,
                      trimArea.size.width * leftImg.scale,
                      trimArea.size.height * leftImg.scale);
  }
  CGImageRef trimmedImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
  UIImage *trimmedImage = [UIImage imageWithCGImage:trimmedImageRef scale:leftImg.scale orientation:leftImg.imageOrientation];
  
  // Draw the body
  int x = 0;
  int width = x;
  while (rectSize.width - x > 5) {
    [trimmedImage drawAtPoint:CGPointMake(x, 0)];
    x += 5;
  }
  width = x;
  [leftImg drawAtPoint:CGPointMake(rectSize.width * 0.5f - leftImg.size.width, rectSize.height - leftImg.size.height)];
  [rightImg drawAtPoint:CGPointMake(rectSize.width * 0.5f, rectSize.height - rightImg.size.height)];
  
  // Draw the bottom side
  trimArea = CGRectMake(15, 45, 5, 10);
  if (leftImg.scale > 1.0f) {
    trimArea = CGRectMake(trimArea.origin.x * leftImg.scale,
                      trimArea.origin.y * leftImg.scale,
                      trimArea.size.width * leftImg.scale,
                      trimArea.size.height * leftImg.scale);
  }
  trimmedImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
  trimmedImage = [UIImage imageWithCGImage:trimmedImageRef scale:leftImg.scale orientation:leftImg.imageOrientation];
  
  x = 0;
  while (rectSize.width - x > 5) {
    if (x < rectSize.width * 0.5f - leftImg.size.width + 5 || x > rectSize.width * 0.5f + rightImg.size.width - 10) {
      [trimmedImage drawAtPoint:CGPointMake(x, rectSize.height - trimmedImage.size.height)];
    }
    x += 5;
  }
  
  // Fill the body area with WHITE color
  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextSetAllowsAntialiasing(context, true);
  CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0);
  CGContextFillRect(context, CGRectMake(0, 5, width, rectSize.height - 15));
  
  //-------------------------------------
  // Draw the contents
  //-------------------------------------
  if (isTextMode) {
    //Draw the title strings
    if (title) {
      [[UIColor blackColor] set];
      [title drawInRect:CGRectMake(2, 2, rectSize.width - 2, textSize.height )
              withFont:titleFont
              lineBreakMode:NSLineBreakByWordWrapping
              alignment:NSTextAlignmentCenter];
    }
    
    //Draw the snippet
    if (snippet) {
      [[UIColor grayColor] set];
      [snippet drawInRect:CGRectMake(2, textSize.height + 2, rectSize.width - 2, snippetSize.height)
              withFont:snippetFont
              lineBreakMode:NSLineBreakByWordWrapping
              alignment:NSTextAlignmentCenter];
    }
  } else {
    //Draw the content image
    CGRect imageRect = CGRectMake((rectSize.width - base64Image.size.width) / 2 ,
                                  -1 * ((rectSize.height - base64Image.size.height - 20) / 2 + 5),
                                  base64Image.size.width, base64Image.size.height);
    CGContextTranslateCTM(context, 0, base64Image.size.height);
    CGContextScaleCTM(context, 1.0, -1.0);
    CGContextDrawImage(context, imageRect, base64Image.CGImage);
  }
  

  //-------------------------------------
  // Generate new image
  //-------------------------------------
  UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
  UIGraphicsEndImageContext();

  UIImageView *imageView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, rectSize.width, rectSize.height)];
  [imageView setContentMode:UIViewContentModeScaleAspectFill];
  [imageView setImage:image];
  return imageView;
}

-(UIImage *)loadImageFromGoogleMap:(NSString *)fileName {
  NSString *imagePath = [[NSBundle bundleWithIdentifier:@"com.google.GoogleMaps"] pathForResource:fileName ofType:@"png"];
  return [[UIImage alloc] initWithContentsOfFile:imagePath];
}





- (GMSCircle *)getCircleByKey: (NSString *)key {
  return [self.overlayManager objectForKey:key];
}

- (GMSMarker *)getMarkerByKey: (NSString *)key {
  return [self.overlayManager objectForKey:key];
}

- (GMSPolygon *)getPolygonByKey: (NSString *)key {
  return [self.overlayManager objectForKey:key];
}

- (GMSPolyline *)getPolylineByKey: (NSString *)key {
  return [self.overlayManager objectForKey:key];
}
- (GMSTileLayer *)getTileLayerByKey: (NSString *)key {
  return [self.overlayManager objectForKey:key];
}
- (GMSGroundOverlay *)getGroundOverlayByKey: (NSString *)key {
  return [self.overlayManager objectForKey:key];
}

- (void)removeObjectForKey: (NSString *)key {
  [self.overlayManager removeObjectForKey:key];
}
@end