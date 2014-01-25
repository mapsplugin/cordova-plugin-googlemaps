const GOOGLE = new plugin.google.maps.LatLng(37.422858, -122.085065);
const GOOGLE_TOKYO = new plugin.google.maps.LatLng(35.660556,139.729167);
const GOOGLE_SYDNEY = new plugin.google.maps.LatLng(-33.867487,151.20699);
const GOOGLE_NY = new plugin.google.maps.LatLng(40.740658,-74.002089);
const STATUE_OF_LIBERTY = new plugin.google.maps.LatLng(40.689249,-74.0445);
const HND_AIR_PORT = new plugin.google.maps.LatLng(35.548852,139.784086);
const SFO_AIR_PORT = new plugin.google.maps.LatLng(37.615223,-122.389979);
const GORYOKAKU_JAPAN = new plugin.google.maps.LatLng(41.796875,140.757007);
const GORYOKAKU_POINTS = [
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
];
var map,
    mTileOverlay;

window.onerror = function(message, file, line) {
  var error = [];
  error.push('---[error]');
  if (typeof message == "object") {
    var keys = Object.keys(message);
    keys.forEach(function(key) {
      error.push('[' + key + '] ' + message[key]);
    });
  } else {
    error.push(line + ' at ' + file);
    error.push(message);
  }
  alert(error.join("\n"));
};
document.addEventListener('deviceready', function() {
  
  var i,
      buttons = document.getElementsByTagName("button");
  for (i = 0; i < buttons.length; i++) {
    if (buttons[i].getAttribute("name") != "initMap") {
      buttons[i].disabled = 'disabled';
    }
  }
}, false);

function getMap1() {
  map = plugin.google.maps.Map.getMap();
  map.addEventListener('map_ready', onMapReady);
}
function getMap2() {
  map = plugin.google.maps.Map.getMap({
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
      'zoom': 16,
      'bearing': 50
    }
  });
  map.addEventListener('map_ready', onMapReady);
}
function onMapReady() {
  var i,
      buttons = document.getElementsByTagName("button");
  for (i = 0; i < buttons.length; i++) {
    if (buttons[i].getAttribute("name") != "initMap") {
      buttons[i].disabled = undefined;
    } else {
      buttons[i].disabled = "disabled";
    }
  }
  
  var evtName = plugin.google.maps.event.MAP_LONG_CLICK;
  map.on(evtName, function(latLng) {
    alert("Map was long clicked.\n" +
          latLng.toUrlValue());
  });
  map.showDialog();
}

function showMap() {
  map.showDialog();
}

function setMapTypeId() {
  var select = document.getElementById('mapTypeSelect');
  var mapType = (plugin.google.maps.MapTypeId[select.value]);
  map.setMapTypeId(mapType);
  map.showDialog();
}

function animateCamera() {
  map.moveCamera({
    'target': GOOGLE,
    'zoom': 0
  }, function() {
    map.showDialog();
    map.animateCamera({
      'target': GOOGLE,
      'tilt': 60,
      'zoom': 18,
      'bearing': 140
    });
  });
  
}

function animateCamera_delay() {
  map.moveCamera({
    'target': GOOGLE,
    'zoom': 0
  }, function() {
  
    map.showDialog();
    map.animateCamera({
      'target': GOOGLE,
      'tilt': 60,
      'zoom': 18,
      'bearing': 140
    }, 10000);
  });
}

function moveCamera() {
  map.moveCamera({
    'target': STATUE_OF_LIBERTY,
    'zoom': 17,
    'tilt': 30
  }, function() {
    var mapType = plugin.google.maps.MapTypeId.HYBRID;
    map.setMapTypeId(mapType);
    map.showDialog();
  });
}

function getCameraPosition() {
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

function getMyLocation() {

  map.getMyLocation(function(location) {
    var msg = ["Current your location:\n",
      "latitude:" + location.latLng.lat,
      "longitude:" + location.latLng.lng,
      "speed:" + location.speed,
      "time:" + location.time,
      "bearing:" + location.bearing].join("\n");
    
    map.moveCamera({
      'target': location.latLng
    });
    
    map.addMarker({
      'position': location.latLng,
      'title': msg
    }, function(marker) {
      marker.showInfoWindow();
      map.showDialog();
    });
    
  });
}
function addMarker1a() {
  map.showDialog();
  map.moveCamera({
    'target': GOOGLE,
    'zoom': 17
  });
  map.addMarker({
    'position': GOOGLE,
    'title': "Hello GoogleMap for Cordova!"
  }, function(marker) {
    marker.showInfoWindow();
  });
}

function addMarker1b() {
  map.showDialog();
  map.moveCamera({
    'target': GOOGLE_NY,
    'zoom': 17
  });
  map.addMarker({
    'position': GOOGLE_NY,
    'title': ["Hello GoogleMap", "for", "Cordova!"].join("\n"),
    'snippet': "This plugin is\n awesome!"
  }, function(marker) {
    marker.showInfoWindow();
  });
}

function addMarker2() {
  map.moveCamera({
    'target': STATUE_OF_LIBERTY,
    'zoom': 16
  });
  map.showDialog();
  map.addMarker({
    'position': STATUE_OF_LIBERTY,
    'title': "Statue of Liberty"
  }, function(marker) {
    marker.showInfoWindow();
  });
}

function addMarker3() {
  map.showDialog();
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

function addMarker4() {
  map.showDialog();
  map.moveCamera({
    'target': GOOGLE_SYDNEY,
    'zoom': 16
  });
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
}

function addPolyline() {
  map.showDialog();
  
  map.addPolyline({
    points: [
      HND_AIR_PORT,
      SFO_AIR_PORT
    ],
    'color' : '#AA00FF',
    'width': 10,
    'geodesic': true
  }, function(polyline) {
    
    map.animateCamera({
      'target': polyline.getPoints(),
      'zoom': 2
    });
  });
}

function addPolygon() {
  map.showDialog();
  
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
}

function addCircle() {
  map.showDialog();
  
  map.addCircle({
    'center': GOOGLE,
    'radius': 300,
    'strokeColor' : '#AA00FF',
    'strokeWidth': 5,
    'fillColor' : '#880000'
  });
  
  map.animateCamera({
    'target': GOOGLE,
    'zoom': 14
  });
}

function addGroundOverlay() {
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
}

function addTileOverlay() {
  map.addTileOverlay({
    // <x>,<y>,<zoom> are replaced with values
    tileUrlFormat: "http://tile.stamen.com/watercolor/<zoom>/<x>/<y>.jpg"
  }, function(tileOverlay) {
    mTileOverlay = tileOverlay;
    map.showDialog();
  });
}

function geocoding() {
  var input = document.getElementById("geocoder_input");
  var request = {
    'address': input.value
  };
  map.geocode(request, function(results) {
    if (results.length) {
      map.showDialog();
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

function reverseGeocoding() {
  var request = {
    'position': new plugin.google.maps.LatLng(37.820905,-122.478576) 
  };
  map.geocode(request, function(results) {
    if (results.length) {
      map.showDialog();
      
      var mapType = plugin.google.maps.MapTypeId.HYBRID;
      map.setMapTypeId(mapType);
      
      var result = results[0];
      var position = result.position; 
      
      map.addMarker({
        'position': position,
        'title':  result.thoroughfare
      }, function(marker) {
        
        map.animateCamera({
          'target': position,
          'zoom': 15
        }, function() {
          marker.showInfoWindow();
        });
        
      });
    } else {
      alert("Not found");
    }
  });
}
