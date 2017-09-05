//
//  PluginKmlOverlay.h
//  cordova-googlemaps-plugin v2
//
//  Created by Katsumata Masashi.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"
#import "PluginUtil.h"
#import "TBXML.h"
#import "MyPlgunProtocol.h"

@interface PluginKmlOverlay : CDVPlugin<MyPlgunProtocol>

@property (nonatomic) BOOL initialized;
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
@property (nonatomic, strong) NSString* kmlId;
@property (nonatomic, strong) UIView* _loadingView;
@property (nonatomic, strong) UIActivityIndicatorView *spinner;

- (void)createKmlOverlay:(CDVInvokedUrlCommand*)command;


@end
