//
//  PluginWindow.h
//  DevApp
//
//  Created by masashi on 8/12/14.
//
//

#import <UIKit/UIKit.h>

@interface PluginWindow : UIWindow

@property (nonatomic) NSDictionary *embedRect;
@property (nonatomic) BOOL isMapAction;

- (void)sendEvent:(UIEvent *)event;
@end
