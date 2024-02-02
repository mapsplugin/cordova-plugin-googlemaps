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


- (void)pluginInitialize
{
  if (self.initialized) {
    return;
  }
  self.initialized = YES;
  [super pluginInitialize];

  // Initialize this plugin
  self.waitCntManager = [NSMutableDictionary dictionary];
  self.allResults = [NSMutableDictionary dictionary];
  self.pluginMarkers = [NSMutableDictionary dictionary];
  self.debugFlags = [NSMutableDictionary dictionary];
  self.deleteMarkers = [NSMutableArray array];
  self.semaphore = dispatch_semaphore_create(0);
  self.deleteThreadLock = dispatch_semaphore_create(0);
  self.stopFlag = NO;
  int waitTimeOut = 1000 * 1000 * 1000; // 1sec

  //---------------------
  // Delete thread
  //---------------------
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

    while(!self.stopFlag) {
      @synchronized (self.deleteThreadLock) {
        dispatch_semaphore_wait(self.deleteThreadLock, dispatch_time(DISPATCH_TIME_NOW, waitTimeOut));
      }
      if ([self.deleteMarkers count] == 0) {
        continue;
      }

      dispatch_async(dispatch_get_main_queue(), ^(void) {
        @synchronized (self.deleteMarkers) {
          NSString *markerId;
          GMSMarker *marker = nil;
          //---------
          // delete
          //---------
          int deleteCnt = (int)[self.deleteMarkers count];
          for (int i = (deleteCnt - 1); i > -1; i--) {
            markerId = [self.deleteMarkers objectAtIndex:i];

            @synchronized (self.mapCtrl.objects) {
              marker = [self.mapCtrl.objects objectForKey: markerId];
            }

            @synchronized (self.pluginMarkers) {
              if ([[self.pluginMarkers objectForKey:markerId] isEqualToString:@"WORKING"]) {
                [self.pluginMarkers setObject:@"DELETED" forKey:markerId];
              } else {
                @synchronized (self.mapCtrl.objects) {
                  [self _removeMarker:marker];
                  marker = nil;
                  [self.mapCtrl.objects removeObjectForKey:markerId];
                  if ([self.mapCtrl.objects objectForKey:[NSString stringWithFormat:@"marker_property_%@", markerId]]) {
                    [self.mapCtrl.objects removeObjectForKey:[NSString stringWithFormat:@"marker_property_%@", markerId]];
                  }

                  if ([self.mapCtrl.objects objectForKey:[NSString stringWithFormat:@"marker_icon_%@", markerId]]) {
                    [self.mapCtrl.objects removeObjectForKey:[NSString stringWithFormat:@"marker_icon_%@", markerId]];
                  }
                }
                [self.pluginMarkers removeObjectForKey:markerId];
                [self.deleteMarkers removeObjectAtIndex:i];
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
  [super pluginUnload];

  @synchronized (self.pluginMarkers) {
    NSString *key;
    NSArray *keys = self.pluginMarkers.allKeys;
    for (int i = 0; i < keys.count; i++) {
      key = [keys objectAtIndex:i];
      [self.pluginMarkers setObject:@"DELETED" forKey:key];
      [self.deleteMarkers addObject:key];
    }
  }

}

- (void)remove:(CDVInvokedUrlCommand*)command {

  NSString *clusterId = [command.arguments objectAtIndex: 0];
  @synchronized (self.debugFlags) {
    [self.debugFlags removeObjectForKey:clusterId];
    [self.waitCntManager removeObjectForKey:clusterId];
  }

  @synchronized (self.pluginMarkers) {
    NSString *key;
    NSArray *keys = self.pluginMarkers.allKeys;
    for (int i = 0; i < keys.count; i++) {
      key = [keys objectAtIndex:i];
      if ([key hasPrefix:clusterId]) {
        [self.pluginMarkers setObject:@"DELETED" forKey:key];
        [self.deleteMarkers addObject:key];
      }
    }
  }

  dispatch_semaphore_signal(self.deleteThreadLock);


  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK ];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

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

  NSString *clusterId = [NSString stringWithFormat:@"markercluster_%lu%d", command.hash, arc4random() % 100000];
  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
  [result setObject:geocellList forKey:@"geocellList"];
  [result setObject:clusterId forKey:@"__pgmId"];
  [result setObject:[NSNumber numberWithFloat:[[UIScreen mainScreen] scale]] forKey:@"scale"];

  [self.debugFlags setObject:[NSNumber numberWithBool:[[params objectForKey:@"debug"] boolValue]] forKey:clusterId];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)redrawClusters:(CDVInvokedUrlCommand*)command {

  __block NSMutableArray *updateClusterIDs = [NSMutableArray array];
  __block NSMutableDictionary *changeProperties = [NSMutableDictionary dictionary];
  __block NSString *clusterId = [command.arguments objectAtIndex: 0];

  [self.mapCtrl.executeQueue addOperationWithBlock:^{
    [self.allResults removeAllObjects];
    BOOL isDebug = [[self.debugFlags objectForKey:clusterId] boolValue];

    __block NSDictionary *params = [command.arguments objectAtIndex:1];
    NSString *clusterId_markerId, *markerId;

    NSMutableArray *new_or_update = nil;
    if ([params objectForKey:@"new_or_update"]) {
      new_or_update = [params objectForKey:@"new_or_update"];
    }

    //---------------------------
    // Determine new or update
    //---------------------------
    int new_or_updateCnt = 0;
    if (new_or_update != nil) {
      new_or_updateCnt = (int)[new_or_update count];
    }

    NSDictionary *clusterData;
    NSMutableDictionary *properties;
    for (int i = 0; i < new_or_updateCnt; i++) {
      clusterData = [new_or_update objectAtIndex:i];
      markerId = [clusterData objectForKey:@"__pgmId"];
      clusterId_markerId = [NSString stringWithFormat:@"%@-%@", clusterId, markerId];

      // Save the marker properties
      [self.mapCtrl.objects setObject:clusterData forKey:[NSString stringWithFormat:@"marker_property_%@", clusterId_markerId]];

      // Set the WORKING status flag
      [updateClusterIDs addObject:clusterId_markerId];
      @synchronized (self.pluginMarkers) {
        [self.pluginMarkers setObject:@"WORKING" forKey:clusterId_markerId];
      }

      // Prepare the marker properties for addMarker()
      properties = [NSMutableDictionary dictionary];
      [properties setObject:[clusterData objectForKey:@"position"] forKey:@"position"];
      if ([clusterData objectForKey:@"title"]) {
        [properties setObject:[clusterData objectForKey:@"title"] forKey:@"title"];
      }
      if (clusterData[@"visible"]) {
        [properties setObject:[clusterData objectForKey:@"visible"] forKey:@"visible"];
      } else {
        [properties setObject:[NSNumber numberWithBool:true] forKey:@"visible"];
      }
      [properties setObject:clusterId_markerId forKey:@"__pgmId"];

      if ([clusterData objectForKey:@"icon"]) {
        id iconObj = [clusterData objectForKey:@"icon"];
        if ([[iconObj class] isSubclassOfClass:[NSString class]]) {
          NSMutableDictionary *iconProperties = [NSMutableDictionary dictionary];
          [iconProperties setObject:iconObj forKey:@"url"];
          [properties setObject:iconProperties forKey:@"icon"];

        } else if ([[iconObj class] isSubclassOfClass:[NSDictionary class]]) {

          NSMutableDictionary *iconProperties = [NSMutableDictionary dictionaryWithDictionary:iconObj];

          if ([[clusterData objectForKey:@"isClusterIcon"] boolValue]) {

            if ([iconProperties objectForKey:@"label"]) {

              NSMutableDictionary *label = [NSMutableDictionary dictionaryWithDictionary:[iconProperties objectForKey:@"label"]];
              if (isDebug == YES) {
                [label setObject:[clusterId_markerId stringByReplacingOccurrencesOfString:clusterId withString:@""] forKey:@"text"];
              } else {
                [label setObject:[clusterData objectForKey:@"count"] forKey:@"text"];
              }
              [iconProperties setObject:label forKey:@"label"];

            } else {
              NSMutableDictionary *label = [NSMutableDictionary dictionary];
              if (isDebug == YES) {
                [label setObject:[clusterId_markerId stringByReplacingOccurrencesOfString:clusterId withString:@""] forKey:@"text"];
              } else {
                [label setObject:[NSNumber numberWithInt:15] forKey:@"fontSize"];
                [label setObject:[NSNumber numberWithBool:TRUE] forKey:@"bold"];
                [label setObject:[clusterData objectForKey:@"count"] forKey:@"text"];
              }
              [iconProperties setObject:label forKey:@"label"];
            }


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


    if ([updateClusterIDs count] == 0) {
      [self deleteProcess:params clusterId:clusterId];
      [self endRedraw:command];
      return;
    }

    //---------------------------
    // mapping markers on the map
    //---------------------------
    [[NSOperationQueue mainQueue] addOperationWithBlock: ^{
      //self.mapCtrl.map.selectedMarker = nil;
      NSString *clusterId_markerId;
      NSMutableDictionary *markerProperties;
      GMSMarker *marker;
      CLLocationCoordinate2D position;
      double latitude, longitude;
      BOOL isNew;
      NSDictionary *positionJSON;

      //---------------------
      // new or update
      //---------------------
      [self.waitCntManager setObject:[NSNumber numberWithInteger:[updateClusterIDs count]] forKey:clusterId];
      for (int i = 0; i < [updateClusterIDs count]; i++) {
        clusterId_markerId = [updateClusterIDs objectAtIndex:i];
        @synchronized(self.pluginMarkers) {
          [self.pluginMarkers setObject:@"WORKING" forKey:clusterId_markerId];
        }

        // Get the marker properties
        markerProperties = [self.mapCtrl.objects objectForKey:[NSString stringWithFormat:@"marker_property_%@", clusterId_markerId]];

        isNew = [self.mapCtrl.objects objectForKey:clusterId_markerId] == nil;
        //--------------------------
        // regular marker
        //--------------------------
        if ([clusterId_markerId containsString:@"-marker_"]) {
          if (isNew) {
            markerProperties = [self.mapCtrl.objects objectForKey:[NSString stringWithFormat:@"marker_property_%@", clusterId_markerId]];
            [super _create:clusterId_markerId markerOptions:markerProperties callbackBlock:^(BOOL successed, id resultObj) {

              @synchronized (self.pluginMarkers) {
                if (successed) {
                  //((GMSMarker *)resultObj).map = self.mapCtrl.map;
                  [self.pluginMarkers setObject:@"CREATED" forKey:clusterId_markerId];

                  NSArray *tmp = [clusterId_markerId componentsSeparatedByString:@"-"];
                  NSString *markerId = [tmp objectAtIndex:1];
                  NSMutableDictionary *createResult = [NSMutableDictionary dictionary];
                  GMSMarker *marker = resultObj;
                  UIImage *image;
                  NSString *iconKey = [NSString stringWithFormat:@"marker_icon_%@", marker.userData];
                  // retrieve key mapping set by the PluginMarker.setIcon_
                  NSString *iconCacheKey = [self.mapCtrl.objects objectForKey:iconKey];
                  if (iconCacheKey != nil) {
                      // use it to retrieve cached icon
                      image = [[UIImageCache sharedInstance] getCachedImageForKey:iconCacheKey];
                  }
                  if (image == nil) {
                      // fallback to old behaviour
                      image = [[UIImageCache sharedInstance] getCachedImageForKey:iconKey];
                  }
                  if (image != nil) {
                    [createResult setObject:[NSNumber numberWithInt: (int)image.size.width] forKey:@"width"];
                    [createResult setObject:[NSNumber numberWithInt: (int)image.size.height] forKey:@"height"];
                  } else {
                    [createResult setObject:[NSNumber numberWithInt: 24] forKey:@"width"];
                    [createResult setObject:[NSNumber numberWithInt: 40] forKey:@"height"];
                  }
                  [self.allResults setObject:createResult forKey:markerId];
                } else {
                  //--------------------------------------
                  // Could not read icon for some reason
                  //--------------------------------------
                  [self.pluginMarkers setObject:@"DELETED" forKey:clusterId_markerId];
                  NSLog(@"(error) %@", resultObj);
                  @synchronized (self.deleteMarkers) {
                    [self.deleteMarkers addObject:clusterId_markerId];
                  }
                }
              }
              [self decreaseWaitWithClusterId:clusterId command:command];

            }];
          } else {

            marker = [self.mapCtrl.objects objectForKey:clusterId_markerId];
            //----------------------------------------
            // Set the title and snippet properties
            //----------------------------------------
            if ([markerProperties objectForKey:@"title"]) {
              marker.title = [markerProperties objectForKey:@"title"];
            }
            if ([markerProperties objectForKey:@"snippet"]) {
              marker.snippet = [markerProperties objectForKey:@"snippet"];
            }
            @synchronized (self.pluginMarkers) {
              [self.pluginMarkers setObject:@"CREATED" forKey:clusterId_markerId];
            }
            [self decreaseWaitWithClusterId:clusterId command:command];
          }
          continue;
        }
        //--------------------------
        // cluster icon
        //--------------------------
        markerProperties = [changeProperties objectForKey:clusterId_markerId];


        if (isNew) {
          // If the requested id is new location, create a marker
          positionJSON = [markerProperties objectForKey:@"position"];
          latitude = [[positionJSON objectForKey:@"lat"] doubleValue];
          longitude = [[positionJSON objectForKey:@"lng"] doubleValue];
          position = CLLocationCoordinate2DMake(latitude, longitude);
          marker = [GMSMarker markerWithPosition:position];
          marker.userData = clusterId_markerId;

          // Store the marker instance with markerId
          @synchronized (self.mapCtrl.objects) {
            [self.mapCtrl.objects setObject:marker forKey:clusterId_markerId];
          }
        } else {
          @synchronized (self.mapCtrl.objects) {
            marker = [self.mapCtrl.objects objectForKey:clusterId_markerId];
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


        if ([markerProperties objectForKey:@"icon"]) {
          PluginMarkerCluster *self_ = self;
          NSDictionary *icon = [markerProperties objectForKey:@"icon"];
          [self setIconToClusterMarker:clusterId_markerId marker:marker iconProperty:icon callbackBlock:^(BOOL successed, id resultObj) {
            if (successed == NO) {
              //--------------------------------------
              // Could not read icon for some reason
              //--------------------------------------
              NSLog(@"(error) %@", resultObj);
              @synchronized (self_.deleteMarkers) {
                [self_.deleteMarkers addObject:clusterId_markerId];
              }
              @synchronized (self_.pluginMarkers) {
                [self_.pluginMarkers setObject:@"DELETED" forKey:clusterId_markerId];
              }
            } else {
              //--------------------------------------
              // Marker was updated
              //--------------------------------------
              marker.map = self_.mapCtrl.map;
              @synchronized (self_.pluginMarkers) {
                [self_.pluginMarkers setObject:@"CREATED" forKey:clusterId_markerId];
              }
            }
            [self_ decreaseWaitWithClusterId:clusterId command:command];
          }];
        } else {
          marker.map = self.mapCtrl.map;
          @synchronized (self.pluginMarkers) {
            [self.pluginMarkers setObject:@"CREATED" forKey:clusterId_markerId];
          }
          [self decreaseWaitWithClusterId:clusterId command:command];
        }

      } // for (int i = 0; i < [updateClusterIDs count]; i++) {..}

      [self deleteProcess:params clusterId:clusterId];

    }]; // [[NSOperationQueue mainQueue] addOperationWithBlock: ^{..}


  }]; // dispatch_async
}

- (void) endRedraw:(CDVInvokedUrlCommand*)command {
  NSLog(@"--->allResults = %@", self.allResults);
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:self.allResults];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) deleteProcess:(NSDictionary *) params  clusterId:(NSString *)clusterId{

  NSMutableArray *deleteClusters = nil;
  if ([params objectForKey:@"delete"]) {
    deleteClusters = [params objectForKey:@"delete"];
  }
  if (deleteClusters != nil) {
    //-------------------------------------
    // delete markers on the delete thread
    //-------------------------------------
    int deleteCnt = 0;
    deleteCnt = (int)[deleteClusters count];
    NSString *clusterId_markerId;
    @synchronized (self.deleteMarkers) {
      for (int i = 0; i < deleteCnt; i++) {
        clusterId_markerId = [NSString stringWithFormat:@"%@-%@",
                              clusterId, [deleteClusters objectAtIndex:i]];
        [self.deleteMarkers addObject:clusterId_markerId];
      }
    }

    dispatch_semaphore_signal(self.deleteThreadLock);
  }


}

- (void) setIconToClusterMarker:(NSString *) markerId marker:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty callbackBlock:(void (^)(BOOL successed, id resultObj)) callbackBlock {
  PluginMarkerCluster *self_ = self;
  @synchronized (_pluginMarkers) {
    if ([[_pluginMarkers objectForKey:markerId] isEqualToString:@"DELETED"]) {
      [self _removeMarker:marker];
      if ([self.pluginMarkers objectForKey:markerId]) {
        [_pluginMarkers removeObjectForKey:markerId];
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

      callbackBlock(NO, resultObj);
    }
  }];
}

- (void) decreaseWaitWithClusterId:(NSString *) clusterId command:(CDVInvokedUrlCommand*)command {

  @synchronized (_waitCntManager) {
    int waitCnt = [[_waitCntManager objectForKey:clusterId] intValue];
    waitCnt = waitCnt - 1;
    [self.waitCntManager setObject:[NSNumber numberWithInt:waitCnt] forKey:clusterId];
    if (waitCnt == 0) {
      [self endRedraw: command];
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
