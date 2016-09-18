//
//  Polygon.m
//  SimpleMap
//
//  Created by masashi on 11/13/13.
//
//

#import "PluginPolygon.h"

@implementation PluginPolygon

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}
- (void)pluginUnload
{
}

-(void)create:(CDVInvokedUrlCommand *)command
{
/*
  // Initialize this plugin
  if (self.mapCtrl == nil) {
    CDVViewController *cdvViewController = (CDVViewController*)self.viewController;
    CordovaGoogleMaps *googlemaps = [cdvViewController getCommandInstance:@"CordovaGoogleMaps"];
    //self.mapCtrl = googlemaps.mapCtrl;
  }


  [self.commandDelegate runInBackground:^{
    // Parse the polygonOptions
    NSDictionary *json = [command.arguments objectAtIndex:0];

    GMSMutablePath *path = [GMSMutablePath path];
    NSArray *points = [json objectForKey:@"points"];
    int i = 0;
    NSDictionary *latLng;
    for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
    }

    // Create paths of the hole property if specified.
    NSMutableArray *holePaths = nil;
    if ([json valueForKey:@"holes"]) {
      NSArray *holes = [json objectForKey:@"holes"];
      NSArray *latLngArray;
      NSDictionary *latLng;
      int j;
      holePaths = [NSMutableArray array];


      for (i = 0; i < holes.count; i++) {
        latLngArray = [holes objectAtIndex:i];
        GMSMutablePath *holePath = [GMSMutablePath path];
        for (j = 0; j < latLngArray.count; j++) {
          latLng = [latLngArray objectAtIndex:j];
          [holePath addLatitude:[[latLng objectForKey:@"lat"] floatValue] longitude:[[latLng objectForKey:@"lng"] floatValue]];
        }
        [holePaths addObject:holePath];
      }
    }

    dispatch_sync(dispatch_get_main_queue(), ^{

      // Create the polygon, and assign it to the map on UI thread.
      GMSPolygon *polygon = [GMSPolygon polygonWithPath:path];
      polygon.title = @"polygon";

      if (holePaths != nil) {
        polygon.holes = holePaths;
      }

      if ([[json valueForKey:@"visible"] boolValue]) {
        //polygon.map = self.mapCtrl.map;
      }
      if ([[json valueForKey:@"geodesic"] boolValue]) {
        polygon.geodesic = true;
      }
      NSArray *rgbColor = [json valueForKey:@"fillColor"];
      polygon.fillColor = [rgbColor parsePluginColor];

      rgbColor = [json valueForKey:@"strokeColor"];
      polygon.strokeColor = [rgbColor parsePluginColor];

      polygon.strokeWidth = [[json valueForKey:@"strokeWidth"] floatValue];
      polygon.zIndex = [[json valueForKey:@"zIndex"] floatValue];

      polygon.tappable = YES;


      // Register polygon to the overlayManager.
      dispatch_async(dispatch_get_main_queue(), ^{
        NSString *id = [NSString stringWithFormat:@"polygon_%lu", (unsigned long)polygon.hash];
        [self.mapCtrl.overlayManager setObject:polygon forKey: id];
        polygon.title = id;


        NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
        [result setObject:id forKey:@"id"];
        [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)polygon.hash] forKey:@"hashCode"];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      });

    });

  }];
*/
}


/**
 * Set holes
 * @params key
 */
-(void)setHoles:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{

    // Obtain the polygon matched with the id.
    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];

    // Create holes.
    NSArray *holes = [command.arguments objectAtIndex:1];
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

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      polygon.holes = holePaths;

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });

  }];
*/
}


/**
 * Set points
 * @params key
 */
-(void)setPoints:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{
    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
    GMSMutablePath *path = [GMSMutablePath path];

    // Parse the option.
    NSArray *points = [command.arguments objectAtIndex:1];
    int i = 0;
    NSDictionary *latLng;
    for (i = 0; i < points.count; i++) {
      latLng = [points objectAtIndex:i];
      [path addCoordinate:CLLocationCoordinate2DMake([[latLng objectForKey:@"lat"] floatValue], [[latLng objectForKey:@"lng"] floatValue])];
    }

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      [polygon setPath:path];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });


  }];
*/
}
/**
 * Set fill color
 * @params key
 */
-(void)setFillColor:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{

    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];

    NSArray *rgbColor = [command.arguments objectAtIndex:1];

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      [polygon setFillColor:[rgbColor parsePluginColor]];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
  }];
*/
}


/**
 * Set stroke color
 * @params key
 */
-(void)setStrokeColor:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{
    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];

    NSArray *rgbColor = [command.arguments objectAtIndex:1];

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      [polygon setStrokeColor:[rgbColor parsePluginColor]];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
  }];
*/
}

/**
 * Set stroke width
 * @params key
 */
-(void)setStrokeWidth:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{
    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
    float width = [[command.arguments objectAtIndex:1] floatValue];

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      [polygon setStrokeWidth:width];
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
  }];
*/
}

/**
 * Set z-index
 * @params key
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{

    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
    NSInteger zIndex = [[command.arguments objectAtIndex:1] integerValue];

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      [polygon setZIndex:(int)zIndex];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
  }];
*/
}

/**
 * Set visibility
 * @params key
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{

    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
    Boolean isVisible = [[command.arguments objectAtIndex:1] boolValue];

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      if (isVisible) {
        //polygon.map = self.mapCtrl.map;
      } else {
        polygon.map = nil;
      }

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
  }];
*/
}
/**
 * Set geodesic
 * @params key
 */
-(void)setGeodesic:(CDVInvokedUrlCommand *)command
{
/*
  [self.commandDelegate runInBackground:^{

    NSString *polygonKey = [command.arguments objectAtIndex:0];
    GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
    Boolean isGeodisic = [[command.arguments objectAtIndex:1] boolValue];

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      [polygon setGeodesic:isGeodisic];

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
  }];
*/
}

/**
 * Remove the polygon
 * @params key
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
/*
    [self.commandDelegate runInBackground:^{

    NSString *polygonKey = [command.arguments objectAtIndex:0];
    [self.mapCtrl removeObjectForKey:polygonKey];

    // Apply to the polygon on UI thread.
    dispatch_sync(dispatch_get_main_queue(), ^{
      GMSPolygon *polygon = [self.mapCtrl getPolygonByKey: polygonKey];
      polygon.map = nil;
      polygon = nil;

      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    });
  }];
  */
}

@end
