//
//  MyWindow.m
//  touchTest
//
//  Created by Katsumata Masashi on 8/8/14.
//
//

#import "MyWindow.h"


@implementation MyWindow

@synthesize wView, wDelegate;

- (void)sendEvent:(UIEvent *)event {
    [super sendEvent:event];
    if (wView == nil || wDelegate == nil) {
        return;
    }
    // 2本指でのマルチタッチか
    NSSet *touches = [event allTouches];
    /*
    if (touches.count != 2) {
        return;
    }
    */

    UITouch *touch = touches.anyObject;
    // 指定のUIWebViewへのタッチか
    if ([touch.view isDescendantOfView:wView] == NO) {
        return;
    }

    switch (touch.phase) {
        case UITouchPhaseBegan:
            if ([self.wDelegate
                 respondsToSelector:@selector(touchesBeganWeb:withEvent:)]) {
                [self.wDelegate
                    performSelector:@selector(touchesBeganWeb:withEvent:)
                    withObject:touches withObject:event];
            }
            break;
        case UITouchPhaseMoved:
            if ([self.wDelegate
                 respondsToSelector:@selector(touchesMovedWeb:withEvent:)]) {
                [self.wDelegate
                    performSelector:@selector(touchesMovedWeb:withEvent:)
                    withObject:touches withObject:event];
            }
            break;
        case UITouchPhaseEnded:
            if ([self.wDelegate
                 respondsToSelector:@selector(touchesEndedWeb:withEvent:)]) {
                [self.wDelegate
                    performSelector:@selector(touchesEndedWeb:withEvent:)
                    withObject:touches withObject:event];
            }
        default:
            return;
            break;
    }
}

@end