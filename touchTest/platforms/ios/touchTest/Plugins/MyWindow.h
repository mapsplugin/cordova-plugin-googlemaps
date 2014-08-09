//
//  MyWindow.h
//  touchTest
//
//  Created by Katsumata Masashi on 8/8/14.
//
//

#import <UIKit/UIKit.h>

@protocol MyWindowDelegate

- (void) touchesBeganWeb:(NSSet *)touches withEvent:(UIEvent *)event;
- (void) touchesMovedWeb:(NSSet *)touches withEvent:(UIEvent *)event;
- (void) touchesEndedWeb:(NSSet *)touches withEvent:(UIEvent *)event;

@end

@interface MyWindow : UIWindow {
    UIWebView* wView;
    id wDelegate;
}

@property (nonatomic, retain) UIWebView* wView;
@property (nonatomic) id wDelegate;

@end
