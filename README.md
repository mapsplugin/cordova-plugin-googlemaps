# Cordova GoogleMaps plugin for iOS and Android (version 2.3.0)

This plugin is a thin wrapper for [Google Maps Android API](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).

Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

-----

## Quick install

*Stable version(npm)*
```
$> cordova plugin add cordova-plugin-googlemaps#2.3.0 \
    --variable API_KEY_FOR_ANDROID="..." \
    --variable API_KEY_FOR_IOS="..."

(Optional variables)
  // [Android]
  //    The Google Play Services SDK version
  //    You need to specify the same version number with all other plugins.
  //    Check out the latest version here.
  //    https://developers.google.com/android/guides/releases
  PLAY_SERVICES_VERSION="15.0.1" (for Android)

  // [Android]
  //    This plugin requires the Android support library v4.
  //    The minimum version is 24.1.0
  //    Check out the latest version here.
  //    https://developer.android.com/topic/libraries/support-library/revisions.html
  ANDROID_SUPPORT_V4_VERSION="27.1.1" (for Android)

  // [iOS]
  //    These variable messages are displayed when application requests
  //    end-user location.
  LOCATION_WHEN_IN_USE_DESCRIPTION="(your custom message)"  (for iOS)
  LOCATION_ALWAYS_USAGE_DESCRIPTION="(your custom message)"  (for iOS)
```

## PhoneGap Build settings

```xml
<widget ...>
  <plugin name="cordova-plugin-googlemaps" spec="^2.3.0">
    <variable name="API_KEY_FOR_ANDROID" value="(api key)" />
    <variable name="API_KEY_FOR_IOS" value="(api key)" />

    <!-- these are optional settings -->
    <variable name="PLAY_SERVICES_VERSION" value="15.0.1" />
    <variable name="ANDROID_SUPPORT_V4_VERSION" value="27.1.1" />
    <variable name="LOCATION_WHEN_IN_USE_DESCRIPTION" value="(your custom message)" />
    <variable name="LOCATION_ALWAYS_USAGE_DESCRIPTION" value="(your custom message)" />
  </plugin>

  <!--
    You need to specify cli-7.1.0 or greater version.
    https://build.phonegap.com/current-support
  -->
  <preference name="phonegap-version" value="cli-8.0.0" />
</widget>
```

---

## Release Notes

---

## Quick demo

![](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/top/demo.gif)

```html
document.addEventListener("deviceready", function() {
  var div = document.getElementById("map_canvas");

  // Initialize the map view
  var map = plugin.google.maps.Map.getMap(div);


  var button = document.getElementById("button");
  button.addEventListener("click", function() {

    // Move to the position with animation
    map.animateCamera({
      target: {lat: 37.422359, lng: -122.084344},
      zoom: 17,
      tilt: 60,
      bearing: 140,
      duration: 5000
    });

    // Add a maker
    var marker = map.addMarker({
      position: {lat: 37.422359, lng: -122.084344},
      title: "Welecome to \n" +
             "Cordova GoogleMaps plugin for iOS and Android",
      snippet: "This plugin is awesome!",
      animation: plugin.google.maps.Animation.BOUNCE
    });

    // Show the info window
    marker.showInfoWindow();

    // Catch the click event
    marker.on(plugin.google.maps.event.INFO_CLICK, function() {

      // To do something...
      alert("Hello world!");

    });
  }
}, false);
```

-----

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


-----

### What is the difference between this plugin and Google Maps JavaScript API v3?

This plugin displays the map view using the native API's via (Java and Objective-C), which is **faster** than Google Maps JavaScript API v3.

The native map view even works if the device is **offline**.

This plugin provides the features of the native map view to JS developers.

You can write your code `similar to` the Google Maps JavaScript API v3.

**Feature comparison table**

|                | Google Maps JavaScript API v3     | Cordova-Plugin-GoogleMaps             |
|----------------|-----------------------------------|---------------------------------------|
|Rendering system| JavaScript + HTML                 | JavaScript + Native API's             |
|Offline map     | Not possible                      | Possible (only your displayed area)   |
|3D View         | Not possible                      | Possible                              |
|Platform        | All browsers                      | Android and iOS applications only     |
|Tile image      | Bitmap                            | Vector                                |

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
| google.maps.KmlLayer              | KMLLayer                              |
| (not available)                   | LocationService                       |
| google.maps.StreetView            | StreetView                            |
| google.maps.Data                  | (not available)                       |
| google.maps.DirectionsService     | (not available)                       |
| google.maps.DistanceMatrixService | (not available)                       |
| google.maps.FusionTablesLayer     | (not available)                       |
| google.maps.TransitLayer          | (not available)                       |
| google.maps.places.*              | (not available)                       |
| google.maps.visualization.*       | (not available)                       |

### How does this plugin work?

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

---

## Official Communities

- Google+ : (managed by @wf9a5m75)

  https://plus.google.com/communities/117427728522929652853

- Gitter : (managed by @Hirbod)

  https://gitter.im/nightstomp/cordova-plugin-googlemaps

---

## Buy us a beer

Thank you for supporting our activities.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=SQPLZJ672HJ9N&lc=US&item_name=cordova%2dgooglemaps%2dplugin&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

All donations are used for purchasing testing devices (both iOS and Android) and/or new software.


## Buy us a beer (with bitcoin)

Thank you for supporting us via bitcoin.

3LyVAfANZwcitEEnFbsHup3mDJfuqp8QFb
