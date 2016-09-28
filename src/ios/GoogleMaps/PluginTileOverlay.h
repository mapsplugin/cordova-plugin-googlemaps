//
//  TileOverlay.h
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "LocalSyncTileLayer.h"

@interface PluginTileOverlay : CDVPlugin<MyPlgunProtocol>

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic, strong) NSMutableDictionary* objects;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic, strong) NSCache* imgCache;

- (void)create:(CDVInvokedUrlCommand*)command;
-(void)setVisible:(CDVInvokedUrlCommand *)command;
-(void)remove:(CDVInvokedUrlCommand *)command;
-(void)setZIndex:(CDVInvokedUrlCommand *)command;
-(void)setFadeIn:(CDVInvokedUrlCommand *)command;
-(void)setOpacity:(CDVInvokedUrlCommand *)command;
-(void)setVisible:(CDVInvokedUrlCommand *)command;

@end
