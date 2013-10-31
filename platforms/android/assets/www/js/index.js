const GOOGLE = new plugin.google.maps.LatLng(37.422858, -122.085065);
const GOOGLE_TOKYO = new plugin.google.maps.LatLng(35.660556,139.729167);

function onMapReady(map) {
  $("button").removeAttr("disabled");
  $("#showBtn").click(function(){
    onShowBtn(map);
  })
  $(".changeMapTypeBtn").click(function(){
    onChangeMapTypeBtn(map, $(this).attr('typeId'));
  })
  $("#addMarkerBtn").click(function(){
    onAddMarkerBtn(map);
  })
  $("#addIconMarkerBtn").click(function(){
    onAddIconMarkerBtn(map);
  })
  $("#addCircleBtn").click(function(){
    onAddCircleBtn(map);
  })
  
  map.show();
  map.setCenter(GOOGLE);
  map.setMyLocationEnabled(true);
  map.setIndoorEnabled(true);
  map.setTrafficEnabled(true);
  map.on('click', onMapClick);
  map.on('long_click', onMapLongClick);
}

function onShowBtn(map) {
  map.show();
}

function onChangeMapTypeBtn(map, typeId) {
  map.show();
  var mapTypeId = plugin.google.maps.MapTypeId.NORMAL
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
      'markerClick': function(marker) {
        onMarkerClicked(map, marker);
      },
      'infoClick': function(marker) {
        onMarkerClicked(map, marker);
      }
    }, function(marker) {
      marker.showInfoWindow();
    });
  
  setTimeout(function() {
    map.animateCamera({
      'target': GOOGLE,
      'tilt': 60,
      'zoom': 16,
      'bearing': 140
    })
  }, 1000);
}
function onAddIconMarkerBtn(map) {
  map.show();
  
  map.addMarker({
    'position': GOOGLE_TOKYO,
    'title': 'Google Tokyo!',
    'draggable': true,
    'icon': 'www/images/google_tokyo_icon.png'
  }, function(marker) {
    marker.showInfoWindow();
  });
  
  setTimeout(function() {
    map.animateCamera({
      'target': GOOGLE_TOKYO,
      'tilt': 60,
      'zoom': 14,
      'bearing': 0
    });
  }, 1000);
}

function onMarkerClicked(map, marker) {
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
    })
  });
}

function onAddCircleBtn(map) {
  map.show();
  map.addCircle({
    'center': GOOGLE,
    'radius': 300,
    'strokeColor' : '#AA00FF00',
    'strokeWidth': 5,
    'fillColor' : '#880000FF'
  });
  map.animateCamera({
    'target': GOOGLE,
    'zoom': 13
  })
};


function onInitBtnClicked() {
  
  button = document.getElementById('addMarkerBtn');
  button.addEventListener('click', function(){
    onAddMarkerBtn(map);
  }, false);
  
  button = document.getElementById('addCircleBtn');
  button.addEventListener('click', function(){
    onCircleBtn(map);
  }, false);
}

$(document).on('deviceready',  function() {
  var map = plugin.google.maps.Map.getMap();
  map.bind('map_ready', onMapReady);
});

$("button").attr("disabled", "disabled");
