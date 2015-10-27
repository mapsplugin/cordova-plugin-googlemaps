//
//  GAnimatedClusterRenderer.m
//  Erdgastankstellen
//
//  Created by Christian on 26.05.15.
//
//

#import <CoreText/CoreText.h>
#import "GAnimatedClusterRenderer.h"
#import "GCluster.h"

@implementation GAnimatedClusterRenderer {
  
  /* Holds the last zoom level to detect if it is nessesary to render new clusters. */
  int previewsDiscreteZoom;
  
  /* The time of one animation */
  float animationTime;
  
  /* The minimal amount of markers inside an area that will be summarized in a cluster. */
  NSUInteger minimumClusterCount;
    
  GMSVisibleRegion viewPort;
  
  /* Holds the clusters that are on the map while new clusters are rendered. */
  NSMutableSet *oldMarkers;
  
  /* Holds all markers that are currently on the map. To render only the markers in the viewport. */
  NSMutableSet *markers;
  
  /* A container to hold cluster images to avoid dublications and save memory. */
  NSMutableDictionary * clusterIcons;
}

-(id)init {
  
  self = [self initWithMapView:nil];
  
  return [super init];
}

-(id)initWithMapView:(GMSMapView *)googleMap {
  
  animationTime = 0.15;
  
  oldMarkers = [[NSMutableSet alloc] init];
  
  markers = [[NSMutableSet alloc]init];
  
  clusterIcons = [[NSMutableDictionary alloc] init];
  
  minimumClusterCount = 4;
  
  previewsDiscreteZoom = 0;
  
  return [super initWithMapView:googleMap];
}

-(void)clustersChanged:(NSSet *)clusters {
    
  NSLog(@"clusterChanged Not implemented here use GDefaultClusterRenderer");
}

-(void)clustersChanged:(NSSet *)clusters inRegion:(GMSVisibleRegion)region {

  NSLog(@"clusterChanged Not implemented here use GDefaultClusterRenderer");
}

-(void)freeClusterIconCache {
  clusterIcons = [[NSMutableDictionary alloc] init];
}

-(void)clustersChanged:(NSSet *)clusters inRegion:(GMSVisibleRegion)region
              withZoom:(int)currentDiscreteZoom {
  
//  NSLog(@"\n\ncluster changed - new - %lu", (unsigned long)clusters.count);
  
  if (clusters.count == 0) {                                                    // if there are no clusters, there is nothing to do.
//    NSLog(@"-0-");
    return;
  }
  if (previewsDiscreteZoom == 0) {
    previewsDiscreteZoom = currentDiscreteZoom;                                 // initialize previewsDiscreteZoom.
  }
  viewPort = region;                                                            // set classglobal region.
  
  if (oldMarkers.count == 0) {                                                  // first call of this method
//    NSLog(@"-1-");
    markers = [self makeMarkersFrom:clusters];                                  // create markers from incomming markerdata.
    
    [self renderClusterMarkers:markers inRegion:viewPort];                      // render markers on the map.
    
    oldMarkers = [[NSMutableSet alloc ] initWithSet: clusters];                 // initalize oldMarkers.
    
    return;                                                                     // nothing else to do in first method call.
  }
  
  if (previewsDiscreteZoom != currentDiscreteZoom) {
    
    [self clearCache:markers];                                                    // remove all markers from the map and empty the markers container.
    
    NSMutableDictionary *clusterBundle = nil;
    
    if (previewsDiscreteZoom < currentDiscreteZoom) {                             // Zoom in
     
      clusterBundle = [self bundleChilds:clusters inParents:oldMarkers];
      markers = [self renderClustersZoomIn:clusterBundle];
//       NSLog(@"-2- bundle: %lu", clusterBundle.count);
    }
    else if (previewsDiscreteZoom > currentDiscreteZoom){                         // Zoom out
//      NSLog(@"-3-");
      clusterBundle = [self bundleChilds:oldMarkers inParents:clusters];
      markers = [self renderClustersZoomOut:clusterBundle toParents:clusters];
    }
    
    previewsDiscreteZoom = currentDiscreteZoom;
    
//    NSLog(@"Markers count: %lu", (unsigned long)markers.count);
  }
  else {
//    NSLog(@"-4-");
    [self updateCluster:clusters inRegion:viewPort];
  }
  
  [self updateViewPortInRegion:viewPort];
  
  oldMarkers = [[NSMutableSet alloc ] initWithSet: clusters];
}


-(NSMutableSet *)renderClustersZoomIn:(NSMutableDictionary *)bundle {
  
//  NSLog(@"Zoom in: %lu clusters", (unsigned long)bundle.count);
  
  NSMutableSet *result = [[NSMutableSet alloc]init];
  
  NSArray *keys = [bundle allKeys];
  
  for (NSString *key in keys) {
    
    CLLocationCoordinate2D parentPosition = [self getPostitionOfMarkerWithHash:key];
    
    NSArray * children = [bundle objectForKey:key];
    
    [result unionSet:[self moveMarkers:children from:parentPosition]];
  }
  
  return result;
}


-(NSMutableSet *)renderClustersZoomOut:(NSMutableDictionary *)bundle toParents:(NSSet *)parents {
  
//  NSLog(@"Zoom out");
  
  NSMutableSet * result = [[NSMutableSet alloc]init];
  
  NSArray *keys = [bundle allKeys];
  
  for (NSString *key in keys) {
    // parent
    CLLocationCoordinate2D parentPosition = [self getPostitionOfMarkerWithHash:key in:parents];
    
    NSArray * children = [bundle objectForKey:key];
    
    [self moveMarkers:children to:parentPosition];
  }
  
  for (id<GCluster> c in parents) {
    
    GMSMarker *marker = [self makeMarkerFrom:c];
    
    [result addObject:marker];
  }
  
  return result;
}

/**
 * Creates a Dictionary with the keys of the parenthashes. The value is an array 
 * with all childmarkers of the parent calculated by the distance.
 * @param NSSet child: All childclusters that should be assigned to parentclusters.
 * @param NSSet parents: All parentclusters to assingn the childclusters.
 * @return A Dictionary that includes all parenthashes as keys and the assigned 
 *         childclusters in an array as values.
 */
-(NSMutableDictionary*)bundleChilds:(NSSet*)child inParents:(NSSet*)parents {
  
  NSMutableDictionary * result  = [[NSMutableDictionary alloc] init];
  NSMutableSet * childClusters  = [[NSMutableSet alloc]initWithSet:child];
  
  for (id <GCluster> parent in parents) {
    NSString *key = [NSString stringWithFormat:@"%lu", (unsigned long)parent.marker.hash];
    [result setObject:[[NSMutableSet alloc] init] forKey:key];
  }
  
  for (id<GCluster> child in childClusters) {
    [[result objectForKey:[NSString stringWithFormat:@"%lu", (unsigned long)[self findClosestParent:child in:parents]]]addObject:child];
  }
  
  return result;
}

/**
 * Search the closest cluster in a set compared to one cluster.
 * @param id<GCluster> child: The cluster with the position from where the 
 *                            parents should be searched.
 * @param NSSet parents: A Set of clusters where the closest chluster should be 
 *                       searched compared to the child.
 * @return: The hash of the closest parent compared to the childcluster. 
 *          If there are no parents 0 will be returnd.
 */
-(unsigned long)findClosestParent:(id<GCluster>)child in:(NSSet *)parents {
  
  CLLocationDistance closest = DBL_MAX;
  unsigned long result = 0;
  
  for (id<GCluster> p in parents) {
    
    CLLocationDistance distance = GMSGeometryDistance(p.position, child.position);
    
    if (distance < closest) {
      closest = distance;
      result = p.marker.hash;
    }
  }
  return result;
}

/**
 * Creates the the markers that represents the markerdate for a set of markerdatas.
 * Also see makeMarkerFrom:cluster.
 * @param NSSet markerData: A set of data for the markers that should be created.
 * @return: A set of markers that are chreated from the given data set.
 */
-(NSMutableSet *)makeMarkersFrom:(NSSet *)markerData {
  if (markerData.count == 0)
    return nil;
  
  NSMutableSet *result = [[NSMutableSet alloc]init];
  
  for (id<GCluster> cluster in markerData) {
    [result addObject:[self makeMarkerFrom:cluster]];
  }
  
  return result;
}

/**
 * Creates a marker that represents the given cluster. Also decides if the 
 * markericon will be a normal marker- of a clustericon.
 * @param id<GCluster> cluster: includes the data from which the marker will be created.
 * @return: A new marker including the given data.
 */
-(GMSMarker *)makeMarkerFrom:(id <GCluster>)cluster {
  
  GMSMarker * marker = nil;
  
  if (cluster.items.count >= minimumClusterCount) {
    marker = [self makeClusterMarker:cluster withCount:cluster.items.count];
  }
  else {
    marker = [[GMSMarker alloc] init];
    marker = cluster.marker;
  }
  return marker;
}

/**
 * Explodes a cluster.
 * Animates the movement of a set of markers away from a given markerposition to
 * there origin position.
 * @param NSArray children: An array that includes the data of all markers that 
 *                          should be moved to there origin position.
 * @param CLLocationCoordinate2D parentPosition: The position from where all 
 *        children start there animation.
 */
-(NSMutableSet *)moveMarkers:(NSArray *)children from:(CLLocationCoordinate2D)parentPosition {
  
  NSMutableSet * result = [[NSMutableSet alloc]init];
  
  for (id <GCluster> child in children) {
    
    GMSMarker * marker = [self makeMarkerFrom:child];
    
    marker.position = parentPosition;
    marker.map = self.map;
    
    [CATransaction begin]; {
      
      [CATransaction setAnimationDuration:animationTime];
      
      marker.position = child.position;
      
    } [CATransaction commit];
    
    [result addObject:marker];
  }
  return result;
}

/**
 * Collaps a cluster.
 * Animates the movement of a set of markers from there origin position to a given position.
 * @param NSArray children: An array that includes the data of all markers that
 *                          should be moved to a given position.
 * @param CLLocationCoordinate2D parentPosition: The destinationposition of all
 *        childrenmarkers.
 */
-(void)moveMarkers:(NSArray *)children to:(CLLocationCoordinate2D)parentPosition {
  
  if (children.count < minimumClusterCount) {
//    [self updateViewPortInRegion:viewPort];
    return;
  }
  
  [CATransaction begin]; {
    
    [CATransaction setCompletionBlock:^(void) {
    
      [self updateViewPortInRegion:viewPort];
    }];

    for (id<GCluster> child in children) {
    
      GMSMarker * marker = [self makeMarkerFrom:child];
    
      marker.map = self.map;
    
      [CATransaction setAnimationDuration:animationTime];
      
      [CATransaction setCompletionBlock:^(void) {
        
        marker.map = nil;
      }];
      
      marker.position = parentPosition;
    }
  } [CATransaction commit];
}


-(void) clearMarkerCache {
  
  [self clearCache:markers];
}


-(void) clearCache:(NSMutableSet *)cache {

  if (cache.count == 0)
    return;
  
//  NSLog(@"clear Cache");
  
  NSArray *cacheArray = cache.allObjects;
    
  for (unsigned int i = 0; i < cacheArray.count; i++) {
      
    if ([[cacheArray objectAtIndex:i] isKindOfClass:[GMSMarker class]]) {
        
      ((GMSMarker*)[cacheArray objectAtIndex:i]).map = nil;
    }
  }
    
  [cache removeAllObjects];
}

/**
 * Renders the currently saved markerdata on the map
 *
 */
-(void)updateViewPortInRegion:(GMSVisibleRegion)region {
  
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:region];
  
  for (GMSMarker *marker in markers) {
    
    if ([bounds containsCoordinate: marker.position]) {
    
      if (marker.map != self.map) {
        
        marker.map = self.map;
      }
    }
    else {
      
      if (marker.map != nil) {
    
        marker.map = nil;
      }
    }
  }
}

/**
 * Renders a given set of clusterdata inside of a region on the map.
 * @param NSSet clusters: a set of clusterdata that should be renderd on the map.
 *                        All clusterdatas must implement GCluster.
 * @param GMSVisibleRegion region: only clusters / markers whithin this region will be renderd.
 * @return All markers that are generated from the markerdata.
 */
-(void) updateCluster:(NSSet *)clusters inRegion:(GMSVisibleRegion)region {
//  NSLog(@"update Cluster - %lu", clusters.count);
  [self clearMarkerCache];
  
  markers = [self makeMarkersFrom:clusters];                                    // create markers from incomming markerdata.
  [self updateViewPortInRegion:region];                                         // render markers on the map.
  
  oldMarkers = [[NSMutableSet alloc ] initWithSet: clusters];                   // set oldMarkers.
}

-(void)renderClusterMarkers:(NSSet *)clusterMarkers inRegion:(GMSVisibleRegion)region {
  
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:region];
  
  for (GMSMarker *marker in clusterMarkers) {
    
    if ([bounds containsCoordinate:marker.position]) {
      
      marker.map = self.map;
    }
  }
}

-(GMSMarker *) makeClusterMarker:(id<GCluster>)cluster withCount:(NSUInteger)count {
  
  GMSMarker *marker = [[GMSMarker alloc]init];
  NSString *iconKey = [NSString stringWithFormat:@"%li", (unsigned long)count];
  UIImage *clusterIcon = [clusterIcons objectForKey:iconKey];
  
  if (clusterIcon == nil) {
    clusterIcon = [self generateClusterIconWithCount:count];
    [clusterIcons setObject:clusterIcon forKey:iconKey];
  }
  else {
    clusterIcon = [clusterIcons objectForKey:iconKey];
  }
  
  marker.icon = clusterIcon;
  
  marker.userData = [NSNumber numberWithBool:YES];
  
  marker.groundAnchor = CGPointMake(cluster.marker.groundAnchor.x, 0.5);
  
  marker.position = cluster.position;
  
  return marker;
}

-(CLLocationCoordinate2D) getPostitionOfMarkerWithHash:(NSString *)markerHash {
  
  return [self getPostitionOfMarkerWithHash:markerHash in:oldMarkers ];
}

-(CLLocationCoordinate2D) getPostitionOfMarkerWithHash:(NSString *)markerHash in:(NSSet *)container {
  
  double hash = [markerHash doubleValue];
  
  NSSet *m = [container objectsPassingTest:^(id<GCluster> obj,BOOL *stop){
    
    GMSMarker *marker = obj.marker;
    
    BOOL result = (marker.hash == hash);
    
    return result;
  }];
  
  GMSMarker *marker = nil;
  
  if (m.count == 1) {
    
    marker = [m allObjects][0];
  }
  else {
    
    NSLog(@"Can not find a marker with the hash: %f ", hash);
  }
  
  return marker.position;
}


- (UIImage*)generateClusterIconWithCount:(NSUInteger)count {
    
  int diameter = 80;
    
  CGRect rect = CGRectMake(0, 0, diameter, diameter);
  
  UIGraphicsBeginImageContextWithOptions(rect.size, NO, 0);
    
  CGContextRef ctx = UIGraphicsGetCurrentContext();
  
  [[UIColor colorWithRed:33.0/255.0 green:165.0/255.0 blue:0.0 alpha:0.2]setFill];
  
  // make circle rect 5 px from border
  CGRect circleRect = CGRectMake(0, 0, diameter, diameter);
  
  CGContextFillEllipseInRect(ctx, circleRect);
    
  [[UIColor colorWithRed:33.0/255.0 green:165.0/255.0 blue:0.0 alpha:1.0] setFill];
    
  CGRect circleRectInnerCircle = CGRectMake(diameter/4, diameter/4, diameter/2, diameter/2);
  
  CGContextFillEllipseInRect(ctx, circleRectInnerCircle);
    
  CTFontRef myFont = CTFontCreateWithName( (CFStringRef)@"Helvetica-Bold", 12.0f, NULL);
    
    
  NSDictionary *attributesDict = [NSDictionary dictionaryWithObjectsAndKeys:
                                    (__bridge id)myFont, (id)kCTFontAttributeName,
                                    [UIColor whiteColor], (id)kCTForegroundColorAttributeName, nil];
    
  CFRelease(myFont);
    
  // create a naked string
  NSString *string = [[NSString alloc] initWithFormat:@"%lu", (unsigned long)count];
    
  NSAttributedString *stringToDraw = [[NSAttributedString alloc] initWithString:string
                                                                       attributes:attributesDict];
    
  // flip the coordinate system
  CGContextSetTextMatrix(ctx, CGAffineTransformIdentity);
  
  CGContextTranslateCTM(ctx, 0, diameter);
  
  CGContextScaleCTM(ctx, 1.0, -1.0);
    
  
  CTFramesetterRef frameSetter = CTFramesetterCreateWithAttributedString((__bridge CFAttributedStringRef)(stringToDraw));
  
  CGSize suggestedSize = CTFramesetterSuggestFrameSizeWithConstraints(frameSetter, /* Framesetter */
                                                                      CFRangeMake(0, stringToDraw.length), /* String range (entire string) */
                                                                      NULL, /* Frame attributes */
                                                                      CGSizeMake(diameter, diameter), /* Constraints (CGFLOAT_MAX indicates unconstrained) */
                                                                      NULL /* Gives the range of string that fits into the constraints, doesn't matter in your situation */
                                                                      );
    
  
  CFRelease(frameSetter);
    
  //Get the position on the y axis
  float midHeight = diameter;
  
  midHeight -= suggestedSize.height;
    
  
  float midWidth = diameter / 2;
  
  midWidth -= suggestedSize.width / 2;
    
  
  CTLineRef line = CTLineCreateWithAttributedString( (__bridge CFAttributedStringRef)stringToDraw );
    
  CGContextSetTextPosition(ctx, midWidth, diameter/2 - 4);
    
  CTLineDraw(line, ctx);
  
  CFRelease(line);
  
  UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
  
  UIGraphicsEndImageContext();
    
  return image;
}

@end
