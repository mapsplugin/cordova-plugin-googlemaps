//
//  PolyUtils.m
//  HelloWorld
//
//  Created by Nicolas Torres on 1/12/17.
//
//

#import "PolyUtils.h"
#import "GMUtils.h"
//#import "PolygonManager.h"
//#import "PolylineManager.h"

@implementation PolyUtils

+ (instancetype)sharedInstance
{
    static PolyUtils *_sharedInstance = nil;
    static dispatch_once_t onceToken;
    
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[PolyUtils alloc] init];
    });
    
    return _sharedInstance;
}

- (instancetype)init
{
    self = [super init];
    if (self)
    {
    }
    return self;
}

- (GMSMutablePath *) generatePathWithParameters:(NSArray *)params {
    GMSMutablePath *path = [[GMSMutablePath alloc] init];
    
    for (int i = 0; i < params.count; i++)
    {
        NSDictionary *currentDictionary = (NSDictionary *)[params objectAtIndex:i];
        
        [path addCoordinate:[GMUtils getLocationFromDictionary:currentDictionary]];
    }
    
    return path;
}

// [ {lat, lng}, {lat, lng}, ... ]
- (NSMutableArray *)getLatLngsWithPath:(GMSPath *)path{
    
    NSMutableArray *coordinatesArray = [[NSMutableArray alloc] init];
    
    for (int i = 0; i < path.count; i++) {
        
        CLLocationCoordinate2D coordinate = (CLLocationCoordinate2D)[path coordinateAtIndex:i];
        
        NSMutableDictionary *coordinatesDictionary = [NSMutableDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithDouble:coordinate.latitude], @"lat", [NSNumber numberWithDouble:coordinate.longitude], @"lng", nil];
        
        [coordinatesArray addObject:coordinatesDictionary];
    }
    
    return coordinatesArray;
    
};

// [ [lat, lng], [lat, lng], ... ]
- (NSMutableArray *)getLatLngsForGeoJSONWithPath:(GMSPath *)path{

    NSMutableArray *coordinatesArray = [[NSMutableArray alloc] init];
    
    for (int i = 0; i < path.count; i++) {
        
        CLLocationCoordinate2D coordinate = (CLLocationCoordinate2D)[path coordinateAtIndex:i];
        NSMutableArray *polygonPointCoordinateArray = [NSMutableArray arrayWithObjects:[NSNumber numberWithDouble:coordinate.latitude], [NSNumber numberWithDouble:coordinate.longitude], nil];
        
        [coordinatesArray addObject:polygonPointCoordinateArray];
        
    }
    
    return coordinatesArray;
    
};

- (GMSMutablePath *)pathByAddingCoordinates:(NSArray *)params toPath:(GMSPath *)path{
    
    // This method should add {lat, lng} to the polygon's path
    
    NSDictionary *coordinatesDictionary = (NSDictionary *)[params objectAtIndex:0];
    
    GMSMutablePath *newPath = [[GMSMutablePath alloc] initWithPath:path];
    
    [newPath addCoordinate:[GMUtils getLocationFromDictionary:coordinatesDictionary]];
    
    return newPath;
    
}

- (NSString *)getStrokeColorStringFromColor:(UIColor *)color{
    
    CGColorRef colorRef = color.CGColor;
    return [CIColor colorWithCGColor:colorRef].stringRepresentation;

}


- (GMSMarker *)createMarkerForOverlay:(GMSOverlay *)overlay withCoordinates:(CLLocationCoordinate2D)coordinates andIsEditMode:(BOOL)editMode{
    
    GMSMarker *marker = [[GMSMarker alloc] init];
    marker.position = coordinates;
    [marker setDraggable:editMode];
    marker.groundAnchor = CGPointMake(0.5, 0.5);
    marker.icon = [UIImage imageNamed:@"circle-image"];
    [marker setTappable:NO];
    marker.userData = overlay;
    
    return marker;
}

- (void)createBorderMarkerForOverlay:(GMSOverlay *)overlay withCoordinates:(CLLocationCoordinate2D)coordinates borderMarkersAray:(NSMutableArray *)borderMarkersAray andIsEditMode:(BOOL)editMode andMap:(GMSMapView *)mapView{
    
    GMSMarker *marker = [self createMarkerForOverlay:overlay withCoordinates:coordinates andIsEditMode:editMode];

    [marker setTitle:[NSString stringWithFormat:@"%lu",(unsigned long)borderMarkersAray.count]];
        
    marker.opacity = editMode;
        
    [borderMarkersAray addObject:marker];
    
    marker.map = mapView;
}


- (void)createMidMarkerForOverlay:(GMSOverlay *)overlay withCoordinates:(CLLocationCoordinate2D)coordinates midMarkersAray:(NSMutableArray *)midMarkersAray andIsEditMode:(BOOL)editMode andMap:(GMSMapView *)mapView{
    
    GMSMarker *marker = [self createMarkerForOverlay:overlay withCoordinates:coordinates andIsEditMode:editMode];
    
    // TODO: review the title
    [marker setTitle:@"MidPoint"];
    
    marker.opacity = 0.6 * editMode;
    
    [midMarkersAray addObject:marker];
    
    marker.map = mapView;
}


@end
