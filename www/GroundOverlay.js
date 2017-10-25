var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
* GroundOverlay Class
*****************************************************************************/
var exec;
var GroundOverlay = function(map, groundOverlayId, groundOverlayOptions, _exec) {
  BaseClass.apply(this);
  exec = _exec;

  var self = this;
  groundOverlayOptions.visible = groundOverlayOptions.visible === undefined ? true : groundOverlayOptions.visible;
  groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 1;
  groundOverlayOptions.opacity = groundOverlayOptions.opacity || 1;
  groundOverlayOptions.bounds = groundOverlayOptions.bounds || [];
  groundOverlayOptions.anchor = groundOverlayOptions.anchor || [0, 0];
  groundOverlayOptions.bearing = groundOverlayOptions.bearing || 0;
  Object.defineProperty(self, "_isReady", {
      value: true,
      writable: false
  });
  Object.defineProperty(self, "id", {
     value: groundOverlayId,
     writable: false
  });
  Object.defineProperty(self, "type", {
     value: "GroundOverlay",
     writable: false
  });
  Object.defineProperty(self, "map", {
     value: map,
     writable: false
  });
  var ignores = ["map", "id", "hashCode", "type"];
  for (var key in groundOverlayOptions) {
      if (ignores.indexOf(key) === -1) {
          self.set(key, groundOverlayOptions[key]);
      }
  }

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on("visible_changed", function(oldValue, visible) {
      exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
  });
  self.on("image_changed", function(oldValue, url) {
      exec.call(self, null, self.errorHandler, self.getPluginName(), 'setImage', [self.getId(), url]);
  });
  self.on("bounds_changed", function(oldValue, bounds) {
      exec.call(self, null, self.errorHandler, self.getPluginName(), 'setBounds', [self.getId(), bounds]);
  });
  self.on("opacity_changed", function(oldValue, opacity) {
      exec.call(self, null, self.errorHandler, self.getPluginName(), 'setOpacity', [self.getId(), opacity]);
  });
  self.on("clickable_changed", function(oldValue, clickable) {
      exec.call(self, null, self.errorHandler, self.getPluginName(), 'setClickable', [self.getId(), clickable]);
  });
  self.on("bearing_changed", function(oldValue, bearing) {
      exec.call(self, null, self.errorHandler, self.getPluginName(), 'setBearing', [self.getId(), bearing]);
  });
  self.on("zIndex_changed", function(oldValue, zIndex) {
     exec.call(self, null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
  });

};


utils.extend(GroundOverlay, BaseClass);

GroundOverlay.prototype.getPluginName = function() {
    return this.map.getId() + "-groundoverlay";
};

GroundOverlay.prototype.getHashCode = function() {
    return this.hashCode;
};

GroundOverlay.prototype.getMap = function() {
    return this.map;
};
GroundOverlay.prototype.getId = function() {
    return this.id;
};

GroundOverlay.prototype.setVisible = function(visible) {
    this.set('visible', visible);
};

GroundOverlay.prototype.getVisible = function() {
    return this.get('visible');
};

GroundOverlay.prototype.setImage = function(url) {
    this.set('image', url);
};

GroundOverlay.prototype.setBounds = function(points) {
    var i,
        bounds = [];
    for (i = 0; i < points.length; i++) {
        bounds.push({
            "lat": points[i].lat,
            "lng": points[i].lng
        });
    }
    this.set('bounds', bounds);
};

GroundOverlay.prototype.getOpacity = function() {
    return this.get("opacity");
};

GroundOverlay.prototype.getBearing = function() {
    return this.get("bearing");
};

GroundOverlay.prototype.setOpacity = function(opacity) {
    if (!opacity && opacity !== 0) {
        console.log('opacity value must be int or double');
        return false;
    }
    this.set('opacity', opacity);
};
GroundOverlay.prototype.setBearing = function(bearing) {
    this.set('bearing', bearing);
};

GroundOverlay.prototype.getZIndex = function() {
    return this.get("zIndex");
};

GroundOverlay.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
};
GroundOverlay.prototype.setClickable = function(clickable) {
    clickable = common.parseBoolean(clickable);
    this.set('clickable', clickable);
    return this;
};
GroundOverlay.prototype.getClickable = function() {
    return this.get('clickable');
};

GroundOverlay.prototype.remove = function() {
    var self = this;
    if (self._isRemoved) {
      return;
    }
    Object.defineProperty(self, "_isRemoved", {
        value: true,
        writable: false
    });
    self.trigger(self.id + "_remove");
    exec.call(self, null, self.errorHandler, self.getPluginName(), 'remove', [self.getId()]);
    self.destroy();
};


module.exports = GroundOverlay;
