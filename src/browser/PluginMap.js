var utils = require('cordova/utils');
var event = require('cordova-plugin-googlemaps.event');
var BaseClass = require('cordova-plugin-googlemaps.BaseClass');

function PluginMap(mapId, options, mapDivId) {
  BaseClass.apply(this);
  var mapDiv = document.querySelector("[__pluginMapId='" + mapId + "']");

  var self = this;
  self.set("isGoogleReady", false);

  self.one("googleready", function() {
    self.set("isGoogleReady", true);
    var map = new google.maps.Map(mapDiv, {
      mapTypeId: google.maps.MapTypeId.ROADMAP,
      center: {lat: 0, lng: 0},
      zoom : 1,
      noClear: true
    });
    self.set("map", map);

    google.maps.event.addListenerOnce(map, "projection_changed", function() {
      self.trigger(event.MAP_READY);
    });
  });

}

utils.extend(PluginMap, BaseClass);

PluginMap.prototype.resizeMap = function() {
  var self = this;
console.log(self);
  var map = self.get("map");
console.log(arguments);

  google.maps.event.trigger(map, "resize");
};

module.exports = PluginMap;
