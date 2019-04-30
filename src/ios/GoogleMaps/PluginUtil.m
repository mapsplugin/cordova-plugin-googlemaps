//
//  PluginUtil.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginUtil.h"


@implementation CDVCommandDelegateImpl (GoogleMapsPlugin)

WKWebView* _hook_webView;
CDVPluginManager* _hook_manager;
NSRegularExpression* _hook_callbackIdPattern;

- (id)initWithWebView:(WKWebView*)webView pluginManager:(CDVPluginManager *)manager {
  self = [super init];
  _hook_manager = manager;
  _hook_webView = webView;
  
  NSError* err = nil;
  _hook_callbackIdPattern = [NSRegularExpression regularExpressionWithPattern:@"[^A-Za-z0-9._-]" options:0 error:&err];
  if (err != nil) {
      // Couldn't initialize Regex
      NSLog(@"Error: Couldn't initialize regex");
      _hook_callbackIdPattern = nil;
  }
  return self;
}

// Override callbackIdPattern property
// http://ddeville.me/2011/03/add-variables-to-an-existing-class-in-objective-c
- (void)webView:(WKWebView *)webView {
  _hook_webView = webView;
}
- (WKWebView *)webView {
  return _hook_webView;
}

// Override callbackIdPattern property
// http://ddeville.me/2011/03/add-variables-to-an-existing-class-in-objective-c
- (NSRegularExpression *)callbackIdPattern {
  return _hook_callbackIdPattern;
}


// Override manager property
// http://ddeville.me/2011/03/add-variables-to-an-existing-class-in-objective-c
- (CDVPluginManager *)manager {
  NSLog(@"--->manager");
  return _hook_manager;
}

- (NSString*)pathForResource:(NSString*)resourcepath
{
    NSBundle* mainBundle = [NSBundle mainBundle];
    NSMutableArray* directoryParts = [NSMutableArray arrayWithArray:[resourcepath componentsSeparatedByString:@"/"]];
    NSString* filename = [directoryParts lastObject];

    [directoryParts removeLastObject];

    NSString* directoryPartsJoined = [directoryParts componentsJoinedByString:@"/"];


    return [mainBundle pathForResource:filename ofType:@"" inDirectory:@"www"];
}

- (void)flushCommandQueueWithDelayedJs
{
    _delayResponses = YES;
    _delayResponses = NO;
}

- (void)evalJsHelper2:(NSString*)js
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [_hook_webView evaluateJavaScript:js completionHandler:^(id obj, NSError* error) {
        // TODO: obj can be something other than string
        if ([obj isKindOfClass:[NSString class]]) {
            NSString* commandsJSON = (NSString*)obj;
            if ([commandsJSON length] > 0) {
                NSLog(@"Exec: Retrieved new exec messages by chaining.");
            }
        }
        }];
    });
}

- (BOOL)isValidCallbackId:(NSString*)callbackId
{
    if ((callbackId == nil) || (_hook_callbackIdPattern == nil)) {
        return NO;
    }

    // Disallow if too long or if any invalid characters were found.
    if (([callbackId length] > 100) || [_hook_callbackIdPattern firstMatchInString:callbackId options:0 range:NSMakeRange(0, [callbackId length])]) {
        return NO;
    }
    return YES;
}

- (void)sendPluginResult:(CDVPluginResult*)result callbackId:(NSString*)callbackId
{

    // This occurs when there is are no win/fail callbacks for the call.
    if ([@"INVALID" isEqualToString:callbackId]) {
        return;
    }
    // This occurs when the callback id is malformed.
    if (![self isValidCallbackId:callbackId]) {
        NSLog(@"Invalid callback id received by sendPluginResult");
        return;
    }
    int status = [result.status intValue];
    BOOL keepCallback = [result.keepCallback boolValue];
    NSString* argumentsAsJSON = [result argumentsAsJSON];
    BOOL debug = NO;
  
#if DEBUG
    debug = YES;
#endif

    NSString* js = [NSString stringWithFormat:@"cordova.require('cordova/exec').nativeCallback('%@',%d,%@,%d, %d)", callbackId, status, argumentsAsJSON, keepCallback, debug];

    [self evalJsHelper2:js];
}

- (void)evalJs:(NSString*)js
{
    [self evalJs:js scheduledOnRunLoop:YES];
}

- (void)evalJs:(NSString*)js scheduledOnRunLoop:(BOOL)scheduledOnRunLoop
{
    js = [NSString stringWithFormat:@"try{cordova.require('cordova/exec').nativeEvalAndFetch(function(){%@})}catch(e){console.log('exception nativeEvalAndFetch : '+e);};", js];
     [self evalJsHelper2:js];
}

- (id)getCommandInstance:(NSString*)pluginName
{
    return [_hook_manager getCommandInstance:pluginName];
}

- (void)runInBackground:(void (^)())block
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), block);
}

- (NSString*)userAgent
{
    return nil;
}

- (NSDictionary*)settings
{
    return _hook_manager.settings;
}

@end





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
    CDVPlugin<IPluginProtocol> *plugin = [self getCommandInstance:pluginName];
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

+ (CLLocationCoordinate2D)isPointOnTheLine:(GMSPath *)path coordinate:(CLLocationCoordinate2D)coordinate projection:(GMSProjection *)projection {
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
      return [path coordinateAtIndex:i];
    }
  }
  return kCLLocationCoordinate2DInvalid;
}

+ (CLLocationCoordinate2D)isPointOnTheGeodesicLine:(GMSPath *)path coordinate:(CLLocationCoordinate2D)point threshold:(double)threshold projection:(GMSProjection *)projection {
  
  int fingerSize = 40;  // assume finger size is 20px
  CGPoint touchPoint = [projection pointForCoordinate:CLLocationCoordinate2DMake(point.latitude, point.longitude)];
  GMSCoordinateBounds *possibleBounds = [[GMSCoordinateBounds alloc] init];
  possibleBounds = [possibleBounds includingCoordinate:[projection coordinateForPoint:CGPointMake(touchPoint.x - fingerSize, touchPoint.y - fingerSize)]];
  possibleBounds = [possibleBounds includingCoordinate:[projection coordinateForPoint:CGPointMake(touchPoint.x + fingerSize, touchPoint.y + fingerSize)]];


  //-------------------------------------------------------------------
  // Intersection for geodesic line
  // http://my-clip-devdiary.blogspot.com/2014/01/html5canvas.html
  //-------------------------------------------------------------------
  double trueDistance, testDistance1, testDistance2;
  CGPoint p0, p1;
  NSValue *value;
  //CGPoint touchPoint = CGPointMake(point.latitude * 100000, point.longitude * 100000);
  CLLocationCoordinate2D position1, position2;
  CLLocationCoordinate2D start, finish;
  NSMutableArray *points = [[NSMutableArray alloc] init];
  BOOL firstTest = NO;

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
      start = position1;
      finish = position2;
      firstTest = YES;
      break;
    }
  }
  
  if (firstTest == NO) {
    return kCLLocationCoordinate2DInvalid;
  }
  
  //----------------------------------------------------------------
  // Calculate waypoints from start to finish on geodesic line
  // @ref http://jamesmccaffrey.wordpress.com/2011/04/17/drawing-a-geodesic-line-for-bing-maps-ajax/
  //----------------------------------------------------------------
  
  // convert to radians
  double lat1 = start.latitude * (M_PI / 180.0);
  double lng1 = start.longitude * (M_PI / 180.0);
  double lat2 = finish.latitude * (M_PI / 180.0);
  double lng2 = finish.longitude * (M_PI / 180.0);
  
  double d = 2 * asin(sqrt(pow((sin((lat1 - lat2) / 2)), 2) +
      cos(lat1) * cos(lat2) * pow((sin((lng1 - lng2) / 2)), 2)));
  GMSMutablePath *wayPoints = [GMSMutablePath path];
  double f = 0.00000000f; // fraction of the curve
  double finc = 0.01000000f; // fraction increment

  while (f <= 1.00000000f) {
    double A = sin((1.0 - f) * d) / sin(d);
    double B = sin(f * d) / sin(d);

    double x = A * cos(lat1) * cos(lng1) + B * cos(lat2) * cos(lng2);
    double y = A * cos(lat1) * sin(lng1) + B * cos(lat2) * sin(lng2);
    double z = A * sin(lat1) + B * sin(lat2);
    double lat = atan2(z, sqrt((x*x) + (y*y)));
    double lng = atan2(y, x);

    CLLocationCoordinate2D wp = CLLocationCoordinate2DMake(lat / (M_PI / 180.0), lng / ( M_PI / 180.0));
    if ([possibleBounds containsCoordinate:wp]) {
      [wayPoints addCoordinate:wp];
    }

    f += finc;
  } // while
  
  // break into waypoints with negative longitudes and those with positive longitudes
  GMSMutablePath *negLons = [GMSMutablePath path]; // lat-lons where the lon part is negative
  GMSMutablePath *posLons = [GMSMutablePath path];
  GMSMutablePath *connect = [GMSMutablePath path];

  for (int i = 0; i < [wayPoints count]; ++i) {
    if ([wayPoints coordinateAtIndex:i].longitude <= 0.0f)
      [negLons addCoordinate:[wayPoints coordinateAtIndex:i]];
    else
      [posLons addCoordinate:[wayPoints coordinateAtIndex:i]];
  }

  // we may have to connect over 0.0 longitude
  for (int i = 0; i < [wayPoints count] - 1; ++i) {
    if (([wayPoints coordinateAtIndex:i].longitude <= 0.0f && [wayPoints coordinateAtIndex:(i + 1)].longitude >= 0.0f) ||
        ([wayPoints coordinateAtIndex:i].longitude >= 0.0f && [wayPoints coordinateAtIndex:(i + 1)].longitude <= 0.0f)) {
      if ((fabs([wayPoints coordinateAtIndex:i].longitude) + fabs([wayPoints coordinateAtIndex:(i + 1)].longitude)) < 100.0f) {
        [connect addCoordinate:[wayPoints coordinateAtIndex:i]];
        [connect addCoordinate:[wayPoints coordinateAtIndex:(i + 1)]];
      }
    }
  }
  
  GMSMutablePath *inspectPoints = [GMSMutablePath path];
  if ([negLons count] > 2) {
    for (int i = 0; i < [negLons count]; i++) {
      [inspectPoints addCoordinate:[negLons coordinateAtIndex:i]];
    }
  }
  if ([posLons count] > 2) {
    for (int i = 0; i < [posLons count]; i++) {
      [inspectPoints addCoordinate:[posLons coordinateAtIndex:i]];
    }
  }
  if ([connect count] > 2) {
    for (int i = 0; i < [connect count]; i++) {
      [inspectPoints addCoordinate:[connect coordinateAtIndex:i]];
    }
  }
  
  double minDistance = 999999999;
  double distance;
  CLLocationCoordinate2D mostClosePoint;

  for (int i = 0; i < [inspectPoints count]; i++) {
    distance = GMSGeometryDistance([inspectPoints coordinateAtIndex:i], point);
    if (distance < minDistance) {
      minDistance = distance;
      mostClosePoint = [inspectPoints coordinateAtIndex:i];
    }
  }
  return mostClosePoint;
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

+ (NSString *)PGM_LOCALIZATION:(NSString *)key {
  NSFileManager *fileManager = [NSFileManager defaultManager];
  NSArray<NSString *> *preferredLanguages = [NSLocale preferredLanguages];
  NSString *localeCode, *languageCode, *path, *filename, *fileContents, *foundFilePath = nil;
  NSBundle *mainBundle = [NSBundle mainBundle];

  for (int i = 0; i < [preferredLanguages count]; i++) {
    localeCode = [preferredLanguages objectAtIndex:i];

    // Find json file for pgm_Localizable_(localeCode).json  // pgm_Localizable_en-US.json
    filename = [NSString stringWithFormat:@"pgm_Localizable_%@", localeCode];
    path = [mainBundle pathForResource:filename ofType:@"json"];
    if ([fileManager fileExistsAtPath:path]) {
      foundFilePath = path;
      break;
    }

    languageCode = [[localeCode componentsSeparatedByString:@"-"] firstObject];
    // Find json file for pgm_Localizable_(languageCode).json  // pgm_Localizable_en.json
    filename = [NSString stringWithFormat:@"pgm_Localizable_%@", languageCode];
    path = [mainBundle pathForResource:filename ofType:@"json"];
    if ([fileManager fileExistsAtPath:path]) {
      foundFilePath = path;
      break;
    }
  }

  if (!foundFilePath) {
    foundFilePath  = [mainBundle pathForResource:@"pgm_Localizable_en" ofType:@"json"];
  }

  fileContents = [NSString stringWithContentsOfFile:foundFilePath encoding:NSUTF8StringEncoding error:nil];
  NSData *data = [fileContents dataUsingEncoding:NSUTF8StringEncoding];
  NSDictionary *json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
  NSString *result = [json objectForKey:key];
  if (!result) {
    result = key;
  }
  return result;
}

@end
