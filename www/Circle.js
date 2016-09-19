var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
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
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setCenter', [this.getId(), center.lat, center.lng]);
};
Circle.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setFillColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Circle.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setStrokeColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Circle.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setStrokeWidth', [this.getId(), width]);
};
Circle.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setVisible', [this.getId(), visible]);
};
Circle.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setZIndex', [this.getId(), zIndex]);
};
Circle.prototype.setRadius = function(radius) {
    this.set('radius', radius);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setRadius', [this.getId(), radius]);
};

Circle.prototype.remove = function() {
    this.trigger(this.id + "_remove");
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'remove', [this.getId()]);
};

module.exports = Circle;
