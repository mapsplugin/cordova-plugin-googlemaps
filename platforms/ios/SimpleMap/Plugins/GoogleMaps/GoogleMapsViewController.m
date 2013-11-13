//
//  GoogleMapsViewController.m
//  SimpleMap
//
//  Created by masashi on 11/6/13.
//
//

#import "GoogleMapsViewController.h"

@interface GoogleMapsViewController ()

@end

@implementation GoogleMapsViewController
NSDictionary *initOptions;

- (id)initWithOptions:(NSDictionary *) options {
    self = [super init];
    initOptions = options;
    return self;
}

- (void)loadView {
  [super loadView];
  
  CGRect screenSize = [[UIScreen mainScreen] bounds];
  CGRect pluginRect;
  
  int direction = self.interfaceOrientation;
  if (direction == UIInterfaceOrientationLandscapeLeft ||
      direction == UIInterfaceOrientationLandscapeRight) {
    pluginRect = CGRectMake(screenSize.size.height * 0.05, screenSize.size.width * 0.05, screenSize.size.height * 0.9, screenSize.size.width * 0.9);
      
  } else {
    pluginRect = CGRectMake(screenSize.size.width * 0.05, screenSize.size.height * 0.05, screenSize.size.width * 0.9, screenSize.size.height * 0.9);
  }
  
  
  [self.view setFrame:pluginRect];
  self.view.backgroundColor = [UIColor lightGrayColor];
  
}

- (void)viewDidLoad
{
    [super viewDidLoad];
  
    //------------
    // Initialize
    //------------
    self.markerManager = [NSMutableDictionary dictionary];
    self.circleManager = [NSMutableDictionary dictionary];
  
    //------------------
    // Create a map view
    //------------------
    NSString *APIKey = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"Google Maps API Key"];
    NSLog(@"APIKey=%@", APIKey);
    [GMSServices provideAPIKey:APIKey];
  
    //Intial camera position
    NSDictionary *cameraOpts = [initOptions objectForKey:@"camera"];
    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithFloat:0.0f] forKey:@"lng"];
    
    if (cameraOpts) {
      NSDictionary *latLngJSON = [cameraOpts objectForKey:@"latLng"];
      [latLng setObject:[NSNumber numberWithFloat:[[latLngJSON valueForKey:@"lat"] floatValue]] forKey:@"lat"];
      [latLng setObject:[NSNumber numberWithFloat:[[latLngJSON valueForKey:@"lng"] floatValue]] forKey:@"lng"];
    }
    GMSCameraPosition *camera = [GMSCameraPosition
                                  cameraWithLatitude: [[latLng valueForKey:@"lat"] floatValue]
                                  longitude: [[latLng valueForKey:@"lng"] floatValue]
                                  zoom: [[cameraOpts valueForKey:@"zoom"] floatValue]
                                  bearing:[[cameraOpts objectForKey:@"bearing"] doubleValue]
                                  viewingAngle:[[cameraOpts objectForKey:@"tilt"] doubleValue]];
  
    CGRect pluginRect = self.view.frame;
    CGRect mapRect = CGRectMake(0, 0, pluginRect.size.width, pluginRect.size.height - 30);
    self.map = [GMSMapView mapWithFrame:mapRect camera:camera];
    self.map.delegate = self;
    self.map.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  
    Boolean isEnabled = false;
    //controls
    NSDictionary *controls = [initOptions objectForKey:@"controls"];
    if (controls) {
      //compass
      isEnabled = [[controls valueForKey:@"compass"] boolValue];
      if (isEnabled) {
        self.map.settings.compassButton = isEnabled;
      }
      //myLocationButton
      isEnabled = [[controls valueForKey:@"myLocationButton"] boolValue];
      if (isEnabled) {
        self.map.settings.myLocationButton = isEnabled;
        self.map.myLocationEnabled = isEnabled;
      }
      //indoorPicker
      isEnabled = [[controls valueForKey:@"indoorPicker"] boolValue];
      if (isEnabled) {
        self.map.settings.indoorPicker = isEnabled;
        self.map.indoorEnabled = isEnabled;
      }
    }

  
    //gestures
    NSDictionary *gestures = [initOptions objectForKey:@"gestures"];
    if (gestures) {
      //rotate
      isEnabled = [[gestures valueForKey:@"rotate"] boolValue];
      if (isEnabled) {
        self.map.settings.rotateGestures = isEnabled;
      }
      //scroll
      isEnabled = [[gestures valueForKey:@"scroll"] boolValue];
      if (isEnabled) {
        self.map.settings.scrollGestures = isEnabled;
      }
      //tilt
      isEnabled = [[gestures valueForKey:@"tilt"] boolValue];
      if (isEnabled) {
        self.map.settings.tiltGestures = isEnabled;
      }
    }
  
    //mapType
    NSString *typeStr = [initOptions valueForKey:@"mapType"];
    if (typeStr) {
      
      NSDictionary *mapTypes = [NSDictionary dictionaryWithObjectsAndKeys:
                                ^() {return kGMSTypeHybrid; }, @"MAP_TYPE_HYBRID",
                                ^() {return kGMSTypeSatellite; }, @"MAP_TYPE_SATELLITE",
                                ^() {return kGMSTypeTerrain; }, @"MAP_TYPE_TERRAIN",
                                ^() {return kGMSTypeNormal; }, @"MAP_TYPE_NORMAL",
                                ^() {return kGMSTypeNone; }, @"MAP_TYPE_NONE",
                                nil];
      
      typedef GMSMapViewType (^CaseBlock)();
      GMSMapViewType mapType;
      CaseBlock caseBlock = mapTypes[typeStr];
      if (caseBlock) {
        // Change the map type
        mapType = caseBlock();
        self.map.mapType = mapType;
      }
    }
  
  
    [self.view addSubview: self.map];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

#pragma mark - GMSMapViewDelegate

- (void)mapView:(GMSMapView *)mapView
    didTapAtCoordinate:(CLLocationCoordinate2D)coordinate {
  
  NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onMapClick(\"%f,%f\");", coordinate.latitude, coordinate.longitude];
  [self.webView stringByEvaluatingJavaScriptFromString:jsString];
}

#pragma mark - GMSMapViewDelegate

- (void)mapView:(GMSMapView *)mapView willMove:(BOOL)gesture {
NSLog(@"willMove gesture=%hhd", gesture);
  //[mapView clear];
}

- (BOOL)mapView:(GMSMapView *)mapView didTapMarker:(GMSMarker *)marker {
  NSString* jsString = [NSString stringWithFormat:@"plugin.google.maps.Map._onMarkerClick(\"marker%d\");", marker.hash];
  [self.webView stringByEvaluatingJavaScriptFromString:jsString];

	return YES;
}
/*
-(UIView *)mapView:(GMSMapView *)mapView markerInfoWindow:(GMSMarker*)marker
{
    UIView *view = [[UIView alloc]init];
    //customize the UIView, for example, in your case, add a UILabel as the subview of the view
    return view;
}
*/

@end
