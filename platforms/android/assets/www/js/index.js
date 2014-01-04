const GOOGLE = new plugin.google.maps.LatLng(37.422858, -122.085065);
const GOOGLE_TOKYO = new plugin.google.maps.LatLng(35.660556,139.729167);
const STATUE_OF_LIBERTY = new plugin.google.maps.LatLng(40.689249,-74.0445);

const ZINDEX_TILE = 1;
const ZINDEX_OVERLAY = 2;
var mTileOverlay = null;
var mGroundOverlay = null;

function onMapReady(map) {
  $("button").removeAttr("disabled");
  $("#showDialog").click(function(){
    onShowBtn(map);
  });
  $(".changeMapType").click(function(){
    onChangeMapTypeBtn(map, $(this).attr('typeId'));
  });
  $("#addMarker").click(function(){
    onAddMarkerBtn(map);
  });
  $("#addIconMarker").click(function(){
    onAddIconMarkerBtn(map);
  });
  $("#addCircle").click(function(){
    onAddCircleBtn(map);
  });
  $("#addPolyline").click(function(){
    onAddPolylineBtn(map);
  });
  $("#addPolygon").click(function(){
    onAddPolygonBtn(map);
  });
  $("#getCameraPosition").click(function() {
    onGetCameraPosition(map);
  });
  $("#addTileOverlay").click(function() {
    onAddTileOverlayBtn(map);
  });
  $("#removeTileOverlay").hide().click(function() {
    onRemoveTileOverlayBtn(map);
  });
  
  $("#getMyLocation").click(function() {
    onGetMyLocation(map);
  });
  $("#addGroundOverlay").click(function() {
    onAddGroundOverlayBtn(map);
  });
  $("#removeGroundOverlay").hide().click(function() {
    onRemoveGroundOverlayBtn(map);
  });
  $("#geocoding").click(function() {
    onGeocodingBtn(map);
  });
  $("#reveseGeocoding").click(function() {
    onReverseGeocodingBtn(map);
  });
  
  map.showDialog();
  map.setMyLocationEnabled(true);
  map.setIndoorEnabled(true);
  map.setTrafficEnabled(true);
  map.setCompassEnabled(true);
}
function onReverseGeocodingBtn(map) {
  var latLngTxt = $("#reverseGeo_input").val().split(","),
      latitude = parseFloat(latLngTxt[0], 10),
      longitude = parseFloat(latLngTxt[1], 10);
  
  var request = {
    'position': new plugin.google.maps.LatLng(latitude, longitude) 
  };
  map.showDialog();
  map.geocode(request, function(results) {
    if (results.length) {
      var result = results[0];
      var position = result.position; 
      
      map.addMarker({
        'position': position,
        'title':  result.thoroughfare
      }, function(marker) {
        
        map.animateCamera({
          'target': position,
          'zoom': 17
        }, function() {
          marker.showInfoWindow();
        });
        
      });
    } else {
      alert("Not found");
    }
  });
}

/**
 * Search the specified address;
 * then add a marker
 */
function onGeocodingBtn(map) {
  var request = {
    'address': $("#geocoder_input").val()
  };
  map.showDialog();
  map.geocode(request, function(results) {
    if (results.length) {
      var result = results[0];
      var position = result.position; 
      
      map.addMarker({
        'position': position,
        'title':  JSON.stringify(result.position)
      }, function(marker) {
        
        map.animateCamera({
          'target': position,
          'zoom': 17
        }, function() {
          marker.showInfoWindow();
        });
        
      });
    } else {
      alert("Not found");
    }
  });
}
/**
 * Show the current location of you
 */
function onGetMyLocation(map) {
  map.getMyLocation(function(location) {
    var buff = ["Current your location:\n",
                "latitude:" + location.latLng.lat,
                "longitude:" + location.latLng.lng,
                "speed:" + location.speed,
                "time:" + location.time,
                "bearing:" + location.speed].join("\n");
    alert(buff);
  });
}

/**
 * Show the current camera information
 */
function onGetCameraPosition(map) {
  map.getCameraPosition(function(camera) {
    var buff = ["Current camera position:\n",
                "latitude:" + camera.target.lat,
                "longitude:" + camera.target.lng,
                "zoom:" + camera.zoom,
                "tilt:" + camera.tilt,
                "bearing:" + camera.bearing].join("\n");
    alert(buff);
  });
}

function onShowBtn(map) {
  map.showDialog();
}

function onChangeMapTypeBtn(map, typeId) {
  map.showDialog();
  var mapTypeId = plugin.google.maps.MapTypeId.NORMAL;
  if (typeId === "HYBRID") {
    mapTypeId = plugin.google.maps.MapTypeId.HYBRID;
  }
  map.setMapTypeId(mapTypeId);
}


function onAddMarkerBtn(map) {
  map.showDialog();
  
  map.addMarker({
    'position': GOOGLE,
    'title': "Hello GoogleMap on Cordova(Android)!",
    'snippet': "click me!",
    'draggable': true,
    'markerClick': onMarkerClicked,
    'infoClick': onMarkerClicked,
    'zIndex': ZINDEX_OVERLAY
  }, function(marker) {
    
    // move the map with animation in 3000ms
    map.animateCamera({
      'target': GOOGLE,
      'tilt': 60,
      'zoom': 16,
      'bearing': 140
    }, function() {
      marker.showInfoWindow();
    });
    
  });
  
  
}
function onAddIconMarkerBtn(map) {
  map.showDialog();
  
  map.addMarker({
    'position': GOOGLE_TOKYO,
    'title': 'Google Tokyo!',
    'draggable': true,
    'icon': 'www/images/google_tokyo_icon.png',
    'zIndex': ZINDEX_OVERLAY,
    'markerClick': function(marker) {
      marker.setVisible(false);
    }
  }, function(marker) {
    map.animateCamera({
      'target': GOOGLE_TOKYO,
      'tilt': 60,
      'zoom': 14,
      'bearing': 0
    }, function() {
      marker.showInfoWindow();
    });
    
  });
  
  
}

// callback: A marker is clicked.
function onMarkerClicked(marker, map) {
  marker.hideInfoWindow();
  marker.getPosition(function(latLng) {
    map.animateCamera({
      'target': latLng,
      'tilt': 60,
      'zoom': 18,
      'bearing': 140
    }, function() {
      marker.setTitle('Google!');
      marker.setSnippet("1600 Amphitheatre Parkway,\n Mountain View, CA 94043");
      marker.showInfoWindow();
    });
  });
}

function onAddCircleBtn(map) {
  map.showDialog();
  
  map.addCircle({
    'center': GOOGLE,
    'radius': 300,
    'strokeColor' : '#AA00FF',
    'strokeWidth': 5,
    'fillColor' : '#880000',
    'zIndex': ZINDEX_OVERLAY
  });
  
  
  map.animateCamera({
    'target': GOOGLE,
    'zoom': 14
  });
}

function onAddPolylineBtn(map) {
  map.showDialog();
  
  map.addPolyline({
    points: [
      new plugin.google.maps.LatLng(35.548852,139.784086),
      new plugin.google.maps.LatLng(37.615223,-122.389979)
    ],
    'color' : '#AA00FF',
    'width': 10,
    'geodesic': true,
    'zIndex': ZINDEX_OVERLAY
  }, function(polyline) {
    
    map.animateCamera({
      'target': polyline.getPoints(),
      'zoom': 2
    });
  });
}

function onAddPolygonBtn(map) {
  map.showDialog();
  
  map.addPolygon({
    points: [
      new plugin.google.maps.LatLng(41.79883, 140.75675),
      new plugin.google.maps.LatLng(41.799240000000005, 140.75875000000002),
      new plugin.google.maps.LatLng(41.797650000000004, 140.75905),
      new plugin.google.maps.LatLng(41.79637, 140.76018000000002),
      new plugin.google.maps.LatLng(41.79567, 140.75845),
      new plugin.google.maps.LatLng(41.794470000000004, 140.75714000000002),
      new plugin.google.maps.LatLng(41.795010000000005, 140.75611),
      new plugin.google.maps.LatLng(41.79477000000001, 140.75484),
      new plugin.google.maps.LatLng(41.79576, 140.75475),
      new plugin.google.maps.LatLng(41.796150000000004, 140.75364000000002),
      new plugin.google.maps.LatLng(41.79744, 140.75454000000002),
      new plugin.google.maps.LatLng(41.79909000000001, 140.75465),
      new plugin.google.maps.LatLng(41.79883, 140.75673)
    ],
    'strokeColor' : '#AA00FF',
    'strokeWidth': 5,
    'fillColor' : '#880000',
    'zIndex': ZINDEX_OVERLAY
  }, function(polygon) {
  
    map.animateCamera({
      'target': polygon.getPoints()
    });
  });
  
}


function onRemoveTileOverlayBtn(map) {
  map.showDialog();
  $("#removeTileOverlay").hide();
  $("#addTileOverlay").show();
  
  mTileOverlay.remove();
  mTileOverlay = null;
}
function onAddTileOverlayBtn(map) {
  map.showDialog();
  $("#removeTileOverlay").show();
  $("#addTileOverlay").hide();
  
  map.addTileOverlay({
    // <x>,<y>,<zoom> are replaced with values
    tileUrlFormat: "http://tile.stamen.com/watercolor/<zoom>/<x>/<y>.jpg",
    zIndex: ZINDEX_TILE
  }, function(tileOverlay) {
    mTileOverlay = tileOverlay;
  });
}

/***
 * Add a ground overlay
 */
function onAddGroundOverlayBtn(map) {
  $("#removeGroundOverlay").show();
  $("#addGroundOverlay").hide();
  
  var bounds = [
    new plugin.google.maps.LatLng(40.712216,-74.22655),
    new plugin.google.maps.LatLng(40.773941,-74.12544)
  ];
  map.showDialog();
  
  map.addGroundOverlay({
    'url': "http://www.lib.utexas.edu/maps/historical/newark_nj_1922.jpg",
    'bounds': bounds,
    'opacity': 0.75
  }, function(groundOverlay) {
    mGroundOverlay = groundOverlay;
    map.animateCamera({
      'target': bounds
    });
  });
  
}

/***
 * Remove the ground overlay
 */
function onRemoveGroundOverlayBtn(map) {
  $("#removeGroundOverlay").hide();
  $("#addGroundOverlay").show();
  map.showDialog();
  mGroundOverlay.remove();
  mGroundOverlay = null;
}


$(document).on('deviceready',  function() {
  var map = plugin.google.maps.Map.getMap(/*{
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
      'latLng': GOOGLE,
      'tilt': 30,
      'zoom': 15,
      'bearing': 50
    }
  }*/);
  
  //involved when the map is ready.
  map.on(plugin.google.maps.event.MAP_READY, onMapReady);
  
  //involved when the map is clicked.
  map.on(plugin.google.maps.event.MAP_CLICK, function(latLng) {
    alert("Map was clicked.\n" + latLng.toUrlValue());
  });
  
  //involved when the map is long clicked.
  map.on(plugin.google.maps.event.MAP_LONG_CLICK, function(latLng) {
    alert("Map was long clicked.\n" + latLng.toUrlValue());
  });
  
  // involved when the map camera is moved.
  //map.on(plugin.google.maps.event.CAMERA_CHANGE, function(camera) {
  //  console.log("onCameraChange:" + JSON.stringify(camera));
  //});
  
});



$("button").attr("disabled", "disabled");

window.onerror = function(message, file, line) {
  console.error('---[error]');
  if (typeof message == "object") {
    var keys = Object.keys(message);
    keys.forEach(function(key) {
      console.error('[' + key + '] ' + message[key]);
    });
  } else {
    console.log(line + ' at ' + file);
    console.error(message);
  }
};