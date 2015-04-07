/*
The MIT License (MIT)

Copyright (c) 2014

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
#import "YoikScreenOrientation.h"

@implementation YoikScreenOrientation

-(void)screenOrientation:(CDVInvokedUrlCommand *)command
{
    NSArray* arguments = command.arguments;
    NSString* orientationIn = [arguments objectAtIndex:1];

    // grab the device orientation so we can pass it back to the js side.
    NSString *orientation;
    switch ([[UIDevice currentDevice] orientation]) {
        case UIDeviceOrientationLandscapeLeft:
            orientation = @"landscape-secondary";
            break;
        case UIDeviceOrientationLandscapeRight:
            orientation = @"landscape-primary";
            break;
        case UIDeviceOrientationPortrait:
            orientation = @"portrait-primary";
            break;
        case UIDeviceOrientationPortraitUpsideDown:
            orientation = @"portrait-secondary";
            break;
        default:
            orientation = @"portait";
            break;
    }

    if ([orientationIn isEqual: @"unlocked"]) {
        orientationIn = orientation;
    }

    // we send the result prior to the view controller presentation so that the JS side
    // is ready for the unlock call.
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
        messageAsDictionary:@{@"device":orientation}];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    // SEE https://github.com/Adlotto/cordova-plugin-recheck-screen-orientation
    // HACK: Force rotate by changing the view hierarchy. Present modal view then dismiss it immediately
    // This has been changed substantially since iOS8 broke it...
    ForcedViewController *vc = [[ForcedViewController alloc] init];
    vc.calledWith = orientationIn;

    // backgound should be transparent as it is briefly visible
    // prior to closing.
    vc.view.backgroundColor = [UIColor clearColor];
    // vc.view.alpha = 0.0;
    vc.view.opaque = YES;

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 80000
    // This stops us getting the black application background flash, iOS8
    vc.modalPresentationStyle = UIModalPresentationOverFullScreen;
#endif

    [self.viewController presentViewController:vc animated:NO completion:^{
        // added to support iOS8 beta 5, @see issue #19
        dispatch_after(0, dispatch_get_main_queue(), ^{
            [self.viewController dismissViewControllerAnimated:NO completion:nil];
        });
    }];
}

@end

@implementation ForcedViewController

- (NSUInteger) supportedInterfaceOrientations
{
    if ([self.calledWith rangeOfString:@"portrait"].location != NSNotFound) {
        return UIInterfaceOrientationMaskPortrait;
    } else if([self.calledWith rangeOfString:@"landscape"].location != NSNotFound) {
        return UIInterfaceOrientationMaskLandscape;
    }
    return UIInterfaceOrientationMaskAll;
}
@end