//
//  Marker.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "Marker.h"

@implementation Marker
-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

/**
 * @param marker options
 * @return marker key
 */
-(void)createMarker:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  NSDictionary *latLng = [json objectForKey:@"position"];
  float latitude = [[latLng valueForKey:@"lat"] floatValue];
  float longitude = [[latLng valueForKey:@"lng"] floatValue];
  
  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
  GMSMarker *marker = [GMSMarker markerWithPosition:position];
  if ([[json valueForKey:@"visible"] boolValue] == true) {
    marker.map = self.mapCtrl.map;
  }
  if ([json valueForKey:@"title"]) {
    [marker setTitle: [json valueForKey:@"title"]];
  }
  if ([json valueForKey:@"snippet"]) {
    [marker setSnippet: [json valueForKey:@"snippet"]];
  }
  if ([json valueForKey:@"draggable"]) {
    [marker setDraggable:[[json valueForKey:@"draggable"] boolValue]];
  }
  if ([json valueForKey:@"flat"]) {
    [marker setFlat:[[json valueForKey:@"flat"] boolValue]];
  }
  if ([json valueForKey:@"rotation"]) {
    CLLocationDegrees degrees = [[json valueForKey:@"rotation"] doubleValue];
    [marker setRotation:degrees];
  }
  if ([json valueForKey:@"opacity"]) {
    [marker setOpacity:[[json valueForKey:@"opacity"] floatValue]];
  }
  
  NSString *id = [NSString stringWithFormat:@"marker_%lu", (unsigned long)marker.hash];
  [self.mapCtrl.overlayManager setObject:marker forKey: id];
  
  // Custom properties
  NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
  NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
  
  if ([json valueForKey:@"styles"]) {
    NSDictionary *styles = [json valueForKey:@"styles"];
    [properties setObject:styles forKey:@"styles"];
  }
  
  BOOL disableAutoPan = NO;
  if ([json valueForKey:@"disableAutoPan"] != nil) {
    disableAutoPan = [[json valueForKey:@"disableAutoPan"] boolValue];
  }
  [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
  [self.mapCtrl.overlayManager setObject:properties forKey: markerPropertyId];
  
  // Create icon
  NSMutableDictionary *iconProperty = nil;
  NSObject *icon = [json valueForKey:@"icon"];
  if ([icon isKindOfClass:[NSString class]]) {
    iconProperty = [NSMutableDictionary dictionary];
    [iconProperty setObject:icon forKey:@"url"];
    
  } else if ([icon isKindOfClass:[NSDictionary class]]) {
    iconProperty = [json valueForKey:@"icon"];
  }
  
  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:id forKey:@"id"];
  [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)marker.hash] forKey:@"hashCode"];
  
  CDVPluginResult* pluginResult = nil;
  if (iconProperty) {
    if ([json valueForKey:@"infoWindowAnchor"]) {
      [iconProperty setObject:[json valueForKey:@"infoWindowAnchor"] forKey:@"infoWindowAnchor"];
    }
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    [self setIcon_:marker iconProperty:iconProperty pluginResult:pluginResult callbackId:command.callbackId];
  } else {
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }
}

/**
 * Show the infowindow of the current marker
 * @params MarkerKey
 */
-(void)showInfoWindow:(CDVInvokedUrlCommand *)command
{
  
  NSString *hashCode = [command.arguments objectAtIndex:1];
  
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:hashCode];
  if (marker) {
    self.mapCtrl.map.selectedMarker = marker;
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  
}
/**
 * Hide current infowindow
 * @params MarkerKey
 */
-(void)hideInfoWindow:(CDVInvokedUrlCommand *)command
{
  self.mapCtrl.map.selectedMarker = nil;
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * @params MarkerKey
 * @return current marker position with array(latitude, longitude)
 */
-(void)getPosition:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  NSNumber *latitude = @0.0;
  NSNumber *longitude = @0.0;
  if (marker) {
    latitude = [NSNumber numberWithFloat: marker.position.latitude];
    longitude = [NSNumber numberWithFloat: marker.position.longitude];
  }
  NSMutableDictionary *json = [NSMutableDictionary dictionary];
  [json setObject:latitude forKey:@"lat"];
  [json setObject:longitude forKey:@"lng"];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * @params MarkerKey
 * @return boolean
 */
-(void)isInfoWindowShown:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  Boolean isOpen = false;
  if (self.mapCtrl.map.selectedMarker == marker) {
    isOpen = YES;
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isOpen];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)setTitle:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  marker.title = [command.arguments objectAtIndex:2];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)setSnippet:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  marker.snippet = [command.arguments objectAtIndex:2];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the specified marker
 * @params MarkerKey
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
  marker.map = nil;
  [self.mapCtrl removeObjectForKey:markerKey];
  marker = nil;
  
  if ([self.mapCtrl.overlayManager objectForKey:propertyId]) {
    [self.mapCtrl removeObjectForKey:propertyId];
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set anchor of the marker
 * @params MarkerKey
 */
-(void)setIconAnchor:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  CGFloat anchorX = [[command.arguments objectAtIndex:2] floatValue];
  CGFloat anchorY = [[command.arguments objectAtIndex:3] floatValue];
  
  if (marker.icon) {
    anchorX = anchorX / marker.icon.size.width;
    anchorY = anchorY / marker.icon.size.height;
    [marker setGroundAnchor:CGPointMake(anchorX, anchorY)];
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set anchor of the info window
 * @params MarkerKey
 */
-(void)setInfoWindowAnchor:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  float anchorX = [[command.arguments objectAtIndex:2] floatValue];
  float anchorY = [[command.arguments objectAtIndex:3] floatValue];
  
  if (marker.icon) {
    anchorX = anchorX / marker.icon.size.width;
    anchorY = anchorY / marker.icon.size.height;
    [marker setGroundAnchor:CGPointMake(anchorX, anchorY)];
  }
  [marker setInfoWindowAnchor:CGPointMake(anchorX, anchorY)];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set opacity
 * @params MarkerKey
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  marker.opacity = [[command.arguments objectAtIndex:2] floatValue];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set draggable
 * @params MarkerKey
 */
-(void)setDraggable:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  Boolean isEnabled = [[command.arguments objectAtIndex:2] boolValue];
  [marker setDraggable:isEnabled];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set disable auto pan
 * @params MarkerKey
 */
-(void)setDisableAutoPan:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  BOOL disableAutoPan = [[command.arguments objectAtIndex:2] boolValue];
  
  NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
  NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                      [self.mapCtrl.overlayManager objectForKey:markerPropertyId]];
  [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
  [self.mapCtrl.overlayManager setObject:properties forKey:markerPropertyId];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set visibility
 * @params MarkerKey
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
  
  if (isVisible) {
    marker.map = self.mapCtrl.map;
  } else {
    marker.map = nil;
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set position
 * @params key
 */
-(void)setPosition:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl getMarkerByKey: markerKey];
 
  float latitude = [[command.arguments objectAtIndex:2] floatValue];
  float longitude = [[command.arguments objectAtIndex:3] floatValue];
  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
  [marker setPosition:position];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set flattable
 * @params MarkerKey
 */
-(void)setFlat:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  Boolean isFlat = [[command.arguments objectAtIndex:2] boolValue];
  [marker setFlat: isFlat];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * set icon
 * @params MarkerKey
 */
-(void)setIcon:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  
  // Create icon
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  NSDictionary *iconProperty = [command.arguments objectAtIndex:2];
  [self setIcon_:marker iconProperty:iconProperty pluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * set rotation
 */
-(void)setRotation:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
  
  CLLocationDegrees degrees = [[command.arguments objectAtIndex:2] doubleValue];
  [marker setRotation:degrees];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * @private
 * Load the icon; then set to the marker
 */
-(void)setIcon_:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty
                pluginResult:(CDVPluginResult *)pluginResult
                callbackId:(NSString*)callbackId {
  NSString *iconPath = nil;
  CGFloat width = 0;
  CGFloat height = 0;
  CGFloat anchorX = 0;
  CGFloat anchorY = 0;
  
  // The `url` property
  iconPath = [iconProperty valueForKey:@"url"];
  
  // The `size` property
  if ([iconProperty valueForKey:@"size"]) {
    NSDictionary *size = [iconProperty valueForKey:@"size"];
    width = [[size objectForKey:@"width"] floatValue];
    height = [[size objectForKey:@"height"] floatValue];
  }

  if (iconPath) {
    NSRange range = [iconPath rangeOfString:@"http"];
    if (range.location != 0) {
      Boolean isTextMode = true;
          
      UIImage *image;
      if ([iconPath rangeOfString:@"data:image/"].location != NSNotFound &&
          [iconPath rangeOfString:@";base64,"].location != NSNotFound) {
        
        /**
         * Base64 icon
         */
        isTextMode = false;
        NSArray *tmp = [iconPath componentsSeparatedByString:@","];
        
        NSData *decodedData;
        if ([PluginUtil isIOS7_OR_OVER]) {
          decodedData = [[NSData alloc] initWithBase64EncodedString:tmp[1] options:0];
        } else {
          decodedData = [NSData dataFromBase64String:tmp[1]];
        }
        image = [[UIImage alloc] initWithData:decodedData];
        if (width && height) {
          image = [image resize:width height:height];
        }
        
        // The `anchor` property for the icon
        if ([iconProperty valueForKey:@"anchor"]) {
          NSArray *points = [iconProperty valueForKey:@"anchor"];
          anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
          anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
          marker.groundAnchor = CGPointMake(anchorX, anchorY);
        }
      
      } else {
        /**
         * Load the icon from local path
         */
        
        range = [iconPath rangeOfString:@"cdvfile://"];
        if (range.location != NSNotFound) {
        
          // Convert cdv:// path to the device real path
          // (http://docs.monaca.mobi/3.5/en/reference/phonegap_34/en/file/plugins/)
          NSString *filePath = nil;
          Class CDVFilesystemURLCls = NSClassFromString(@"CDVFilesystemURL");
          Class CDVFileCls = NSClassFromString(@"CDVFile");
          if (CDVFilesystemURLCls != nil && CDVFileCls != nil) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
          
            SEL fileSystemURLWithString = NSSelectorFromString(@"fileSystemURLWithString:");
            if ([CDVFilesystemURLCls respondsToSelector:fileSystemURLWithString]) {
              id cdvFilesystemURL = [CDVFilesystemURLCls performSelector:fileSystemURLWithString withObject:iconPath];
              if (cdvFilesystemURL != nil) {
              
                CDVPlugin *filePlugin = (CDVPlugin *)[[CDVFileCls alloc] initWithWebView:self.webView];
                [filePlugin pluginInitialize];
                
                SEL filesystemPathForURL = NSSelectorFromString(@"filesystemPathForURL:");
                filePath = [filePlugin performSelector: filesystemPathForURL withObject:cdvFilesystemURL];
              }
            }
            #pragma clang diagnostic pop
          } else {
            NSLog(@"(debug)File and FileTransfer plugins are required to convert cdvfile:// to localpath.");
          }
          
          if (filePath != nil) {
            iconPath = filePath;
          } else {
            NSLog(@"(debug)Can not convert '%@' to device full path.", iconPath);
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
            return;
          }
        }
      
        range = [iconPath rangeOfString:@"file://"];
        if (range.location != NSNotFound) {
          iconPath = [iconPath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
          NSFileManager *fileManager = [NSFileManager defaultManager];
          if (![fileManager fileExistsAtPath:iconPath]) {
            NSLog(@"(debug)There is no file at '%@'.", iconPath);
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
            return;
          }
        }
        image = [UIImage imageNamed:iconPath];
        
        if (width && height) {
          image = [image resize:width height:height];
        }
      }
      
      marker.icon = image;
      // The `anchor` property for the icon
      if ([iconProperty valueForKey:@"anchor"]) {
        NSArray *points = [iconProperty valueForKey:@"anchor"];
        anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.groundAnchor = CGPointMake(anchorX, anchorY);
      }
      
      // The `infoWindowAnchor` property
      if ([iconProperty valueForKey:@"infoWindowAnchor"]) {
        NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
        anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
        anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
        marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
      }
      [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    } else {
      /***
       * Load the icon from over the internet
       */
      marker.map = nil;
    
      dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0ul);
      dispatch_async(queue, ^{

        NSURL *url = [NSURL URLWithString:iconPath];
        // download the image asynchronously
        [self downloadImageWithURL:url completionBlock:^(BOOL succeeded, UIImage *image) {
            if (!succeeded) {
              marker.map = self.mapCtrl.map;
            
              [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
              return;
            }

            if (width && height) {
                image = [image resize:width height:height];
            }

            dispatch_async(dispatch_get_main_queue(), ^{
                marker.icon = image;

                // The `anchor` property for the icon
                if ([iconProperty valueForKey:@"anchor"]) {
                    NSArray *points = [iconProperty valueForKey:@"anchor"];
                    CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                    CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                    marker.groundAnchor = CGPointMake(anchorX, anchorY);
                }


                // The `infoWindowAnchor` property
                if ([iconProperty valueForKey:@"infoWindowAnchor"]) {
                    NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
                    CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                    CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                    marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
                }

                marker.map = self.mapCtrl.map;
                [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];

            });
        }];

      });
    }
  }
  
}
- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock
{
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    [NSURLConnection sendAsynchronousRequest:request
                                       queue:[NSOperationQueue mainQueue]
                           completionHandler:^(NSURLResponse *response, NSData *data, NSError *error) {
                               if ( !error )
                               {
                                   UIImage *image = [[UIImage alloc] initWithData:data];
                                   completionBlock(YES,image);
                               } else{
                                   completionBlock(NO,nil);
                               }
                           }];
}
@end
