//
//  External.h
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface External : CDVPlugin<MyPlgunProtocol>

@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
-(void)launchNavigation:(CDVInvokedUrlCommand *)command;

@end
