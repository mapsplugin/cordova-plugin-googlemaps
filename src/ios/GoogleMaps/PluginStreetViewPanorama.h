//
//  PluginStreetViewPanorama.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "IPluginOverlay.h"

@interface PluginStreetViewPanorama : CDVPlugin<MyPlgunProtocol, IPluginOverlay>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic) BOOL isRemoved;
@property (nonatomic) BOOL initialized;

- (void)getPanorama:(CDVInvokedUrlCommand*)command;
- (void)attachToWebView:(CDVInvokedUrlCommand*)command;
- (void)detachFromWebView:(CDVInvokedUrlCommand*)command;
@end
