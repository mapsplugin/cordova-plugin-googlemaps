//
//  PluginKmlOverlay.m
//  cordova-googlemaps-plugin v2
//
//  Created by Katsumata Masashi.
//
//

#import "PluginKmlOverlay.h"


@implementation PluginKmlOverlay

-(void)setPluginViewController:(PluginViewController *)viewCtrl
{
    self.mapCtrl = (PluginMapViewController *)viewCtrl;
}
- (void)pluginUnload
{
}

-(void)create:(CDVInvokedUrlCommand *)command
{

  [[NSOperationQueue mainQueue] addOperationWithBlock:^{
    NSDictionary *json = [command.arguments objectAtIndex:1];

    NSError *error;

    NSString *urlStr = [json objectForKey:@"url"];
    NSRange range;
    if (![urlStr hasPrefix:@"http"]) {

      if (![urlStr containsString:@"://"] &&
          ![urlStr hasPrefix:@"/"] &&
          ![urlStr hasPrefix:@"www"] &&
          ![urlStr hasPrefix:@"./"] &&
          ![urlStr hasPrefix:@"../"]) {
        urlStr = [NSString stringWithFormat:@"./%@", urlStr];
      }

      if ([urlStr hasPrefix:@"./"] || [urlStr hasPrefix:@"../"]) {
        NSError *error = nil;

        // replace repeated "./" (i.e ./././test.png)
        NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"(\\.\\/)+" options:NSRegularExpressionCaseInsensitive error:&error];
        urlStr = [regex stringByReplacingMatchesInString:urlStr options:0 range:NSMakeRange(0, [urlStr length]) withTemplate:@"./"];


        // Get the current URL, then calculate the relative path.
        CDVViewController *cdvViewController = (CDVViewController*)self.viewController;

        id webview = cdvViewController.webView;
        NSString *clsName = [webview className];
        NSURL *url;
        NSString *currentURL;
        if ([clsName isEqualToString:@"UIWebView"]) {
          //------------------------------------------
          // UIWebView
          //------------------------------------------
          url = ((UIWebView *)cdvViewController.webView).request.URL;
          currentURL = url.absoluteString;

        } else {
          //------------------------------------------
          // WKWebView
          //------------------------------------------
          NSURL *url = [webview URL];
          currentURL = url.absoluteString;
          if (![[url lastPathComponent] isEqualToString:@"/"]) {
            currentURL = [currentURL stringByReplacingOccurrencesOfString:[url lastPathComponent] withString:@""];
          }
        }
        // remove page unchor (i.e index.html#page=test, index.html?key=value)
        regex = [NSRegularExpression regularExpressionWithPattern:@"[#\\?].*$" options:NSRegularExpressionCaseInsensitive error:&error];
        currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];

        // remove file name (i.e /index.html)
        regex = [NSRegularExpression regularExpressionWithPattern:@"\\/[^\\/]+\\.[^\\/]+$" options:NSRegularExpressionCaseInsensitive error:&error];
        currentURL = [regex stringByReplacingMatchesInString:currentURL options:0 range:NSMakeRange(0, [currentURL length]) withTemplate:@""];

        if (![currentURL hasSuffix:@"/"]) {
          currentURL = [NSString stringWithFormat:@"%@/", currentURL];
        }
        urlStr = [NSString stringWithFormat:@"%@%@", currentURL, urlStr];

        // remove file name (i.e /index.html)
        regex = [NSRegularExpression regularExpressionWithPattern:@"(\\/\\.\\/+)+" options:NSRegularExpressionCaseInsensitive error:&error];
        urlStr = [regex stringByReplacingMatchesInString:urlStr options:0 range:NSMakeRange(0, [urlStr length]) withTemplate:@"/"];

        urlStr = [urlStr stringByReplacingOccurrencesOfString:@"%20" withString:@" "];

        if (self.mapCtrl.debuggable) {
          NSLog(@"urlStr = %@", urlStr);
        }
      }
      range = [urlStr rangeOfString:@"cdvfile://"];
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
    }
    if (self.mapCtrl.debuggable) {
      NSLog(@"urlStr = %@", urlStr);
    }

    [self.mapCtrl.executeQueue addOperationWithBlock:^{
      [self loadKml:urlStr  completionBlock:^(BOOL succeeded, id result) {
        CDVPluginResult* pluginResult;
        if (succeeded) {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:(NSDictionary *)result];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
      }];

    }];

  }];

}

- (void)loadKml:(NSString *)urlStr completionBlock:(void (^)(BOOL succeeded, id result))completionBlock {
  urlStr = [urlStr stringByReplacingOccurrencesOfString:@" " withString:@"%20"];
  NSError *error;
  TBXML *tbxml = [TBXML alloc];// initWithXMLFile:urlStr error:&error];
  if ([urlStr hasPrefix:@"http://"] || [urlStr hasPrefix:@"https://"]) {
      NSURLRequest *req = [NSURLRequest requestWithURL:[NSURL URLWithString:urlStr]];
      bool valid = [NSURLConnection canHandleRequest:req];
      if (valid) {
          NSURLSessionConfiguration *sessionConfiguration = [NSURLSessionConfiguration defaultSessionConfiguration];
          NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfiguration];
          NSURLSessionDataTask *getTask = [session dataTaskWithRequest:req
                                                     completionHandler:^(NSData *data, NSURLResponse *res, NSError *error) {
                                                       [session finishTasksAndInvalidate];
                                                       
                                                       if (error) {
                                                          NSMutableDictionary* details = [NSMutableDictionary dictionary];
                                                          [details setValue:[NSString stringWithFormat:@"Cannot load KML data from %@", urlStr] forKey:NSLocalizedDescriptionKey];
                                                          error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
                                                          completionBlock(NO, error);
                                                          return;
                                                       }
                                                       
                                                        TBXML *tbxml = [TBXML alloc];
                                                        tbxml = [tbxml initWithXMLData:data error:&error];
                                                        NSDictionary *result = [self parseXmlWithTbXml:tbxml];
                                                        completionBlock(YES, result);

                                                     }];
          [getTask resume];
      } else {

          NSMutableDictionary* details = [NSMutableDictionary dictionary];
          [details setValue:[NSString stringWithFormat:@"Cannot load KML data from %@", urlStr] forKey:NSLocalizedDescriptionKey];
          error = [NSError errorWithDomain:@"world" code:200 userInfo:details];
          completionBlock(NO, error);
      }
  } else {
      tbxml = [tbxml initWithXMLFile:urlStr error:&error];
      NSDictionary *result = [self parseXmlWithTbXml:tbxml];
      completionBlock(YES, result);
  }


}
- (NSDictionary *)parseXmlWithTbXml:(TBXML *)tbxml {
  
  KmlParseClass *parser = [[KmlParseClass alloc] init];
  NSMutableDictionary *root = [parser parseXml:tbxml rootElement:tbxml.rootXMLElement];

  NSMutableDictionary *result = [NSMutableDictionary dictionary];
  [result setObject:root forKey:@"root"];
  [result setObject:parser.schemaHolder forKey:@"schemas"];
  [result setObject:parser.styleHolder forKey:@"styles"];
  return result;
}
@end


//------------------------------------------
// @private
// KmlParseClass
//------------------------------------------
@implementation KmlParseClass

-(instancetype)init {
  self = [super init];
  self.styleHolder = [NSMutableDictionary dictionary];
  self.schemaHolder = [NSMutableDictionary dictionary];
  return self;
}

-(NSMutableDictionary *)parseXml:(TBXML *)tbxml rootElement:(TBXMLElement *)rootElement
{

  NSMutableDictionary *result = [NSMutableDictionary dictionary];
  NSString *tagName = [[TBXML elementName:rootElement] lowercaseString];
  NSString *styleId, *schemaId, *txt;

  //NSLog(@"---> tagName = %@", tagName);
  [result setObject:tagName forKey:@"tagName"];

  NSString *attrName, *value;
  TBXMLAttribute *attribute = rootElement->firstAttribute;
  while (attribute) {
    attrName = [TBXML attributeName:attribute];
    value = [TBXML attributeValue:attribute];
    [result setObject:value forKey:attrName];
    attribute = attribute->next;
  }

  if ([@"styleurl" isEqualToString:tagName]) {
    styleId = [TBXML textForElement:rootElement];
    [result setObject:styleId forKey:@"styleId"];
    return result;

  }


  if ([@"style" isEqualToString:tagName] || [@"stylemap" isEqualToString:tagName]) {
    // Generate a style id for the tag
    styleId = [TBXML valueOfAttributeNamed:@"id" forElement:rootElement];
    if (styleId == nil || [styleId isEqualToString:@""]) {
      styleId = [NSString stringWithFormat:@"__%ld%d__", tagName.hash, rand() * RAND_MAX];
    }
    [result setObject:styleId forKey:@"styleId"];

    // Store style information into the styleHolder.
    NSMutableDictionary *styles = [NSMutableDictionary dictionary];
    NSMutableArray *children = [NSMutableArray array];
    TBXMLElement *childNode = rootElement->firstChild;
    while (childNode) {

      NSMutableDictionary *node = [self parseXml:tbxml rootElement:childNode];
      if (node) {
        if ([node objectForKey:@"value"]) {
          [result setObject:[node objectForKey:@"value"] forKey:[node objectForKey:@"tagName"]];
        } else {
          [children addObject:node];
        }
      }
      childNode = childNode->nextSibling;
    }
    if ([children count] > 0) {
      [styles setObject:children forKey:@"children"];
    }
    [self.styleHolder setObject:styles forKey:styleId];
    return result;

  }


  if ([@"schema" isEqualToString:tagName]) {
    // Generate a schema id for the tag
    schemaId = [TBXML valueOfAttributeNamed:@"id" forElement:rootElement];
    if (schemaId == nil || [schemaId isEqualToString:@""]) {
      schemaId = [NSString stringWithFormat:@"__%ld%d__", tagName.hash, rand() * RAND_MAX];
    }

    // Store schema information into the schemaHolder.
    NSMutableDictionary *schema = [NSMutableDictionary dictionary];
    [schema setObject:[TBXML valueOfAttributeNamed:@"name" forElement:rootElement] forKey:@"name"];

    NSMutableArray *children = [NSMutableArray array];
    TBXMLElement *childNode = rootElement->firstChild;
    while (childNode) {

      NSMutableDictionary *node = [self parseXml:tbxml rootElement:childNode];
      if (node) {
        [children addObject:node];
      }
      childNode = childNode->nextSibling;
    }
    if ([children count] > 0) {
      [schema setObject:children forKey:@"children"];
    }
    [self.schemaHolder setObject:schema forKey:schemaId];
    return result;
  }

  if ([@"coordinates" isEqualToString:tagName]) {
    NSMutableArray *latLngList = [NSMutableArray array];
    txt = [TBXML textForElement:rootElement];
    txt = [txt regReplace:@"\\s+" replaceTxt:@"\n" options:0];
    txt = [txt regReplace:@"\\n+" replaceTxt:@"\n" options:0];
    NSArray<NSString *> *lines = [txt componentsSeparatedByString:@"\n"];
    NSArray<NSString *> *tmpArry;
    NSMutableDictionary *latLng;
    NSString *tmp;
    for (int i = 0; i < lines.count; i++) {
      tmp = [lines objectAtIndex:i];
      tmp = [tmp regReplace:@"[^0-9,.\\-]" replaceTxt:@"" options:0];
      if ([tmp isEqualToString:@""] == NO) {
        tmpArry = [tmp componentsSeparatedByString:@","];
        latLng = [NSMutableDictionary dictionary];
        [latLng setObject:[tmpArry objectAtIndex:1] forKey:@"lat"];
        [latLng setObject:[tmpArry objectAtIndex:0] forKey:@"lng"];
        [latLngList addObject:latLng];
      }
    }

    [result setObject:latLngList forKey:tagName];
    return result;
  }


  TBXMLElement *childNode = rootElement->firstChild;
  NSMutableArray<NSString *> *styleIDs;
  if (childNode) {
    NSMutableArray *children = [NSMutableArray array];
    while(childNode) {
      NSMutableDictionary *node = [self parseXml:tbxml rootElement:childNode];
      if (node) {
        if ([node objectForKey:@"styleId"]) {
          styleIDs = [result objectForKey:@"styleIDs"];
          if (styleIDs == nil) {
            styleIDs = [NSMutableArray array];
          }
          [styleIDs addObject:[node objectForKey:@"styleId"]];
          [result setObject:styleIDs forKey:@"styleIDs"];
        } else if ([@"schema" isEqualToString:[node objectForKey:@"tagName"]] == NO) {
          [children addObject:node];
        }
      }
      childNode = childNode->nextSibling;
    }
    [result setObject:children forKey:@"children"];
  } else {
    [result setObject:[TBXML textForElement:rootElement] forKey:@"value"];
  }
  return result;
}


@end
