var argscheck = require('cordova/argscheck'),
  utils = require('cordova/utils'),
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
    id: streetViewId
  });

  var self = this;
  //-----------------------------------------------
  // Sets the initialize option to each property
  //-----------------------------------------------
  // var ignores = ["map", "id", "hashCode", "type"];
  // for (var key in tileOverlayOptions) {
  //   if (ignores.indexOf(key) === -1) {
  //     self.set(key, tileOverlayOptions[key]);
  //   }
  // }

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  // self.on("fadeIn_changed", function() {
  //     var fadeIn = self.get("fadeIn");
  //     exec.call(self, null, self.errorHandler, self.getPluginName(), 'setFadeIn', [self.getId(), fadeIn]);
  // });
  // self.on("opacity_changed", function() {
  //     var opacity = self.get("opacity");
  //     exec.call(self, null, self.errorHandler, self.getPluginName(), 'setOpacity', [self.getId(), opacity]);
  // });
  // self.on("zIndex_changed", function() {
  //     var zIndex = self.get("zIndex");
  //     exec.call(self, null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
  // });
  // self.on("visible_changed", function() {
  //     var visible = self.get("visible");
  //     exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
  // });
};


utils.extend(StreetViewPanorama, Overlay);

StreetViewPanorama.prototype.getPluginName = function() {
  return this.map.getId() + "-streetview";
};

StreetViewPanorama.prototype.getHashCode = function() {
  return this.hashCode;
};

StreetViewPanorama.prototype.getId = function() {
  return this.id;
};
StreetViewPanorama.prototype.getPanorama = function(panoramaId, div, options) {
  var self = this,
    args = [panoramaId];
  options = options || {};


  self.set("clickable", options.clickable === false ? false : true);
  self.set("visible", options.visible === false ? false : true);
  args.push(options);

  // Initial camera options
  if ("camera" in options) {
    self.set("camera", options.camera);
    if ("zoom" in options.camera) {
      self.set("camera_zoom", options.camera.zoom);
    }
    if ("bearing" in options.camera) {
      self.set("camera_bearing", options.camera.bearing);
    }
    if ("tilt" in options.camera) {
      self.set("camera_tilt", options.camera.tilt);
    }
    if ("target" in options.camera) {
      self.set("position", options.camera.target);
    }
  }

  // Gesture options
  options.gestures = options.gestures || {};
  options.gestures.panning = common.defaultTrueOption(options.gestures.panning);
  options.gestures.zoom = common.defaultTrueOption(options.gestures.zoom);
  self.set("gesture_panning", options.gestures.panning);
  self.set("gesture_zoom", options.gestures.zoom);

  // Controls options
  options.controls = options.controls || {};
  options.controls.panning = common.defaultTrueOption(options.controls.navigation);
  options.controls.zoom = common.defaultTrueOption(options.controls.streetNames);
  self.set("control_navigation", options.controls.navigation);
  self.set("control_streetNames", options.controls.streetNames);

  // Gets the map div size.
  // The plugin needs to consider the viewport zoom ratio
  // for the case window.innerHTML > body.offsetWidth.
  self.set("div", div);
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
      positionCSS = common.getStyle(div.children[i], "position");
      if (positionCSS === "static") {
        div.children[i].style.position = "relative";
      }
    }
    //div.insertBefore(self._layers.info, div.firstChild);


    while (div.parentNode) {
      div.style.backgroundColor = 'rgba(0,0,0,0) !important';

      // prevent multiple readding the class
      if (div.classList && !div.classList.contains('_gmaps_cdv_')) {
        div.classList.add('_gmaps_cdv_');
      } else if (div.className && div.className.indexOf('_gmaps_cdv_') === -1) {
        div.className = div.className + ' _gmaps_cdv_';
      }

      div = div.parentNode;
    }
    cordova.fireDocumentEvent("plugin_touch", {
      force: true
    });
    self._privateInitialize();
    delete self._privateInitialize;

    self.trigger(event.PANORAMA_READY, self);
  }, self.errorHandler, 'CordovaGoogleMaps', 'getPanorama', args, {
    sync: true
  });
};

StreetViewPanorama.prototype.getVisible = function() {
  return this.get("visible");
};
StreetViewPanorama.prototype.getClickable = function() {
  return this.get("clickable");
};
StreetViewPanorama.prototype.getDiv = function() {
  return this.get("div");
};
StreetViewPanorama.prototype.getMap = function() {
  // stub
};

StreetViewPanorama.prototype.moveCamera = function(cameraPosition) {
  var self = this;
  if ("camera" in cameraPosition) {
    self.set("camera", cameraPosition.camera);
    if ("zoom" in cameraPosition.camera) {
      self.set("camera_zoom", cameraPosition.camera.zoom);
    }
    if ("bearing" in cameraPosition.camera) {
      self.set("camera_bearing", cameraPosition.camera.bearing);
    }
    if ("tilt" in cameraPosition.camera) {
      self.set("camera_tilt", cameraPosition.camera.tilt);
    }
  }
  self.exec.call(self, function() {
    if (typeof callback === "function") {
      callback.call(self);
    }
  }, self.errorHandler, self.id, 'moveCamera', [cameraPosition], {
    sync: true
  });
};


StreetViewPanorama.prototype._onPanoramaCameraChange = function(eventName, cameraPosition) {
  this.set('camera', Object.assign(self.get("camera"), cameraPosition));
  this.set('camera_zoom', cameraPosition.zoom);
  this.set('camera_bearing', cameraPosition.bearing);
  this.set('camera_tilt', cameraPosition.viewAngle || cameraPosition.tilt);
  if (this._isReady) {
    this._onPanoramaEvent(eventName, cameraPosition);
  }
};

StreetViewPanorama.prototype._onPanoramaLocationChange = function(eventName, panoramaLocation) {
  this.set('location', panoramaLocation);
  this.set('panoId', panoramaLocation.panoId);
  this.set('position', panoramaLocation.position);
  if (this._isReady) {
    this._onPanoramaEvent(eventName, panoramaLocation);
  }
};
StreetViewPanorama.prototype._onPanoramaEvent = function(eventName) {
  if (this._isReady) {
    var args = Array.prototype.slice.call(arguments, 0);
    args.push(this);
    this.trigger.apply(this, args);
  }
};

module.exports = StreetViewPanorama;
