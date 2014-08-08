//
//  TapDetectingWindow.h
//  touchTest
//
//  Created by masashi on 8/7/14.
//
//

#import <UIKit/UIKit.h>
@protocol TapDetectingWindowDelegate
- (void)userDidTapWebView:(id)tapPoint;
@end
@interface TapDetectingWindow : UIWindow {
    UIView *viewToObserve;
    id <TapDetectingWindowDelegate> controllerThatObserves;
}
@property (nonatomic, retain) UIView *viewToObserve;
@property (nonatomic) id <TapDetectingWindowDelegate> controllerThatObserves;
@end