# Cordova GoogleMaps plugin for iOS and Android (version 2.0)

==========================

This plugin is a thin wrapper for [Google Maps Android API](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).
Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

-----

## Quick install

*Stable version(npm)*
```
$> cordova plugin add cordova-plugin-googlemaps \
    --variable API_KEY_FOR_ANDROID="..." \
    --variable API_KEY_FOR_IOS="..."
```

*Develop version (current multiple_maps branch)*
```bash
$> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps#multiple_maps \
    --variable API_KEY_FOR_ANDROID="..." \
    --variable API_KEY_FOR_IOS="..."
```

If you re-install the plugin, please always remove the plugin first, then remove the SDK

```bash
$> cordova plugin rm cordova-plugin-googlemaps

$> cordova plugin rm com.googlemaps.ios

$> cordova plugin add cordova-plugin-googlemaps \
    --variable API_KEY_FOR_ANDROID="..." \
    --variable API_KEY_FOR_IOS="..." \
    --no-fetch
```

### Configuration

You can also configure the following variables to customize the iOS location plist entries

```
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="..." --variable API_KEY_FOR_IOS="..."

- `LOCATION_WHEN_IN_USE_DESCRIPTION` for `NSLocationWhenInUseUsageDescription` (defaults to "Show your location on the map")
- `LOCATION_ALWAYS_USAGE_DESCRIPTION` for `NSLocationAlwaysUsageDescription` (defaults t "Trace your location on the map")

Example using the Cordova CLI

```bash
$> cordova plugin rm cordova-plugin-googlemaps

$> cordova plugin rm com.googlemaps.ios

$> cordova plugin add cordova-plugin-googlemaps \
    --variable API_KEY_FOR_ANDROID="..." \
    --variable API_KEY_FOR_IOS="..." \
    --variable LOCATION_WHEN_IN_USE_DESCRIPTION="My custom when in use message" \
    --variable LOCATION_ALWAYS_USAGE_DESCRIPTION="My custom always usage message"
```

Example using config.xml
```xml
<plugin name="cordova-plugin-googlemaps" spec="2.0.0">
    <variable name="API_KEY_FOR_ANDROID" value="YOUR_ANDROID_API_KEY_IS_HERE" />
    <variable name="API_KEY_FOR_IOS" value="YOUR_IOS_API_KEY_IS_HERE" />
    <variable name="LOCATION_WHEN_IN_USE_DESCRIPTION" value="My custom when in use message" />
    <variable name="LOCATION_ALWAYS_USAGE_DESCRIPTION" value="My custom always usage message" />
</plugin>
```


## Quick demo

![](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/top/demo.gif)

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
  button.addEventListener("click", onButtonClick);
}

function onButtonClick() {

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

## Documentation

[All documentations are here!!](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/README.md)

-----

### How different between Google Maps JavaScript API v3?

This plugin displays the map view of native(Java and Objective-C) features, which is **faster** than Google Maps JavaScript API v3.

And the native map view works even if the device is **offline**.

This plugin provides the features of the native map view to JS developers.

You can write your code `similar like` the Google Maps JavaScript API v3.

**Features compare table**

                | Google Maps JavaScript API v3     | Cordova-Plugin-GoogleMaps             |
----------------|-----------------------------------|---------------------------------------|
Rendering system| JavaScript + HTML                 | JavaScript + Native APIs              |
Offline map     | Not possible                      | Possible (only you displayed area)    |
3D View         | Not possible                      | Possible                              |
Platform        | All browsers                      | Android and iOS app only              |
Tile image      | Bitmap                            | Vector                                |

**Class compare table**

| Google Maps JavaScript API v3     | Cordova-Plugin-GoogleMaps             |
|-----------------------------------|---------------------------------------|
| google.maps.Map                   | Map                                   |
| google.maps.Marker                | Marker                                |
| google.maps.InfoWindow            | Default InfoWindow, and HtmlInfoWindow|
| google.maps.Circle                | Circle                                |
| google.maps.Rectangle             | Polygon                               |
| google.maps.Polyline              | Polyline                              |
| google.maps.Polygon               | Polygon                               |
| google.maps.GroundOverlay         | GroundOverlay                         |
| google.maps.ImageMapType          | TileOverlay                           |
| google.maps.MVCObject             | BaseClass                             |
| google.maps.MVCArray              | BaseArrayClass                        |
| google.maps.Geocoder              | plugin.google.maps.geocoder           |
| google.maps.geometry.spherical    | plugin.google.maps.geometry.spherical |
| google.maps.geometry.encoding     | plugin.google.maps.geometry.encoding  |
| (not available)                   | MarkerCluster                         |
| google.maps.KmlLayer              | KMLLayer (v1.4.5 is available)        |
| google.maps.StreetView            | (not available)                       |
| google.maps.Data                  | (not available)                       |
| google.maps.DirectionsService     | (not available)                       |
| google.maps.DistanceMatrixService | (not available)                       |
| google.maps.FusionTablesLayer     | (not available)                       |
| google.maps.TransitLayer          | map.setTrafficEnabled()               |
| google.maps.places.*              | (not available)                       |
| google.maps.visualization.*       | (not available)                       |

### How does this plugin work?

This plugin generates native map views, and put them **under the browser**.

The map views are not an HTML element. It means they are not kind of `<div>` or something.
But you can specify the size, position of the map view using `<div>`.

This plugin changes the background as `transparent` of your app.
Then the plugin detects your finger tap position which is for: `native map` or `html element`.

![](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/class/Map/mechanism.png)

The benefit of this plugin is able to detect which HTML elements are over the map or not automatically.

In the below image, you tap on the header div, which is over the map view.
This plugin detects your tap is for the header div or the map view, then pass the mouse event.

It means **you can use the native Google Maps views similar like HTML element**.

<img src="https://user-images.githubusercontent.com/167831/28293819-fa689ee0-6b0a-11e7-9eab-6b981a7084e6.png" width="500">

---

## Buy me a beer

I have been spend **tons of time for this plugin project, but even though the plugin is still FREE!!**.

I appreciate if you donate some amount to help this project from this button.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=SQPLZJ672HJ9N&lc=US&item_name=cordova%2dgooglemaps%2dplugin&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

The donated amount is used for buying testing machine (such as iPhone, Android) or new software.
