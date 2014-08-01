//
//  HttpRequest.h
//  MyTest
//
//  Created by masashi on 4/14/14.
//
//

#import <Cordova/CDVPlugin.h>

@interface HttpRequest : CDVPlugin
- (void) execute:(CDVInvokedUrlCommand*)command;
@end
