//
//  PluginStreetViewPanoramaController.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginStreetViewPanoramaController.h"
#if CORDOVA_VERSION_MIN_REQUIRED < __CORDOVA_4_0_0
#import <Cordova/CDVJSON.h>
#endif


@implementation PluginViewController

- (id)initWithOptions:(NSDictionary *) options {
  self = [super init];
  self.plugins = [NSMutableDictionary dictionary];
  self.isFullScreen = NO;
  self.screenSize = [[UIScreen mainScreen] bounds];
  self.screenScale = [[UIScreen mainScreen] scale];
  self.clickable = YES;
  self.isRenderedAtOnce = NO;
  self.divId = nil;
  self.objects = [[PluginObjects alloc] init];
  self.executeQueue =  [NSOperationQueue new];
  self.executeQueue.maxConcurrentOperationCount = 10;

  return self;
}

- (void)viewDidLoad
{
  [super viewDidLoad];

}


- (void)didReceiveMemoryWarning
{
  [super didReceiveMemoryWarning];
}


- (void)execJS: (NSString *)jsString {
  // Insert setTimeout() in order to prevent the GDC and webView deadlock
  // ( you can not click the ok button of Alert() )
  // https://stackoverflow.com/a/23833841/697856
  jsString = [NSString stringWithFormat:@"setTimeout(function(){%@}, 0);", jsString];

  if ([self.webView respondsToSelector:@selector(stringByEvaluatingJavaScriptFromString:)]) {
    [self.webView performSelector:@selector(stringByEvaluatingJavaScriptFromString:) withObject:jsString];
  } else if ([self.webView respondsToSelector:@selector(evaluateJavaScript:completionHandler:)]) {
    [self.webView performSelector:@selector(evaluateJavaScript:completionHandler:) withObject:jsString withObject:nil];
  }
}

@end
