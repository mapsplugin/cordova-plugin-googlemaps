//
//  Polygon.h
//  SimpleMap
//
//  Created by masashi on 11/13/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"
@interface Polygon : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)createPolygon:(CDVInvokedUrlCommand*)command;

@end
