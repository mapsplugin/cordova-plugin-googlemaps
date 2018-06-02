
var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass');

var PLUGINS = {};

function PluginMap(mapId, options, mapDivId) {
  BaseClass.apply(this);
  var mapDiv = document.querySelector("[__pluginMapId='" + mapId + "']");

  var self = this;
  self.set("isGoogleReady", false);

  Object.defineProperty(self, "id", {
    value: mapId,
    writable: false
  });
  Object.defineProperty(self, "objects", {
    value: {},
    enumerable: false,
    writable: false
  });

  self.one("googleready", function() {
    self.set("isGoogleReady", true);

    var mapInitOptions = {
      mapTypeId: google.maps.MapTypeId.ROADMAP,
      noClear: true
    };

    if (options) {
      if (options.camera) {
        if (options.camera.target) {
          mapInitOptions.center = options.camera.target;
        }
        if (options.camera.target) {
          mapInitOptions.zoom = options.camera.zoom;
        }
      }
    }

    var map = new google.maps.Map(mapDiv, mapInitOptions);
    self.set("map", map);

    google.maps.event.addListenerOnce(map, "projection_changed", function() {
      self.trigger(event.MAP_READY);
    });
  });

}

utils.extend(PluginMap, BaseClass);

PluginMap.prototype.resizeMap = function() {
  var self = this;
  var map = self.get("map");

  google.maps.event.trigger(map, "resize");
};

PluginMap.prototype.loadPlugin = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var className = args[0];

  var plugin;
  if (className in PLUGINS) {
    plugin = PLUGINS[className];
  } else {
    var OverlayClass = require('cordova-plugin-googlemaps.Plugin' + className);
    plugin = new OverlayClass(this);
    PLUGINS[className] = plugin;

    // Since Cordova involes methods as Window,
    // the `this` keyword of involved method is Window, not overlay itself.
    // In order to keep indicate the `this` keyword as overlay itself,
    // wrap the method.
    var dummyObj = {};
    var keys = Object.getOwnPropertyNames(OverlayClass.prototype).filter(function (p) {
      return p !== "_create";
    });
    keys.forEach(function(key) {
      if (typeof OverlayClass.prototype[key] === 'function') {
        dummyObj[key] = plugin[key].bind(plugin);
      } else {
        dummyObj[key] = plugin[key];
      }
    });
    require('cordova/exec/proxy').add(self.id + '-marker', dummyObj);
  }

  plugin._create.call(plugin, onSuccess, onError, args);
};

module.exports = PluginMap;
