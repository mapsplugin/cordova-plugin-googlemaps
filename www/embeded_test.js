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
    $("#map_canvas").css("position", "relative");
    map.toDataURL(function(image) {
      $("<img cache='cache'>").css({
        "position": "absolute"
      }).attr("src", image).appendTo("#map_canvas");
      map.setDiv(null);
    });
  }
  
  function showMap() {
    map.setDiv($("#map_canvas")[0]);
    map.refreshLayout();
    $("#map_canvas > img[cache]").remove();
  }
  
  $("#menulist").panel({
    "beforeclose": hideMap,
    "close": showMap,
    "beforeopen": hideMap,
    "open": showMap
  });
  
  loadPage(map, "welcome");
  
  $("#testBtn").click(function() {
    map.toDataURL(function(image) {
      console.log(image);
      $("#testImg").attr("src", image);
    });
  });
});
function loadPage(map, pageName) {
  $.get("./pages/" + pageName + ".html", function(html) {
    $("#container").html(html);
    onPageLoaded(map);
  });
}