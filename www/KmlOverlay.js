var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    BaseArrayClass = require('./BaseArrayClass');

/*****************************************************************************
 * KmlOverlay Class
 *****************************************************************************/
var exec;
var KmlOverlay = function(map, kmlId, viewport, overlays) {
    BaseClass.apply(this);
    console.log(overlays);

    var self = this;
    self._overlays = [];
    //self.set("visible", kmlOverlayOptions.visible === undefined ? true : kmlOverlayOptions.visible);
    //self.set("zIndex", kmlOverlayOptions.zIndex || 0);
    Object.defineProperty(self, "_isReady", {
        value: true,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "KmlOverlay",
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: kmlId,
        writable: false
    });
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "viewport", {
        value: viewport,
        writable: false
    });
    Object.defineProperty(self, "overlays", {
        value: overlays,
        writable: false
    });
/*
    var ignores = ["map", "id", "hashCode", "type"];
    for (var key in kmlOverlayOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, kmlOverlayOptions[key]);
        }
    }
*/
};

utils.extend(KmlOverlay, BaseClass);

KmlOverlay.prototype.getPluginName = function() {
    return this.map.getId() + "-kmloverlay";
};

KmlOverlay.prototype.getHashCode = function() {
    return this.hashCode;
};

KmlOverlay.prototype.getOverlays = function() {
    return this._overlays;
};
KmlOverlay.prototype.getMap = function() {
    return this.map;
};
KmlOverlay.prototype.getId = function() {
    return this.id;
};

KmlOverlay.prototype.remove = function(callback) {
    var self = this;
    if (self._isRemoved) {
      return;
    }
    Object.defineProperty(self, "_isRemoved", {
        value: true,
        writable: false
    });


    var removeChildren = function(children, cb) {
      if (!children || !utils.isArray(children)) {
        return cb();
      }

      var baseArray = new BaseArrayClass(children);
      baseArray.forEach(function(child, next) {
        if ('remove' in child &&
          typeof child.remove === 'function') {
          child.remove(next);
          return;
        }
        if ('children' in child &&
          utils.isArray(child.children)) {
          removeChildren(child.children, next);
        } else {
          next();
        }
      }, cb);
    };

    removeChildren(self.overlays, function() {
      self.destroy();
      if (typeof callback === "function") {
          callback.call(self);
      }
    });
};

module.exports = KmlOverlay;
