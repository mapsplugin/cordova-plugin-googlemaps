var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

var PLUGIN_NAME = "Polyline";

/*****************************************************************************
 * Polyline Class
 *****************************************************************************/
var Polyline = function(map, polylineId, polylineOptions) {
    BaseClass.apply(this);

    var self = this;
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: polylineId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "Polyline",
        writable: false
    });

    var ignores = ["map", "id", "type"];
    for (var key in polylineOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, polylineOptions[key]);
        }
    }
};

utils.extend(Polyline, BaseClass);

Polyline.prototype.getId = function() {
    return this.id;
};

Polyline.prototype.setPoints = function(points) {
    this.set('points', points);
    var i,
        path = [];
    for (i = 0; i < points.length; i++) {
        path.push({
            "lat": points[i].lat,
            "lng": points[i].lng
        });
    }
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setPoints', [this.getId(), path]);
};
Polyline.prototype.getPoints = function() {
    return this.get("points");
};
Polyline.prototype.setColor = function(color) {
    this.set('color', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Polyline.prototype.getColor = function() {
    return this.get('color');
};
Polyline.prototype.setWidth = function(width) {
    this.set('width', width);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setWidth', [this.getId(), width]);
};
Polyline.prototype.getWidth = function() {
    return this.get('width');
};
Polyline.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setVisible', [this.getId(), visible]);
};
Polyline.prototype.getVisible = function() {
    return this.get('visible');
};
Polyline.prototype.setGeodesic = function(geodesic) {
    geodesic = common.parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setGeodesic', [this.getId(), geodesic]);
};
Polyline.prototype.getGeodesic = function() {
    return this.get('geodesic');
};
Polyline.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setZIndex', [this.getId(), zIndex]);
};
Polyline.prototype.getZIndex = function() {
    return this.get('zIndex');
};

Polyline.prototype.getMap = function() {
    return this.map;
};

module.exports = Polyline;
