//
//  touchTest.m
//  touchTest
//
//  Created by masashi on 8/6/14.
//
//

#import "touchTest.h"

@implementation touchTest

- (void)pluginInitialize {
  
  MyWindow *myWindow = [[MyWindow alloc] initWithFrame:self.webView.frame];
  
  self.root = self.webView.superview;
  
  UIImageView *testView = [[UIImageView alloc] initWithFrame:self.webView.frame];
  testView.image = [UIImage imageNamed:@"test.png"];

  self.webView.opaque = NO;
  self.webView.backgroundColor = [UIColor clearColor];
  [self.webView removeFromSuperview];
  [self.webView reload];
  
  [myWindow addSubview:testView];
  [myWindow addSubview:self.webView];
  [self.root addSubview:myWindow];
  [myWindow makeKeyAndVisible];
  
  
  UISwipeGestureRecognizer *swipeRight = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(swipeRightAction:)];
  swipeRight.direction = UISwipeGestureRecognizerDirectionDown;
  swipeRight.delegate = self;
  swipeRight.numberOfTouchesRequired = 1;
  swipeRight.delaysTouchesBegan = YES;
  swipeRight.delaysTouchesEnded = YES;
  [self.webView addGestureRecognizer:swipeRight];
  
}
- (void)exec:(CDVInvokedUrlCommand *)command {
  
  //NSLog(@"%f,%f", self.mapCtrl_.view.frame.size.width, self.mapCtrl_.view.frame.size.height);
  self.webView.opaque = NO;
  self.webView.backgroundColor = [UIColor clearColor];
}

-(UIWindow*)getWindow{
    UIWindow* window=[UIApplication sharedApplication].keyWindow;
    if(!window)window=[[UIApplication sharedApplication].windows objectAtIndex:0];
    return window;
}

- (void)swipeRightAction:(UISwipeGestureRecognizer *)sender
{
  CGPoint location = [sender locationInView:self.webView];
  NSLog(@"%f, %f", location.x, location.y);
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
  otherGestureRecognizer.enabled = NO;
    return YES;
}
@end
