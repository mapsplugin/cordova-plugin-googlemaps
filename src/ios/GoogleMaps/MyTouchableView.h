//
//  MyTouchableView.h
//  DevApp
//
//  Created by Masashi Katsumata on 3/10/15.
//
//

#import <UIKit/UIKit.h>
#import <GoogleMaps/GoogleMaps.h>
#import "GoogleMapsViewController.h"

@interface MyTouchableView : UIView

@property (nonatomic) GoogleMapsViewController* mapCtrl;
- (id)initWithFrame:(CGRect)aRect;
@end
