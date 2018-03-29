//
//  IPluginOverlay
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import <Foundation/Foundation.h>

@protocol IPluginOverlay <NSObject>
- (void)attachToWebView:(CDVInvokedUrlCommand*)command;
- (void)detachFromWebView:(CDVInvokedUrlCommand*)command;
@end
