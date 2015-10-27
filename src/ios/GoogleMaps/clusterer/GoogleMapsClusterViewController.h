//
//  GoogleMapsClusterViewController.h
//  Erdgastankstellen
//
//  Created by Christian on 21.04.15.
//
//

#import <Cordova/CDV.h>
#import <GoogleMaps/GoogleMaps.h>
#import "GoogleMapsViewController.h"
#import <UIKit/UIKit.h>
#import "PluginUtil.h"
#import "NSData+Base64.h"

// Clustering
#import "GClusterAlgorithm.h"
#import "GClusterRenderer.h"
#import "GQTPointQuadTreeItem.h"
//#import "GAnimatedClusterRenderer.h"

@interface GoogleMapsClusterViewController : GoogleMapsViewController

@property(nonatomic, strong) id<GClusterAlgorithm> clusterAlgorithm;
@property(nonatomic, strong) id<GClusterRenderer> clusterRenderer;
@property(nonatomic) double previousZoom;

-(id)initWithAlgorithm:(id<GClusterAlgorithm>) algorithm andRenderer:(id<GClusterRenderer>) renderer andOptions:(NSDictionary *)options;
-(void)cluster;
-(void)updateCluster;

@end
