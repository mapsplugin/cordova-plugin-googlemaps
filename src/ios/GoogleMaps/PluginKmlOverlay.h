//
//  PluginKmlOverlay.h
//  cordova-googlemaps-plugin v2
//
//  Created by Katsumata Masashi.
//
//

#import "CordovaGoogleMaps.h"
#import "IPluginProtocol.h"
#import "PluginUtil.h"
#import "TBXML.h"
#import "IPluginProtocol.h"

@interface PluginKmlOverlay : CDVPlugin<IPluginProtocol>

@property (nonatomic) BOOL initialized;
@property (nonatomic, strong) PluginMapViewController* mapCtrl;
- (void)create:(CDVInvokedUrlCommand*)command;
@end

@interface KmlParseClass : NSObject
@property (nonatomic) NSMutableDictionary *styleHolder;
@property (nonatomic) NSMutableDictionary *schemaHolder;

-(NSMutableDictionary *)parseXml:(TBXML *)tbxml rootElement:(TBXMLElement *)rootElement;
@end
