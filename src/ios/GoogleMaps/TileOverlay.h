//
//  TileOverlay.h
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface TileOverlay : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) NSMutableDictionary* tileLayerFormats;
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)createTileOverlay:(CDVInvokedUrlCommand*)command;
-(void)setVisible:(CDVInvokedUrlCommand *)command;
-(void)remove:(CDVInvokedUrlCommand *)command;
-(void)clearTileCache:(CDVInvokedUrlCommand *)command;
-(void)setTileUrlFormat:(CDVInvokedUrlCommand *)command;
-(void)setZIndex:(CDVInvokedUrlCommand *)command;
-(void)setFadeIn:(CDVInvokedUrlCommand *)command;
-(void)setOpacity:(CDVInvokedUrlCommand *)command;

@end
