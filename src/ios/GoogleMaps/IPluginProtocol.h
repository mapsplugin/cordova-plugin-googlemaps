//
//  IPluginProtocol.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "PluginViewController.h"

@protocol IPluginProtocol <NSObject>
//- (void)onHookedPluginResult:(CDVPluginResult*)result callbackId:(NSString*)callbackId;
- (void)pluginUnload;
- (void)setPluginViewController: (PluginViewController*)viewCtrl;
@end
