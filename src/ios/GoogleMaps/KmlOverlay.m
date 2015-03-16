//
//  GroundOverlay.m
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/4/13.
//
//

#import "KmlOverlay.h"


@implementation KmlOverlay

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createKmlOverlay:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  
  NSError *error;
  TBXML *tbxml = [TBXML alloc];// initWithXMLFile:urlStr error:&error];
  
  NSString *urlStr = [json objectForKey:@"url"];
  NSRange range = [urlStr rangeOfString:@"cdvfile://"];
  if (range.location != NSNotFound) {
    urlStr = [PluginUtil getAbsolutePathFromCDVFilePath:self.webView cdvFilePath:urlStr];
    if (urlStr == nil) {
      NSMutableDictionary* details = [NSMutableDictionary dictionary];
      [details setValue:[NSString stringWithFormat:@"Can not convert '%@' to device full path.", urlStr] forKey:NSLocalizedDescriptionKey];
      error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
    }
  }
  
  range = [urlStr rangeOfString:@"file://"];
  if (range.location != NSNotFound) {
    urlStr = [urlStr stringByReplacingOccurrencesOfString:@"file://" withString:@""];
    NSFileManager *fileManager = [NSFileManager defaultManager];
    if (![fileManager fileExistsAtPath:urlStr]) {
      NSMutableDictionary* details = [NSMutableDictionary dictionary];
      [details setValue:[NSString stringWithFormat:@"There is no file at '%@'.", urlStr] forKey:NSLocalizedDescriptionKey];
      error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
    }
  }
  
  range = [urlStr rangeOfString:@"http://"];
  if (range.location == NSNotFound) {
    tbxml = [tbxml initWithXMLFile:urlStr error:&error];
  } else {
    NSURLRequest *req = [NSURLRequest requestWithURL:[NSURL URLWithString:urlStr]];
    bool valid = [NSURLConnection canHandleRequest:req];
    if (valid) {
      NSError *error;
      NSData *xmlData = [[NSData alloc] initWithContentsOfURL:[NSURL URLWithString:urlStr]];
      tbxml = [tbxml initWithXMLData:xmlData error:&error];
    } else {
      
      NSMutableDictionary* details = [NSMutableDictionary dictionary];
      [details setValue:[NSString stringWithFormat:@"Cannot load KML data from %@", urlStr] forKey:NSLocalizedDescriptionKey];
      error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
    }
  }
  
  // If there is an error, return
  CDVPluginResult* pluginResult;
  if (error) {
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    return;
  }
  
  if ([json valueForKey:@"kmlId"]) {
    self.kmlId = [json valueForKey:@"kmlId"];
  } else {
    self.kmlId = [NSString stringWithFormat:@"kml%d-", arc4random()];
  }
  
  //Show the spinner
  self._loadingView = [[UIView alloc] init];
  [self._loadingView setFrameWithInt:0 top:0 width:self.mapCtrl.view.frame.size.width height:self.mapCtrl.view.frame.size.height];
  self._loadingView.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.5];
  self._loadingView.clipsToBounds = YES;
  self._loadingView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  
  //Create the dialog
  CGRect dialogRect = self.mapCtrl.view.frame;
  dialogRect.size.width = dialogRect.size.width * 0.8;
  dialogRect.size.height = 80;
  
  UIView *licenseDialog = [[UIView alloc] initWithFrame:dialogRect];
  licenseDialog.center = CGPointMake(self._loadingView.frame.size.width / 2, self._loadingView.frame.size.height / 2);
  [licenseDialog setBackgroundColor:[UIColor blackColor]];
  [licenseDialog.layer setBorderColor:[UIColor colorWithRed:255 green:255 blue:255 alpha:0.5].CGColor];
  [licenseDialog.layer setBorderWidth:1.0];
  [licenseDialog.layer setCornerRadius:10];
  licenseDialog.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleHeight;
  [self._loadingView addSubview:licenseDialog];
  
  self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
  self.spinner.center = CGPointMake(licenseDialog.frame.size.width / 2, licenseDialog.frame.size.height / 2 + 10);
  [licenseDialog addSubview:self.spinner];
  self.spinner.hidesWhenStopped = YES;
  [self.spinner startAnimating];

  UILabel *message = [[UILabel alloc] initWithFrame:licenseDialog.frame];
  message.center = CGPointMake(licenseDialog.frame.size.width / 2, licenseDialog.frame.size.height / 2 - 20);
  message.backgroundColor = [UIColor clearColor];
  message.textColor = [UIColor whiteColor];
  message.adjustsFontSizeToFitWidth = YES;
  message.textAlignment = NSTextAlignmentCenter;
  message.text = @"Please wait...";
  [licenseDialog addSubview:message];

  [self.mapCtrl.view addSubview:self._loadingView];
  
  // Parse the kml file
  [self parseKML:tbxml command:command];
}
-(void)parseKML:(TBXML *)tbxml command:(CDVInvokedUrlCommand *)command
{

  
  NSString *idPrefix = [NSString stringWithFormat:@"%@-", self.kmlId];
  
  CDVPluginResult* pluginResult;
  
  dispatch_queue_t gueue = dispatch_queue_create("plugin.google.maps.Map.createKmlOverlay", NULL);
  
  //--------------------------------
  // Parse the kml file
  //--------------------------------
  __block NSMutableDictionary *kmlData;
  dispatch_async(gueue, ^{
    //NSLog(@"%@", [[TBXML elementName:tbxml.rootXMLElement] lowercaseString]);
    //NSLog(@"-----------------> parseXML");
    kmlData = [self parseXML:tbxml.rootXMLElement];
    //NSLog(@"%@", kmlData);

  });
  
  //--------------------------------
  // Separate styles and placemarks
  //--------------------------------
  __block NSDictionary *tag;
  __block NSMutableDictionary *styles = [NSMutableDictionary dictionary];
  __block NSMutableArray *placeMarks = [NSMutableArray array];
  dispatch_async(gueue, ^{
    //NSLog(@"-----------------> _filterPlaceMarks");
    [self _filterPlaceMarks:kmlData placemarks:&placeMarks];
    //NSLog(@"-----------------> _filterPlaceMarks was successful");
  });
  
  //------------------------------------
  // Implement placemarks onto the map
  //------------------------------------
  dispatch_async(gueue, ^{
    //NSLog(@"-----------------> placeMarks = %d", [placeMarks count]);
    if ([placeMarks count] > 0) {
      // Pick up style tags only
      dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW, 0ul), ^{
        [self _filterStyleTag:kmlData styles:&styles];
        //NSLog(@"-----------------> _filterStyleTag was successful.");
      });
      
      // Pick up styleMap tags only
      dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW, 0ul), ^{
        [self _filterStyleMapTag:kmlData styles:&styles];
        //NSLog(@"-----------------> _filterStyleMapTag was successful.");
      });
      
      //Implement placemarks
      dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW, 0ul), ^{
        NSMutableArray *defaultViewport = [NSMutableArray array];
        for (tag in placeMarks) {
            NSMutableDictionary *options = nil;
            [self implementPlaceMarkToMap:tag
                  options:&options
                  styles:styles
                  styleUrl:nil
                  idPrefix:idPrefix
                  viewportRef:&defaultViewport];
        }
        
        //Close the loading view
        dispatch_sync(dispatch_get_main_queue(), ^{
          [self.spinner stopAnimating];
          [self._loadingView removeFromSuperview];
          [self._loadingView removeFromSuperview];
        });
        
    
        //Change the viewport
        NSDictionary *json = [command.arguments objectAtIndex:1];
        if ([[json valueForKey:@"preserveViewport"] boolValue] == NO) {
          NSString *changeMethod = @"animateCamera";
          if ([[json valueForKey:@"animation"] boolValue] == NO) {
            changeMethod = @"moveCamera";
          }
          NSMutableDictionary *cameraOptions = [NSMutableDictionary dictionary];
          [cameraOptions setObject:defaultViewport forKey:@"target"];
          [self _execOtherClassMethod:@"Map" methodName:changeMethod options:cameraOptions callbackId:@"kmlOverlay.viewChange"];
        }
      });

    } else {
      //Find network tag
      NSString *linkUrl = nil;
      [self _findLinkedKMLUrl:kmlData linkUrl:&linkUrl];

      if (linkUrl != nil) {
        NSMutableDictionary *options2 = [NSMutableDictionary dictionary];
        [options2 setObject:linkUrl forKey:@"url"];
        [options2 setObject:self.kmlId forKey:@"kmlId"];
        [self _implementToMap:@"KmlOverlay" options:options2 needJSCallback:NO];
      }
      
      
      [self.spinner stopAnimating];
      [self._loadingView removeFromSuperview];
    }
    
  });


  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:self.kmlId];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}



-(void)_findLinkedKMLUrl:(NSDictionary *)rootNode linkUrl:(NSString **)linkUrl
{
  NSDictionary *tag, *tag2;
  NSString *tagName;
  
  NSArray *children = [rootNode objectForKey:@"children"];
  for (tag in children) {
    tagName = tag[@"_tag"];
    
    if ([tagName isEqualToString:@"link"]) {
      NSArray *children2 = [tag objectForKey:@"children"];
      for (tag2 in children2) {
        tagName = tag2[@"_tag"];
        if ([tagName isEqualToString:@"href"]) {
          *linkUrl = [tag2 objectForKey:@"href"];
          return;
        }
      }
      continue;
    } else {
      [self _findLinkedKMLUrl:tag linkUrl:linkUrl];
      if (*linkUrl != nil) {
        return;
      }
    }
  }

}

-(void)_filterPlaceMarks:(NSDictionary *)rootNode placemarks:(NSMutableArray **)placemarks
{
  NSDictionary *tag;
  NSString *tagName;
  
  NSArray *children = [rootNode objectForKey:@"children"];
  for (tag in children) {
    tagName = tag[@"_tag"];
    
    if ([tagName isEqualToString:@"placemark"]) {
      [*placemarks addObject:tag];
      continue;
    } else {
      [self _filterPlaceMarks:tag placemarks:placemarks];
    }
  }

}

-(void)_filterStyleTag:(NSDictionary *)rootNode styles:(NSMutableDictionary **)styles
{
  NSDictionary *tag;
  NSString *tagName;
  NSString *styleId;
  
  NSArray *children = [rootNode objectForKey:@"children"];
  for (tag in children) {
    tagName = tag[@"_tag"];
    
    if ([tagName isEqualToString:@"style"]) {
      styleId = tag[@"_id"];
      if (styleId == nil) {
        styleId = @"__default__";
      }
      [*styles setObject:tag[@"children"] forKey:styleId];
      continue;
    } else {
      [self _filterStyleTag:tag styles:styles];
    }
  }
  
}


-(void)_filterStyleMapTag:(NSDictionary *)rootNode styles:(NSMutableDictionary **)styles
{
  NSDictionary *tag;
  NSString *tagName;
  NSString *styleId;
  
  NSArray *children = [rootNode objectForKey:@"children"];
  for (tag in children) {
    tagName = tag[@"_tag"];
    
    if ([tagName isEqualToString:@"stylemap"]) {
      styleId = nil;
      [self _getNormalStyleUrlForStyleMap:tag output:&styleId];
      if (styleId != nil) {
        [*styles setObject:[*styles objectForKey:styleId] forKey:tag[@"_id"]];
      }
      continue;
    } else {
      [self _filterStyleMapTag:tag styles:styles];
    }
  }

}

-(void)_getNormalStyleUrlForStyleMap:(NSDictionary *)rootNode output:(NSString **)output
{
  NSArray *children = [rootNode objectForKey:@"children"];
  NSDictionary *node;
  NSString *tagName;
  bool isNormal = false;
  
  for (node in children) {
    tagName = [node objectForKey:@"_tag"];
    if ([tagName isEqualToString:@"key"]) {
      if ([[node objectForKey:@"key"] isEqualToString:@"normal"]) {
        isNormal = true;
      } else {
        isNormal = false;
      }
    }
    if (isNormal == true) {
      if ([tagName isEqualToString:@"styleurl"] && isNormal == true) {
        *output = [[node objectForKey:@"styleurl"] stringByReplacingOccurrencesOfString:@"#" withString:@""];
        break;
      }
    }
    [self _getNormalStyleUrlForStyleMap:node output:output];
    if (*output != nil) {
      return;
    }
  }
}


-(void)implementPlaceMarkToMap:(NSDictionary *)placeMarker
                            options:(NSMutableDictionary**)options
                            styles:(NSMutableDictionary *)styles
                            styleUrl:(NSString *)styleUrl
                            idPrefix:(NSString *)idPrefix
                            viewportRef:(NSMutableArray **)viewportRef {
  
  NSArray *children = [placeMarker objectForKey:@"children"];
  
  NSDictionary *childNode;
  NSString *tagName;
  NSString *targetClass;
  NSMutableArray *coordinatesList = [NSMutableArray array];
  NSMutableArray *coordinates;
  
  if ([[placeMarker objectForKey:@"_tag"] isEqualToString:@"placemark"]) {
    *options = [NSMutableDictionary dictionary];
  }
  for (childNode in children) {
    tagName = [childNode objectForKey:@"_tag"];
    
    if ([tagName isEqualToString:@"linestring"] ||
        [tagName isEqualToString:@"polygon"]) {
      
      if ([tagName isEqualToString:@"linestring"]) {
        targetClass = @"Polyline";
      } else {
        targetClass = @"Polygon";
      }
      [*options setObject:[NSNumber numberWithBool:true] forKey:@"visible"];
      [*options setObject:[NSNumber numberWithBool:true] forKey:@"geodesic"];
      coordinates = [NSMutableArray array];
      [self _getCoordinates:childNode output:&coordinates];
      if ([coordinates count] > 0) {
        [coordinatesList addObject:coordinates];
      }
    } else if ([tagName isEqualToString:@"styleurl"]) {
      styleUrl = [[childNode objectForKey:@"styleurl"] stringByReplacingOccurrencesOfString:@"#" withString:@""];
    } else if ([tagName isEqualToString:@"point"]) {
      targetClass = @"Marker";
      
      [*options setObject:[NSNumber numberWithBool:true] forKey:@"visible"];
      coordinates = [NSMutableArray array];
      [self _getCoordinates:childNode output:&coordinates];
      
      if ([coordinates count] > 0) {
        [*options setObject:[coordinates objectAtIndex:0] forKey:@"position"];
      }
    } else {
      if ([childNode objectForKey:@"children"]) {
        [self implementPlaceMarkToMap:childNode
              options:options
              styles:styles
              styleUrl:styleUrl
              idPrefix:idPrefix
              viewportRef:viewportRef];
        
      } else if (*options != nil) {
        [*options setObject:[childNode objectForKey:tagName] forKey:tagName];
      }
    }
  }
  if (*options == nil) {
    return;
  }
  
  if (styleUrl == nil) {
    styleUrl = @"__default__";
  }
  NSDictionary *style = [styles objectForKey:styleUrl];
  if (style) {
    [self _applyStyleTag:style options:options targetClass:targetClass];
  }
  
  if ([targetClass isEqualToString:@"Polyline"] ||
      [targetClass isEqualToString:@"Polygon"]) {
    //------------------------------
    // Create a polyline or polygon
    //------------------------------
    for (coordinates in coordinatesList) {
      [*options setObject:coordinates forKey:@"points"];
      
      // Add the latLngs to the default viewport
      [*viewportRef addObjectsFromArray:coordinates];
      
      [self _implementToMap:targetClass
            options:[NSDictionary dictionaryWithDictionary:*options] needJSCallback:YES];
    }
  } else if ([targetClass isEqualToString:@"Marker"]) {
    //-----------------
    // Create a marker
    //-----------------
    NSString *title = @"";
    if ([*options objectForKey:@"name"]) {
      title = [*options objectForKey:@"name"];
    }
    if ([*options objectForKey:@"description"]) {
      if ([title isEqualToString:@""] == false) {
        title = [NSString stringWithFormat:@"%@\n\n", title];
      }
      title = [NSString stringWithFormat:@"%@%@", title, [*options objectForKey:@"description"]];
    }
    [*options setObject:title forKey:@"title"];
    
    // Add the latLng to the default viewport
    [*viewportRef addObject:[*options objectForKey:@"position"]];
    
    [self _implementToMap:targetClass
          options:[NSDictionary dictionaryWithDictionary:*options]
          needJSCallback:YES];
  }
}
-(void)_applyStyleTag:(NSDictionary *)styleElements options:(NSMutableDictionary **)options targetClass:(NSString *)targetClass
{
  
  NSDictionary *node;
  NSString *tagName1, *tagName2;
  NSDictionary *style;
  NSArray *children;
  NSString *value, *prefix = @"";
  for (style in styleElements) {
    
    children = [style objectForKey:@"children"];
    tagName1 = [style objectForKey:@"_tag"];
    
    if ([targetClass isEqualToString:@"Polygon"]) {
      if([tagName1 isEqualToString:@"polystyle"]) {
        prefix = @"fill";
      } else if([tagName1 isEqualToString:@"linestyle"]) {
        prefix = @"stroke";
      }
    }
    
    if ([children count] > 0) {
      for (node in children) {
      
        if ([node objectForKey:@"children"]) {
          [self _applyStyleTag:node[@"children"] options:options targetClass:targetClass];
        } else {
          tagName2 = [node objectForKey:@"_tag"];
          
          
          value = [node valueForKey:tagName2];
          if ([tagName2 isEqualToString:@"color"]) {
            if ([prefix isEqualToString:@""] == false) {
              tagName2 = [NSString stringWithFormat:@"%@Color", prefix];
            }
            [*options setObject:[self _parseKMLColor:value] forKey:tagName2];
          } else {
            if ([prefix isEqualToString:@""] == false) {
              tagName2 = [NSString stringWithFormat:@"%@%@",
              [[tagName2 substringWithRange:NSMakeRange(0, 1)] uppercaseString],
              [tagName2 substringFromIndex:1]];
            }
            [*options setObject:value forKey:[NSString stringWithFormat:@"%@%@", prefix, tagName2]];
          }
        }
      }
    } else if ([targetClass isEqualToString:@"Marker"]) {
      if ([tagName1 isEqualToString:@"href"]) {
        [*options setObject:[style objectForKey:tagName1] forKey:@"icon"];
      }
    }

  }
  
}
-(void)_getCoordinates:(NSDictionary *)rootNode output:(NSMutableArray **)output
{
  NSArray *children = [rootNode objectForKey:@"children"];
  NSDictionary *node;
  NSString *tagName;
  
  for (node in children) {
    tagName = [node objectForKey:@"_tag"];
    if ([tagName isEqualToString:@"coordinates"]) {
      *output = [self _coordinateToLatLngArray:node];
      break;
    } else {
      [self _getCoordinates:node output:output];
      if ([*output count] > 0) {
        return;
      }
    }
  }
}


/**
 * @Private
 * Execute the method of other plugin class internally.
 */
-(void)_execOtherClassMethod:(NSString *)className methodName:(NSString *)methodName options:(NSDictionary *)options callbackId:(NSString *)callbackId
{
  NSArray* args = [NSArray arrayWithObjects:@"exec", options, nil];
  NSArray* jsonArr = [NSArray arrayWithObjects:callbackId, className, methodName, args, nil];
  CDVInvokedUrlCommand* command2 = [CDVInvokedUrlCommand commandFromJson:jsonArr];
  
  CDVPlugin<MyPlgunProtocol> *pluginClass = [self.mapCtrl.plugins objectForKey:className];
  if (!pluginClass) {
    pluginClass = [[NSClassFromString(className)alloc] initWithWebView:self.webView];
    if (pluginClass) {
      pluginClass.commandDelegate = self.commandDelegate;
      [pluginClass setGoogleMapsViewController:self.mapCtrl];
      [self.mapCtrl.plugins setObject:pluginClass forKey:className];
    }
  }
  SEL selector = NSSelectorFromString([NSString stringWithFormat:@"%@:", methodName]);
  if ([pluginClass respondsToSelector:selector]){
    [pluginClass performSelectorOnMainThread:selector withObject:command2 waitUntilDone:NO];
  }
}
-(void)evalJsHelper:(NSString*)jsString
{
  [self.webView stringByEvaluatingJavaScriptFromString:jsString];
}

-(void)_implementToMap:(NSString *)className options:(NSDictionary *)options needJSCallback:(BOOL)needJSCallback
{
  NSString* callbackId = [NSString stringWithFormat:@"%@_%d", className, arc4random()];
  if (needJSCallback == YES){
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:options options:0 error:&error];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    
    //Add callback
    NSString* jsString = [NSString stringWithFormat:@"cordova.callbacks['%@']={'success': function(result) {plugin.google.maps.Map._onKmlEventForIOS('%@',result, %@);}, 'fail': null};", callbackId, self.kmlId, jsonString];
    [self performSelectorOnMainThread:@selector(evalJsHelper:) withObject:jsString waitUntilDone:YES];
  }
  
  [self _execOtherClassMethod:className
        methodName:[NSString stringWithFormat:@"create%@", className]
        options:options
        callbackId:callbackId];

  
}


-(NSMutableArray *)_parseKMLColor:(NSString *)ARGB {
  ARGB = [ARGB stringByReplacingOccurrencesOfString:@"#" withString:@""];
  NSMutableArray *rgbaArray = [NSMutableArray array];
  NSString *hexStr;
  NSString *RGBA = [NSString stringWithFormat:@"%@%@",
                      [ARGB substringWithRange:NSMakeRange(2, 6)],
                      [ARGB substringWithRange:NSMakeRange(0, 2)]
                    ];
  
  unsigned int outVal;
  NSScanner* scanner;
  
  for (int i = 0; i < 8; i+= 2) {
    hexStr = [RGBA substringWithRange:NSMakeRange(i, 2)];
    scanner = [NSScanner scannerWithString:hexStr];
    [scanner scanHexInt:&outVal];
    
    [rgbaArray addObject:[NSNumber numberWithInt:outVal]];
  }
  
  [rgbaArray addObject:[NSNumber numberWithInt:outVal]];
    
  return rgbaArray;
}

-(NSMutableArray *)_coordinateToLatLngArray:(NSDictionary *)coordinateTag {
  NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"[^0-9\\-\\.\\,]+" options:NSRegularExpressionCaseInsensitive error:nil];
  NSMutableArray *coordinates = [NSMutableArray array];
  NSString *coordinateStr = [coordinateTag objectForKey:@"coordinates"];
  coordinateStr = [regex stringByReplacingMatchesInString:coordinateStr options:0 range:NSMakeRange(0, [coordinateStr length]) withTemplate:@"@"];
  NSArray *lngLatArray = [coordinateStr componentsSeparatedByString:@"@"];
  NSString *lngLat;
  NSArray *lngLatAlt;
  for (lngLat in lngLatArray) {
    lngLatAlt = [lngLat componentsSeparatedByString:@","];
    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:lngLatAlt[0] forKey:@"lng"];
    [latLng setObject:lngLatAlt[1] forKey:@"lat"];
    [coordinates addObject:latLng];
  }
  return coordinates;
}


-(NSMutableDictionary *)parseXML:(TBXMLElement *)rootElement
{

  TBXMLElement *childNode = rootElement->firstChild;
  NSString *tagName = [[TBXML elementName:rootElement] lowercaseString];
  NSMutableDictionary *result = [NSMutableDictionary dictionary];
  NSMutableArray *children = [NSMutableArray array];
  result[@"_tag"] = tagName;
  NSString *attrName;
  
  if (childNode) {
    while(childNode) {
      NSMutableDictionary *tmp = [self parseXML:childNode];
      TBXMLAttribute *attribute = childNode->firstAttribute;
      while (attribute) {
        attrName = [NSString stringWithFormat:@"_%@", [[TBXML attributeName:attribute] lowercaseString]];
        tmp[attrName] = [TBXML attributeValue:attribute];
        attribute = attribute->next;
      }
      
      [children addObject: tmp];
      childNode = childNode->nextSibling;
    }
  } else if ([TBXML textForElement:rootElement] != nil) {
      result[tagName] = [TBXML textForElement:rootElement];
  }
  if ([children count] > 0) {
    result[@"children"] = children;
  }
  return result;
}

@end
