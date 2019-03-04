


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng'),
  MapTypeId = require('cordova-plugin-googlemaps.MapTypeId');

var MAP_TYPES = {};
MAP_TYPES[MapTypeId.NORMAL] = 'roadmap';
MAP_TYPES[MapTypeId.ROADMAP] = 'roadmap';
MAP_TYPES[MapTypeId.SATELLITE] = 'satellite';
MAP_TYPES[MapTypeId.HYBRID] = 'hybrid';
MAP_TYPES[MapTypeId.TERRAIN] = 'terrain';
MAP_TYPES[MapTypeId.NONE] = 'none';

var LOCATION_ERROR = {};
LOCATION_ERROR[1] = 'service_denied';
LOCATION_ERROR[2] = 'not_available';
LOCATION_ERROR[3] = 'cannot_detect';
var LOCATION_ERROR_MSG = {};
LOCATION_ERROR_MSG[1] = 'Location service is rejected by user.';
LOCATION_ERROR_MSG[2] = 'Since this device does not have any location provider, this app can not detect your location.';
LOCATION_ERROR_MSG[3] = 'Can not detect your location. Try again.';

function displayGrayMap(container) {
  var gmErrorContent = document.querySelector('.gm-err-container');
  var gmnoprint = document.querySelector('.gmnoprint');
  if (!gmErrorContent && !gmnoprint) {
    container.innerHTML = [
      '<div style="position: absolute; left: 0; top: 0; width: 100%; height: 100%; background-color:rgb(229, 227, 223)">',
      '<div style="text-align: center; position: absolute; left: 0; top: 0; bottom: 0; right: 0; width: 80%; height: 100px; margin: auto; color: #616161">',
      '<img src="https://maps.gstatic.com/mapfiles/api-3/images/icon_error.png"><br>',
      '<h3>Can not display map.<br>Check the developer console.</h3>',
      '</div>',
      '</div>'
    ].join('\n');
  }
}

function PluginMap(mapId, options) {
  var self = this;
  BaseClass.apply(this);
  var mapDiv = document.querySelector('[__pluginMapId=\'' + mapId + '\']');
  mapDiv.style.backgroundColor = 'rgb(229, 227, 223)';

  var container = document.createElement('div');
  container.style.userSelect='none';
  container.style['-webkit-user-select']='none';
  container.style['-moz-user-select']='none';
  container.style['-ms-user-select']='none';
  mapDiv.style.position = 'relative';
  container.style.position = 'absolute';
  container.style.top = 0;
  container.style.bottom = 0;
  container.style.right = 0;
  container.style.left = 0;
  mapDiv.insertBefore(container, mapDiv.firstElementChild);

  self.set('isGoogleReady', false);
  self.set('container', container);
  self.PLUGINS = {};

  Object.defineProperty(self, '__pgmId', {
    value: mapId,
    writable: false
  });
  Object.defineProperty(self, 'objects', {
    value: {},
    writable: false
  });
  Object.defineProperty(self, 'activeMarker', {
    value: null,
    writable: true
  });
  self.set('clickable', true);


  self.one('googleready', function() {
    self.set('isGoogleReady', true);

    var mapTypeReg = new google.maps.MapTypeRegistry();
    mapTypeReg.set('none', new google.maps.ImageMapType({
      'getTileUrl': function() { return null; },
      'name': 'none_type',
      'tileSize': new google.maps.Size(256, 256),
      'minZoom': 0,
      'maxZoom': 25
    }));

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
      if (options.styles) {
        mapInitOptions.styles = JSON.parse(options.styles);
      }

      if (options.controls) {
        if (options.controls.zoom !== undefined) {
          mapInitOptions.zoomControl = options.controls.zoom == true;
        }
      }
      if (options.preferences) {
        if (options.preferences.zoom) {
          mapInitOptions.minZoom = options.preferences.zoom.minZoom;
          if (options.preferences.zoom.maxZoom) {
            mapInitOptions.maxZoom = options.preferences.zoom.maxZoom;
          }
        }
      }
    }

    var map = new google.maps.Map(container, mapInitOptions);
    map.mapTypes = mapTypeReg;
    self.set('map', map);

    var boundsLimit = null;
    if (options.preferences && options.preferences.gestureBounds &&
        options.preferences.gestureBounds.length > 0) {
      boundsLimit = new google.maps.LatLngBounds();
      options.preferences.gestureBounds.forEach(function(pos) {
        boundsLimit.extend(pos);
      });
    }
    map.set('boundsLimit', boundsLimit);

    var timeoutError = setTimeout(function() {
      self.trigger('load_error');
      displayGrayMap(mapDiv);
    }, 3000);

    map.addListener('bounds_changed', function() {
      var boundsLimit = map.get('boundsLimit');
      if (!boundsLimit) {
        return;
      }
      var visibleBounds = map.getBounds();
      if (boundsLimit.intersects(visibleBounds) ||
          visibleBounds.contains(boundsLimit.getNorthEast()) && visibleBounds.contains(boundsLimit.getSouthWest()) ||
          boundsLimit.contains(visibleBounds.getNorthEast()) && boundsLimit.contains(visibleBounds.getSouthWest())) {
        return;
      }
      var center = map.getCenter();
      var dummyLat = center.lat(),
        dummyLng = center.lng();
      var ne = boundsLimit.getNorthEast(),
        sw = boundsLimit.getSouthWest();
      if (dummyLat < sw.lat() ) {
        dummyLat = sw.lat();
      } else if (dummyLat > ne.lat()) {
        dummyLat = ne.lat();
      }
      if (dummyLng < 0) {
        // the Western Hemisphere
        if (dummyLng > ne.lng()) {
          dummyLng = ne.lng();
        } else if (dummyLng < sw.lng()) {
          dummyLng = sw.lng();
        }
      } else {
        // the Eastern Hemisphere
        if (dummyLng > ne.lng()) {
          dummyLng = ne.lng();
        } else if (dummyLng < sw.lng()) {
          dummyLng = sw.lng();
        }
      }
      var dummyLatLng = new google.maps.LatLng(dummyLat, dummyLng);
      map.panTo(dummyLatLng);
    });

    google.maps.event.addListenerOnce(map, 'projection_changed', function() {
      clearTimeout(timeoutError);

      self.trigger(event.MAP_READY);
      map.addListener('idle', self._onCameraEvent.bind(self, 'camera_move_end'));
      //map.addListener("bounce_changed", self._onCameraEvent.bind(self, 'camera_move'));
      map.addListener('drag', self._onCameraEvent.bind(self, event.CAMERA_MOVE));
      map.addListener('dragend', self._onCameraEvent.bind(self, event.CAMERA_MOVE_END));
      map.addListener('dragstart', self._onCameraEvent.bind(self, event.CAMERA_MOVE_START));

      map.addListener('click', function(evt) {
        self._onMapEvent.call(self, event.MAP_CLICK, evt);
      });
      map.addListener('mousedown', function() {
        map.set('mousedown_time', Date.now());
      });
      map.addListener('mouseup', function(evt) {
        if (Date.now() - (map.get('mousedown_time') || Date.now()) > 500) {
          self._onMapEvent.call(self, event.MAP_LONG_CLICK, evt);
        }
      });
      map.addListener('drag', function(evt) {
        self._onMapEvent.call(self, event.MAP_DRAG, evt);
      });
      map.addListener('dragend', function(evt) {
        self._onMapEvent.call(self, event.MAP_DRAG_END, evt);
      });
      map.addListener('dragstart', function(evt) {
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
            map.fitBounds(bounds, 5);
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
  var map = self.get('map'),
    options = args[0];

  var mapInitOptions = {};

  if (options) {
    if (options.mapType) {
      mapInitOptions.mapTypeId = MAP_TYPES[options.mapType];
    }
    if (options.styles) {
      mapInitOptions.styles = JSON.parse(options.styles);
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

      if ('gestureBounds' in options.preferences) {
        var boundsLimit = null;
        if (options.preferences.gestureBounds && options.preferences.gestureBounds.length > 0) {
          boundsLimit = new google.maps.LatLngBounds();
          options.preferences.gestureBounds.forEach(function(pos) {
            boundsLimit.extend(pos);
          });
        }
        map.set('boundsLimit', boundsLimit);
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
          map.fitBounds(bounds, 5);
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
PluginMap.prototype.clear = function(onSuccess) {
  this.activeMarker = null;
  onSuccess();
};

PluginMap.prototype.getFocusedBuilding = function(onSuccess) {
  // stub
  onSuccess(-1);
};

PluginMap.prototype.setDiv = function(onSuccess, onError, args) {
  var self = this,
    map = self.get('map'),
    container = self.get('container');

  if (args.length === 0) {
    if (container && container.parentNode) {
      container.parentNode.removeAttribute('__pluginMapId');
      container.parentNode.removeChild(container);
    }
  } else {
    var domId = args[0];
    var mapDiv = document.querySelector('[__pluginDomId=\'' + domId + '\']');
    mapDiv.style.position = 'relative';
    mapDiv.insertBefore(container, mapDiv.firstElementChild);
    mapDiv.setAttribute('__pluginMapId', self.__pgmId);
  }

  google.maps.event.trigger(map, 'resize');
  onSuccess();
};
PluginMap.prototype.resizeMap = function(onSuccess) {
  var self = this;
  var map = self.get('map');

  google.maps.event.trigger(map, 'resize');
  onSuccess();
};

PluginMap.prototype.panBy = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  map.panBy.apply(map, args);
  onSuccess();
};

PluginMap.prototype.setCameraBearing = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  var heading = args[0];

  map.setHeading(heading);
  onSuccess();
};

PluginMap.prototype.setCameraZoom = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  var zoom = args[0];

  map.setZoom(zoom);
  onSuccess();
};

PluginMap.prototype.setCameraTarget = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  var lat = args[0],
    lng = args[1];

  map.setCenter(new google.maps.LatLng(lat, lng));
  onSuccess();
};

PluginMap.prototype.setCameraTilt = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  var tilt = args[0];

  map.setTilt(tilt);
  onSuccess();
};
PluginMap.prototype.setMyLocationEnabled = function(onSuccess) {
  // stub
  onSuccess();
};

PluginMap.prototype.animateCamera = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');

  var options = args[0];
  var padding = 'padding' in options ? options.padding : 5;
  var bounds;
  if (Array.isArray(options.target)) {
    bounds = new google.maps.LatLngBounds();
    options.target.forEach(function(pos) {
      bounds.extend(pos);
    });
    map.fitBounds(bounds, padding);
  } else {
    var zoomFlag = typeof options.zoom === 'number';
    var targetFlag = !!options.target;

    if (zoomFlag && targetFlag) {
      var projection = map.getProjection();
      var centerLatLng = new google.maps.LatLng(options.target.lat, options.target.lng, true);
      var centerPoint = projection.fromLatLngToPoint(centerLatLng);

      var scale = Math.pow(2, options.zoom);

      var div = map.getDiv();
      var harfWidth = div.offsetWidth / 2;
      var harfHeight = div.offsetHeight / 2;
      var swPoint = new google.maps.Point((centerPoint.x * scale - harfWidth) / scale, (centerPoint.y * scale + harfHeight) / scale );
      var nePoint = new google.maps.Point((centerPoint.x * scale + harfWidth) / scale, (centerPoint.y * scale - harfHeight)  / scale);
      var sw = projection.fromPointToLatLng(swPoint);
      var ne = projection.fromPointToLatLng(nePoint);
      bounds = new google.maps.LatLngBounds(sw, ne);
      map.fitBounds(bounds, padding);

    } else if (zoomFlag) {
      map.setZoom(options.zoom);
    } else if (targetFlag) {
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
  this.animateCamera.call(this, onSuccess, onError, args);
};

PluginMap.prototype.setMapTypeId = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  var mapTypeId = args[0];
  map.setMapTypeId(MAP_TYPES[mapTypeId]);
  onSuccess();
};
PluginMap.prototype.setClickable = function(onSuccess, onError, args) {
  var self = this;
  var clickable = args[0];
  self.set('clickable', clickable);
  onSuccess();
};
PluginMap.prototype.setVisible = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  var visibility = args[0];
  var mapDiv = map.getDiv();
  if (mapDiv) {
    mapDiv.style.visibility = visibility === true ? 'visible' : 'hidden';
  }
  onSuccess();
};

PluginMap.prototype.setPadding = function(onSuccess) {
  // stub
  onSuccess();
};
PluginMap.prototype.setAllGesturesEnabled = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
  var enabled = args[0];
  map.setOptions({
    gestureHandling: enabled === true ? 'auto': 'none'
  });

  onSuccess();
};
PluginMap.prototype.setCompassEnabled = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
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
  var map = self.get('map');
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
  var map = self.get('map');
  var lat = args[0],
    lng = args[1];

  var projection = map.getProjection(),
    bounds = map.getBounds(),
    ne = bounds.getNorthEast(),
    sw = bounds.getSouthWest(),
    zoom = map.getZoom(),
    north = ne.lat(),
    west = sw.lng();

  var nowrapFlag = !bounds.contains(new google.maps.LatLng(north, 179));

  var scale = Math.pow(2, zoom),
    topLeft = projection.fromLatLngToPoint(new google.maps.LatLng(north, west + 360, nowrapFlag)),
    worldPoint = projection.fromLatLngToPoint(new google.maps.LatLng(lat, lng + 360, true));
  onSuccess([(worldPoint.x - topLeft.x) * scale, (worldPoint.y - topLeft.y) * scale]);
};

PluginMap.prototype.fromPointToLatLng = function(onSuccess, onError, args) {
  var self = this;
  var map = self.get('map');
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

PluginMap.prototype.setIndoorEnabled = function(onSuccess) {
  // stub
  onSuccess();
};

PluginMap.prototype.toDataURL = function(onSuccess) {
  // stub
  onSuccess();
};

PluginMap.prototype._syncInfoWndPosition = function() {
  var self = this;

  if (!self.activeMarker) {
    return;
  }

  var latLng = self.activeMarker.getPosition();
  self.fromLatLngToPoint(function(point) {

    plugin.google.maps[self.__pgmId]({
      'evtName': 'syncPosition',
      'callback': '_onSyncInfoWndPosition',
      'args': [{'x': point[0], 'y': point[1]}]
    });

  }, null, [latLng.lat(), latLng.lng()]);

};

PluginMap.prototype._onMapEvent = function(evtName, evt) {
  var self = this;

  if (self.get('clickable') === false &&
    (evtName === event.MAP_CLICK || evtName === event.MAP_LONG_CLICK)) {
    evt.stop();
    return;
  }
  if (self.__pgmId in plugin.google.maps) {
    if (evt) {
      if (evtName === event.MAP_CLICK) {
        if (evt.placeId) {
          evt.stop();
          plugin.google.maps[self.__pgmId]({
            'evtName': event.POI_CLICK,
            'callback': '_onMapEvent',
            'args': [evt.placeId, undefined, new LatLng(evt.latLng.lat(), evt.latLng.lng())]
          });
          return;
        }
      }
      plugin.google.maps[self.__pgmId]({
        'evtName': evtName,
        'callback': '_onMapEvent',
        'args': [new LatLng(evt.latLng.lat(), evt.latLng.lng())]
      });
    } else {
      plugin.google.maps[self.__pgmId]({
        'evtName': evtName,
        'callback': '_onMapEvent',
        'args': []
      });
    }
  }

};

PluginMap.prototype._onCameraEvent = function(evtName) {
  var self = this,
    map = self.get('map'),
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
  if (self.__pgmId in plugin.google.maps) {
    plugin.google.maps[self.__pgmId]({
      'evtName': evtName,
      'callback': '_onCameraEvent',
      'args': [cameraInfo]
    });
  }
};

PluginMap.prototype.loadPlugin = function(onSuccess, onError, args) {
  var self = this;
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
    for (var key in OverlayClass.prototype) {
      if (typeof OverlayClass.prototype[key] === 'function') {
        dummyObj[key] = plugin[key].bind(plugin);
      } else {
        dummyObj[key] = plugin[key];
      }
    }
    require('cordova/exec/proxy').add(self.__pgmId + '-' + className.toLowerCase(), dummyObj);
  }

  plugin._create.call(plugin, onSuccess, onError, args);
};

module.exports = PluginMap;
