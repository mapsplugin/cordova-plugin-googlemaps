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

function onMapReady() {
  loadPage("welcome");
}

function loadPage(tmplName, params) {
  $(document).trigger("pageLeave");

  $("#nextBtn, #execBtn, #searchBtn").off();

  $.get("./pages/" + tmplName + ".html", function(html) {
    $("#map_canvas").html(html);

    // Call `map.refreshLayout()` if you change the HTML in the map div.
    map.refreshLayout();

    map.off();

    prettyPrint();

    // Execute the code
    setTimeout(function() {
      $(document).trigger("pageLoad", [params]);
    }, 1000);
  });
}
