//
//  touchTest.h
//  touchTest
//
//  Created by masashi on 8/6/14.
//
//

#import <Cordova/CDV.h>
#import "TapDetectingWindow.h"
#import "TouchDetectingViewController.h"

@interface touchTest : CDVPlugin<UIGestureRecognizerDelegate>
@property (nonatomic) UIView *root;
@end
