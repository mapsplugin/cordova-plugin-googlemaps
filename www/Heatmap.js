var utils = require('cordova/utils'),
  common = require('./Common'),
  LatLngBounds = require('./LatLngBounds'),
  Overlay = require('./Overlay');

/*****************************************************************************
 * Heatmap Class
 *****************************************************************************/
var Heatmap = function (map, heatmapOptions, _exec) {
  Overlay.call(this, map, heatmapOptions, 'Heatmap', _exec);

  var self = this;

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on('clickable_changed', function () {
    var clickable = self.get('clickable');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setClickable', [self.getId(), clickable]);
  });
  self.on('data_changed', function () {
    var data = self.get('data');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setData', [self.getId(), data]);
  });
  self.on('zIndex_changed', function () {
    var zIndex = self.get('zIndex');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
  });
  self.on('visible_changed', function () {
    var visible = self.get('visible');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
  });

};

utils.extend(Heatmap, Overlay);

Heatmap.prototype.getData = function () {
  return this.get('data');
};
Heatmap.prototype.getZIndex = function () {
  return this.get('zIndex');
};
Heatmap.prototype.getVisible = function () {
  return this.get('visible');
};
Heatmap.prototype.getClickable = function () {
  return this.get('clickable');
};
Heatmap.prototype.setVisible = function (visible) {
  visible = common.parseBoolean(visible);
  this.set('visible', visible);
  return this;
};
Heatmap.prototype.setClickable = function (clickable) {
  clickable = common.parseBoolean(clickable);
  this.set('clickable', clickable);
  return this;
};
Heatmap.prototype.setZIndex = function (zIndex) {
  this.set('zIndex', zIndex);
  return this;
};
Heatmap.prototype.setData = function (data) {
  this.set('data', data);
  return this;
};

Heatmap.prototype.remove = function (callback) {
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
  self.trigger(self.__pgmId + '_remove');

  var resolver = function(resolve, reject) {
    self.exec.call(self,
      function() {
        self.destroy();
        resolve.call(self);
      },
      reject.bind(self),
      self.getPluginName(), 'remove', [self.getId()], {
        remove: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }

};

module.exports = Heatmap;
