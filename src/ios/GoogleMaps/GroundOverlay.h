//
//  GroundOverlay.h
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/4/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"
#import "NSData+Base64.h"

@interface GroundOverlay : CDVPlugin<MyPlgunProtocol>

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)createGroundOverlay:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand *)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setImage:(CDVInvokedUrlCommand*)command;
- (void)setBounds:(CDVInvokedUrlCommand*)command;
- (void)setOpacity:(CDVInvokedUrlCommand*)command;
- (void)setBearing:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand *)command;
- (void)_setImage:(GMSGroundOverlay *)layer urlStr:(NSString *)urlStr completionHandler:(MYCompletionHandler)completionHandler;

@end
