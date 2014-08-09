//
//  touchTest.h
//  touchTest
//
//  Created by masashi on 8/6/14.
//
//

#import <Cordova/CDV.h>
#import "MyWindow.h"
#import <GoogleMaps/GoogleMaps.h>
#import "GoogleMapsViewController.h"

@interface touchTest : CDVPlugin
@property (nonatomic) UIView *root;
@property (nonatomic) GoogleMapsViewController *mapCtrl_;
@end
