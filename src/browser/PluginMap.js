


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
      noClear: true,
      zoom: 2,
      disableDefaultUI: true
    };

    if (options) {
      if (options.mapType) {
        mapInitOptions.mapTypeId = options.mapType.toLowerCase().replace("map_type_", "");
      }

      if (options.controls) {
        if (options.controls.zoom !== undefined) {
          mapInitOptions.zoomControl = options.controls.zoom == true;
        }
      }
      if (options.preferences) {
        if (options.preferences.zoom) {
          mapInitOptions.minZoom = Math.max(options.preferences.zoom || 2, 2);
          if (options.preferences.zoom.maxZoom) {
            mapInitOptions.maxZoom = options.preferences.zoom.maxZoom;
          }
        }
      }
    }

    var map = new google.maps.Map(mapDiv, mapInitOptions);

    google.maps.event.addListenerOnce(map, "projection_changed", function() {
      self.trigger(event.MAP_READY);
    });

    if (options) {
      if (options.camera) {
        if (options.camera.target) {

          if (Array.isArray(options.camera.target)) {
            var bounds = new google.maps.LatLngBounds();
            options.camera.target.forEach(function(pos) {
              bounds.extend(pos);
            });
            map.fitBounds(bounds, 20);
          } else {
            map.setCenter(options.camera.target);
          }
          if (typeof options.camera.tilt === 'number') {
            map.setTilt(options.camera.tilt);
          }
          if (typeof options.camera.bearing === 'number') {
            map.setHeading(options.camera.bearing);
          }
          if (typeof options.camera.zoom === 'number') {
            map.setZoom(options.camera.zoom);
          }
        }
      } else {
        map.setCenter({lat: 0, lng: 0});
      }
    } else {
      map.setCenter({lat: 0, lng: 0});
    }
    self.set("map", map);

  });

}

utils.extend(PluginMap, BaseClass);

PluginMap.prototype.resizeMap = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");

  google.maps.event.trigger(map, "resize");
  onSuccess();
};

PluginMap.prototype.animateCamera = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");

  var options = args[0];
  var padding = 20 || options.padding;
  if (Array.isArray(options.target)) {
    var bounds = new google.maps.LatLngBounds();
    options.forEach(function(pos) {
      bounds.extend(pos);
    });
    map.panToBounds(bounds, padding);
  } else {
    if (typeof options.zoom === 'number') {
      map.setZoom(options.zoom);
    }
    map.panTo(options.target);
  }
  if (typeof options.tilt === 'number') {
    map.setTilt(options.tilt);
  }
  if (typeof options.bearing === 'number') {
    map.setHeading(options.bearing);
  }
  onSuccess();

};

PluginMap.prototype.moveCamera = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");

  var options = args[0];
  var padding = 20 || options.padding;
  if (Array.isArray(options.target)) {
    var bounds = new google.maps.LatLngBounds();
    options.forEach(function(pos) {
      bounds.extend(pos);
    });
    map.fitBounds(bounds, padding);
  } else {
    if (typeof options.zoom === 'number') {
      map.setZoom(options.zoom);
    }
    map.setCenter(options.target);
  }
  if (typeof options.tilt === 'number') {
    map.setTilt(options.tilt);
  }
  if (typeof options.bearing === 'number') {
    map.setHeading(options.bearing);
  }
  onSuccess();

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
