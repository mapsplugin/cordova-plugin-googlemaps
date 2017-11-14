//
//  CordovaGoogleMaps.m
//  cordova-googlemaps-plugin v2
//
//  Created by Masashi Katsumata.
//
//

#import "PluginGeolocation.h"

@implementation PluginGeolocation

- (void)pluginInitialize
{
    self.locationCommandQueue = [[NSMutableArray alloc] init];

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
        UIAlertView *alertView = [[UIAlertView alloc]
                                  initWithTitle:@"Location Services disabled"
                                  message:@"This app needs access to your location. Please turn on Location Services in your device settings."
                                  delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
        [alertView show];

        NSString *error_code = @"service_denied";
        NSString *error_message = @"This app has rejected to use Location Services.";

        NSMutableDictionary *json = [NSMutableDictionary dictionary];
        [json setObject:[NSNumber numberWithBool:NO] forKey:@"status"];
        [json setObject:[NSString stringWithString:error_message] forKey:@"error_message"];
        [json setObject:[NSString stringWithString:error_code] forKey:@"error_code"];

        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:json];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
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

        [self.locationManager stopUpdatingLocation];
        [self.locationManager startUpdatingLocation];
        [self.locationCommandQueue addObject:command];

        //CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        //[pluginResult setKeepCallbackAsBool:YES];
        //[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

-(void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations {
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
    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    [json setObject:[NSNumber numberWithBool:NO] forKey:@"status"];
    NSString *error_code = @"error";
    NSString *error_message = @"Cannot get your location.";
    if (error.code == kCLErrorDenied) {
        error_code = @"service_denied";
        error_message = @"This app has rejected to use Location Services.";
    }

    [json setObject:[NSString stringWithString:error_message] forKey:@"error_message"];
    [json setObject:[NSString stringWithString:error_code] forKey:@"error_code"];

    for (CDVInvokedUrlCommand *command in self.locationCommandQueue) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:json];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

@end
