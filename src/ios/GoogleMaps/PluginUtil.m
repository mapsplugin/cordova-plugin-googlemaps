//
//  PluginUtil.m
//  cordova-googlemaps-plugin v2
//
//  Created by masashi.
//
//

#import "PluginUtil.h"
@implementation UIView (GoogleMapsPlugin)
- (void)setFrameWithDictionary:(NSDictionary *)params
{
  float left = [[params objectForKey:@"left"] floatValue];
  float top = [[params objectForKey:@"top"] floatValue];
  float width = [[params objectForKey:@"width"] floatValue];
  float height = [[params objectForKey:@"height"] floatValue];

  CGRect r = [self frame];
  if (r.origin.x == left &&
      r.origin.y == top &&
      r.size.width == width &&
      r.size.height == height) {
    return;
  }
  r.origin.x = left;
  r.origin.y = top;
  r.size.width = width;
  r.size.height = height;
  self.frame = r;
}


- (void)setFrameWithInt:(int)left top:(int)top width:(int)width height:(int)height
{
  CGRect r = [self frame];
  if (r.origin.x == left &&
      r.origin.y == top &&
      r.size.width == width &&
      r.size.height == height) {
    return;
  }
  r.origin.x = left;
  r.origin.y = top;
  r.size.width = width;
  r.size.height = height;
  self.frame = r;
}

@end

@implementation NSArray (GoogleMapsPlugin)
- (UIColor*)parsePluginColor
{
  return [UIColor colorWithRed:[[self objectAtIndex:0] floatValue]/255.0
                              green:[[self objectAtIndex:1] floatValue]/255.0
                              blue:[[self objectAtIndex:2] floatValue]/255.0
                              alpha:[[self objectAtIndex:3] floatValue]/255.0];

}
@end

@implementation NSString (GoogleMapsPlugin)
- (NSString*)regReplace:(NSString*)pattern replaceTxt:(NSString*)replaceTxt options:(NSRegularExpressionOptions)options
{
  NSError *error = nil;
  NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:pattern options:options error:&error];
  return [regex stringByReplacingMatchesInString:self options:0 range:NSMakeRange(0, [self length]) withTemplate:replaceTxt];
}
@end

// Generate a new image with the specified alpha from a uiImage
// http://stackoverflow.com/questions/5084845/how-to-set-the-opacity-alpha-of-a-uiimage#10819117
@implementation UIImage (GoogleMapsPlugin)
- (UIImage *)imageByApplyingAlpha:(CGFloat) alpha {
    UIGraphicsBeginImageContextWithOptions(self.size, NO, 0.0f);

    CGContextRef ctx = UIGraphicsGetCurrentContext();
    CGRect area = CGRectMake(0, 0, self.size.width, self.size.height);

    CGContextScaleCTM(ctx, 1, -1);
    CGContextTranslateCTM(ctx, 0, -area.size.height);

    CGContextSetBlendMode(ctx, kCGBlendModeMultiply);

    CGContextSetAlpha(ctx, alpha);

    CGContextDrawImage(ctx, area, self.CGImage);

    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();

    UIGraphicsEndImageContext();

    return newImage;
}

-(UIImage *)resize:(CGFloat)width height:(CGFloat)height {
  if (width > 0 && height > 0) {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 0.0);
    [self drawInRect:CGRectMake(0, 0, width, height)];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
  }
  return self;
}


@end


/*
@implementation CDVCommandDelegateImpl (GoogleMapsPlugin)

- (void)hookSendPluginResult:(CDVPluginResult*)result callbackId:(NSString*)callbackId {

  NSRange pos = [callbackId rangeOfString:@"://"];
  if (pos.location == NSNotFound) {
    [self sendPluginResult:result callbackId:callbackId];
  } else {
    NSArray *tmp = [callbackId componentsSeparatedByString:@"://"];
    NSString *pluginName = [tmp objectAtIndex:0];
    CDVPlugin<MyPlgunProtocol> *plugin = [self getCommandInstance:pluginName];
    [plugin onHookedPluginResult:result callbackId:callbackId];
  }

}
@end
*/

static char CAAnimationGroupBlockKey;
@implementation CAAnimationGroup (Blocks)

- (void)setCompletionBlock:(TIFAnimationGroupCompletionBlock)handler {
    objc_setAssociatedObject(self, &CAAnimationGroupBlockKey, handler, OBJC_ASSOCIATION_COPY_NONATOMIC);

    self.delegate = self;
}

- (void)animationDidStop:(CAAnimation *)animation finished:(BOOL)finished
{
    if (finished)
    {
        TIFAnimationGroupCompletionBlock handler = (TIFAnimationGroupCompletionBlock)objc_getAssociatedObject(self, &CAAnimationGroupBlockKey);
        if (handler) {
            handler();
        }
    }
}

@end


@implementation MainViewController (CDVViewController)
#if CORDOVA_VERSION_MIN_REQUIRED < __CORDOVA_4_0_0
- (void)webViewDidFinishLoad:(UIWebView*)theWebView
{
  theWebView.backgroundColor = [UIColor clearColor];
  theWebView.opaque = NO;
  return [super webViewDidFinishLoad:theWebView];
}
#endif
@end
@implementation PluginUtil

+ (BOOL)isPolygonContains:(GMSPath *)path coordinate:(CLLocationCoordinate2D)coordinate projection:(GMSProjection *)projection {
  //-------------------------------------------------------------------
  // Intersects using the Winding Number Algorithm
  // http://www.nttpc.co.jp/company/r_and_d/technology/number_algorithm.html
  //-------------------------------------------------------------------
  int wn = 0;
  GMSVisibleRegion visibleRegion = projection.visibleRegion;
  GMSCoordinateBounds *bounds = [[GMSCoordinateBounds alloc] initWithRegion:visibleRegion];
  CGPoint sw = [projection pointForCoordinate:bounds.southWest];

  CGPoint touchPoint = [projection pointForCoordinate:coordinate];
  touchPoint.y = sw.y - touchPoint.y;
  double vt;

  for (int i = 0; i < [path count] - 1; i++) {
    CGPoint a = [projection pointForCoordinate:[path coordinateAtIndex:i]];
    a.y = sw.y - a.y;
    CGPoint b = [projection pointForCoordinate:[path coordinateAtIndex:(i + 1)]];
    b.y = sw.y - b.y;

    if ((a.y <= touchPoint.y) && (b.y > touchPoint.y)) {
      vt = (touchPoint.y - a.y) / (b.y - a.y);
      if (touchPoint.x < (a.x + (vt * (b.x - a.x)))) {
        wn++;
      }
    } else if ((a.y > touchPoint.y) && (b.y <= touchPoint.y)) {
      vt = (touchPoint.y - a.y) / (b.y - a.y);
      if (touchPoint.x < (a.x + (vt * (b.x - a.x)))) {
        wn--;
      }
    }
  }

  return (wn != 0);
}

+ (BOOL)isPointOnTheLine:(GMSPath *)path coordinate:(CLLocationCoordinate2D)coordinate projection:(GMSProjection *)projection {
  //-------------------------------------------------------------------
  // Intersection for non-geodesic line
  // http://movingahead.seesaa.net/article/299962216.html
  // http://www.softsurfer.com/Archive/algorithm_0104/algorithm_0104B.htm#Line-Plane
  //-------------------------------------------------------------------
  double Sx, Sy;
  CGPoint touchPoint = [projection pointForCoordinate:coordinate];
  CGPoint p0, p1;

  p0 = [projection pointForCoordinate:[path coordinateAtIndex:0]];
  for (int i = 1; i < [path count]; i++) {
    p1 = [projection pointForCoordinate:[path coordinateAtIndex:i]];
    Sx = (touchPoint.x - p0.x) / (p1.x - p0.x);
    Sy = (touchPoint.y - p0.y) / (p1.y - p0.y);
    if (fabs(Sx - Sy) < 0.05 && Sx < 1 && Sy > 0) {
      return YES;
    }
  }
  return NO;
}

+ (BOOL)isPointOnTheGeodesicLine:(GMSPath *)path coordinate:(CLLocationCoordinate2D)point threshold:(double)threshold {
  //-------------------------------------------------------------------
  // Intersection for geodesic line
  // http://my-clip-devdiary.blogspot.com/2014/01/html5canvas.html
  //-------------------------------------------------------------------
  double trueDistance, testDistance1, testDistance2;
  CGPoint p0, p1;
  NSValue *value;
  //CGPoint touchPoint = CGPointMake(point.latitude * 100000, point.longitude * 100000);
  CLLocationCoordinate2D position1, position2;
  NSMutableArray *points = [[NSMutableArray alloc] init];

  for (int i = 0; i < [path count]; i++) {
    position1 = [path coordinateAtIndex:i];
    p0 = CGPointMake(position1.latitude * 100000, position1.longitude * 100000);
    [points addObject:[NSValue valueWithCGPoint:p0]];
  }
  for (int i = 0; i < [path count] - 1; i++) {
    value = (NSValue *)[points objectAtIndex:i];
    p0 = [value CGPointValue];

    value = (NSValue *)[points objectAtIndex:(i+1)];
    p1 = [value CGPointValue];

    position1 = [path coordinateAtIndex:i];
    position2 = [path coordinateAtIndex:(i + 1)];

    trueDistance = GMSGeometryDistance(position1, position2);
    testDistance1 = GMSGeometryDistance(position1, point);
    testDistance2 = GMSGeometryDistance(point, position2);
    // the distance is exactly same if the point is on the straight line
    if (fabs(trueDistance - (testDistance1 + testDistance2)) < threshold) {
      return YES;
    }
  }
  return NO;
}
+ (BOOL) isInDebugMode
{
    #ifndef __OPTIMIZE__   // Debug Mode
        return YES;
    #else
        return NO;
    #endif
}

+ (BOOL)isCircleContains:(GMSCircle *)circle coordinate:(CLLocationCoordinate2D)point {
    CLLocationDistance distance = GMSGeometryDistance(circle.position, point);
    return (distance < circle.radius);
}

+ (GMSMutablePath *)getMutablePathFromCircle:(CLLocationCoordinate2D)center radius:(double)radius {

    double d2r = M_PI / 180;   // degrees to radians
    double r2d = 180 / M_PI;   // radians to degrees
    double earthsradius = 3963.189; // 3963 is the radius of the earth in miles
    radius = radius * 0.000621371192; // convert to mile

    // find the raidus in lat/lon
    double rlat = (radius / earthsradius) * r2d;
    double rlng = rlat / cos(center.latitude * d2r);

    GMSMutablePath *mutablePath = [[GMSMutablePath alloc] init];
    double ex, ey;
    for (int i = 0; i < 360; i++) {
      ey = center.longitude + (rlng * cos(i * d2r)); // center a + radius x * cos(theta)
      ex = center.latitude + (rlat * sin(i * d2r)); // center b + radius y * sin(theta)
      [mutablePath addLatitude:ex longitude:ey];
    }
    return mutablePath;
}

+ (NSString *)getAbsolutePathFromCDVFilePath:(UIView*)webView cdvFilePath:(NSString *)cdvFilePath {

  NSRange range = [cdvFilePath rangeOfString:@"cdvfile://"];
  if (range.location == NSNotFound) {
    return nil;
  }

  // Convert cdv:// path to the device real path
  // (http://docs.monaca.mobi/3.5/en/reference/phonegap_34/en/file/plugins/)
  NSString *filePath = nil;
  Class CDVFilesystemURLCls = NSClassFromString(@"CDVFilesystemURL");
  Class CDVFileCls = NSClassFromString(@"CDVFile");
  if (CDVFilesystemURLCls != nil && CDVFileCls != nil) {
    #pragma clang diagnostic push
    #pragma clang diagnostic ignored "-Warc-performSelector-leaks"

    SEL fileSystemURLWithString = NSSelectorFromString(@"fileSystemURLWithString:");
    if ([CDVFilesystemURLCls respondsToSelector:fileSystemURLWithString]) {
      id cdvFilesystemURL = [CDVFilesystemURLCls performSelector:fileSystemURLWithString withObject:cdvFilePath];
      if (cdvFilesystemURL != nil) {
#if CORDOVA_VERSION_MIN_REQUIRED >= __CORDOVA_4_0_0
		CDVPlugin *filePlugin = [(CDVPlugin *)[CDVFileCls alloc] init];
#else
        CDVPlugin *filePlugin = (CDVPlugin *)[[CDVFileCls alloc] initWithWebView:webView];
#endif
        [filePlugin pluginInitialize];

        SEL filesystemPathForURL = NSSelectorFromString(@"filesystemPathForURL:");
        filePath = [filePlugin performSelector: filesystemPathForURL withObject:cdvFilesystemURL];
      }
    }
    #pragma clang diagnostic pop
  } else {
    NSLog(@"(debug)File and FileTransfer plugins are required to convert cdvfile:// to localpath.");
  }

  return filePath;
}

@end
