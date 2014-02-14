phonegap-googlemaps-plugin
==========================
This plugin helps you to control [Google Maps Android SDK v2][0] and [Google Maps SDK for iOS][1] from your JavaScript code.
This plugin works with [Apache Cordova][2].

[日本語のドキュメント][doc_ja]からご覧いただけます。

![ScreenShot](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/phonegap-googlemaps-plugin_small.png)

###Installation
See the [wiki page](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki/Manual-Installation).

###Documentation
Please read the [document of this plugin](https://github.com/wf9a5m75/phonegap-googlemaps-plugin/wiki).

###Initialize a map
To initialize the map plugin, you need to call the **getMap()** method of the Map class.
The map class raises `MAP_READY` event when the map is initialized.
You can receive the event with either **addEventListener()** or **on()** method.
```js
var map = plugin.google.maps.Map.getMap();
map.addEventListener(plugin.google.maps.event.MAP_READY, function() {
  //Something you want to do
});
```

###Listen events
You can listen several events, such as map clicked.
Available events for Map class are the below:
 * MAP_CLICK
 * MAP_LONG_CLICK
 * MY_LOCATION_CHANGE(Android)
 * MY_LOCATION_BUTTON_CLICK
 * CAMERA_CHANGE 
 * MAP_READY
 * MAP_LOADED(Android)
 * MAP_WILL_MOVE(iOS)

Available events for Marker class are the below:
 * MARKER_CLICK
 * INFO_CLICK
 * MARKER_DRAG
 * MARKER_DRAG_START
 * MARKER_DRAG_END
 
```js
var evtName = plugin.google.maps.event.MAP_LONG_CLICK;
map.on(evtName, function(latLng) {
  alert("Map was long clicked.\n" +
        latLng.toUrlValue());
});
```

###Show the map dialog
This plugin show the map on a dialog window. To open it, call **showDialog()** method.
```js
map.showDialog();
```

###Move the camera within the specified duration time
The **animateCamera()** acepts the duration time for animating with **duration** option.
If you want to animate slowly, you can specify the duration in millisecond.
```js
map.animateCamera({
  'target': GOOGLE,
  'tilt': 60,
  'zoom': 18,
  'bearing': 140,
  'duration': 10000
});
```

###Add a marker
You can make a marker using **addMarker()** method.
```js
map.addMarker({
  'position': GOOGLE,
  'title': "Hello GoogleMap for Cordova!"
});
```
![marker0](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker0.png)


###Add a marker with base64 encoded image
This is really useful feature!
You can set base64 encoded image strings to the **icon** and the **title** options.
That means you are able to create marker image programmatically.
```js
var canvas = document.getElementById("canvas");
map.addMarker({
  'position': GOOGLE_TOKYO,
  'title': canvas.toDataURL(),
  'icon': "data:image/png;base64,iVBORw0KGgoA...",
}, function(marker) {
  marker.showInfoWindow();
});
```
![marker_base64](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker_base64.png)

###Add a polyline, polygon and circle
Adding a polyline uses **addPolyline()** method.
```js
map.addPolyline({
  points: [
    HND_AIR_PORT,
    SFO_AIR_PORT
  ],
  'color' : '#AA00FF',
  'width': 10,
  'geodesic': true
});
```

For adding a polygon, use **addPolygon()** method.
```js
map.addPolygon({
  points: GORYOKAKU_POINTS,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
}, function(polygon) {
  map.animateCamera({
    'target': polygon.getPoints()
  });
});
```

For a circle, use **addCircle()** method.
```js
map.addCircle({
  'center': GOOGLE,
  'radius': 300,
  'strokeColor' : '#AA00FF',
  'strokeWidth': 5,
  'fillColor' : '#880000'
});
```
![image7](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/overlays.png)


###Add a ground overlay
A ground overlay is an image that is fixed to a map. To add an image, call **addGroundOverlay()** method.
```js
var bounds = [
  new plugin.google.maps.LatLng(40.712216,-74.22655),
  new plugin.google.maps.LatLng(40.773941,-74.12544)
];

map.addGroundOverlay({
  'url': "http://www.lib.utexas.edu/maps/historical/newark_nj_1922.jpg",
  'bounds': bounds,
  'opacity': 0.5
}, function(groundOverlay) {
  map.showDialog();
  map.animateCamera({
    'target': bounds
  });
});
```
![image8](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ground_overlay.png)

###Add a tile overlay
A Tile Overlay is a set of images which are displayed on top of the base map tiles.
To your tile layer, call **addTileOverlay()** method.
You need to include `<x>`,`<y>` and `<zoom>` strings into your URL.
These are replaced with values.
```js
map.addTileOverlay({
  // <x>,<y> and <zoom> are replaced with values
  tileUrlFormat: "http://tile.stamen.com/watercolor/<zoom>/<x>/<y>.jpg"
}, function(tileOverlay) {
  mTileOverlay = tileOverlay;
  map.showDialog();
});
```
![image9](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/tile_overlay.png)

###Geocoding
This plugin supports geocoding. You can convert address or landscape names to latitude and longitude.
In Android, this plugin uses Google Play Services feature, while in iOS this plugin uses iOS feature (not Google).
```js
var request = {
  'address': "Kyoto, Japan"
};
map.geocode(request, function(results) {
  if (results.length) {
    map.showDialog();
    var result = results[0];
    var position = result.position; 
    
    map.addMarker({
      'position': position
    });
  } else {
    alert("Not found");
  }
});
```
![geocoding](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/geocoding.png)


###Reverse geocoding
This plugin also supports reverse geocoding. 
In Android, this plugin uses Google Play Services feature, while in iOS this plugin uses iOS feature (not Google).
```js
var request = {
  'position': GOOGLE
};
map.geocode(request, function(results) {
  if (results.length) {
    var result = results[0];
    var position = result.position; 
    var address = [
      result.subThoroughfare || "",
      result.thoroughfare || "",
      result.locality || "",
      result.adminArea || "",
      result.postalCode || "",
      result.country || ""].join(", ");
    
    map.addMarker({
      'position': position,
      'title':  address
    });
  } else {
    alert("Not found");
  }
});
```
![reverse_geocoding](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/reverse_geocoding.png)

[0]: https://developers.google.com/maps/documentation/android/
[1]: https://developers.google.com/maps/documentation/ios/
[2]: http://cordova.apache.org/
[3]: http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface
[4A]: http://developer.android.com/google/play-services/setup.html#Install
[4B]: http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject
[5]: https://developers.google.com/maps/documentation/android/start#specify_app_settings_in_the_application_manifest
[6]: https://developers.google.com/maps/documentation/android/start#get_an_android_certificate_and_the_google_maps_api_key

[iOS1]: https://developers.google.com/maps/documentation/ios/start#getting_the_google_maps_sdk_for_ios
[iOS2]: https://developers.google.com/maps/documentation/ios/start#adding_the_google_maps_sdk_for_ios_to_your_project
[iOS3]: https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings.png

[doc_ja]: https://github.com/wf9a5m75/phonegap-googlemaps-plugin/blob/master/README_ja.md