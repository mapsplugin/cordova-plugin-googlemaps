const GOOGLE = new plugin.google.maps.LatLng(37.422858, -122.085065);
const GOOGLE_TOKYO = new plugin.google.maps.LatLng(35.660556,139.729167);

function onMapReady(map) {
  $("button").removeAttr("disabled");
  $("#showBtn").click(function(){
    onShowBtn(map);
  });
  $(".changeMapTypeBtn").click(function(){
    onChangeMapTypeBtn(map, $(this).attr('typeId'));
  });
  $("#addMarkerBtn").click(function(){
    onAddMarkerBtn(map);
  });
  $("#addIconMarkerBtn").click(function(){
    onAddIconMarkerBtn(map);
  });
  $("#addCircleBtn").click(function(){
    onAddCircleBtn(map);
  });
  
  map.show();
  map.setCenter(GOOGLE);
  map.setMyLocationEnabled(true);
  map.setIndoorEnabled(true);
  map.setTrafficEnabled(true);
  map.setCompassEnabled(true);
  map.on(plugin.google.maps.event.MAP_CLICK, onMapClick);
  map.on(plugin.google.maps.event.MAP_LONG_CLICK, onMapLongClick);
}

function onShowBtn(map) {
  map.show();
}

function onChangeMapTypeBtn(map, typeId) {
  map.show();
  var mapTypeId = plugin.google.maps.MapTypeId.NORMAL;
  if (typeId === "HYBRID") {
    mapTypeId = plugin.google.maps.MapTypeId.HYBRID;
  }
  map.setMapTypeId(mapTypeId);
}
function onMapClick(latLng) {
  alert("Map was clicked.\n" + latLng.toString());
}
function onMapLongClick(latLng) {
  alert("Map was long clicked.\n" + latLng.toString());
}

function onAddMarkerBtn(map) {
  map.show();
  
  map.addMarker({
    'position': GOOGLE,
    'title': "Hello GoogleMap on Cordova(Android)!",
    'snippet': "click me!",
    'draggable': true,
    'markerClick': onMarkerClicked,
    'infoClick': onMarkerClicked
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
  map.show();
  
  map.addMarker({
    'position': GOOGLE_TOKYO,
    'title': 'Google Tokyo!',
    'draggable': true,
    'icon': 'www/images/google_tokyo_icon.png'
  }, function(marker) {
    map.animateCamera({
      'target': GOOGLE_TOKYO,
      'tilt': 60,
      'zoom': 14,
      'bearing': 0
    }, 3000, function() {
      marker.showInfoWindow();
    });
    
  });
  
  
}

function onMarkerClicked(map) {
  var marker = this;
  
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
  map.show();
  map.addCircle({
    'center': GOOGLE,
    'radius': 300,
    'strokeColor' : '#AA00FF',
    'strokeWidth': 5,
    'fillColor' : '#880000'
  });
  map.animateCamera({
    'target': GOOGLE,
    'zoom': 13
  });
};

$(document).on('deviceready',  function() {
  var map = plugin.google.maps.Map.getMap();
  map.on(plugin.google.maps.event.MAP_READY, onMapReady);
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