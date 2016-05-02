//
//  MyPluginLayer.h
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import <UIKit/UIKit.h>
#import <GoogleMaps/GoogleMaps.h>
#import "GoogleMapsViewController.h"

@interface MyPluginLayer : UIView

@property (nonatomic) GoogleMapsViewController* mapCtrl;
@property (nonatomic) NSMutableDictionary *drawRects;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) BOOL clickable;
@property (nonatomic) NSMutableDictionary *HTMLNodes;
@property (nonatomic) NSMutableDictionary *mapViews;

- (id)init:(CGRect)aRect;
- (void)putHTMLElement:(NSString *)mapId domId:(NSString *)domId size:(NSDictionary *)size;
- (void)removeHTMLElement:(NSString *)mapId domId:(NSString *)domId;
- (void)clearHTMLElement:(NSString *)mapId;
- (void)updateViewPosition:(NSString *)mapId;
- (void)addMapView:(NSString *)mapId map:(GMSMapView *)map;

@end
