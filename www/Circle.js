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

    //-----------------------------------------------
    // Sets the initialize option to each property
    //-----------------------------------------------
    var ignores = ["map", "id", "hashCode", "type"];
    for (var key in circleOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, circleOptions[key]);
        }
    }

    //-----------------------------------------------
    // Sets event listeners
    //-----------------------------------------------
    self.on("center_changed", function(oldValue, center) {
        exec(null, self.errorHandler, self.getPluginName(), 'setCenter', [self.getId(), center.lat, center.lng]);
    });
    self.on("fillColor_changed", function(oldValue, color) {
        exec(null, self.errorHandler, self.getPluginName(), 'setFillColor', [self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
    });
    self.on("strokeColor_changed", function(oldValue, color) {
        exec(null, self.errorHandler, self.getPluginName(), 'setStrokeColor', [self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
    });
    self.on("strokeWidth_changed", function(oldValue, width) {
        exec(null, self.errorHandler, self.getPluginName(), 'setStrokeWidth', [self.getId(), width]);
    });
    self.on("clickable_changed", function(oldValue, clickable) {
        exec(null, self.errorHandler, self.getPluginName(), 'setClickable', [self.getId(), clickable]);
    });
    self.on("radius_changed", function(oldValue, radius) {
        exec(null, self.errorHandler, self.getPluginName(), 'setRadius', [self.getId(), radius]);
    });
    self.on("zIndex_changed", function(oldValue, zIndex) {
        exec(null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
    });
    self.on("visible_changed", function(oldValue, visible) {
        exec(null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
    });

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
Circle.prototype.getClickable = function() {
    return this.get('clickable');
};
Circle.prototype.setCenter = function(center) {
    this.set('center', center);
    return this;
};
Circle.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    return this;
};
Circle.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    return this;
};
Circle.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    return this;
};
Circle.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    return this;
};
Circle.prototype.setClickable = function(clickable) {
    clickable = common.parseBoolean(clickable);
    this.set('clickable', clickable);
    return this;
};
Circle.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    return this;
};
Circle.prototype.setRadius = function(radius) {
    this.set('radius', radius);
    return this;
};

Circle.prototype.remove = function() {
    this.trigger(this.id + "_remove");
    exec(null, this.errorHandler, this.getPluginName(), 'remove', [this.getId()]);
};

Circle.prototype.getBounds = function() {
  var d2r = Math.PI / 180;   // degrees to radians
  var r2d = 180 / Math.PI;   // radians to degrees
  var earthsradius = 3963.189; // 3963 is the radius of the earth in miles
  var radius = this.get("radius");
  var center = this.get("center");
  radius *= 0.000621371192;

  // find the raidus in lat/lon
  var rlat = (radius / earthsradius) * r2d;
  var rlng = rlat / Math.cos(center.lat * d2r);

  var bounds = new LatLngBounds();
  var ex, ey;
  for (var i = 0; i < 360; i += 90) {
    ey = center.lng + (rlng * Math.cos(i * d2r)); // center a + radius x * cos(theta)
    ex = center.lat + (rlat * Math.sin(i * d2r)); // center b + radius y * sin(theta)
    bounds.extend({lat: ex, lng: ey});
  }
  return bounds;
};

module.exports = Circle;
