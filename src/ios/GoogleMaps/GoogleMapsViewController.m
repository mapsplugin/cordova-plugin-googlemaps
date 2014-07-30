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
  if ([overlayClass isEqualToString:@"GMSPolygon"] ||
      [overlayClass isEqualToString:@"GMSPolyline"] ||
      [overlayClass isEqualToString:@"GMSCircle"] ||
      [overlayClass isEqualToString:@"GMSGroundOverlay"]) {
    [self triggerOverlayEvent:@"overlay_click" id:overlay.title];
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
  [json setObject:[NSNumber numberWithFloat:position.zoom] forKey:@"zoom"];
  
  
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
- (void)triggerOverlayEvent: (NSString *)eventName id:(NSString *) id
{
  NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onOverlayEvent('%@', '%@');",
                                      eventName, id];
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
  
  // Load styles
  NSString *marker_style_id = [NSString stringWithFormat:@"marker_style_%lu", (unsigned long)marker.hash];
  NSDictionary *styles = [self.overlayManager objectForKey:marker_style_id];
  
  // Load images
  UIImage *leftImg = nil;
  UIImage *rightImg = nil;[self loadImageFromGoogleMap:@"bubble_right@2x"];
  leftImg = [self loadImageFromGoogleMap:@"bubble_left@2x"];
  rightImg = [self loadImageFromGoogleMap:@"bubble_right@2x"];
  float scale = leftImg.scale;
  int sizeEdgeWidth = 10;
  
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
  
    isTextMode = true;
    
    BOOL isBold = FALSE;
    BOOL isItalic = FALSE;
    if (styles) {
      if ([[styles objectForKey:@"font-style"] isEqualToString:@"italic"]) {
        isItalic = TRUE;
      }
      if ([[styles objectForKey:@"font-weight"] isEqualToString:@"bold"]) {
        isBold = TRUE;
      }
    }
    if (isBold == TRUE && isItalic == TRUE) {
      if ([PluginUtil isIOS7] == true) {
        // ref: http://stackoverflow.com/questions/4713236/how-do-i-set-bold-and-italic-on-uilabel-of-iphone-ipad#21777132
        titleFont = [UIFont systemFontOfSize:17.0f];
        UIFontDescriptor *fontDescriptor = [titleFont.fontDescriptor
                                                fontDescriptorWithSymbolicTraits:UIFontDescriptorTraitBold | UIFontDescriptorTraitItalic];
        titleFont = [UIFont fontWithDescriptor:fontDescriptor size:0];
      } else {
        titleFont = [UIFont fontWithName:@"Helvetica-BoldOblique" size:17.0];
      }
    } else if (isBold == TRUE && isItalic == FALSE) {
      titleFont = [UIFont boldSystemFontOfSize:17.0f];
    } else if (isBold == TRUE && isItalic == FALSE) {
      titleFont = [UIFont italicSystemFontOfSize:17.0f];
    } else {
      titleFont = [UIFont systemFontOfSize:17.0f];
    }
    
    // Calculate the size for the title strings
    textSize = [title sizeWithFont:titleFont constrainedToSize: CGSizeMake(mapView.frame.size.width - 13 * scale, mapView.frame.size.height - 13 * scale)];
    rectSize = CGSizeMake(textSize.width , textSize.height + 11 * scale);
    
    // Calculate the size for the snippet strings
    if (snippet) {
      snippetFont = [UIFont systemFontOfSize:12.0f];
      snippet = [snippet stringByReplacingOccurrencesOfString:@"\n" withString:@""];
      snippetSize = [snippet sizeWithFont:snippetFont constrainedToSize: CGSizeMake(mapView.frame.size.width - 13 * scale, mapView.frame.size.height - 13 * scale)];
      rectSize.height += snippetSize.height + 2 * scale;
      if (rectSize.width < snippetSize.width + leftImg.size.width) {
        rectSize.width = snippetSize.width + leftImg.size.width;
      }
    }
  }
  if (rectSize.width < leftImg.size.width * scale) {
    rectSize.width = leftImg.size.width * scale;
  } else {
    rectSize.width += sizeEdgeWidth;
  }
  
  //-------------------------------------
  // Draw the the info window
  //-------------------------------------
  UIGraphicsBeginImageContextWithOptions(rectSize, NO, 0.0f);
  
  CGRect trimArea = CGRectMake(15, 0, 5, MIN(45, rectSize.height - 20));
  
  trimArea = CGRectMake(15, 0, 15, leftImg.size.height);
  if (scale > 1.0f) {
    trimArea = CGRectMake(trimArea.origin.x * scale,
                      trimArea.origin.y * scale,
                      trimArea.size.width * scale +1,
                      trimArea.size.height * scale);
  }
  CGImageRef shadowImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
  UIImage *shadowImageLeft = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUp];
  UIImage *shadowImageRight = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUpMirrored];

  int y;
  int i = 0;
  int x = shadowImageLeft.size.width;
  float centerPos = rectSize.width * 0.5f;
  while (centerPos - x > shadowImageLeft.size.width) {
    y = 1;
    while (y + shadowImageLeft.size.height < rectSize.height) {
      [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
      [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];
      y += shadowImageRight.size.height;
    }
    y = rectSize.height - shadowImageLeft.size.height;
    [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
    [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];
    
    if (i == 0) {
      x += 5;
    
      trimArea = CGRectMake(15, 0, 5, leftImg.size.height);
      if (scale > 1.0f) {
        trimArea = CGRectMake(trimArea.origin.x * scale,
                          trimArea.origin.y * scale,
                          trimArea.size.width * scale,
                          trimArea.size.height * scale);
      }
      shadowImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
      shadowImageLeft = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUp];
      shadowImageRight = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUpMirrored];
    
    } else {
      x += shadowImageLeft.size.width;
    }
    i++;
  }
  
  // Draw left & right side edges
  x -= shadowImageLeft.size.width;
  trimArea = CGRectMake(0, 0, sizeEdgeWidth, leftImg.size.height);
  if (scale > 1.0f) {
    trimArea = CGRectMake(trimArea.origin.x * scale,
                      trimArea.origin.y * scale,
                      trimArea.size.width * scale,
                      trimArea.size.height * scale);
  }
  shadowImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
  shadowImageLeft = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUp];
  shadowImageRight = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUpMirrored];
  x += shadowImageLeft.size.width;
  
  y = 1;
  while (y + shadowImageLeft.size.height < rectSize.height) {
    [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
    [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];
    y += shadowImageRight.size.height;
  }
  y = rectSize.height - shadowImageLeft.size.height;
  [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
  [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];
  
  // Fill the body area with WHITE color
  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextSetAllowsAntialiasing(context, true);
  CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0);
  
  if (isTextMode) {

    if (snippet) {
      CGContextFillRect(context, CGRectMake(centerPos - x + 5, 4, rectSize.width - (centerPos - x + 7), rectSize.height - 16));
    } else {
      CGContextFillRect(context, CGRectMake(centerPos - x + 5, 0, rectSize.width - (centerPos - x + 7), rectSize.height - 11));
    }
  } else {
    CGContextFillRect(context, CGRectMake(centerPos - x + 5, 4, rectSize.width - (centerPos - x + 5), rectSize.height - 16));
  }
  
  //--------------------------------
  // text-align: left/center/right
  //--------------------------------
  NSTextAlignment textAlignment = NSTextAlignmentLeft;
  if (styles && [styles objectForKey:@"text-align"]) {
    NSString *textAlignValue = [styles objectForKey:@"text-align"];
    
    NSDictionary *aligments = [NSDictionary dictionaryWithObjectsAndKeys:
                            ^() {return NSTextAlignmentLeft; }, @"left",
                            ^() {return NSTextAlignmentRight; }, @"right",
                            ^() {return NSTextAlignmentCenter; }, @"center",
                            nil];
  
    typedef NSTextAlignment (^CaseBlock)();
    CaseBlock caseBlock = aligments[textAlignValue];
    if (caseBlock) {
      textAlignment = caseBlock();
    }
  }
  
  //-------------------------------------
  // Draw the contents
  //-------------------------------------
  if (isTextMode) {
    //Draw the title strings
    if (title) {
      UIColor *titleColor = [UIColor blackColor];
      if (styles && [styles objectForKey:@"color"]) {
        titleColor = [[styles valueForKey:@"color"] parsePluginColor];
      }
      
      CGRect textRect = CGRectMake(5, 4 * scale, rectSize.width - 10, textSize.height );
      if ([PluginUtil isIOS7] == true) {
        // iOS7 and above
        NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
        style.lineBreakMode = NSLineBreakByWordWrapping;
        style.alignment = textAlignment;
        
        NSDictionary *attributes = @{
            NSForegroundColorAttributeName : titleColor,
            NSFontAttributeName : titleFont,
            NSParagraphStyleAttributeName : style
        };
        [title drawInRect:textRect
               withAttributes:attributes];
        
        
      } else {
        // iOS6
        [titleColor set];
        [title drawInRect:textRect
                withFont:titleFont
                lineBreakMode:NSLineBreakByWordWrapping
                alignment:textAlignment];
      }
      //CGContextSetRGBStrokeColor(context, 1.0, 0.0, 0.0, 0.5);
      //CGContextStrokeRect(context, textRect);
    }
    
    //Draw the snippet
    if (snippet) {
      CGRect textRect = CGRectMake(5, textSize.height + 4 * scale, rectSize.width - 10, snippetSize.height );
      if ([PluginUtil isIOS7] == true) {
          // iOS7 and above
          NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
          style.lineBreakMode = NSLineBreakByWordWrapping;
          style.alignment = textAlignment;
          
          NSDictionary *attributes = @{
              NSForegroundColorAttributeName : [UIColor grayColor],
              NSFontAttributeName : snippetFont,
              NSParagraphStyleAttributeName : style
          };
          [snippet drawInRect:textRect withAttributes:attributes];
        } else {
          // iOS6
          [[UIColor grayColor] set];
          [snippet drawInRect:textRect
                  withFont:snippetFont
                  lineBreakMode:NSLineBreakByWordWrapping
                  alignment:textAlignment];
        }
    }
  } else {
    //Draw the content image
    CGRect imageRect = CGRectMake((rectSize.width - base64Image.size.width) / 2 ,
                                  -1 * ((rectSize.height - base64Image.size.height - 20) / 2 + 7.5),
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
- (UIImage *)getUIImageByKey:(NSString *)key {
  return [self.overlayManager objectForKey:key];
}

- (void)removeObjectForKey: (NSString *)key {
  [self.overlayManager removeObjectForKey:key];
}
@end
