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
  
  NSString *urlStr = [json objectForKey:@"url"];
  
  //--------------------------------
  // Parse the kml file
  //--------------------------------
  NSError *error = nil;
  TBXML *tbxml = [[TBXML alloc] initWithXMLFile:urlStr error:&error];
  CDVPluginResult* pluginResult;
  if (error) {
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    return;
  }
  NSMutableDictionary *kmlData = [self parseXML:tbxml.rootXMLElement];
  
  //--------------------------------
  // Separate styles and placemarks
  //--------------------------------
  NSMutableDictionary *kmlTag = kmlData[@"children"][0];
  NSMutableDictionary *documentTag = kmlTag[@"children"];
  
  NSMutableDictionary *styles = [NSMutableDictionary dictionary];
  NSMutableArray *placeMarks = [NSMutableArray array];
  NSDictionary *tag;
  NSString *tagName;
  NSString *styleId;
  
  for (tag in documentTag) {
    tagName = tag[@"_tag"];
    if ([tagName isEqualToString:@"style"]) {
      styleId = tag[@"_id"];
      if (styleId != nil) {
        styles[styleId] = tag[@"children"];
      }
    }

    if ([tagName isEqualToString:@"placemark"]) {
      [placeMarks addObject:tag];
    }
  }
  NSLog(@"---implement start");
  for (tag in placeMarks) {
    [self implementPlaceMarkToMap:tag styles:styles];
  }
  
  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void)implementPlaceMarkToMap:(NSDictionary *)placeMarker styles:(NSMutableDictionary *)styles {
  NSArray *children = [placeMarker objectForKey:@"children"];
  
  NSDictionary *childNode, *node, *styleNode;
  NSMutableDictionary *options = [NSMutableDictionary dictionary];
  NSString *tagName, *tagName2, *tagName3;
  NSArray *tmpArray, *tmpArray2;
  NSString *className = nil;
  
  [options setObject:[NSNumber numberWithBool:true] forKey:@"visible"];
  
  for (childNode in children) {
    tagName = [childNode objectForKey:@"_tag"];
    
    if ([tagName isEqualToString:@"linestring"]) {
      className = @"Polyline";
      [options setObject:[NSNumber numberWithBool:true] forKey:@"geodesic"];
      tmpArray = [childNode objectForKey:@"children"];
      for (node in tmpArray) {
        tagName2 = [node objectForKey:@"_tag"];
        if ([tagName2 isEqualToString:@"coordinates"]) {
          [options setObject:[self _coordinateToLatLngArray:node] forKey:@"points"];
          break;
        }
      }
    }
    
    if ([tagName isEqualToString:@"styleurl"]) {
      NSString *styleUrl = [[childNode objectForKey:@"styleurl"] stringByReplacingOccurrencesOfString:@"#" withString:@""];
      tmpArray = [styles objectForKey:styleUrl];
      for (node in tmpArray) {
        tagName2 = [node objectForKey:@"_tag"];
        tmpArray2 = [node objectForKey:@"children"];
        
        for (styleNode in tmpArray2) {
          tagName3 = [styleNode objectForKey:@"_tag"];
          [options setObject:[styleNode objectForKey:tagName3] forKey:[NSString stringWithFormat:@"%@%@", tagName2, tagName3]];
        }
      }
      
    }
  }
  
  
  NSMutableDictionary *options2 = [NSMutableDictionary dictionary];
  NSString *optionName, *optionName2;
  id optionValue;
  for (optionName in options) {
    optionValue = options[optionName];
    
    if ([className isEqualToString:@"Polyline"]) {
      optionName2 = [optionName stringByReplacingOccurrencesOfString:@"linestyle" withString:@""];
      if ([optionName2 isEqualToString:@"color"]) {
        optionValue = [self _parseKMLColor:optionValue];
      }
      
      if ([optionName rangeOfString:@"linestyle"].location != NSNotFound) {
        [options2 setObject:optionValue forKey:optionName2];
        continue;
      }
    }
    [options2 setObject:[options objectForKey:optionName] forKey:optionName];
  }
  NSLog(@"%@", options2);

  [self _callOtherMethod:className options:options2];
  
}

-(void)_callOtherMethod:(NSString *)className options:(NSMutableDictionary *)options
{
  NSArray* args = [NSArray arrayWithObjects:@"exec", options, nil];
  NSArray* jsonArr = [NSArray arrayWithObjects:@"callbackId", @"className", @"methodName", args, nil];
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
  NSLog(@"class=%@", pluginClass);
  SEL selector = NSSelectorFromString([NSString stringWithFormat:@"create%@:", className]);
  NSLog(@"selector=%hhd", [pluginClass respondsToSelector:selector]);
  if ([pluginClass respondsToSelector:selector]){
    [pluginClass performSelectorOnMainThread:selector withObject:command2 waitUntilDone:NO];
  }
  NSLog(@"done");
}

-(NSMutableArray *)_parseKMLColor:(NSString *)ARGB {
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

/**
 * Remove the kml overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  layer.map = nil;
  [self.mapCtrl removeObjectForKey:key];
  layer = nil;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
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
