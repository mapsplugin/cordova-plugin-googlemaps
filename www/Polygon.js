var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    BaseArrayClass = require('./BaseArrayClass');

/*****************************************************************************
 * Polygon Class
 *****************************************************************************/
var Polygon = function(map, polygonId, polygonOptions) {
    BaseClass.apply(this);

    var self = this;
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "hashCode", {
        value: polygonOptions.hashCode,
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: polygonId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "Polygon",
        writable: false
    });

    //--------------------------
    // points property
    //--------------------------
    var pointsProperty = common.createMvcArray(polygonOptions.points);
    pointsProperty.on('set_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setPointAt', [polygonId, index, pointsProperty.getAt(index)]);
    });
    pointsProperty.on('insert_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'insertPointAt', [polygonId, index, pointsProperty.getAt(index)]);
    });
    pointsProperty.on('remove_at', function(index) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'removePointAt', [polygonId, index]);
    });

    Object.defineProperty(self, "points", {
        value: pointsProperty,
        writable: false
    });
    //--------------------------
    // holes property
    //--------------------------
    var holesProperty = common.createMvcArray(polygonOptions.holes);
    var _holes = common.createMvcArray(holesProperty.getArray());

    holesProperty.on('set_at', function(index) {
      _holes.setAt(index, holesProperty.get(index));
    });
    holesProperty.on('remove_at', function(index) {
      _holes.removeAt(index);
    });
    holesProperty.on('insert_at', function(index) {
      var array = holesProperty.getAt(index);
      if (array && (array instanceof Array || Array.isArray(array))) {
        array = common.createMvcArray(array);
      }
      array.on('insert_at', function(idx) {
        var position = array.getAt(idx);
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'insertPointOfHoleAt', [polygonId, index, idx, position]);
      });
      array.on('set_at', function(idx) {
        var position = array.getAt(idx);
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'setPointOfHoleAt', [polygonId, index, idx, position]);
      });
      array.on('remove_at', function(idx) {
        cordova.exec(null, self.errorHandler, self.getPluginName(), 'removePointOfHoleAt', [polygonId, index, idx]);
      });

      cordova.exec(null, self.errorHandler, self.getPluginName(), 'insertHoleAt', [polygonId, index, array.getArray()]);
    });

    Object.defineProperty(self, "holes", {
        value: holesProperty,
        writable: false
    });

    //--------------------------
    // other properties
    //--------------------------
    var ignores = ["map", "id", "hashCode", "type", "points", "holes"];
    for (var key in polygonOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, polygonOptions[key]);
        }
    }
};

utils.extend(Polygon, BaseClass);

Polygon.prototype.remove = function() {
    this.trigger(this.id + "_remove");
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
};
Polygon.prototype.getPluginName = function() {
    return this.map.getId() + "-polygon";
};

Polygon.prototype.getHashCode = function() {
    return this.hashCode;
};
Polygon.prototype.getMap = function() {
    return this.map;
};
Polygon.prototype.getId = function() {
    return this.id;
};

Polygon.prototype.setPoints = function(points) {
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
Polygon.prototype.getPoints = function() {
    return this.points;
};
Polygon.prototype.setHoles = function(holes) {
    argscheck.checkArgs('A', 'Polygon.setHoles', arguments);
    this.set('holes', holes);
    holes = holes || [];
    if (holes.length > 0 && !utils.isArray(holes[0])) {
      holes = [holes];
    }
    holes = holes.map(function(hole) {
      if (!utils.isArray(hole)) {
        return [];
      }
      return hole.map(function(latLng) {
        return {lat: latLng.lat, lng: latLng.lng};
      });
    });
    exec(null, this.errorHandler, this.getPluginName(), 'setHoles', [this.getId(), holes]);
};
Polygon.prototype.getHoles = function() {
    return this.holes;
};
Polygon.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    exec(null, this.errorHandler, this.getPluginName(), 'setFillColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Polygon.prototype.getFillColor = function() {
    return this.get('fillColor');
};
Polygon.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    exec(null, this.errorHandler, this.getPluginName(), 'setStrokeColor', [this.getId(), common.HTMLColor2RGBA(color, 0.75)]);
};
Polygon.prototype.getStrokeColor = function() {
    return this.get('strokeColor');
};
Polygon.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    exec(null, this.errorHandler, this.getPluginName(), 'setStrokeWidth', [this.getId(), width]);
};
Polygon.prototype.getStrokeWidth = function() {
    return this.get('strokeWidth');
};
Polygon.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    exec(null, this.errorHandler, this.getPluginName(), 'setVisible', [this.getId(), visible]);
};
Polygon.prototype.getVisible = function() {
    return this.get('visible');
};
Polygon.prototype.setGeodesic = function(geodesic) {
    geodesic = common.parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    exec(null, this.errorHandler, this.getPluginName(), 'setGeodesic', [this.getId(), geodesic]);
};
Polygon.prototype.getGeodesic = function() {
    return this.get('geodesic');
};
Polygon.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    exec(null, this.errorHandler, this.getPluginName(), 'setZIndex', [this.getId(), zIndex]);
};
Polygon.prototype.getZIndex = function() {
    return this.get('zIndex');
};

module.exports = Polygon;
