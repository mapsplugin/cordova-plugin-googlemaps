# Marker cluster for Cordova GoogleMaps plugin the v2.0

The marker cluster displays the organized markers that collected by algorithm.
A cluster marker indicates there are multiple markers in the around.

If you tap on the cluster icon, map camera will be changed to the around the area.

## Notice

This is the alpha version. Masashi may change the feature and/or property names without notification in advance.

----

## Algorithm

There are some algorithms, but **the geocell algorithm is only available at this time**.
Masashi will add another algorithms in the future, but not now.

----

## Install

Since the code is not merged to the github repository, you need to install from the bitbacket private repository.

Adding the `--no-fetch` option is recommended (this prevents the caching by npm command).

```js
$> cordova plugin rm cordova-googlmaps-plugin // if you installed the regular version

$> cordova plugin add https://bitbucket.org/wf9a5m75/cordova-plugin-googlemaps-cluster#cluster_work --variable API_KEY_FOR_ANDROID=... --variable API_KEY_FOR_IOS=... --no-fetch
```

I will update this private repository if the github repository is updated (at least one time per day).
So you can use this version as well as the github version.
(It means the bug fixes are applied to this version as well.)

----

## Usage of the marker cluster?

You can create a marker cluster through `map.addMarkerCluster()`.

You need to configure **require parameters**:
- `markers`: Array of marker options (not actual markers)
- `icons`: Array of conditions for cluster icon

And other optional parameters are:

- `debug`: draw bounds of geocell grid
- `maxZoomLevel`: maximum zoom level of clustering

```js
var data = [
  {
    "position": {"lat": 21.382314, "lng": -157.933097},
    "title": "Starbucks - HI - Aiea  03641",
    "icon": "img/starbucks.gif"
  },
  {
    "position": {"lat": 21.3871, "lng": -157.9482},
    "title": "Starbucks - HI - Aiea  03642",
    "icon": "img/starbucks.gif"
  },
  ...
  {
    "position": {"lat": 21.363402, "lng": -157.928275},
    "title": "Starbucks - HI - Aiea  03643",
    "icon": "img/starbucks.gif"
  }
];

map.addMarkerCluster({
  debug: false,
  maxZoomLevel: 10,  //default: 15
  markers: data,
  icons: [
    {min: 3, max: 100, url: "./img/blue.png", anchor: {x: 16, y: 16}},
    {min: 100, max: 1000, url: "./img/yellow.png", anchor: {x: 16, y: 16}},
    {min: 1000, max: 2000, url: "./img/purple.png", anchor: {x: 24, y: 24}},
    {min: 2000, url: "./img/red.png", anchor: {x: 32, y: 32}},
  ]
});
```

-------

## marker click event?

If you click on a regular marker (not cluster marker), the `MARKER_CLICK` event occurs on the makrer cluster instance.
You can receive the marker instance in the second argument.

```js
markerCluster.on(plugin.google.maps.event.MARKER_CLICK, function(position, marker) {

  marker.showInfoWindow();

});
```

## Add a marker

If you want to a marker later, you can do that using `markerCluster.addMarker(marker)`;
The marker cluster will redraw the clusters every times.

```js

markerCluster.addMarker({
  position: {lat: ..., lng: ...}
});
```


## Remove a marker

Currently, the only way is like this. This maybe changed in the future.

```js
markerCluster.on(plugin.google.maps.event.MARKER_CLICK, function(position, marker) {

  marker.remove();

});
```

-------

## Properties

### markers `(required)`

Array of marker option properties.
You can specify the same options as the regular way (`map.addMarker()`)

### icons `(required)`

The number of collected markers are drew in the center of the icon.
Array of conditions for cluster icons. You can specify your image files.

The format is like this:

```js
{
  min: 3,                   // minimal number of markers
  max: 100,                 // maximum number of markers
  url: "./img/blue.png",    // the same as markerOptions.url
  anchor: {x: 16, y: 16},   // the same as markerOptions.anchor
  label: {
    color: "blue"           // color for the number
    bold: true/false,
    italic: true/false,
    fontSize: 10            // default: 10px
  }
}
```

### maxZoomLevel

The marker cluster stops clustering if the map zoom level becomes over this number.

### debug

In order to confirm the clustered bounds for a cluster icon, you can set true of this flag.
Blue polygon will be drawn.


-------

## Known issues

- The clustering calculation is kind of slow when you zoom out.

-------

## Questions

If you have a question, feature request, and bug report etc, please let me know at the [issue list](https://github.com/mapsplugin/cordova-plugin-googlemaps/issues).

Or send e-mail to me if you want to hide your information.

(But I will ask to share your project files anyway)


-------

## Demo

code

https://bitbucket.org/wf9a5m75/markercluster

[demo.apk](./demo.apk)

![](https://user-images.githubusercontent.com/167831/28303766-8851617c-6b49-11e7-9679-f31673e2b9ec.gif)

```js
function onMapReady() {
  var map = this;

  var label = document.getElementById("label");

  map.addMarkerCluster({
    debug: false,
    //maxZoomLevel: 5,
    markers: data,
    icons: [
      {min: 2, max: 100, url: "./img/blue.png", anchor: {x: 16, y: 16}},
      {min: 100, max: 1000, url: "./img/yellow.png", anchor: {x: 16, y: 16}},
      {min: 1000, max: 2000, url: "./img/purple.png", anchor: {x: 24, y: 24}},
      {min: 2000, url: "./img/red.png", anchor: {x: 32, y: 32}},
    ]
  }, function(markerCluster) {
    map.set("markerCluster", markerCluster);

    markerCluster.on("resolution_changed", function(prev, newResolution) {
      var self = this;
      label.innerHTML = "<b>zoom = " + self.get("zoom") + ", resolution = " + self.get("resolution") + "</b>";
    });
    markerCluster.trigger("resolution_changed");

    markerCluster.on(plugin.google.maps.event.MARKER_CLICK, function(position, marker) {
      marker.showInfoWindow();
    });


  });

  map.on(plugin.google.maps.event.MAP_CLICK, function(position) {
    var markerCluster = map.get("markerCluster");

    markerCluster.addMarker({
      position: position,
      title: "clicked point"
    });
  });
}
```
