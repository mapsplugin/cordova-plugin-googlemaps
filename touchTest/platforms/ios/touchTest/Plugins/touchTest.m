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
  
  self.root = self.webView.superview;
  
  MyWindow *myWindow = [[MyWindow alloc] initWithFrame:self.webView.frame];
  myWindow.wDelegate = [[UIApplication sharedApplication] delegate];
  
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
  myWindow.wView = self.webView;
  
  //CDVViewController *cdvViewController =  (CDVViewController *)self.viewController;
  //NSLog(@"hogehoge = %@", [cdvViewController.settings objectForKey:@"hogehoge"]);
  
  
  //UITapGestureRecognizer *tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self.webView action:@selector(detectedTapGesture:)];
  //UIWindow *window = [self getWindow];
  //[self.webView addGestureRecognizer:tapGestureRecognizer];
  
}
- (void)exec:(CDVInvokedUrlCommand *)command {
  self.webView.opaque = NO;
  self.webView.backgroundColor = [UIColor clearColor];
  
  NSLog(@"%@", self.webView.superview);
  NSLog(@"%@", self.webView.superview.superview);
  
  
}

-(UIWindow*)getWindow{
    UIWindow* window=[UIApplication sharedApplication].keyWindow;
    if(!window)window=[[UIApplication sharedApplication].windows objectAtIndex:0];
    return window;
}
/*
- (void)detectedTapGesture:(UITapGestureRecognizer *)sender {
 
    //UIWindow *window = [[[UIApplication sharedApplication] delegate] window];
    CGPoint point = [sender locationOfTouch:0 inView:self.webView];
    NSLog(@"tap point: %@", NSStringFromCGPoint(point));
 
}
*/

@end
