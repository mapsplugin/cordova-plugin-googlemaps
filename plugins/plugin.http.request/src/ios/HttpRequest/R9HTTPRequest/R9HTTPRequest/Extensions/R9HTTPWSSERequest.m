//
//  R9HTTPWSSERequest.m
//
//  Created by Fujita Taiuke on 12/02/28.
//  Copyright (c) 2012 Revolution 9. All rights reserved.
//

#import "R9HTTPWSSERequest.h"

@interface R9HTTPWSSERequest(private)

- (NSString *)credentialsWithUserId:(NSString *)userId andPassword:(NSString *)password;

@end

@implementation R9HTTPWSSERequest

- (id)initWithURL:(NSURL *)targetUrl andUserId:(NSString *)userId andPassword:(NSString *)password
{
    self = [super initWithURL:targetUrl];
    if (self) {
        [super addHeader:@"application/x.atom+xml, application/xml, text/xml, */*" forKey:@"Accept"];
        [super addHeader:[self credentialsWithUserId:userId andPassword:password] forKey:@"X-WSSE"];
    }  
    return self;
}

#pragma mark - Private methods

- (NSString *)credentialsWithUserId:(NSString *)userId andPassword:(NSString *)password
{
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:sszzz"];
    NSString *formattedDate = [dateFormatter stringFromDate:[NSDate date]];
    formattedDate = [formattedDate stringByReplacingOccurrencesOfString:[NSString stringWithUTF8String:"午前"] 
                                                             withString:@""];
    formattedDate = [formattedDate stringByReplacingOccurrencesOfString:[NSString stringWithUTF8String:"午後"] 
                                                             withString:@""];
    srand(time(nil));
    NSString *nonce = [[NSString stringWithFormat:@"%@%d", formattedDate, rand()] sha1HexHash];
    NSString *passwordDigest = [[[NSString stringWithFormat:@"%@%@%@", nonce, formattedDate, password]
                                 sha1Hash] stringEncodedWithBase64];
    NSString *base64 = [[nonce dataUsingEncoding:NSASCIIStringEncoding] stringEncodedWithBase64];
    NSString *credentials = [NSString stringWithFormat:
                             @"UsernameToken Username=\"%@\", "
                             @"PasswordDigest=\"%@\", "
                             @"Nonce=\"%@\", "
                             @"Created=\"%@\"",  userId, passwordDigest, base64, formattedDate];
    return credentials;
}

@end
