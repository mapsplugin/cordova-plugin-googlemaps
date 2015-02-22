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

/**
 * Start from here
 */
$(document).on("deviceready", function() {
  var map = plugin.google.maps.Map.getMap();
  map.on(plugin.google.maps.event.MAP_READY, function() {
    alert("MAP_READY");
    navigator.splashscreen.hide();
  });
  
  $("li[action]").click(function() {
    $("#menulist").panel("close");
    
    // Map.clear() method removes all mark-ups, such as marker.
    map.clear();
    
    // Map.off() method removes all event listeners.
    map.off();
    
    var action = $(this).attr("action");
    loadPage(map, action);
  });
  
  /**
   * The side menu overlays above the map, but it's not the children of the map div.
   * In this case, you must call map.setClickable(false) to be able to click the side menu.
   */
  function onSideMenuClose() {
    map.setClickable(true);
  }
  
  function onSideMenuOpen() {
    map.setClickable(false);
  }
  
  $("#menulist").panel({
    "close": onSideMenuClose,
    "open": onSideMenuOpen
  });
  
  loadPage(map, "test");
});

/**
 * Change the embed page view.
 * @param {Object} map
 * @param {String} pageName
 */
function loadPage(map, pageName) {
  $(document).trigger("pageLeave", map);
  $.get("./pages/" + pageName + ".html", function(html) {
    $("#container").html(html);
    $.mobile.activePage.trigger("create");
    
    // PrettyPrint
    // @refer https://code.google.com/p/google-code-prettify/
    if (typeof prettyPrint === "function") {
      prettyPrint();
    }
    
    map.clear();
    map.off();
    
    // Embed a map into the div tag.
    var div = $("#map_canvas")[0];
    if (div) {
      map.setDiv(div);
    }
    
    // Execute the code
    setTimeout(function() {
      $(document).trigger("pageLoad", map);
    }, 1000);
  });
}