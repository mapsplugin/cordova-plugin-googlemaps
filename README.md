# Cordova GoogleMaps plugin for iOS and Android (version 2.0 beta)

==========================

This plugin is a thin wrapper for [Google Maps Android API](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).
Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

-----

### Quick install

Since this version is still in beta, you need to install the plugin from github directory.

*Github (current multiple_maps branch)*
```bash
$> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps#multiple_maps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

If you re-install the plugin, please always remove the plugin first, then remove the SDK

```bash
$> cordova plugin rm cordova-plugin-googlemaps
$> cordova plugin rm com.googlemaps.ios
$> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps#multiple_maps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

### Configuration

You can also configure the following variables to customize the iOS location plist entries

- `LOCATION_WHEN_IN_USE_DESCRIPTION` for `NSLocationWhenInUseUsageDescription` (defaults to "Show your location on the map")
- `LOCATION_ALWAYS_USAGE_DESCRIPTION` for `NSLocationAlwaysUsageDescription` (defaults t "Trace your location on the map")

Exmaple using the cordova CLI

```bash
$> cordova plugin rm cordova-plugin-googlemaps
$> cordova plugin rm com.googlemaps.ios
$> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps#multiple_maps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE" --variable LOCATION_WHEN_IN_USE_DESCRIPTION="My custom when in use message" --variable LOCATION_ALWAYS_USAGE_DESCRIPTION="My custom always usage message"
```

Example using config.xml
```xml
<plugin name="cordova-plugin-googlemaps" spec="1.4.0">
    <variable name="API_KEY_FOR_ANDROID" value="YOUR_ANDROID_API_KEY_IS_HERE" />
    <variable name="API_KEY_FOR_IOS" value="YOUR_IOS_API_KEY_IS_HERE" />
    <variable name="LOCATION_WHEN_IN_USE_DESCRIPTION" value="My custom when in use message" />
    <variable name="LOCATION_ALWAYS_USAGE_DESCRIPTION" value="My custom always usage message" />
</plugin>
```


### Quick demo

```html
<script type="text/javascript">
var map;
document.addEventListener("deviceready", function() {
  var div = document.getElementById("map_canvas");

  // Initialize the map view
  map = plugin.google.maps.Map.getMap(div);

  // Wait until the map is ready status.
  map.addEventListener(plugin.google.maps.event.MAP_READY, onMapReady);
}, false);

function onMapReady() {
  var button = document.getElementById("button");
  button.addEventListener("click", onBtnClicked);
}

function onBtnClicked() {

  // Move to the position with animation
  map.animateCamera({
    target: {lat: 37.422359, lng: -122.084344},
    zoom: 17,
    tilt: 60,
    bearing: 140,
    duration: 5000
  }, function() {

    // Add a maker
    map.addMarker({
      position: {lat: 37.422359, lng: -122.084344},
      title: "Welecome to \n" +
             "Cordova GoogleMaps plugin for iOS and Android",
      snippet: "This plugin is awesome!",
      animation: plugin.google.maps.Animation.BOUNCE
    }, function(marker) {

      // Show the info window
      marker.showInfoWindow();

      // Catch the click event
      marker.on(plugin.google.maps.event.INFO_CLICK, function() {

        // To do something...
        alert("Hello world!");

      });
    });
  });
}
</script>
```

-----

### Documentation

[All documentations are here!!](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/README.md)

-----

### Buy me a beer

I have been spend tons of time for this plugin project, but even though the plugin is still free.

I appreciate if you donate some amount to help this project from this button.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=SQPLZJ672HJ9N&lc=US&item_name=cordova%2dgooglemaps%2dplugin&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

The donated amount is used for buying testing machine (such as iPhone, Android) or new software.
