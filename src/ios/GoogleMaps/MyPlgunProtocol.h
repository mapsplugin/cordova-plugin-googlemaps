//
//  MyPlgunProtocol.h
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>

@protocol MyPlgunProtocol <NSObject>
@property (nonatomic, strong) NSMutableDictionary* objects;
- (void)pluginUnload;
@end
