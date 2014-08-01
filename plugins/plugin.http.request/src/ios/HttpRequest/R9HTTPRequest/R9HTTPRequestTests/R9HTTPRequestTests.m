//
//  R9HTTPRequestTests.m
//  R9HTTPRequestTests
//
//  Created by taisuke fujita on 12/03/02.
//  Copyright (c) 2012年 Revolution 9. All rights reserved.
//

#import "R9HTTPRequestTests.h"
#import "R9HTTPRequest.h"
#import "R9HTTPWSSERequest.h"

@implementation R9HTTPRequestTests {
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

- (void)testGETRequest
{
    NSLog(@"isMainThread:%d", [[NSThread currentThread] isMainThread]);
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"http://www.apple.com"]];
    [request setTimeoutInterval:360];
    [request setFailedHandler:^(NSError *error){
        NSLog(@"%@", error);
        STFail(@"Fail");
        _isFinished = YES;
    }];
    [request setCompletionHandler:^(NSHTTPURLResponse *responseHeader, NSString *responseString){
        NSLog(@"%@", responseString);
        STAssertTrue([[NSThread currentThread] isMainThread] == YES, @"");
        STAssertTrue(responseHeader.statusCode == 200, @"");
        _isFinished = YES;
    }];
    [request startRequest];
}

- (void)test404NotFound
{
    NSLog(@"%d", [[NSThread currentThread] isMainThread]);
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"http://www.apple.com/jpkjijbhb"]];
    [request setFailedHandler:^(NSError *error){
        NSLog(@"%@", error);
        STFail(@"Fail");
        _isFinished = YES;
    }];
    [request setCompletionHandler:^(NSHTTPURLResponse *responseHeader, NSString *responseString){
        STAssertTrue(responseHeader.statusCode == 404, @"");
        _isFinished = YES;
    }];
    [request startRequest];
}

- (void)testConnectionError
{
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"http://fdsfsdfsdfsd.co/"]];
    [request setCompletionHandler:^(NSHTTPURLResponse *responseHeader, NSString *responseString){
        STFail(@"Fail");
        _isFinished = YES;
    }];
    [request setTimeoutInterval:10];
    [request setFailedHandler:^(NSError *error){
        NSLog(@"%@", error);
        STAssertTrue([[NSThread currentThread] isMainThread] == YES, @"");
        STAssertTrue(YES, @"");
        _isFinished = YES;
    }];
    [request startRequest];
}

- (void)testPOSTRequest
{
    // see http://posttestserver.com/
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"http://posttestserver.com/post.php"]];
    [request setTimeoutInterval:360];
    [request setHTTPMethod:@"POST"];
    [request addBody:@"test" forKey:@"TestKey"];
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

- (void)testMultipartPOSTRequestWithPNG
{
    // see http://posttestserver.com/
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"https://posttestserver.com/post.php"]];
    [request setHTTPMethod:@"POST"];
    [request addBody:@"test" forKey:@"TestKey"];
    // create image 
    UIImage *image = [UIImage imageNamed:@"sync"];
    NSData *pngData = [[NSData alloc] initWithData:UIImagePNGRepresentation(image)];
    if (!pngData) {
        STFail(@"Fail to create image.");
    }
    [request setData:pngData withFileName:@"sample.png" andContentType:@"image/png" forKey:@"file"];
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

- (void)testMultipartPOSTRequestWithJPG
{
    // see http://posttestserver.com/
    R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:[NSURL URLWithString:@"https://posttestserver.com/post.php"]];
    [request setTimeoutInterval:360];
    [request setHTTPMethod:@"POST"];
    [request addBody:@"test" forKey:@"TestKey"];
    // create image 
    UIImage *image = [UIImage imageNamed:@"sample.jpg"];
    NSData *jpgData = [[NSData alloc] initWithData:UIImageJPEGRepresentation(image, 80)];
    if (!jpgData) {
        STFail(@"Fail to create image.");
    }
    [request setData:jpgData withFileName:@"sample.jpg" andContentType:@"image/jpg" forKey:@"file"];
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
    [request setUploadProgressHandler:^(float newProgress){
        STAssertTrue([[NSThread currentThread] isMainThread] == YES, @"");
        STAssertTrue(newProgress > 0.0, @"");
        NSLog(@"%g", newProgress);
    }];
    [request startRequest];
}

@end
