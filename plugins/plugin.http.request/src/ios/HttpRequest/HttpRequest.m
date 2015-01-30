//
//  HttpRequest.m
//  MyTest
//
//  Created by masashi on 4/14/14.
//
//

#import "HttpRequest.h"
#import "R9HTTPRequest.h"

@implementation HttpRequest

- (void)execute:(CDVInvokedUrlCommand *)command
{
    NSString *method = [command.arguments objectAtIndex:0];
    NSString *urlStr = [command.arguments objectAtIndex:1];
    int argCnt = [command.arguments count];
    
    NSURL *URL = [NSURL URLWithString:urlStr];
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:URL];
    

    [request setHTTPMethod:[method uppercaseString]];
    if (argCnt == 3) {
        NSDictionary *params = [command.arguments objectAtIndex:2];
        if (params != nil && ![params isEqual:[NSNull null]]) {
          NSEnumerator *enumerator = [params keyEnumerator];
          id key;
          while((key = [enumerator nextObject])) {
            [request addBody:[params objectForKey:key] forKey:key];
          }
        }
      
    }
    [request setTimeoutInterval:5];
    [request setFailedHandler:^(NSError *error){
        CDVPluginResult *result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_ERROR
                                   messageAsString: [NSString stringWithFormat:@"%@", error]];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
    
    [request setCompletionHandler:^(NSHTTPURLResponse *responseHeader, NSString *responseString){
        CDVPluginResult *result = [CDVPluginResult
                                   resultWithStatus:CDVCommandStatus_OK
                                   messageAsString:responseString];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
    [request startRequest];
}
@end
