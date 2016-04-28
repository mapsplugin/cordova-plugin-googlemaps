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

@property (nonatomic) UIView *webView;
@property (nonatomic) NSMutableDictionary *drawRects;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) BOOL clickable;
@property (nonatomic) NSMutableDictionary *HTMLNodes;

- (id)init:(CGRect)aRect;
- (void)putHTMLElement:(NSString *)mapId domId:(NSString *)domId size:(NSDictionary *)size;
- (void)removeHTMLElement:(NSString *)mapId domId:(NSString *)domId;
- (void)clearHTMLElement:(NSString *)mapId;
@end
