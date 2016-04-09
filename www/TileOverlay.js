var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

var PLUGIN_NAME = "TileOverlay";

/*****************************************************************************
 * TileOverlay Class
 *****************************************************************************/
var TileOverlay = function(map, tileOverlayId, tileOverlayOptions) {
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
    var ignores = ["map", "id", "type"];
    for (var key in tileOverlayOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, tileOverlayOptions[key]);
        }
    }
};


utils.extend(TileOverlay, BaseClass);

TileOverlay.prototype.getMap = function() {
    return this.map;
};
TileOverlay.prototype.clearTileCache = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'clearTileCache', [this.getId()]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setZIndex', [this.getId(), zIndex]);
};
TileOverlay.prototype.setFadeIn = function(fadeIn) {
    fadeIn = common.parseBoolean(fadeIn);
    this.set('fadeIn', fadeIn);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setFadeIn', [this.getId(), fadeIn]);
};
TileOverlay.prototype.getFadeIn = function() {
    return this.get('fadeIn');
};
TileOverlay.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setVisible', [this.getId(), visible]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setOpacity', [this.getId(), opacity]);
};
TileOverlay.prototype.getVisible = function() {
    return this.get('visible');
};

module.exports = TileOverlay;
