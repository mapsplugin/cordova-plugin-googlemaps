//
//  R9HTTPRequest.m
//
//  Created by 藤田 泰介 on 12/02/25.
//  Copyright (c) 2012 Revolution 9. All rights reserved.
//

#import "R9HTTPRequest.h"

static NSString *kBoundary = @"----------0xKhTmLbOuNdArY";

@interface R9HTTPRequest()

@property (nonatomic) NSMutableDictionary *headers;
@property (nonatomic) NSMutableDictionary *bodies;
@property (nonatomic) NSMutableDictionary *fileInfo;

- (NSData *)createBodyData;
- (NSData *)createMultipartBodyData;
- (void)finish;

@end

@implementation R9HTTPRequest {
    NSURL *_url;
    NSTimeInterval _timeoutSeconds;
    NSHTTPURLResponse *_responseHeader;
    NSMutableData *_responseData;
    NSOperationQueue *_queue;
    NSMutableDictionary *_headers, *_bodies, *_fileInfo;
    BOOL _isExecuting, _isFinished;
}

+ (BOOL)automaticallyNotifiesObserversForKey:(NSString*)key
{
    if ([key isEqualToString:@"isExecuting"] ||
        [key isEqualToString:@"isFinished"]) {
        return YES;
    }
    return [super automaticallyNotifiesObserversForKey:key];
}

- (BOOL)isConcurrent
{
    return YES;
}

- (BOOL)isExecuting
{
    return _isExecuting;
}

- (BOOL)isFinished
{
    return _isFinished;
}

- (NSDictionary *)headers
{
    if (!_headers) {
        _headers = [[NSMutableDictionary alloc] init];
    }
    return _headers;
}

- (NSDictionary *)bodies
{
    if (!_bodies) {
        _bodies = [[NSMutableDictionary alloc] init];
    }
    return _bodies;
}

- (NSDictionary *)fileInfo
{
    if (!_fileInfo) {
        _fileInfo = [[NSMutableDictionary alloc] init];
    }
    return _fileInfo;
}

- (id)initWithURL:(NSURL *)targetUrl
{
    self = [super init];
    if (self) {
        _url = targetUrl;
        _shouldRedirect = YES;
        _HTTPMethod = @"GET";
    }
    return self;
}

/*
- (void)dealloc
{
    NSLog(@"%@#%@", NSStringFromClass([self class]), NSStringFromSelector(_cmd));
}
*/

- (void)startRequest
{
    _queue = [[NSOperationQueue alloc] init];
    [_queue addOperation:self];
}

- (void)start
{
    [self setValue:@(YES) forKey:@"isExecuting"];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:_url];
    if ([self.headers count] > 0) {
        [request setAllHTTPHeaderFields:self.headers];
    }
    [request setHTTPMethod:self.HTTPMethod];
    if ([self.fileInfo count] > 0) {
        NSString *contentType = [NSString stringWithFormat:@"multipart/form-data; boundary=%@", kBoundary];
        [request addValue:contentType forHTTPHeaderField:@"Content-Type"];
        [request setHTTPBody:[self createMultipartBodyData]];
    } else {
        [request setHTTPBody:[self createBodyData]];
    }
    [request setTimeoutInterval:_timeoutSeconds];
    NSURLConnection *conn = [NSURLConnection connectionWithRequest:request delegate:self];
    if (conn != nil) {
        do {
            [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate distantFuture]];
        } while (_isExecuting);
    }
}

- (void)setTimeoutInterval:(NSTimeInterval)seconds
{
    _timeoutSeconds = seconds;
}

- (void)addHeader:(NSString *)value forKey:(NSString *)key
{
    [self.headers setObject:value forKey:key];
}

- (void)addBody:(NSString *)value forKey:(NSString *)key
{
    [self.bodies setObject:value forKey:key];
}

- (void)setData:(NSData *)data withFileName:(NSString *)fileName andContentType:(NSString *)contentType forKey:(NSString *)key
{
	[self.fileInfo setValue:key forKey:@"key"];
	[self.fileInfo setValue:fileName forKey:@"fileName"];
	[self.fileInfo setValue:contentType forKey:@"contentType"];
	[self.fileInfo setValue:data forKey:@"data"];
}

#pragma mark - Private methods

- (NSData *)createMultipartBodyData
{
    NSMutableString *bodyString = [NSMutableString string];
    [bodyString appendFormat:@"--%@\r\n", kBoundary];
    [self.bodies enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        [bodyString appendFormat:@"Content-Disposition: form-data; name=\"%@\"\r\n\r\n", key];
        [bodyString appendFormat:@"%@", obj];
        [bodyString appendFormat:@"\r\n--%@\r\n", kBoundary];
    }];
    [bodyString appendFormat:@"Content-Disposition: form-data; name=\"%@\";"
                                @" filename=\"%@\"\r\n", [self.fileInfo objectForKey:@"key"], [self.fileInfo objectForKey:@"fileName"]];
    [bodyString appendFormat:@"Content-Type: %@\r\n\r\n", [self.fileInfo objectForKey:@"contentType"]];
    NSMutableData *bodyData = [NSMutableData data];
    [bodyData appendData:[bodyString dataUsingEncoding:NSUTF8StringEncoding]];
    [bodyData appendData:[self.fileInfo objectForKey:@"data"]];
    [bodyData appendData:[[NSString stringWithFormat:@"\r\n--%@--\r\n", kBoundary] dataUsingEncoding:NSUTF8StringEncoding]];
    return bodyData;
}

- (NSData *)createBodyData
{
    NSMutableString *content = [NSMutableString string];
    [self.bodies enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        if (![content isEqualToString:@""]) {
            [content appendString:@"&"];
        }
        if (![key isEqualToString:@""]) {
            [content appendFormat:@"%@=%@", key, obj];
        } else {
            [content appendString:obj];
        }
    }];
    return [content dataUsingEncoding:NSUTF8StringEncoding];
}

#pragma mark - NSURLConnectionDelegate and NSURLConnectionDataDelegate methods

// リダイレクトの処理
- (NSURLRequest *)connection:(NSURLConnection *)connection
             willSendRequest:(NSURLRequest *)request redirectResponse:(NSURLResponse *)response
{
    if (response && self.shouldRedirect == NO) {
        return nil;
    }
    return request;
}

// レスポンスヘッダの受け取り
- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
    _responseHeader = [(NSHTTPURLResponse *)response copy];
    _responseData = [[NSMutableData alloc] init];
}

// データの受け取り
- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    [_responseData appendData:data];
}

// Progress
- (void)connection:(NSURLConnection *)connection didSendBodyData:(NSInteger)bytesWritten
    totalBytesWritten:(NSInteger)totalBytesWritten
    totalBytesExpectedToWrite:(NSInteger)totalBytesExpectedToWrite
{
    if (totalBytesExpectedToWrite == 0) return;
    if (self.uploadProgressHandler) {
        float progress = [[NSNumber numberWithInteger:totalBytesWritten] floatValue];
        float total = [[NSNumber numberWithInteger: totalBytesExpectedToWrite] floatValue];
        __weak R9HTTPRequest *_self = self;
        NSOperationQueue *queue = [NSOperationQueue mainQueue];
        [queue addOperationWithBlock:^{
            _self.uploadProgressHandler(progress / total);
        }];
    }
}

// 通信エラー
- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
    [self performSelectorOnMainThread:@selector(failed:) withObject:error waitUntilDone:NO];
}

// 通信終了
- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    // Run on main thread.
    [self performSelectorOnMainThread:@selector(done) withObject:nil waitUntilDone:NO];
}

- (void)failed:(NSError *)error
{
    self.failedHandler(error);
    [self finish];
}

- (void)done
{
    NSString *responseString = nil;
    if (_responseData) {
        responseString = [[NSString alloc] initWithData:_responseData encoding:NSUTF8StringEncoding];
    }
    //NSLog(@"is main thread:%d", [[NSThread currentThread] isMainThread]);

	// In the case of response is NSString
	if( self.completionHandler ) {
		self.completionHandler(_responseHeader, responseString);
	}
	else if( self.completionHandlerWithData ){
		// In the case of response is NSData
		self.completionHandlerWithData(_responseHeader, _responseData);
	}

    [self finish];
}

- (void)finish
{
    [self setValue:@(NO) forKey:@"isExecuting"];
    [self setValue:@(YES) forKey:@"isFinished"];
}

@end
