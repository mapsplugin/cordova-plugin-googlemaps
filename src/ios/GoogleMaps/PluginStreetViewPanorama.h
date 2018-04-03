//
//  PluginStreetViewPanorama.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"
#import "IPluginView.h"
#import "PluginStreetViewPanoramaController.h"

@interface PluginStreetViewPanorama : CDVPlugin<IPluginProtocol, IPluginView>
@property (nonatomic, strong) PluginStreetViewPanoramaController* panoramaCtrl;
@property (nonatomic) BOOL isRemoved;
@property (nonatomic) BOOL initialized;

- (void)getPanorama:(CDVInvokedUrlCommand*)command;
- (void)attachToWebView:(CDVInvokedUrlCommand*)command;
- (void)detachFromWebView:(CDVInvokedUrlCommand*)command;
- (void)moveCamera:(CDVInvokedUrlCommand*)command;
@end
