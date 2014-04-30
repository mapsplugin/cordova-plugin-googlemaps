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
var map;
document.addEventListener('deviceready', function() {
  var div = document.getElementById("test");
  map = plugin.google.maps.Map.getMap(div);
}, false);

function showMap() {
  map.showDialog();
  
}