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
- (void)create:(CDVInvokedUrlCommand*)command;
@end

@interface KmlParseClass : NSObject
@property (nonatomic) NSMutableDictionary *styleHolder;
@property (nonatomic) NSMutableDictionary *schemaHolder;

-(NSMutableDictionary *)parseXml:(TBXML *)tbxml rootElement:(TBXMLElement *)rootElement;
@end
