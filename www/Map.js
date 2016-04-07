var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    LatLng = require('./LatLng'),
    MapTypeId = require('./MapTypeId'),
    event = require('./event');

var PLUGIN_NAME = "GoogleMaps";

/**
 * Google Maps model.
 */
var Map = function() {
    BaseClass.apply(this);
    Object.defineProperty(this, "type", {
        value: "Map",
        writable: false
    });
};

utils.extend(Map, BaseClass);


Map.prototype.getMap = function(div, params) {
    var self = this,
        args = [];

    if (!common.isDom(div)) {
        params = div;
        params = params || {};
        params.backgroundColor = params.backgroundColor || '#ffffff';
        params.backgroundColor = common.HTMLColor2RGBA(params.backgroundColor);
        if (params.camera && params.camera.latLng) {
          params.camera.target = params.camera.latLng;
          delete params.camera.latLng;
        }
        args.push(params);
    } else {

        var currentDiv = self.get("div");
        if (currentDiv !== div && currentDiv) {
            var children = common.getAllChildren(currentDiv);
            for (var i = 0; i < children.length; i++) {
                element = children[i];
                elemId = element.getAttribute("__pluginDomId");
                element.removeAttribute("__pluginDomId");
            }
            currentDiv.removeEventListener("DOMNodeRemoved", _remove_child);

            while (currentDiv) {
                if (currentDiv.style) {
                    currentDiv.style.backgroundColor = '';
                }
                if (currentDiv.classList) {
                    currentDiv.classList.remove('_gmaps_cdv_');
                } else if (currentDiv.className) {
                    currentDiv.className = currentDiv.className.replace(/_gmaps_cdv_/g, "");
                    currentDiv.className = currentDiv.className.replace(/\s+/g, " ");
                }
                currentDiv = currentDiv.parentNode;
            }
            self.set("div", null);
            self.set("keepWatching", false);
        }


        var children = common.getAllChildren(div);
        params = params || {};
        params.backgroundColor = params.backgroundColor || '#ffffff';
        params.backgroundColor = common.HTMLColor2RGBA(params.backgroundColor);
        if (params.camera && params.camera.latLng) {
          params.camera.target = params.camera.latLng;
          delete params.camera.latLng;
        }
        args.push(params);

        self.set("div", div);
        args.push(common.getDivRect(div));
        var elements = [];
        var elemId, clickable;

        for (var i = 0; i < children.length; i++) {
            element = children[i];
            elemId = element.getAttribute("__pluginDomId");
            if (!elemId) {
                elemId = "pgm" + Math.floor(Math.random() * Date.now()) + i;
                element.setAttribute("__pluginDomId", elemId);
            }
            elements.push({
                id: elemId,
                size: common.getDivRect(element)
            });
            i++;
        }
        args.push(elements);

        div.addEventListener("DOMNodeRemoved", _remove_child);
        div.addEventListener("DOMNodeInserted", _append_child);

        self.set("keepWatching", true);
        var className;
        while (div.parentNode) {
            div.style.backgroundColor = 'rgba(0,0,0,0)';
            className = div.className;

            // prevent multiple readding the class
            if (div.classList && !div.classList.contains('_gmaps_cdv_')) {
                div.classList.add('_gmaps_cdv_');
            } else if (div.className && !div.className.indexOf('_gmaps_cdv_') == -1) {
                div.className = div.className + ' _gmaps_cdv_';
            }

            div = div.parentNode;
        }
    }
    cordova.exec(function() {
        setTimeout(function() {
            self.refreshLayout();
            self.trigger(event.MAP_READY, self);
        }, 100);
    }, self.errorHandler, PLUGIN_NAME, 'getMap', self.deleteFromObject(args,'function'));
    return self;
};


Map.prototype.getLicenseInfo = function(callback) {
    var self = this;
    cordova.exec(function(txt) {
        callback.call(self, txt);
    }, self.errorHandler, PLUGIN_NAME, 'getLicenseInfo', []);
};


/**
 * @desc get watchDogTimer value for map positioning changes
 */
Map.prototype.getWatchDogTimer = function() {
    var self = this;
    time = self.get('watchDogTimer') || 100;
    return time;
};

/**
 * @desc Set watchDogTimer for map positioning changes
 */
Map.prototype.setWatchDogTimer = function(time) {
    var self = this;
    time = time || 100;
    self.set('watchDogTimer', time);

    if (time < 50) {
        //console.log('Warning: watchdog values under 50ms will drain battery a lot. Just use for short operation times.');
    }

};


Map.prototype.setOptions = function(options) {
    options = options || {};
    if (options.hasOwnProperty('backgroundColor')) {
        options.backgroundColor = common.HTMLColor2RGBA(options.backgroundColor);
    }
    if (options.camera && options.camera.latLng) {
      options.camera.target = options.camera.latLng;
      delete options.camera.latLng;
    }
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setOptions', [this.deleteFromObject(options,'function')]);
};

Map.prototype.setCenter = function(latLng) {
    this.set('center', latLng);
    cordova.exec(null, this.errorHandler,
        PLUGIN_NAME, 'setCenter', [latLng.lat, latLng.lng]);
};

Map.prototype.setZoom = function(zoom) {
    this.set('zoom', zoom);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setZoom', [zoom]);
};
Map.prototype.panBy = function(x, y) {
    x = parseInt(x, 10);
    y = parseInt(y, 10);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'panBy', [x, y]);
};

/**
 * @desc Change the map type
 * @param {String} mapTypeId   Specifies the one of the follow strings:
 *                               MAP_TYPE_HYBRID
 *                               MAP_TYPE_SATELLITE
 *                               MAP_TYPE_TERRAIN
 *                               MAP_TYPE_NORMAL
 *                               MAP_TYPE_NONE
 */
Map.prototype.setMapTypeId = function(mapTypeId) {
    if (mapTypeId !== MapTypeId[mapTypeId.replace("MAP_TYPE_", '')]) {
        return this.errorHandler("Invalid MapTypeId was specified.");
    }
    this.set('mapTypeId', mapTypeId);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setMapTypeId', [mapTypeId]);
};

/**
 * @desc Change the map view angle
 * @param {Number} tilt  The angle
 */
Map.prototype.setTilt = function(tilt) {
    this.set('tilt', tilt);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setTilt', [tilt]);
};


/**
 * @desc   Move the map camera with animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
Map.prototype.animateCamera = function(cameraPosition, callback) {
    var self = this;
    if (cameraPosition.target && cameraPosition.target.type === "LatLngBounds") {
        cameraPosition.target = [cameraPosition.target.southwest, cameraPosition.target.northeast];
    }

    if (!cameraPosition.hasOwnProperty('zoom')) {
        self.getZoom(function(zoom) {
            cameraPosition.zoom = zoom;
        });
    }

    if (!cameraPosition.hasOwnProperty('tilt')) {
        self.getTilt(function(tilt) {
            cameraPosition.tilt = tilt;
        });
    }

    if (!cameraPosition.hasOwnProperty('bearing')) {
        self.getBearing(function(bearing) {
            cameraPosition.bearing = bearing;
        });
    }

    var self = this;
    setTimeout(function() {
        cordova.exec(function() {
            if (typeof callback === "function") {
                callback.call(self);
            }
        }, self.errorHandler, PLUGIN_NAME, 'animateCamera', [self.deleteFromObject(cameraPosition,'function')]);
    }.bind(self), 10);


};
/**
 * @desc   Move the map camera without animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
Map.prototype.moveCamera = function(cameraPosition, callback) {
    if (cameraPosition.target &&
        cameraPosition.target.type === "LatLngBounds") {
        cameraPosition.target = [cameraPosition.target.southwest, cameraPosition.target.northeast];
    }
    var self = this;

    if (!cameraPosition.hasOwnProperty('zoom')) {
        self.getZoom(function(zoom) {
            cameraPosition.zoom = zoom;
        });
    }

    if (!cameraPosition.hasOwnProperty('tilt')) {
        self.getTilt(function(tilt) {
            cameraPosition.tilt = tilt;
        });
    }

    if (!cameraPosition.hasOwnProperty('bearing')) {
        self.getBearing(function(bearing) {
            cameraPosition.bearing = bearing;
        });
    }

    setTimeout(function() {
        cordova.exec(function() {
            if (typeof callback === "function") {
                callback.call(self);
            }
        }, self.errorHandler, PLUGIN_NAME, 'moveCamera', [self.deleteFromObject(cameraPosition,'function')]);
    }.bind(self), 10);

};

Map.prototype.setMyLocationEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setMyLocationEnabled', [enabled]);
};
Map.prototype.setIndoorEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setIndoorEnabled', [enabled]);
};
Map.prototype.setTrafficEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'setTrafficEnabled', [enabled]);
};
Map.prototype.setCompassEnabled = function(enabled) {
    var self = this;
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setCompassEnabled', [enabled]);
};
Map.prototype.getMyLocation = function(params, success_callback, error_callback) {
    var args = [params || {}, success_callback || null, error_callback];
    if (typeof args[0] === "function") {
        args.unshift({});
    }
    params = args[0];
    success_callback = args[1];
    error_callback = args[2];

    params.enableHighAccuracy = params.enableHighAccuracy  === true;
    var self = this;
    var successHandler = function(location) {
        if (typeof success_callback === "function") {
            location.latLng = new LatLng(location.latLng.lat, location.latLng.lng);
            success_callback.call(self, location);
        }
    };
    var errorHandler = function(result) {
        if (typeof error_callback === "function") {
            error_callback.call(self, result);
        }
    };
    cordova.exec(successHandler, errorHandler, PLUGIN_NAME, 'getMyLocation', [self.deleteFromObject(params,'function')]);
};
Map.prototype.getFocusedBuilding = function(callback) {
    var self = this;
    cordova.exec(callback, this.errorHandler, PLUGIN_NAME, 'getFocusedBuilding', []);
};
Map.prototype.setVisible = function(isVisible) {
    var self = this;
    isVisible = common.parseBoolean(isVisible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setVisible', [isVisible]);
};
Map.prototype.setClickable = function(isClickable) {
    var self = this;
    isClickable = common.parseBoolean(isClickable);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'pluginLayer_setClickable', [isClickable]);
};

Map.prototype.setBackgroundColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'pluginLayer_setBackGroundColor', [common.HTMLColor2RGBA(color)]);
};


Map.prototype.setDebuggable = function(debug) {
    var self = this;
    debug = common.parseBoolean(debug);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'pluginLayer_setDebuggable', [debug]);
};

/**
 * Sets the preference for whether all gestures should be enabled or disabled.
 */
Map.prototype.setAllGesturesEnabled = function(enabled) {
    var self = this;
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setAllGesturesEnabled', [enabled]);
};

/**
 * Return the current position of the camera
 * @return {CameraPosition}
 */
Map.prototype.getCameraPosition = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            camera.target = new LatLng(camera.target.lat, camera.target.lng);
            callback.call(self, camera);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};


Map.prototype.getZoom = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.zoom);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};

Map.prototype.getTilt = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.tilt);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};

Map.prototype.getBearing = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.bearing);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};



/**
 * Remove the map completely.
 */
Map.prototype.remove = function(callback) {
    var self = this;
    var div = this.get('div');
    if (div) {
        while (div) {
            if (div.style) {
                div.style.backgroundColor = '';
            }
            if (div.classList) {
                div.classList.remove('_gmaps_cdv_');
            } else if (div.className) {
                div.className = div.className.replace(/_gmaps_cdv_/g, "");
                div.className = div.className.replace(/\s+/g, " ");
            }
            div = div.parentNode;
        }
    }
    this.set('div', undefined);
    self.set("keepWatching", false);
    this.clear();
    this.empty();
    this.off();
    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, self.errorHandler, PLUGIN_NAME, 'remove', []);
};


Map.prototype.isAvailable = function(callback) {
    var self = this;

    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self, true);
        }
    }, function(message) {
        if (typeof callback === "function") {
            callback.call(self, false, message);
        }
    }, PLUGIN_NAME, 'isAvailable', ['']);
};

Map.prototype.toDataURL = function(params, callback) {
    var args = [params || {}, callback];
    if (typeof args[0] === "function") {
        args.unshift({});
    }

    params = args[0];
    callback = args[1];

    params.uncompress = params.uncompress === true;
    var self = this;
    cordova.exec(function(image) {
        if (typeof callback === "function") {
            callback.call(self, image);
        }
    }, self.errorHandler, PLUGIN_NAME, 'toDataURL', [self.deleteFromObject(params,'function')]);
};

var _append_child = function(event) {
    event = event || window.event;
    event = event || {};
    var target = event.srcElement;
    if (!target || "nodeType" in target == false) {
        return;
    }
    if (target.nodeType != 1) {
        return;
    }
    var size = common.getDivRect(target);
    var elemId = "pgm" + Math.floor(Math.random() * Date.now());
    target.setAttribute("__pluginDomId", elemId);

    cordova.exec(null, null, PLUGIN_NAME, 'pluginLayer_pushHtmlElement', [elemId, size]);
};

var _remove_child = function(event) {
    event = event || window.event;
    event = event || {};
    var target = event.srcElement;
    if (!target || "nodeType" in target == false) {
        return;
    }
    if (target.nodeType != 1) {
        return;
    }
    var elemId = target.getAttribute("__pluginDomId");
    if (!elemId) {
        return;
    }
    target.removeAttribute("__pluginDomId");
    cordova.exec(null, null, PLUGIN_NAME, 'pluginLayer_removeHtmlElement', [elemId]);
};

/**
 * Show the map into the specified div.
 */
Map.prototype.setDiv = function(div) {
    var self = this,
        args = [],
        element;

    var currentDiv = self.get("div");
    if (common.isDom(div) === false || currentDiv !== div) {
        if (currentDiv) {
            var children = common.getAllChildren(currentDiv);
            for (var i = 0; i < children.length; i++) {
                element = children[i];
                elemId = element.getAttribute("__pluginDomId");
                element.removeAttribute("__pluginDomId");
            }
            currentDiv.removeEventListener("DOMNodeRemoved", _remove_child);

            while (currentDiv) {
                if (currentDiv.style) {
                    currentDiv.style.backgroundColor = '';
                }
                if (currentDiv.classList) {
                    currentDiv.classList.remove('_gmaps_cdv_');
                } else if (currentDiv.className) {
                    currentDiv.className = currentDiv.className.replace(/_gmaps_cdv_/g, "");
                    currentDiv.className = currentDiv.className.replace(/\s+/g, " ");
                }
                currentDiv = currentDiv.parentNode;
            }
        }
        self.set("div", null);
        self.set("keepWatching", false);
    }

    if (common.isDom(div)) {
        var children = common.getAllChildren(div);;
        self.set("div", div);
        args.push(common.getDivRect(div));
        var elements = [];
        var elemId;
        var clickable;

        for (var i = 0; i < children.length; i++) {
            element = children[i];
            if (element.nodeType != 1) {
                continue;
            }
            clickable = element.getAttribute("data-clickable");
            if (clickable && common.parseBoolean(clickable) == false) {
                continue;
            }
            elemId = element.getAttribute("__pluginDomId");
            if (!elemId) {
                elemId = "pgm" + Math.floor(Math.random() * Date.now()) + i;
                element.setAttribute("__pluginDomId", elemId);
            }
            elements.push({
                id: elemId,
                size: common.getDivRect(element)
            });
        }
        args.push(elements);

        div.addEventListener("DOMNodeRemoved", _remove_child);
        div.addEventListener("DOMNodeInserted", _append_child);

        var className;
        while (div.parentNode) {
            div.style.backgroundColor = 'rgba(0,0,0,0)';
            div.style.backgroundImage = '';
            className = div.className;

            // prevent multiple reading the class
            if (div.classList && !div.classList.contains('_gmaps_cdv_')) {
                div.classList.add('_gmaps_cdv_');
            } else if (div.className && !div.className.indexOf('_gmaps_cdv_') == -1) {
                div.className = div.className + ' _gmaps_cdv_';
            }

            div = div.parentNode;
        }
        setTimeout(function() {
            self.refreshLayout();
            self.set("keepWatching", true);
        }, 1000);
    }
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setDiv', self.deleteFromObject(args,'function'));
};

/**
 * Return the visible region of the map.
 * Thanks @fschmidt
 */
Map.prototype.getVisibleRegion = function(callback) {
    var self = this;

    cordova.exec(function(result) {
        if (typeof callback === "function") {
            var latLngBounds = new LatLngBounds(result.latLngArray);
            latLngBounds.northeast = new LatLng(result.northeast.lat, result.northeast.lng);
            latLngBounds.southwest = new LatLng(result.southwest.lat, result.southwest.lng);
            callback.call(self, latLngBounds);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getVisibleRegion']);
};

/**
 * Maps an Earth coordinate to a point coordinate in the map's view.
 */
Map.prototype.fromLatLngToPoint = function(latLng, callback) {
    var self = this;
    if ("lat" in latLng && "lng" in latLng) {
        cordova.exec(function(result) {
            if (typeof callback === "function") {
                callback.call(self, result);
            }
        }, self.errorHandler, PLUGIN_NAME, 'fromLatLngToPoint', [latLng.lat, latLng.lng]);
    } else {
        if (typeof callback === "function") {
            callback.call(self, [undefined, undefined]);
        }
    }

};
/**
 * Maps a point coordinate in the map's view to an Earth coordinate.
 */
Map.prototype.fromPointToLatLng = function(pixel, callback) {
    var self = this;
    if (pixel.length == 2 && utils.isArray(pixel)) {
        cordova.exec(function(result) {
            if (typeof callback === "function") {
                var latLng = new LatLng(result[0] || 0, result[1] || 0);
                callback.call(self, result);
            }
        }, self.errorHandler, PLUGIN_NAME, 'fromPointToLatLng', [pixel[0], pixel[1]]);
    } else {
        if (typeof callback === "function") {
            callback.call(self, [undefined, undefined]);
        }
    }

};

Map.prototype.setPadding = function(p1, p2, p3, p4) {
    if (arguments.length === 0 || arguments.length > 4) {
        return;
    }
    var padding = {};
    padding.top = parseInt(p1, 10);
    switch (arguments.length) {
        case 4:
            // top right bottom left
            padding.right = parseInt(p2, 10);
            padding.bottom = parseInt(p3, 10);
            padding.left = parseInt(p4, 10);
            break;

        case 3:
            // top right&left bottom
            padding.right = parseInt(p2, 10);
            padding.left = padding.right;
            padding.bottom = parseInt(p3, 10);
            break;

        case 2:
            // top & bottom right&left
            padding.bottom = parseInt(p1, 10);
            padding.right = parseInt(p2, 10);
            padding.left = padding.right;
            break;

        case 1:
            // top & bottom right & left
            padding.bottom = padding.top;
            padding.right = padding.top;
            padding.left = padding.top;
            break;
    }
    cordova.exec(function(result) {
        if (typeof callback === "function") {
            var latLng = new LatLng(result[0] || 0, result[1] || 0);
            callback.call(self, result);
        }
    }, self.errorHandler, PLUGIN_NAME, 'setPadding', [padding]);
};


module.exports = Map;
