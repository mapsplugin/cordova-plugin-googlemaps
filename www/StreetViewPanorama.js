var argscheck = require('cordova/argscheck'),
  utils = require('cordova/utils'),
  common = require('./Common'),
  event = require('./event'),
  BaseClass = require('./BaseClass');

/*****************************************************************************
 * StreetViewPanorama Class
 *****************************************************************************/
var exec;
var StreetViewPanorama = function(streetViewId, _exec) {
  exec = _exec;
  BaseClass.apply(this);

  var self = this;
  Object.defineProperty(self, "id", {
    value: streetViewId,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "StreetViewPanorama",
    writable: false
  });
  Object.defineProperty(self, "_isReady", {
    value: true,
    writable: false
  });
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


utils.extend(StreetViewPanorama, BaseClass);

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


    Object.defineProperty(self, "_isReady", {
      value: true,
      writable: false
    });
    //self.refreshLayout();
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

StreetViewPanorama.prototype._onPanoramaCameraEvent = function(eventName, cameraPosition) {
  this.set('camera', cameraPosition);
  this.set('camera_zoom', cameraPosition.zoom);
  this.set('camera_bearing', cameraPosition.bearing);
  this.set('camera_tilt', cameraPosition.viewAngle || cameraPosition.tilt);
  this.set('camera_orientation_bearing', cameraPosition.orientation.bearing);
  this.set('camera_orientation_tilt', cameraPosition.orientation.tilt);
  if (this._isReady) {
    this.trigger(eventName, cameraPosition, this);
  }
};

module.exports = StreetViewPanorama;
