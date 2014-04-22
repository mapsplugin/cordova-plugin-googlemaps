phonegap-googlemaps-plugin
==========================
This plugin helps you to control [Google Maps Android SDK v2](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/) from your JavaScript code.
This plugin works with [Apache Cordova](http://cordova.apache.org/).


![ScreenShot](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/phonegap-googlemaps-plugin_small.png)


###Snippet
![img](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/first-example.gif)
```js
//Define the location
var GOOGLE = new plugin.google.maps.LatLng(37.422858, -122.085065);

//Initialize a map
var map = plugin.google.maps.Map.getMap();
map.addEventListener(plugin.google.maps.event.MAP_READY, function(map) {
  
  // The map is initialized, then show a map dialog
  map.showDialog();

  // Add a marker onto the map
  map.addMarker({
    'position': GOOGLE,
    'title': ["Hello Google Map", "for", "Cordova!"].join("\n"),
    'snippet': "This plugin is awesome!"
  }, function(marker) {
    
    // Move camera position
    map.animateCamera({
      'target': GOOGLE,
      'zoom': 15
    }, function() {
      
      //Show the infoWindow assigned with the marker
      marker.showInfoWindow();
    });
  });
});
```

###Example
You can try the example of this plugin. [SimpleMap_v1.0.2.apk](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/raw/Images/examples/SimpleMap_v1.0.2.apk)

![image](https://raw2.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/examples/simple.png)


###Documentation

* [Installation] (https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Installation)
  * Automatic Installation
  * Tutorials
    * [Tutorial for Windows](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Tutorial-for-Windows)
    * [Tutorial for Mac/Linux](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Tutorial-for-Mac)
* [Map](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Map)
  * Create a map
  * Create a map with initialize options
  * Change the map type
  * Move the camera
  * Move the camera within the specified duration time
  * Get the camera position
  * Get my location
  * Map Class Reference
* [Marker](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Marker)
  * Add a Marker
  * Show InfoWindow
  * Add a marker with multiple line
  * callback
  * Simple Icon
  * Scaled Icon
  * Base64 Encoded Icon
  * Remove the marker
  * Click a marker
  * Click an infoWindow
  * Create a marker draggable
  * Drag Events
  * Create a flat marker
  * Marker Class Reference
* [Circle](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Circle)
  * Add a circle
  * callback
  * Remove the circle
  * Circle Class Reference
* [Polyline](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Polyline)
  * Add a polyline
  * callback
  * Remove the polyline
  * Polyline Class Reference
* [Polygon](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Polygon)
  * Add a polygon
  * callback
  * Remove the polygon
  * Polygon Class Reference
* [Tile Overlay](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/TileOverlay)
  * Add a tile overlay
  * TileOverlay Class Reference
* [Ground Overlay](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/GroundOverlay)
  * Add a ground overlay
  * GroundOverlay Class Reference
* [Kml Overlay](./KmlOverlay)
  * Add a kml overlay
  * KmlOverlay Class Reference
* [Geocode](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Geocode)
  * Geocoding
  * Reverse geocoding
* [LatLng](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/LatLng)
  * Create a LatLng object
  * LatLng Class Reference
* [CameraPosition](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/CameraPosition)
  * CameraPosition Class Reference
* [Location](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Location)
  * Location Class Reference

***

* [Release notes](./Release-Notes)

