document.addEventListener('deviceReady', function() {
  var buttons = document.getElementsByTagName("button");
  buttons.forEach(function(button) {
    button.disabled = "disabeld";
  });
  map = plugin.google.maps.Map.getMap();
  map.addEventListener(plugin.google.maps.event.MAP_READY, function() {
    buttons.forEach(function(button) {
      button.disabled = undefined;
    });
  });
});


