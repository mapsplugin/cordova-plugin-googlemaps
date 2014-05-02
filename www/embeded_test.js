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

$(document).on("deviceready", function() {
  var map = plugin.google.maps.Map.getMap();
  
  $("li[action]").click(function() {
    $("#menulist").panel("close");
    
    map.clear();
    
    var action = $(this).attr("action");
    loadPage(map, action);
  });
  
  function hideMap() {
    map.setVisible(false);
  }
  
  function showMap() {
    map.refreshLayout();
    map.setVisible(true);
  }
  
  $("#menulist").panel({
    "beforeclose": hideMap,
    "close": showMap,
    "beforeopen": hideMap,
    "open": showMap
  });
  
  loadPage(map, "welcome");
});
function loadPage(map, pageName) {
  $.get("./pages/" + pageName + ".html", function(html) {
    $("#container").html(html);
    $.mobile.activePage.trigger("create");
    if (typeof prettyPrint === "function") {
      prettyPrint();
    }
    onPageLoaded(map);
  });
}