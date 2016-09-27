//
//  PluginGroundOverlay.h
//  SimpleMap
//
//  Created by Katsumata Masashi on 12/4/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"

@interface PluginGroundOverlay : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) NSMutableDictionary* objects;
@property (nonatomic) NSOperationQueue *executeQueue;
@property (nonatomic, strong) NSCache* imgCache;

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;
- (void)remove:(CDVInvokedUrlCommand *)command;
- (void)setClickable:(CDVInvokedUrlCommand*)command;
- (void)setVisible:(CDVInvokedUrlCommand*)command;
- (void)setImage:(CDVInvokedUrlCommand*)command;
- (void)setBounds:(CDVInvokedUrlCommand*)command;
- (void)setOpacity:(CDVInvokedUrlCommand*)command;
- (void)setBearing:(CDVInvokedUrlCommand*)command;
- (void)setZIndex:(CDVInvokedUrlCommand *)command;
- (void)_setImage:(GMSGroundOverlay *)layer urlStr:(NSString *)urlStr completionHandler:(MYCompletionHandler)completionHandler;
- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(NSError *error, UIImage *image))completionBlock;

@end
