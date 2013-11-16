//
//  PluginUtil.m
//  SimpleMap
//
//  Created by masashi on 11/15/13.
//
//

#import "PluginUtil.h"

@implementation NSArray (GoogleMapsPlugin)
- (UIColor*)parsePluginColor
{
  return [UIColor colorWithRed:[[self objectAtIndex:0] floatValue]/255.0
                              green:[[self objectAtIndex:1] floatValue]/255.0
                              blue:[[self objectAtIndex:2] floatValue]/255.0
                              alpha:[[self objectAtIndex:3] floatValue]/255.0];
  
}
@end


@implementation PluginUtil
@end

