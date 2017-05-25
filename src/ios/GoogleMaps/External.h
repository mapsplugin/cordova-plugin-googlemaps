//
//  External.h
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "CordovaGoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface External : CDVPlugin

-(void)launchNavigation:(CDVInvokedUrlCommand *)command;

@end
