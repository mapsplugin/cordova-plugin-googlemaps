var utils = require('cordova/utils'),
  common = require('./Common'),
  Overlay = require('./Overlay');

/*****************************************************************************
 * Polygon Class
 *****************************************************************************/
var Polygon = function (map, polygonOptions, _exec) {
  Overlay.call(this, map, polygonOptions, 'Polygon', _exec);

  var self = this;
  var polygonId = this.getId();

  //--------------------------
  // points property
  //--------------------------
  var pointsProperty = common.createMvcArray(polygonOptions.points);
  pointsProperty.on('set_at', function (index) {
    var value = common.getLatLng(pointsProperty.getAt(index));
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setPointAt', [self.map.getId(), polygonId, index, value]);
  });
  pointsProperty.on('insert_at', function (index) {
    var value = common.getLatLng(pointsProperty.getAt(index));
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'insertPointAt', [self.map.getId(), polygonId, index, value]);
  });
  pointsProperty.on('remove_at', function (index) {
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'removePointAt', [self.map.getId(), polygonId, index]);
  });

  Object.defineProperty(self, 'points', {
    value: pointsProperty,
    writable: false
  });
  //--------------------------
  // holes property
  //--------------------------
  var holesProperty = common.createMvcArray(polygonOptions.holes);
  var _holes = common.createMvcArray(holesProperty.getArray());

  holesProperty.on('set_at', function (index) {
    var value = common.getLatLng(holesProperty.getAt(index));
    _holes.setAt(index, value);
  });
  holesProperty.on('remove_at', function (index) {
    _holes.removeAt(index);
  });
  holesProperty.on('insert_at', function (index) {
    var array = holesProperty.getAt(index);
    if (array && (array instanceof Array || Array.isArray(array))) {
      array = common.createMvcArray(array);
    }
    array.on('insert_at', function (idx) {
      var value = common.getLatLng(array.getAt(idx));
      self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'insertPointOfHoleAt', [self.map.getId(), polygonId, index, idx, value]);
    });
    array.on('set_at', function (idx) {
      var value = common.getLatLng(array.getAt(idx));
      self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setPointOfHoleAt', [self.map.getId(), polygonId, index, idx, value]);
    });
    array.on('remove_at', function (idx) {
      self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'removePointOfHoleAt', [self.map.getId(), polygonId, index, idx]);
    });

    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'insertHoleAt', [self.map.getId(), polygonId, index, array.getArray()]);
  });

  Object.defineProperty(self, 'holes', {
    value: holesProperty,
    writable: false
  });

  //--------------------------
  // other properties
  //--------------------------.
  // var ignores = ['map', '__pgmId', 'hashCode', 'type', 'points', 'holes'];
  // for (var key in polygonOptions) {
  //   if (ignores.indexOf(key) === -1) {
  //     self.set(key, polygonOptions[key]);
  //   }
  // }
  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on('clickable_changed', function () {
    var clickable = self.get('clickable');
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setClickable', [self.map.getId(), self.getId(), clickable]);
  });
  self.on('geodesic_changed', function () {
    var geodesic = self.get('geodesic');
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setGeodesic', [self.map.getId(), self.getId(), geodesic]);
  });
  self.on('zIndex_changed', function () {
    var zIndex = self.get('zIndex');
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setZIndex', [self.map.getId(), self.getId(), zIndex]);
  });
  self.on('visible_changed', function () {
    var visible = self.get('visible');
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setVisible', [self.map.getId(), self.getId(), visible]);
  });
  self.on('strokeWidth_changed', function () {
    var width = self.get('strokeWidth');
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setStrokeWidth', [self.map.getId(), self.getId(), width]);
  });
  self.on('strokeColor_changed', function () {
    var color = self.get('strokeColor');
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setStrokeColor', [self.map.getId(), self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
  });
  self.on('fillColor_changed', function () {
    var color = self.get('fillColor');
    self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setFillColor', [self.map.getId(), self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
  });

};

utils.extend(Polygon, Overlay);

Polygon.prototype.remove = function (callback) {
  var self = this;
  if (self._isRemoved) {
    if (typeof callback === 'function') {
      return;
    } else {
      return Promise.resolve();
    }
  }
  Object.defineProperty(self, '_isRemoved', {
    value: true,
    writable: false
  });
  self.trigger(this.__pgmId + '_remove');

  var resolver = function(resolve, reject) {
    self.exec.call(self,
      function() {
        self.destroy();
        resolve.call(self);
      },
      reject.bind(self),
      'PluginPolygon', 'remove', [self.map.getId(), self.getId()], {
        remove: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }

};

Polygon.prototype.setPoints = function (points) {
  var self = this;
  var mvcArray = self.points;
  mvcArray.empty(true);

  var i;

  for (i = 0; i < points.length; i++) {
    mvcArray.push(common.getLatLng(points[i]), true);
  }
  self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setPoints', [self.map.getId(), self.__pgmId, mvcArray.getArray()]);
  return self;
};
Polygon.prototype.getPoints = function () {
  return this.points;
};
Polygon.prototype.setHoles = function (holes) {
  var self = this;
  var mvcArray = this.holes;
  mvcArray.empty(true);

  holes = holes || [];
  if (holes.length > 0 && !utils.isArray(holes[0])) {
    holes = [holes];
  }
  holes.forEach(function (hole) {
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
  self.exec.call(self, null, self.errorHandler, 'PluginPolygon', 'setHoles', [self.map.getId(), self.__pgmId, mvcArray.getArray()]);
  return this;
};
Polygon.prototype.getHoles = function () {
  return this.holes;
};
Polygon.prototype.setFillColor = function (color) {
  this.set('fillColor', color);
  return this;
};
Polygon.prototype.getFillColor = function () {
  return this.get('fillColor');
};
Polygon.prototype.setStrokeColor = function (color) {
  this.set('strokeColor', color);
  return this;
};
Polygon.prototype.getStrokeColor = function () {
  return this.get('strokeColor');
};
Polygon.prototype.setStrokeWidth = function (width) {
  this.set('strokeWidth', width);
  return this;
};
Polygon.prototype.getStrokeWidth = function () {
  return this.get('strokeWidth');
};
Polygon.prototype.setVisible = function (visible) {
  visible = common.parseBoolean(visible);
  this.set('visible', visible);
  return this;
};
Polygon.prototype.getVisible = function () {
  return this.get('visible');
};
Polygon.prototype.setClickable = function (clickable) {
  clickable = common.parseBoolean(clickable);
  this.set('clickable', clickable);
  return this;
};
Polygon.prototype.getClickable = function () {
  return this.get('clickable');
};
Polygon.prototype.setGeodesic = function (geodesic) {
  geodesic = common.parseBoolean(geodesic);
  this.set('geodesic', geodesic);
  return this;
};
Polygon.prototype.getGeodesic = function () {
  return this.get('geodesic');
};
Polygon.prototype.setZIndex = function (zIndex) {
  this.set('zIndex', zIndex);
  return this;
};
Polygon.prototype.getZIndex = function () {
  return this.get('zIndex');
};

module.exports = Polygon;
