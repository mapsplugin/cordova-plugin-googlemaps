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
- (void)setGoogleMapsViewController: (GoogleMapsViewController*)viewCtrl;
@end
