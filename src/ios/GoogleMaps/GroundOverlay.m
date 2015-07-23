//
//  GroundOverlay.m
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/4/13.
//
//

#import "GroundOverlay.h"

@implementation GroundOverlay

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createGroundOverlay:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];

  NSArray *points = [json objectForKey:@"bounds"];

  GMSGroundOverlay *layer;
  GMSMutablePath *path = [GMSMutablePath path];
  GMSCoordinateBounds *bounds;

  if (points) {
    //Generate a bounds
    int i = 0;
    NSDictionary *latLng;
    for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
    }
  }
  bounds = [[GMSCoordinateBounds alloc] initWithPath:path];
  layer = [GMSGroundOverlay groundOverlayWithBounds:bounds icon:nil];


  if ([[json valueForKey:@"visible"] boolValue]) {
    layer.map = self.mapCtrl.map;
  }
  if ([json valueForKey:@"zIndex"]) {
    layer.zIndex = [[json valueForKey:@"zIndex"] floatValue];
  }

  __block GroundOverlay *self_ = self;
  MYCompletionHandler callback = ^(NSError *error) {
    
    if ([json valueForKey:@"opacity"]) {
      CGFloat opacity = [[json valueForKey:@"opacity"] floatValue];
      layer.icon = [layer.icon imageByApplyingAlpha:opacity];
    }
    if ([json valueForKey:@"bearing"]) {
      layer.bearing = [[json valueForKey:@"bearing"] floatValue];
    }

    layer.tappable = YES;

    NSString *id = [NSString stringWithFormat:@"groundOverlay_%lu", (unsigned long)layer.hash];
    [self_.mapCtrl.overlayManager setObject:layer forKey: id];
    layer.title = id;

    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
    [result setObject:id forKey:@"id"];
    [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)layer.hash] forKey:@"hashCode"];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

  };

  NSString *urlStr = [json objectForKey:@"url"];
  if (urlStr) {
    [self _setImage:layer urlStr:urlStr completionHandler: callback];
  } else {
    callback(nil);
  }}

- (void)_setImage:(GMSGroundOverlay *)layer urlStr:(NSString *)urlStr completionHandler:(MYCompletionHandler)completionHandler {

  NSString *id = [NSString stringWithFormat:@"groundOverlay_icon_%lu", (unsigned long)layer.hash];
  
  NSError *error;
  NSRange range = [urlStr rangeOfString:@"://"];
  if (range.location == NSNotFound) {
    range = [urlStr rangeOfString:@"www/"];
    if (range.location == NSNotFound) {
      range = [urlStr rangeOfString:@"/"];
      if (range.location != 0) {
        urlStr = [NSString stringWithFormat:@"./%@", urlStr];
      }
    }
  }
  
  range = [urlStr rangeOfString:@"./"];
  if (range.location != NSNotFound) {
    NSString *currentPath = [self.webView.request.URL absoluteString];
    NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"[^\\/]*$" options:NSRegularExpressionCaseInsensitive error:&error];
    currentPath= [regex stringByReplacingMatchesInString:currentPath options:0 range:NSMakeRange(0, [currentPath length]) withTemplate:@""];
    urlStr = [urlStr stringByReplacingOccurrencesOfString:@"./" withString:currentPath];
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
    
  // If there is an error, return
  if (error) {
    completionHandler(error);
    return;
  }

  NSURL *isUrl = [NSURL URLWithString:urlStr];
  if (!isUrl || !isUrl.scheme || isUrl.host) {
    layer.icon = [UIImage imageNamed:urlStr];
  
  //range = [urlStr rangeOfString:@"http://"];
  //if (range.location == NSNotFound) {
  //  layer.icon = [UIImage imageWithContentsOfFile:urlStr];
    [self.mapCtrl.overlayManager setObject:layer.icon forKey: id];
    completionHandler(nil);
  } else {
    dispatch_queue_t gueue = dispatch_queue_create("GoogleMap_createGroundOverlay", NULL);
    dispatch_sync(gueue, ^{
      NSURL *url = [NSURL URLWithString:urlStr];
      NSData *data = [NSData dataWithContentsOfURL:url];
      UIImage *layerImg = [UIImage imageWithData:data];
      layer.icon = layerImg;
      [self.mapCtrl.overlayManager setObject:layerImg forKey: id];
      completionHandler(nil);
    });

  }
}

/**
 * Remove the ground overlay
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  NSString *id = [NSString stringWithFormat:@"groundOverlay_icon_%lu", (unsigned long)layer.hash];
  layer.map = nil;
  [self.mapCtrl removeObjectForKey:key];
  [self.mapCtrl removeObjectForKey:id];
  layer = nil;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
  
  if (isVisible) {
    layer.map = self.mapCtrl.map;
  } else {
    layer.map = nil;
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * set image
 * @params key
 */
-(void)setImage:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  
  NSString *urlStr = [command.arguments objectAtIndex:2];
  if (urlStr) {
    __block GroundOverlay *self_ = self;
    [self _setImage:layer urlStr:urlStr completionHandler:^(NSError *error) {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self_.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
  } else {
  
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }
  
}

/**
 * Set bounds
 * @params key
 */
-(void)setBounds:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  GMSMutablePath *path = [GMSMutablePath path];

  NSArray *points = [command.arguments objectAtIndex:2];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }
  GMSCoordinateBounds *bounds;
  bounds = [[GMSCoordinateBounds alloc] initWithPath:path];
  [layer setBounds:bounds];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set opacity
 * @params key
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
  
  
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  CGFloat opacity = [[command.arguments objectAtIndex:2] floatValue];
  
  NSString *id = [NSString stringWithFormat:@"groundOverlay_icon_%lu", (unsigned long)layer.hash];
  UIImage *icon = [self.mapCtrl getUIImageByKey:id];
  
  layer.icon = [icon imageByApplyingAlpha:opacity];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set bearing
 * @params key
 */
-(void)setBearing:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  layer.bearing = [[command.arguments objectAtIndex:2] floatValue];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *key = [command.arguments objectAtIndex:1];
  GMSGroundOverlay *layer = [self.mapCtrl getGroundOverlayByKey:key];
  NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
  [layer setZIndex:(int)zIndex];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
