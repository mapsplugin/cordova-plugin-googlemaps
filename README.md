Cordova GoogleMaps plugin for iOS and Android
==========================

This plugin provides features of [Google Maps Android API](https://developers.google.com/maps/documentation/android/) and [Google Maps SDK for iOS](https://developers.google.com/maps/documentation/ios/).
Both [PhoneGap](http://phonegap.com/) and [Apache Cordova](http://cordova.apache.org/) are supported.

## How different between Google Maps JavaScript API v3?

This plugin displays the map view of native(Java and Objective-C) features, which is **faster** than Google Maps JavaScript API v3.

And the native map view works even if the device is **offline**.

This plugin provides the features of the native map view to JS developers.

You can write your code `similar like` the Google Maps JavaScript API v3.

## How does this plugin work?

Understanding `how does work this plugin` is very important.

The map view is not an HTML element. It means it is not kind of `<div>` or something.
But you can specify the size, position of the map view using `<div>`.

This plugin changes the background as `transparent` of your app.
Then the plugin detects your finger tap position which is for: `native map` or `html element`.

**The plugin v1.4.5** recognizes only `the child elements of the map div`.

**The plugin v2.0-beta3** recognizes all html elements.

![](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/class/Map/mechanism.png)

## Quick demo

![](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/top/demo.gif)

```js
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
```


## Versioning

There are two versions:

- v1.4.5
  Only one map is available in your app. Stable, but no more development. Only critical bug fixes.
  - [Documentation for v1.4.5](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v1.4.0/README.md)


- v2.0-beta3
  Multiple maps in your app is available. Mostly stable, but still developing.
  - [Documentation for v2.0-beta3](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/README.md)
  - [Demo](https://github.com/mapsplugin/v2.0-demo)
  - [Release notes](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/ReleaseNotes/v2.0-beta3/README.md)

<table>
<tr>
  <th>v1.4.5</th>
  <th>v2.0-beta3</th>
</tr>
<tr>
  <td><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/raw/master/v1.4.0/top/demo.gif" width="250"></td>
  <td><img src="https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/images/v2demo.gif?raw=true" width="250"></td>
</tr>
</table>

I recommended you to use the **v2.0-beta3**.

However if you want to use `map.addKmlOverlay()` or `you don't want to update the plugin frequently`,
please use the **v1.4.5**.



## How to install v1.4.5?

```
$> cordova plugin add cordova-plugin-googlemaps --variable API_KEY_FOR_ANDROID="..." --variable API_KEY_FOR_IOS="..."
```


## How to install v2.0-beta3?

```
$> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps#multiple_maps --variable API_KEY_FOR_ANDROID="..." --variable API_KEY_FOR_IOS="..."
```

### More details

- v1.4.5
  - [Documentation for v1.4.5](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v1.4.0/README.md)


- v2.0-beta3
  - [Documentation for v2.0-beta3](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/README.md)
  - [Demo](https://github.com/mapsplugin/v2.0-demo)
  - [Release notes](https://github.com/mapsplugin/cordova-plugin-googlemaps-doc/blob/master/v2.0.0/ReleaseNotes/v2.0-beta3/README.md)
