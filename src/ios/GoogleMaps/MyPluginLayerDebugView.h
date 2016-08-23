//
//  MyPluginLayerDebugView.h
//  DevApp
//
//  Created by Katsumata Masashi on 9/22/14.
//
//

#import <UIKit/UIKit.h>
#import "GoogleMapsViewController.h"

@interface MyPluginLayerDebugView : UIView
@property (nonatomic) UIWebView *webView;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) NSMutableDictionary *mapCtrls;
@property (nonatomic) NSMutableDictionary *HTMLNodes;
@end
