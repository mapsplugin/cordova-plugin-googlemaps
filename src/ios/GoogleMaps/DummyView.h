//
//  DummyView.h
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import <UIKit/UIKit.h>
#import <GoogleMaps/GoogleMaps.h>

@interface DummyView : UIView

@property (nonatomic) UIWebView *webView;
@property (nonatomic) GMSMapView *map;
@property (nonatomic) NSDictionary *embedRect;
@end
