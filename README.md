# Cordova GoogleMaps plugin for Android, iOS and Browser v2.5.1-beta

| Download | Build test (master branch)|
|----------|---------------------------|
| [![](https://img.shields.io/npm/dm/cordova-plugin-googlemaps.svg)](https://npm-stat.com/charts.html?package=cordova-plugin-googlemaps) |[![](https://travis-ci.org/mapsplugin/cordova-plugin-googlemaps.svg?branch=multiple_maps)](https://travis-ci.org/mapsplugin/cordova-plugin-googlemaps/branches) |

  This plugin displays Google Maps in your application.
  This plugin uses these libraries for each platforms:

  - Android : [Google Maps Android API](https://developers.google.com/maps/documentation/android/)
  - iOS : [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/)
  - Browser : [Google Maps JavaScript API v3](https://developers.google.com/maps/documentation/javascript/)

  Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

<table>
<tr>
<td>
<h3>Android, iOS</h3>
<img src="https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps-doc/master/v2.3.0/hello-world/image1.gif"  height="300">
</td>
<td>
<h3>Browser</h3>
<img src="https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/browser_demo.gif" height="300">
</td>
</tr>
</table>

-----

## Guides

  - [How to generate API keys?](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/api_key/README.md)
  - [Hello, World](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/hello-world/README.md)
  - [Hello, World (with PhoneGap Build)](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/hello-world-phonegap-build/README.md)
  - [Trouble shootings](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/tree/master/troubleshootings/README.md)

## Quick install

  - *Stable version(npm)*
    ```
    $> cordova plugin add cordova-plugin-googlemaps \
        --variable API_KEY_FOR_ANDROID="..." \
        --variable API_KEY_FOR_IOS="..."
    ```

  - *Development version(beta version)*
    ```
    $> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps#multiple_maps \
        --variable API_KEY_FOR_ANDROID="..." \
        --variable API_KEY_FOR_IOS="..."
    ```

## PhoneGap Build settings

  ```xml
  <widget ...>
    <plugin name="cordova-plugin-googlemaps">
      <variable name="API_KEY_FOR_ANDROID" value="(api key)" />
      <variable name="API_KEY_FOR_IOS" value="(api key)" />
    </plugin>

    <!--
      You need to specify cli-7.1.0 or greater version.
      https://build.phonegap.com/current-support
    -->
    <preference name="phonegap-version" value="cli-8.0.0" />
  </widget>
  ```

## Install optional variables

  - ![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/icon-android.png) **PLAY_SERVICES_VERSION = (15.0.1)**<br>
    The Google Play Services SDK version.
    _You need to specify the same version number with all other plugins._
    Check out the latest version [here](https://developers.google.com/android/guides/releases).

  - ![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/icon-android.png) **ANDROID_SUPPORT_V4_VERSION = (27.1.1)**<br>
    This plugin requires the Android support library v4.
    _The minimum version is 24.1.0._
    Check out the latest version [here](https://developer.android.com/topic/libraries/support-library/revisions.html).

  - ![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/icon-ios.png) **LOCATION_WHEN_IN_USE_DESCRIPTION**<br>
    This message is displayed when your application requests **LOCATION PERMISSION for only necessary times**.

  - ![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/icon-ios.png) **LOCATION_ALWAYS_USAGE_DESCRIPTION**<br>
    This message is displayed when your application requests **LOCATION PERMISSION for always**.

---------------------------------------------------------------------------------------------------------

## Browser platform

  We support browser platform now!
  You can develop your application with browser, then run it!
  At the end of development, you can upload the html files to your server, or run it on Android or iOS devices.

  ```
  $> cordova run browser
  ```

  If you use [ionic framework](https://ionicframework.com/), it supports `live-reload`.

  ```
  $> ionic cordova run browser -l
  ```

  If you want to use `live-reload`, but you don't want to use other framework or without framework,
  [cordova-plugin-browsersync](https://www.npmjs.com/package/cordova-plugin-browsersync) is useful.

  ```
  $> cordova plugin add cordova-plugin-browsersync

  $> cordova run (browser/android/ios) -- --live-reload
  ```

### API key

  In the browser platform, the maps plugin uses [Google Maps JavaScript API v3](https://developers.google.com/maps/documentation/javascript/)

  You need to set **two API keys for Google Maps JavaScript API v3**.

  ```js
  // If your app runs this program on browser,
  // you need to set `API_KEY_FOR_BROWSER_RELEASE` and `API_KEY_FOR_BROWSER_DEBUG`
  // before `plugin.google.maps.Map.getMap()`
  //
  //   API_KEY_FOR_BROWSER_RELEASE for `https:` protocol
  //   API_KEY_FOR_BROWSER_DEBUG for `http:` protocol
  //
  plugin.google.maps.environment.setEnv({
    'API_KEY_FOR_BROWSER_RELEASE': '(YOUR_API_KEY_IS_HERE)',
    'API_KEY_FOR_BROWSER_DEBUG': ''
  });

  // Create a Google Maps native view under the map_canvas div.
  var map = plugin.google.maps.Map.getMap(div);

  ```

### Why **two API keys**?

  `JavaScript` code is `text code`. Even if you do obfuscation, it's still readable finally.
  In order to protect your API key, you need to set `Key restriction` for these keys.

  ![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/api_key_restrictions.png)

  If you don't set API key, the maps plugin still work with `development mode`.

  ```js
  plugin.google.maps.environment.setEnv({
    'API_KEY_FOR_BROWSER_RELEASE': '(YOUR_API_KEY_IS_HERE)',
    'API_KEY_FOR_BROWSER_DEBUG': '' // If key is empty or unset,
                                    // the maps plugin runs under the development mode.
  });
  ```
  <img src="https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/development_mode.png" width="300">

### Which browser supported?

  Modern browsers should work without any problem.

  ![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/modern_browsers.png)

  Internet Explorer 11 might work. We don't confirm all features, but basic features work.

### Behavior differences

  `Google Maps JavaScript API v3` is completely different ecosystem with `Google Maps Android API` and `Google Maps SDK for iOS`.

  `Google Maps JavaScript API v3` :
  - **can't** draw 3D building,
  - **does't work** if offline,
  - **can't** map rotation,
  - etc...

  In the browser platform, the maps plugin works almost the same behaviors as native platforms(Android, and iOS),
  but not exactly the same behaviors.
  So don't expect too much.

### Touch mechanism difference

  As you may know, [this plugin displays native Google Maps view under the browser in Android and iOS](#how-does-this-plugin-work-android-ios).
  However this plugin displays as `normal HTML element` in browser platform.

  Because of this, touch behavior is different.
  The maps plugin does not hook the touch position, do not set `background: transparent`, ... etc.
  But if you use this plugin as normal behavior, you don't need to consider about this.

---------------------------------------------------------------------------------------------------------

## Please support this plugin activity.

  In order to keep this plugin as free, please consider to donate little amount for this project.

  [![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=SQPLZJ672HJ9N&lc=US&item_name=cordova%2dgooglemaps%2dplugin&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

---------------------------------------------------------------------------------------------------------

## Release Notes
  - **v2.5.1**
    - Fix: (Android/iOS/Browser) Marker cluster does not work when you zoom in.
    - Fix: (iOS) HTML click detection is incorrect.
    - Fix: (Android/iOS) Clicking on POI with an apostrophe in its name causes a SyntaxError.
    - Fix: (Browser) Can not set icon color for marker cluster icons.

  - **v2.5.0**
    - Add: (Android/iOS/Browser) Support `promise` for `TileOverlayOptions.getTile`. You must return new URL in 5 seconds.
      ```js
      var tileOverlay = map.addTileOverlay({
        getTile: function(x, y, zoom) {
          return new Promise(function(resolve, reject) {
            somethingAsync(function(url) {
              resolve(url);
            });
          });
        }
      });
      ```

    - Add: (Android/iOS/Browser) `BaseClass.onThrottled()/addThrottledEventListener()/hasEventListener()` are added.
      ```js
      var marker = map.addMarker({ ... });
      marker.onThrottled('position_changed', function(latLng) {
        console.log(latLng);
      }, 1000);
      ```

    - Add: (Android/iOS/Browser) `TileOverlayOptions.getTile` can return **base64 encoded image(png,gif,jpeg)**.
    - Fix: (Android) Can not load icon image file for Marker after external link opened.
    - Fix: (Browser) `MapOptions.styles` does not work.
    - Fix: (Android) `map.setOptions()` asks location permission always even options do no include `myLocation` and/or `myLocationButton` options.
    - Update: (Android) Set `transparent` backgroundColor at `onResume()` because some other plugins change background color.
    - Update: (Android/iOS) Improve accuracy of touch detection on geodesic polyline.
    - Update: (iOS) Remove "NSData+Base64" library. No longer necessary.
    - Update: (js) ionic 4 hides Google Maps view.
    - Fix: (Browser) `MarkerCluster.remove()` does not work on browser platform.
    - Fix: (Android/iOS/Browser) App crashes (or error) if no panorama available.
    - Fix: (Android/iOS/Browser) `INFO_CLICK` does not work on marker cluster.
    - Fix: (iOS) Can not click on HtmlInfoWindow.

---------------------------------------------------------------------------------------------------------

## Demos


[Demo (Browser)](https://mapsplugin.github.io/HelloGoogleMap/)

![](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/top/demo.gif)


---------------------------------------------------------------------------------------------------------

## Documentation

![](https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/documentations.png?raw=true)

[All documentations are here!!](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/README.md)

https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/README.md

**Quick examples**
<table>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/Map/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/raw/master/images/map.png?raw=true"><br>Map</a></td>
  <td><pre>
var options = {
  camera: {
    target: {lat: ..., lng: ...},
    zoom: 19
  }
};
var map = plugin.google.maps.Map.getMap(mapDiv, options)</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/Marker/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/marker.png?raw=true"><br>Marker</a></td>
  <td><pre>
var marker = map.addMarker({
  position: {lat: ..., lng: ...},
  title: "Hello Cordova Google Maps for iOS and Android",
  snippet: "This plugin is awesome!"
})</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/MarkerCluster/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/markercluster.png?raw=true"><br>MarkerCluster</a></td>
  <td><pre>
var markerCluster = map.addMarkerCluster({
  //maxZoomLevel: 5,
  boundsDraw: true,
  markers: dummyData(),
  icons: [
      {min: 2, max: 100, url: "./img/blue.png", anchor: {x: 16, y: 16}},
      {min: 100, max: 1000, url: "./img/yellow.png", anchor: {x: 16, y: 16}},
      {min: 1000, max: 2000, url: "./img/purple.png", anchor: {x: 24, y: 24}},
      {min: 2000, url: "./img/red.png",anchor: {x: 32,y: 32}}
  ]
});</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/HtmlInfoWindow/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/htmlInfoWindow.png?raw=true"><br>HtmlInfoWindow</a></td>
  <td><pre>
var html = "&lt;img src='./House-icon.png' width='64' height='64' &gt;" +
           "&lt;br&gt;" +
           "This is an example";
htmlInfoWindow.setContent(html);
htmlInfoWindow.open(marker);
</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/Circle/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/circle.png?raw=true"><br>Circle</a></td>
  <td><pre>
var circle = map.addCircle({
  'center': {lat: ..., lng: ...},
  'radius': 300,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
});</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/Polyline/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/polyline.png?raw=true"><br>Polyline</a></td>
  <td><pre>
var polyline = map.addPolyline({
  points: AIR_PORTS,
  'color' : '#AA00FF',
  'width': 10,
  'geodesic': true
});</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/Polygon/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/polygon.png?raw=true"><br>Polygon</a></td>
  <td><pre>
var polygon = map.addPolygon({
  'points': GORYOKAKU_POINTS,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
});</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/GroundOverlay/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/groundoverlay.png?raw=true"><br>GroundOverlay</a></td>
  <td><pre>
var groundOverlay = map.addGroundOverlay({
  'url': "./newark_nj_1922.jpg",
  'bounds': [
    {"lat": 40.712216, "lng": -74.22655},
    {"lat": 40.773941, "lng": -74.12544}
  ],
  'opacity': 0.5
});
</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/TileOverlay/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/tileoverlay.png?raw=true"><br>TileOverlay</a></td>
  <td><pre>
var tileOverlay = map.addTileOverlay({
  debug: true,
  opacity: 0.75,
  getTile: function(x, y, zoom) {
    return "../images/map-for-free/" + zoom + "_" + x + "-" + y + ".gif"
  }
});</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/KmlOverlay/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/multiple_maps/images/kmloverlay.png?raw=true"><br>KmlOverlay</a></td>
  <td><pre>
map.addKmlOverlay({
  'url': 'polygon.kml'
}, function(kmlOverlay) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/Geocoder/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/geocoder.png?raw=true"><br>Geocoder</a></td>
  <td><pre>
plugin.google.maps.Geocoder.geocode({
  // US Capital cities
  "address": [
    "Montgomery, AL, USA", ... "Cheyenne, Wyoming, USA"
  ]
}, function(mvcArray) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/utilities/geometry/poly/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/poly.png?raw=true"><br>poly utility</a></td>
  <td><pre>
var GORYOKAKU_POINTS = [
  {lat: 41.79883, lng: 140.75675},
  ...
  {lat: 41.79883, lng: 140.75673}
]
var contain = plugin.google.maps.geometry.poly.containsLocation(
                    position, GORYOKAKU_POINTS);
marker.setIcon(contain ? "blue" : "red");
</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/tree/master/v2.3.0/class/utilities/geometry/encoding/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/encode.png?raw=true"><br>encode utility</a></td>
  <td><pre>
var GORYOKAKU_POINTS = [
  {lat: 41.79883, lng: 140.75675},
  ...
  {lat: 41.79883, lng: 140.75673}
]
var encodedPath = plugin.google.maps.geometry.
                       encoding.encodePath(GORYOKAKU_POINTS);
</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/utilities/geometry/spherical/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/spherical.png?raw=true"><br>spherical utility</a></td>
  <td><pre>
var heading = plugin.google.maps.geometry.spherical.computeHeading(
                        markerA.getPosition(), markerB.getPosition());
label.innerText = "heading : " + heading.toFixed(0) + "&deg;";
</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/locationservice/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/locationService.png?raw=true"><br>Location service</a></td>
  <td><pre>
plugin.google.maps.LocationService.getMyLocation(function(result) {
  alert(["Your current location:\n",
      "latitude:" + location.latLng.lat.toFixed(3),
      "longitude:" + location.latLng.lng.toFixed(3),
      "speed:" + location.speed,
      "time:" + location.time,
      "bearing:" + location.bearing].join("\n"));
});
</pre></td>
</tr>

<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.3.0/class/StreetView/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/raw/master/images/streetview.png?raw=true"><br>StreetView</a></td>
  <td><pre>
var div = document.getElementById("pano_canvas1");
var panorama = plugin.google.maps.StreetView.getPanorama(div, {
  camera: {
    target: {lat: 42.345573, lng: -71.098326}
  }
});</pre></td>
</tr>
</table>


---------------------------------------------------------------------------------------------------------

### What is the difference between this plugin and Google Maps JavaScript API v3?

Google Maps JavaScript API v3 works on any platforms,
but it does not work if device is **offline**.

This plugin uses three different APIs:
- Android : [Google Maps Android API](https://developers.google.com/maps/documentation/android/)
- iOS : [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/)
- Browser : [Google Maps JavaScript API v3](https://developers.google.com/maps/documentation/javascript/)

In Android and iOS applications, this plugin displays native Google Maps views, which is **faster** than Google Maps JavaScript API v3.
And it even works if the device is **offline**.

In Browser platform, this plugin displays JS map views (Google Maps JavaScript API v3).
It should work as PWA (progressive web application), but the device has to be **online**.

In order to work for all platforms, this plugin provides **own API** instead of each original APIs.
You can write your code `similar to` the Google Maps JavaScript API v3.

**Feature comparison table**

|                | Google Maps JavaScript API v3     | Cordova-Plugin-GoogleMaps(Android,iOS)| Cordova-Plugin-GoogleMaps(Browser)    |
|----------------|-----------------------------------|---------------------------------------|---------------------------------------|
|Rendering system| JavaScript + HTML                 | JavaScript + Native API's             | JavaScript                            |
|Offline map     | Not possible                      | Possible (only your displayed area)   | Not possible                          |
|3D View         | Not possible                      | Possible                              | Not possible                          |
|Platform        | All browsers                      | Android and iOS applications only     | All browsers                          |
|Tile image      | Bitmap                            | Vector                                | Bitmap                                |

**Class comparison table**

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
| google.maps.geometry.poly         | plugin.google.maps.geometry.poly      |
| (not available)                   | MarkerCluster                         |
| google.maps.KmlLayer              | KmlOverlay                            |
| (not available)                   | LocationService                       |
| google.maps.StreetView            | StreetView :sparkles:                 |
| google.maps.Data                  | (not available)                       |
| google.maps.DirectionsService     | (not available)                       |
| google.maps.DistanceMatrixService | (not available)                       |
| google.maps.TransitLayer          | (not available)                       |
| google.maps.places.*              | (not available)                       |
| google.maps.visualization.*       | (not available)                       |

### How does this plugin work (Android, iOS)?

This plugin generates native map views, and puts them **under the browser**.

The map views are not HTML elements. This means that they are not a `<div>` or anything HTML related.
But you can specify the size and position of the map view using its containing `<div>`.

This plugin changes the background to `transparent` in your application.
Then the plugin detects your touch position, which is either meant for the `native map` or an `html element`
(which can be on top of your map, or anywhere else on the screen).

![](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/class/Map/mechanism.png)

The benefit of this plugin is the ability to automatically detect which HTML elements are over the map or not.

For instance, in the image below, say you tap on the header div (which is over the map view).
The plugin will detect whether your tap is for the header div or for the map view and then pass the touch event appropriately.

This means **you can use the native Google Maps views similar to HTML elements**.

![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/touch.png)


---------------------------------------------------------------------------------------------------------
## Official Communities
- Google+ : (managed by @wf9a5m75)

  https://plus.google.com/communities/117427728522929652853

- Gitter : (managed by @Hirbod)

  https://gitter.im/nightstomp/cordova-plugin-googlemaps
