//
//  R9HTTPWSSERequest.h
//
//  Created by Fujita Taiuke on 12/02/28.
//  Copyright (c) 2012 Revolution 9. All rights reserved.
//

#import "R9HTTPRequest.h"
#import "NSData+Base64.h"
#import "NSData+Crypto.h"
#import "NSString+Crypto.h"

@interface R9HTTPWSSERequest : R9HTTPRequest

- (id)initWithURL:(NSURL *)targetUrl andUserId:(NSString *)userId andPassword:(NSString *)password;

@end
