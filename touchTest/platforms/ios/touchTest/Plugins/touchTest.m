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
  self.mapCtrl_ = [[GoogleMapsViewController alloc] initMyWay];
  self.mapCtrl_.webView = self.webView;
  myWindow.wDelegate = self.mapCtrl_;
  myWindow.wView = self.webView;
  
  UIImageView *testView = [[UIImageView alloc] initWithFrame:self.webView.frame];
  testView.image = [UIImage imageNamed:@"test.png"];

  self.webView.opaque = NO;
  self.webView.backgroundColor = [UIColor clearColor];
  [self.webView removeFromSuperview];
  [self.webView reload];
  
  self.mapCtrl_.view.frame = self.webView.frame;
  self.mapCtrl_.view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  
  [myWindow addSubview:self.mapCtrl_.view];
  [myWindow addSubview:self.webView];
  [self.root addSubview:myWindow];
  [myWindow makeKeyAndVisible];
  
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
@end
