//
//  External.h
//  SimpleMap
//
//  Created by Masashi Katsumata on 11/19/13.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface External : CDVPlugin

-(void)launchNavigation:(CDVInvokedUrlCommand *)command;

@end
