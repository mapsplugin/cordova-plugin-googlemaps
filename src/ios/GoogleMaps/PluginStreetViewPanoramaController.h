//
//  PluginStreetViewPanoramaController.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>
#import <math.h>
#import "PluginUtil.h"
#import "NSData+Base64.h"
#import "IPluginProtocol.h"
#import "PluginObjects.h"
#import "PluginViewController.h"
#import <GoogleMaps/GoogleMaps.h>

@interface PluginStreetViewPanoramaController : PluginViewController<GMSPanoramaViewDelegate>

@property (nonatomic) GMSPanoramaView* panorama;
@end
