//
//  touchTest.h
//  touchTest
//
//  Created by masashi on 8/6/14.
//
//

#import <Cordova/CDV.h>
#import "MyWindow.h"

@interface touchTest : CDVPlugin<UIGestureRecognizerDelegate>
@property (nonatomic) UIView *root;
@end
@implementation UIGestureRecognizer (Cancel)

- (void)cancel {
    self.enabled = NO;
    self.enabled = YES;
}

@end