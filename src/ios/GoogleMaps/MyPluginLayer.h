//
//  MyPluginLayer.h
//  DevApp
//
//  Created by masashi on 8/13/14.
//
//

#import <UIKit/UIKit.h>
#import <GoogleMaps/GoogleMaps.h>

@interface MyPluginLayer : UIView

@property (nonatomic) UIWebView *webView;
@property (nonatomic) GMSMapView *map;
@property (nonatomic) NSDictionary *embedRect;
@property (nonatomic) BOOL clickable;

- (id)initWithFrame:(CGRect)aRect;
- (void)putHTMLElement:(NSString *)domId size:(NSDictionary *)size;
- (void)removeHTMLElement:(NSString *)domId;
- (void)clearHTMLElement;

@end
