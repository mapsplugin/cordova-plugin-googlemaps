var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    LatLngBounds = require('./LatLngBounds'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * Circle Class
 *****************************************************************************/
var Circle = function(map, circleId, circleOptions) {
    BaseClass.apply(this);

    var self = this;
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: circleId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "Circle",
        writable: false
    });
    Object.defineProperty(self, "hashCode", {
        value: circleOptions.hashCode,
        writable: false
    });

    self.on("center_changed", function(oldValue, center) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setCenter', [self.getId(), center.lat, center.lng]);
    });
    self.on("fillColor_changed", function(oldValue, color) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setFillColor', [self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
    });
    self.on("strokeColor_changed", function(oldValue, color) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setStrokeColor', [self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
    });
    self.on("strokeWidth_changed", function(oldValue, width) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setStrokeWidth', [self.getId(), width]);
    });
    self.on("visible_changed", function(oldValue, visible) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
    });
    self.on("radius_changed", function(oldValue, radius) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setRadius', [self.getId(), radius]);
    });
    self.on("zIndex_changed", function(oldValue, zIndex) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
    });

    var ignores = ["map", "id", "hashCode", "type"];
    for (var key in circleOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, circleOptions[key]);
        }
    }
};

utils.extend(Circle, BaseClass);

Circle.prototype.getPluginName = function() {
    return this.map.getId() + "-circle";
};

Circle.prototype.getHashCode = function() {
    return this.hashCode;
};

Circle.prototype.getMap = function() {
    return this.map;
};
Circle.prototype.getId = function() {
    return this.id;
};
Circle.prototype.getCenter = function() {
    return this.get('center');
};
Circle.prototype.getRadius = function() {
    return this.get('radius');
};
Circle.prototype.getStrokeColor = function() {
    return this.get('strokeColor');
};
Circle.prototype.getStrokeWidth = function() {
    return this.get('strokeWidth');
};
Circle.prototype.getZIndex = function() {
    return this.get('zIndex');
};
Circle.prototype.getVisible = function() {
    return this.get('visible');
};
Circle.prototype.setCenter = function(center) {
    this.set('center', center);
};
Circle.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
};
Circle.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
};
Circle.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
};
Circle.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
};
Circle.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
};
Circle.prototype.setRadius = function(radius) {
    this.set('radius', radius);
};

Circle.prototype.remove = function() {
    this.trigger(this.id + "_remove");
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'remove', [this.getId()]);
};

Circle.prototype.getBounds = function() {
  var d2r = Math.PI / 180;   // degrees to radians
  var r2d = 180 / Math.PI;   // radians to degrees
  var earthsradius = 3963.189; // 3963 is the radius of the earth in miles
  var radius = this.get("radius");
  var center = this.get("center");
  radius *= 0.000621371192;

  var points = 32;

  // find the raidus in lat/lon
  var rlat = (radius / earthsradius) * r2d;
  var rlng = rlat / Math.cos(center.lat * d2r);

  var bounds = new LatLngBounds();
  var ex, ey;
  for (var i = 0; i < points + 1; i++) {
    var theta = Math.PI * (i / (points/2));
    ey = center.lng + (rlng * Math.cos(theta)); // center a + radius x * cos(theta)
    ex = center.lat + (rlat * Math.sin(theta)); // center b + radius y * sin(theta)
    bounds.extend({lat: ex, lng: ey});
  }
  return bounds;
};

module.exports = Circle;
