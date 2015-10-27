#import <CoreText/CoreText.h>
#import "GDefaultClusterRenderer.h"
#import "GQuadItem.h"
#import "GCluster.h"

@implementation GDefaultClusterRenderer {
//    GMSMapView *_map;
    NSMutableArray *_markerCache;
    UIImage * clusterIcon;
}

- (id)initWithMapView:(GMSMapView*)googleMap {
    if (self = [super init]) {
        self.map = googleMap;
        _markerCache = [[NSMutableArray alloc] init];
        clusterIcon = [self generateClusterIconWithCount:5];
    }
    return self;
}

- (void)clustersChanged:(NSSet*)clusters {

    for (GMSMarker *marker in _markerCache) {

        marker.map = nil;
    }

    [_markerCache removeAllObjects];
    
    for (id <GCluster> cluster in clusters) {
      GMSMarker *marker;
      marker = [[GMSMarker alloc] init];
      [_markerCache addObject:marker];
        
      NSUInteger count = cluster.items.count;
      if (count > 1) {
        marker.icon = [self generateClusterIconWithCount:count];
        marker.position = cluster.position;
      }
      else {
          marker = cluster.marker;
      }

      marker.map = self.map;
    }
}

- (void)clustersChanged:(NSSet *)clusters inRegion:(GMSVisibleRegion)region {
    
  for (GMSMarker *marker in _markerCache) {
        
      marker.map = nil;
  }
    
  [_markerCache removeAllObjects];
    
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:region];

  for (id <GCluster> cluster in clusters) {
    if ([bounds containsCoordinate:cluster.marker.position]) {
      GMSMarker *marker;
      marker = [[GMSMarker alloc] init];
      [_markerCache addObject:marker];
      
      NSUInteger count = cluster.items.count;
      if (count > 1) {
        marker.icon = [self generateClusterIconWithCount:count];
        marker.position = cluster.position;
      }
      else {
        marker = cluster.marker;
      }
      
      marker.map = self.map;
    }
  }
}

- (void)updateViewPortInRegion:(GMSVisibleRegion)region {
  
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:region];
  
  for (GMSMarker *marker in _markerCache) {
    if ([bounds containsCoordinate:marker.position]) {
      marker.map = self.map;
    }
    else {
      marker.map = nil;
    }
  }
}

- (void)clustersChanged:(NSSet *)clusters inRegion:(GMSVisibleRegion)region withZoom:(int)currentDiscreteZoom {
  
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
    CGSize suggestedSize = CTFramesetterSuggestFrameSizeWithConstraints(
                                                                        frameSetter, /* Framesetter */
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

    CTLineRef line = CTLineCreateWithAttributedString(
            (__bridge CFAttributedStringRef)stringToDraw);
    CGContextSetTextPosition(ctx, midWidth, diameter/2 - 4);
    CTLineDraw(line, ctx);

    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return image;
}
@end
