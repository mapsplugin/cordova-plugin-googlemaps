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

    var mvcArray;
    if (polylineOptions.points && typeof polylineOptions.points.getArray === "function") {
        mvcArray = new BaseArrayClass(polylineOptions.points.getArray());
        polylineOptions.points.on('set_at', function(index) {
            var position = polylineOptions.points.getAt(index);
            var value = {
              lat: position.lat,
              lng: position.lng
            };
            mvcArray.setAt(index, value);
        });
        polylineOptions.points.on('insert_at', function(index) {
            var position = polylineOptions.points.getAt(index);
            var value = {
              lat: position.lat,
              lng: position.lng
            };
            mvcArray.insertAt(index, value);
        });
        polylineOptions.points.on('remove_at', function(index) {
            mvcArray.removeAt(index);
        });

    } else {
        mvcArray = new BaseArrayClass(polylineOptions.points.slice(0));
    }
    mvcArray.on('set_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setPointAt', [polylineId, index, mvcArray.getAt(index)]);
    });

    mvcArray.on('insert_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'insertPointAt', [polylineId, index, mvcArray.getAt(index)]);
    });

    mvcArray.on('remove_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'removePointAt', [polylineId, index]);
    });

    Object.defineProperty(self, "points", {
        value: mvcArray,
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
        }, true);
    }
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setPoints', [this.getId(), mvcArray.getArray()]);
};
Polyline.prototype.getPoints = function() {
    return this.points;
};
Polyline.prototype.setColor = function(color) {
    this.set('color', color);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Polyline.prototype.getColor = function() {
    return this.get('color');
};
Polyline.prototype.setWidth = function(width) {
    this.set('width', width);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setWidth', [this.getId(), width]);
};
Polyline.prototype.getWidth = function() {
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
