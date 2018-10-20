


var utils = require('cordova/utils'),
  cordova_exec = require('cordova/exec'),
  common = require('./Common'),
  Overlay = require('./Overlay'),
  BaseClass = require('./BaseClass'),
  BaseArrayClass = require('./BaseArrayClass'),
  LatLng = require('./LatLng'),
  MapTypeId = require('./MapTypeId'),
  event = require('./event'),
  VisibleRegion = require('./VisibleRegion'),
  Marker = require('./Marker'),
  Circle = require('./Circle'),
  Polyline = require('./Polyline'),
  Polygon = require('./Polygon'),
  TileOverlay = require('./TileOverlay'),
  GroundOverlay = require('./GroundOverlay'),
  KmlOverlay = require('./KmlOverlay'),
  KmlLoader = require('./KmlLoader'),
  FusionTableOverlay = require('./FusionTableOverlay'),
  MarkerCluster = require('./MarkerCluster');

/**
 * Google Maps model.
 */
var exec;
var Map = function(__pgmId, _exec) {
  var self = this;
  exec = _exec;
  Overlay.call(self, self, {}, 'Map', _exec, {
    __pgmId: __pgmId
  });
  delete self.map;


  self.set('myLocation', false);
  self.set('myLocationButton', false);

  self.MARKERS = {};
  self.OVERLAYS = {};

  var infoWindowLayer = document.createElement('div');
  infoWindowLayer.style.position = 'absolute';
  infoWindowLayer.style.left = 0;
  infoWindowLayer.style.top = 0;
  infoWindowLayer.style.width = 0;
  infoWindowLayer.style.height = 0;
  infoWindowLayer.style.overflow = 'visible';
  infoWindowLayer.style['z-index'] = 1;

  Object.defineProperty(self, '_layers', {
    value: {
      info: infoWindowLayer
    },
    enumerable: false,
    writable: false
  });

  self.on(event.MAP_CLICK, function() {
    self.set('active_marker', undefined);
  });

  self.on('active_marker_changed', function(prevMarker, newMarker) {
    var newMarkerId = newMarker ? newMarker.getId() : null;
    if (prevMarker) {
      prevMarker.hideInfoWindow.call(prevMarker);
    }
    self.exec.call(self, null, null, self.__pgmId, 'setActiveMarkerId', [newMarkerId]);
  });
};

utils.extend(Map, Overlay);

/**
 * @desc Recalculate the position of HTML elements
 */
Map.prototype.refreshLayout = function() {
  this.exec.call(this, null, null, this.__pgmId, 'resizeMap', []);
};

Map.prototype.getMap = function(meta, div, options) {
  var self = this,
    args = [meta];
  options = options || {};

  self.set('clickable', options.clickable === false ? false : true);
  self.set('visible', options.visible === false ? false : true);

  if (options.controls) {
    this.set('myLocation', options.controls.myLocation === true);
    this.set('myLocationButton', options.controls.myLocationButton === true);
  }

  if (options.preferences && options.preferences.gestureBounds) {
    if (utils.isArray(options.preferences.gestureBounds) ||
        options.preferences.gestureBounds.type === 'LatLngBounds') {
      options.preferences.gestureBounds = common.convertToPositionArray(options.preferences.gestureBounds);
    }
  }

  if (!common.isDom(div)) {
    self.set('visible', false);
    options = div;
    options = options || {};
    if (options.camera) {
      if (options.camera.latLng) {
        options.camera.target = options.camera.latLng;
        delete options.camera.latLng;
      }
      this.set('camera', options.camera);
      if (options.camera.target) {
        this.set('camera_target', options.camera.target);
      }
      if (options.camera.bearing) {
        this.set('camera_bearing', options.camera.bearing);
      }
      if (options.camera.zoom) {
        this.set('camera_zoom', options.camera.zoom);
      }
      if (options.camera.tilt) {
        this.set('camera_tilt', options.camera.tilt);
      }
    }
    args.push(options);
  } else {

    var positionCSS = common.getStyle(div, 'position');
    if (!positionCSS || positionCSS === 'static') {
      // important for HtmlInfoWindow
      div.style.position = 'relative';
    }
    options = options || {};
    if (options.camera) {
      if (options.camera.latLng) {
        options.camera.target = options.camera.latLng;
        delete options.camera.latLng;
      }
      this.set('camera', options.camera);
      if (options.camera.target) {
        this.set('camera_target', options.camera.target);
      }
      if (options.camera.bearing) {
        this.set('camera_bearing', options.camera.bearing);
      }
      if (options.camera.zoom) {
        this.set('camera_zoom', options.camera.zoom);
      }
      if (options.camera.tilt) {
        this.set('camera_tilt', options.camera.tilt);
      }
    }
    if (utils.isArray(options.styles)) {
      options.styles = JSON.stringify(options.styles);
    }
    args.push(options);

    div.style.overflow = 'hidden';
    self.set('div', div);

    if (div.offsetWidth < 100 || div.offsetHeight < 100) {
      // If the map Div is too small, wait a little.
      var callee = arguments.callee;
      setTimeout(function() {
        callee.call(self, meta, div, options);
      }, 250 + Math.random() * 100);
      return;
    }

    // Gets the map div size.
    // The plugin needs to consider the viewport zoom ratio
    // for the case window.innerHTML > body.offsetWidth.
    var elemId = common.getPluginDomId(div);
    args.push(elemId);

  }

  exec.call({
    _isReady: true
  }, function() {

    //------------------------------------------------------------------------
    // Clear background colors of map div parents after the map is created
    //------------------------------------------------------------------------
    var div = self.get('div');
    if (common.isDom(div)) {

      // Insert the infoWindow layer
      if (self._layers.info.parentNode) {
        try {
          self._layers.info.parentNode.removeChild(self._layers.info.parentNode);
        } catch (e) {
          // ignore
        }
      }
      var positionCSS;
      for (var i = 0; i < div.children.length; i++) {
        positionCSS = common.getStyle(div.children[i], 'position');
        if (positionCSS === 'static') {
          div.children[i].style.position = 'relative';
        }
      }
      div.insertBefore(self._layers.info, div.firstChild);


      while (div.parentNode) {
        div.style.backgroundColor = 'rgba(0,0,0,0) !important';

        // Add _gmaps_cdv_ class
        common.attachTransparentClass(div);

        div = div.parentNode;
      }
    }
    cordova.fireDocumentEvent('plugin_touch', {
      force: true
    });

    //------------------------------------------------------------------------
    // In order to work map.getVisibleRegion() correctly, wait a little.
    //------------------------------------------------------------------------
    var waitCnt = 0;
    var waitCameraSync = function() {
      if (!self.getVisibleRegion() && (waitCnt++ < 10)) {
        setTimeout(function() {
          common.nextTick(waitCameraSync);
        }, 100);
        return;
      }


      self._privateInitialize();
      delete self._privateInitialize;
      self.refreshLayout();
      self.trigger(event.MAP_READY, self);
    };
    setTimeout(function() {
      common.nextTick(waitCameraSync);
    }, 100);
  }, self.errorHandler, 'CordovaGoogleMaps', 'getMap', args, {
    sync: true
  });
};

Map.prototype.setOptions = function(options) {
  options = options || {};

  if (options.controls) {
    var myLocation = this.get('myLocation');
    if ('myLocation' in options.controls) {
      myLocation = options.controls.myLocation === true;
    }
    var myLocationButton = this.get('myLocationButton');
    if ('myLocationButton' in options.controls) {
      myLocationButton = options.controls.myLocationButton === true;
    }
    this.set('myLocation', myLocation);
    this.set('myLocationButton', myLocationButton);
    if (myLocation === true || myLocation === false) {
      options.controls.myLocation = myLocation;
    }
    if (myLocationButton === true || myLocationButton === false) {
      options.controls.myLocationButton = myLocationButton;
    }
  }
  if (options.camera) {
    if (options.camera.latLng) {
      options.camera.target = options.camera.latLng;
      delete options.camera.latLng;
    }
    this.set('camera', options.camera);
    if (options.camera.target) {
      this.set('camera_target', options.camera.target);
    }
    if (options.camera.bearing) {
      this.set('camera_bearing', options.camera.bearing);
    }
    if (options.camera.zoom) {
      this.set('camera_zoom', options.camera.zoom);
    }
    if (options.camera.tilt) {
      this.set('camera_tilt', options.camera.tilt);
    }
  }

  if (options.preferences && options.preferences.gestureBounds) {
    if (utils.isArray(options.preferences.gestureBounds) ||
        options.preferences.gestureBounds.type === 'LatLngBounds') {
      options.preferences.gestureBounds = common.convertToPositionArray(options.preferences.gestureBounds);
    }
  }

  if (utils.isArray(options.styles)) {
    options.styles = JSON.stringify(options.styles);
  }
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setOptions', [options]);
  return this;
};

Map.prototype.getMyLocation = function(params, success_callback, error_callback) {
  return window.plugin.google.maps.LocationService.getMyLocation.call(this, params, success_callback, error_callback);
};

Map.prototype.setCameraTarget = function(latLng) {
  this.set('camera_target', latLng);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setCameraTarget', [latLng.lat, latLng.lng]);
  return this;
};

Map.prototype.setCameraZoom = function(zoom) {
  this.set('camera_zoom', zoom);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setCameraZoom', [zoom], {
    sync: true
  });
  return this;
};
Map.prototype.panBy = function(x, y) {
  x = parseInt(x, 10);
  y = parseInt(y, 10);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'panBy', [x, y], {
    sync: true
  });
  return this;
};

/**
 * Clears all markup that has been added to the map,
 * including markers, polylines and ground overlays.
 */
Map.prototype.clear = function(callback) {
  var self = this;
  if (self._isRemoved) {
    // Simply ignore because this map is already removed.
    return Promise.resolve();
  }

  // Close the active infoWindow
  var active_marker = self.get('active_marker');
  if (active_marker) {
    active_marker.trigger(event.INFO_CLOSE);
  }

  var clearObj = function(obj) {
    var ids = Object.keys(obj);
    var id, instance;
    for (var i = 0; i < ids.length; i++) {
      id = ids[i];
      instance = obj[id];
      if (instance) {
        if (typeof instance.remove === 'function') {
          instance.remove();
        }
        instance.off();
        delete obj[id];
      }
    }
    obj = {};
  };

  clearObj(self.OVERLAYS);
  clearObj(self.MARKERS);
  self.trigger('map_clear');

  var resolver = function(resolve, reject) {
    self.exec.call(self,
      resolve.bind(self),
      reject.bind(self),
      self.__pgmId, 'clear', [], {
        sync: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }

};

/**
 * @desc Change the map type
 * @param {String} mapTypeId   Specifies the one of the follow strings:
 *                               MAP_TYPE_HYBRID
 *                               MAP_TYPE_SATELLITE
 *                               MAP_TYPE_TERRAIN
 *                               MAP_TYPE_NORMAL
 *                               MAP_TYPE_NONE
 */
Map.prototype.setMapTypeId = function(mapTypeId) {
  if (mapTypeId !== MapTypeId[mapTypeId.replace('MAP_TYPE_', '')]) {
    return this.errorHandler('Invalid MapTypeId was specified.');
  }
  this.set('mapTypeId', mapTypeId);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setMapTypeId', [mapTypeId]);
  return this;
};

/**
 * @desc Change the map view angle
 * @param {Number} tilt  The angle
 */
Map.prototype.setCameraTilt = function(tilt) {
  this.set('camera_tilt', tilt);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setCameraTilt', [tilt], {
    sync: true
  });
  return this;
};

/**
 * @desc Change the map view bearing
 * @param {Number} bearing  The bearing
 */
Map.prototype.setCameraBearing = function(bearing) {
  this.set('camera_bearing', bearing);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setCameraBearing', [bearing], {
    sync: true
  });
  return this;
};

Map.prototype.moveCameraZoomIn = function(callback) {
  var self = this;
  var cameraPosition = self.get('camera');
  cameraPosition.zoom++;
  cameraPosition.zoom = cameraPosition.zoom < 0 ? 0 : cameraPosition.zoom;

  return self.moveCamera(cameraPosition, callback);

};
Map.prototype.moveCameraZoomOut = function(callback) {
  var self = this;
  var cameraPosition = self.get('camera');
  cameraPosition.zoom--;
  cameraPosition.zoom = cameraPosition.zoom < 0 ? 0 : cameraPosition.zoom;

  return self.moveCamera(cameraPosition, callback);
};
Map.prototype.animateCameraZoomIn = function(callback) {
  var self = this;
  var cameraPosition = self.get('camera');
  cameraPosition.zoom++;
  cameraPosition.zoom = cameraPosition.zoom < 0 ? 0 : cameraPosition.zoom;
  cameraPosition.duration = 500;
  return self.animateCamera(cameraPosition, callback);
};
Map.prototype.animateCameraZoomOut = function(callback) {
  var self = this;
  var cameraPosition = self.get('camera');
  cameraPosition.zoom--;
  cameraPosition.zoom = cameraPosition.zoom < 0 ? 0 : cameraPosition.zoom;
  cameraPosition.duration = 500;
  return self.animateCamera(cameraPosition, callback);
};
/**
 * @desc   Move the map camera with animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
Map.prototype.animateCamera = function(cameraPosition, callback) {
  var self = this;

  var target = cameraPosition.target;
  if (!target && 'position' in cameraPosition) {
    target = cameraPosition.position;
  }
  if (!target) {
    return Promise.reject('No target field is specified.');
  }
  // if (!('padding' in cameraPosition)) {
  //   cameraPosition.padding = 10;
  // }

  if (utils.isArray(target) || target.type === 'LatLngBounds') {
    target = common.convertToPositionArray(target);
    if (target.length === 0) {
      // skip if no point is specified
      if (typeof callback === 'function') {
        callback.call(self);
        return;
      } else {
        return Promise.reject('No point is specified.');
      }
    }
  }
  cameraPosition.target = target;

  var resolver = function(resolve, reject) {

    self.exec.call(self,
      resolve.bind(self),
      reject.bind(self),
      self.__pgmId, 'animateCamera', [cameraPosition], {
        sync: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }
};
/**
 * @desc   Move the map camera without animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
Map.prototype.moveCamera = function(cameraPosition, callback) {
  var self = this;
  var target = cameraPosition.target;
  if (!target && 'position' in cameraPosition) {
    target = cameraPosition.position;
  }
  if (!target) {
    return Promise.reject('No target field is specified.');
  }

  // if (!('padding' in cameraPosition)) {
  //   cameraPosition.padding = 10;
  // }
  if (utils.isArray(target) || target.type === 'LatLngBounds') {
    target = common.convertToPositionArray(target);
    if (target.length === 0) {
      // skip if no point is specified
      if (typeof callback === 'function') {
        callback.call(self);
        return;
      } else {
        return Promise.reject('No point is specified.');
      }
    }
  }
  cameraPosition.target = target;

  var resolver = function(resolve, reject) {

    self.exec.call(self,
      resolve.bind(self),
      reject.bind(self),
      self.__pgmId, 'moveCamera', [cameraPosition], {
        sync: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }
};

Map.prototype.setMyLocationButtonEnabled = function(enabled) {
  var self = this;
  enabled = common.parseBoolean(enabled);
  this.set('myLocationButton', enabled);
  self.exec.call(self, null, this.errorHandler, this.__pgmId, 'setMyLocationEnabled', [{
    myLocationButton: enabled,
    myLocation: self.get('myLocation') === true
  }], {
    sync: true
  });
  return this;
};

Map.prototype.setMyLocationEnabled = function(enabled) {
  var self = this;
  enabled = common.parseBoolean(enabled);
  this.set('myLocation', enabled);
  self.exec.call(self, null, this.errorHandler, this.__pgmId, 'setMyLocationEnabled', [{
    myLocationButton: self.get('myLocationButton') === true,
    myLocation: enabled
  }], {
    sync: true
  });
  return this;
};

Map.prototype.setIndoorEnabled = function(enabled) {
  enabled = common.parseBoolean(enabled);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setIndoorEnabled', [enabled]);
  return this;
};
Map.prototype.setTrafficEnabled = function(enabled) {
  enabled = common.parseBoolean(enabled);
  this.exec.call(this, null, this.errorHandler, this.__pgmId, 'setTrafficEnabled', [enabled]);
  return this;
};
Map.prototype.setCompassEnabled = function(enabled) {
  var self = this;
  enabled = common.parseBoolean(enabled);
  self.exec.call(self, null, self.errorHandler, this.__pgmId, 'setCompassEnabled', [enabled]);
  return this;
};
Map.prototype.getFocusedBuilding = function(callback) {
  var self = this;
  var resolver = function(resolve, reject) {
    self.exec.call(self,
      resolve.bind(self),
      reject.bind(self),
      self.__pgmId, 'getFocusedBuilding', []);
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }
};
Map.prototype.getVisible = function() {
  return this.get('visible');
};
Map.prototype.setVisible = function(isVisible) {
  cordova.fireDocumentEvent('plugin_touch');
  var self = this;
  isVisible = common.parseBoolean(isVisible);
  self.set('visible', isVisible);
  self.exec.call(self, null, self.errorHandler, this.__pgmId, 'setVisible', [isVisible]);
  return this;
};

Map.prototype.setClickable = function(isClickable) {
  cordova.fireDocumentEvent('plugin_touch');
  var self = this;
  isClickable = common.parseBoolean(isClickable);
  self.set('clickable', isClickable);
  self.exec.call(self, null, self.errorHandler, this.__pgmId, 'setClickable', [isClickable]);
  return this;
};
Map.prototype.getClickable = function() {
  return this.get('clickable');
};


/**
 * Sets the preference for whether all gestures should be enabled or disabled.
 */
Map.prototype.setAllGesturesEnabled = function(enabled) {
  var self = this;
  enabled = common.parseBoolean(enabled);
  self.exec.call(self, null, self.errorHandler, this.__pgmId, 'setAllGesturesEnabled', [enabled]);
  return this;
};

/**
 * Return the current position of the camera
 * @return {CameraPosition}
 */
Map.prototype.getCameraPosition = function() {
  return this.get('camera');
};

/**
 * Remove the map completely.
 */
Map.prototype.remove = function(callback) {
  var self = this;
  if (self._isRemoved) {
    return;
  }
  Object.defineProperty(self, '_isRemoved', {
    value: true,
    writable: false
  });

  self.trigger('remove');
  // var div = self.get('div');
  // if (div) {
  //   while (div) {
  //     if (div.style) {
  //       div.style.backgroundColor = '';
  //     }
  //     if (div.classList) {
  //       div.classList.remove('_gmaps_cdv_');
  //     } else if (div.className) {
  //       div.className = div.className.replace(/_gmaps_cdv_/g, '');
  //       div.className = div.className.replace(/\s+/g, ' ');
  //     }
  //     div = div.parentNode;
  //   }
  // }
  // self.set('div', undefined);


  // Close the active infoWindow
  var active_marker = self.get('active_marker');
  if (active_marker) {
    active_marker.trigger(event.INFO_CLOSE);
  }

  var clearObj = function(obj) {
    var ids = Object.keys(obj);
    var id, instance;
    for (var i = 0; i < ids.length; i++) {
      id = ids[i];
      instance = obj[id];
      if (instance) {
        if (typeof instance.remove === 'function') {
          instance.remove();
        }
        instance.off();
        delete obj[id];
      }
    }
    obj = {};
  };

  clearObj(self.OVERLAYS);
  clearObj(self.MARKERS);


  var resolver = function(resolve, reject) {
    self.exec.call(self,
      resolve.bind(self),
      reject.bind(self),
      'CordovaGoogleMaps', 'removeMap', [self.__pgmId],
      {
        sync: true,
        remove: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }
};


Map.prototype.toDataURL = function(params, callback) {
  var args = [params || {}, callback];
  if (typeof args[0] === 'function') {
    args.unshift({});
  }

  params = args[0];
  callback = args[1];

  params.uncompress = params.uncompress === true;
  var self = this;

  var resolver = function(resolve, reject) {
    self.exec.call(self,
      resolve.bind(self),
      reject.bind(self),
      self.__pgmId, 'toDataURL', [params]);
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }
};

/**
 * Show the map into the specified div.
 */
Map.prototype.getDiv = function() {
  return this.get('div');
};

/**
 * Show the map into the specified div.
 */
Map.prototype.setDiv = function(div) {
  var self = this,
    args = [];

  if (!common.isDom(div)) {
    div = self.get('div');
    if (common.isDom(div)) {
      div.removeAttribute('__pluginMapId');
    }
    self.set('div', null);
  } else {
    div.setAttribute('__pluginMapId', self.__pgmId);

    // Insert the infoWindow layer
    if (self._layers.info.parentNode) {
      try {
        self._layers.info.parentNode.removeChild(self._layers.info.parentNode);
      } catch(e) {
        //ignore
      }
    }
    var positionCSS;
    for (var i = 0; i < div.children.length; i++) {
      positionCSS = common.getStyle(div.children[i], 'position');
      if (positionCSS === 'static') {
        div.children[i].style.position = 'relative';
      }
    }
    div.insertBefore(self._layers.info, div.firstChild);

    // Webkit redraw mandatory
    // http://stackoverflow.com/a/3485654/697856
    div.style.display = 'none';
    div.offsetHeight;
    div.style.display = '';

    self.set('div', div);

    if (cordova.platform === 'browser') {
      return;
    }


    positionCSS = common.getStyle(div, 'position');
    if (!positionCSS || positionCSS === 'static') {
      div.style.position = 'relative';
    }
    var elemId = common.getPluginDomId(div);
    args.push(elemId);
    while (div.parentNode) {
      div.style.backgroundColor = 'rgba(0,0,0,0)';

      // Add _gmaps_cdv_ class
      common.attachTransparentClass(div);

      div = div.parentNode;
    }
  }
  self.exec.call(self, function() {
    cordova.fireDocumentEvent('plugin_touch', {
      force: true,
      action: 'setDiv'
    });
    self.refreshLayout();
  }, self.errorHandler, self.__pgmId, 'setDiv', args, {
    sync: true
  });
  return self;
};

/**
 * Return the visible region of the map.
 */
Map.prototype.getVisibleRegion = function(callback) {
  var self = this;
  var cameraPosition = self.get('camera');
  if (!cameraPosition || !cameraPosition.southwest || !cameraPosition.northeast) {
    return null;
  }

  var latLngBounds = new VisibleRegion(
    cameraPosition.southwest,
    cameraPosition.northeast,
    cameraPosition.farLeft,
    cameraPosition.farRight,
    cameraPosition.nearLeft,
    cameraPosition.nearRight
  );

  if (typeof callback === 'function') {
    console.log('[deprecated] getVisibleRegion() is changed. Please check out the https://goo.gl/yHstHQ');
    callback.call(self, latLngBounds);
  }
  return latLngBounds;
};

/**
 * Maps an Earth coordinate to a point coordinate in the map's view.
 */
Map.prototype.fromLatLngToPoint = function(latLng, callback) {
  var self = this;

  if ('lat' in latLng && 'lng' in latLng) {

    var resolver = function(resolve, reject) {
      self.exec.call(self,
        resolve.bind(self),
        reject.bind(self),
        self.__pgmId, 'fromLatLngToPoint', [latLng.lat, latLng.lng]);
    };

    if (typeof callback === 'function') {
      resolver(callback, self.errorHandler);
    } else {
      return new Promise(resolver);
    }
  } else {
    var rejector = function(resolve, reject) {
      reject('The latLng is invalid');
    };

    if (typeof callback === 'function') {
      rejector(callback, self.errorHandler);
    } else {
      return new Promise(rejector);
    }
  }

};
/**
 * Maps a point coordinate in the map's view to an Earth coordinate.
 */
Map.prototype.fromPointToLatLng = function(pixel, callback) {
  var self = this;
  if (typeof pixel === 'object' && 'x' in pixel && 'y' in pixel) {
    pixel = [pixel.x, pixel.y];
  }
  if (pixel.length == 2 && utils.isArray(pixel)) {

    var resolver = function(resolve, reject) {
      self.exec.call(self,
        function(result) {
          var latLng = new LatLng(result[0] || 0, result[1] || 0);
          resolve.call(self, latLng);
        },
        reject.bind(self),
        self.__pgmId, 'fromPointToLatLng', [pixel[0], pixel[1]]);
    };

    if (typeof callback === 'function') {
      resolver(callback, self.errorHandler);
    } else {
      return new Promise(resolver);
    }
  } else {
    var rejector = function(resolve, reject) {
      reject('The pixel[] argument is invalid');
    };

    if (typeof callback === 'function') {
      rejector(callback, self.errorHandler);
    } else {
      return new Promise(rejector);
    }
  }

};

Map.prototype.setPadding = function(p1, p2, p3, p4) {
  if (arguments.length === 0 || arguments.length > 4) {
    return this;
  }
  var padding = {};
  padding.top = parseInt(p1, 10);
  switch (arguments.length) {
  case 4:
    // top right bottom left
    padding.right = parseInt(p2, 10);
    padding.bottom = parseInt(p3, 10);
    padding.left = parseInt(p4, 10);
    break;

  case 3:
    // top right&left bottom
    padding.right = parseInt(p2, 10);
    padding.left = padding.right;
    padding.bottom = parseInt(p3, 10);
    break;

  case 2:
    // top & bottom right&left
    padding.bottom = parseInt(p1, 10);
    padding.right = parseInt(p2, 10);
    padding.left = padding.right;
    break;

  case 1:
    // top & bottom right & left
    padding.bottom = padding.top;
    padding.right = padding.top;
    padding.left = padding.top;
    break;
  }
  this.exec.call(this, null, self.errorHandler, this.__pgmId, 'setPadding', [padding]);
  return this;
};


Map.prototype.addKmlOverlay = function(kmlOverlayOptions, callback) {
  var self = this;
  kmlOverlayOptions = kmlOverlayOptions || {};
  kmlOverlayOptions.url = kmlOverlayOptions.url || null;
  kmlOverlayOptions.clickable = common.defaultTrueOption(kmlOverlayOptions.clickable);
  kmlOverlayOptions.suppressInfoWindows = kmlOverlayOptions.suppressInfoWindows === true;

  if (kmlOverlayOptions.url) {

    var link = document.createElement('a');
    link.href = kmlOverlayOptions.url;
    kmlOverlayOptions.url = link.protocol+'//'+link.host+link.pathname + link.search;

    var invisible_dot = self.get('invisible_dot');
    if (!invisible_dot || invisible_dot._isRemoved) {
      // Create an invisible marker for kmlOverlay
      self.set('invisible_dot', self.addMarker({
        position: {
          lat: 0,
          lng: 0
        },
        icon: 'skyblue',
        visible: false
      }));
    }
    if ('icon' in kmlOverlayOptions) {
      self.get('invisible_dot').setIcon(kmlOverlayOptions.icon);
    }

    var resolver = function(resolve, reject) {

      var loader = new KmlLoader(self, self.exec, kmlOverlayOptions);
      loader.parseKmlFile(function(camera, kmlData) {
        if (kmlData instanceof BaseClass) {
          kmlData = new BaseArrayClass([kmlData]);
        }
        var kmlId = 'kmloverlay_' + Math.floor(Math.random() * Date.now());
        var kmlOverlay = new KmlOverlay(self, kmlId, camera, kmlData, kmlOverlayOptions);
        self.OVERLAYS[kmlId] = kmlOverlay;
        resolve.call(self, kmlOverlay);
      }, reject);

    };

    if (typeof callback === 'function') {
      resolver(callback, self.errorHandler);
    } else {
      return new Promise(resolver);
    }
  } else {

    if (typeof callback === 'function') {
      throw new Error('KML file url is required.');
    } else {
      return Promise.reject('KML file url is required.');
    }
  }
};

//-----------------------------------------
// Experimental: FusionTableOverlay
//-----------------------------------------
Map.prototype.addFusionTableOverlay = function(fusionTableOptions, callback) {
  var self = this;
  if (!fusionTableOptions) {
    throw new Error('Please specify fusionTableOptions');
  }
  if (!fusionTableOptions.select) {
    throw new Error('Please specify fusionTableOptions.select');
  }
  if (!fusionTableOptions.from) {
    throw new Error('Please specify fusionTableOptions.from');
  }


  var fusionTableOverlay = new FusionTableOverlay(self, fusionTableOptions, exec);
  if (cordova.platformId === 'browser') {
    //----------------------------------
    // Browser: use FusionTable layer
    //----------------------------------

    self.exec.call(self, function() {
      fusionTableOverlay._privateInitialize();
      delete fusionTableOverlay._privateInitialize;

      if (typeof callback === 'function') {
        callback.call(self, fusionTableOverlay);
      }
    }, self.errorHandler, self.__pgmId, 'loadPlugin', ['FusionTableOverlay', fusionTableOptions, fusionTableOverlay.hashCode]);

    if (typeof callback === 'function') {
      callback(fusionTableOverlay);
      return;
    } else {
      return Promise.resolve(fusionTableOverlay);
    }
  }


  var query = ['select+',
    fusionTableOptions.select,
    '+from+',
    fusionTableOptions.from];
  if (fusionTableOptions.where) {
    query.push('+where+' + fusionTableOptions.where);
  }
  if (fusionTableOptions.orderBy) {
    query.push('+orderBy+' + fusionTableOptions.orderBy);
  }
  if (fusionTableOptions.offset) {
    query.push('+offset+' + fusionTableOptions.offset);
  }
  if (fusionTableOptions.limit) {
    query.push('+limit+' + fusionTableOptions.limit);
  } else {
    query.push('+limit+1000');
  }

  fusionTableOptions.url =
    'https://fusiontables.google.com/exporttable?query=' +
    query.join('') +
    '&o=kml&g=' + fusionTableOptions.select +
    '&styleId=2&templateId=2'; // including '&styleId=2&templateId=2', FusionTable exports the latest KML file

  fusionTableOptions.clickable = common.defaultTrueOption(fusionTableOptions.clickable);
  fusionTableOptions.suppressInfoWindows = fusionTableOptions.suppressInfoWindows === true;

  return self.addKmlOverlay(fusionTableOptions, callback);
};


//-------------
// Ground overlay
//-------------
Map.prototype.addGroundOverlay = function(groundOverlayOptions, callback) {
  var self = this;
  groundOverlayOptions = groundOverlayOptions || {};
  groundOverlayOptions.anchor = groundOverlayOptions.anchor || [0.5, 0.5];
  groundOverlayOptions.bearing = 'bearing' in groundOverlayOptions ? groundOverlayOptions.bearing : 0;
  groundOverlayOptions.url = groundOverlayOptions.url || null;
  groundOverlayOptions.clickable = groundOverlayOptions.clickable === true;
  groundOverlayOptions.visible = common.defaultTrueOption(groundOverlayOptions.visible);
  groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 0;
  groundOverlayOptions.bounds = common.convertToPositionArray(groundOverlayOptions.bounds);
  groundOverlayOptions.noCaching = true;

  var groundOverlay = new GroundOverlay(self, groundOverlayOptions, exec);
  var groundOverlayId = groundOverlay.getId();
  self.OVERLAYS[groundOverlayId] = groundOverlay;
  groundOverlay.one(groundOverlayId + '_remove', function() {
    groundOverlay.off();
    delete self.OVERLAYS[groundOverlayId];
    groundOverlay = undefined;
  });

  self.exec.call(self, function() {
    groundOverlay._privateInitialize();
    delete groundOverlay._privateInitialize;
    if (typeof callback === 'function') {
      callback.call(self, groundOverlay);
    }
  }, self.errorHandler, self.__pgmId, 'loadPlugin', ['GroundOverlay', groundOverlayOptions, groundOverlay.hashCode]);

  return groundOverlay;
};

//-------------
// Tile overlay
//-------------
Map.prototype.addTileOverlay = function(tilelayerOptions, callback) {
  var self = this;
  tilelayerOptions = tilelayerOptions || {};
  tilelayerOptions.tileUrlFormat = tilelayerOptions.tileUrlFormat || null;
  if (typeof tilelayerOptions.tileUrlFormat === 'string') {
    console.log('[deprecated] the tileUrlFormat property is now deprecated. Use the getTile property.');
    tilelayerOptions.getTile = function(x, y, zoom) {
      return tilelayerOptions.tileUrlFormat.replace(/<x>/gi, x)
        .replace(/<y>/gi, y)
        .replace(/<zoom>/gi, zoom);
    };
  }
  if (typeof tilelayerOptions.getTile !== 'function') {
    throw new Error('[error] the getTile property is required.');
  }
  tilelayerOptions.visible = common.defaultTrueOption(tilelayerOptions.visible);
  tilelayerOptions.zIndex = tilelayerOptions.zIndex || 0;
  tilelayerOptions.tileSize = tilelayerOptions.tileSize || 512;
  tilelayerOptions.opacity = (tilelayerOptions.opacity === null || tilelayerOptions.opacity === undefined) ? 1 : tilelayerOptions.opacity;
  tilelayerOptions.debug = tilelayerOptions.debug === true;
  tilelayerOptions.userAgent = tilelayerOptions.userAgent || navigator.userAgent;


  var tileOverlay = new TileOverlay(self, tilelayerOptions, exec);
  var tileOverlayId = tileOverlay.getId();
  self.OVERLAYS[tileOverlayId] = tileOverlay;
  var hashCode = tileOverlay.hashCode;

  tileOverlay.one(tileOverlayId + '_remove', function() {
    document.removeEventListener(tileOverlayId + '-' + hashCode + '-tileoverlay', onNativeCallback);
    tileOverlay.off();
    delete self.OVERLAYS[tileOverlayId];
    tileOverlay = undefined;
  });

  var options = {
    visible: tilelayerOptions.visible,
    zIndex: tilelayerOptions.zIndex,
    tileSize: tilelayerOptions.tileSize,
    opacity: tilelayerOptions.opacity,
    userAgent: tilelayerOptions.userAgent,
    debug: tilelayerOptions.debug
  };

  var onNativeCallback = function(params) {
    var url = tilelayerOptions.getTile(params.x, params.y, params.zoom);
    if (!url || url === '(null)' || url === 'undefined' || url === 'null') {
      url = '(null)';
    }
    if (url instanceof Promise) {
      common.promiseTimeout(5000, url)
        .then(function(finalUrl) {
          cordova_exec(null, self.errorHandler, self.__pgmId + '-tileoverlay', 'onGetTileUrlFromJS', [hashCode, params.key, finalUrl]);
        })
        .catch(function() {
          cordova_exec(null, self.errorHandler, self.__pgmId + '-tileoverlay', 'onGetTileUrlFromJS', [hashCode, params.key, '(null)']);
        });
    } else {
      cordova_exec(null, self.errorHandler, self.__pgmId + '-tileoverlay', 'onGetTileUrlFromJS', [hashCode, params.key, url]);
    }
  };
  document.addEventListener(self.__pgmId + '-' + hashCode + '-tileoverlay', onNativeCallback);

  self.exec.call(self, function() {
    tileOverlay._privateInitialize();
    delete tileOverlay._privateInitialize;

    if (typeof callback === 'function') {
      callback.call(self, tileOverlay);
    }
  }, self.errorHandler, self.__pgmId, 'loadPlugin', ['TileOverlay', options, hashCode]);

  return tileOverlay;
};

//-------------
// Polygon
//-------------
Map.prototype.addPolygon = function(polygonOptions, callback) {
  var self = this;
  polygonOptions.points = polygonOptions.points || [];
  var _orgs = polygonOptions.points;
  polygonOptions.points = common.convertToPositionArray(polygonOptions.points);
  polygonOptions.holes = polygonOptions.holes || [];
  if (polygonOptions.holes.length > 0 && !Array.isArray(polygonOptions.holes[0])) {
    polygonOptions.holes = [polygonOptions.holes];
  }
  polygonOptions.holes = polygonOptions.holes.map(function(hole) {
    if (!utils.isArray(hole)) {
      return [];
    }
    return hole.map(function(position) {
      return {
        'lat': position.lat,
        'lng': position.lng
      };
    });
  });
  polygonOptions.strokeColor = common.HTMLColor2RGBA(polygonOptions.strokeColor || '#FF000080', 0.75);
  if (polygonOptions.fillColor) {
    polygonOptions.fillColor = common.HTMLColor2RGBA(polygonOptions.fillColor || '#FF000080', 0.75);
  } else {
    polygonOptions.fillColor = common.HTMLColor2RGBA('#FF000080', 0.75);
  }
  polygonOptions.strokeWidth = 'strokeWidth' in polygonOptions ? polygonOptions.strokeWidth : 10;
  polygonOptions.visible = common.defaultTrueOption(polygonOptions.visible);
  polygonOptions.clickable = polygonOptions.clickable === true;
  polygonOptions.zIndex = polygonOptions.zIndex || 0;
  polygonOptions.geodesic = polygonOptions.geodesic === true;

  var opts = JSON.parse(JSON.stringify(polygonOptions));
  polygonOptions.points = _orgs;
  var polygon = new Polygon(self, polygonOptions, exec);
  var polygonId = polygon.getId();
  self.OVERLAYS[polygonId] = polygon;
  polygon.one(polygonId + '_remove', function() {
    polygon.off();
    delete self.OVERLAYS[polygonId];
    polygon = undefined;
  });

  self.exec.call(self, function() {
    polygon._privateInitialize();
    delete polygon._privateInitialize;

    if (typeof callback === 'function') {
      callback.call(self, polygon);
    }
  }, self.errorHandler, self.__pgmId, 'loadPlugin', ['Polygon', opts, polygon.hashCode]);

  return polygon;
};

//-------------
// Polyline
//-------------
Map.prototype.addPolyline = function(polylineOptions, callback) {
  var self = this;
  polylineOptions.points = polylineOptions.points || [];
  var _orgs = polylineOptions.points;
  polylineOptions.points = common.convertToPositionArray(polylineOptions.points);
  polylineOptions.color = common.HTMLColor2RGBA(polylineOptions.color || '#FF000080', 0.75);
  polylineOptions.width = 'width' in polylineOptions ? polylineOptions.width : 10;
  polylineOptions.visible = common.defaultTrueOption(polylineOptions.visible);
  polylineOptions.clickable = polylineOptions.clickable === true;
  polylineOptions.zIndex = polylineOptions.zIndex || 0;
  polylineOptions.geodesic = polylineOptions.geodesic === true;

  var opts = JSON.parse(JSON.stringify(polylineOptions));
  polylineOptions.points = _orgs;
  var polyline = new Polyline(self, polylineOptions, exec);
  var polylineId = polyline.getId();
  self.OVERLAYS[polylineId] = polyline;

  polyline.one(polylineId + '_remove', function() {
    polyline.off();
    delete self.OVERLAYS[polylineId];
    polyline = undefined;
  });

  self.exec.call(self, function() {
    polyline._privateInitialize();
    delete polyline._privateInitialize;

    if (typeof callback === 'function') {
      callback.call(self, polyline);
    }
  }, self.errorHandler, self.__pgmId, 'loadPlugin', ['Polyline', opts, polyline.hashCode]);

  return polyline;
};

//-------------
// Circle
//-------------
Map.prototype.addCircle = function(circleOptions, callback) {
  var self = this;
  circleOptions.center = circleOptions.center || {};
  circleOptions.center.lat = circleOptions.center.lat || 0.0;
  circleOptions.center.lng = circleOptions.center.lng || 0.0;
  circleOptions.strokeColor = common.HTMLColor2RGBA(circleOptions.strokeColor || '#FF0000', 0.75);
  circleOptions.fillColor = common.HTMLColor2RGBA(circleOptions.fillColor || '#000000', 0.75);
  circleOptions.strokeWidth = 'strokeWidth' in circleOptions ? circleOptions.strokeWidth : 10;
  circleOptions.visible = common.defaultTrueOption(circleOptions.visible);
  circleOptions.zIndex = circleOptions.zIndex || 0;
  circleOptions.radius = 'radius' in circleOptions ? circleOptions.radius : 1;

  var circle = new Circle(self, circleOptions, exec);
  var circleId = circle.getId();
  self.OVERLAYS[circleId] = circle;
  circle.one(circleId + '_remove', function() {
    circle.off();
    delete self.OVERLAYS[circleId];
    circle = undefined;
  });

  self.exec.call(self, function() {
    circle._privateInitialize();
    delete circle._privateInitialize;

    if (typeof callback === 'function') {
      callback.call(self, circle);
    }
  }, self.errorHandler, self.__pgmId, 'loadPlugin', ['Circle', circleOptions, circle.hashCode]);

  return circle;
};

//-------------
// Marker
//-------------

Map.prototype.addMarker = function(markerOptions, callback) {
  var self = this;
  markerOptions = common.markerOptionsFilter(markerOptions);

  //------------------------------------
  // Generate a makrer instance at once.
  //------------------------------------
  markerOptions.icon = markerOptions.icon || {};
  if (typeof markerOptions.icon === 'string' || Array.isArray(markerOptions.icon)) {
    markerOptions.icon = {
      url: markerOptions.icon
    };
  }

  var marker = new Marker(self, markerOptions, exec);
  var markerId = marker.getId();

  self.MARKERS[markerId] = marker;
  self.OVERLAYS[markerId] = marker;
  marker.one(markerId + '_remove', function() {
    delete self.MARKERS[markerId];
    delete self.OVERLAYS[markerId];
    marker.destroy();
    marker = undefined;
  });

  self.exec.call(self, function(result) {

    markerOptions.icon.size = markerOptions.icon.size || {};
    markerOptions.icon.size.width = markerOptions.icon.size.width || result.width;
    markerOptions.icon.size.height = markerOptions.icon.size.height || result.height;
    markerOptions.icon.anchor = markerOptions.icon.anchor || [markerOptions.icon.size.width / 2, markerOptions.icon.size.height];

    if (!markerOptions.infoWindowAnchor) {
      markerOptions.infoWindowAnchor = [markerOptions.icon.size.width / 2, 0];
    }
    marker._privateInitialize(markerOptions);
    delete marker._privateInitialize;

    if (typeof callback === 'function') {
      callback.call(self, marker);
    }
  }, self.errorHandler, self.__pgmId, 'loadPlugin', ['Marker', markerOptions, marker.hashCode]);

  return marker;
};


//------------------
// Marker cluster
//------------------
Map.prototype.addMarkerCluster = function(markerClusterOptions, callback) {
  var self = this;
  if (typeof markerClusterOptions === 'function') {
    callback = markerClusterOptions;
    markerClusterOptions = null;
  }
  markerClusterOptions = markerClusterOptions || {};
  var positionList = markerClusterOptions.markers.map(function(marker) {
    return marker.position;
  });

  var markerCluster = new MarkerCluster(self, {
    'icons': markerClusterOptions.icons,
    //'markerMap': markerMap,
    'idxCount': positionList.length + 1,
    'maxZoomLevel': Math.min(markerClusterOptions.maxZoomLevel || 15, 18),
    'debug': markerClusterOptions.debug === true,
    'boundsDraw': common.defaultTrueOption(markerClusterOptions.boundsDraw)
  }, exec);
  var markerClusterId = markerCluster.getId();
  self.OVERLAYS[markerClusterId] = markerCluster;

  self.exec.call(self, function(result) {

    result.geocellList.forEach(function(geocell, idx) {
      var markerOptions = markerClusterOptions.markers[idx];
      markerOptions = common.markerOptionsFilter(markerOptions);

      markerOptions._cluster = {
        isRemoved: false,
        isAdded: false,
        geocell: geocell
      };
      markerCluster.addMarker(markerOptions);

      //self.MARKERS[marker.getId()] = marker;
      //self.OVERLAYS[marker.getId()] = marker;
    });


    markerCluster.one('remove', function() {
      delete self.OVERLAYS[result.__pgmId];
      /*
            result.geocellList.forEach(function(geocell, idx) {
              var markerOptions = markerClusterOptions.markers[idx];
              var markerId = result.__pgmId + '-' + (markerOptions.__pgmId || 'marker_' + idx);
              var marker = self.MARKERS[markerId];
              if (marker) {
                marker.off();
              }
              //delete self.MARKERS[markerId];
              delete self.OVERLAYS[markerId];
            });
      */
      markerCluster.destroy();
    });

    markerCluster._privateInitialize();
    delete markerCluster._privateInitialize;

    markerCluster._triggerRedraw.call(markerCluster, {
      force: true
    });

    if (typeof callback === 'function') {
      callback.call(self, markerCluster);
    }
  }, self.errorHandler, self.__pgmId, 'loadPlugin', ['MarkerCluster', {
    'positionList': positionList,
    'debug': markerClusterOptions.debug === true
  }, markerCluster.hashCode]);

  return markerCluster;
};

/*****************************************************************************
 * Callbacks from the native side
 *****************************************************************************/

Map.prototype._onSyncInfoWndPosition = function(eventName, points) {
  this.set('infoPosition', points);
};

Map.prototype._onMapEvent = function(eventName) {
  if (!this._isReady) {
    return;
  }
  var args = [eventName];
  for (var i = 1; i < arguments.length; i++) {
    args.push(arguments[i]);
  }
  this.trigger.apply(this, args);
};

Map.prototype._onMarkerEvent = function(eventName, markerId, position) {
  var self = this;
  var marker = self.MARKERS[markerId] || null;

  if (marker) {
    marker.set('position', position);
    if (eventName === event.INFO_OPEN) {
      marker.set('isInfoWindowVisible', true);
    }
    if (eventName === event.INFO_CLOSE) {
      marker.set('isInfoWindowVisible', false);
    }
    marker.trigger(eventName, position, marker);
  }
};

Map.prototype._onClusterEvent = function(eventName, markerClusterId, clusterId, position) {
  var self = this;
  var markerCluster = self.OVERLAYS[markerClusterId] || null;
  if (markerCluster) {
    if (/^marker_/i.test(clusterId)) {
      // regular marker
      var marker = markerCluster.getMarkerById(clusterId);
      if (eventName === event.MARKER_CLICK) {
        markerCluster.trigger(eventName, position, marker);
      } else {
        if (eventName === event.INFO_OPEN) {
          marker.set('isInfoWindowVisible', true);
        }
        if (eventName === event.INFO_CLOSE) {
          marker.set('isInfoWindowVisible', false);
        }
      }
      marker.trigger(eventName, position, marker);
    } else {
      // cluster marker
      var cluster = markerCluster._getClusterByClusterId(clusterId);
      if (cluster) {
        markerCluster.trigger(eventName, cluster);
      } else {
        console.log('-----> This is remained cluster icon : ' + clusterId);
      }
    }
  }
};

Map.prototype._onOverlayEvent = function(eventName, overlayId) {
  var self = this;
  var overlay = self.OVERLAYS[overlayId] || null;
  if (overlay) {
    var args = [eventName];
    for (var i = 2; i < arguments.length; i++) {
      args.push(arguments[i]);
    }
    args.push(overlay); // for ionic
    overlay.trigger.apply(overlay, args);
  }
};

Map.prototype.getCameraTarget = function() {
  return this.get('camera_target');
};

Map.prototype.getCameraZoom = function() {
  return this.get('camera_zoom');
};
Map.prototype.getCameraTilt = function() {
  return this.get('camera_tilt');
};
Map.prototype.getCameraBearing = function() {
  return this.get('camera_bearing');
};

Map.prototype._onCameraEvent = function(eventName, cameraPosition) {
  this.set('camera', cameraPosition);
  this.set('camera_target', cameraPosition.target);
  this.set('camera_zoom', cameraPosition.zoom);
  this.set('camera_bearing', cameraPosition.bearing);
  this.set('camera_tilt', cameraPosition.viewAngle || cameraPosition.tilt);
  this.set('camera_northeast', cameraPosition.northeast);
  this.set('camera_southwest', cameraPosition.southwest);
  this.set('camera_nearLeft', cameraPosition.nearLeft);
  this.set('camera_nearRight', cameraPosition.nearRight);
  this.set('camera_farLeft', cameraPosition.farLeft);
  this.set('camera_farRight', cameraPosition.farRight);
  if (this._isReady) {
    this.trigger(eventName, cameraPosition, this);
  }
};

module.exports = Map;
