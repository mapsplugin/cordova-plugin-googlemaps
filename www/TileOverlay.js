var utils = require('cordova/utils'),
  common = require('./Common'),
  Overlay = require('./Overlay');

/*****************************************************************************
 * TileOverlay Class
 *****************************************************************************/
var TileOverlay = function (map, tileOverlayOptions, _exec) {
  Overlay.call(this, map, tileOverlayOptions, 'TileOverlay', _exec);

  var self = this;

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on('fadeIn_changed', function () {
    var fadeIn = self.get('fadeIn');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setFadeIn', [self.getId(), fadeIn]);
  });
  self.on('opacity_changed', function () {
    var opacity = self.get('opacity');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setOpacity', [self.getId(), opacity]);
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

utils.extend(TileOverlay, Overlay);

TileOverlay.prototype.getPluginName = function () {
  return this.map.getId() + '-tileoverlay';
};

TileOverlay.prototype.getHashCode = function () {
  return this.hashCode;
};

TileOverlay.prototype.getMap = function () {
  return this.map;
};
TileOverlay.prototype.getId = function () {
  return this.id;
};
TileOverlay.prototype.getTileSize = function () {
  return this.get('tileSize');
};
TileOverlay.prototype.getZIndex = function () {
  return this.get('zIndex');
};
TileOverlay.prototype.setZIndex = function (zIndex) {
  this.set('zIndex', zIndex);
};
TileOverlay.prototype.setFadeIn = function (fadeIn) {
  fadeIn = common.parseBoolean(fadeIn);
  this.set('fadeIn', fadeIn);
};
TileOverlay.prototype.getFadeIn = function () {
  return this.get('fadeIn');
};
TileOverlay.prototype.setVisible = function (visible) {
  visible = common.parseBoolean(visible);
  this.set('visible', visible);
};
TileOverlay.prototype.getOpacity = function () {
  return this.get('opacity');
};
TileOverlay.prototype.setOpacity = function (opacity) {
  if (!opacity && opacity !== 0) {
    console.log('opacity value must be int or double');
    return false;
  }
  this.set('opacity', opacity);
};
TileOverlay.prototype.getVisible = function () {
  return this.get('visible');
};

TileOverlay.prototype.remove = function (callback) {
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
  self.trigger(self.id + '_remove');

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

module.exports = TileOverlay;
