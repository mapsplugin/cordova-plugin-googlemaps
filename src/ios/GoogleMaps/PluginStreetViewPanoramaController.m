//
//  PluginStreetViewPanoramaController.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginStreetViewPanoramaController.h"

@implementation PluginStreetViewPanoramaController

//This is invoked every time the view.panorama property changes.
- (void)panoramaView:(GMSPanoramaView *)view didMoveToPanorama:(nullable GMSPanorama *)panorama {
  [self locationChangeEvent:panorama.coordinate];
  
}

// Called when the panorama change was caused by invoking moveToPanoramaNearCoordinate:.
- (void)panoramaView:(GMSPanoramaView *)view didMoveToPanorama:(nonnull GMSPanorama *)panorama nearCoordinate:(CLLocationCoordinate2D)coordinate {
  [self locationChangeEvent:coordinate];
}

// Called when moveNearCoordinate: produces an error.
- (void)panoramaView:(GMSPanoramaView *)view error:(nonnull NSError *)error onMoveNearCoordinate:(CLLocationCoordinate2D)coordinate {
  [self locationChangeEvent:coordinate];
}


// Called when moveToPanoramaID: produces an error.
- (void)panoramaView:(GMSPanoramaView *)view error:(nonnull NSError *)error onMoveToPanoramaID:(nonnull NSString *)panoramaID {
  [self locationChangeEvent:view.panorama.coordinate];
}

// Called repeatedly during changes to the camera on GMSPanoramaView.
- (void)panoramaView:(GMSPanoramaView *)view didMoveCamera:(nonnull GMSPanoramaCamera *)camera {
  
  NSMutableDictionary *cameraInfo = [NSMutableDictionary dictionary];
  [cameraInfo setObject:[NSNumber numberWithDouble:camera.orientation.heading] forKey:@"bearing"];
  [cameraInfo setObject:[NSNumber numberWithDouble:camera.orientation.pitch] forKey:@"tilt"];
  [cameraInfo setObject:[NSNumber numberWithDouble:camera.zoom] forKey:@"zoom"];

  NSData* jsonData = [NSJSONSerialization dataWithJSONObject:cameraInfo options:0 error:nil];
  NSString* sourceArrayString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onPanoramaCameraChange', args: [%@]});}",
                        self.overlayId, self.overlayId, @"panorama_camera_change", sourceArrayString];
  [self execJS:jsString];
}

// Called when a user has tapped on the GMSPanoramaView, but this tap was not consumed (taps may be consumed by e.g., tapping on a navigation arrow).
- (void)panoramaView:(GMSPanoramaView *)view didTap:(CGPoint)point {

  GMSOrientation svOrientation = [self.panoramaView orientationForPoint:point];
  
  NSMutableDictionary *clickInfo = [NSMutableDictionary dictionary];
  NSMutableDictionary *orientaion = [NSMutableDictionary dictionary];
  [orientaion setObject:[NSNumber numberWithDouble:svOrientation.heading] forKey:@"bearing"];
  [orientaion setObject:[NSNumber numberWithDouble:svOrientation.pitch] forKey:@"tilt"];
  [clickInfo setObject:orientaion forKey:@"orientation"];
  
  NSMutableArray *pointArray = [NSMutableArray array];
  [pointArray addObject:[NSNumber numberWithInt:point.x]];
  [pointArray addObject:[NSNumber numberWithInt:point.y]];
  [clickInfo setObject:pointArray forKey:@"point"];

  NSData* jsonData = [NSJSONSerialization dataWithJSONObject:clickInfo options:0 error:nil];
  NSString* sourceArrayString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onPanoramaEvent', args: [%@]});}",
                        self.overlayId, self.overlayId, @"panorama_click", sourceArrayString];
  [self execJS:jsString];
  
}

- (void) locationChangeEvent:(CLLocationCoordinate2D)coordinate {

  NSMutableDictionary *location = [NSMutableDictionary dictionary];
  [location setObject:self.panoramaView.panorama.panoramaID forKey:@"panoId"];
  
  NSMutableDictionary *target = [NSMutableDictionary dictionary];
  [target setObject:[NSNumber numberWithDouble:coordinate.latitude] forKey:@"lat"];
  [target setObject:[NSNumber numberWithDouble:coordinate.longitude] forKey:@"lng"];
  [location setObject:target forKey:@"position"];
  
  NSMutableArray *links = [NSMutableArray array];
  for (GMSPanoramaLink *linkRef in self.panoramaView.panorama.links) {
    NSMutableDictionary *link = [NSMutableDictionary dictionary];
    [link setObject:linkRef.panoramaID forKey:@"panoId"];
    [link setObject:[NSNumber numberWithDouble:linkRef.heading] forKey:@"bearing"];
    [links addObject:link];
  }
  [location setObject:links forKey:@"links"];

  NSData* jsonData = [NSJSONSerialization dataWithJSONObject:location options:0 error:nil];
  NSString* sourceArrayString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
  NSString* jsString = [NSString
                        stringWithFormat:@"javascript:if('%@' in plugin.google.maps){plugin.google.maps['%@']({evtName: '%@', callback: '_onPanoramaLocationChange', args: [%@]});}",
                        self.overlayId, self.overlayId, @"panorama_location_change", sourceArrayString];
  [self execJS:jsString];
}
@end
