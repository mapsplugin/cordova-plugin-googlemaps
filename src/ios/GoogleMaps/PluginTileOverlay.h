//
//  PluginTileOverlay.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginTileProvider.h"

@interface PluginTileOverlay : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic, strong) NSCache* imgCache;
@property (nonatomic) BOOL initialized;

-(void)create:(CDVInvokedUrlCommand*)command;
-(void)setVisible:(CDVInvokedUrlCommand *)command;
-(void)remove:(CDVInvokedUrlCommand *)command;
-(void)invalidate:(CDVInvokedUrlCommand *)command;
-(void)setZIndex:(CDVInvokedUrlCommand *)command;
-(void)setFadeIn:(CDVInvokedUrlCommand *)command;
-(void)setOpacity:(CDVInvokedUrlCommand *)command;
-(void)onGetTileUrlFromJS:(CDVInvokedUrlCommand *)command;

@end
