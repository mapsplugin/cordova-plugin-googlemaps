//
//  PluginStreetViewPanorama.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"
#import "PluginStreetViewPanoramaController.h"

@interface PluginStreetViewPanorama : CDVPlugin<IPluginProtocol>
@property (nonatomic, strong) PluginStreetViewPanoramaController* panoramaCtrl;
@property (nonatomic) BOOL isRemoved;
@property (nonatomic) BOOL initialized;

- (void)getPanorama:(CDVInvokedUrlCommand*)command;
- (void)setPov:(CDVInvokedUrlCommand*)command;
- (void)setPosition:(CDVInvokedUrlCommand*)command;
- (void)setPanningGesturesEnabled:(CDVInvokedUrlCommand*)command;
- (void)setZoomGesturesEnabled:(CDVInvokedUrlCommand*)command;
- (void)setNavigationEnabled:(CDVInvokedUrlCommand*)command;
- (void)setStreetNamesEnabled:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
@end
