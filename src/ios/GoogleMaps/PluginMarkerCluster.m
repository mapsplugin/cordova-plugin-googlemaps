//
//  Marker.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "PluginMarkerCluster.h"
@implementation PluginMarkerCluster

const NSString *GEOCELL_ALPHABET = @"0123456789abcdef";
const int GEOCELL_GRID_SIZE = 4;

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
    self.mapCtrl = viewCtrl;
}

- (void)pluginInitialize
{
  // Initialize this plugin
  self.imgCache = [[NSCache alloc]init];
  self.imgCache.totalCostLimit = 3 * 1024 * 1024 * 1024; // 3MB = Cache for image
  self.executeQueue =  [NSOperationQueue new];
  self.objects = [[NSMutableDictionary alloc] init];
  self._pluginResults = [[NSMutableDictionary alloc] init];  self.resolutions = [NSMutableDictionary dictionary];
  self.pluginMarkers = [NSMutableDictionary dictionary];
}

- (void)pluginUnload
{

  if (self.executeQueue != nil){
      self.executeQueue.suspended = YES;
      [self.executeQueue cancelAllOperations];
      self.executeQueue.suspended = NO;
      self.executeQueue = nil;
  }

  // Plugin destroy
  NSArray *keys = [self.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;

  keys = [self._pluginResults allKeys];
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      [self._pluginResults removeObjectForKey:key];
  }
  self._pluginResults = nil;
}

- (void)onHookedPluginResult:(CDVPluginResult*)pluginResult callbackId:(NSString*)callbackId {
  NSArray *tmp = [callbackId componentsSeparatedByString:@"/"];
  NSString *geocell = [tmp objectAtIndex:4];
  NSString *clusterId = [tmp objectAtIndex:3];
  NSLog(@"tmp = %@, geocell = %@, clusterId = %@", tmp, geocell, clusterId);
  int currentCellLength = [[self.resolutions objectForKey:clusterId] intValue] + 1;

  NSDictionary *result = pluginResult.message;
  NSString *markerId = [result objectForKey:@"id"];

  if (geocell.length != currentCellLength) {

    NSMutableArray *args2 = [[NSMutableArray alloc] init];
    [args2 setObject:markerId atIndexedSubscript:0];


    CDVInvokedUrlCommand *command2 = [[CDVInvokedUrlCommand alloc]
                                      initWithArguments:args2
                                      callbackId: @"INVALID"
                                      className:@"PluginMarker"
                                      methodName:@"remove"];
    NSString *pluginName = [NSString stringWithFormat:@"%@-marker", self.mapCtrl.mapId];


    PluginMarker *pluginMarker = [self.commandDelegate getCommandInstance:pluginName];
    [pluginMarker remove:command2];

  }

  NSString *clusterId_geocell = [NSString stringWithFormat:@"%@-%@", clusterId, geocell];
  [self.pluginMarkers setObject:markerId forKey:clusterId_geocell];
}


- (void)create:(CDVInvokedUrlCommand*)command {
  NSString *clusterId = [NSString stringWithFormat:@"markercluster_%lu", command.hash];
  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:clusterId forKey:@"id"];
  [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)command.hash] forKey:@"hashCode"];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setResolution:(CDVInvokedUrlCommand*)command {
  NSString *clusterId = [command.arguments objectAtIndex:0];
  int resolution = [[command.arguments objectAtIndex:1] intValue];
  int currentCellLength = resolution + 1;


  @synchronized(self.resolutions) {
    [self.resolutions setObject:[NSNumber numberWithInt:resolution] forKey:clusterId];
  }

  NSString *clusterId_geocell;
  NSArray *keys = [self.pluginMarkers allKeys];
  NSArray *tmp;
  NSString *geocell;

  for (int i = 0; i < [keys count]; i++) {
    clusterId_geocell = [keys objectAtIndex:i];
    tmp = [clusterId_geocell componentsSeparatedByString:@"-"];
    geocell = [tmp objectAtIndex:1];
    if (geocell == nil || geocell.length == currentCellLength) {
      continue;
    }
    [self.executeQueue addOperationWithBlock:[self deleteOldClusterTaskWithKey:clusterId_geocell]];
  }

  CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];


}
- (void)deleteClusters:(CDVInvokedUrlCommand*)command {


  NSArray *geocellList = [command.arguments objectAtIndex:1];
  if ([geocellList count] == 0) {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    return;
  }

  NSString *clusterId = [command.arguments objectAtIndex:0];
  NSString *geocell, *clusterId_geocell;


  for (int i = 0; i < [geocellList count]; i++) {
    geocell = [geocellList objectAtIndex:i];
    clusterId_geocell = [NSString stringWithFormat:@"%@-%@", clusterId, geocell];
    [self.executeQueue addOperationWithBlock:[self deleteOldClusterTaskWithKey:clusterId_geocell]];

  }

  CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];

}
- (void (^)())deleteOldClusterTaskWithKey:(NSString *)clusterId_geocell {
  return ^{
    NSString *markerId = nil;
    //
    markerId = [self.pluginMarkers objectForKey:clusterId_geocell];
    [self.pluginMarkers removeObjectForKey:clusterId_geocell];

    if (markerId == nil) {
      return;
    }

    NSMutableArray *args = [[NSMutableArray alloc] init];
    [args setObject:markerId atIndexedSubscript:0];

    CDVInvokedUrlCommand *command = [[CDVInvokedUrlCommand alloc]
                                    initWithArguments:args
                                    callbackId: @"INVALID"
                                    className:@"PluginMarker"
                                    methodName:@"remove"];

    NSString *pluginName = [NSString stringWithFormat:@"%@-marker", self.mapCtrl.mapId];
    PluginMarker *pluginMarker = [self.commandDelegate getCommandInstance:pluginName];
    [pluginMarker remove:command];
  };
}

- (void)redrawClusters:(CDVInvokedUrlCommand*)command {


  NSArray *clusters = [command.arguments objectAtIndex:1];
  if ([clusters count] == 0) {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    return;
  }

  NSString *clusterId = [command.arguments objectAtIndex:0];
  int resolution = [[self.resolutions objectForKey:clusterId] integerValue];
  int currentCellLength = resolution + 1;
  NSLog(@"--->redrawClusters");

  //--------
  // delete old clusters
  //--------

  NSString *clusterId_geocell;
  NSArray *keys = [self.pluginMarkers allKeys];
  NSArray *tmp;
  NSString *geocell;

  if ([keys count] > 0) {
    for (int i = 0; i < [keys count]; i++) {
      clusterId_geocell = [keys objectAtIndex:i];
      tmp = [clusterId_geocell componentsSeparatedByString:@"-"];
      geocell = [tmp objectAtIndex:1];
      if (geocell == nil || geocell.length == currentCellLength) {
        continue;
      }
      [self deleteOldClusterTaskWithKey:clusterId_geocell];
    }
  }


  NSString *firstCell = [[clusters objectAtIndex:0] objectForKey:@"geocell"];
  if (currentCellLength == firstCell.length) {

    NSDictionary *clusterData;
    for (int i = 0; i < [clusters count]; i++) {
      clusterData = [clusters objectAtIndex:i];
      [self.executeQueue addOperationWithBlock:[self createClusterTaskWithClusterId:clusterId clusterData:clusterData]];

    }

  }


  CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];

}

- (void (^)())createClusterTaskWithClusterId:(NSString*)clusterId clusterData:(NSDictionary *)clusterData {
  NSString *geocell = [clusterData objectForKey:@"geocell"];
  int itemCnt = [[clusterData objectForKey:@"count"] intValue];
  NSString *cluster_geocell = [NSString stringWithFormat:@"%@-%@", clusterId, geocell];

  GMSCoordinateBounds *bounds = [self computeBox:geocell];
  NSMutableDictionary *position = [[NSMutableDictionary alloc] init];
  [position setObject:[NSNumber numberWithFloat:bounds.center.latitude] forKey:@"lat"];
  [position setObject:[NSNumber numberWithFloat:bounds.center.longitude] forKey:@"lng"];

  NSMutableDictionary *markerOpts = [[NSMutableDictionary alloc] init];
  [markerOpts setObject:position forKey:@"position"];
  [markerOpts setObject:[NSString stringWithFormat:@"%@-%@", clusterId, geocell] forKey:@"title"];
  [markerOpts setObject:@"true" forKey:@"visible"];
  [markerOpts setObject:[NSString
    stringWithFormat:@"https://mt.google.com/vt/icon/text=%lu&psize=16&font=fonts/arialuni_t.ttf&color=ff330000&name=icons/spotlight/spotlight-waypoint-b.png&ax=44&ay=48&scale=1",
      (unsigned long)geocell.length]
    forKey:@"icon"];

  NSMutableArray *args = [[NSMutableArray alloc] init];
  [args setObject:@"Marker" atIndexedSubscript:0];
  [args setObject:markerOpts atIndexedSubscript:1];

  NSString *pluginId = [NSString stringWithFormat:@"%@-markercluster", self.mapCtrl.mapId];
  NSString *callbackId = [NSString stringWithFormat:@"%@://create/%@/%@", pluginId, clusterId, geocell];
  CDVInvokedUrlCommand *command2 = [[CDVInvokedUrlCommand alloc]
                                    initWithArguments:args
                                    callbackId:callbackId
                                    className:@"PluginMap"
                                    methodName:@"loadPlugin"];


  return ^() {
    if ([self.pluginMarkers objectForKey:cluster_geocell] != nil) {
      return;
    }

    //------------------
    // Create a marker
    //------------------
    PluginMap *pluginMap = [self.commandDelegate getCommandInstance:self.mapCtrl.mapId];
    [pluginMap loadPlugin:command2];
  };
}

- (GMSCoordinateBounds *)computeBox:(NSString *) geocell {
  NSString *geoChar;
  double north = 90.0;
  double south = -90.0;
  double east = 180.0;
  double west = -180.0;

  double subcell_lng_span, subcell_lat_span;
  int x, y, pos;
  NSRange range;

  for (int i = 0; i < [geocell length]; i++) {
    geoChar = [geocell substringWithRange:NSMakeRange(i, 1)];
    range = [GEOCELL_ALPHABET rangeOfString:geoChar];
    pos = (int)range.location;

    subcell_lng_span = (east - west) / GEOCELL_GRID_SIZE;
    subcell_lat_span = (north - south) / GEOCELL_GRID_SIZE;

    x = (int) ((int)floor(pos / 4) % 2 * 2 + pos % 2);
    y = (int) (pos - floor(pos / 4) * 4);
    y = y >> 1;
    y += floor(pos / 4) > 1 ? 2 : 0;

    south += subcell_lat_span * y;
    north = south + subcell_lat_span;

    west += subcell_lng_span * x;
    east = west + subcell_lng_span;
  }

  GMSMutablePath *mutablePath = [[GMSMutablePath alloc] init];
  [mutablePath addCoordinate:CLLocationCoordinate2DMake(south, west)];
  [mutablePath addCoordinate:CLLocationCoordinate2DMake(north, east)];

  return [[GMSCoordinateBounds alloc] initWithPath:mutablePath];
}

@end
