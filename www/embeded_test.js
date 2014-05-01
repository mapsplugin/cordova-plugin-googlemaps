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
  
  $("#menulist").panel({
    "close": function(event) {
      map.refreshLayout();
    },
    "open": function(event) {
      map.refreshLayout();
    }
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