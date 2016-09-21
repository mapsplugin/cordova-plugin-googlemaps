//
//  GoogleMapsViewController.m
//  SimpleMap
//
//  Created by masashi on 11/6/13.
//
//

#import "GoogleMapsViewController.h"
#if CORDOVA_VERSION_MIN_REQUIRED < __CORDOVA_4_0_0
#import <Cordova/CDVJSON.h>
#endif


@implementation GoogleMapsViewController

- (id)initWithOptions:(NSDictionary *) options {
    self = [super init];
    self.plugins = [NSMutableDictionary dictionary];
    self.isFullScreen = NO;
    self.screenSize = [[UIScreen mainScreen] bounds];
    self.clickable = YES;
    self.isRenderedAtOnce = NO;
    self.mapDivId = nil;

    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
  
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
  
	NSString* jsString = [NSString
      stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: '%@', callback: '_onMapEvent', args: []});",
      self.mapId, @"my_location_button_click"];
  [self execJS:jsString];
	return NO;
}

#pragma mark - GMSMapViewDelegate

/**
 * @callback the my location button is clicked.
 */
- (void)mapView:(GMSMapView *)mapView didTapAtCoordinate:(CLLocationCoordinate2D)coordinate {
  
  NSArray *pluginNames =[self.plugins allKeys];
  NSString *pluginName, *key;
  NSDictionary *properties;
  CDVPlugin<MyPlgunProtocol> *plugin;
  GMSCoordinateBounds *bounds;
  NSArray *keys;
  NSNumber *isVisible;
  NSMutableArray *boundsHitList = [NSMutableArray array];
  int i,j;
  
  for (i = 0; i < [pluginNames count]; i++) {
    pluginName = [pluginNames objectAtIndex:i];
    NSLog(@"---> pluginName = %@", pluginName);
    
    // Skip marker class
    if ([pluginName hasSuffix:@"-marker"]) {
      continue;
    }
    
    // Get the plugin (marker, polyline, polygon, circle, groundOverlay)
    plugin = [self.plugins objectForKey:pluginName];
    
    
    keys = [plugin.objects allKeys];
    for (j = 0; j < [keys count]; j++) {
      key = [keys objectAtIndex:j];
        NSLog(@"--->key = %@", key);
      if ([key hasPrefix:@"polyline_property"]) {
        properties = [plugin.objects objectForKey:key];
        isVisible = (NSNumber *)[properties objectForKey:@"isVisible"];
        
        // Skip invisible polyline
        if ([isVisible boolValue] == NO) {
          continue;
        }
        
        // Skip if the click point is out of the polyline bounds.
        bounds = (GMSCoordinateBounds *)[properties objectForKey:@"bounds"];
        if ([bounds containsCoordinate:coordinate] == YES) {
          [boundsHitList addObject:[key stringByReplacingOccurrencesOfString:@"_property" withString:@""]];
        }
        
      
      }
      
    }
    
  }
  
  //[self triggerMapEvent:@"map_click" coordinate:coordinate];
}
/**
 * @callback map long_click
 */
- (void) mapView:(GMSMapView *)mapView didLongPressAtCoordinate:(CLLocationCoordinate2D)coordinate {
  [self triggerMapEvent:@"map_long_click" coordinate:coordinate];
}

/**
 * @callback plugin.google.maps.event.CAMERA_MOVE_START
 */
- (void) mapView:(GMSMapView *)mapView willMove:(BOOL)gesture
{
  // In order to pass the gesture parameter to the callbacks,
  // use the _onMapEvent callback instead of the _onCameraEvent callback.
	NSString* jsString = [NSString
    stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: '%@', callback: '_onMapEvent', args: [%@]});",
    self.mapId, @"camera_move_start", gesture ? @"true": @"false"];
  [self execJS:jsString];
}


/**
 * @callback plugin.google.maps.event.CAMERA_MOVE
 */
- (void)mapView:(GMSMapView *)mapView didChangeCameraPosition:(GMSCameraPosition *)position {
  [self triggerCameraEvent:@"camera_move" position:position];
}

/**
 * @callback plugin.google.maps.event.CAMERA_MOVE_END
 */
- (void) mapView:(GMSMapView *)mapView idleAtCameraPosition:(GMSCameraPosition *)position
{
  [self triggerCameraEvent:@"camera_move_end" position:position];
}


/**
 * @callback marker info_click
 */
- (void) mapView:(GMSMapView *)mapView didTapInfoWindowOfMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"info_click" marker:marker];
}
/**
 * Called after a marker's info window has been long pressed.
 */
- (void)mapView:(GMSMapView *)mapView didLongPressInfoWindowOfMarker:(GMSMarker *)marker {

  [self triggerMarkerEvent:@"info_long_click" marker:marker];
}
/**
 * @callback plugin.google.maps.event.MARKER_DRAG_START
 */
- (void) mapView:(GMSMapView *) mapView didBeginDraggingMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"marker_drag_start" marker:marker];
}
/**
 * @callback plugin.google.maps.event.MARKER_DRAG_END
 */
- (void) mapView:(GMSMapView *) mapView didEndDraggingMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"marker_drag_end" marker:marker];
}
/**
 * @callback plugin.google.maps.event.MARKER_DRAG
 */
- (void) mapView:(GMSMapView *) mapView didDragMarker:(GMSMarker *)marker
{
  [self triggerMarkerEvent:@"marker_drag" marker:marker];
}

/**
 * @callback plugin.google.maps.event.MARKER_CLICK
 */
- (BOOL)mapView:(GMSMapView *)mapView didTapMarker:(GMSMarker *)marker {
  [self triggerMarkerEvent:@"marker_click" marker:marker];
  
  // Get the marker plugin
  NSString *pluginId = [NSString stringWithFormat:@"%@-marker", self.mapId];
  CDVPlugin<MyPlgunProtocol> *plugin = [self.plugins objectForKey:pluginId];
  
  // Get the marker properties
  NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
  NSDictionary *properties = [plugin.objects objectForKey:markerPropertyId];

  BOOL disableAutoPan = false;
  if ([properties objectForKey:@"disableAutoPan"] != nil) {
    disableAutoPan = [[properties objectForKey:@"disableAutoPan"] boolValue];
    if (disableAutoPan) {
      self.map.selectedMarker = marker;
      return YES;
    }
  }
	return NO;
}

- (void)mapView:(GMSMapView *)mapView didCloseInfoWindowOfMarker:(nonnull GMSMarker *)marker {
  [self triggerMarkerEvent:@"info_close" marker:marker];
}


/*
- (void)mapView:(GMSMapView *)mapView didTapOverlay:(GMSOverlay *)overlay {
  NSString *overlayClass = NSStringFromClass([overlay class]);
  if ([overlayClass isEqualToString:@"GMSPolygon"] ||
      [overlayClass isEqualToString:@"GMSPolyline"] ||
      [overlayClass isEqualToString:@"GMSCircle"] ||
      [overlayClass isEqualToString:@"GMSGroundOverlay"]) {
    [self triggerOverlayEvent:@"overlay_click" id:overlay.title];
  }
}
*/

/**
 * plugin.google.maps.event.MAP_***(new google.maps.LatLng(lat,lng)) events
 */
- (void)triggerMapEvent: (NSString *)eventName coordinate:(CLLocationCoordinate2D)coordinate
{

	NSString* jsString = [NSString
      stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: '%@', callback: '_onMapEvent', args: [new plugin.google.maps.LatLng(%f,%f)]});",
      self.mapId, eventName, coordinate.latitude, coordinate.longitude];
  [self execJS:jsString];
}

/**
 * plugin.google.maps.event.CAMERA_*** events
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
  [json setObject:[NSNumber numberWithInt:(int)position.hash] forKey:@"hashCode"];
  [json setObject:[NSNumber numberWithFloat:position.zoom] forKey:@"zoom"];
  
  
  NSData* jsonData = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
  NSString* sourceArrayString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
  NSString* jsString = [NSString
    stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: '%@', callback: '_onCameraEvent', args: [%@]});",
    self.mapId, eventName, sourceArrayString];
  [self execJS:jsString];
}

- (void)execJS: (NSString *)jsString {
    if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
        [self.webView performSelector:@selector(stringByEvaluatingJavaScriptFromString:) withObject:jsString];
    } else if ([self.webView respondsToSelector:@selector(evaluateJavaScript:completionHandler:)]) {
        [self.webView performSelector:@selector(evaluateJavaScript:completionHandler:) withObject:jsString withObject:nil];
    }
}

/**
 * plugin.google.maps.event.MARKER_*** events
 */
- (void)triggerMarkerEvent: (NSString *)eventName marker:(GMSMarker *)marker
{
  NSString* jsString = [NSString
                              stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: '%@', callback: '_onMarkerEvent', args: ['marker_%lu', {lat: %f, lng: %f}]});",
                              self.mapId, eventName, (unsigned long)marker.hash,
                              marker.position.latitude,
                              marker.position.longitude];
  [self execJS:jsString];
}

/**
 * Involve App._onOverlayEvent
 */
- (void)triggerOverlayEvent: (NSString *)eventName id:(NSString *) id
{
	NSString* jsString = [NSString
    stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: '%@', callback: '_onOverlayEvent', args: ['%@']});",
    self.mapId, eventName, id];
  [self execJS:jsString];
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
  
  [self triggerMarkerEvent:@"info_open" marker:marker];
  
  // Get the marker plugin
  NSString *pluginId = [NSString stringWithFormat:@"%@-marker", self.mapId];
  CDVPlugin<MyPlgunProtocol> *plugin = [self.plugins objectForKey:pluginId];
  
  // Get the marker properties
  NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
  NSDictionary *properties = [plugin.objects objectForKey:markerPropertyId];

  // Load styles
  NSDictionary *styles = nil;
  if ([properties objectForKey:@"styles"]) {
    styles = [properties objectForKey:@"styles"];
  }
  
  // Load images
  UIImage *leftImg = nil;
  UIImage *rightImg = nil;[self loadImageFromGoogleMap:@"bubble_right@2x"];
  leftImg = [self loadImageFromGoogleMap:@"bubble_left@2x"];
  rightImg = [self loadImageFromGoogleMap:@"bubble_right@2x"];
  float scale = leftImg.scale;
  int sizeEdgeWidth = 10;

	int width = 0;

	if (styles && [styles objectForKey:@"width"]) {
		NSString *widthString = [styles valueForKey:@"width"];
        
        // check if string is numeric
        NSNumberFormatter *nf = [[NSNumberFormatter alloc] init];
        BOOL isNumeric = [nf numberFromString:widthString] != nil;

		if ([widthString hasSuffix:@"%"]) {
			double widthDouble = [[widthString stringByReplacingOccurrencesOfString:@"%" withString:@""] doubleValue];
			
			width = (int)((double)mapView.frame.size.width * (widthDouble / 100));
		} else if (isNumeric) {
			double widthDouble = [widthString doubleValue];

			if (widthDouble <= 1.0) {
				width = (int)((double)mapView.frame.size.width * (widthDouble));
			} else {
				width = (int)widthDouble;
			}
		}
	}

	int maxWidth = 0;

	if (styles && [styles objectForKey:@"maxWidth"]) {
		NSString *widthString = [styles valueForKey:@"maxWidth"];
		
        NSNumberFormatter *nf = [[NSNumberFormatter alloc] init];
        BOOL isNumeric = [nf numberFromString:widthString] != nil;
        
		if ([widthString hasSuffix:@"%"]) {
			double widthDouble = [[widthString stringByReplacingOccurrencesOfString:@"%" withString:@""] doubleValue];
			
			maxWidth = (int)((double)mapView.frame.size.width * (widthDouble / 100));
			
			// make sure to take padding into account.
			maxWidth -= sizeEdgeWidth;
		} else if (isNumeric) {
			double widthDouble = [widthString doubleValue];
			
			if (widthDouble <= 1.0) {
				maxWidth = (int)((double)mapView.frame.size.width * (widthDouble));
			} else {
				maxWidth = (int)widthDouble;
			}
		}
	}

  //-------------------------------------
  // Calculate the size for the contents
  //-------------------------------------
  if ([title rangeOfString:@"data:image/"].location != NSNotFound &&
      [title rangeOfString:@";base64,"].location != NSNotFound) {
    
      isTextMode = false;
      NSArray *tmp = [title componentsSeparatedByString:@","];
      NSData *decodedData = [NSData dataFromBase64String:tmp[1]];    
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
          // ref: http://stackoverflow.com/questions/4713236/how-do-i-set-bold-and-italic-on-uilabel-of-iphone-ipad#21777132
          titleFont = [UIFont systemFontOfSize:17.0f];
          UIFontDescriptor *fontDescriptor = [titleFont.fontDescriptor
                                                  fontDescriptorWithSymbolicTraits:UIFontDescriptorTraitBold | UIFontDescriptorTraitItalic];
          titleFont = [UIFont fontWithDescriptor:fontDescriptor size:0];
      } else if (isBold == TRUE && isItalic == FALSE) {
          titleFont = [UIFont boldSystemFontOfSize:17.0f];
      } else if (isBold == TRUE && isItalic == FALSE) {
          titleFont = [UIFont italicSystemFontOfSize:17.0f];
      } else {
          titleFont = [UIFont systemFontOfSize:17.0f];
      }
      
      // Calculate the size for the title strings
      textSize = [title sizeWithFont:titleFont constrainedToSize: CGSizeMake(mapView.frame.size.width - 13, mapView.frame.size.height - 13)];
      rectSize = CGSizeMake(textSize.width + 10, textSize.height + 22);
      
      // Calculate the size for the snippet strings
      if (snippet) {
          snippetFont = [UIFont systemFontOfSize:12.0f];
          snippet = [snippet stringByReplacingOccurrencesOfString:@"\n" withString:@""];
          snippetSize = [snippet sizeWithFont:snippetFont constrainedToSize: CGSizeMake(mapView.frame.size.width - 13, mapView.frame.size.height - 13)];
          rectSize.height += snippetSize.height + 4;
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
	
	if (width > 0) {
		rectSize.width = width;
	}
	if (maxWidth > 0 &&
		maxWidth < rectSize.width) {
		rectSize.width = maxWidth;
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
      
      CGRect textRect = CGRectMake(5, 5 , rectSize.width - 10, textSize.height );
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
      //CGContextSetRGBStrokeColor(context, 1.0, 0.0, 0.0, 0.5);
      //CGContextStrokeRect(context, textRect);
    }
    
    //Draw the snippet
    if (snippet) {
      CGRect textRect = CGRectMake(5, textSize.height + 10 , rectSize.width - 10, snippetSize.height );
      NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
      style.lineBreakMode = NSLineBreakByWordWrapping;
      style.alignment = textAlignment;
      
      NSDictionary *attributes = @{
          NSForegroundColorAttributeName : [UIColor grayColor],
          NSFontAttributeName : snippetFont,
          NSParagraphStyleAttributeName : style
      };
      [snippet drawInRect:textRect withAttributes:attributes];
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


- (void) didChangeActiveBuilding: (GMSIndoorBuilding *)building {
  if (building == nil) {
      return;
  }
  //Notify to the JS
	NSString* jsString = [NSString
    stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: 'indoor_building_focused', callback: '_onMapEvent'});",
    self.mapId];
  [self execJS:jsString];
}

- (void) didChangeActiveLevel: (GMSIndoorLevel *)activeLevel {
  
  if (activeLevel == nil) {
      return;
  }
  GMSIndoorBuilding *building = self.map.indoorDisplay.activeBuilding;
  
  NSMutableDictionary *result = [NSMutableDictionary dictionary];
  
  NSUInteger activeLevelIndex = [building.levels indexOfObject:activeLevel];
  [result setObject:[NSNumber numberWithInteger:activeLevelIndex] forKey:@"activeLevelIndex"];
  [result setObject:[NSNumber numberWithInteger:building.defaultLevelIndex] forKey:@"defaultLevelIndex"];
  
  GMSIndoorLevel *level;
  NSMutableDictionary *levelInfo;
  NSMutableArray *levels = [NSMutableArray array];
  for (level in building.levels) {
    levelInfo = [NSMutableDictionary dictionary];
    
    [levelInfo setObject:[NSString stringWithString:level.name] forKey:@"name"];
    [levelInfo setObject:[NSString stringWithString:level.shortName] forKey:@"shortName"];
    [levels addObject:levelInfo];
  }
  [result setObject:levels forKey:@"levels"];
  
  NSError *error;
  NSData *data = [NSJSONSerialization dataWithJSONObject:result options:NSJSONWritingPrettyPrinted error:&error];
  
  
  NSString *JSONstring = [[NSString alloc] initWithData:data
                                           encoding:NSUTF8StringEncoding];

  //Notify to the JS
	NSString* jsString = [NSString
    stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@', {evtName: 'indoor_level_activated', callback: '_onMapEvent', args: [%@]});",
    self.mapId, JSONstring];
  
  [self execJS:jsString];
}

@end
