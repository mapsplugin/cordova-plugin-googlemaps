var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    BaseArrayClass = require('./BaseArrayClass');

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
    Object.defineProperty(self, "hashCode", {
        value: polylineOptions.hashCode,
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

    var pointsProperty = common.createMvcArray(polylineOptions.points);
    pointsProperty.on('set_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setPointAt', [polylineId, index, pointsProperty.getAt(index)]);
    });
    pointsProperty.on('insert_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'insertPointAt', [polylineId, index, pointsProperty.getAt(index)]);
    });
    pointsProperty.on('remove_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'removePointAt', [polylineId, index]);
    });

    Object.defineProperty(self, "points", {
        value: pointsProperty,
        writable: false
    });

    var ignores = ["map", "id", "hashCode", "type", "points"];
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

Polyline.prototype.getPluginName = function() {
    return this.map.getId() + "-polyline";
};

Polyline.prototype.getHashCode = function() {
    return this.hashCode;
};

Polyline.prototype.setPoints = function(points) {
    var mvcArray = this.points;
    mvcArray.empty();

    var i,
        path = [];

    for (i = 0; i < points.length; i++) {
        mvcArray.push({
            "lat": points[i].lat,
            "lng": points[i].lng
        });
    }
};
Polyline.prototype.getPoints = function() {
    return this.points;
};
Polyline.prototype.setStrokeColor = function(color) {
    this.set('color', color);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setStrokeColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Polyline.prototype.getStrokeColor = function() {
    return this.get('color');
};
Polyline.prototype.setStrokeWidth = function(width) {
    this.set('width', width);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setStrokeWidth', [this.getId(), width]);
};
Polyline.prototype.getStrokeWidth = function() {
    return this.get('width');
};
Polyline.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setVisible', [this.getId(), visible]);
};
Polyline.prototype.getVisible = function() {
    return this.get('visible');
};
Polyline.prototype.setClickable = function(clickable) {
    clickable = common.parseBoolean(clickable);
    this.set('clickable', clickable);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setClickable', [this.getId(), clickable]);
};
Polyline.prototype.getClickable = function() {
    return this.get('clickable');
};
Polyline.prototype.setGeodesic = function(geodesic) {
    geodesic = common.parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setGeodesic', [this.getId(), geodesic]);
};
Polyline.prototype.getGeodesic = function() {
    return this.get('geodesic');
};
Polyline.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setZIndex', [this.getId(), zIndex]);
};
Polyline.prototype.getZIndex = function() {
    return this.get('zIndex');
};

Polyline.prototype.getMap = function() {
    return this.map;
};

Polyline.prototype.remove = function() {
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'remove', [this.getId()]);
    this.trigger(this.id + "_remove");
    this.get("points").clear();
    this.off();
};
module.exports = Polyline;
