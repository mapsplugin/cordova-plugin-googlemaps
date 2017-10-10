var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * TileOverlay Class
 *****************************************************************************/
var exec;
var TileOverlay = function(map, tileOverlayId, tileOverlayOptions, _exec) {
    exec = _exec;
    BaseClass.apply(this);

    var self = this;
    Object.defineProperty(self, "id", {
        value: tileOverlayId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "TileOverlay",
        writable: false
    });
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "_isReady", {
        value: true,
        writable: false
    });
    //-----------------------------------------------
    // Sets the initialize option to each property
    //-----------------------------------------------
    var ignores = ["map", "id", "hashCode", "type"];
    for (var key in tileOverlayOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, tileOverlayOptions[key]);
        }
    }

    //-----------------------------------------------
    // Sets event listeners
    //-----------------------------------------------
    self.on("fadeIn_changed", function(oldValue, fadeIn) {
        exec.call(self, null, self.errorHandler, self.getPluginName(), 'setFadeIn', [self.getId(), fadeIn]);
    });
    self.on("opacity_changed", function(oldValue, opacity) {
        exec.call(self, null, self.errorHandler, self.getPluginName(), 'setOpacity', [self.getId(), opacity]);
    });
    self.on("zIndex_changed", function(oldValue, zIndex) {
        exec.call(self, null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
    });
    self.on("visible_changed", function(oldValue, visible) {
        exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
    });
};


utils.extend(TileOverlay, BaseClass);

TileOverlay.prototype.getPluginName = function() {
    return this.map.getId() + "-tileoverlay";
};

TileOverlay.prototype.getHashCode = function() {
    return this.hashCode;
};

TileOverlay.prototype.getMap = function() {
    return this.map;
};
TileOverlay.prototype.getId = function() {
    return this.id;
};
TileOverlay.prototype.getTileSize = function() {
    return this.get("tileSize");
};
TileOverlay.prototype.getZIndex = function() {
    return this.get("zIndex");
};
TileOverlay.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
};
TileOverlay.prototype.setFadeIn = function(fadeIn) {
    fadeIn = common.parseBoolean(fadeIn);
    this.set('fadeIn', fadeIn);
};
TileOverlay.prototype.getFadeIn = function() {
    return this.get('fadeIn');
};
TileOverlay.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
};
TileOverlay.prototype.getOpacity = function() {
    return this.get('opacity');
};
TileOverlay.prototype.setOpacity = function(opacity) {
    if (!opacity && opacity !== 0) {
        console.log('opacity value must be int or double');
        return false;
    }
    this.set('opacity', opacity);
};
TileOverlay.prototype.getVisible = function() {
    return this.get('visible');
};

TileOverlay.prototype.remove = function() {
    this.trigger(this.id + "_remove");
    exec.call(this, null, this.errorHandler, this.getPluginName(), 'remove', [this.getId()]);
    Object.defineProperty(this, "_isRemoved", {
        value: true,
        writable: false
    });
    this.destroy();
};

module.exports = TileOverlay;
