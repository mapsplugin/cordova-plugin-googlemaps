//
//  PluginGroundOverlay.h
//  cordova-googlemaps-plugin v2
//
//  Created by Katsumata Masashi.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"
#import "PluginUtil.h"

@interface PluginGroundOverlay : CDVPlugin<IPluginProtocol>
//@property (nonatomic, strong) NSCache* imgCache;
@property (nonatomic) BOOL initialized;

@property (nonatomic, strong) PluginMapViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand *)command;
- (void)setClickable:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setImage:(CDVInvokedUrlCommand*)command;
- (void)setBounds:(CDVInvokedUrlCommand*)command;
- (void)setOpacity:(CDVInvokedUrlCommand*)command;
- (void)setBearing:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand *)command;
- (void)_setImage:(GMSGroundOverlay *)layer urlStr:(NSString *)urlStr completionHandler:(void (^)(BOOL succeeded))completionHandler;
- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock;

@end
