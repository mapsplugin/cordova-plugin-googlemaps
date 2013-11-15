//
//  Polyline.h
//  SimpleMap
//
//  Created by masashi on 11/14/13.
//
//

#import "GoogleMaps.h"
#import "MyPlgunProtocol.h"

@interface Polyline : CDVPlugin<MyPlgunProtocol>
@property (nonatomic, strong) GoogleMapsViewController* mapCtrl;
- (void)createPolyline:(CDVInvokedUrlCommand*)command;


@end
