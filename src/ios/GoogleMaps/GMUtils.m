//
//  Utils.m
//  HelloWorld
//
//  Created by Nicolas Torres on 12/30/16.
//
//
#import "GMUtils.h"

#define ToRadian(x) ((x) * M_PI/180)
#define ToDegrees(x) ((x) * 180/M_PI)

@interface GMUtils()

@property(nonatomic, assign) int maxZIndex;

@end

@implementation GMUtils

+ (instancetype)sharedInstance
{
    static GMUtils *_sharedInstance = nil;
    static dispatch_once_t onceToken;
    
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[GMUtils alloc] init];
    });
    
    return _sharedInstance;
}

- (instancetype)init
{
    self = [super init];
    if (self)
    {
        self.maxZIndex = 0;
    }
    return self;
}

+ (NSString *)getMethodNameStringFromSelector:(SEL)selector{
    
    // We need to remove ":" string
    return [NSStringFromSelector(selector) stringByReplacingOccurrencesOfString:@":" withString:@""];
}

- (int)getMaxZIndexValue{
    return self.maxZIndex;
};

- (void)updateMaxZIndexValue:(int)value{

    self.maxZIndex = value;
};

+ (CLLocationCoordinate2D)getLocationFromDictionary:(NSDictionary *)locationDictionary{
    
    CLLocationDegrees latitude = ((NSNumber *)locationDictionary[@"lat"]).doubleValue;
    CLLocationDegrees longitude = ((NSNumber *)locationDictionary[@"lng"]).doubleValue;
    
    return CLLocationCoordinate2DMake(latitude, longitude);
}

+ (CLLocationCoordinate2D)getMidPointBetweenCoordinate:(CLLocationCoordinate2D)cordA andCoordinate:(CLLocationCoordinate2D)cordB{
    
    cordA.latitude = ToRadian(cordA.latitude);
    cordB.latitude = ToRadian(cordB.latitude);
    CLLocationDegrees dLon = ToRadian(cordB.longitude - cordA.longitude);
    CLLocationDegrees bx = cos(cordB.latitude) * cos(dLon);
    CLLocationDegrees by = cos(cordB.latitude) * sin(dLon);
    CLLocationDegrees latitude = atan2(sin(cordA.latitude) + sin(cordB.latitude), sqrt((cos(cordA.latitude) + bx) * (cos(cordA.latitude) + bx) + by*by));
    CLLocationDegrees longitude = ToRadian(cordA.longitude) + atan2(by, cos(cordA.latitude) + bx);
    
    CLLocationCoordinate2D midpointCoordinate;
    midpointCoordinate.longitude = ToDegrees(longitude);
    midpointCoordinate.latitude = ToDegrees(latitude);
    
    return midpointCoordinate;
    
}

+ (CLLocationCoordinate2D)calculateCenterCoordinate:(NSMutableArray *)arrayOfMarkers{

    float maxLat = -200;
    float maxLong = -200;
    float minLat = MAXFLOAT;
    float minLong = MAXFLOAT;
    
    for (int i = 0; i < [arrayOfMarkers count]; i++)
    {
        CLLocationCoordinate2D location = ((GMSMarker *)[arrayOfMarkers objectAtIndex:i]).position;
        
        if (location.latitude < minLat)
        {
            minLat = location.latitude;
        }
        
        if (location.longitude < minLong)
        {
            minLong = location.longitude;
        }
        
        if (location.latitude > maxLat)
        {
            maxLat = location.latitude;
        }
        
        if (location.longitude > maxLong)
        {
            maxLong = location.longitude;
        }
    }
    
    //Center point
    
    return CLLocationCoordinate2DMake((maxLat + minLat) * 0.5, (maxLong + minLong) * 0.5);
}

+ (BOOL)checkNonConvexHullPath:(GMSPath *)path
{
    if (path.count < 3) {
        return NO;
    }
    
    GMSMutablePath *currentPath = [[GMSMutablePath alloc] initWithPath:path];
    CLLocationCoordinate2D coordinate = [path coordinateAtIndex:0];
    for (NSInteger i = path.count - 1; i >= 1; i--) {

        BOOL intersects = [self segmentToPoint:coordinate intersectsWithPath:currentPath];
        
        if (intersects) {
            return YES;
        }
        
        coordinate = [currentPath coordinateAtIndex:i];
        [currentPath removeLastCoordinate];
        
    }
    
    return NO;
}

+ (BOOL)segmentToPoint:(CLLocationCoordinate2D)point intersectsWithPath:(GMSPath *)path
{
    if (path.count < 3) {
        return NO;
    }
    
    CLLocationCoordinate2D lastPoint = [path coordinateAtIndex:path.count - 1];
    
    return [GMUtils lineSegmentFrom:lastPoint toPoint:point intersectsToPath:path inRange:NSMakeRange(0, path.count - 2)];
}

+ (BOOL)lineSegmentFrom:(CLLocationCoordinate2D)pointA toPoint:(CLLocationCoordinate2D)pointB intersectsToPath:(GMSPath *)path inRange:(NSRange)range
{
    for (NSUInteger i = range.location; i < range.location + range.length && i < (path.count - 1); i++) {
        CLLocationCoordinate2D pointC = [path coordinateAtIndex:i];
        CLLocationCoordinate2D pointD = [path coordinateAtIndex:i + 1];
        
        BOOL intersects = [GMUtils segmentFromPoint:pointA to:pointB intersectsWithsegmentFromPoint:pointC to:pointD];
        
        if (intersects) {
            return YES;
        }
    }
    
    return NO;
}

+ (BOOL)segmentFromPoint:(CLLocationCoordinate2D)pointA to:(CLLocationCoordinate2D)pointB intersectsWithsegmentFromPoint:(CLLocationCoordinate2D)pointC to:(CLLocationCoordinate2D)pointD
{
    
    if ([GMUtils isCoordinate:pointB equalTo:pointC]) {
        return NO;
    }
    
    return ([GMUtils checkCounterClockWiseFrom:pointA thru:pointC to:pointD] != [GMUtils checkCounterClockWiseFrom:pointB thru:pointC to:pointD]) &&
    ([GMUtils checkCounterClockWiseFrom:pointA thru:pointB to:pointC] != [GMUtils checkCounterClockWiseFrom:pointA thru:pointB to:pointD]);
}

+ (BOOL)checkCounterClockWiseFrom:(CLLocationCoordinate2D)pointA thru:(CLLocationCoordinate2D)pointB to:(CLLocationCoordinate2D)pointC
{
    
    
    return (pointC.latitude - pointA.latitude) * (pointB.longitude - pointA.longitude) > (pointB.latitude - pointA.latitude) * (pointC.longitude - pointA.longitude);
}

+ (BOOL)isCoordinate:(CLLocationCoordinate2D)pointA equalTo:(CLLocationCoordinate2D)pointB
{
    return pointA.latitude == pointB.latitude && pointA.longitude == pointB.longitude;
}

@end
