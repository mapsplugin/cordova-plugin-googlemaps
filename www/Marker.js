var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    LatLng = require('./LatLng'),
    BaseClass = require('./BaseClass');

var PLUGIN_NAME = "Marker";

/*****************************************************************************
 * Marker Class
 *****************************************************************************/
var Marker = function(map, id, markerOptions) {
    BaseClass.apply(this);

    var self = this;

    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "hashCode", {
        value: markerOptions.hashCode,
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: id,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "Marker",
        writable: false
    });

    var ignores = ["hashCode", "id", "hashCode", "type"];
    for (var key in markerOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, markerOptions[key]);
        }
    }
};

utils.extend(Marker, BaseClass);

Marker.prototype.isVisible = function() {
    return this.get('visible');
};


Marker.prototype.getPosition = function(callback) {
    var self = this;
    cordova.exec(function(latlng) {
        if (typeof callback === "function") {
            callback.call(self, new LatLng(latlng.lat, latlng.lng));
        }
    }, self.errorHandler, PLUGIN_NAME, 'getPosition', [this.getId()]);
};
Marker.prototype.getId = function() {
    return this.id;
};
Marker.prototype.getMap = function() {
    return this.map;
};
Marker.prototype.getHashCode = function() {
    return this.hashCode;
};

Marker.prototype.setAnimation = function(animation, callback) {
    var self = this;

    animation = animation || null;
    if (!animation) {
        return;
    }
    this.set("animation", animation);

    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, this.errorHandler, PLUGIN_NAME, 'setAnimation', [this.getId(), self.deleteFromObject(animation,'function')]);
};

Marker.prototype.remove = function(callback) {
    var self = this;
    self.set("keepWatching", false);
    delete MARKERS[this.id];
    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
};
Marker.prototype.setDisableAutoPan = function(disableAutoPan) {
    disableAutoPan = common.parseBoolean(disableAutoPan);
    this.set('disableAutoPan', disableAutoPan);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setDisableAutoPan', [this.getId(), disableAutoPan]);
};
Marker.prototype.getParams = function() {
    return this.get('params');
};
Marker.prototype.setOpacity = function(opacity) {
    if (!opacity && opacity !== 0) {
        console.log('opacity value must be int or double');
        return false;
    }
    this.set('opacity', opacity);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setOpacity', [this.getId(), opacity]);
};
Marker.prototype.setZIndex = function(zIndex) {
    if (typeof zIndex === 'undefined') {
        return false;
    }
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setZIndex', [this.getId(), zIndex]);
};
Marker.prototype.getOpacity = function() {
    return this.get('opacity');
};
Marker.prototype.setIconAnchor = function(anchorX, anchorY) {
    this.set('anchor', [anchorX, anchorY]);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setIconAnchor', [this.getId(), anchorX, anchorY]);
};
Marker.prototype.setInfoWindowAnchor = function(anchorX, anchorY) {
    this.set('anchor', [anchorX, anchorY]);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setInfoWindowAnchor', [this.getId(), anchorX, anchorY]);
};
Marker.prototype.setDraggable = function(draggable) {
    draggable = common.parseBoolean(draggable);
    this.set('draggable', draggable);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setDraggable', [this.getId(), draggable]);
};
Marker.prototype.isDraggable = function() {
    return this.get('draggable');
};
Marker.prototype.setFlat = function(flat) {
    flat = common.parseBoolean(flat);
    this.set('flat', flat);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setFlat', [this.getId(), flat]);
};
Marker.prototype.setIcon = function(url) {
    if (url && common.isHTMLColorString(url)) {
        url = common.HTMLColor2RGBA(url);
    }
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setIcon', [this.getId(), url]);
};
Marker.prototype.setTitle = function(title) {
    if (!title) {
        console.log('missing value for title');
        return false;
    }
    this.set('title', String(title));
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setTitle', [this.getId(), title]);
};
Marker.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setVisible', [this.getId(), visible]);
};
Marker.prototype.getTitle = function() {
    return this.get('title');
};
Marker.prototype.setSnippet = function(snippet) {
    this.set('snippet', snippet);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setSnippet', [this.getId(), snippet]);
};
Marker.prototype.getSnippet = function() {
    return this.get('snippet');
};
Marker.prototype.setRotation = function(rotation) {
    if (!rotation) {
        console.log('missing value for rotation');
        return false;
    }
    this.set('rotation', rotation);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setRotation', [this.getId(), rotation]);
};
Marker.prototype.getRotation = function() {
    return this.get('rotation');
};
Marker.prototype.showInfoWindow = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'showInfoWindow', [this.getId()]);
};
Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'hideInfoWindow', [this.getId()]);
};
Marker.prototype.isInfoWindowShown = function(callback) {
    var self = this;
    cordova.exec(function(isVisible) {
        isVisible = common.parseBoolean(isVisible);
        if (typeof callback === "function") {
            callback.call(self, isVisible);
        }
    }, self.errorHandler, PLUGIN_NAME, 'isInfoWindowShown', [this.getId()]);
};
Marker.prototype.isVisible = function() {
    return this.get("visible");
};

Marker.prototype.setPosition = function(position) {
    if (!position) {
        console.log('missing value for position');
        return false;
    }
    this.set('position', position);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setPosition', [this.getId(), position.lat, position.lng]);
};

module.exports = Marker;
