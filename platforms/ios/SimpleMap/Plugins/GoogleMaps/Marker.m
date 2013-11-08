//
//  Marker.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "Marker.h"

@implementation Marker
-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}

/**
 * @param marker options
 * @return marker key
 */
-(void)createMarker:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  float latitude = [[json valueForKey:@"lat"] floatValue];
  float longitude = [[json valueForKey:@"lng"] floatValue];
  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
  GMSMarker *marker = [GMSMarker markerWithPosition:position];
  if ([[json valueForKey:@"visible"] boolValue]) {
    marker.map = self.mapCtrl.map;
  }
  
  marker.title = [json valueForKey:@"title"];
  marker.snippet = [json valueForKey:@"snippet"];
  marker.draggable = [[json valueForKey:@"draggable"] boolValue];
  marker.flat = [[json valueForKey:@"flat"] boolValue];
  marker.rotation = [[json valueForKey:@"flat"] floatValue];
  
  NSString *key = [NSString stringWithFormat:@"marker%d", marker.hash];
  [self.mapCtrl.markerManager setObject:marker forKey: key];
  
  // Create icon
  NSString *iconPath = [json valueForKey:@"icon"];
  if (iconPath) {
    NSRange range = [iconPath rangeOfString:@"http"];
    if (range.location == NSNotFound) {
      marker.icon  = [UIImage imageNamed:iconPath];
    } else {
      dispatch_queue_t gueue = dispatch_queue_create("GoogleMap_addMarker", NULL);
      dispatch_sync(gueue, ^{
        NSURL *url = [NSURL URLWithString:iconPath];
        NSData *data = [NSData dataWithContentsOfURL:url];
        marker.icon = [UIImage imageWithData:data];
      });
      dispatch_release(gueue);
      
    }
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: key];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Show the infowindow of the current marker
 * @params MarkerKey
 */
-(void)showInfoWindow:(CDVInvokedUrlCommand *)command
{
  
  NSString *hashCode = [command.arguments objectAtIndex:1];
  
  GMSMarker *marker = [self.mapCtrl.markerManager objectForKey:hashCode];
  if (marker) {
    self.mapCtrl.map.selectedMarker = marker;
  }
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  
}
/**
 * Hide current infowindow
 * @params MarkerKey
 */
-(void)hideInfoWindow:(CDVInvokedUrlCommand *)command
{
  self.mapCtrl.map.selectedMarker = nil;
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * @params MarkerKey
 * @return current marker position with array(latitude, longitude)
 */
-(void)getPosition:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  
  GMSMarker *marker = [self.mapCtrl.markerManager objectForKey:markerKey];
  NSNumber *latitude = @0.0;
  NSNumber *longitude = @0.0;
  if (marker) {
    latitude = [NSNumber numberWithFloat: marker.position.latitude];
    longitude = [NSNumber numberWithFloat: marker.position.longitude];
  }
  NSArray *latlng = @[latitude, longitude];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:latlng];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)setTitle:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.markerManager objectForKey:markerKey];
  marker.title = [command.arguments objectAtIndex:2];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)setSnippet:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.markerManager objectForKey:markerKey];
  marker.snippet = [command.arguments objectAtIndex:2];
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the specified marker
 * @params MarkerKey
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
  NSString *markerKey = [command.arguments objectAtIndex:1];
  GMSMarker *marker = [self.mapCtrl.markerManager objectForKey:markerKey];
  marker.map = nil;
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
