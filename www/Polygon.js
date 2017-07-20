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
        var value = common.getLatLng(pointsProperty.getAt(index));
        exec(null, self.errorHandler, self.getPluginName(), 'setPointAt', [polygonId, index, value]);
    });
    pointsProperty.on('insert_at', function(index) {
        var value = common.getLatLng(pointsProperty.getAt(index));
        exec(null, self.errorHandler, self.getPluginName(), 'insertPointAt', [polygonId, index, value]);
    });
    pointsProperty.on('remove_at', function(index) {
        exec(null, self.errorHandler, self.getPluginName(), 'removePointAt', [polygonId, index]);
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
      var value = common.getLatLng(holesProperty.getAt(index));
      _holes.setAt(index,value);
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
        var value = common.getLatLng(array.getAt(idx));
        exec(null, self.errorHandler, self.getPluginName(), 'insertPointOfHoleAt', [polygonId, index, idx, value]);
      });
      array.on('set_at', function(idx) {
        var value = common.getLatLng(array.getAt(idx));
        exec(null, self.errorHandler, self.getPluginName(), 'setPointOfHoleAt', [polygonId, index, idx, value]);
      });
      array.on('remove_at', function(idx) {
        exec(null, self.errorHandler, self.getPluginName(), 'removePointOfHoleAt', [polygonId, index, idx]);
      });

      exec(null, self.errorHandler, self.getPluginName(), 'insertHoleAt', [polygonId, index, array.getArray()]);
    });

    Object.defineProperty(self, "holes", {
        value: holesProperty,
        writable: false
    });

    //--------------------------
    // other properties
    //--------------------------.
    var ignores = ["map", "id", "hashCode", "type", "points", "holes"];
    for (var key in polygonOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, polygonOptions[key]);
        }
    }
    //-----------------------------------------------
    // Sets event listeners
    //-----------------------------------------------
    self.on("clickable_changed", function(oldValue, clickable) {
        exec(null, self.errorHandler, self.getPluginName(), 'setClickable', [self.getId(), clickable]);
    });
    self.on("geodesic_changed", function(oldValue, geodesic) {
        exec(null, self.errorHandler, self.getPluginName(), 'setGeodesic', [self.getId(), geodesic]);
    });
    self.on("zIndex_changed", function(oldValue, zIndex) {
        exec(null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
    });
    self.on("visible_changed", function(oldValue, visible) {
        exec(null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
    });
    self.on("strokeWidth_changed", function(oldValue, width) {
        exec(null, self.errorHandler, self.getPluginName(), 'setStrokeWidth', [self.getId(), width]);
    });
    self.on("strokeColor_changed", function(oldValue, color) {
        exec(null, self.errorHandler, self.getPluginName(), 'setStrokeColor', [self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
    });
    self.on("fillColor_changed", function(oldValue, color) {
        exec(null, self.errorHandler, self.getPluginName(), 'setFillColor', [self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
    });

};

utils.extend(Polygon, BaseClass);

Polygon.prototype.remove = function() {
    this.trigger(this.id + "_remove");
    exec(null, this.errorHandler, this.getPluginName(), 'remove', [this.getId()]);
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
    var self = this;
    var mvcArray = self.points;
    mvcArray.empty(true);

    var i,
        path = [];

    for (i = 0; i < points.length; i++) {
        mvcArray.push(common.getLatLng(points[i]), true);
    }
    exec(null, self.errorHandler, self.getPluginName(), 'setPoints', [self.id, mvcArray.getArray()]);
    return self;
};
Polygon.prototype.getPoints = function() {
    return this.points;
};
Polygon.prototype.setHoles = function(holes) {
    var self = this;
    var mvcArray = this.holes;
    mvcArray.empty(true);

    holes = holes || [];
    if (holes.length > 0 && !utils.isArray(holes[0])) {
        holes = [holes];
    }
    holes.forEach(function(hole) {
        if (!utils.isArray(hole)) {
            hole = [hole];
            mvcArray.push(hole, true);
        } else {
            var newHole = [];
            for (var i = 0; i < hole.length; i++) {
              newHole.push(common.getLatLng(hole[i]));
            }
            mvcArray.push(newHole, true);
        }
    });
    exec(null, self.errorHandler, self.getPluginName(), 'setHoles', [self.id, mvcArray.getArray()]);
    return this;
};
Polygon.prototype.getHoles = function() {
    return this.holes;
};
Polygon.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    return this;
};
Polygon.prototype.getFillColor = function() {
    return this.get('fillColor');
};
Polygon.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    return this;
};
Polygon.prototype.getStrokeColor = function() {
    return this.get('strokeColor');
};
Polygon.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    return this;
};
Polygon.prototype.getStrokeWidth = function() {
    return this.get('strokeWidth');
};
Polygon.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    return this;
};
Polygon.prototype.getVisible = function() {
    return this.get('visible');
};
Polygon.prototype.setClickable = function(clickable) {
    clickable = common.parseBoolean(clickable);
    this.set('clickable', clickable);
    return this;
};
Polygon.prototype.getClickable = function() {
    return this.get('clickable');
};
Polygon.prototype.setGeodesic = function(geodesic) {
    geodesic = common.parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    return this;
};
Polygon.prototype.getGeodesic = function() {
    return this.get('geodesic');
};
Polygon.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    return this;
};
Polygon.prototype.getZIndex = function() {
    return this.get('zIndex');
};

module.exports = Polygon;
