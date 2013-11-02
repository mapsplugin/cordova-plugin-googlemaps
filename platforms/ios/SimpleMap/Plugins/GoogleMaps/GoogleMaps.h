//
//  GoogleMaps.h
//  SimpleMap
//
//  Created by masashi on 10/31/13.
//
//

#import <Cordova/CDV.h>
#import <GoogleMaps/GoogleMaps.h>

@interface GoogleMaps : CDVPlugin

- (void)GoogleMap_getMap:(CDVInvokedUrlCommand*)command;

@end
