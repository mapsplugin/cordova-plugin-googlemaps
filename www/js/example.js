/**
 * For debug purpose, catch JavaScript errors.
 */
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
$(document).ready(function(){
  var pageWidth = window.innerWidth || 
                  document.documentElement.clientWidth ||
                  document.body.clientWidth;
  $("body").css({
    "fontSize": (pageWidth * 0.02) + "px"
  });
});
var map = null;
$(document).on("deviceready", function() {
  map = plugin.google.maps.Map.getMap();
  map.on(plugin.google.maps.event.MAP_READY, onMapReady);
});

$("body").on("touchstart", function() {
  console.log("touchstart on body");
});
$("body").on("touchend", function() {
  console.log("touchend on body");
});
function onMapReady() {
  loadPage("test");
}

function loadPage(tmplName, params) {
  $(document).trigger("pageLeave");
  
  $.get("./pages/" + tmplName + ".html", function(html) {
    $("#map_canvas").html(html);
    
    map.off();
    
map.on("touchstart", function(point, latLng) {
  console.log("touchstart, " + point + " / " + latLng);
});
map.on("touchend", function(point, latLng) {
  console.log("touchend, " + point + " / " + latLng);
});
map.on("touchleave", function(point, latLng) {
  console.log("touchleave, " + point + " / " + latLng);
});
map.on("touchmove", function(point, latLng) {
  console.log("touchmove, " + point + " / " + latLng);
});
map.on("touchcancel", function(point, latLng) {
  console.log("touchcancel, " + point + " / " + latLng);
});
    prettyPrint();
    
    // Execute the code
    setTimeout(function() {
      $(document).trigger("pageLoad", [params]);
    }, 1000);
  });
}