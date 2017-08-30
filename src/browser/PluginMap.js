
(function() {
  var utils = require('cordova/utils'),
    BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
    event = require('cordova-plugin-googlemaps.event');

  function PluginMap(args) {
    BaseClass.call(this);
    var self = this;
    Object.defineProperty(this, "mapId", {
      value: args[0],
      writable: false
    });

    self.set("isGoogleReady", false);

    this.one("googleready", function() {
      self.set("isGoogleReady", true);

      if (args.length === 3) {
        var mapOptions = args[1];
        var pluginId = args[2];
        var mapDiv = document.querySelector("[__plugindomid='" + pluginId + "']");

        var gmap = new google.maps.Map(mapDiv, {
          mapTypeId: google.maps.MapTypeId.ROADMAP,
          center: {lat: 0, lng: 0},
          zoom: 0
        });
        self.set("gmap", gmap);

        google.maps.event.addListenerOnce(gmap, "projection_changed", function() {
          self.trigger(event.MAP_READY);
        });
      }
    });

  }

  utils.extend(PluginMap, BaseClass);


  module.exports = PluginMap;
})();
