var utils = require('cordova/utils'),
  common = require('./Common'),
  Overlay = require('./Overlay');

/*****************************************************************************
 * Polyline Class
 *****************************************************************************/
var Polyline = function (map, polylineOptions, _exec) {
  Overlay.call(this, map, polylineOptions, 'Polyline', _exec, {
    'ignores': ['points']
  });

  var self = this;
  var polylineId = this.getId();

  var pointsProperty = common.createMvcArray(polylineOptions.points);
  pointsProperty.on('set_at', function (index) {
    if (self._isRemoved) return;
    var value = common.getLatLng(pointsProperty.getAt(index));
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setPointAt', [polylineId, index, value]);
  });
  pointsProperty.on('insert_at', function (index) {
    if (self._isRemoved) return;
    var value = common.getLatLng(pointsProperty.getAt(index));
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'insertPointAt', [polylineId, index, value]);
  });
  pointsProperty.on('remove_at', function (index) {
    if (self._isRemoved) return;
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'removePointAt', [polylineId, index]);
  });

  Object.defineProperty(self, 'points', {
    value: pointsProperty,
    writable: false
  });
  //-----------------------------------------------
  // Sets the initialize option to each property
  //-----------------------------------------------
  // var ignores = ['map', 'id', 'hashCode', 'type', 'points'];
  // for (var key in polylineOptions) {
  //   if (ignores.indexOf(key) === -1) {
  //     self.set(key, polylineOptions[key]);
  //   }
  // }

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on('geodesic_changed', function () {
    if (self._isRemoved) return;
    var geodesic = self.get('geodesic');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setGeodesic', [self.getId(), geodesic]);
  });
  self.on('zIndex_changed', function () {
    if (self._isRemoved) return;
    var zIndex = self.get('zIndex');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
  });
  self.on('clickable_changed', function () {
    if (self._isRemoved) return;
    var clickable = self.get('clickable');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setClickable', [self.getId(), clickable]);
  });
  self.on('visible_changed', function () {
    if (self._isRemoved) return;
    var visible = self.get('visible');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
  });
  self.on('strokeWidth_changed', function () {
    if (self._isRemoved) return;
    var strokeWidth = self.get('strokeWidth');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setStrokeWidth', [self.getId(), strokeWidth]);
  });
  self.on('strokeColor_changed', function () {
    if (self._isRemoved) return;
    var color = self.get('strokeColor');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setStrokeColor', [self.getId(), common.HTMLColor2RGBA(color, 0.75)]);
  });

};

utils.extend(Polyline, Overlay);

Polyline.prototype.setPoints = function (points) {
  var self = this;
  var mvcArray = self.points;
  mvcArray.empty(true);

  var i;

  for (i = 0; i < points.length; i++) {
    mvcArray.push({
      'lat': points[i].lat,
      'lng': points[i].lng
    }, true);
  }
  self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setPoints', [self.id, mvcArray.getArray()]);
  return self;
};
Polyline.prototype.getPoints = function () {
  return this.points;
};
Polyline.prototype.setStrokeColor = function (color) {
  this.set('strokeColor', color);
  return this;
};
Polyline.prototype.getStrokeColor = function () {
  return this.get('strokeColor');
};
Polyline.prototype.setStrokeWidth = function (width) {
  this.set('strokeWidth', width);
  return this;
};
Polyline.prototype.getStrokeWidth = function () {
  return this.get('strokeWidth');
};
Polyline.prototype.setVisible = function (visible) {
  visible = common.parseBoolean(visible);
  this.set('visible', visible);
  return this;
};
Polyline.prototype.getVisible = function () {
  return this.get('visible');
};
Polyline.prototype.setClickable = function (clickable) {
  clickable = common.parseBoolean(clickable);
  this.set('clickable', clickable);
  return this;
};
Polyline.prototype.getClickable = function () {
  return this.get('clickable');
};
Polyline.prototype.setGeodesic = function (geodesic) {
  geodesic = common.parseBoolean(geodesic);
  this.set('geodesic', geodesic);
  return this;
};
Polyline.prototype.getGeodesic = function () {
  return this.get('geodesic');
};
Polyline.prototype.setZIndex = function (zIndex) {
  this.set('zIndex', zIndex);
  return this;
};
Polyline.prototype.getZIndex = function () {
  return this.get('zIndex');
};

Polyline.prototype.remove = function (callback) {
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

  var resolver = function (resolve, reject) {
    self.exec.call(self,
      function () {
        self.destroy();
        resolve.call(self);
      },
      reject.bind(self),
      self.getPluginName(), 'remove', [self.getId()], {
        remove: true
      });
  };

  var result;
  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    result = new Promise(resolver);
  }

  if (self.points) {
    self.points.empty();
  }
  self.trigger(self.id + '_remove');

  return result;
};
module.exports = Polyline;
