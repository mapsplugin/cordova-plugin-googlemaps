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
  map = plugin.google.maps.Map.getMap();
  map.addEventListener('map_ready', function() {
    
  });
}, false);

function showMap() {
  map.showDialog();
  
}

function addKml() {
  map.showDialog();
  
  map.addKmlOverlay({
    'url': 'www/radio-folder.kml'
  //  'url': 'https://www.google.com/fusiontables/exporttable?query=select+col2+from+1-v6i33Lf_FjhRZcHKO0PG2DADipCg4L-dGiucAE&o=kml&g=col2'
  }, function(kmlOverlay) {
    
    kmlOverlay.on("marker_click", function(marker) {
      alert("clicked");
      marker.remove();
    });
    
    map.on("REMOVE_KML", function() {
      kmlOverlay.remove();
    });
    
  });
}

function removeKml() {
  map.showDialog();
  
  map.trigger("REMOVE_KML");
}
