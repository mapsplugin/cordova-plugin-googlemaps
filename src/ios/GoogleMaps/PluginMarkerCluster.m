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
NSObject *dummyObject;
BOOL recyleOption = NO;

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
  dummyObject = [[NSObject alloc] init];
  self.objects = [[NSMutableDictionary alloc] init];
  self._pluginResults = [[NSMutableDictionary alloc] init];
  self.waitCntManager = [NSMutableDictionary dictionary];
  self.pluginMarkers = [NSMutableDictionary dictionary];
  self.executeQueue =  [NSOperationQueue new];
}

- (void)pluginUnload
{


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

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)redrawClusters:(CDVInvokedUrlCommand*)command {


  __block NSMutableDictionary *updateClusterIDs = [NSMutableDictionary dictionary];
  __block NSMutableArray *deleteClusterIDs = [NSMutableArray array];
  __block NSMutableDictionary *changeProperties = [NSMutableDictionary dictionary];
  __block NSString *clusterId = [command.arguments objectAtIndex: 0];

  @synchronized (dummyObject) {
    NSDictionary *params = [command.arguments objectAtIndex:1];
    NSString *clusterId_markerId, *deleteMarkerId, *markerId;
    NSMutableArray *deleteClusters = nil;

    if ([params objectForKey:@"delete"]) {
      deleteClusters = [params objectForKey:@"delete"];
    }
    NSMutableArray *new_or_update = nil;
    if ([params objectForKey:@"new_or_update"]) {
      new_or_update = [params objectForKey:@"new_or_update"];
    }

    int deleteCnt = 0;
    int new_or_updateCnt = 0;
    if (deleteClusters != nil) {
      deleteCnt = (int)[deleteClusters count];
    }
    if (new_or_update != nil) {
      new_or_updateCnt = (int)[new_or_update count];
    }
    for (int i = 0; i < deleteCnt; i++) {
      markerId = [deleteClusters objectAtIndex:i];
      [deleteClusterIDs addObject:[NSString stringWithFormat:@"%@-%@", clusterId, markerId]];
    }


    //---------------------------
    // Determine new or update
    //---------------------------
    NSDictionary *clusterData, *positionJSON;
    NSMutableDictionary *properties;
    for (int i = 0; i < new_or_updateCnt; i++) {
      clusterData = [new_or_update objectAtIndex:i];
      positionJSON = [clusterData objectForKey:@"position"];
      markerId = [clusterData objectForKey:@"id"];
      clusterId_markerId = [NSString stringWithFormat:@"%@-%@", clusterId, markerId];

      @synchronized (self.objects) {
        [self.objects setObject:clusterData forKey:[NSString stringWithFormat:@"marker_property_%@", clusterId_markerId]];
      }

      if ([self.objects objectForKey:clusterId_markerId] != nil || [_pluginMarkers objectForKey:clusterId_markerId] != nil) {
        [updateClusterIDs setObject:clusterId_markerId forKey:clusterId_markerId];
      } else {
        if (recyleOption && deleteCnt > 0) {
          //---------------
          // Reuse a marker
          //---------------
          deleteMarkerId = [deleteClusterIDs objectAtIndex:0];
          [deleteClusterIDs removeObjectAtIndex:0];
          deleteCnt--;
          [updateClusterIDs setObject:clusterId_markerId forKey:deleteMarkerId];
        } else {
          [_pluginMarkers setObject:@"WORKING" forKey:clusterId_markerId];
          [updateClusterIDs setObject:@"nil" forKey:clusterId_markerId];
        }
      }

      properties = [NSMutableDictionary dictionary];
      if ([clusterData objectForKey:@"geocell"]) {
        [properties setObject:[clusterData objectForKey:@"geocell"] forKey:@"geocell"];
      }
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
            [label setObject:[clusterData objectForKey:@"count"] forKey:@"text"];
            [iconProperties setObject:label forKey:@"label"];
          } else {
            NSMutableDictionary *label = [NSMutableDictionary dictionary];
            [label setObject:[NSNumber numberWithInt:20] forKey:@"fontSize"];
            [label setObject:[NSNumber numberWithBool:TRUE] forKey:@"bold"];
            [label setObject:[clusterData objectForKey:@"count"] forKey:@"text"];
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
      }
      [changeProperties setObject:properties forKey:clusterId_markerId];
    }
  }

  //---------------------------
  // mapping markers on the map
  //---------------------------
  [[NSOperationQueue mainQueue] addOperationWithBlock: ^{
    self.mapCtrl.map.selectedMarker = nil;
    NSString *oldMakrerId, *newMarkerId, *targetMarkerId, *iconCacheKey;
    NSMutableDictionary *markerProperties;
    GMSMarker *marker;
    GMSPolygon *polygon;
    GMSCoordinateBounds *bounds;
    GMSMutablePath *boundsPath;
    CLLocationCoordinate2D position;
    double latitude, longitude;

    //---------------------
    // reuse or update
    //---------------------
    [_waitCntManager setObject:[NSNumber numberWithInteger:[updateClusterIDs count]] forKey:clusterId];
    NSArray<NSString *> *markerIDList = [updateClusterIDs allKeys];
    for (int i = 0; i < [markerIDList count]; i++) {
      oldMakrerId = [markerIDList objectAtIndex:i];
      newMarkerId = [updateClusterIDs objectForKey:oldMakrerId];
      if ([newMarkerId isEqualToString:@"nil"]) {
        targetMarkerId = oldMakrerId;
        markerProperties = [changeProperties objectForKey:targetMarkerId];

        latitude = [[markerProperties objectForKey:@"lat"] doubleValue];
        longitude = [[markerProperties objectForKey:@"lng"] doubleValue];
        position = CLLocationCoordinate2DMake(latitude, longitude);
        marker = [GMSMarker markerWithPosition:position];
        marker.tracksViewChanges = NO;
        marker.tracksInfoWindowChanges = NO;
        marker.appearAnimation = NO;
        marker.map = self.mapCtrl.map;
        if ([markerProperties objectForKey:@"title"]) {
          marker.title = [markerProperties objectForKey:@"title"];
        }
        if ([markerProperties objectForKey:@"snippet"]) {
          marker.snippet = [markerProperties objectForKey:@"snippet"];
        }
        marker.userData = targetMarkerId;
        @synchronized (self.objects) {
          [self.objects setObject:marker forKey:targetMarkerId];
          [_pluginMarkers setObject:@"WORKING" forKey:targetMarkerId];
        }

        if ([markerProperties objectForKey:@"geocell"]) {
          bounds = [self computeBox:[markerProperties objectForKey:@"geocell"]];

          boundsPath = [GMSMutablePath path];
          [boundsPath addCoordinate:bounds.northEast];
          [boundsPath addLatitude:bounds.northEast.latitude longitude:bounds.southWest.longitude];
          [boundsPath addCoordinate:bounds.southWest];
          [boundsPath addLatitude:bounds.southWest.latitude longitude:bounds.northEast.longitude];
          polygon = [GMSPolygon polygonWithPath: boundsPath];
          polygon.map = self.mapCtrl.map;
          polygon.strokeColor = UIColor.blackColor;
          polygon.strokeWidth = 2;
          polygon.userData = @"polygon";
          [self.objects setObject:polygon forKey:[NSString stringWithFormat:@"polygon%@", targetMarkerId]];
        }
      } else {
        marker = nil;
        polygon = nil;
        while (marker == nil) {
          marker = [self.objects objectForKey:oldMakrerId];
          polygon = [self.objects objectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]];

          if (marker == nil || marker.userData == nil) {
            @synchronized (self.objects) {
              if ([self.objects objectForKey:oldMakrerId]) {
                [self.objects removeObjectForKey:oldMakrerId];
              }
              if ([self.objects objectForKey:[NSString stringWithFormat:@"marker_property_%@", oldMakrerId]]) {
                [self.objects removeObjectForKey:[NSString stringWithFormat:@"marker_property_%@", oldMakrerId]];
              }
              if ([[self.pluginMarkers objectForKey:oldMakrerId] isEqualToString:@"DELETED"]) {
                [self.pluginMarkers removeObjectForKey:oldMakrerId];
              } else {
                [self.pluginMarkers setObject:@"DELETED" forKey:oldMakrerId];
              }
              polygon = [self.objects objectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]];
              if (polygon != nil && polygon.userData != nil) {
                polygon.userData = nil;
                polygon.map = nil;
                polygon = nil;
                [self.objects removeObjectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]];
              }
            }
            if ([deleteClusterIDs count] > 0) {
              oldMakrerId = [deleteClusterIDs objectAtIndex:0];
              [deleteClusterIDs removeObjectAtIndex:0];
            } else {
              marker = [GMSMarker markerWithPosition:CLLocationCoordinate2DMake(0, 0)];
              marker.tracksViewChanges = NO;
              marker.tracksInfoWindowChanges = NO;
              marker.map = nil;
            }
          } else {
            marker.map = nil;
          }
        }

        markerProperties = [changeProperties objectForKey:newMarkerId];
        if ([[_pluginMarkers objectForKey:newMarkerId] isEqualToString:@"DELETED"]) {

          if ([self.objects objectForKey:[NSString stringWithFormat:@"polygon%@", newMarkerId]]) {
            [self.objects removeObjectForKey:[NSString stringWithFormat:@"polygon%@", newMarkerId]];
          }

          [self decreaseWaitCntOrExit:clusterId command:command];
          continue;
        }

        marker.position = CLLocationCoordinate2DMake(
            [[markerProperties objectForKey:@"lat"] doubleValue], [[markerProperties objectForKey:@"lng"] doubleValue]);
        if ([markerProperties objectForKey:@"title"]) {
          marker.title = [markerProperties objectForKey:@"title"];
        } else {
          marker.title = nil;
        }
        if ([markerProperties objectForKey:@"snippet"]) {
          marker.snippet = [markerProperties objectForKey:@"snippet"];
        } else {
          marker.snippet = nil;
        }
        marker.userData = newMarkerId;

        targetMarkerId = newMarkerId;
        if ([markerProperties objectForKey:@"geocell"]) {
          bounds = [self computeBox:[markerProperties objectForKey:@"geocell"]];
          boundsPath = [GMSMutablePath path];
          [boundsPath addCoordinate:bounds.northEast];
          [boundsPath addLatitude:bounds.northEast.latitude longitude:bounds.southWest.longitude];
          [boundsPath addCoordinate:bounds.southWest];
          [boundsPath addLatitude:bounds.southWest.latitude longitude:bounds.northEast.longitude];

          if (polygon == nil || polygon.userData == nil) {
            polygon = [GMSPolygon polygonWithPath: boundsPath];
            polygon.map = self.mapCtrl.map;
            polygon.strokeColor = UIColor.blackColor;
            polygon.strokeWidth = 2;
            polygon.userData = @"polygon";
          } else {
            polygon.path = boundsPath;
          }
        } else if (polygon != nil && polygon.userData != nil) {
          polygon.userData = nil;
          polygon.map = nil;
          polygon = nil;

          if ([self.objects objectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]]) {
            [self.objects removeObjectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]];
          }
        }

        if (![oldMakrerId isEqualToString:newMarkerId]) {
          @synchronized (self.objects) {
            [self.objects setObject:marker forKey:newMarkerId];
            if (polygon != nil && polygon.userData != nil) {
              [self.objects setObject:polygon forKey:[NSString stringWithFormat:@"polygon%@", newMarkerId]];
            }
            [_pluginMarkers setObject:@"CREATD" forKey:newMarkerId];
            [self.objects removeObjectForKey:oldMakrerId];
            iconCacheKey = [self.objects objectForKey:[NSString stringWithFormat:@"marker_icon_%@", oldMakrerId]];
            if (iconCacheKey != nil) {
              [self.objects removeObjectForKey:[NSString stringWithFormat:@"marker_icon_%@", oldMakrerId]];
              [self.objects setObject:iconCacheKey forKey:[NSString stringWithFormat:@"marker_icon_%@", newMarkerId]];
            }
            [_pluginMarkers removeObjectForKey:oldMakrerId];
          }
        } else {
          @synchronized (self.objects) {
            [self.objects setObject:marker forKey:newMarkerId];
            if (polygon != nil && polygon.userData != nil) {
              [self.objects setObject:polygon forKey:[NSString stringWithFormat:@"polygon%@", newMarkerId]];
            }
          }
        }
      }

      if ([markerProperties objectForKey:@"icon"]) {
        PluginMarkerCluster *self_ = self;
        NSDictionary *icon = [markerProperties objectForKey:@"icon"];
        [self setIconToClusterMarker:targetMarkerId marker:marker iconProperty:icon callbackBlock:^(BOOL successed, id resultObj) {
          if (successed == NO) {
            NSLog(@"(error) %@", resultObj);
          } else {
            marker.map = self_.mapCtrl.map;
          }
          [self_ decreaseWaitCntOrExit:clusterId command:command];
        }];
      } else {
        marker.map = self.mapCtrl.map;
        @synchronized (self.pluginMarkers) {
          [self.pluginMarkers setObject:@"CREATED" forKey:targetMarkerId];
        }
        [self decreaseWaitCntOrExit:clusterId command:command];
        marker.icon = nil;
      }
    }


    //---------
    // delete
    //---------
    for (int i = 0; i < [deleteClusterIDs count]; i++) {
      oldMakrerId = [deleteClusterIDs objectAtIndex:i];
      @synchronized (self.objects) {
        marker = [self.objects objectForKey:oldMakrerId];
        polygon = [self.objects objectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]];
      }
      @synchronized (self.pluginMarkers) {
        if (![[self.pluginMarkers objectForKey:oldMakrerId] isEqualToString:@"WORKING"]) {
          if (polygon != nil && polygon.userData != nil) {
            polygon.userData = nil;
            polygon.map = nil;
            polygon = nil;
          }
          @synchronized (self.objects) {
            [self _removeMarker:marker];
            marker = nil;
            if ([self.objects objectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]]) {
              [self.objects removeObjectForKey:[NSString stringWithFormat:@"polygon%@", oldMakrerId]];
            }
          }
          [self.pluginMarkers removeObjectForKey:oldMakrerId];
        } else {
          [self.pluginMarkers setObject:@"DELETED" forKey:oldMakrerId];
        }
      }
    }
  }];

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

- (void) decreaseWaitCntOrExit:(NSString *) key command: (CDVInvokedUrlCommand*)command{

  @synchronized (_waitCntManager) {
    int waitCnt = [[_waitCntManager objectForKey:key] intValue];
    waitCnt = waitCnt - 1;
    [_waitCntManager setObject:[NSNumber numberWithInt:waitCnt] forKey:key];

    if (waitCnt == 0) {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [(CDVCommandDelegateImpl *)self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
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
