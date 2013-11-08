//
//  Circle.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "Circle.h"

@implementation Circle

-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
  self.mapCtrl = viewCtrl;
}
-(void)createCircle:(CDVInvokedUrlCommand *)command
{
  NSDictionary *json = [command.arguments objectAtIndex:1];
  float latitude = [[json valueForKey:@"lat"] floatValue];
  float longitude = [[json valueForKey:@"lng"] floatValue];
  float radius = [[json valueForKey:@"radius"] floatValue];
  CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
  GMSCircle *circle = [GMSCircle circleWithPosition:position radius:radius];
  
  if ([[json valueForKey:@"visible"] boolValue]) {
    circle.map = self.mapCtrl.map;
  }
  NSScanner* pScanner = [NSScanner scannerWithString: [json objectForKey:@"fillColor"]];
  unsigned int fillColor;
  [pScanner scanHexInt: &fillColor];
  circle.fillColor = UIColorFromRGB(fillColor);
  
  /*
   circleOptions.strokeColor = circleOptions.strokeColor || "#FF000000";
   circleOptions.fillColor = circleOptions.fillColor || "#00000000";
   circleOptions.strokeWidth = circleOptions.strokeWidth || 10;
   circleOptions.visible = circleOptions.visible || true;
   circleOptions.zIndex = circleOptions.zIndex || 0.0;
   */
  
  NSString *key = [NSString stringWithFormat:@"circle%d", circle.hash];
  [self.mapCtrl.circleManager setObject:circle forKey: key];
  
  
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: key];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
@end
