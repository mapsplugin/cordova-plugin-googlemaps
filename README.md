Cordova GoogleMaps plugin <br>for iOS and Android
==========================
This plugin is a thin wrapper for [Google Maps Android SDK v2](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).
Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

###Chat
Join our online chat at<br> 
[![Gitter](https://badges.gitter.im/cordova-plugin-googlemaps.svg)](https://gitter.im/nightstomp/cordova-plugin-googlemaps)

###Update status

**v.1.3.4 - 15/10/2015**

- Updated Google Maps SDK for iOS to 1.10.4
 - Will fix some bugs on iOS 9

**v.1.3.3**

**Added**

- added zIndex (iOS only, not available for Android)
 - [659](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/issues/659)

```js
map.addMarker({
    zIndex: int
})
// and
marker.setZIndex()
```

**Fixed**
- Status-Bar Shift-Down
 - [657](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/issues/657#issuecomment-146036169)
- Temp Dom-not-updated fix
 - [658](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/issues/658)
- Stabilized iOS loading
 - [623](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/issues/623)


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
If you want to use crosswalk, just follow this easy documentation. 
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

