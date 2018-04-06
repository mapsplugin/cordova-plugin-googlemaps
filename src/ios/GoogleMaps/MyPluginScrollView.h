//
//  MyPluginScrollView.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <UIKit/UIKit.h>
#import "MyPluginLayerDebugView.h"

@interface MyPluginScrollView : UIScrollView

@property (nonatomic) MyPluginLayerDebugView *debugView;

- (void)attachView:(UIView *)view depth:(NSInteger)depth;
- (void)detachView:(UIView *)view;
@end
