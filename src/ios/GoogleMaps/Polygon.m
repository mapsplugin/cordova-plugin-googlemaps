//
//  Polygon.m
//  SimpleMap
//
//  Created by masashi on 11/13/13.
//
//

#import "Polygon.h"
#import "DDPolygon.h"

@implementation Polygon

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

-(void)createPolygon:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];

  GMSMutablePath *path = [GMSMutablePath path];

  NSArray *points = [json objectForKey:@"points"];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }

  // Create the polygon, and assign it to the map.
  //GMSPolygon *polygon = [GMSPolygon polygonWithPath:path];

    DDPolygon *customPolygon = [[DDPolygon alloc] initPolygonWithWithGMSPath:path andMapView:self.mapCtrl.map];
    
  customPolygon.title = @"polygon";
  if ([json valueForKey:@"holes"]) {
    NSArray *holes = [json objectForKey:@"holes"];
    NSArray *latLngArray;
    NSDictionary *latLng;
    int j;
    NSMutableArray *holePaths = [NSMutableArray array];
    
    
    for (i = 0; i < holes.count; i++) {
      latLngArray = [holes objectAtIndex:i];
      GMSMutablePath *holePath = [GMSMutablePath path];
      for (j = 0; j < latLngArray.count; j++) {
        latLng = [latLngArray objectAtIndex:j];
        [holePath addLatitude:[[latLng objectForKey:@"lat"] floatValue] longitude:[[latLng objectForKey:@"lng"] floatValue]];
      }
      [holePaths addObject:holePath];
    }
    customPolygon.holes = holePaths;
  }
  
  if ([[json valueForKey:@"visible"] boolValue]) {
    customPolygon.map = self.mapCtrl.map;
  }
  if ([[json valueForKey:@"geodesic"] boolValue]) {
    customPolygon.geodesic = true;
  }
  NSArray *rgbColor = [json valueForKey:@"fillColor"];
  customPolygon.fillColor = [rgbColor parsePluginColor];

  rgbColor = [json valueForKey:@"strokeColor"];
  customPolygon.strokeColor = [rgbColor parsePluginColor];

  customPolygon.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
  customPolygon.zIndex = [[json valueForKey:@"zIndex"] floatValue];
    
  customPolygon.tappable = YES;

  NSString *id = [NSString stringWithFormat:@"polygon_%lu", (unsigned long)customPolygon.hash];
  [self.mapCtrl.overlayManager setObject:customPolygon forKey: id];
  customPolygon.title = id;


  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:id forKey:@"id"];
  [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)customPolygon.hash] forKey:@"hashCode"];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set holes
 * @params key
 */
-(void)setHoles:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  
  NSArray *holes = [command.arguments objectAtIndex:2];
  NSArray *latLngArray;
  NSDictionary *latLng;
  int i, j;
  NSMutableArray *holePaths = [NSMutableArray array];
  
  
  for (i = 0; i < holes.count; i++) {
    latLngArray = [holes objectAtIndex:i];
    GMSMutablePath *holePath = [GMSMutablePath path];
    for (j = 0; j < latLngArray.count; j++) {
      latLng = [latLngArray objectAtIndex:j];
      [holePath addLatitude:[[latLng objectForKey:@"lat"] floatValue] longitude:[[latLng objectForKey:@"lng"] floatValue]];
    }
    [holePaths addObject:holePath];
  }
  polygon.holes = holePaths;



  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set points
 * @params key
 */
-(void)setPoints:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  GMSMutablePath *path = [GMSMutablePath path];

  NSArray *points = [command.arguments objectAtIndex:2];
  int i = 0;
  NSDictionary *latLng;
  for (i = 0; i < points.count; i++) {
    latLng = [points objectAtIndex:i];
    [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
  }
    
    [polygon updatePath:path];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set fill color
 * @params key
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];

  NSArray *rgbColor = [command.arguments objectAtIndex:2];
  [polygon setFillColor:[rgbColor parsePluginColor]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set stroke color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];

  NSArray *rgbColor = [command.arguments objectAtIndex:2];
  [polygon setStrokeColor:[rgbColor parsePluginColor]];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set stroke width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  float width = [[command.arguments objectAtIndex:2] floatValue];
  [polygon setStrokeWidth:width];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  NSInteger zIndex = [[command.arguments objectAtIndex:2] integerValue];
  [polygon updateZIndex:(int)zIndex];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];
  
    if (isVisible)
  {
      [polygon updateDDPolygonVisibilityInMap:self.mapCtrl.map];
  }
  else
  {
      [polygon updateDDPolygonVisibilityInMap:nil];
  }

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set geodesic
 * @params key
 */
-(void)setGeodesic:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
  Boolean isGeodisic = [[command.arguments objectAtIndex:2] boolValue];
  [polygon setGeodesic:isGeodisic];

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setEditable:(CDVInvokedUrlCommand *)command{
    
    NSString *polygonKey = [command.arguments objectAtIndex:1];
    DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
    Boolean isEditable = [[command.arguments objectAtIndex:2] boolValue];
    [polygon setPolygonEditable:isEditable];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
};

/**
 * Remove the polygon
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *polygonKey = [command.arguments objectAtIndex:1];
  DDPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
    
    [polygon removeFromMap];
    
  polygon.map = nil;
  [self.mapCtrl removeObjectForKey:polygonKey];
  polygon = nil;

  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
