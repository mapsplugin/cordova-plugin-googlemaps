var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
* GroundOverlay Class
*****************************************************************************/
var GroundOverlay = function(map, groundOverlayId, groundOverlayOptions) {
   BaseClass.apply(this);

   var self = this;
   groundOverlayOptions.visible = groundOverlayOptions.visible === undefined ? true : groundOverlayOptions.visible;
   groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 1;
   groundOverlayOptions.opacity = groundOverlayOptions.opacity || 1;
   groundOverlayOptions.bounds = groundOverlayOptions.bounds || [];
   groundOverlayOptions.anchor = groundOverlayOptions.anchor || [0, 0];
   groundOverlayOptions.bearing = groundOverlayOptions.bearing || 0;
   Object.defineProperty(self, "id", {
       value: groundOverlayId,
       writable: false
   });
   Object.defineProperty(self, "type", {
       value: "GroundOverlay",
       writable: false
   });
   Object.defineProperty(self, "map", {
       value: map,
       writable: false
   });
   Object.defineProperty(self, "hashCode", {
       value: groundOverlayOptions.hashCode,
       writable: false
   });
   var ignores = ["map", "id", "hashCode", "type"];
   for (var key in groundOverlayOptions) {
       if (ignores.indexOf(key) === -1) {
           self.set(key, groundOverlayOptions[key]);
       }
   }
};


utils.extend(GroundOverlay, BaseClass);

GroundOverlay.prototype.getPluginName = function() {
    return this.map.getId() + "-groundoverlay";
};

GroundOverlay.prototype.getHashCode = function() {
    return this.hashCode;
};

GroundOverlay.prototype.getMap = function() {
    return this.map;
};
GroundOverlay.prototype.getId = function() {
    return this.id;
};

GroundOverlay.prototype.setVisible = function(visible) {
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setVisible', [this.getId(), visible]);
};

GroundOverlay.prototype.getVisible = function() {
    return this.get('visible');
};

GroundOverlay.prototype.setImage = function(url) {
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setImage', [this.getId(), url]);
};

GroundOverlay.prototype.setBounds = function(points) {
    this.set('bounds', points);
    var i,
        bounds = [];
    for (i = 0; i < points.length; i++) {
        bounds.push({
            "lat": points[i].lat,
            "lng": points[i].lng
        });
    }
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setBounds', [this.getId(), bounds]);
};

GroundOverlay.prototype.getOpacity = function() {
    return this.get("opacity");
};

GroundOverlay.prototype.getBearing = function() {
    return this.get("bearing");
};

GroundOverlay.prototype.setOpacity = function(opacity) {
    if (!opacity && opacity !== 0) {
        console.log('opacity value must be int or double');
        return false;
    }
    this.set('opacity', opacity);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setOpacity', [this.getId(), opacity]);
};
GroundOverlay.prototype.setBearing = function(bearing) {
    this.set('bearing', bearing);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setBearing', [this.getId(), bearing]);
};

GroundOverlay.prototype.getZIndex = function() {
    return this.get("zIndex");
};

GroundOverlay.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, this.getPluginName(), 'setZIndex', [this.getId(), zIndex]);
};

module.exports = GroundOverlay;
