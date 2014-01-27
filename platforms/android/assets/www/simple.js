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
  
      map.moveCamera({
        target: GOOGLE,
        zoom: 17,
      }, function() {
        map.addMarker({
          position: GOOGLE,
          'title': "data:image/png;base64,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wU4K24ZcPfYHvShPNjG6o/evv/5Sb7/9tlqwYEFFIzr22GPV+PHj1eabb15YI/O54Sa2mUAfIyvpKEjeTdgrV/BxJjplxYoVavbs2eq7776rWdy0adPUPvvsUzMdE7ijgESwlk6ZMsXIY2yNASIjSI9InPnLc37++Wd19dVXD+jNSZMmqT333FONGDFC/fDDD+qFF15Qn376aX+aa665Ru28887uWABrUksBI2sgRkcQE68/gL8xa9asfuPfb7/91DnnnKN9jvLjlVdeUQ8++KD+eLvttlMzZszQAPHwQgEjIV6jgJiIZC1btkzdcsstuod333131dbWVtUZl2FaSZ3U0qVLdXoAssMOOwywjt9++0398ssvau3atQrpGxoa1FZbbaWd/fABfwdBgA022ECNHDlS/42R7I8//tDQbbnllmrTTTftP6Wvr0/nh3yqBQuGShO1XkGByOvbb79Va9aswTNt1ejRo9WOO+7obaDCVATLKCAoLGtH/ZFHHlGLFq1/idX06dPVXnvtNeTl8OOPP1YdHR3aYC677DK100476fTwYR577DH13nvvDTp/s802U+eee67ae++9+79766231H333acOOuggdcQRR6h7771XwYjDx9FHH62am5s1bID466+/VmPGjFHt7e26/PCBNLfeeqtavnz5gNEtbr2Q5zvvvKPuueeeQe1AmRdddJHaY489vBgywpU05aDbAKRHCs3ED8FVPDC8rbfeWo8ISUK5uMrefPPNAxz8fffdV3344YcDDOnSSy/Vfg2Od999V82fP7+moZ1xxhnq8MMPV88995xauHChTn/55Zfr0S58fPPNN+q6667TH5122mmqqalJX/3j1qu3t1ePksGBC8CqVasGwHvxxRcrtM+jw5j/YQOQdik0ky0nAOTGG2/Uhn3IIYeo1tZWveYR93j//ffVnXfeqU/DFf/II4/UoOGqjumYjIL6u1NPPVVNmDChIiCnn366Ovjgg9WoUaP0Ffz+++/X6Q444AA1depUXccAAIwsJ5xwwoBqPv/88+qJJ57Qn91www1qm222UXHrFb5gAICzzjpLTxFxfPHFF+ruu+/WoEAr+Gnl08a4uhlMb8z/MA5Iln5I2CBgnOj0JIC8+eab2qAx/YFxwq8IHw888IB69dVX+6/s+C48gpx55pnqsMMO6z8FvgbWYl5//XU9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"
        }, function(marker) {
          map.showDialog();
          marker.showInfoWindow();
        });
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
