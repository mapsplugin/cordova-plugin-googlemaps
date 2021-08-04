//
//  IPluginProtocol.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Foundation/Foundation.h>
#import "PluginViewController.h"

@protocol IPluginProtocol <NSObject>
- (void)pluginUnload;
- (void)setPluginViewController: (PluginViewController*)viewCtrl;
@end
