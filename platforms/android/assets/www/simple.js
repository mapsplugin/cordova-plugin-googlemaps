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
  
  map.addKmlOverlay({
    'url': 'www/Social Rewards Redemption Map.kml'
  });
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
      'bearing': 140,
      'duration': 10000
    });
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

function addMarker3a() {
  map.showDialog();
  map.trigger("REMOVE_TOKYO_MARKER");
  map.moveCamera({
    'target': GOOGLE_TOKYO,
    'tilt': 60,
    'zoom': 14,
    'bearing': 0
  });
  map.addMarker({
    'position': GOOGLE_TOKYO,
    'title': 'Google Tokyo!',
    'icon': 'www/images/google_tokyo_icon.png'
  }, function(marker) {
    marker.showInfoWindow();
    map.addEventListenerOnce("REMOVE_TOKYO_MARKER", function() {
      marker.remove();
    });
  });
}

function addMarker3b() {
  map.showDialog();
  map.trigger("REMOVE_TOKYO_MARKER");
  map.moveCamera({
    'target': GOOGLE_TOKYO,
    'tilt': 60,
    'zoom': 14,
    'bearing': 0
  });
  map.addMarker({
    'position': GOOGLE_TOKYO,
    'title': 'Google Tokyo!',
    'icon': {
      'url': 'www/images/google_tokyo_icon.png',
      'size': {
        'width': 74,
        'height': 126
      }
     }
  }, function(marker) {
    marker.showInfoWindow();
    map.addEventListenerOnce("REMOVE_TOKYO_MARKER", function() {
      marker.remove();
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
    'markerClick': function(marker) {
      marker.showInfoWindow();
    },
    'infoClick': function(marker) {
      marker.remove();
    }
  });
}

function addMarkerDrag() {
  map.showDialog();
  map.moveCamera({
    'target': GOOGLE,
    'zoom': 16
  });
  map.addMarker({
    'position': GOOGLE,
    'draggable': true
  }, function(marker) {
    
    marker.addEventListener(plugin.google.maps.event.MARKER_DRAG_END, function(marker) {
      marker.getPosition(function(latLng) {
        marker.setTitle(latLng.toUrlValue());
        marker.showInfoWindow();
      });
    });
  });
}
function addMarker5() {
  map.showDialog();
  map.trigger("REMOVE_TOKYO_MARKER");
  map.moveCamera({
    'target': GOOGLE_TOKYO,
    'tilt': 60,
    'zoom': 14,
    'bearing': 0
  });
  
  var icon = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAACVUlEQVRIS8WWjVXCMBRGwwTqBMIEuAG4ARuIE6gTKBOgEyAT4AbABjKBMIE/C+h3m6S2pWlJ8BzfOTkpad6770teEzom3bZy/VbrpYTopDjJZ6w2c77X6p9j46SCUXvuYDxHq04BZ2rPHXa3y/DRqlPAmdqZW+hrkMZEq44F52q3oGTdrjEpqmPBudoxKVBVKqsU1THgPbW+klNUt4GHCn6idqEGuMveerUeXFGtNTCvah9qaz+n2gMmKMGBnLrfjPFcMirZ7231XUF19RUJkIhPZqXnT8AM9Osy62v0VPihUqIfjWwx1RkJvbxIpjArhabfbEJ6zQYwysiiT3CW8kJ6Q4BgqMALEnqVNAqQZGSkM/R7nMOBLhZ/B/ZQeg9V/1EsrpLy5dIqP8aAXV6WlQIlZrWq/wzeBK0DM3Y0vA0aAh8FPwTaBC7B2W8+qUOMT4l9dYUUrJK2k4tCOHl7O7zK+Xx69nbWU/iebgKz1+9E+OYPToR1fqOe+SquujeBWdzlYGBPohhjW9b2lGbRa72bwLdyml5d2auvaPyeTOzIw4MxzCkal8h8no3cqT3WJd0ExuFmOjXmlhRIXbnfKZQ7hfJ4HDTM8wVIMi6xJ01y3mV8E5glGlDRGIEKS75DrAtFn/0DA3x/b0ddZbPgGt23JnBW0agpKPzUGCvhoT4iv1HG9Zodtc6HGBTYnoXAXc3UR5SbBwK1d8y+8RUAzxNwU2orOwQeyolF/lLT7mUqQ8BqCj4Bt+j1lR0Cs3Sopt8GFLYNF/2JU7K2k6stePL7fwP/AER2xy+mY1/QAAAAAElFTkSuQmCC";
  var canvas = document.getElementById("canvas");
  map.addMarker({
    'position': GOOGLE_TOKYO,
    'title': canvas.toDataURL(),
    'icon': icon
  }, function(marker) {
    marker.showInfoWindow();
    map.addEventListenerOnce("REMOVE_TOKYO_MARKER", function() {
      marker.remove();
    });
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
    
    map.addEventListenerOnce("REMOVE_GROUND_OVERLAY", function() {
      groundOverlay.remove();
    });
  });
}

function removeGroundOverlay() {
  map.showDialog();
  map.trigger("REMOVE_GROUND_OVERLAY");
}


function addTileOverlay() {
  map.addTileOverlay({
    // <x>,<y>,<zoom> are replaced with values
    tileUrlFormat: "http://tile.stamen.com/watercolor/<zoom>/<x>/<y>.jpg"
  }, function(tileOverlay) {
    mTileOverlay = tileOverlay;
    map.showDialog();
    
    map.addEventListenerOnce("REMOVE_TILE_OVERLAY", function() {
      tileOverlay.remove();
    });
  });
}
function removeTileOverlay() {
  map.showDialog();
  map.trigger("REMOVE_TILE_OVERLAY");
}

function addKmlOverlay1() {
  map.addKmlOverlay({
    //'url': 'www/cta.kml'
    'url': 'http://www.googledrive.com/host/0B1ECfqTCcLE8blRHZVVZM1QtRkE/cta.kml'
  }, function() {
    map.moveCamera({
      'target': new plugin.google.maps.LatLng(41.871432, -87.669511),
      'zoom': 10
    });
    map.showDialog();
  });
}
function addKmlOverlay_US() {
  map.addKmlOverlay({
    'url': 'www/US Regions State Boundaries.kml'
  }, function() {
    map.moveCamera({
      'target': new plugin.google.maps.LatLng(39.99994, -114.04658),
      'zoom': 3
    });
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
    'position': GOOGLE
  };
  map.geocode(request, function(results) {
    if (results.length) {
      map.showDialog();
      
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
