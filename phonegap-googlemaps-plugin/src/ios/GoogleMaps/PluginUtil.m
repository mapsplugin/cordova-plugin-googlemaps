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

// Generate a new image with the specified alpha from a uiImage
// http://stackoverflow.com/questions/5084845/how-to-set-the-opacity-alpha-of-a-uiimage#10819117
@implementation UIImage (GoogleMapsPlugin)
- (UIImage *)imageByApplyingAlpha:(CGFloat) alpha {
    UIGraphicsBeginImageContextWithOptions(self.size, NO, 0.0f);

    CGContextRef ctx = UIGraphicsGetCurrentContext();
    CGRect area = CGRectMake(0, 0, self.size.width, self.size.height);

    CGContextScaleCTM(ctx, 1, -1);
    CGContextTranslateCTM(ctx, 0, -area.size.height);

    CGContextSetBlendMode(ctx, kCGBlendModeMultiply);

    CGContextSetAlpha(ctx, alpha);

    CGContextDrawImage(ctx, area, self.CGImage);

    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();

    UIGraphicsEndImageContext();

    return newImage;
}
@end

@implementation PluginUtil
@end

