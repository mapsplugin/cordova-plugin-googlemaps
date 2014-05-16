//
//  PluginUtil.m
//  SimpleMap
//
//  Created by masashi on 11/15/13.
//
//

#import "PluginUtil.h"
@implementation UIView (GoogleMapsPlugin)
- (void)setFrameWithDictionary:(NSDictionary *)params
{
  float left = [[params objectForKey:@"left"] floatValue];
  float top = [[params objectForKey:@"top"] floatValue];
  float width = [[params objectForKey:@"width"] floatValue];
  float height = [[params objectForKey:@"height"] floatValue];

  CGRect r = [self frame];
  if (r.origin.x == left &&
      r.origin.y == top &&
      r.size.width == width &&
      r.size.height == height) {
    return;
  }
  
  r.origin.x = left;
  r.origin.y = top;
  r.size.width = width;
  r.size.height = height;
  self.frame = r;
}


- (void)setFrameWithInt:(int)left top:(int)top width:(int)width height:(int)height
{
  CGRect r = [self frame];
  if (r.origin.x == left &&
      r.origin.y == top &&
      r.size.width == width &&
      r.size.height == height) {
    return;
  }
  r.origin.x = left;
  r.origin.y = top;
  r.size.width = width;
  r.size.height = height;
  self.frame = r;
}

@end

@implementation NSArray (GoogleMapsPlugin)
- (UIColor*)parsePluginColor
{
  return [UIColor colorWithRed:[[self objectAtIndex:0] floatValue]/255.0
                              green:[[self objectAtIndex:1] floatValue]/255.0
                              blue:[[self objectAtIndex:2] floatValue]/255.0
                              alpha:[[self objectAtIndex:3] floatValue]/255.0];
  
}
@end

@implementation NSString (GoogleMapsPlugin)
- (NSString*)regReplace:(NSString*)pattern replaceTxt:(NSString*)replaceTxt options:(NSRegularExpressionOptions)options
{
  NSError *error = nil;
  NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:pattern options:options error:&error];
  return [regex stringByReplacingMatchesInString:self options:0 range:NSMakeRange(0, [self length]) withTemplate:replaceTxt];
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

-(UIImage *)resize:(CGFloat)width height:(CGFloat)height {
  if (width > 0 && height > 0) {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 0.0);
    [self drawInRect:CGRectMake(0, 0, width, height)];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
  }
  return self;
}


@end


@implementation PluginUtil
+ (BOOL)isIOS7
{
    NSArray  *aOsVersions = [[[UIDevice currentDevice]systemVersion] componentsSeparatedByString:@"."];
    NSInteger iOsVersionMajor  = [[aOsVersions objectAtIndex:0] intValue];
    if (iOsVersionMajor == 7)
    {
        return YES;
    }

    return NO;
}
@end


