//
//  MyPluginLayerDebugView.h
//  DevApp
//
//  Created by Katsumata Masashi on 9/22/14.
//
//

#import <UIKit/UIKit.h>

@interface MyPluginLayerDebugView : UIView
@property (nonatomic) NSDictionary *embedRect;
@property (nonatomic) UIWebView *webView;
@property (nonatomic) float offsetX;
@property (nonatomic) float offsetY;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) BOOL clickable;
@property (nonatomic) NSMutableDictionary *HTMLNodes;
- (void)putHTMLElement:(NSString *)domId size:(NSDictionary *)size;
- (void)removeHTMLElement:(NSString *)domId;
- (void)clearHTMLElement;
@end
