//
//  UIView+ColorOfPoint.m
//
//  Created by Ivan Zezyulya on 12.01.11.
//

#import "UIView+ColorOfPoint.h"
#import <QuartzCore/QuartzCore.h>

@implementation UIView (ColorOfPoint)

- (unsigned char) colorAlphaPoint:(CGPoint)point
{
    unsigned char pixel[4] = {0};
    
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    
    CGContextRef context = CGBitmapContextCreate(pixel, 1, 1, 8, 4, colorSpace, kCGBitmapAlphaInfoMask & kCGImageAlphaPremultipliedLast);
    
    CGContextTranslateCTM(context, -point.x, -point.y);
    
    [self.layer renderInContext:context];
    
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    
    return pixel[3];
}

@end
