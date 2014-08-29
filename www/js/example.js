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
  var map = plugin.google.maps.Map.getMap({
    'backgroundColor': 'green'
  });
  
  $("li[action]").click(function() {
    $("#menulist").panel("close");
    
    // Map.clear() method removes all mark-ups, such as marker.
    map.clear();
    
    var action = $(this).attr("action");
    loadPage(map, action);
  });
  
  /**
   * jQuery Mobile's panel feature uses CSS transition,
   * However this plugin can not detect when CSS transition is started.
   * 
   * For better performance, hide the map before transition,
   * then show it again after the transition is finished.
   */
  function hideMap() {
    map.setVisible(false);
  }
  
  function showMap() {
    // Map.refreshLayout() changes the map position forcely.
    // It causes slow or hang up on iOS,
    // so do not use too much.
    map.refreshLayout();
    
    map.setVisible(true);
  }
  
  $("#menulist").panel({
    "beforeclose": hideMap,
    "close": showMap,
    "beforeopen": hideMap,
    "open": showMap
  });
  
  loadPage(map, "test");
});

/**
 * Change the embed page view.
 * @param {Object} map
 * @param {String} pageName
 */
function loadPage(map, pageName) {
  $.get("./pages/" + pageName + ".html", function(html) {
    $("#container").html(html);
    $.mobile.activePage.trigger("create");
    
    // PrettyPrint
    // @refer https://code.google.com/p/google-code-prettify/
    if (typeof prettyPrint === "function") {
      prettyPrint();
    }
    
    map.clear();
    
    // Embed a map into the div tag.
    var div = $("#map_canvas")[0];
    if (div) {
      map.setDiv(div);
    }
    
    // Execute the code
    setTimeout(function() {
      onPageLoaded(map);
    }, 1000);
  });
}