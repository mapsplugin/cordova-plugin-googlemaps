var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * KmlOverlay Class
 *****************************************************************************/
var KmlOverlay = function(map, kmlOverlayId, kmlOverlayOptions) {
    BaseClass.apply(this);

    var self = this;
    self._overlays = [];
    //self.set("visible", kmlOverlayOptions.visible === undefined ? true : kmlOverlayOptions.visible);
    //self.set("zIndex", kmlOverlayOptions.zIndex || 0);
    Object.defineProperty(self, "id", {
        value: kmlOverlayId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "KmlOverlay",
        writable: false
    });
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    var ignores = ["map", "id", "hashCode", "type"];
    for (var key in kmlOverlayOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, kmlOverlayOptions[key]);
        }
    }


    document.addEventListener(map.getId()  + "_" + kmlOverlayId, function(event) {
console.log(event.placeMarkId);
    });
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

KmlOverlay.prototype.remove = function() {
    var layerId = this.id,
        self = this;

    //this.trigger("_REMOVE");
    setTimeout(function() {
        self.trigger(self.id + "_remove");
    }, 1000);
};

module.exports = KmlOverlay;
