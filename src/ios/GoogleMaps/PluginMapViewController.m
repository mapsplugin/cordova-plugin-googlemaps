//
//  PluginMapViewController.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginMapViewController.h"

@implementation PluginMapViewController

- (void)mapView:(GMSMapView *)mapView didTapPOIWithPlaceID:(NSString *)placeID name:(NSString *)name location:(CLLocationCoordinate2D)location {
  NSString* jsName = [name stringByReplacingOccurrencesOfString:@"'" withString:@"\\'"];
  jsName = [jsName stringByReplacingOccurrencesOfString:@"\n" withString:@"\\n"];
  jsName = [jsName stringByReplacingOccurrencesOfString:@"\r" withString:@"\\r"];

  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onMapEvent', args: ['%@', \"%@\", new plugin.google.maps.LatLng(%f,%f)]});}",
                        self.overlayId, self.overlayId, @"poi_click", placeID, jsName, location.latitude, location.longitude];
  [self execJS:jsString];
}

/**
 * Called when the My Location button is tapped.
 *
 * @return YES if the listener has consumed the event (i.e., the default behavior should not occur),
 *         NO otherwise (i.e., the default behavior should occur). The default behavior is for the
 *         camera to move such that it is centered on the user location.
 */
- (BOOL)didTapMyLocationButtonForMapView:(GMSMapView *)mapView {

  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onMapEvent', args: []});}",
                        self.overlayId, self.overlayId, @"my_location_button_click"];
  [self execJS:jsString];
  return NO;
}

#pragma mark - GMSMapViewDelegate
- (void)mapView:(GMSMapView *)mapView didTapMyLocation:(CLLocationCoordinate2D)location {

    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithDouble:location.latitude] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithDouble:location.longitude] forKey:@"lng"];

    NSMutableDictionary *json = [NSMutableDictionary dictionary];

    [json setObject:latLng forKey:@"latLng"];
    [json setObject:[NSNumber numberWithFloat:[self.map.myLocation speed]] forKey:@"speed"];
    [json setObject:[NSNumber numberWithFloat:[self.map.myLocation altitude]] forKey:@"altitude"];

    //todo: calcurate the correct accuracy based on horizontalAccuracy and verticalAccuracy
    [json setObject:[NSNumber numberWithFloat:[self.map.myLocation horizontalAccuracy]] forKey:@"accuracy"];
    [json setObject:[NSNumber numberWithDouble:[self.map.myLocation.timestamp timeIntervalSince1970]] forKey:@"time"];
    [json setObject:[NSNumber numberWithInteger:[self.map.myLocation hash]] forKey:@"hashCode"];

    NSData* jsonData = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
    NSString* sourceArrayString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];

    NSString* jsString = [NSString
                          stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onMapEvent', args: [%@]});}",
                          self.overlayId, self.overlayId, @"my_location_click", sourceArrayString];
    [self execJS:jsString];
}

/**
 * @callback the my location button is clicked.
 */
- (void)mapView:(GMSMapView *)mapView didTapAtCoordinate:(CLLocationCoordinate2D)coordinate {


  if (self.activeMarker) {
    /*
     NSString *clusterId_markerId =[NSString stringWithFormat:@"%@", self.activeMarker.userData];
     NSArray *tmp = [clusterId_markerId componentsSeparatedByString:@"_"];
     NSString *className = [tmp objectAtIndex:0];
     if ([className isEqualToString:@"markercluster"]) {
     if ([clusterId_markerId containsString:@"-marker_"]) {
     [self triggerClusterEvent:@"info_close" marker:self.activeMarker];
     }
     } else {
     [self triggerMarkerEvent:@"info_close" marker:self.activeMarker];
     }
     */

    //self.map.selectedMarker = nil;
    self.activeMarker = nil;
  }

  //NSArray *pluginNames =[self.plugins allKeys];
  //NSString *pluginName;
  NSString *key;
  NSDictionary *properties;
  //CDVPlugin<IPluginProtocol> *plugin;
  GMSCoordinateBounds *bounds;
  GMSPath *path;
  NSArray *keys;
  NSNumber *isVisible, *geodesic, *isClickable;
  NSMutableArray *boundsHitList = [NSMutableArray array];
  //NSMutableArray *boundsPluginList = [NSMutableArray array];
  int i,j;
  float zIndex, maxZIndex;
  NSString* hitKey = nil;


  keys = [self.objects allKeys];
  for (j = 0; j < [keys count]; j++) {
    key = [keys objectAtIndex:j];
    if ([key containsString:@"-marker"] ||
        [key containsString:@"property"] == NO) {
      continue;
    }

    properties = [self.objects objectForKey:key];
    //NSLog(@"--> key = %@, properties = %@", key, properties);

    // Skip invisible polyline
    isVisible = (NSNumber *)[properties objectForKey:@"isVisible"];
    if ([isVisible boolValue] == NO) {
      //NSLog(@"--> key = %@, isVisible = NO", key);
      continue;
    }

    // Skip isClickable polyline
    isClickable = (NSNumber *)[properties objectForKey:@"isClickable"];
    if ([isClickable boolValue] == NO) {
      //NSLog(@"--> key = %@, isClickable = NO", key);
      continue;
    }
    //NSLog(@"--> key = %@, isVisible = YES, isClickable = YES", key);

    // Skip if the click point is out of the polyline bounds.
    bounds = (GMSCoordinateBounds *)[properties objectForKey:@"bounds"];
    if ([bounds containsCoordinate:coordinate]) {
      [boundsHitList addObject:key];
      //[boundsPluginList addObject:plugin];
    }

  }
  /*
   for (i = 0; i < [pluginNames count]; i++) {
   pluginName = [pluginNames objectAtIndex:i];

   // Skip marker class
   if ([pluginName hasSuffix:@"-marker"]) {
   continue;
   }

   // Get the plugin (marker, polyline, polygon, circle, groundOverlay)
   plugin = [self.plugins objectForKey:pluginName];


   keys = [plugin.objects allKeys];
   for (j = 0; j < [keys count]; j++) {
   key = [keys objectAtIndex:j];
   if ([key containsString:@"property"]) {
   properties = [plugin.objects objectForKey:key];

   // Skip invisible polyline
   isVisible = (NSNumber *)[properties objectForKey:@"isVisible"];
   if ([isVisible boolValue] == NO) {
   //NSLog(@"--> key = %@, isVisible = NO", key);
   continue;
   }

   // Skip isClickable polyline
   isClickable = (NSNumber *)[properties objectForKey:@"isClickable"];
   if ([isClickable boolValue] == NO) {
   //NSLog(@"--> key = %@, isClickable = NO", key);
   continue;
   }
   //NSLog(@"--> key = %@, isVisible = YES, isClickable = YES", key);

   // Skip if the click point is out of the polyline bounds.
   bounds = (GMSCoordinateBounds *)[properties objectForKey:@"bounds"];
   if ([bounds containsCoordinate:coordinate]) {
   [boundsHitList addObject:key];
   [boundsPluginList addObject:plugin];
   }
   }
   }
   }
   */


  CLLocationCoordinate2D origin = [self.map.projection coordinateForPoint:CGPointMake(0, 0)];
  CLLocationCoordinate2D hitArea = [self.map.projection coordinateForPoint:CGPointMake(1, 1)];
  CLLocationDistance threshold = GMSGeometryDistance(origin, hitArea);



  //
  maxZIndex = -1;
  CLLocationCoordinate2D touchPoint;
  for (i = 0; i < [boundsHitList count]; i++) {
    key = [boundsHitList objectAtIndex:i];
    //plugin = [boundsPluginList objectAtIndex:i];
    properties = [self.objects objectForKey:key];


    zIndex = [[properties objectForKey:@"zIndex"] floatValue];
    //NSLog(@"--> zIndex = %f, maxZIndex = %f", zIndex, maxZIndex);
    if (zIndex < maxZIndex) {
      continue;
    }

    if ([key hasPrefix:@"polyline_"]) {
      geodesic = (NSNumber *)[properties objectForKey:@"geodesic"];
      path = (GMSPath *)[properties objectForKey:@"mutablePath"];
      if ([geodesic boolValue] == YES) {
        touchPoint = [PluginUtil isPointOnTheGeodesicLine:path coordinate:coordinate threshold:threshold projection:self.map.projection];
        if (CLLocationCoordinate2DIsValid(touchPoint)) {
          maxZIndex = zIndex;
          hitKey = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
          continue;
        }
      } else {
        touchPoint = [PluginUtil isPointOnTheLine:path coordinate:coordinate projection:self.map.projection];
        if (CLLocationCoordinate2DIsValid(touchPoint)) {
          maxZIndex = zIndex;
          hitKey = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
          continue;
        }
      }
    }

    if ([key hasPrefix:@"polygon_"]) {
      path = (GMSPath *)[properties objectForKey:@"mutablePath"];
      if ([PluginUtil isPolygonContains:path coordinate:coordinate projection:self.map.projection]) {
        touchPoint = coordinate;
        maxZIndex = zIndex;
        hitKey = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
        continue;
      }
    }


    if ([key hasPrefix:@"circle_"]) {
      key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
      GMSCircle *circle = (GMSCircle *)[self.objects objectForKey:key];
      if ([PluginUtil isCircleContains:circle coordinate:coordinate]) {
        touchPoint = coordinate;
        maxZIndex = zIndex;
        hitKey = key;
        continue;
      }
    }

    if ([key hasPrefix:@"groundoverlay_"]) {
      key = [key stringByReplacingOccurrencesOfString:@"_property" withString:@""];
      GMSGroundOverlay *groundOverlay = (GMSGroundOverlay *)[self.objects objectForKey:key];
      if ([groundOverlay.bounds containsCoordinate:coordinate]) {
        touchPoint = coordinate;
        maxZIndex = zIndex;
        hitKey = key;
        continue;
      }
    }

  }

  if (hitKey != nil) {
    NSArray *tmp = [hitKey componentsSeparatedByString:@"_"];
    NSString *eventName = [NSString stringWithFormat:@"%@_click", [tmp objectAtIndex:0]];
    [self triggerOverlayEvent:eventName overlayId:hitKey coordinate:touchPoint];
  } else {
    [self triggerMapEvent:@"map_click" coordinate:coordinate];
  }


}
/**
 * @callback map long_click
 */
- (void) mapView:(GMSMapView *)mapView didLongPressAtCoordinate:(CLLocationCoordinate2D)coordinate {
  [self triggerMapEvent:@"map_long_click" coordinate:coordinate];
}

/**
 * @callback plugin.google.maps.event.CAMERA_MOVE_START
 */
- (void) mapView:(GMSMapView *)mapView willMove:(BOOL)gesture
{
  self.isDragging = gesture;

  if (self.isDragging) {
    [self triggerMapEvent:@"map_drag_start"];
  }
  [self triggerCameraEvent:@"camera_move_start" position:self.map.camera];
}


/**
 * @callback plugin.google.maps.event.CAMERA_MOVE
 */
- (void)mapView:(GMSMapView *)mapView didChangeCameraPosition:(GMSCameraPosition *)position {

  if (self.isDragging) {
    [self triggerMapEvent:@"map_drag"];
  }
  [self triggerCameraEvent:(@"camera_move") position:position];
}

/**
 * @callback plugin.google.maps.event.CAMERA_MOVE_END
 */
- (void) mapView:(GMSMapView *)mapView idleAtCameraPosition:(GMSCameraPosition *)position
{
  if (self.isDragging) {
    [self triggerMapEvent:@"map_drag_end"];
  }
  [self triggerCameraEvent:(@"camera_move_end") position:position];
  self.isDragging = NO;
}


/**
 * @callback marker info_click
 */
- (void) mapView:(GMSMapView *)mapView didTapInfoWindowOfMarker:(GMSMarker *)marker
{
  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  if ([markerTag hasPrefix:@"markercluster_"]) {
    [self triggerClusterEvent:@"info_click" marker:marker];
  } else {
    [self triggerMarkerEvent:@"info_click" marker:marker];
  }
  [self syncInfoWndPosition];
}
/**
 * Called after a marker's info window has been long pressed.
 */
- (void)mapView:(GMSMapView *)mapView didLongPressInfoWindowOfMarker:(GMSMarker *)marker {

  [self syncInfoWndPosition];
  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  if ([markerTag hasPrefix:@"markercluster_"]) {
    [self triggerClusterEvent:@"info_long_click" marker:marker];
  } else {
    [self triggerMarkerEvent:@"info_long_click" marker:marker];
  }
}
/**
 * @callback plugin.google.maps.event.MARKER_DRAG_START
 */
- (void) mapView:(GMSMapView *) mapView didBeginDraggingMarker:(GMSMarker *)marker
{
  [self syncInfoWndPosition];
  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  if ([markerTag hasPrefix:@"markercluster_"]) {
    [self triggerClusterEvent:@"marker_drag_start" marker:marker];
  } else {
    [self triggerMarkerEvent:@"marker_drag_start" marker:marker];
  }
}
/**
 * @callback plugin.google.maps.event.MARKER_DRAG_END
 */
- (void) mapView:(GMSMapView *) mapView didEndDraggingMarker:(GMSMarker *)marker
{
  [self syncInfoWndPosition];
  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  if ([markerTag hasPrefix:@"markercluster_"]) {
    [self triggerClusterEvent:@"marker_drag_end" marker:marker];
  } else {
    [self triggerMarkerEvent:@"marker_drag_end" marker:marker];
  }
}
/**
 * @callback plugin.google.maps.event.MARKER_DRAG
 */
- (void) mapView:(GMSMapView *) mapView didDragMarker:(GMSMarker *)marker
{
  [self syncInfoWndPosition];
  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  if ([markerTag hasPrefix:@"markercluster_"]) {
    [self triggerClusterEvent:@"marker_drag" marker:marker];
  } else {
    [self triggerMarkerEvent:@"marker_drag" marker:marker];
  }
}

- (void) syncInfoWndPosition {
  if (self.activeMarker == nil) {
    //NSLog(@"-->no active marker");
    return;
  }
  CLLocationCoordinate2D position = self.activeMarker.position;
  CGPoint point = [self.map.projection
                   pointForCoordinate:CLLocationCoordinate2DMake(position.latitude, position.longitude)];
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: 'syncPosition', callback: '_onSyncInfoWndPosition', args: [{x: %f, y: %f}]});}",
                        self.overlayId, self.overlayId, point.x, point.y ];
  [self execJS:jsString];
}

/**
 * @callback plugin.google.maps.event.MARKER_CLICK
 */
- (BOOL)mapView:(GMSMapView *)mapView didTapMarker:(GMSMarker *)marker {

  NSString *clusterId_markerId = [NSString stringWithString:marker.userData];

  if ([clusterId_markerId containsString:@"markercluster_"]) {
    if ([clusterId_markerId containsString:@"-marker_"]) {
      //NSLog(@"--->activeMarker = %@", marker.userData);
      self.map.selectedMarker = marker;
      self.activeMarker = marker;
      NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
      if ([markerTag hasPrefix:@"markercluster_"]) {
        [self triggerClusterEvent:@"marker_click" marker:marker];
      } else {
        [self triggerMarkerEvent:@"marker_click" marker:marker];
      }
    } else {
      if (self.activeMarker != nil) {
        NSString *markerTag = [NSString stringWithFormat:@"%@", self.activeMarker.userData];
        if ([markerTag hasPrefix:@"markercluster_"]) {
          if ([markerTag containsString:@"-marker_"]) {
            [self triggerClusterEvent:@"info_close" marker:self.activeMarker];
          }
//        } else {
//          [self triggerMarkerEvent:@"info_close" marker:self.activeMarker];
        }
      }
      [self triggerClusterEvent:@"cluster_click" marker:marker];
    }
  } else {
    [self execJS:@"javascript:if(window.cordova){cordova.fireDocumentEvent('plugin_touch', {});}"];
    [self triggerMarkerEvent:@"marker_click" marker:marker];
    //NSLog(@"--->activeMarker = %@", marker.userData);
    self.map.selectedMarker = marker;
    self.activeMarker = marker;
  }

  //NSArray *tmp = [clusterId_markerId componentsSeparatedByString:@"_"];
  //NSString *className = [tmp objectAtIndex:0];

  // Get the marker plugin
  //NSString *pluginId = [NSString stringWithFormat:@"%@-%@", self.overlayId, className];
  //CDVPlugin<IPluginProtocol> *plugin = [self.plugins objectForKey:pluginId];

  // Get the marker properties
  NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%@", clusterId_markerId];
  NSDictionary *properties = [self.objects objectForKey:markerPropertyId];

  BOOL disableAutoPan = false;
  if ([properties objectForKey:@"disableAutoPan"] != nil) {
    disableAutoPan = [[properties objectForKey:@"disableAutoPan"] boolValue];
    if (disableAutoPan) {
      return YES;
    }
  }

  //--------------------------
  // Pan the camera mondatory
  //--------------------------
  GMSCameraPosition* cameraPosition = [GMSCameraPosition
                                       cameraWithTarget:marker.position zoom:self.map.camera.zoom];

  [self.map animateToCameraPosition:cameraPosition];
  return YES;
}

- (void)mapView:(GMSMapView *)mapView didCloseInfoWindowOfMarker:(nonnull GMSMarker *)marker {

  // Get the marker plugin
  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  if ([markerTag hasPrefix:@"markercluster_"]) {
    if ([markerTag containsString:@"-marker_"]) {
      [self triggerClusterEvent:@"info_close" marker:marker];
    }
  } else {
    [self triggerMarkerEvent:@"info_close" marker:marker];
  }

  //self.map.selectedMarker = nil; // <-- this causes the didCloseInfoWindowOfMarker event again
  //self.activeMarker = nil; // <-- This causes HTMLinfoWindow is not able to close when you tap on the map.
}


/*
 - (void)mapView:(GMSMapView *)mapView didTapOverlay:(GMSOverlay *)overlay {
 NSString *overlayClass = NSStringFromClass([overlay class]);
 if ([overlayClass isEqualToString:@"GMSPolygon"] ||
 [overlayClass isEqualToString:@"GMSPolyline"] ||
 [overlayClass isEqualToString:@"GMSCircle"] ||
 [overlayClass isEqualToString:@"GMSGroundOverlay"]) {
 [self triggerOverlayEvent:@"overlay_click" id:overlay.title];
 }
 }
 */

/**
 * Map tiles are loaded
 */
- (void) mapViewDidFinishTileRendering:(GMSMapView *)mapView {
  // no longer available from v2.2.0
  //[self triggerMapEvent:@"map_loaded"];
}

/**
 * plugin.google.maps.event.MAP_***()) events
 */
- (void)triggerMapEvent: (NSString *)eventName
{

  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onMapEvent', args: []});}",
                        self.overlayId, self.overlayId, eventName];
  [self execJS:jsString];
}

/**
 * plugin.google.maps.event.MAP_***(new google.maps.LatLng(lat,lng)) events
 */
- (void)triggerMapEvent: (NSString *)eventName coordinate:(CLLocationCoordinate2D)coordinate
{

  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onMapEvent', args: [new plugin.google.maps.LatLng(%f,%f)]});}",
                        self.overlayId, self.overlayId, eventName, coordinate.latitude, coordinate.longitude];
  [self execJS:jsString];
}

/**
 * plugin.google.maps.event.CAMERA_*** events
 */
- (void)triggerCameraEvent: (NSString *)eventName position:(GMSCameraPosition *)position
{

  NSMutableDictionary *target = [NSMutableDictionary dictionary];
  [target setObject:[NSNumber numberWithDouble:position.target.latitude] forKey:@"lat"];
  [target setObject:[NSNumber numberWithDouble:position.target.longitude] forKey:@"lng"];

  NSMutableDictionary *json = [NSMutableDictionary dictionary];
  [json setObject:[NSNumber numberWithFloat:position.bearing] forKey:@"bearing"];
  [json setObject:target forKey:@"target"];
  [json setObject:[NSNumber numberWithDouble:position.viewingAngle] forKey:@"tilt"];
  [json setObject:[NSNumber numberWithInt:(int)position.hash] forKey:@"hashCode"];
  [json setObject:[NSNumber numberWithFloat:position.zoom] forKey:@"zoom"];


  GMSVisibleRegion visibleRegion = self.map.projection.visibleRegion;
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:visibleRegion];
  NSMutableDictionary *northeast = [NSMutableDictionary dictionary];
  NSMutableDictionary *southwest = [NSMutableDictionary dictionary];

  [northeast setObject:[NSNumber numberWithDouble:bounds.northEast.latitude] forKey:@"lat"];
  [northeast setObject:[NSNumber numberWithDouble:bounds.northEast.longitude] forKey:@"lng"];
  [json setObject:northeast forKey:@"northeast"];
  [southwest setObject:[NSNumber numberWithDouble:bounds.southWest.latitude] forKey:@"lat"];
  [southwest setObject:[NSNumber numberWithDouble:bounds.southWest.longitude] forKey:@"lng"];
  [json setObject:southwest forKey:@"southwest"];



  NSMutableDictionary *farLeft = [NSMutableDictionary dictionary];
  [farLeft setObject:[NSNumber numberWithDouble:visibleRegion.farLeft.latitude] forKey:@"lat"];
  [farLeft setObject:[NSNumber numberWithDouble:visibleRegion.farLeft.longitude] forKey:@"lng"];
  [json setObject:farLeft forKey:@"farLeft"];

  NSMutableDictionary *farRight = [NSMutableDictionary dictionary];
  [farRight setObject:[NSNumber numberWithDouble:visibleRegion.farRight.latitude] forKey:@"lat"];
  [farRight setObject:[NSNumber numberWithDouble:visibleRegion.farRight.longitude] forKey:@"lng"];
  [json setObject:farRight forKey:@"farRight"];

  NSMutableDictionary *nearLeft = [NSMutableDictionary dictionary];
  [nearLeft setObject:[NSNumber numberWithDouble:visibleRegion.nearLeft.latitude] forKey:@"lat"];
  [nearLeft setObject:[NSNumber numberWithDouble:visibleRegion.nearLeft.longitude] forKey:@"lng"];
  [json setObject:nearLeft forKey:@"nearLeft"];

  NSMutableDictionary *nearRight = [NSMutableDictionary dictionary];
  [nearRight setObject:[NSNumber numberWithDouble:visibleRegion.nearRight.latitude] forKey:@"lat"];
  [nearRight setObject:[NSNumber numberWithDouble:visibleRegion.nearRight.longitude] forKey:@"lng"];
  [json setObject:nearRight forKey:@"nearRight"];

  NSData* jsonData = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
  NSString* sourceArrayString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onCameraEvent', args: [%@]});}",
                        self.overlayId, self.overlayId, eventName, sourceArrayString];
  [self execJS:jsString];

  [self syncInfoWndPosition];
}


/**
 * cluster_*** events
 */
- (void)triggerClusterEvent: (NSString *)eventName marker:(GMSMarker *)marker
{
  if (marker.userData == nil) {
    return;
  }

  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  NSArray *tmp = [markerTag componentsSeparatedByString:@"-"];
  NSString *clusterId = [tmp objectAtIndex:0];
  NSString *markerId = [tmp objectAtIndex:1];

  // Get the marker plugin
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onClusterEvent', args: ['%@', '%@', new plugin.google.maps.LatLng(%f, %f)]});}",
                        self.overlayId, self.overlayId, eventName, clusterId, markerId,
                        marker.position.latitude,
                        marker.position.longitude];
  [self execJS:jsString];
}

/**
 * plugin.google.maps.event.MARKER_*** events
 */
- (void)triggerMarkerEvent: (NSString *)eventName marker:(GMSMarker *)marker
{

  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  NSArray *tmp = [markerTag componentsSeparatedByString:@"-"];
  NSString *markerId = [tmp objectAtIndex:([tmp count] - 1)];

  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onMarkerEvent', args: ['%@', new plugin.google.maps.LatLng(%f, %f)]});}",
                        self.overlayId, self.overlayId, eventName, markerId,
                        marker.position.latitude,
                        marker.position.longitude];
  [self execJS:jsString];
}

/**
 * Involve App._onOverlayEvent
 */
- (void)triggerOverlayEvent: (NSString *)eventName overlayId:(NSString*)overlayId coordinate:(CLLocationCoordinate2D)coordinate
{
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onOverlayEvent', args: ['%@', new plugin.google.maps.LatLng(%f, %f)]});}",
                        self.overlayId, self.overlayId, eventName, overlayId, coordinate.latitude, coordinate.longitude];
  [self execJS:jsString];
}

//future support: custom info window
-(UIView *)mapView:(GMSMapView *)mapView markerInfoWindow:(GMSMarker*)marker
{
  CGSize rectSize;
  CGSize textSize;
  CGSize snippetSize;
  UIFont *titleFont;
  UIFont *snippetFont;
  UIImage *base64Image;

  Boolean isTextMode = false;
  NSString *title = marker.title;
  NSString *snippet = marker.snippet;


  // Get the marker plugin
  //NSString *pluginId = [NSString stringWithFormat:@"%@-marker", self.overlayId];
  //CDVPlugin<IPluginProtocol> *plugin = [self.plugins objectForKey:pluginId];

  // Get the marker properties
  NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%@", marker.userData];
  NSDictionary *properties = [self.objects objectForKey:markerPropertyId];
  Boolean useHtmlInfoWnd = marker.title == nil && marker.snippet == nil;

  if (useHtmlInfoWnd) {
    [self syncInfoWndPosition];
    NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
    if ([markerTag hasPrefix:@"markercluster_"]) {
      [self triggerClusterEvent:@"info_open" marker:marker];
    } else {
      [self triggerMarkerEvent:@"info_open" marker:marker];
    }

    return [[UIView alloc]initWithFrame:CGRectMake(0, 0, 1, 1)];
  }

  if (title == nil) {
    return NULL;
  }

  NSString *markerTag = [NSString stringWithFormat:@"%@", marker.userData];
  if ([markerTag hasPrefix:@"markercluster_"]) {
    [self triggerClusterEvent:@"info_open" marker:marker];
  } else {
    [self triggerMarkerEvent:@"info_open" marker:marker];
  }

  // Load styles
  NSDictionary *styles = nil;
  if ([properties objectForKey:@"styles"]) {
    styles = [properties objectForKey:@"styles"];
  }

  // Load images
  UIImage *leftImg = nil;
  UIImage *rightImg = nil;[self loadImageFromGoogleMap:@"bubble_right@2x"];
  leftImg = [self loadImageFromGoogleMap:@"bubble_left@2x"];
  rightImg = [self loadImageFromGoogleMap:@"bubble_right@2x"];
  float scale = leftImg.scale;
  int sizeEdgeWidth = 10;

  int width = 0;

  if (styles && [styles objectForKey:@"width"]) {
    NSString *widthString = [styles valueForKey:@"width"];

    // check if string is numeric
    NSNumberFormatter *nf = [[NSNumberFormatter alloc] init];
    BOOL isNumeric = [nf numberFromString:widthString] != nil;

    if ([widthString hasSuffix:@"%"]) {
      double widthDouble = [[widthString stringByReplacingOccurrencesOfString:@"%" withString:@""] doubleValue];

      width = (int)((double)mapView.frame.size.width * (widthDouble / 100));
    } else if (isNumeric) {
      double widthDouble = [widthString doubleValue];

      if (widthDouble <= 1.0) {
        width = (int)((double)mapView.frame.size.width * (widthDouble));
      } else {
        width = (int)widthDouble;
      }
    }
  }

  int maxWidth = 0;

  if (styles && [styles objectForKey:@"maxWidth"]) {
    NSString *widthString = [styles valueForKey:@"maxWidth"];

    NSNumberFormatter *nf = [[NSNumberFormatter alloc] init];
    BOOL isNumeric = [nf numberFromString:widthString] != nil;

    if ([widthString hasSuffix:@"%"]) {
      double widthDouble = [[widthString stringByReplacingOccurrencesOfString:@"%" withString:@""] doubleValue];

      maxWidth = (int)((double)mapView.frame.size.width * (widthDouble / 100));

      // make sure to take padding into account.
      maxWidth -= sizeEdgeWidth;
    } else if (isNumeric) {
      double widthDouble = [widthString doubleValue];

      if (widthDouble <= 1.0) {
        maxWidth = (int)((double)mapView.frame.size.width * (widthDouble));
      } else {
        maxWidth = (int)widthDouble;
      }
    }
  }

  //-------------------------------------
  // Calculate the size for the contents
  //-------------------------------------
  if ([title rangeOfString:@"data:image/"].location != NSNotFound &&
      [title rangeOfString:@";base64,"].location != NSNotFound) {

    isTextMode = false;
    NSArray *tmp = [title componentsSeparatedByString:@","];
    NSData *decodedData = [[NSData alloc] initWithBase64EncodedString:[tmp objectAtIndex:1] options:0];
    base64Image = [[UIImage alloc] initWithData:decodedData];
    rectSize = CGSizeMake(base64Image.size.width + leftImg.size.width, base64Image.size.height + leftImg.size.height / 2);

  } else {

    isTextMode = true;

    BOOL isBold = FALSE;
    BOOL isItalic = FALSE;
    if (styles) {
      if ([[styles objectForKey:@"font-style"] isEqualToString:@"italic"]) {
        isItalic = TRUE;
      }
      if ([[styles objectForKey:@"font-weight"] isEqualToString:@"bold"]) {
        isBold = TRUE;
      }
    }
    if (isBold == TRUE && isItalic == TRUE) {
      // ref: http://stackoverflow.com/questions/4713236/how-do-i-set-bold-and-italic-on-uilabel-of-iphone-ipad#21777132
      titleFont = [UIFont systemFontOfSize:17.0f];
      UIFontDescriptor *fontDescriptor = [titleFont.fontDescriptor
                                          fontDescriptorWithSymbolicTraits:UIFontDescriptorTraitBold | UIFontDescriptorTraitItalic];
      titleFont = [UIFont fontWithDescriptor:fontDescriptor size:0];
    } else if (isBold == TRUE && isItalic == FALSE) {
      titleFont = [UIFont boldSystemFontOfSize:17.0f];
    } else if (isBold == TRUE && isItalic == FALSE) {
      titleFont = [UIFont italicSystemFontOfSize:17.0f];
    } else {
      titleFont = [UIFont systemFontOfSize:17.0f];
    }

    // Calculate the size for the title strings
    CGRect textRect = [title boundingRectWithSize:CGSizeMake(mapView.frame.size.width - 13, mapView.frame.size.height - 13)
                                 options:NSStringDrawingUsesLineFragmentOrigin
                              attributes:@{NSFontAttributeName:titleFont}
                                 context:nil];
    textSize = textRect.size;
    rectSize = CGSizeMake(textSize.width + 10, textSize.height + 22);

    // Calculate the size for the snippet strings
    if (snippet) {
      snippetFont = [UIFont systemFontOfSize:12.0f];
      snippet = [snippet stringByReplacingOccurrencesOfString:@"\n" withString:@""];
      textRect = [snippet boundingRectWithSize:CGSizeMake(mapView.frame.size.width - 13, mapView.frame.size.height - 13)
                                 options:NSStringDrawingUsesLineFragmentOrigin
                              attributes:@{NSFontAttributeName:snippetFont}
                                 context:nil];
      snippetSize = textRect.size;
      rectSize.height += snippetSize.height + 4;
      if (rectSize.width < snippetSize.width + leftImg.size.width) {
        rectSize.width = snippetSize.width + leftImg.size.width;
      }
    }
  }
  if (rectSize.width < leftImg.size.width * scale) {
    rectSize.width = leftImg.size.width * scale;
  } else {
    rectSize.width += sizeEdgeWidth;
  }

  if (width > 0) {
    rectSize.width = width;
  }
  if (maxWidth > 0 &&
      maxWidth < rectSize.width) {
    rectSize.width = maxWidth;
  }

  //-------------------------------------
  // Draw the the info window
  //-------------------------------------
  UIGraphicsBeginImageContextWithOptions(rectSize, NO, 0.0f);

  CGRect trimArea = CGRectMake(15, 0, 5, MIN(45, rectSize.height - 20));

  trimArea = CGRectMake(15, 0, 15, leftImg.size.height);
  if (scale > 1.0f) {
    trimArea = CGRectMake(trimArea.origin.x * scale,
                          trimArea.origin.y * scale,
                          trimArea.size.width * scale +1,
                          trimArea.size.height * scale);
  }
  CGImageRef shadowImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
  UIImage *shadowImageLeft = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUp];
  UIImage *shadowImageRight = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUpMirrored];
  CGImageRelease(shadowImageRef);

  int y;
  int i = 0;
  int x = shadowImageLeft.size.width;
  float centerPos = rectSize.width * 0.5f;
  while (centerPos - x > shadowImageLeft.size.width) {
    y = 1;
    while (y + shadowImageLeft.size.height < rectSize.height) {
      [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
      [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];
      y += shadowImageRight.size.height;
    }
    y = rectSize.height - shadowImageLeft.size.height;
    [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
    [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];

    if (i == 0) {
      x += 5;

      trimArea = CGRectMake(15, 0, 5, leftImg.size.height);
      if (scale > 1.0f) {
        trimArea = CGRectMake(trimArea.origin.x * scale,
                              trimArea.origin.y * scale,
                              trimArea.size.width * scale,
                              trimArea.size.height * scale);
      }
      shadowImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
      shadowImageLeft = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUp];
      shadowImageRight = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUpMirrored];
      CGImageRelease(shadowImageRef);

    } else {
      x += shadowImageLeft.size.width;
    }
    i++;
  }

  // Draw left & right side edges
  x -= shadowImageLeft.size.width;
  trimArea = CGRectMake(0, 0, sizeEdgeWidth, leftImg.size.height);
  if (scale > 1.0f) {
    trimArea = CGRectMake(trimArea.origin.x * scale,
                          trimArea.origin.y * scale,
                          trimArea.size.width * scale,
                          trimArea.size.height * scale);
  }
  shadowImageRef = CGImageCreateWithImageInRect(leftImg.CGImage, trimArea);
  shadowImageLeft = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUp];
  shadowImageRight = [UIImage imageWithCGImage:shadowImageRef scale:scale orientation:UIImageOrientationUpMirrored];
  CGImageRelease(shadowImageRef);
  x += shadowImageLeft.size.width;

  y = 1;
  while (y + shadowImageLeft.size.height < rectSize.height) {
    [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
    [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];
    y += shadowImageRight.size.height;
  }
  y = rectSize.height - shadowImageLeft.size.height;
  [shadowImageLeft drawAtPoint:CGPointMake(centerPos - x, y)];
  [shadowImageRight drawAtPoint:CGPointMake(centerPos + x - shadowImageLeft.size.width, y)];

  // Fill the body area with WHITE color
  CGContextRef context = UIGraphicsGetCurrentContext();
  CGContextSetAllowsAntialiasing(context, true);
  CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0);

  if (isTextMode) {

    if (snippet) {
      CGContextFillRect(context, CGRectMake(centerPos - x + 5, 4, rectSize.width - (centerPos - x + 7), rectSize.height - 16));
    } else {
      CGContextFillRect(context, CGRectMake(centerPos - x + 5, 0, rectSize.width - (centerPos - x + 7), rectSize.height - 11));
    }
  } else {
    CGContextFillRect(context, CGRectMake(centerPos - x + 5, 4, rectSize.width - (centerPos - x + 5), rectSize.height - 16));
  }

  //--------------------------------
  // text-align: left/center/right
  //--------------------------------
  NSTextAlignment textAlignment = NSTextAlignmentLeft;
  if (styles && [styles objectForKey:@"text-align"]) {
    NSString *textAlignValue = [styles objectForKey:@"text-align"];

    NSDictionary *aligments = [NSDictionary dictionaryWithObjectsAndKeys:
                               ^() {return NSTextAlignmentLeft; }, @"left",
                               ^() {return NSTextAlignmentRight; }, @"right",
                               ^() {return NSTextAlignmentCenter; }, @"center",
                               nil];

    typedef NSTextAlignment (^CaseBlock)();
    CaseBlock caseBlock = aligments[textAlignValue];
    if (caseBlock) {
      textAlignment = caseBlock();
    }
  }

  //-------------------------------------
  // Draw the contents
  //-------------------------------------
  if (isTextMode) {
    //Draw the title strings
    if (title) {
      UIColor *titleColor = [UIColor blackColor];
      if (styles && [styles objectForKey:@"color"]) {
        titleColor = [[styles valueForKey:@"color"] parsePluginColor];
      }

      CGRect textRect = CGRectMake(5, 5 , rectSize.width - 10, textSize.height );
      NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
      style.lineBreakMode = NSLineBreakByWordWrapping;
      style.alignment = textAlignment;

      NSDictionary *attributes = @{
                                   NSForegroundColorAttributeName : titleColor,
                                   NSFontAttributeName : titleFont,
                                   NSParagraphStyleAttributeName : style
                                   };
      [title drawInRect:textRect
         withAttributes:attributes];
      //CGContextSetRGBStrokeColor(context, 1.0, 0.0, 0.0, 0.5);
      //CGContextStrokeRect(context, textRect);
    }

    //Draw the snippet
    if (snippet) {
      CGRect textRect = CGRectMake(5, textSize.height + 10 , rectSize.width - 10, snippetSize.height );
      NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
      style.lineBreakMode = NSLineBreakByWordWrapping;
      style.alignment = textAlignment;

      NSDictionary *attributes = @{
                                   NSForegroundColorAttributeName : [UIColor grayColor],
                                   NSFontAttributeName : snippetFont,
                                   NSParagraphStyleAttributeName : style
                                   };
      [snippet drawInRect:textRect withAttributes:attributes];
    }
  } else {
    //Draw the content image
    CGRect imageRect = CGRectMake((rectSize.width - base64Image.size.width) / 2 ,
                                  -1 * ((rectSize.height - base64Image.size.height - 20) / 2 + 7.5),
                                  base64Image.size.width, base64Image.size.height);
    CGContextTranslateCTM(context, 0, base64Image.size.height);
    CGContextScaleCTM(context, 1.0, -1.0);
    CGContextDrawImage(context, imageRect, base64Image.CGImage);
  }

  //-------------------------------------
  // Generate new image
  //-------------------------------------
  UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
  UIGraphicsEndImageContext();

  UIImageView *imageView = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, rectSize.width, rectSize.height)];
  [imageView setContentMode:UIViewContentModeScaleAspectFill];
  [imageView setImage:image];
  return imageView;
}

-(UIImage *)loadImageFromGoogleMap:(NSString *)fileName {
  NSString *imagePath = [[NSBundle bundleWithIdentifier:@"com.google.GoogleMaps"] pathForResource:fileName ofType:@"png"];
  return [[UIImage alloc] initWithContentsOfFile:imagePath];
}


- (void) didChangeActiveBuilding: (GMSIndoorBuilding *)building {
  if (building == nil) {
    return;
  }
  //Notify to the JS
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: 'indoor_building_focused', callback: '_onMapEvent'});}",
                        self.overlayId, self.overlayId];
  [self execJS:jsString];
}

- (void) didChangeActiveLevel: (GMSIndoorLevel *)activeLevel {

  if (activeLevel == nil) {
    return;
  }
  GMSIndoorBuilding *building = self.map.indoorDisplay.activeBuilding;

  NSMutableDictionary *result = [NSMutableDictionary dictionary];

  NSUInteger activeLevelIndex = [building.levels indexOfObject:activeLevel];
  [result setObject:[NSNumber numberWithInteger:activeLevelIndex] forKey:@"activeLevelIndex"];
  [result setObject:[NSNumber numberWithInteger:building.defaultLevelIndex] forKey:@"defaultLevelIndex"];

  GMSIndoorLevel *level;
  NSMutableDictionary *levelInfo;
  NSMutableArray *levels = [NSMutableArray array];
  for (level in building.levels) {
    levelInfo = [NSMutableDictionary dictionary];

    [levelInfo setObject:[NSString stringWithString:level.name] forKey:@"name"];
    [levelInfo setObject:[NSString stringWithString:level.shortName] forKey:@"shortName"];
    [levels addObject:levelInfo];
  }
  [result setObject:levels forKey:@"levels"];

  NSError *error;
  NSData *data = [NSJSONSerialization dataWithJSONObject:result options:NSJSONWritingPrettyPrinted error:&error];


  NSString *JSONstring = [[NSString alloc] initWithData:data
                                               encoding:NSUTF8StringEncoding];

  //Notify to the JS
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: 'indoor_level_activated', callback: '_onMapEvent', args: [%@]});}",
                        self.overlayId, self.overlayId, JSONstring];

  [self execJS:jsString];
}

@end
