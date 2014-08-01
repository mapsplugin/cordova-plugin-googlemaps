//
//  R9HTTPRequestRedirectTest.m
//  R9HTTPRequest
//
//  Created by taisuke fujita on 12/07/02.
//  Copyright (c) 2012年 Revolution 9. All rights reserved.
//

#import "R9HTTPRequestRedirectTest.h"
#import "R9HTTPRequest.h"
#import "R9HTTPWSSERequest.h"

@implementation R9HTTPRequestRedirectTest {
    BOOL _isFinished;
}

- (void)setUp
{
    [super setUp];
    _isFinished = NO;
}

- (void)tearDown
{
    do {
        [[NSRunLoop currentRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:1.0]];
    } while (!_isFinished);
    [super tearDown];
}


- (void)testShouldRedirectYes
{
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"http://jigsaw.w3.org/HTTP/300/301.html"]];
    [request setFailedHandler:^(NSError *error){
        NSLog(@"%@", error);
        STFail(@"Fail");
        _isFinished = YES;
    }];
    [request setCompletionHandler:^(NSHTTPURLResponse *responseHeader, NSString *responseString){
        NSLog(@"%@", responseString);
        STAssertTrue(responseHeader.statusCode == 200, @"");
        _isFinished = YES;
    }];
    [request startRequest];
}

- (void)testShouldRedirectNo
{
    
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"http://jigsaw.w3.org/HTTP/300/301.html"]];
    request.shouldRedirect = NO;
    [request setFailedHandler:^(NSError *error){
        STFail(@"Fail");
        _isFinished = YES;
    }];
    [request setCompletionHandler:^(NSHTTPURLResponse *responseHeader, NSString *responseString){
        NSLog(@"%@", responseString);
        STAssertTrue(responseHeader.statusCode == 301, @"");
        _isFinished = YES;
    }];
    [request startRequest];
}

@end
