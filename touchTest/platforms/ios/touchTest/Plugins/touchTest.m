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
  
  CGRect rect = self.webView.frame;
  rect.size.width = 300;
  rect.size.height = 300;
  //self.webView.frame = rect;
  
  self.root = self.webView.superview;
  
  UIImageView *testView = [[UIImageView alloc] initWithFrame:self.webView.frame];
  testView.image = [UIImage imageNamed:@"test.png"];
  
  self.webView.opaque = NO;
  self.webView.backgroundColor = [UIColor clearColor];
  
  [self.webView removeFromSuperview];
  [self.webView reload];
  [self.root addSubview:testView];
  [self.root addSubview:self.webView];
  
  CDVViewController *cdvViewController =  (CDVViewController *)self.viewController;
  NSLog(@"hogehoge = %@", [cdvViewController.settings objectForKey:@"hogehoge"]);
  
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


@end
