//
//  R9HTTPWSSERequestTest.m
//  R9HTTPRequest
//
//  Created by taisuke fujita on 12/07/02.
//  Copyright (c) 2012年 Revolution 9. All rights reserved.
//

#import "R9HTTPWSSERequestTest.h"
#import "R9HTTPRequest.h"
#import "R9HTTPWSSERequest.h"

@implementation R9HTTPWSSERequestTest {
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

- (void)_testWSSERequest 
{
    // Replace {hatenaID} and {password}.
    R9HTTPWSSERequest *request = [[R9HTTPWSSERequest alloc] initWithURL:[NSURL URLWithString:@"http://d.hatena.ne.jp/{hatenaID}/atom/draft"] andUserId:@"{hatenaID}" andPassword:@"{password}"];
    [request setCompletionHandler:^(NSHTTPURLResponse *responseHeader, NSString *responseString){
        NSLog(@"%@", responseString);
        STAssertTrue(responseHeader.statusCode == 200, @"");
        _isFinished = YES;
    }];
    [request setFailedHandler:^(NSError *error) {
        STFail(@"%@", error);
        _isFinished = YES;
    }];
    [request startRequest];
}

@end
