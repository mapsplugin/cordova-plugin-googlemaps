# Marker cluster (alpha version)

## How to install

```
$> cordova plugin add https://wf9a5m75@bitbucket.org/wf9a5m75/cordova-plugin-googlemaps-cluster.git#cluster_work --variable API_KEY_FOR_IOS=... --variable API_KEY_FOR_ANDROID
```

## How to use?

```js
var data = [
  {
    "position": {"lat": 61.21759217, "lng": -149.8935557},
    "name": "Starbucks - AK - Anchorage  00001",
    "address": "601 West Street_601 West 5th Avenue_Anchorage, Alaska 99501",
    "phone": "907-277-2477"
  },

  ...

  {
    "position": {"lat": 40.055326,"lng": -82.864207},
    "name": "Starbucks - OH - Columbus [W]  06434",
    "address": "Morse Rd & Hamilton Rd_4784 Morse Rd_Columbus, Ohio 43230",
    "phone": "614-475-4147"
  }
];

document.addEventListener("deviceready", function() {
  //plugin.google.maps.environment.setDebuggable(true);

  var mapDiv = document.getElementById("map_canvas");
  var options = {
    'camera': {
      'target': data[0].position,
      zoom: 3
    }
  };
  var map = plugin.google.maps.Map.getMap(mapDiv, options);
  map.on(plugin.google.maps.event.MAP_READY, onMapReady);
});

function onMapReady() {
  var map = this;

  map.addMarkerCluster({
    markers: data
  }, function(markerCluster) {

  });
}
```
