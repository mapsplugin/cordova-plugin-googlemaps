//
//  PluginLocationService.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginLocationService.h"

@implementation PluginLocationService

- (void)pluginInitialize
{
    self.locationCommandQueue = [[NSMutableArray alloc] init];
    self.lastLocation = nil;
}

/**
 * Return the current position based on GPS
 */
-(void)getMyLocation:(CDVInvokedUrlCommand *)command
{
    // Obtain the authorizationStatus
    CLAuthorizationStatus status = [CLLocationManager authorizationStatus];
    if (status == kCLAuthorizationStatusDenied ||
        status == kCLAuthorizationStatusRestricted) {
        //----------------------------------------------------
        // kCLAuthorizationStatusDenied
        // kCLAuthorizationStatusRestricted
        //----------------------------------------------------
        NSString *LOCAITON_IS_UNAVAILABLE_ERROR_TITLE = PGM_LOCALIZATION(@"LOCAITON_IS_UNAVAILABLE_ERROR_TITLE", nil);
        NSString *LOCAITON_IS_UNAVAILABLE_ERROR_MESSAGE = PGM_LOCALIZATION(@"LOCAITON_IS_UNAVAILABLE_ERROR_MESSAGE", nil);
        UIAlertController* alert = [UIAlertController alertControllerWithTitle:LOCAITON_IS_UNAVAILABLE_ERROR_TITLE
                                                                       message:LOCAITON_IS_UNAVAILABLE_ERROR_MESSAGE
                                                                preferredStyle:UIAlertControllerStyleAlert];

        NSString *closeBtnLabel = PGM_LOCALIZATION(@"CLOSE_BUTTON", nil);
        UIAlertAction* ok = [UIAlertAction actionWithTitle:closeBtnLabel
                                                     style:UIAlertActionStyleDefault
                                                   handler:^(UIAlertAction* action)
            {
                NSString *error_code = @"service_denied";
                NSString *error_message = PGM_LOCALIZATION(@"LOCAITON_IS_DENIED_MESSAGE", nil);

                NSMutableDictionary *json = [NSMutableDictionary dictionary];
                [json setObject:[NSNumber numberWithBool:NO] forKey:@"status"];
                [json setObject:[NSString stringWithString:error_message] forKey:@"error_message"];
                [json setObject:[NSString stringWithString:error_code] forKey:@"error_code"];

                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:json];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                [alert dismissViewControllerAnimated:YES completion:nil];
            }];

        [alert addAction:ok];


        [self.viewController presentViewController:alert
                                          animated:YES
                                        completion:nil];

    } else {

        if (self.locationManager == nil) {
            self.locationManager = [[CLLocationManager alloc] init];
        }
        self.locationManager.delegate = self;


        //----------------------------------------------------
        // kCLAuthorizationStatusNotDetermined
        // kCLAuthorizationStatusAuthorized
        // kCLAuthorizationStatusAuthorizedAlways
        // kCLAuthorizationStatusAuthorizedWhenInUse
        //----------------------------------------------------
        CLLocationAccuracy locationAccuracy = kCLLocationAccuracyNearestTenMeters;
        NSDictionary *opts = [command.arguments objectAtIndex:0];
        BOOL isEnabledHighAccuracy = NO;
        if ([opts objectForKey:@"enableHighAccuracy"]) {
            isEnabledHighAccuracy = [[opts objectForKey:@"enableHighAccuracy"] boolValue];
        }

        if (isEnabledHighAccuracy == YES) {
            locationAccuracy = kCLLocationAccuracyBestForNavigation;
            self.locationManager.distanceFilter = 5;
        } else {
            self.locationManager.distanceFilter = 10;
        }
        self.locationManager.desiredAccuracy = locationAccuracy;

        //http://stackoverflow.com/questions/24268070/ignore-ios8-code-in-xcode-5-compilation
        [self.locationManager requestWhenInUseAuthorization];

        NSTimeInterval timeStamp = [[NSDate date] timeIntervalSince1970];

        if (self.lastLocation && timeStamp - self.lastLocation.timestamp.timeIntervalSince1970 < 2000) {
          //---------------------------------------------------------------------
          // If the user requests the location in two seconds from the last time,
          // return the last result in order to save battery usage.
          // (Don't request the device location too much! Save battery usage!)
          //---------------------------------------------------------------------
          CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:self.lastResult];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          return;
        }

        if (self.locationCommandQueue.count == 0) {
          // Executes getMyLocation() first time

          [self.locationManager stopUpdatingLocation];
          [NSTimer scheduledTimerWithTimeInterval:6000 repeats:NO block:^(NSTimer *timer) {
            if (self.lastLocation != nil) {
              return;
            }

            // Timeout
            [self.locationManager stopUpdatingLocation];

            NSMutableDictionary *json = [NSMutableDictionary dictionary];
            [json setObject:[NSNumber numberWithBool:NO] forKey:@"status"];
            NSString *error_code = @"error";
            NSString *error_message = PGM_LOCALIZATION(@"CAN_NOT_GET_LOCATION_MESSAGE", nil);
            [json setObject:[NSString stringWithString:error_message] forKey:@"error_message"];
            [json setObject:[NSString stringWithString:error_code] forKey:@"error_code"];

            for (CDVInvokedUrlCommand *command in self.locationCommandQueue) {
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:json];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
            [self.locationCommandQueue removeAllObjects];
          }];
          [self.locationManager startUpdatingLocation];
        }
        [self.locationCommandQueue addObject:command];

        //CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        //[pluginResult setKeepCallbackAsBool:YES];
        //[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

-(void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations {
    self.lastLocation = self.locationManager.location;

    NSMutableDictionary *latLng = [NSMutableDictionary dictionary];
    [latLng setObject:[NSNumber numberWithFloat:self.locationManager.location.coordinate.latitude] forKey:@"lat"];
    [latLng setObject:[NSNumber numberWithFloat:self.locationManager.location.coordinate.longitude] forKey:@"lng"];

    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    [json setObject:[NSNumber numberWithBool:YES] forKey:@"status"];

    [json setObject:latLng forKey:@"latLng"];
    [json setObject:[NSNumber numberWithFloat:[self.locationManager.location speed]] forKey:@"speed"];
    [json setObject:[NSNumber numberWithFloat:[self.locationManager.location altitude]] forKey:@"altitude"];

    //todo: calcurate the correct accuracy based on horizontalAccuracy and verticalAccuracy
    [json setObject:[NSNumber numberWithFloat:[self.locationManager.location horizontalAccuracy]] forKey:@"accuracy"];
    [json setObject:[NSNumber numberWithDouble:[self.locationManager.location.timestamp timeIntervalSince1970]] forKey:@"time"];
    [json setObject:[NSNumber numberWithInteger:[self.locationManager.location hash]] forKey:@"hashCode"];
    self.lastResult = json;

    for (CDVInvokedUrlCommand *command in self.locationCommandQueue) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

    [self.locationCommandQueue removeAllObjects];
    [self.locationManager stopUpdatingLocation];
    //self.locationManager.delegate = nil;
    //self.locationManager = nil;
}
- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
    self.lastLocation = nil;
    self.lastResult = nil;

    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    [json setObject:[NSNumber numberWithBool:NO] forKey:@"status"];
    NSString *error_code = @"error";
    NSString *error_message = PGM_LOCALIZATION(@"CAN_NOT_GET_LOCATION_MESSAGE", nil);
    if (error.code == kCLErrorDenied) {
        error_code = @"service_denied";
        error_message = PGM_LOCALIZATION(@"LOCATION_REJECTED_BY_USER_MESSAGE", nil);
    }

    [json setObject:[NSString stringWithString:error_message] forKey:@"error_message"];
    [json setObject:[NSString stringWithString:error_code] forKey:@"error_code"];

    for (CDVInvokedUrlCommand *command in self.locationCommandQueue) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:json];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.locationCommandQueue removeAllObjects];

}

@end
