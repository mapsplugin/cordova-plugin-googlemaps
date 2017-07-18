//
//  PluginMarker.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"
#import "NSData+Base64.h"

@interface PluginMarker : CDVPlugin<MyPlgunProtocol>

@property (nonatomic, strong) NSCache* icons;
@property (nonatomic) NSMutableDictionary* iconCacheKeys;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic) NSMutableDictionary* objects;

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;
- (void)showInfoWindow:(CDVInvokedUrlCommand*)command;
- (void)hideInfoWindow:(CDVInvokedUrlCommand*)command;
- (void)getPosition:(CDVInvokedUrlCommand*)command;
- (void)setSnippet:(CDVInvokedUrlCommand*)command;
- (void)setTitle:(CDVInvokedUrlCommand*)command;
- (void)setActiveMarkerId:(CDVInvokedUrlCommand*)command;
- (void)setFlat:(CDVInvokedUrlCommand*)command;
- (void)setOpacity:(CDVInvokedUrlCommand*)command;
- (void)setDraggable:(CDVInvokedUrlCommand*)command;
- (void)setDisableAutoPan:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setIcon:(CDVInvokedUrlCommand*)command;
- (void)setIconAnchor:(CDVInvokedUrlCommand*)command;
- (void)setInfoWindowAnchor:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand*)command;
- (void)setPosition:(CDVInvokedUrlCommand*)command;
- (void)setRotation:(CDVInvokedUrlCommand*)command;
- (void)setAnimation:(CDVInvokedUrlCommand*)command;
- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock;


// Internal use
/*
-(void)setMarkerAnimation_:(NSString *)animation marker:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId;
-(void)setDropAnimation_:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId;
-(void)setBounceAnimation_:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId;
*/

-(void)setIcon_:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty callbackBlock:(void (^)(BOOL successed, id resultObj)) callbackBlock;
-(void)_removeMarker:(GMSMarker *)marker;


@end
