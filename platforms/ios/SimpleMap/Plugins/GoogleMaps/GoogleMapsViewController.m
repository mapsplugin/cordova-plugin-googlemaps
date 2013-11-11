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
    [GMSServices provideAPIKey:@"AIzaSyADns99mO62aBapBN4_cnCJQnYdh-j6-ug"];
  
    GMSCameraPosition *camera = [GMSCameraPosition cameraWithLatitude:0
                                longitude:0
                                zoom:0];
    CGRect pluginRect = self.view.frame;
    CGRect mapRect = CGRectMake(0, 0, pluginRect.size.width, pluginRect.size.height - 30);
    self.map = [GMSMapView mapWithFrame:mapRect camera:camera];
    self.map.delegate = self;
    self.map.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
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
