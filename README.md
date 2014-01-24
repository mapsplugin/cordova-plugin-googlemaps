phonegap-googlemaps-plugin
==========================
This plugin helps you to control [Google Maps Android SDK v2][0] and [Google Maps SDK for iOS][1] from your JavaScript code.
This plugin works with [Apache Cordova][2].

![ScreenShot](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/phonegap-googlemaps-plugin_small.png)

## Manual Android Instration

1. Create a cordova project and add Android platform.
Please refer [the cordova document][3].
``` bash
$> cordova create ./mydir com.example.googlemap MyProject
$> cd mydir
$> cordova platform add android
```

2. In the Cordova Android application you will need to put the following in your `res/xml/config.xml` file as a child to the plugin tag:
``` xml
<feature name="GoogleMaps">
  <param name="android-package" value="plugin.google.maps.GoogleMaps" />
</feature>
```

3. You'll need to set up the **Google Play Services SDK** and link to it.
 * [Install the Google Play Services SDK for Android][4A]
 * [Import the Google Play Services SDK into Eclipse][4B]
 * Link the Google Play Services SDK library to your project. View the properties for the project, and navigate to the 'Android' tab. In the lower part of the dialog, click 'Add' and choose the 'google-play-services_lib' project from the workspace.
![image1](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/google-play-services.png)

4. Add these permissions and elements to your `AndroidManifest.xml`.
Please refer the more detailed exlpain in the [Google Maps Document][5].
``` xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="com.google.android.providers.gsf.permission.READ_GSERVICES"/>
<!-- The following two permissions are not required to use
     Google Maps Android API v2, but are recommended. -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<!-- OpenGL ES version 2 -->
<uses-feature android:glEsVersion="0x00020000" android:required="true" />
<!-- Google Play Services SDK -->
<meta-data android:name="com.google.android.gms.version"
           android:value="@integer/google_play_services_version" />
```
5. Still in your `AndroidManifest.xml`, add your Google Maps API key under the &lt;application&gt; tag.
Please refer the more detailed exlpain in the [Google Maps Document][6].
Replace YOUR_GOOGLE_MAPS_ANDROID_API_KEY_IS_HERE with your google maps api key.
``` xml
<meta-data
  android:name="com.google.android.maps.v2.API_KEY"
  android:value="YOUR_GOOGLE_MAPS_ANDROID_API_KEY_IS_HERE" />
<!-- for Google Play Services SDK -->
<meta-data
  android:name="com.google.android.gms.version"
  android:value="@integer/google_play_services_version" />
```

6. From this plugin folder copy the `www/googlemaps-cdv-plugin.js`and `example/Simple/` files into your application's `assets/www` folder. Overwrite the existing index.html file.

7. From this plugin folder copy the `src/android/` folder into your application's `src` folder.
 
## Manual iOS Instration
1. First of all, download the latest SDK from the [official document][iOS1].

2. Create a cordova project and add iOS platform.
Please refer [the cordova document][3].
``` bash
$> cordova create ./mydir com.example.googlemap MyProject
$> cd mydir
$> cordova platform add ios
```

3. Install the SDK to your project following the [official document][iOS2].
![image2](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings.png)

4. From this plugin folder copy the `www/googlemaps-cdv-plugin.js` and `example/Simple/` files into your application's `assets/www` folder. Overwrite the existing index.html file.

5. From this plugin folder copy the `src/ios/` folder into your application's `Plugins` folder.

6. In the Cordova iOS application you will need to put the following in your `config.xml` file as a child to the plugin tag:
``` xml
<feature name="GoogleMaps">
    <param name="ios-package" value="GoogleMaps" />
</feature>
```
7. Under the group Resources, find your **[PROJECTNAME]-Info.plist**, add a new entry.
For the key, add **Google Maps API Key**, and its value is your **Google Maps API Key for iOS**.
![image3](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/ios-project-settings2.png)

## Coding snippets
**Complete example code is available from [here][example_repo]**

![image](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/examples/simple.png)

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

###Initialize a map with options
If you want to initialize a map with parameters, you can do like this.
```js
var map = plugin.google.maps.Map.getMap({
  'mapType': plugin.google.maps.MapTypeId.HYBRID,
  'controls': {
    'compass': true,
    'myLocationButton': true,
    'indoorPicker': true,
    'zoom': true
  },
  'gestures': {
    'scroll': true,
    'tilt': true,
    'rotate': true
  },
  'camera': {
    'latLng': GORYOKAKU_JAPAN,
    'tilt': 30,
    'zoom': 15,
    'bearing': 50
  }
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
This plugin show the map on a dialog window. To open it, call **showDialog()** method.</p>
```js
map.showDialog();
```

###Close the map dialog
If you want to close the dialog, call **closeDialog()** method.
```js
map.closeDialog();
```

###Change the map type
You can choose the map type using **setMapTypeId()** method.
Available map types are `ROADMAP`, `SATELLITE`, `HYBRID`, `TERRAIN` and `NONE`.
```js
map.setMapTypeId(plugin.google.maps.HYBRID);
```
![image4](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/map_type.png)

###Move the camera
Google Maps for mobile has a view camera.
You see the map via the camera, thus if you want to show a specific location, you need to move the camera.
To do that, this plugin provides **animateCamera()** and **moveCamera()** methods.
The **animateCamera()** moves the camera with animation, while the other hand without animation.
```js
map.animateCamera({
  'target': GOOGLE,
  'tilt': 60,
  'zoom': 18,
  'bearing': 140
});
```
Both methods take a callback function as the second argument.
This callback is involved when the movement is finished.</p>
```js
map.moveCamera({
  'target': STATUE_OF_LIBERTY,
  'zoom': 17,
  'tilt': 30
}, function() {
  var mapType = plugin.google.maps.MapTypeId.HYBRID;
  map.setMapTypeId(mapType);
  map.showDialog();
});
```
![image5](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/camera.png)

###Get the camera position
If you want to know the camera position, just call **getCameraPosition()** method.
```js
map.getCameraPosition(function(camera) {
  var buff = ["Current camera position:\n"
      "latitude:" + camera.target.lat,
      "longitude:" + camera.target.lng,
      "zoom:" + camera.zoom,
      "tilt:" + camera.tilt,
      "bearing:" + camera.bearing].join("\n");
  alert(buff);
});
```

###Get my location
If you want to know where you are, just call **getMyLocation()** method.
```js
map.getMyLocation(function(location) {
  var msg = ["Current your location:\n",
    "latitude:" + location.latLng.lat,
    "longitude:" + location.latLng.lng,
    "speed:" + location.speed,
    "time:" + location.time,
    "bearing:" + location.bearing].join("\n");
  
  map.addMarker({
    'position': location.latLng,
    'title': msg
  }, function(marker) {
    marker.showInfoWindow();
  });
});
```
![image](https://raw2.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/mylocation.png)

###Add a marker
You can make a marker using **addMarker()** method.
```js
map.addMarker({
  'position': GOOGLE,
  'title': "Hello GoogleMap for Cordova!"
});
```

The **addMarker()** method taken a function as callback on the second argument.
The callback is involved when the marker is created.
The plugin passes the marker instance as a parameter.
```js
map.addMarker({
  'position': STATUE_OF_LIBERTY,
  'title': "Statue of Liberty"
}, function(marker) {
  marker.showInfoWindow();
});
```
![image6](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker1.png)


The **title** property accepts multiple lines.
This is not available normally if you use just Google Maps SDKs.
```js
map.addMarker({
  'position': GOOGLE,
  'title': ["Hello GoogleMap", "for", "Cordova!"].join("\n")
}, function(marker) {
  marker.showInfoWindow();
});
```
![image](https://raw2.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker3.png)


###Add a marker with icon
If you want to make a marker with icon, just pass the icon path or URL to the **addMarker()** method.
```js
map.addMarker({
  'position': GOOGLE_TOKYO,
  'title': 'Google Tokyo!'
  'icon': 'www/images/google_tokyo_icon.png'
});
```

Or you can scale the icon image with options.
```js
map.addMarker({
  'position': GOOGLE_TOKYO,
  'title': 'Google Tokyo!',
  'icon': {
    'url': 'www/images/google_tokyo_icon.png',
    'size': {
      'width': 37,
      'height': 63
    }
  }
});
```
![image6](https://raw.github.com/wf9a5m75/phonegap-googlemaps-plugin/Images/screencapture/marker2.png)

###Remove the marker
To remove the marker, call the **remove()** method.
```js
marker.remove();
```

###Click events
This plugin also supports the click events for both marker and infoWindow.
```js
map.addMarker({
  'position': GOOGLE_SYDNEY,
  'title': "Google Sydney",
  'snippet': "click, then remove",
  'draggable': true,
  'markerClick': function(marker) {
    marker.showInfoWindow();
  },
  'infoClick': function(marker) {
    marker.remove();
  }
});
```

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

###Remove the ground overlay
To remove the ground overlay, call the **remove()** method.
```js
groundOverlay.remove();
```


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

###Remove the tile overlay
To remove the tile overlay, call the **remove()** method.
```js
tileOverlay.remove();
```


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


###Reverse geocoding
This plugin also supports reverse geocoding. 
In Android, this plugin uses Google Play Services feature, while in iOS this plugin uses iOS feature (not Google).
```js
var request = {
  'position': new plugin.google.maps.LatLng(37.820905,-122.478576) 
};
map.geocode(request, function(results) {
  if (results.length) {
    var result = results[0];
    var position = result.position; 
    
    map.addMarker({
      'position': position,
      'title':  result.thoroughfare
    });
  } else {
    alert("Not found");
  }
});
```

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

[example_repo]: https://github.com/wf9a5m75/phonegap-googlemaps-plugin_examples
