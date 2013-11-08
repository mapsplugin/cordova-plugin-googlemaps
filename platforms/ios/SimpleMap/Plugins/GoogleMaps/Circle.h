//
//  Circle.h
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface Circle : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)createCircle:(CDVInvokedUrlCommand*)command;

@end
