Cordova GoogleMaps plugin for iOS and Android
==========================
This plugin is a thin wrapper for [Google Maps Android API](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).
Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

-----

###Quick install

Before you install, make sure you've read the [instructions](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Installation)

*npm (current stable 1.4.0)*
```bash
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

*Github (current master, potentially unstable)*
```bash
$> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

If you re-install the plugin, please always remove the plugin first, then remove the SDK

```bash
$> cordova plugin rm cordova-plugin-googlemaps
$> cordova plugin rm com.googlemaps.ios
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

You can also configure the following variables to customize the iOS location plist entries

- `LOCATION_WHEN_IN_USE_DESCRIPTION` for `NSLocationWhenInUseUsageDescription` (defaults to "Show your location on the map")
- `LOCATION_ALWAYS_USAGE_DESCRIPTION` for `NSLocationAlwaysUsageDescription` (defaults t "Trace your location on the map")

Exmaple using the cordova CLI
```bash
$> cordova plugin rm cordova-plugin-googlemaps
$> cordova plugin rm com.googlemaps.ios
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE" --variable LOCATION_WHEN_IN_USE_DESCRIPTION="My custom when in use message" --variable LOCATION_ALWAYS_USAGE_DESCRIPTION="My custom always usage message"
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

The SDK-Plugin won't be uninstalled automatically and you will stuck on an old version.


### Last release information

**v1.4.0 - 04/Nov/2016**
- Lots of bugs are fixed.
- Improve performance (especially adding markers using the same url)
- Updated Google Maps SDK for iOS to 2.1.1
- StyledMapType is available.



### Quick demo
![](https://dl.dropboxusercontent.com/u/1456061/cordova-google-maps/top/demo.gif)

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

[All documentations are here!!](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki)

* [Installation](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Installation)
  * Automatic Installation
  * Tutorials
    * [Tutorial for Windows](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Tutorial-for-Windows)
    * [Tutorial for Mac/Linux](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Tutorial-for-Mac)
    * [PhoneGap Usage](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Phonegap-Usage)
    * [Tutorial for Crosswalk](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Tutorial-for-CrossWalk-Webview-Plugin-%28Android%29)
    * [Tutorial for Monaca (Cloud building service)](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Tutorial-for-Monaca)
  * Upgrade
    * Just re-install this plugin

* [Terms of Services](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Terms-of-Services)

#### Classes
- [Map](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Map)
- [Marker](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Marker)
- [Circle](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Circle)
- [Polyline](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Polyline)
- [Polygon](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Polygon)
- [Tile Overlay](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/TileOverlay)
- [Ground Overlay](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/GroundOverlay)
- [Kml Overlay](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/KmlOverlay)
- [LatLng](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/LatLng)
- [LatLngBounds](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/LatLngBounds)
- [CameraPosition](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/CameraPosition)
- [Location](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Location)
- [Geocoder](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Geocoder)
- [BaseClass](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/BaseClass)
- [External Service](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/External-Service)

If you want to use crosswalk, just follow this easy documentation.
[Install Plugin with Crosswalk](https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/Tutorial-for-CrossWalk-Webview-Plugin-%28Android%29)

-----

### Join the official community
New versions will be announced through the official community. Stay tuned!

<a href="https://plus.google.com/u/0/communities/117427728522929652853"><img src="https://dl.dropboxusercontent.com/u/1456061/cordova-google-maps/top/Red-signin_Google_base_44dp.png" height="40"></a>

### Chat
Join our online chat at<br>
[![Gitter](https://badges.gitter.im/cordova-plugin-googlemaps.svg)](https://gitter.im/nightstomp/cordova-plugin-googlemaps)

### Example
You can see an example here. **(old version, but all most the same)**

 [phonegap-googlemaps-plugin-v1.2.5.apk](https://dl.dropboxusercontent.com/u/1456061/cordova-google-maps/apks/phonegap-googlemaps-plugin-v1.2.5.apk)
```bash
$> adb install phonegap-googlemaps-plugin-v1.2.5.apk
```

![image](https://dl.dropboxusercontent.com/u/1456061/cordova-google-maps/top/example-v1.2.5.gif)

-----


### Version 2.0 Beta

The new version 2.0 supports multiple maps on multiple pages.
Lots of issues are fixed, and the performance are improved.

However, the documentation is not enough for the version 2.0.
For the reason, the new version is still in the beta.
If you are interested in it, you can try the new version.

For more details, please read here.
https://github.com/mapsplugin/cordova-plugin-googlemaps/wiki/v2-beta

You can try the demo application from here.

![](https://lh3.googleusercontent.com/3iqhCVsAkGkVRjfEnkRpRgoFMP4hB_NPJpdsrgr1nWBk_2fmBgq-R_5dXsrJFzSsjb9rX95vGk8=w1366-h768-rw-no)

https://github.com/mapsplugin/v2.0-demo

-----


### Version 2.0 Beta Roadmap

The version 1.x will be shutdown in 2017.
The date is not decided yet, but we release the v1.4.0 is the last version of v1.x

For more details are announced through the [Official community](https://plus.google.com/u/0/communities/117427728522929652853)

![](https://dl.dropboxusercontent.com/u/1456061/cordova-google-maps/top/roadmap.png)
