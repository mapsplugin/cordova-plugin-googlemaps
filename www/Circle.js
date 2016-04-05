var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

var PLUGIN_NAME = "Circle";

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

    var ignores = ["map", "id", "type"];
    for (var key in circleOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, circleOptions[key]);
        }
    }
};

utils.extend(Circle, BaseClass);

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
Circle.prototype.remove = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
};
Circle.prototype.setCenter = function(center) {
    this.set('center', center);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setCenter', [this.getId(), center.lat, center.lng]);
};
Circle.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setFillColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Circle.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setStrokeColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Circle.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setStrokeWidth', [this.getId(), width]);
};
Circle.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setVisible', [this.getId(), visible]);
};
Circle.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setZIndex', [this.getId(), zIndex]);
};
Circle.prototype.setRadius = function(radius) {
    this.set('radius', radius);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setRadius', [this.getId(), radius]);
};

module.exports = Circle;
