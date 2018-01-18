# Cordova GoogleMaps plugin for iOS and Android (version 2.2.0)

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

#### If you can't reinstall the plugin, try like this:

```
$> cordova platform rm android ios

$> cordova platform add android@6 ios
```

### Configuration

You can also configure the following variables to customize the iOS location plist entries

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

## Release Notes

- No longer supported for Android 4.3 or lower versions. At least Android 4.4 is required.

- Implement the CSS/HTML element hierarchy correctly.

- Implement map.addKmlOverlay()

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

**Quick examples**
<table>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/Map/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/raw/master/images/map.png?raw=true"><br>Map</a></td>
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
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/Marker/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/marker.png?raw=true"><br>Marker</a></td>
  <td><pre>
map.addMarker({
  position: {lat: ..., lng: ...},
  title: "Hello Cordova Google Maps for iOS and Android",
  snippet: "This plugin is awesome!"
}, function(marker) { ... })</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/MarkerCluster/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/markercluster.png?raw=true"><br>MarkerCluster</a></td>
  <td><pre>
map.addMarkerCluster({
  //maxZoomLevel: 5,
  boundsDraw: true,
  markers: dummyData(),
  icons: [
      {min: 2, max: 100, url: "./img/blue.png", anchor: {x: 16, y: 16}},
      {min: 100, max: 1000, url: "./img/yellow.png", anchor: {x: 16, y: 16}},
      {min: 1000, max: 2000, url: "./img/purple.png", anchor: {x: 24, y: 24}},
      {min: 2000, url: "./img/red.png",anchor: {x: 32,y: 32}}
  ]
}, function(markerCluster) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/HtmlInfoWindow/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/htmlInfoWindow.png?raw=true"><br>HtmlInfoWindow</a></td>
  <td><pre>
var html = "&lt;img src='./House-icon.png' width='64' height='64' &gt;" +
           "&lt;br&gt;" +
           "This is an example";
htmlInfoWindow.setContent(html);
htmlInfoWindow.open(marker);
</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/Circle/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/circle.png?raw=true"><br>Circle</a></td>
  <td><pre>
map.addCircle({
  'center': {lat: ..., lng: ...},
  'radius': 300,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
}, function(circle) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/Polyline/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/polyline.png?raw=true"><br>Polyline</a></td>
  <td><pre>
map.addPolyline({
  points: AIR_PORTS,
  'color' : '#AA00FF',
  'width': 10,
  'geodesic': true
}, function(polyline) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/Polygon/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/polygon.png?raw=true"><br>Polygon</a></td>
  <td><pre>
map.addPolygon({
  'points': GORYOKAKU_POINTS,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
}, function(polygon) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/GroundOverlay/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/groundoverlay.png?raw=true"><br>GroundOverlay</a></td>
  <td><pre>
map.addPolygon({
  'points': GORYOKAKU_POINTS,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
}, function(polygon) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/TileOverlay/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/tileoverlay.png?raw=true"><br>TileOverlay</a></td>
  <td><pre>
map.addTileOverlay({
  debug: true,
  opacity: 0.75,
  getTile: function(x, y, zoom) {
    return "../images/map-for-free/" + zoom + "_" + x + "-" + y + ".gif"
  }
}, function(tileOverlay) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/KmlOverlay/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/multiple_maps/images/kmloverlay.png?raw=true"><br>KmlOverlay</a></td>
  <td><pre>
map.addKmlOverlay({
  'url': 'http://www.weather.gov/source/crh/shapefiles/wwa.kmz'
}, function(kmlOverlay) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/Geocoder/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/geocoder.png?raw=true"><br>Geocoder</a></td>
  <td><pre>
plugin.google.maps.Geocoder.geocode({
  // US Capital cities
  "address": [
    "Montgomery, AL, USA", ... "Cheyenne, Wyoming, USA"
  ]
}, function(mvcArray) { ... });</pre></td>
</tr>
<tr>
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/utilities/geometry/poly/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/poly.png?raw=true"><br>poly utility</a></td>
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
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/tree/master/v2.0.0/class/utilities/geometry/encoding/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/encode.png?raw=true"><br>encode utility</a></td>
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
  <td><a href="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/class/utilities/geometry/spherical/README.md"><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps/blob/master/images/spherical.png?raw=true"><br>spherical utility</a></td>
  <td><pre>
var heading = plugin.google.maps.geometry.spherical.computeHeading(
                        markerA.getPosition(), markerB.getPosition());
label.innerText = "heading : " + heading.toFixed(0) + "&deg;";
</pre></td>
</tr>
</table>


-----

### How different between Google Maps JavaScript API v3?

This plugin displays the map view of native(Java and Objective-C) features, which is **faster** than Google Maps JavaScript API v3.

And the native map view works even if the device is **offline**.

This plugin provides the features of the native map view to JS developers.

You can write your code `similar like` the Google Maps JavaScript API v3.

**Features compare table**

|                | Google Maps JavaScript API v3     | Cordova-Plugin-GoogleMaps             |
|----------------|-----------------------------------|---------------------------------------|
|Rendering system| JavaScript + HTML                 | JavaScript + Native APIs              |
|Offline map     | Not possible                      | Possible (only you displayed area)    |
|3D View         | Not possible                      | Possible                              |
|Platform        | All browsers                      | Android and iOS app only              |
|Tile image      | Bitmap                            | Vector                                |

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
| google.maps.geometry.poly         | plugin.google.maps.geometry.poly      |
| (not available)                   | MarkerCluster                         |
| google.maps.KmlLayer              | KMLLayer (v1.4.5 is available)        |
| google.maps.StreetView            | (not available)                       |
| google.maps.Data                  | (not available)                       |
| google.maps.DirectionsService     | (not available)                       |
| google.maps.DistanceMatrixService | (not available)                       |
| google.maps.FusionTablesLayer     | (not available)                       |
| google.maps.TransitLayer          | (not available)                       |
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

![](https://raw.githubusercontent.com/mapsplugin/cordova-plugin-googlemaps/master/images/touch.png)

---

## Official Communities

- Google+ : @wf9a5m75
  https://plus.google.com/communities/117427728522929652853

- Gitter : @Hirbod
  https://gitter.im/nightstomp/cordova-plugin-googlemaps

---

## Buy me a beer

We appreciate if you donate some amount to help this project from this button.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=SQPLZJ672HJ9N&lc=US&item_name=cordova%2dgooglemaps%2dplugin&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

The donated amount is used for buying testing machine (such as iPhone, Android) or new software.


## Buy me a beer (by bitcoin)

Thank you for supporting us by bitcoin.

3LyVAfANZwcitEEnFbsHup3mDJfuqp8QFb
