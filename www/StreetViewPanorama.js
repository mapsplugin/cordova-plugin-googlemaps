var utils = require('cordova/utils'),
  common = require('./Common'),
  event = require('./event'),
  Overlay = require('./Overlay');

/*****************************************************************************
 * StreetViewPanorama Class
 *****************************************************************************/
var exec;
var StreetViewPanorama = function(streetViewId, _exec) {
  exec = _exec;
  Overlay.call(this, this, {}, 'StreetViewPanorama', _exec, {
    __pgmId: streetViewId
  });

  var self = this;
  self.set('visible', true);
  self.set('gesture_panning', true);
  self.set('gesture_zoom', true);
  self.set('control_navigation', true);
  self.set('control_streetNames', true);
  self.set('camera_zoom', 0);
  self.set('camera_tilt', 0);
  self.set('camera_bearing', 0);

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on('gesture_panning_changed', function() {
    var booleanValue = self.get('gesture_panning');
    self.exec.call(self, null, self.errorHandler, self.__pgmId, 'setPanningGesturesEnabled', [booleanValue]);
  });
  self.on('gesture_zoom_changed', function() {
    var booleanValue = self.get('gesture_zoom');
    self.exec.call(self, null, self.errorHandler, self.__pgmId, 'setZoomGesturesEnabled', [booleanValue]);
  });
  self.on('control_navigation_changed', function() {
    var booleanValue = self.get('control_navigation');
    self.exec.call(self, null, self.errorHandler, self.__pgmId, 'setNavigationEnabled', [booleanValue]);
  });
  self.on('control_streetNames_changed', function() {
    var booleanValue = self.get('control_streetNames');
    self.exec.call(self, null, self.errorHandler, self.__pgmId, 'setStreetNamesEnabled', [booleanValue]);
  });
  self.on('visible_changed', function() {
    var booleanValue = self.get('visible');
    self.exec.call(self, null, self.errorHandler, self.__pgmId, 'setVisible', [booleanValue]);
  });
};


utils.extend(StreetViewPanorama, Overlay);

StreetViewPanorama.prototype.getPluginName = function() {
  return this.map.getId() + '-streetview';
};

StreetViewPanorama.prototype.getHashCode = function() {
  return this.hashCode;
};

StreetViewPanorama.prototype.getId = function() {
  return this.__pgmId;
};
StreetViewPanorama.prototype.getPanorama = function(meta, panorama, div, options) {
  var self = this,
    args = [meta];
  options = options || {};


  //self.set('clickable', options.clickable === false ? false : true);
  self.set('visible', options.visible === false ? false : true, true);
  args.push(options);

  // Initial camera options
  if ('camera' in options) {
    self.set('camera', options.camera, true);
    if ('zoom' in options.camera) {
      self.set('camera_zoom', options.camera.zoom, true);
    }
    if ('bearing' in options.camera) {
      self.set('camera_bearing', options.camera.bearing, true);
    }
    if ('tilt' in options.camera) {
      self.set('camera_tilt', options.camera.tilt, true);
    }
    if ('target' in options.camera) {
      self.set('position', options.camera.target, true);
    }
  }

  // Gesture options
  options.gestures = options.gestures || {};
  options.gestures.panning = common.defaultTrueOption(options.gestures.panning);
  options.gestures.zoom = common.defaultTrueOption(options.gestures.zoom);
  self.set('gesture_panning', options.gestures.panning, true);
  self.set('gesture_zoom', options.gestures.zoom, true);

  // Controls options
  options.controls = options.controls || {};
  options.controls.navigation = common.defaultTrueOption(options.controls.navigation);
  options.controls.streetNames = common.defaultTrueOption(options.controls.streetNames);
  self.set('control_navigation', options.controls.navigation, true);
  self.set('control_streetNames', options.controls.streetNames, true);

  // Gets the map div size.
  // The plugin needs to consider the viewport zoom ratio
  // for the case window.innerHTML > body.offsetWidth.
  self.set('div', div);
  var elemId = common.getPluginDomId(div);
  args.push(elemId);

  exec.call({
    _isReady: true
  }, function() {

    //------------------------------------------------------------------------
    // Clear background colors of map div parents after the map is created
    //------------------------------------------------------------------------
    var positionCSS;
    for (var i = 0; i < div.children.length; i++) {
      positionCSS = common.getStyle(div.children[i], 'position');
      if (positionCSS === 'static') {
        div.children[i].style.position = 'relative';
      }
    }
    //div.insertBefore(self._layers.info, div.firstChild);


    while (div.parentNode) {
      div.style.backgroundColor = 'rgba(0,0,0,0) !important';

      common.attachTransparentClass(div);

      div = div.parentNode;
    }
    cordova.fireDocumentEvent('plugin_touch', {
      force: true
    });
    self._privateInitialize();
    delete self._privateInitialize;

    self.trigger(event.PANORAMA_READY, self);
  }, self.errorHandler, 'CordovaGoogleMaps', 'getPanorama', args, {
    sync: true
  });
};

StreetViewPanorama.prototype.setVisible = function(isVisible) {
  this.set('visible', common.parseBoolean(isVisible));
  return this;
};

StreetViewPanorama.prototype.getVisible = function() {
  return this.get('visible');
};

StreetViewPanorama.prototype.getDiv = function() {
  return this.get('div');
};
StreetViewPanorama.prototype.getMap = function() {
  // stub
};

StreetViewPanorama.prototype.setPanningGesturesEnabled = function (boolValue) {
  boolValue = common.parseBoolean(boolValue);
  this.set('gesture_panning', boolValue);
  return this;
};
StreetViewPanorama.prototype.getPanningGesturesEnabled = function () {
  return this.get('gesture_panning');
};
StreetViewPanorama.prototype.setZoomGesturesEnabled = function (boolValue) {
  boolValue = common.parseBoolean(boolValue);
  this.set('gesture_zoom', boolValue);
  return this;
};
StreetViewPanorama.prototype.getZoomGesturesEnabled = function () {
  return this.get('gesture_zoom');
};
StreetViewPanorama.prototype.setNavigationEnabled = function (boolValue) {
  boolValue = common.parseBoolean(boolValue);
  this.set('control_navigation', boolValue);
  return this;
};
StreetViewPanorama.prototype.getNavigationEnabled = function () {
  return this.get('control_navigation');
};
StreetViewPanorama.prototype.setStreetNamesEnabled = function (boolValue) {
  boolValue = common.parseBoolean(boolValue);
  this.set('control_streetNames', boolValue);
  return this;
};
StreetViewPanorama.prototype.getStreetNamesEnabled = function () {
  return this.get('control_streetNames');
};
StreetViewPanorama.prototype.getLinks = function () {
  return this.get('links');
};
StreetViewPanorama.prototype.getLocation = function () {
  return this.get('location');
};
StreetViewPanorama.prototype.getPanoId = function () {
  return this.get('pano');
};
StreetViewPanorama.prototype.getPosition = function () {
  return this.get('position');
};

StreetViewPanorama.prototype.setPosition = function(cameraPosition, callback) {
  var self = this;
  cameraPosition = cameraPosition || {};
  if (typeof cameraPosition === 'string') {
    // case: panorama.setPosition(panoId);
    cameraPosition = {
      target : cameraPosition
    };
  }
  if (typeof cameraPosition.target === 'object') {
    // case: panorama.setPosition({
    //         target: {lat: ..., lng: ...}
    //       });
    self.set('position', cameraPosition.target);
  }
  self.exec.call(self, function() {
    if (typeof callback === 'function') {
      callback.call(self);
    }
  }, self.errorHandler, self.__pgmId, 'setPosition', [cameraPosition], {
    sync: true
  });
  return this;
};

StreetViewPanorama.prototype.setPov = function(pov, callback) {
  var self = this;
  pov = pov || {};
  if ('zoom' in pov) {
    self.set('camera_zoom', pov.zoom);
  }
  if ('bearing' in pov) {
    self.set('camera_bearing', pov.bearing);
  }
  if ('tilt' in pov) {
    self.set('camera_tilt', pov.tilt);
  }
  self.exec.call(self, function() {
    if (typeof callback === 'function') {
      callback.call(self);
    }
  }, self.errorHandler, self.__pgmId, 'setPov', [pov], {
    sync: true
  });
  return this;
};


StreetViewPanorama.prototype.remove = function(callback) {
  var self = this;

  var resolver = function(resolve, reject) {
    self.exec.call(self,
      function() {
        self.destroy();
        resolve.call(self);
      },
      reject.bind(self),
      'CordovaGoogleMaps', 'removeMap', [self.__pgmId], {
        sync: true,
        remove: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }








  self.trigger('remove');
};
StreetViewPanorama.prototype._onPanoramaCameraChange = function(eventName, cameraPosition) {
  var self = this;
  self.set('camera', utils.clone(self.get('camera') || {}, cameraPosition));
  self.set('camera_zoom', cameraPosition.zoom);
  self.set('camera_bearing', cameraPosition.bearing);
  self.set('camera_tilt', cameraPosition.viewAngle || cameraPosition.tilt);
  self.set('links', cameraPosition.links || []);
  if (self._isReady) {
    self._onPanoramaEvent(eventName, cameraPosition);
  }
};

StreetViewPanorama.prototype._onPanoramaLocationChange = function(eventName, panoramaLocation) {
  var self = this;
  self.set('location', panoramaLocation);
  self.set('pano', panoramaLocation ? panoramaLocation.panoId : null);
  self.set('position', panoramaLocation ? panoramaLocation.latLng : null);
  if (self._isReady) {
    self._onPanoramaEvent(eventName, panoramaLocation);
  }
};

StreetViewPanorama.prototype._onPanoramaEvent = function() {
  var self = this;
  if (self._isReady) {
    var args = Array.prototype.slice.call(arguments, 0);
    args.push(self);
    self.trigger.apply(self, args);
  }
};

module.exports = StreetViewPanorama;
