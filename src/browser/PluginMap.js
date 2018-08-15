


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng'),
  MapTypeId = require('cordova-plugin-googlemaps.MapTypeId');

var MAP_TYPES = {};
MAP_TYPES[MapTypeId.NORMAL] = "roadmap";
MAP_TYPES[MapTypeId.ROADMAP] = "roadmap";
MAP_TYPES[MapTypeId.SATELLITE] = "satellite";
MAP_TYPES[MapTypeId.HYBRID] = "hybrid";
MAP_TYPES[MapTypeId.TERRAIN] = "terrain";
MAP_TYPES[MapTypeId.NONE] = "none";

var LOCATION_ERROR = {};
LOCATION_ERROR[1] = 'service_denied';
LOCATION_ERROR[2] = 'not_available';
LOCATION_ERROR[3] = 'cannot_detect';
var LOCATION_ERROR_MSG = {};
LOCATION_ERROR_MSG[1] = 'Location service is rejected by user.';
LOCATION_ERROR_MSG[2] = 'Since this device does not have any location provider, this app can not detect your location.';
LOCATION_ERROR_MSG[3] = 'Can not detect your location. Try again.';

var mapTypeReg = null;

function PluginMap(mapId, options, mapDivId) {
  var self = this;
  BaseClass.apply(this);
  var mapDiv = document.querySelector("[__pluginMapId='" + mapId + "']");
  var container = document.createElement("div");
  container.style.userSelect="none";
  container.style["-webkit-user-select"]="none";
  container.style["-moz-user-select"]="none";
  container.style["-ms-user-select"]="none";
  mapDiv.style.position = "relative";
  container.style.position = "absolute";
  container.style.top = 0;
  container.style.bottom = 0;
  container.style.right = 0;
  container.style.left = 0;
  mapDiv.insertBefore(container, mapDiv.firstElementChild);


  self.set("isGoogleReady", false);
  self.set("container", container);
  self.PLUGINS = {};

  Object.defineProperty(self, "id", {
    value: mapId,
    writable: false
  });
  Object.defineProperty(self, "objects", {
    value: {},
    enumerable: false,
    writable: false
  });
  Object.defineProperty(self, "activeMarker", {
    value: null,
    enumerable: false,
    writable: true
  });
  self.set('clickable', true);


  self.one("googleready", function() {
    self.set("isGoogleReady", true);

    if (!mapTypeReg) {
      mapTypeReg = new google.maps.MapTypeRegistry();
      mapTypeReg.set('none', new google.maps.ImageMapType({
        'getTileUrl': function(point, zoom) { return null; },
        'name': 'none_type',
        'tileSize': new google.maps.Size(256, 256),
        'minZoom': 0,
        'maxZoom': 25
      }));
    }

    var mapInitOptions = {
      mapTypes: mapTypeReg,
      mapTypeId: google.maps.MapTypeId.ROADMAP,
      noClear: true,
      zoom: 2,
      minZoom: 2,
      disableDefaultUI: true,
      zoomControl: true,
      center: {lat: 0, lng: 0}
    };

    if (options) {
      if (options.mapType) {
        mapInitOptions.mapTypeId = MAP_TYPES[options.mapType];
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

    var map = new google.maps.Map(container, mapInitOptions);
    map.mapTypes = mapTypeReg;
    self.set('map', map);

    google.maps.event.addListenerOnce(map, "projection_changed", function() {
      self.trigger(event.MAP_READY);
      map.addListener("idle", self._onCameraEvent.bind(self, 'camera_move_end'));
      //map.addListener("bounce_changed", self._onCameraEvent.bind(self, 'camera_move'));
      map.addListener("drag", self._onCameraEvent.bind(self, event.CAMERA_MOVE));
      map.addListener("dragend", self._onCameraEvent.bind(self, event.CAMERA_MOVE_END));
      map.addListener("dragstart", self._onCameraEvent.bind(self, event.CAMERA_MOVE_START));

      map.addListener("click", function(evt) {
        self._onMapEvent.call(self, event.MAP_CLICK, evt);
      });
      map.addListener("mousedown", function(evt) {
        map.set('mousedown_time', Date.now());
      });
      map.addListener("mouseup", function(evt) {
        if (Date.now() - (map.get('mousedown_time') || Date.now()) > 500) {
          self._onMapEvent.call(self, event.MAP_LONG_CLICK, evt);
        }
      });
      map.addListener("drag", function(evt) {
        self._onMapEvent.call(self, event.MAP_DRAG, evt);
      });
      map.addListener("dragend", function(evt) {
        self._onMapEvent.call(self, event.MAP_DRAG_END, evt);
      });
      map.addListener("dragstart", function(evt) {
        map.set('mousedown_time', undefined);
        self._onMapEvent.call(self, event.MAP_DRAG_START, evt);
      });
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

  });

}

utils.extend(PluginMap, BaseClass);

PluginMap.prototype.setOptions = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");

  var mapInitOptions = {};

  if (options) {
    if (options.mapType) {
      mapInitOptions.mapTypeId = MAP_TYPES[options.mapType];
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
  map.setOptions(mapInitOptions);

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

  onSuccess();
};

PluginMap.prototype.setActiveMarkerId = function(onSuccess, onError, args) {
  var self = this,
    markerId = args[0];
  self.activeMarker = self.objects[markerId];
  onSuccess();
};
PluginMap.prototype.clear = function(onSuccess, onError, args) {
  self.activeMarker = null;
  onSuccess();
};

PluginMap.prototype.setDiv = function(onSuccess, onError, args) {
  var self = this,
    map = self.get('map'),
    container = self.get('container');

  if (args.length === 0) {
    if (container && container.parentNode) {
      container.parentNode.removeAttribute("__pluginMapId");
      container.parentNode.removeChild(container);
    }
  } else {
    var domId = args[0];
    var mapDiv = document.querySelector("[__pluginDomId='" + domId + "']");
    mapDiv.style.position = "relative";
    mapDiv.insertBefore(container, mapDiv.firstElementChild);
    mapDiv.setAttribute("__pluginMapId", self.id);
  }

  google.maps.event.trigger(map, "resize");
  onSuccess();
};
PluginMap.prototype.resizeMap = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");

  google.maps.event.trigger(map, "resize");
  onSuccess();
};

PluginMap.prototype.panBy = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  map.panBy.apply(map, args);
  onSuccess();
};

PluginMap.prototype.setCameraBearing = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var heading = args[0];

  map.setHeading(heading);
  onSuccess();
};

PluginMap.prototype.setCameraZoom = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var zoom = args[0];

  map.setZoom(zoom);
  onSuccess();
};

PluginMap.prototype.setCameraTarget = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var center = args[0];

  map.setCenter(center);
  onSuccess();
};

PluginMap.prototype.setCameraTilt = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var tilt = args[0];

  map.setTilt();
  onSuccess();
};

PluginMap.prototype.animateCamera = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");

  var options = args[0];
  var padding = 20 || options.padding;
  if (Array.isArray(options.target)) {
    var bounds = new google.maps.LatLngBounds();
    options.target.forEach(function(pos) {
      bounds.extend(pos);
    });
    map.panToBounds(bounds, padding);
  } else {
    if (typeof options.zoom === 'number') {
      map.setZoom(options.zoom);
    }
    if (options.target) {
      map.panTo(options.target);
    }
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
    options.target.forEach(function(pos) {
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
PluginMap.prototype.setMapTypeId = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var mapTypeId = args[0];
  map.setMapTypeId(MAP_TYPES[mapTypeId]);
  onSuccess();
};
PluginMap.prototype.setClickable = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var clickable = args[0];
  self.set('clickable', clickable);
  onSuccess();
};
PluginMap.prototype.setVisible = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var visibility = args[0];
  var mapDiv = map.getDiv();
  if (mapDiv) {
    mapDiv.style.visibility = visibility === true ? 'visible' : 'hidden';
  }
  onSuccess();
};

PluginMap.prototype.setPadding = function(onSuccess, onError, args) {
  console.warn('map.setPadding() is not available for this platform.');
  onSuccess();
};
PluginMap.prototype.setAllGesturesEnabled = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var enabled = args[0];
  map.setOptions({
    gestureHandling: enabled === true ? 'auto': 'none'
  });

  onSuccess();
};
PluginMap.prototype.setCompassEnabled = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var enabled = args[0];
  map.setOptions({
    rotateControl: enabled === true
  });
  var mapTypeId = map.getMapTypeId();
  if (mapTypeId !== google.maps.MapTypeId.SATELLITE &&
    mapTypeId !== google.maps.MapTypeId.HYBRID) {
    console.warn('map.setCompassEnabled() works only HYBRID or SATELLITE for this platform.');
  }
  onSuccess();
};
PluginMap.prototype.setTrafficEnabled = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var enabled = args[0];

  var trafficLayer = map.get('trafficLayer');
  if (!trafficLayer) {
    trafficLayer = new google.maps.TrafficLayer();
    map.set('trafficLayer', trafficLayer);
  }
  if (enabled) {
    trafficLayer.setMap(map);
  } else {
    trafficLayer.setMap(null);
  }

  onSuccess();

};

PluginMap.prototype.fromLatLngToPoint = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var lat = args[0],
    lng = args[1];

  var projection = map.getProjection(),
    bounds = map.getBounds(),
    ne = bounds.getNorthEast(),
    sw = bounds.getSouthWest(),
    zoom = map.getZoom(),
    latLng = new google.maps.LatLng(lat, lng);


  var topRight = projection.fromLatLngToPoint(ne);
  var bottomLeft = projection.fromLatLngToPoint(sw);
  var scale = Math.pow(2, zoom);
  var worldPoint = projection.fromLatLngToPoint(latLng);

  onSuccess([(worldPoint.x - bottomLeft.x) * scale, (worldPoint.y - topRight.y) * scale]);

};
PluginMap.prototype.fromPointToLatLng = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var x = args[0],
    y = args[1];

  var projection = map.getProjection(),
    bounds = map.getBounds(),
    ne = bounds.getNorthEast(),
    sw = bounds.getSouthWest(),
    zoom = map.getZoom();

  var topRight = projection.fromLatLngToPoint(ne);
  var bottomLeft = projection.fromLatLngToPoint(sw);
  var scale = Math.pow(2, zoom);
  var worldPoint = new google.maps.Point(x / scale + bottomLeft.x, y / scale + topRight.y);
  var latLng = map.getProjection().fromPointToLatLng(worldPoint);
  onSuccess([latLng.lat(), latLng.lng()]);

};

PluginMap.prototype.setIndoorEnabled = function(onSuccess, onError, args) {
  onError('map.setIndoorEnabled() is not available for this platform.');
};

PluginMap.prototype.toDataURL = function(onSuccess, onError, args) {
  onError('map.toDataURL() is not available for this platform.');
};

PluginMap.prototype._syncInfoWndPosition = function() {
  var self = this;

  if (!self.activeMarker) {
    return;
  }

  var latLng = self.activeMarker.getPosition();
  var map = self.get("map");

  var projection = map.getProjection(),
    bounds = map.getBounds(),
    ne = bounds.getNorthEast(),
    sw = bounds.getSouthWest(),
    zoom = map.getZoom();


  var topRight = projection.fromLatLngToPoint(ne);
  var bottomLeft = projection.fromLatLngToPoint(sw);
  var scale = Math.pow(2, zoom);
  var worldPoint = projection.fromLatLngToPoint(latLng);
  var x = (worldPoint.x - bottomLeft.x) * scale;
  var y = (worldPoint.y - topRight.y) * scale;

  plugin.google.maps[self.id]({
    'evtName': 'syncPosition',
    'callback': '_onSyncInfoWndPosition',
    'args': [{'x': x, 'y': y}]
  });
};

PluginMap.prototype._onMapEvent = function(evtName, evt) {
  var self = this,
    map = self.get("map");

  if (self.get('clickable') === false &&
    (evtName === event.MAP_CLICK || evtName === event.MAP_LONG_CLICK)) {
    evt.stop();
    return;
  }
  if (self.id in plugin.google.maps) {
    if (evt) {
      if (evtName === event.MAP_CLICK) {
        if (evt.placeId) {
          evt.stop();
          plugin.google.maps[self.id]({
            'evtName': event.POI_CLICK,
            'callback': '_onMapEvent',
            'args': [evt.placeId, undefined, new LatLng(evt.latLng.lat(), evt.latLng.lng())]
          });
          return;
        }
      }
      plugin.google.maps[self.id]({
        'evtName': evtName,
        'callback': '_onMapEvent',
        'args': [new LatLng(evt.latLng.lat(), evt.latLng.lng())]
      });
    } else {
      plugin.google.maps[self.id]({
        'evtName': evtName,
        'callback': '_onMapEvent',
        'args': []
      });
    }
  }

};

PluginMap.prototype._onCameraEvent = function(evtName) {
  var self = this,
    map = self.get("map"),
    center = map.getCenter(),
    bounds = map.getBounds(),
    ne = bounds.getNorthEast(),
    sw = bounds.getSouthWest();

  self._syncInfoWndPosition();

  var cameraInfo = {
    'target': {'lat': center.lat(), 'lng': center.lng()},
    'zoom': map.getZoom(),
    'tilt': map.getTilt() || 0,
    'bearing': map.getHeading() || 0,
    'northeast': {'lat': ne.lat(), 'lng': ne.lng()},
    'southwest': {'lat': sw.lat(), 'lng': sw.lng()},
    'farLeft': {'lat': ne.lat(), 'lng': sw.lng()},
    'farRight': {'lat': ne.lat(), 'lng': ne.lng()}, // = northEast
    'nearLeft': {'lat': sw.lat(), 'lng': sw.lng()}, // = southWest
    'nearRight': {'lat': sw.lat(), 'lng': ne.lng()}
  };
  if (self.id in plugin.google.maps) {
    plugin.google.maps[self.id]({
      'evtName': evtName,
      'callback': '_onCameraEvent',
      'args': [cameraInfo]
    });
  }
};

PluginMap.prototype.loadPlugin = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get("map");
  var className = args[0];

  var plugin;
  if (className in self.PLUGINS) {
    plugin = self.PLUGINS[className];
  } else {
    var OverlayClass = require('cordova-plugin-googlemaps.Plugin' + className);
    plugin = new OverlayClass(this);
    self.PLUGINS[className] = plugin;

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
    require('cordova/exec/proxy').add(self.id + '-' + className.toLowerCase(), dummyObj);
  }

  plugin._create.call(plugin, onSuccess, onError, args);
};

module.exports = PluginMap;
