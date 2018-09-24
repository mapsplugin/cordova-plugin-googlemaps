var argscheck = require('cordova/argscheck'),
  utils = require('cordova/utils'),
  common = require('./Common'),
  Overlay = require('./Overlay');

/*****************************************************************************
 * FusionTableOverlay Class
 *****************************************************************************/
var FusionTableOverlay = function (map, FusionTableOverlayOptions, _exec) {
  Overlay.call(this, map, FusionTableOverlayOptions, 'FusionTableOverlay', _exec);

  var self = this;

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on("visible_changed", function () {
    var visible = self.get("visible");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
  });
};

utils.extend(FusionTableOverlay, Overlay);

FusionTableOverlay.prototype.getPluginName = function () {
  return this.map.getId() + "-fusiontableoverlay";
};

FusionTableOverlay.prototype.getHashCode = function () {
  return this.hashCode;
};

FusionTableOverlay.prototype.getMap = function () {
  return this.map;
};
FusionTableOverlay.prototype.getId = function () {
  return this.id;
};
FusionTableOverlay.prototype.setVisible = function (visible) {
  visible = common.parseBoolean(visible);
  this.set('visible', visible);
};
FusionTableOverlay.prototype.getVisible = function () {
  return this.get('visible');
};

FusionTableOverlay.prototype.remove = function (callback) {
  var self = this;
  if (self._isRemoved) {
    if (typeof callback === "function") {
      return;
    } else {
      return Promise.resolve();
    }
  }
  Object.defineProperty(self, "_isRemoved", {
    value: true,
    writable: false
  });
  self.trigger(self.id + "_remove");

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

  if (typeof callback === "function") {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }

};

module.exports = FusionTableOverlay;
