Cordova GoogleMaps plugin for iOS and Android
==========================
This plugin is a thin wrapper for [Google Maps Android SDK v2](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).
Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

###Chat
Join our online chat at<br> 
[![Gitter](https://badges.gitter.im/cordova-plugin-googlemaps.svg)](https://gitter.im/nightstomp/cordova-plugin-googlemaps)



###Quick install

**Plugin is finally available on npm**<br>
Before you install, make sure you've read the [instructions](https://github.com/phonegap-googlemaps-plugin/cordova-plugin-googlemaps/wiki/Installation)

*npm (current stable 1.3.9)*
```bash
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

*Github (current master, potentially unstable)*
```bash
$> cordova plugin add https://github.com/phonegap-googlemaps-plugin/cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

If you re-install the plugin, please always remove the plugin first, then remove the SDK

```bash
$> cordova plugin rm plugin.google.maps #before 1.4.0
$> cordova plugin rm cordova-plugin-googlemaps #since 1.4+
$> cordova plugin rm com.googlemaps.ios
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="YOUR_ANDROID_API_KEY_IS_HERE" --variable API_KEY_FOR_IOS="YOUR_IOS_API_KEY_IS_HERE"
```

The SDK-Plugin won't be uninstalled automatically and you will stuck on an old version.

###Information
Cordova-iOS 4.X and WKWebView are supported from version 1.4+. There is currently no npm package of 1.4 (work in progress) but if you need this feature, you can grab our master, which is currently considered stable. (We're still fixing bugs, so you might wait until we push 1.4.0 to npm)

###Last release information

**v.1.3.9 - 04/Jan/2016**
Happy new year!
- Fixed a few bugs with Crosswalk, White-Screen Problems.
- Added "maxAddressLines" for Geocoder (Android, iOS  had it already). Check "lines" inside of the extras array.
- Updated Google Maps SDK for iOS to 1.11.1

Please check the new [Tutorial for Crosswalk](https://github.com/phonegap-googlemaps-plugin/cordova-plugin-googlemaps/wiki/Tutorial-for-CrossWalk-Webview-Plugin-%28Android%29)

I recommend to set settings for Crosswalk to 15+ and remove android-platform (`cordova platform rm android`) and re-install it. No patches required anymore to run with crosswalk. It also has some nice performance boosts, as setting translucent isn't required anymore. (in my test-cases)

**v.1.3.6 - 07/Dec/2015**
- Fixed some small bugs
- Updated Google Maps SDK for iOS to 1.11.0
 - Will fix some bugs on iOS 9
 - with BITCODE support
 - fixed blank map problems


Check out the [release notes](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Release-Notes).

###Documentation

* Introduction
  * <a href="https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/How-different-are-the-JavaScript-APIs-%28web%29-and-the-mobile-SDKs-%28native%29">Difference between JavaScript APIs (web) and the mobile SDKs (native)</a>
  * [Why should I use this plugin?](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Why-use-this-plugin%3F)
  * [Java Objective C or JavaScript. Which one do you like?](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Java-Objective-C-or-JavaScript.-Which-one-do-you-like%3F)


* [Installation](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Installation)
  * Automatic Installation
  * Tutorials
    * [Tutorial for Windows](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Tutorial-for-Windows)
    * [Tutorial for Mac/Linux](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Tutorial-for-Mac)
    * [PhoneGap Usage](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Phonegap-Usage)
    * [Tutorial for Crosswalk](https://github.com/phonegap-googlemaps-plugin/cordova-plugin-googlemaps/wiki/Tutorial-for-CrossWalk-Webview-Plugin-%28Android%29)
    * [Tutorial for Monaca (Cloud building service)](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Tutorial-for-Monaca)
  * Upgrade
    * Just re-install this plugin
* [Terms of Services](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Terms-of-Services)
* [Map](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Map)
  * ![img](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/animateCamera.gif)
  * Create a map
  * Create a map with initialize options
  * Change the map type
  * Move the camera
  * Move the camera within the specified duration time
  * Get the camera position
  * Get my location
  * Map Class Reference
* [Marker](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Marker)
  * ![img](https://googledrive.com/host/0B1ECfqTCcLE8LUxUWmhsQmgxVVU/marker5.gif)
  * Add a Marker
  * Show InfoWindow
  * Add a marker with multiple line
  * callback
  * Simple Icon
  * Scaled Icon
  * Text Styling
  * Base64 Encoded Icon
  * Remove the marker
  * Click a marker
  * Click an infoWindow
  * Create a marker draggable
  * Drag Events
  * Create a flat marker
  * Marker Class Reference
* [Circle](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Circle)
  * ![img](https://googledrive.com/host/0B1ECfqTCcLE8ZVQ1djlWNThISEE/circle.png)
  * Add a circle
  * callback
  * Remove the circle
  * Circle Class Reference
* [Polyline](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Polyline)
  * ![img](https://googledrive.com/host/0B1ECfqTCcLE8ZVQ1djlWNThISEE/polyline.png)
  * Add a polyline
  * callback
  * Remove the polyline
  * Polyline Class Reference
* [Polygon](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Polygon)
  * ![img](https://googledrive.com/host/0B1ECfqTCcLE8ZVQ1djlWNThISEE/polygon.png)
  * Add a polygon
  * Click a polygon
  * callback
  * Remove the polygon
  * Polygon Class Reference
* [Tile Overlay](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/TileOverlay)
  * <img src="https://googledrive.com/host/0B1ECfqTCcLE8MU1CbUtNVUs3TEE/tileOverlay.gif" height="250">
  * Add a tile overlay
  * TileOverlay Class Reference
* [Ground Overlay](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/GroundOverlay)
  * <img src="https://googledrive.com/host/0B1ECfqTCcLE8ZVQ1djlWNThISEE/ground_overlay.gif" height="250">
  * Add a ground overlay
  * GroundOverlay Class Reference
* [Kml Overlay](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/KmlOverlay)
  * <img src="https://googledrive.com/host/0B1ECfqTCcLE8MU1CbUtNVUs3TEE/kml-polygon.gif" height="250">
  * Add a kml overlay
  * KmlOverlay Class Reference
* [LatLng](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/LatLng)
  * Create a LatLng object
  * LatLng Class Reference
* [LatLngBounds](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/LatLngBounds)
  * Create a LatLngBounds object
  * LatLngBounds Class Reference
* [CameraPosition](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/CameraPosition)
  * CameraPosition Class Reference
* [Location](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Location)
  * Location Class Reference
* [Geocoder](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Geocoder)
  * <img src="https://googledrive.com/host/0B1ECfqTCcLE8MU1CbUtNVUs3TEE/geocoding.gif" height="250">
  * Geocoding
  * Reverse geocoding
  * Geocoder Class Reference
* [BaseClass](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/BaseClass)
  * BaseClass Reference
* [External Service](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/External-Service)
  * <img src="https://googledrive.com/host/0B1ECfqTCcLE8MU1CbUtNVUs3TEE/direction.gif" height="250">
  * Launch the navigation application

-----

##Crosswalk
If you want to use crosswalk (highly recommended), just follow this easy documentation. 
[Install Plugin with Crosswalk](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Tutorial-for-CrossWalk-Webview-Plugin-%28Android%29)


### Join the official community
New versions will be announced through the official community. Stay tuned!

<a href="https://plus.google.com/u/0/communities/117427728522929652853"><img src="https://googledrive.com/host/0B1ECfqTCcLE8Yng5OUZIY3djUzg/Red-signin_Google_base_44dp.png" height="40"></a>


###Example
You can see an example here. [phonegap-googlemaps-plugin-v1.2.5.apk](https://googledrive.com/host/0B1ECfqTCcLE8TXlUQUJXMmJpNGs/phonegap-googlemaps-plugin-v1.2.5.apk)
```bash
$> adb install phonegap-googlemaps-plugin-v1.2.5.apk
```

![image](https://googledrive.com/host/0B1ECfqTCcLE8ZVQ1djlWNThISEE/example-v1.2.5.gif)

