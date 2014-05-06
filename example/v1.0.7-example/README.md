## jQuery Mobile + phonegap-googlemaps-plugin

###What is change in v1.0.7
This plugin allows you to specify the area which you want to show the Google Maps since the plugin version 1.0.7.
It means you can embed the map into your app design.

###How to embed a map
```js
var div = document.getElementById("div");
var map = plugin.google.maps.Map.getMap(div);
```
or
```js
var map = plugin.google.maps.Map.getMap();
map.addEventListener(plugin.google.maps.event.MAP_READY, function(map) {
  
  var div = document.getElementById("map_canvas");
  map.setDiv(div);
  
});
```

###Example
You can try this example. [phonegap-googlemaps-plugin-v1.0.7.apk](http://goo.gl/OkCzk3)

![image](https://raw.githubusercontent.com/wf9a5m75/phonegap-googlemaps-plugin/Images/examples/example-v1.0.7.gif)

