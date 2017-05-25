//
//  MyPluginLayerDebugView.h
//  DevApp
//
//  Created by Katsumata Masashi.
//
//

#import <UIKit/UIKit.h>
#import "GoogleMapsViewController.h"

@interface MyPluginLayerDebugView : UIView
@property (nonatomic) UIView *webView;
@property (nonatomic) BOOL debuggable;
@property (nonatomic) NSMutableDictionary *mapCtrls;
@property (nonatomic) NSMutableDictionary *HTMLNodes;
@end
