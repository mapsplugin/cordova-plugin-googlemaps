//
//  Marker.m
//  SimpleMap
//
//  Created by masashi on 11/8/13.
//
//

#import "Marker.h"
@implementation Marker
-(void)setGoogleMapsViewController:(GoogleMapsViewController *)viewCtrl
{
    self.mapCtrl = viewCtrl;
}

/**
 * @param marker options
 * @return marker key
 */
-(void)createMarker:(CDVInvokedUrlCommand *)command
{
    NSDictionary *json = [command.arguments objectAtIndex:1];
    NSDictionary *latLng = [json objectForKey:@"position"];
    float latitude = [[latLng valueForKey:@"lat"] floatValue];
    float longitude = [[latLng valueForKey:@"lng"] floatValue];

    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
    GMSMarker *marker = [GMSMarker markerWithPosition:position];


    if (![self.mapCtrl isKindOfClass:[GoogleMapsClusterViewController class]]) {
        if ([[json valueForKey:@"visible"] boolValue] == true) {
            marker.map = self.mapCtrl.map;
        }
    }

    /*
     if ([[json valueForKey:@"visible"] boolValue] == true) {
     marker.map = self.mapCtrl.map;
     }
     */

    if ([json valueForKey:@"title"]) {
        [marker setTitle: [json valueForKey:@"title"]];
    }
    if ([json valueForKey:@"snippet"]) {
        [marker setSnippet: [json valueForKey:@"snippet"]];
    }
    if ([json valueForKey:@"draggable"]) {
        [marker setDraggable:[[json valueForKey:@"draggable"] boolValue]];
    }
    if ([json valueForKey:@"flat"]) {
        [marker setFlat:[[json valueForKey:@"flat"] boolValue]];
    }
    if ([json valueForKey:@"rotation"]) {
        CLLocationDegrees degrees = [[json valueForKey:@"rotation"] doubleValue];
        [marker setRotation:degrees];
    }
    if ([json valueForKey:@"opacity"]) {
        [marker setOpacity:[[json valueForKey:@"opacity"] floatValue]];
    }
    if ([json valueForKey:@"zIndex"]) {
        [marker setZIndex:[[json valueForKey:@"zIndex"] intValue]];
    }

    NSString *id = [NSString stringWithFormat:@"marker_%lu", (unsigned long)marker.hash];
    [self.mapCtrl.overlayManager setObject:marker forKey: id];

    // Custom properties
    NSMutableDictionary *properties = [[NSMutableDictionary alloc] init];
    NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];

    if ([json valueForKey:@"styles"]) {
        NSDictionary *styles = [json valueForKey:@"styles"];
        [properties setObject:styles forKey:@"styles"];
    }

    BOOL disableAutoPan = NO;
    if ([json valueForKey:@"disableAutoPan"] != nil) {
        disableAutoPan = [[json valueForKey:@"disableAutoPan"] boolValue];
    }
    [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
    [self.mapCtrl.overlayManager setObject:properties forKey: markerPropertyId];


    // Create icon
    NSMutableDictionary *iconProperty = nil;
    NSObject *icon = [json valueForKey:@"icon"];
    if ([icon isKindOfClass:[NSString class]]) {
        iconProperty = [NSMutableDictionary dictionary];
        [iconProperty setObject:icon forKey:@"url"];

    } else if ([icon isKindOfClass:[NSDictionary class]]) {
        iconProperty = [json valueForKey:@"icon"];

    } else if ([icon isKindOfClass:[NSArray class]]) {
        NSArray *rgbColor = [json valueForKey:@"icon"];
        iconProperty = [NSMutableDictionary dictionary];
        [iconProperty setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
    }

    // Animation
    NSString *animation = nil;
    if ([json valueForKey:@"animation"]) {
        animation = [json valueForKey:@"animation"];
        if (iconProperty) {
            [iconProperty setObject:animation forKey:@"animation"];
        }
    }

    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
    [result setObject:id forKey:@"id"];
    [result setObject:[NSString stringWithFormat:@"%lu", (unsigned long)marker.hash] forKey:@"hashCode"];

    CDVPluginResult* pluginResult = nil;
    if (iconProperty) {
        if ([json valueForKey:@"infoWindowAnchor"]) {
            [iconProperty setObject:[json valueForKey:@"infoWindowAnchor"] forKey:@"infoWindowAnchor"];
        }

        /*
         // Send an temporally signal at once
         pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
         [pluginResult setKeepCallbackAsBool:YES];
         [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
         */

        // Load icon in asynchronise
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        [self setIcon_:marker iconProperty:iconProperty pluginResult:pluginResult callbackId:command.callbackId];

    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        if (animation) {
            [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:command.callbackId];
        } else {
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }

    self.mapCtrl.dataChanged++;
}

/**
 * Show the infowindow of the current marker
 * @params MarkerKey
 */
-(void)showInfoWindow:(CDVInvokedUrlCommand *)command
{

    NSString *hashCode = [command.arguments objectAtIndex:1];

    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:hashCode];
    if (marker) {
        self.mapCtrl.map.selectedMarker = marker;
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}
/**
 * Hide current infowindow
 * @params MarkerKey
 */
-(void)hideInfoWindow:(CDVInvokedUrlCommand *)command
{
    self.mapCtrl.map.selectedMarker = nil;
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * @params MarkerKey
 * @return current marker position with array(latitude, longitude)
 */
-(void)getPosition:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];

    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    NSNumber *latitude = @0.0;
    NSNumber *longitude = @0.0;
    if (marker) {
        latitude = [NSNumber numberWithFloat: marker.position.latitude];
        longitude = [NSNumber numberWithFloat: marker.position.longitude];
    }
    NSMutableDictionary *json = [NSMutableDictionary dictionary];
    [json setObject:latitude forKey:@"lat"];
    [json setObject:longitude forKey:@"lng"];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:json];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * @params MarkerKey
 * @return boolean
 */
-(void)isInfoWindowShown:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    Boolean isOpen = false;
    if (self.mapCtrl.map.selectedMarker == marker) {
        isOpen = YES;
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isOpen];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)setTitle:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    marker.title = [command.arguments objectAtIndex:2];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set title to the specified marker
 * @params MarkerKey
 */
-(void)setSnippet:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    marker.snippet = [command.arguments objectAtIndex:2];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Remove the specified marker
 * @params MarkerKey
 */
-(void)remove:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    NSString *propertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
    marker.map = nil;
    [self.mapCtrl removeObjectForKey:markerKey];
    marker = nil;

    if ([self.mapCtrl.overlayManager objectForKey:propertyId]) {
        [self.mapCtrl removeObjectForKey:propertyId];
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * Set anchor of the marker
 * @params MarkerKey
 */
-(void)setIconAnchor:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    CGFloat anchorX = [[command.arguments objectAtIndex:2] floatValue];
    CGFloat anchorY = [[command.arguments objectAtIndex:3] floatValue];

    if (marker.icon) {
        anchorX = anchorX / marker.icon.size.width;
        anchorY = anchorY / marker.icon.size.height;
        [marker setGroundAnchor:CGPointMake(anchorX, anchorY)];
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set anchor of the info window
 * @params MarkerKey
 */
-(void)setInfoWindowAnchor:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    float anchorX = [[command.arguments objectAtIndex:2] floatValue];
    float anchorY = [[command.arguments objectAtIndex:3] floatValue];

    if (marker.icon) {
        anchorX = anchorX / marker.icon.size.width;
        anchorY = anchorY / marker.icon.size.height;
        [marker setGroundAnchor:CGPointMake(anchorX, anchorY)];
    }
    [marker setInfoWindowAnchor:CGPointMake(anchorX, anchorY)];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Set opacity
 * @params MarkerKey
 */
-(void)setOpacity:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    marker.opacity = [[command.arguments objectAtIndex:2] floatValue];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set zIndex
 * @params MarkerKey
 */
-(void)setZIndex:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    marker.zIndex = [[command.arguments objectAtIndex:2] intValue];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set draggable
 * @params MarkerKey
 */
-(void)setDraggable:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    Boolean isEnabled = [[command.arguments objectAtIndex:2] boolValue];
    [marker setDraggable:isEnabled];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set disable auto pan
 * @params MarkerKey
 */
-(void)setDisableAutoPan:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    BOOL disableAutoPan = [[command.arguments objectAtIndex:2] boolValue];

    NSString *markerPropertyId = [NSString stringWithFormat:@"marker_property_%lu", (unsigned long)marker.hash];
    NSMutableDictionary *properties = [NSMutableDictionary dictionaryWithDictionary:
                                       [self.mapCtrl.overlayManager objectForKey:markerPropertyId]];
    [properties setObject:[NSNumber numberWithBool:disableAutoPan] forKey:@"disableAutoPan"];
    [self.mapCtrl.overlayManager setObject:properties forKey:markerPropertyId];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set visibility
 * @params MarkerKey
 */
-(void)setVisible:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    Boolean isVisible = [[command.arguments objectAtIndex:2] boolValue];

    if (isVisible) {
        marker.map = self.mapCtrl.map;
    } else {
        marker.map = nil;
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set position
 * @params key
 */
-(void)setPosition:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl getMarkerByKey: markerKey];

    float latitude = [[command.arguments objectAtIndex:2] floatValue];
    float longitude = [[command.arguments objectAtIndex:3] floatValue];
    CLLocationCoordinate2D position = CLLocationCoordinate2DMake(latitude, longitude);
    [marker setPosition:position];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Set flattable
 * @params MarkerKey
 */
-(void)setFlat:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];
    Boolean isFlat = [[command.arguments objectAtIndex:2] boolValue];
    [marker setFlat: isFlat];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * set icon
 * @params MarkerKey
 */
-(void)setIcon:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];

    // Create icon
    NSDictionary *iconProperty;
    id icon = [command.arguments objectAtIndex:2];
    if ([icon isKindOfClass:[NSString class]]) {
        NSMutableDictionary *iconDic = [[NSMutableDictionary alloc] init];
        [iconDic setObject:icon forKey:@"url"];
        iconProperty = iconDic;
    } else if ([icon isKindOfClass:[NSDictionary class]]) {
        iconProperty = [command.arguments objectAtIndex:2];
    } else if ([icon isKindOfClass:[NSArray class]]) {
        NSArray *rgbColor = icon;
        NSMutableDictionary *iconDic = [[NSMutableDictionary alloc] init];
        [iconDic setObject:[rgbColor parsePluginColor] forKey:@"iconColor"];
    }

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self setIcon_:marker iconProperty:iconProperty pluginResult:pluginResult callbackId:command.callbackId];
}
/**
 * set rotation
 */
-(void)setRotation:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];

    CLLocationDegrees degrees = [[command.arguments objectAtIndex:2] doubleValue];
    [marker setRotation:degrees];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


-(void)setAnimation:(CDVInvokedUrlCommand *)command
{
    NSString *markerKey = [command.arguments objectAtIndex:1];
    GMSMarker *marker = [self.mapCtrl.overlayManager objectForKey:markerKey];

    NSString *animation = [command.arguments objectAtIndex:2];

    CDVPluginResult* successResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self setMarkerAnimation_:animation marker:marker pluginResult:successResult callbackId:command.callbackId];
}

-(void)setMarkerAnimation_:(NSString *)animation marker:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId {

    animation = [animation uppercaseString];
    SWITCH(animation) {
        CASE (@"DROP") {
            [self setDropAnimation_:marker pluginResult:pluginResult callbackId:callbackId];
            break;
        }
        CASE (@"BOUNCE") {
            [self setBounceAnimation_:marker pluginResult:pluginResult callbackId:callbackId];
            break;
        }
        DEFAULT {
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
            break;
        }
    }
}

/**
 * set animation
 * (memo) http://stackoverflow.com/a/19316475/697856
 * (memo) http://qiita.com/edo_m18/items/4309d01b67ee42c35b3c
 * (memo) http://stackoverflow.com/questions/12164049/animationdidstop-for-group-animation
 */
-(void)setDropAnimation_:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId {
    int duration = 1;

    CAKeyframeAnimation *longitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"longitude"];
    CAKeyframeAnimation *latitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"latitude"];

    GMSProjection *projection = self.mapCtrl.map.projection;
    CGPoint point = [projection pointForCoordinate:marker.position];
    double distance = point.y ;

    NSMutableArray *latitudePath = [NSMutableArray array];
    NSMutableArray *longitudeath = [NSMutableArray array];
    CLLocationCoordinate2D startLatLng;

    point.y = 0;
    for (double i = 0.75f; i > 0; i-= 0.25f) {
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance;
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance - distance * (i - 0.25f);
    }
    longitudeAnim.values = longitudeath;
    latitudeAnim.values = latitudePath;

    CAAnimationGroup *group = [[CAAnimationGroup alloc] init];
    group.animations = @[longitudeAnim, latitudeAnim];
    group.duration = duration;
    [group setCompletionBlock:^(void){
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }];

    [marker.layer addAnimation:group forKey:@"dropMarkerAnim"];

}
-(void)setBounceAnimation_:(GMSMarker *)marker pluginResult:(CDVPluginResult *)pluginResult callbackId:(NSString*)callbackId
{
    /**
     * Marker drop animation
     */
    int duration = 1;

    CAKeyframeAnimation *longitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"longitude"];
    CAKeyframeAnimation *latitudeAnim = [CAKeyframeAnimation animationWithKeyPath:@"latitude"];

    GMSProjection *projection = self.mapCtrl.map.projection;
    CGPoint point = [projection pointForCoordinate:marker.position];
    double distance = point.y;

    NSMutableArray *latitudePath = [NSMutableArray array];
    NSMutableArray *longitudeath = [NSMutableArray array];
    CLLocationCoordinate2D startLatLng;

    point.y = distance * 0.5f;

    for (double i = 0.5f; i > 0; i-= 0.15f) {
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance;
        startLatLng = [projection coordinateForPoint:point];
        [latitudePath addObject:[NSNumber numberWithDouble:startLatLng.latitude]];
        [longitudeath addObject:[NSNumber numberWithDouble:startLatLng.longitude]];

        point.y = distance - distance * (i - 0.15f);
    }
    longitudeAnim.values = longitudeath;
    latitudeAnim.values = latitudePath;

    CAAnimationGroup *group = [[CAAnimationGroup alloc] init];
    group.animations = @[longitudeAnim, latitudeAnim];
    group.duration = duration;
    [group setCompletionBlock:^(void){
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }];

    [marker.layer addAnimation:group forKey:@"bounceMarkerAnim"];
}

/**
 * @private
 * Load the icon; then set to the marker
 */
-(void)setIcon_:(GMSMarker *)marker iconProperty:(NSDictionary *)iconProperty
   pluginResult:(CDVPluginResult *)pluginResult
     callbackId:(NSString*)callbackId {

    if (self.mapCtrl.debuggable) {
        NSLog(@"---- setIcon_");
    }
    NSString *iconPath = nil;
    CGFloat width = 0;
    CGFloat height = 0;
    CGFloat anchorX = 0;
    CGFloat anchorY = 0;

    // `url` property
    iconPath = [iconProperty valueForKey:@"url"];

    // `size` property
    if ([iconProperty valueForKey:@"size"]) {
        NSDictionary *size = [iconProperty valueForKey:@"size"];
        width = [[size objectForKey:@"width"] floatValue];
        height = [[size objectForKey:@"height"] floatValue];
    }

    // `animation` property
    NSString *animationValue = nil;
    if ([iconProperty valueForKey:@"animation"]) {
        animationValue = [iconProperty valueForKey:@"animation"];
    }
    __block NSString *animation = animationValue;

    if (iconPath) {
        NSError *error;
        NSRange range = [iconPath rangeOfString:@"://"];
        if (range.location == NSNotFound) {
            range = [iconPath rangeOfString:@"www/"];
            if (range.location == NSNotFound) {
                range = [iconPath rangeOfString:@"/"];
                if (range.location != 0) {
                    iconPath = [NSString stringWithFormat:@"./%@", iconPath];
                }
            }
        }

        range = [iconPath rangeOfString:@"./"];
        if (range.location != NSNotFound) {
            NSString *currentPath = [self.webView.request.URL absoluteString];
            NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"[^\\/]*$" options:NSRegularExpressionCaseInsensitive error:&error];
            currentPath= [regex stringByReplacingMatchesInString:currentPath options:0 range:NSMakeRange(0, [currentPath length]) withTemplate:@""];
            iconPath = [iconPath stringByReplacingOccurrencesOfString:@"./" withString:currentPath];
        }

        if (self.mapCtrl.debuggable) {
            NSLog(@"iconPath = %@", iconPath);
        }

        range = [iconPath rangeOfString:@"http"];
        if (range.location != 0) {
            /**
             * Load icon from file or Base64 encoded strings
             */
            Boolean isTextMode = true;

            UIImage *image;
            if ([iconPath rangeOfString:@"data:image/"].location != NSNotFound &&
                [iconPath rangeOfString:@";base64,"].location != NSNotFound) {

                /**
                 * Base64 icon
                 */
                isTextMode = false;
                NSArray *tmp = [iconPath componentsSeparatedByString:@","];

                NSData *decodedData;
#if !defined(__IPHONE_8_0)
                if ([PluginUtil isIOS7_OR_OVER]) {
                    decodedData = [NSData dataFromBase64String:tmp[1]];
                } else {
#if !defined(__IPHONE_7_0)
                    decodedData = [[NSData alloc] initWithBase64Encoding:(NSString *)tmp[1]];
#endif
                }
#else
                decodedData = [NSData dataFromBase64String:tmp[1]];
#endif
                image = [[UIImage alloc] initWithData:decodedData];
                if (width && height) {
                    image = [image resize:width height:height];
                }

                // The `anchor` property for the icon
                if ([iconProperty valueForKey:@"anchor"]) {
                    NSArray *points = [iconProperty valueForKey:@"anchor"];
                    anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                    anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                    marker.groundAnchor = CGPointMake(anchorX, anchorY);
                }

            } else {
                /**
                 * Load the icon from local path
                 */

                range = [iconPath rangeOfString:@"cdvfile://"];
                if (range.location != NSNotFound) {

                    iconPath = [PluginUtil getAbsolutePathFromCDVFilePath:self.webView cdvFilePath:iconPath];

                    if (iconPath == nil) {
                        if (self.mapCtrl.debuggable) {
                            NSLog(@"(debug)Can not convert '%@' to device full path.", iconPath);
                        }
                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        return;
                    }
                }

                range = [iconPath rangeOfString:@"file://"];
                if (range.location != NSNotFound) {
                    iconPath = [iconPath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
                    NSFileManager *fileManager = [NSFileManager defaultManager];
                    if (![fileManager fileExistsAtPath:iconPath]) {
                        if (self.mapCtrl.debuggable) {
                            NSLog(@"(debug)There is no file at '%@'.", iconPath);
                        }
                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        return;
                    }
                }

                image = [UIImage imageNamed:iconPath];

                if (width && height) {
                    image = [image resize:width height:height];
                }
            }

            marker.icon = image;

            // The `anchor` property for the icon
            if ([iconProperty valueForKey:@"anchor"]) {
                NSArray *points = [iconProperty valueForKey:@"anchor"];
                anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                marker.groundAnchor = CGPointMake(anchorX, anchorY);
            }

            // The `infoWindowAnchor` property
            if ([iconProperty valueForKey:@"infoWindowAnchor"]) {
                NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
                anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
            }

            if (animation) {
                // Do animation, then send the result
                [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:callbackId];
            } else {
                // Send the result
                [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
            }
        } else {
            if (self.mapCtrl.debuggable) {
                NSLog(@"---- Load the icon from over the internet");
            }
            /***
             * Load the icon from over the internet
             */
            __block BOOL isMapped = (marker.map != nil);
            marker.map = nil;

            /*
             // download the image asynchronously
             R9HTTPRequest *request = [[R9HTTPRequest alloc] initWithURL:url];
             [request setHTTPMethod:@"GET"];
             [request setTimeoutInterval:5];
             [request setFailedHandler:^(NSError *error){}];
             */


            dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0ul);
            dispatch_async(queue, ^{

                NSURL *url = [NSURL URLWithString:iconPath];

                [self downloadImageWithURL:url completionBlock:^(BOOL succeeded, UIImage *image) {

                    if (!succeeded) {

                        if(isMapped) {
                            marker.map = self.mapCtrl.map;
                        }

                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        return;
                    }

                    if (width && height) {
                        image = [image resize:width height:height];
                    }

                    dispatch_async(dispatch_get_main_queue(), ^{
                        marker.icon = image;

                        // The `anchor` property for the icon
                        if ([iconProperty valueForKey:@"anchor"]) {
                            NSArray *points = [iconProperty valueForKey:@"anchor"];
                            CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                            CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                            marker.groundAnchor = CGPointMake(anchorX, anchorY);
                        }


                        // The `infoWindowAnchor` property
                        if ([iconProperty valueForKey:@"infoWindowAnchor"]) {
                            NSArray *points = [iconProperty valueForKey:@"infoWindowAnchor"];
                            CGFloat anchorX = [[points objectAtIndex:0] floatValue] / image.size.width;
                            CGFloat anchorY = [[points objectAtIndex:1] floatValue] / image.size.height;
                            marker.infoWindowAnchor = CGPointMake(anchorX, anchorY);
                        }

                        if (isMapped) {
                            marker.map = self.mapCtrl.map;
                        }


                        if (animation) {
                            // Do animation, then send the result
                            if (self.mapCtrl.debuggable) {
                                NSLog(@"---- do animation animation = %@", animation);
                            }
                            [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:callbackId];
                        } else {
                            // Send the result
                            if (self.mapCtrl.debuggable) {
                                NSLog(@"---- no marker animation");
                            }
                            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
                        }

                    });


                }];

            });


        }
    } else if ([iconProperty valueForKey:@"iconColor"]) {
        UIColor *iconColor = [iconProperty valueForKey:@"iconColor"];
        marker.icon = [GMSMarker markerImageWithColor:iconColor];

        if (animation) {
            // Do animation, then send the result
            [self setMarkerAnimation_:animation marker:marker pluginResult:pluginResult callbackId:callbackId];
        } else {
            // Send the result
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        }

    }

}

- (void)downloadImageWithURL:(NSURL *)url completionBlock:(void (^)(BOOL succeeded, UIImage *image))completionBlock
{
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    [NSURLConnection sendAsynchronousRequest:request
                                       queue:[NSOperationQueue mainQueue]
                           completionHandler:^(NSURLResponse *response, NSData *data, NSError *error) {
                               if ( !error )
                               {
                                   UIImage *image = [[UIImage alloc] initWithData:data];
                                   completionBlock(YES,image);
                               } else{
                                   completionBlock(NO,nil);
                               }
                           }];
}
@end