//
//  PluginMarkerCluster.m
//  cordova-googlemaps-plugin
//
//  Created by masashi.
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
  if (self.objects) {
    return;
  }
  // Initialize this plugin
  self.objects = [[NSMutableDictionary alloc] init];
  self.waitCntManager = [NSMutableDictionary dictionary];
  self.pluginMarkers = [NSMutableDictionary dictionary];
  self.debugFlags = [NSMutableDictionary dictionary];
  self.deleteMarkers = [NSMutableArray array];
  self.semaphore = dispatch_semaphore_create(0);
  self.stopFlag = NO;

  //---------------------
  // Delete thread
  //---------------------
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

    while(!self.stopFlag) {

      dispatch_async(dispatch_get_main_queue(), ^(void) {
        @synchronized (self.deleteMarkers) {
          NSString *markerId;
          GMSMarker *marker = nil;
          //---------
          // delete
          //---------
          for (int i = (int)([self.deleteMarkers count] - 1); i > -1; i--) {
            markerId = [self.deleteMarkers objectAtIndex:i];
            @synchronized (self.objects) {
              marker = [self.objects objectForKey: markerId];
            }

            @synchronized (self.pluginMarkers) {
              if (![[self.pluginMarkers objectForKey:markerId] isEqualToString:@"WORKING"]) {
                @synchronized (self.objects) {
                  [self _removeMarker:marker];
                  marker = nil;
                  if ([self.objects objectForKey:[NSString stringWithFormat:@"marker_property_%@", markerId]]) {
                    [self.objects removeObjectForKey:[NSString stringWithFormat:@"marker_property_%@", markerId]];
                  }
                }
                [self.pluginMarkers removeObjectForKey:markerId];
                [self.deleteMarkers removeObjectAtIndex:i];
              } else {
                [self.pluginMarkers setObject:@"DELETED" forKey:markerId];
              }
            }
          }
        }

      });

    }
  });
}

- (void)pluginUnload
{
  self.stopFlag = true;

  // Plugin destroy
  NSArray *keys = [self.objects allKeys];
  NSString *key;
  for (int i = 0; i < [keys count]; i++) {
      key = [keys objectAtIndex:i];
      [self.objects removeObjectForKey:key];
  }
  self.objects = nil;
}

- (void)create:(CDVInvokedUrlCommand*)command {
  NSDictionary *params = [command.arguments objectAtIndex:1];
  NSArray *positionList = [params objectForKey:@"positionList"];
  NSMutableArray *geocellList = [NSMutableArray array];
  NSMutableDictionary *position;
  double lat, lng;

  for (int i = 0; i < [positionList count]; i++) {
    position = [positionList objectAtIndex:i];
    lat = [[position objectForKey:@"lat"] doubleValue];
    lng = [[position objectForKey:@"lng"] doubleValue];
    [geocellList addObject:[self getGeocell:lat lng:lng resolution:12]];
  }

  NSString *clusterId = [NSString stringWithFormat:@"markercluster_%lu", command.hash];
  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:geocellList forKey:@"geocellList"];
  [result setObject:clusterId forKey:@"id"];
  [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)command.hash] forKey:@"hashCode"];

  [self.debugFlags setObject:[NSNumber numberWithBool:[[params objectForKey:@"debug"] boolValue]] forKey:clusterId];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)redrawClusters:(CDVInvokedUrlCommand*)command {

  __block NSMutableArray *updateClusterIDs = [NSMutableArray array];
  __block NSMutableDictionary *changeProperties = [NSMutableDictionary dictionary];
  __block NSString *clusterId = [command.arguments objectAtIndex: 0];

  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    @synchronized (self.semaphore) {
      BOOL isDebug = [[self.objects objectForKey:clusterId] boolValue];

      NSDictionary *params = [command.arguments objectAtIndex:1];
      NSString *clusterId_markerId, *markerId;
      NSMutableArray *deleteClusters = nil;

      if ([params objectForKey:@"delete"]) {
        deleteClusters = [params objectForKey:@"delete"];
      }
      NSMutableArray *new_or_update = nil;
      if ([params objectForKey:@"new_or_update"]) {
        new_or_update = [params objectForKey:@"new_or_update"];
      }

      if (deleteClusters != nil) {
        //-------------------------------------
        // delete markers on the delete thread
        //-------------------------------------
        int deleteCnt = 0;
        deleteCnt = (int)[deleteClusters count];
        @synchronized (self.deleteMarkers) {
          for (int i = 0; i < deleteCnt; i++) {
            markerId = [deleteClusters objectAtIndex:i];
            [self.deleteMarkers addObject:[NSString stringWithFormat:@"%@-%@", clusterId, markerId]];
          }
        }
      }

      //---------------------------
      // Determine new or update
      //---------------------------
      int new_or_updateCnt = 0;
      if (new_or_update != nil) {
        new_or_updateCnt = (int)[new_or_update count];
      }

      NSDictionary *clusterData, *positionJSON;
      NSMutableDictionary *properties;
      for (int i = 0; i < new_or_updateCnt; i++) {
        clusterData = [new_or_update objectAtIndex:i];
        positionJSON = [clusterData objectForKey:@"position"];
        markerId = [clusterData objectForKey:@"id"];
        clusterId_markerId = [NSString stringWithFormat:@"%@-%@", clusterId, markerId];

        // Save the marker properties
        [self.objects setObject:clusterData forKey:[NSString stringWithFormat:@"marker_property_%@", clusterId_markerId]];

        // Set the WORKING status flag
        [updateClusterIDs addObject:clusterId_markerId];
        @synchronized (self.pluginMarkers) {
          [self.pluginMarkers setObject:@"WORKING" forKey:clusterId_markerId];
        }

        // Prepare the marker properties for addMarker()
        properties = [NSMutableDictionary dictionary];
        [properties setObject:[positionJSON objectForKey:@"lat"] forKey:@"lat"];
        [properties setObject:[positionJSON objectForKey:@"lng"] forKey:@"lng"];
        if ([clusterData objectForKey:@"title"]) {
          [properties setObject:[clusterData objectForKey:@"title"] forKey:@"title"];
        }
        [properties setObject:clusterId_markerId forKey:@"id"];

        if ([clusterData objectForKey:@"icon"]) {
          id iconObj = [clusterData objectForKey:@"icon"];
          if ([[iconObj class] isSubclassOfClass:[NSString class]]) {
            NSMutableDictionary *iconProperties = [NSMutableDictionary dictionary];
            [iconProperties setObject:iconObj forKey:@"url"];
            [properties setObject:iconProperties forKey:@"icon"];

          } else if ([[iconObj class] isSubclassOfClass:[NSDictionary class]]) {
            NSMutableDictionary *iconProperties = [NSMutableDictionary dictionaryWithDictionary:iconObj];
            if ([iconProperties objectForKey:@"label"]) {
              NSMutableDictionary *label = [NSMutableDictionary dictionaryWithDictionary:[iconProperties objectForKey:@"label"]];
              if (isDebug) {
                [label setObject:markerId forKey:@"text"];
              } else {
                [label setObject:[clusterData objectForKey:@"count"] forKey:@"text"];
              }
              [iconProperties setObject:label forKey:@"label"];
            } else {
              NSMutableDictionary *label = [NSMutableDictionary dictionary];
              if (isDebug) {
                [label setObject:markerId forKey:@"text"];
              } else {
                [label setObject:[NSNumber numberWithInt:15] forKey:@"fontSize"];
                [label setObject:[NSNumber numberWithBool:TRUE] forKey:@"bold"];
                [label setObject:[clusterData objectForKey:@"count"] forKey:@"text"];
              }
              [iconProperties setObject:label forKey:@"label"];
            }

            if ([iconProperties objectForKey:@"anchor"]) {
              [iconProperties setObject:[iconProperties objectForKey:@"anchor"] forKey:@"anchor"];
            }
            if ([iconProperties objectForKey:@"infoWindowAnchor"]) {
              [iconProperties setObject:[iconProperties objectForKey:@"infoWindowAnchor"] forKey:@"infoWindowAnchor"];
            }

            [properties setObject:iconProperties forKey:@"icon"];
          }
        } // if ([clusterData objectForKey:@"icon"]) {..}

        [changeProperties setObject:properties forKey:clusterId_markerId];

      } // for (int i = 0; i < new_or_updateCnt; i++) { .. }

    } // @synchronized (self.semaphore)

    //---------------------------
    // mapping markers on the map
    //---------------------------
    [[NSOperationQueue mainQueue] addOperationWithBlock: ^{
      self.mapCtrl.map.selectedMarker = nil;
      NSString *markerId;
      NSMutableDictionary *markerProperties;
      GMSMarker *marker;
      CLLocationCoordinate2D position;
      double latitude, longitude;
      BOOL isNew;

      //---------------------
      // new or update
      //---------------------
      [self.waitCntManager setObject:[NSNumber numberWithInteger:[updateClusterIDs count]] forKey:clusterId];
      for (int i = 0; i < [updateClusterIDs count]; i++) {
        markerId = [updateClusterIDs objectAtIndex:i];
        @synchronized(self.pluginMarkers) {
            [self.pluginMarkers setObject:@"WORKING" forKey:markerId];
        }
        isNew = [self.objects objectForKey:markerId] == nil;

        // Get the marker properties
        markerProperties = [changeProperties objectForKey:markerId];

        if (isNew) {
          // If the requested id is new location, create a marker
          latitude = [[markerProperties objectForKey:@"lat"] doubleValue];
          longitude = [[markerProperties objectForKey:@"lng"] doubleValue];
          position = CLLocationCoordinate2DMake(latitude, longitude);
          marker = [GMSMarker markerWithPosition:position];
          marker.tracksViewChanges = NO;
          marker.tracksInfoWindowChanges = NO;
          marker.userData = markerId;

          // Store the marker instance with markerId
          @synchronized (self.objects) {
            [self.objects setObject:marker forKey:markerId];
          }
        } else {
          @synchronized (self.objects) {
            marker = [self.objects objectForKey:markerId];
          }
        }

        //----------------------------------------
        // Set the title and snippet properties
        //----------------------------------------
        if ([markerProperties objectForKey:@"title"]) {
          marker.title = [markerProperties objectForKey:@"title"];
        }
        if ([markerProperties objectForKey:@"snippet"]) {
          marker.snippet = [markerProperties objectForKey:@"snippet"];
        }

/*
        if (isNew == NO) {
          @synchronized(self.pluginMarkers) {
            [self.pluginMarkers setObject:@"CREATED" forKey:markerId];
          }
          [self decreaseWaitWithClusterId:clusterId];
          continue;
        }
*/
        if ([markerProperties objectForKey:@"icon"]) {
          PluginMarkerCluster *self_ = self;
          NSDictionary *icon = [markerProperties objectForKey:@"icon"];
          [self setIconToClusterMarker:markerId marker:marker iconProperty:icon callbackBlock:^(BOOL successed, id resultObj) {
            if (successed == NO) {
              //--------------------------------------
              // Could not read icon for some reason
              //--------------------------------------
              NSLog(@"(error) %@", resultObj);
            } else {
              //--------------------------------------
              // Marker was updated
              //--------------------------------------
              marker.map = self_.mapCtrl.map;
            }
            [self_ decreaseWaitWithClusterId:clusterId];
          }];
        } else {
          marker.map = self.mapCtrl.map;
          @synchronized (self.pluginMarkers) {
            [self.pluginMarkers setObject:@"CREATED" forKey:markerId];
          }
          [self decreaseWaitWithClusterId:clusterId];
        }

      } // for (int i = 0; i < [updateClusterIDs count]; i++) {..}

    }]; // [[NSOperationQueue mainQueue] addOperationWithBlock: ^{..}

    dispatch_semaphore_wait(self.semaphore, DISPATCH_TIME_FOREVER);
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];


  }); // dispatch_async
}

- (void) setIconToClusterMarker:(NSString *) markerId marker:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty callbackBlock:(void (^)(BOOL successed, id resultObj)) callbackBlock {
  PluginMarkerCluster *self_ = self;
  @synchronized (_pluginMarkers) {
    if ([[_pluginMarkers objectForKey:markerId] isEqualToString:@"DELETED"]) {
      [self _removeMarker:marker];
      if ([self.pluginMarkers objectForKey:markerId]) {
        [_pluginMarkers removeObjectForKey:markerId];
      }
      GMSPolygon *polygon = [self.objects objectForKey:[NSString stringWithFormat:@"polygon%@", markerId]];
      if (polygon != nil && polygon.userData != nil) {
        polygon.userData = nil;
        polygon.map = nil;
        polygon = nil;
        [self.objects removeObjectForKey:[NSString stringWithFormat:@"polygon%@", markerId]];
      }

      callbackBlock(NO, @"marker has been removed");
      return;
    }
    [_pluginMarkers setObject:@"WORKING" forKey:markerId];
  }
  [self setIcon_:marker iconProperty:iconProperty callbackBlock:^(BOOL successed, id resultObj) {
    if (successed) {
      //----------------------------------------------------------------------
      // If marker has been already marked as DELETED, remove the marker.
      //----------------------------------------------------------------------
      GMSMarker *marker = resultObj;
      @synchronized (self_.pluginMarkers) {
        if ([[self_.pluginMarkers objectForKey:markerId] isEqualToString:@"DELETED"]) {
            [self_ _removeMarker:marker];
            [self_.pluginMarkers removeObjectForKey:markerId];

            GMSPolygon *polygon = [self_.objects objectForKey:[NSString stringWithFormat:@"polygon%@", markerId]];
            if (polygon != nil && polygon.userData != nil) {
                [self_.objects removeObjectForKey:[NSString stringWithFormat:@"polygon%@", markerId]];
                polygon.userData = nil;
                polygon.map = nil;
                polygon = nil;
            }
            callbackBlock(YES, nil);
            return;
        }

        [self_.pluginMarkers setObject:@"CREATED" forKey:markerId];
        callbackBlock(YES, marker);
        return;
      }
    } else {
      if (marker != nil && marker.userData != nil) {
        [self_ _removeMarker:marker];
      }
      @synchronized (self_.pluginMarkers) {
        [self_.pluginMarkers removeObjectForKey:markerId];
      }

      GMSPolygon *polygon = [self_.objects objectForKey:[NSString stringWithFormat:@"polygon%@", markerId]];
      if (polygon != nil && polygon.userData != nil) {
          [self_.objects removeObjectForKey:[NSString stringWithFormat:@"polygon%@", markerId]];
          polygon.userData = nil;
          polygon.map = nil;
          polygon = nil;
      }
      callbackBlock(NO, resultObj);
    }
  }];
}

- (void) decreaseWaitWithClusterId:(NSString *) clusterId{

  @synchronized (_waitCntManager) {
    int waitCnt = [[_waitCntManager objectForKey:clusterId] intValue];
    waitCnt = waitCnt - 1;
    [_waitCntManager setObject:[NSNumber numberWithInt:waitCnt] forKey:clusterId];

    if (waitCnt == 0) {
      dispatch_semaphore_signal(self.semaphore);
    }
  }
}

- (NSString *)getGeocell:(double) lat lng:(double) lng resolution:(int)resolution {

  NSMutableString *cell = [NSMutableString string];
  double north = 90.0;
  double south = -90.0;
  double east = 180.0;
  double west = -180.0;
  double subcell_lng_span, subcell_lat_span;
  char x, y;

  while ([cell length] < resolution) {
    subcell_lng_span = (east - west) / GEOCELL_GRID_SIZE;
    subcell_lat_span = (north - south) / GEOCELL_GRID_SIZE;

    x = (char)MIN(floor(GEOCELL_GRID_SIZE * (lng - west) / (east - west)), GEOCELL_GRID_SIZE - 1);
    y = (char)MIN(floor(GEOCELL_GRID_SIZE * (lat - south) / (north - south)), GEOCELL_GRID_SIZE - 1);
    [cell appendString:[NSString stringWithFormat:@"%c", [self _subdiv_char:x y:y]]];

    south += subcell_lat_span * y;
    north = south + subcell_lat_span;

    west += subcell_lng_span * x;
    east = west + subcell_lng_span;
  }

  return cell;
}

- (char) _subdiv_char:(int) posX y:(int)posY {
  return [GEOCELL_ALPHABET characterAtIndex:(
          (posY & 2) << 2 |
          (posX & 2) << 1 |
          (posY & 1) << 1 |
          (posX & 1))];
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
