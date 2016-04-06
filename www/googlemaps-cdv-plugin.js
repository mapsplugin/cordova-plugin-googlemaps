/* global cordova, plugin, CSSPrimitiveValue */
var PLUGIN_NAME = 'GoogleMaps';
var MARKERS = {};
var KML_LAYERS = {};
var OVERLAYS = {};

var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    BaseClass = require('./BaseClass'),
    common = require('./Common'),
    Marker = require('./Marker'),
    Circle = require('./Circle'),
    Polyline = require('./Polyline'),
    Polygon = require('./Polygon');

/**
 * Google Maps model.
 */
var App = function() {
    BaseClass.apply(this);
    Object.defineProperty(this, "type", {
        value: "Map",
        writable: false
    });
};
App.prototype = new BaseClass();

/*
 * Callback from Native
 */

App.prototype._onMarkerEvent = function(eventName, hashCode) {
    var marker = MARKERS[hashCode] || null;
    if (marker) {
        marker.trigger(eventName, marker);
    }
};

App.prototype._onOverlayEvent = function(eventName, hashCode) {
    var overlay = OVERLAYS[hashCode] || null;
    if (overlay) {
        var args = [eventName, overlay];
        for (var i = 2; i < arguments.length; i++) {
            args.push(arguments[i]);
        }
        overlay.trigger.apply(this, args);
    }
};

/*
 * Callback from Native
 */
App.prototype._onKmlEvent = function(eventName, objectType, kmlLayerId, result, options) {
    var kmlLayer = KML_LAYERS[kmlLayerId] || null;
    if (kmlLayer) {
        var self = this;
        var args = [eventName];
        if (eventName === "add") {
            var overlay = null;

            switch ((objectType + "").toLowerCase()) {
                case "marker":
                    overlay = new Marker(self, result.id, options);
                    MARKERS[result.id] = overlay;
                    args.push({
                        "type": "Marker",
                        "object": overlay
                    });
                    overlay.on(plugin.google.maps.event.MARKER_CLICK, function() {
                        kmlLayer.trigger(plugin.google.maps.event.OVERLAY_CLICK, overlay, overlay.getPosition());
                    });
                    break;

                case "polygon":
                    overlay = new Polygon(self, result.id, options);
                    args.push({
                        "type": "Polygon",
                        "object": overlay
                    });

                    overlay.on(plugin.google.maps.event.OVERLAY_CLICK, function(latLng) {
                        kmlLayer.trigger(plugin.google.maps.event.OVERLAY_CLICK, overlay, latLng);
                    });
                    break;

                case "polyline":
                    overlay = new Polyline(self, result.id, options);
                    args.push({
                        "type": "Polyline",
                        "object": overlay
                    });
                    overlay.on(plugin.google.maps.event.OVERLAY_CLICK, function(latLng) {
                        kmlLayer.trigger(plugin.google.maps.event.OVERLAY_CLICK, overlay, latLng);
                    });
                    break;
            }
            if (overlay) {
                OVERLAYS[result.id] = overlay;
                overlay.hashCode = result.hashCode;
                kmlLayer._overlays.push(overlay);
                kmlLayer.on("_REMOVE", function() {
                    var idx = kmlLayer._overlays.indexOf(overlay);
                    if (idx > -1) {
                        kmlLayer._overlays.splice(idx, 1);
                    }
                    overlay.remove();
                    overlay.off();
                });
            }
        } else {
            for (var i = 2; i < arguments.length; i++) {
                args.push(arguments[i]);
            }
        }
        //kmlLayer.trigger.apply(kmlLayer, args);
    }
};

/**
 * Callback from Native
 */
App.prototype._onMapEvent = function(eventName) {
    var args = [eventName];
    for (var i = 1; i < arguments.length; i++) {
        if (typeof(arguments[i]) === "string") {
            if (["true", "false"].indexOf(arguments[i].toLowerCase()) > -1) {
                arguments[i] = common.parseBoolean(arguments[i]);
            }
        }
        args.push(arguments[i]);
    }
    args.push(this);
    this.trigger.apply(this, args);
};
/**
 * Callback from Native
 */
App.prototype._onMyLocationChange = function(params) {
    var location = new Location(params);
    this.trigger('my_location_change', location, this);
};
/**
 * Callback from Native
 */
App.prototype._onCameraEvent = function(eventName, params) {
    var cameraPosition = new CameraPosition(params);
    this.trigger(eventName, cameraPosition, this);
};


App.prototype.getMap = function(div, params) {
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
            var children = getAllChildren(currentDiv);
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


        var children = getAllChildren(div);
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
            self.trigger(plugin.google.maps.event.MAP_READY, self);
        }, 100);
    }, self.errorHandler, PLUGIN_NAME, 'getMap', self.deleteFromObject(args,'function'));
    return self;
};


App.prototype.getLicenseInfo = function(callback) {
    var self = this;
    cordova.exec(function(txt) {
        callback.call(self, txt);
    }, self.errorHandler, PLUGIN_NAME, 'getLicenseInfo', []);
};


/**
 * @desc get watchDogTimer value for map positioning changes
 */
App.prototype.getWatchDogTimer = function() {
    var self = this;
    time = self.get('watchDogTimer') || 100;
    return time;
};

/**
 * @desc Set watchDogTimer for map positioning changes
 */
App.prototype.setWatchDogTimer = function(time) {
    var self = this;
    time = time || 100;
    self.set('watchDogTimer', time);

    if (time < 50) {
        //console.log('Warning: watchdog values under 50ms will drain battery a lot. Just use for short operation times.');
    }

};

function onBackbutton() {
    _mapInstance.closeDialog();
}

/**
 * @desc Open the map dialog
 */
App.prototype.showDialog = function() {
    document.addEventListener("backbutton", onBackbutton, false);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'showDialog', []);
};

/**
 * @desc Close the map dialog
 */
App.prototype.closeDialog = function() {
    document.removeEventListener("backbutton", onBackbutton, false);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'closeDialog', []);
};

App.prototype.setOptions = function(options) {
    options = options || {};
    if (options.hasOwnProperty('backgroundColor')) {
        options.backgroundColor = common.HTMLColor2RGBA(options.backgroundColor);
    }
    if (options.camera && options.camera.latLng) {
      options.camera.target = options.camera.latLng;
      delete options.camera.latLng;
    }
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setOptions', this.deleteFromObject(options,'function')]);
};

App.prototype.setCenter = function(latLng) {
    this.set('center', latLng);
    cordova.exec(null, this.errorHandler,
        PLUGIN_NAME, 'exec', ['Map.setCenter', latLng.lat, latLng.lng]);
};

App.prototype.setZoom = function(zoom) {
    this.set('zoom', zoom);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setZoom', zoom]);
};
App.prototype.panBy = function(x, y) {
    x = parseInt(x, 10);
    y = parseInt(y, 10);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.panBy', x, y]);
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
App.prototype.setMapTypeId = function(mapTypeId) {
    if (mapTypeId !== plugin.google.maps.MapTypeId[mapTypeId.replace("MAP_TYPE_", '')]) {
        return this.errorHandler("Invalid MapTypeId was specified.");
    }
    this.set('mapTypeId', mapTypeId);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setMapTypeId', mapTypeId]);
};

/**
 * @desc Change the map view angle
 * @param {Number} tilt  The angle
 */
App.prototype.setTilt = function(tilt) {
    this.set('tilt', tilt);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setTilt', tilt]);
};


/**
 * @desc   Move the map camera with animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
App.prototype.animateCamera = function(cameraPosition, callback) {
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
        }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.animateCamera', self.deleteFromObject(cameraPosition,'function')]);
    }.bind(self), 10);


};
/**
 * @desc   Move the map camera without animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
App.prototype.moveCamera = function(cameraPosition, callback) {
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
        }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.moveCamera', self.deleteFromObject(cameraPosition,'function')]);
    }.bind(self), 10);

};

App.prototype.setMyLocationEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setMyLocationEnabled', enabled]);
};
App.prototype.setIndoorEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setIndoorEnabled', enabled]);
};
App.prototype.setTrafficEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setTrafficEnabled', enabled]);
};
App.prototype.setCompassEnabled = function(enabled) {
    var self = this;
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setCompassEnabled', enabled]);
};
App.prototype.getMyLocation = function(params, success_callback, error_callback) {
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
App.prototype.getFocusedBuilding = function(callback) {
    var self = this;
    cordova.exec(callback, this.errorHandler, PLUGIN_NAME, 'getFocusedBuilding', []);
};
App.prototype.setVisible = function(isVisible) {
    var self = this;
    isVisible = common.parseBoolean(isVisible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setVisible', [isVisible]);
};
App.prototype.setClickable = function(isClickable) {
    var self = this;
    isClickable = common.parseBoolean(isClickable);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'pluginLayer_setClickable', [isClickable]);
};

App.prototype.setBackgroundColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'pluginLayer_setBackGroundColor', [common.HTMLColor2RGBA(color)]);
};


App.prototype.setDebuggable = function(debug) {
    var self = this;
    debug = common.parseBoolean(debug);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'pluginLayer_setDebuggable', [debug]);
};

/**
 * Sets the preference for whether all gestures should be enabled or disabled.
 */
App.prototype.setAllGesturesEnabled = function(enabled) {
    var self = this;
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setAllGesturesEnabled', enabled]);
};

/**
 * Return the current position of the camera
 * @return {CameraPosition}
 */
App.prototype.getCameraPosition = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            camera.target = new LatLng(camera.target.lat, camera.target.lng);
            callback.call(self, camera);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};


App.prototype.getZoom = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.zoom);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};

App.prototype.getTilt = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.tilt);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};

App.prototype.getBearing = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.bearing);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};

/**
 * Clears all markup that has been added to the map,
 * including markers, polylines and ground overlays.
 */
App.prototype.clear = function(callback) {
    var self = this;

    var clearObj = function (obj) {
        var ids = Object.keys(obj);
        var id;
        for (var i = 0; i < ids.length; i++) {
            id = ids[i];
            obj[id].off();
            delete obj[id];
        }
        obj = {};
    };

    clearObj(OVERLAYS);
    clearObj(MARKERS);
    clearObj(KML_LAYERS);

    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, self.errorHandler, PLUGIN_NAME, 'clear', []);
};

/**
 * Remove the map completely.
 */
App.prototype.remove = function(callback) {
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

App.prototype.refreshLayout = function() {
    onMapResize(undefined, false);
};

App.prototype.isAvailable = function(callback) {
    var self = this;

    /*
    var tmpmap = plugin.google.maps.Map.getMap(document.createElement("div"), {});
    tmpmap.remove();
    tmpmap = null;
    */

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

App.prototype.toDataURL = function(params, callback) {
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
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.toDataURL', self.deleteFromObject(params,'function')]);
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
App.prototype.setDiv = function(div) {
    var self = this,
        args = [],
        element;

    var currentDiv = self.get("div");
    if (common.isDom(div) === false || currentDiv !== div) {
        if (currentDiv) {
            var children = getAllChildren(currentDiv);
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
        var children = getAllChildren(div);;
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
App.prototype.getVisibleRegion = function(callback) {
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
App.prototype.fromLatLngToPoint = function(latLng, callback) {
    var self = this;
    if ("lat" in latLng && "lng" in latLng) {
        cordova.exec(function(result) {
            if (typeof callback === "function") {
                callback.call(self, result);
            }
        }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.fromLatLngToPoint', latLng.lat, latLng.lng]);
    } else {
        if (typeof callback === "function") {
            callback.call(self, [undefined, undefined]);
        }
    }

};
/**
 * Maps a point coordinate in the map's view to an Earth coordinate.
 */
App.prototype.fromPointToLatLng = function(pixel, callback) {
    var self = this;
    if (pixel.length == 2 && utils.isArray(pixel)) {
        cordova.exec(function(result) {
            if (typeof callback === "function") {
                var latLng = new LatLng(result[0] || 0, result[1] || 0);
                callback.call(self, result);
            }
        }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.fromPointToLatLng', pixel[0], pixel[1]]);
    } else {
        if (typeof callback === "function") {
            callback.call(self, [undefined, undefined]);
        }
    }

};

App.prototype.setPadding = function(p1, p2, p3, p4) {
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
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setPadding', padding]);
};

//-------------
// Marker
//-------------
App.prototype.addMarker = function(markerOptions, callback) {
    var self = this;
    markerOptions.animation = markerOptions.animation || undefined;
    markerOptions.position = markerOptions.position || {};
    markerOptions.position.lat = markerOptions.position.lat || 0.0;
    markerOptions.position.lng = markerOptions.position.lng || 0.0;
    markerOptions.anchor = markerOptions.anchor || [0.5, 0.5];
    markerOptions.draggable = markerOptions.draggable === true;
    markerOptions.icon = markerOptions.icon || undefined;
    markerOptions.snippet = markerOptions.snippet || undefined;
    markerOptions.title = markerOptions.title !== undefined ? String(markerOptions.title) : undefined;
    markerOptions.visible = markerOptions.visible === undefined ? true : markerOptions.visible;
    markerOptions.flat = markerOptions.flat  === true;
    markerOptions.rotation = markerOptions.rotation || 0;
    markerOptions.opacity = parseFloat("" + markerOptions.opacity, 10) || 1;
    markerOptions.disableAutoPan = markerOptions.disableAutoPan === undefined ? false : markerOptions.disableAutoPan;
    if ("styles" in markerOptions) {
        markerOptions.styles = typeof markerOptions.styles === "object" ? markerOptions.styles : {};

        if ("color" in markerOptions.styles) {
            markerOptions.styles.color = common.HTMLColor2RGBA(markerOptions.styles.color || "#000000");
        }
    }
    if (markerOptions.icon && common.isHTMLColorString(markerOptions.icon)) {
        markerOptions.icon = common.HTMLColor2RGBA(markerOptions.icon);
    }

    cordova.exec(function(result) {
        markerOptions.hashCode = result.hashCode;
        var marker = new Marker(self, result.id, markerOptions);

        MARKERS[result.id] = marker;
        OVERLAYS[result.id] = marker;

        if (typeof markerOptions.markerClick === "function") {
            marker.on(plugin.google.maps.event.MARKER_CLICK, markerOptions.markerClick);
        }
        if (typeof markerOptions.infoClick === "function") {
            marker.on(plugin.google.maps.event.INFO_CLICK, markerOptions.infoClick);
        }
        if (typeof callback === "function") {
            callback.call(self, marker, self);
        }
    }, self.errorHandler, 'Marker', 'create', [self.deleteFromObject(markerOptions,'function')]);
};


//-------------
// Circle
//-------------
App.prototype.addCircle = function(circleOptions, callback) {
    var self = this;
    circleOptions.center = circleOptions.center || {};
    circleOptions.center.lat = circleOptions.center.lat || 0.0;
    circleOptions.center.lng = circleOptions.center.lng || 0.0;
    circleOptions.strokeColor = common.HTMLColor2RGBA(circleOptions.strokeColor || "#FF0000", 0.75);
    circleOptions.fillColor = common.HTMLColor2RGBA(circleOptions.fillColor || "#000000", 0.75);
    circleOptions.strokeWidth = circleOptions.strokeWidth || 10;
    circleOptions.visible = circleOptions.visible === undefined ? true : circleOptions.visible;
    circleOptions.zIndex = circleOptions.zIndex || 3;
    circleOptions.radius = circleOptions.radius || 1;

    cordova.exec(function(result) {
        var circle = new Circle(self, result.id, circleOptions);
        OVERLAYS[result.id] = circle;
        if (typeof circleOptions.onClick === "function") {
            circle.on(plugin.google.maps.event.OVERLAY_CLICK, circleOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, circle, self);
        }
    }, self.errorHandler, 'Circle', 'create', [self.deleteFromObject(circleOptions,'function')]);
};
//-------------
// Polyline
//-------------
App.prototype.addPolyline = function(polylineOptions, callback) {
    var self = this;
    polylineOptions.points = polylineOptions.points || [];
    polylineOptions.color = common.HTMLColor2RGBA(polylineOptions.color || "#FF000080", 0.75);
    polylineOptions.width = polylineOptions.width || 10;
    polylineOptions.visible = polylineOptions.visible === undefined ? true : polylineOptions.visible;
    polylineOptions.zIndex = polylineOptions.zIndex || 4;
    polylineOptions.geodesic = polylineOptions.geodesic  === true;

    cordova.exec(function(result) {
        var polyline = new Polyline(self, result.id, polylineOptions);
        OVERLAYS[result.id] = polyline;
        /*if (typeof polylineOptions.onClick === "function") {
          polyline.on(plugin.google.maps.event.OVERLAY_CLICK, polylineOptions.onClick);
        }*/
        if (typeof callback === "function") {
            callback.call(self, polyline, self);
        }
    }, self.errorHandler, 'Polyline', 'create', [self.deleteFromObject(polylineOptions,'function')]);
};
//-------------
// Polygon
//-------------
App.prototype.addPolygon = function(polygonOptions, callback) {
    var self = this;
    polygonOptions.points = polygonOptions.points || [];
    polygonOptions.holes = polygonOptions.holes || [];
    if (polygonOptions.holes.length > 0 && !Array.isArray(polygonOptions.holes[0])) {
      polygonOptions.holes = [polygonOptions.holes];
    }
    polygonOptions.holes = polygonOptions.holes.map(function(hole) {
      if (!Array.isArray(hole)) {
        return [];
      }
      return hole.map(function(latLng) {
        return {lat: latLng.lat, lng: latLng.lng};
      });
    });
    polygonOptions.strokeColor = common.HTMLColor2RGBA(polygonOptions.strokeColor || "#FF000080", 0.75);
    if (polygonOptions.fillColor) {
        polygonOptions.fillColor = common.HTMLColor2RGBA(polygonOptions.fillColor, 0.75);
    }
    polygonOptions.strokeWidth = polygonOptions.strokeWidth || 10;
    polygonOptions.visible = polygonOptions.visible === undefined ? true : polygonOptions.visible;
    polygonOptions.zIndex = polygonOptions.zIndex || 2;
    polygonOptions.geodesic = polygonOptions.geodesic  === true;

    cordova.exec(function(result) {
        var polygon = new Polygon(self, result.id, polygonOptions);
        OVERLAYS[result.id] = polygon;
        if (typeof polygonOptions.onClick === "function") {
            polygon.on(plugin.google.maps.event.OVERLAY_CLICK, polygonOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, polygon, self);
        }
    }, self.errorHandler, "Polygon", 'create', [self.deleteFromObject(polygonOptions,'function')]);
};

//-------------
// Tile overlay
//-------------
App.prototype.addTileOverlay = function(tilelayerOptions, callback) {
    var self = this;
    tilelayerOptions = tilelayerOptions || {};
    tilelayerOptions.tileUrlFormat = tilelayerOptions.tileUrlFormat || null;
    if (typeof tilelayerOptions.tileUrlFormat !== "string") {
        throw new Error("tilelayerOptions.tileUrlFormat should set a string.");
    }
    tilelayerOptions.visible = tilelayerOptions.visible === undefined ? true : tilelayerOptions.visible;
    tilelayerOptions.zIndex = tilelayerOptions.zIndex || 0;
    tilelayerOptions.tileSize = tilelayerOptions.tileSize || 256;
    tilelayerOptions.opacity = tilelayerOptions.opacity || 1;

    cordova.exec(function(result) {
        var tileOverlay = new TileOverlay(self, result.id, tilelayerOptions);
        OVERLAYS[result.id] = tileOverlay;
        /*
        if (typeof tilelayerOptions.onClick === "function") {
          tileOverlay.on(plugin.google.maps.event.OVERLAY_CLICK, tilelayerOptions.onClick);
        }
        */
        if (typeof callback === "function") {
            callback.call(self, tileOverlay, self);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.createTileOverlay', self.deleteFromObject(tilelayerOptions,'function')]);
};
//-------------
// Ground overlay
//-------------
App.prototype.addGroundOverlay = function(groundOverlayOptions, callback) {
    var self = this;
    groundOverlayOptions = groundOverlayOptions || {};
    groundOverlayOptions.url = groundOverlayOptions.url || null;
    groundOverlayOptions.visible = groundOverlayOptions.visible === undefined ? true : groundOverlayOptions.visible;
    groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 1;
    groundOverlayOptions.bounds = groundOverlayOptions.bounds || [];

    cordova.exec(function(result) {
        var groundOverlay = new GroundOverlay(self, result.id, groundOverlayOptions);
        OVERLAYS[result.id] = groundOverlay;
        if (typeof groundOverlayOptions.onClick === "function") {
            groundOverlay.on(plugin.google.maps.event.OVERLAY_CLICK, groundOverlayOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, groundOverlay, self);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.createGroundOverlay', self.deleteFromObject(groundOverlayOptions,'function')]);

};

//-------------
// KML Layer
//-------------
App.prototype.addKmlOverlay = function(kmlOverlayOptions, callback) {
    var self = this;
    kmlOverlayOptions = kmlOverlayOptions || {};
    kmlOverlayOptions.url = kmlOverlayOptions.url || null;
    kmlOverlayOptions.preserveViewport = kmlOverlayOptions.preserveViewport  === true;
    kmlOverlayOptions.animation = kmlOverlayOptions.animation === undefined ? true : kmlOverlayOptions.animation;

    var kmlId = "kml" + (Math.random() * 9999999);
    kmlOverlayOptions.kmlId = kmlId;

    var kmlOverlay = new KmlOverlay(self, kmlId, kmlOverlayOptions);
    OVERLAYS[kmlId] = kmlOverlay;
    KML_LAYERS[kmlId] = kmlOverlay;

    cordova.exec(function(kmlId) {
        if (typeof callback === "function") {
            callback.call(self, kmlOverlay, self);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['KmlOverlay.createKmlOverlay', self.deleteFromObject(kmlOverlayOptions,'function')]);

};
//-------------
// Geocoding
//-------------
App.prototype.geocode = function(geocoderRequest, callback) {
    console.log("Map.geocode will be deprecated. Please use Geocoder.geocode instead.");
    Geocoder.geocode(geocoderRequest, callback);
};
/********************************************************************************
 * @name CameraPosition
 * @class This class represents new camera position
 * @property {LatLng} target The location where you want to show
 * @property {Number} [tilt] View angle
 * @property {Number} [zoom] Zoom level
 * @property {Number} [bearing] Map orientation
 * @property {Number} [duration] The duration of animation
 *******************************************************************************/
var CameraPosition = function(params) {
    var self = this;
    self.zoom = params.zoom;
    self.tilt = params.tilt;
    self.target = params.target;
    self.bearing = params.bearing;
    self.hashCode = params.hashCode;
    self.duration = params.duration;
};
/*****************************************************************************
 * Location Class
 *****************************************************************************/
var Location = function(params) {
    var self = this;
    self.latLng = params.latLng || new LatLng(params.lat || 0, params.lng || 0);
    self.elapsedRealtimeNanos = params.elapsedRealtimeNanos;
    self.time = params.time;
    self.accuracy = params.accuracy || null;
    self.bearing = params.bearing || null;
    self.altitude = params.altitude || null;
    self.speed = params.speed || null;
    self.provider = params.provider;
    self.hashCode = params.hashCode;
};

/*******************************************************************************
 * @name LatLng
 * @class This class represents new camera position
 * @param {Number} latitude
 * @param {Number} longitude
 ******************************************************************************/
var LatLng = function(latitude, longitude) {
    var self = this;
    /**
     * @property {Number} latitude
     */
    self.lat = parseFloat(latitude || 0, 10);

    /**
     * @property {Number} longitude
     */
    self.lng = parseFloat(longitude || 0, 10);

    /**
     * Comparison function.
     * @method
     * @return {Boolean}
     */
    self.equals = function(other) {
        other = other || {};
        return other.lat === self.lat &&
            other.lng === self.lng;
    };

    /**
     * @method
     * @return {String} latitude,lontitude
     */
    self.toString = function() {
        return self.lat + "," + self.lng;
    };

    /**
     * @method
     * @param {Number}
     * @return {String} latitude,lontitude
     */
    self.toUrlValue = function(precision) {
        precision = precision || 6;
        return self.lat.toFixed(precision) + "," + self.lng.toFixed(precision);
    };
};

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

TileOverlay.prototype = new BaseClass();

TileOverlay.prototype.getMap = function() {
    return this.map;
};
TileOverlay.prototype.clearTileCache = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.clearTileCache', this.getId()]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setZIndex', this.getId(), zIndex]);
};
TileOverlay.prototype.setFadeIn = function(fadeIn) {
    fadeIn = common.parseBoolean(fadeIn);
    this.set('fadeIn', fadeIn);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setFadeIn', this.getId(), fadeIn]);
};
TileOverlay.prototype.getFadeIn = function() {
    return this.get('fadeIn');
};
TileOverlay.prototype.setVisible = function(visible) {
    visible = common.parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setVisible', this.getId(), visible]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setOpacity', this.getId(), opacity]);
};
TileOverlay.prototype.getVisible = function() {
    return this.get('visible');
};
TileOverlay.prototype.remove = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.remove', this.getId()]);
    this.off();
};

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
    var ignores = ["map", "id", "type"];
    for (var key in groundOverlayOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, groundOverlayOptions[key]);
        }
    }
};

GroundOverlay.prototype = new BaseClass();

GroundOverlay.prototype.getMap = function() {
    return this.map;
};
GroundOverlay.prototype.getId = function() {
    return this.id;
};
GroundOverlay.prototype.remove = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.remove', this.getId()]);
    this.off();
};

GroundOverlay.prototype.setVisible = function(visible) {
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setVisible', this.getId(), visible]);
};

GroundOverlay.prototype.getVisible = function() {
    return this.get('visible');
};

GroundOverlay.prototype.setImage = function(url) {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setImage', this.getId(), url]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setBounds', this.getId(), bounds]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setOpacity', this.getId(), opacity]);
};
GroundOverlay.prototype.setBearing = function(bearing) {
    this.set('bearing', bearing);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setBearing', this.getId(), bearing]);
};

GroundOverlay.prototype.getZIndex = function() {
    return this.get("zIndex");
};
GroundOverlay.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setZIndex', this.getId(), zIndex]);
};
/*****************************************************************************
 * KmlOverlay Class
 *****************************************************************************/
var KmlOverlay = function(map, kmlOverlayId, kmlOverlayOptions) {
    BaseClass.apply(this);

    var self = this;
    self._overlays = [];
    //self.set("visible", kmlOverlayOptions.visible === undefined ? true : kmlOverlayOptions.visible);
    //self.set("zIndex", kmlOverlayOptions.zIndex || 0);
    kmlOverlayOptions.animation = kmlOverlayOptions.animation === undefined ? true : kmlOverlayOptions.animation;
    kmlOverlayOptions.preserveViewport = kmlOverlayOptions.preserveViewport  === true;
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
    var ignores = ["map", "id", "type"];
    for (var key in kmlOverlayOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, kmlOverlayOptions[key]);
        }
    }
};

KmlOverlay.prototype = new BaseClass();

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

    this.trigger("_REMOVE");
    setTimeout(function() {
        delete KML_LAYERS[layerId];
        self.off();
    }, 1000);
};

/*****************************************************************************
 * LatLngBounds Class
 *****************************************************************************/
var LatLngBounds = function() {
    Object.defineProperty(this, "type", {
        value: "LatLngBounds",
        writable: false
    });

    var args = [];
    if (arguments.length === 1 &&
        typeof arguments[0] === "object" &&
        "push" in arguments[0]) {
        args = arguments[0];
    } else {
        args = Array.prototype.slice.call(arguments, 0);
    }
    for (var i = 0; i < args.length; i++) {
        if ("lat" in args[i] && "lng" in args[i]) {
            this.extend(args[i]);
        }
    }
};

LatLngBounds.prototype.northeast = null;
LatLngBounds.prototype.southwest = null;

LatLngBounds.prototype.toString = function() {
    return "[[" + this.southwest.toString() + "],[" + this.northeast.toString() + "]]";
};
LatLngBounds.prototype.toUrlValue = function(precision) {
    return "[[" + this.southwest.toUrlValue(precision) + "],[" + this.northeast.toUrlValue(precision) + "]]";
};

LatLngBounds.prototype.extend = function(latLng) {
    if ("lat" in latLng && "lng" in latLng) {
        if (!this.southwest && !this.northeast) {
            this.southwest = latLng;
            this.northeast = latLng;
        } else {
            var swLat = Math.min(latLng.lat, this.southwest.lat);
            var swLng = Math.min(latLng.lng, this.southwest.lng);
            var neLat = Math.max(latLng.lat, this.northeast.lat);
            var neLng = Math.max(latLng.lng, this.northeast.lng);

            delete this.southwest;
            delete this.northeast;
            this.southwest = new LatLng(swLat, swLng);
            this.northeast = new LatLng(neLat, neLng);
        }
        this[0] = this.southwest;
        this[1] = this.northeast;
    }
};

LatLngBounds.prototype.getCenter = function() {
    var centerLat = (this.southwest.lat + this.northeast.lat) / 2;

    var swLng = this.southwest.lng;
    var neLng = this.northeast.lng;
    var sumLng = swLng + neLng;
    var centerLng = sumLng / 2;

    if ((swLng > 0 && neLng < 0 && sumLng < 180)) {
        centerLng += sumLng > 0 ? -180 : 180;
    }
    return new LatLng(centerLat, centerLng);
};

LatLngBounds.prototype.contains = function(latLng) {
    if (!("lat" in latLng) || !("lng" in latLng)) {
        return false;
    }
    return (latLng.lat >= this.southwest.lat) && (latLng.lat <= this.northeast.lat) &&
        (latLng.lng >= this.southwest.lng) && (latLng.lng <= this.northeast.lng);
};

/*****************************************************************************
 * Private functions
 *****************************************************************************/


function onMapResize(event) {
    var self = window.plugin.google.maps.Map;
    var div = self.get("div");
    if (!div) {
        return;
    }
    if (common.isDom(div) === false) {
        self.set("div", null);
        cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setDiv', []);
    } else {
        var args = [];
        var element, elements = [];
        var children = getAllChildren(div);
        var elemId, clickable;

        args.push(common.getDivRect(div));
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
        //alert(JSON.stringify(common.getDivRect(div), null, 4));
        cordova.exec(null, null, PLUGIN_NAME, 'resizeMap', args);
    }

}
/*****************************************************************************
 * External service
 *****************************************************************************/
var externalService = {};

externalService.launchNavigation = function(params) {
    params = params || {};
    if (!params.from || !params.to) {
        return;
    }
    if (typeof params.from === "object" && "toUrlValue" in params.from) {
        params.from = params.from.toUrlValue();
    }
    if (typeof params.to === "object" && "toUrlValue" in params.to) {
        params.to = params.to.toUrlValue();
    }
    //params.from = params.from.replace(/\s+/g, "%20");
    //params.to = params.to.replace(/\s+/g, "%20");
    cordova.exec(null, null, "External", 'launchNavigation', [params]);
};
/*****************************************************************************
 * Geocoder class
 *****************************************************************************/
var Geocoder = {};

Geocoder.geocode = function(geocoderRequest, callback) {
    geocoderRequest = geocoderRequest || {};

    if ("position" in geocoderRequest) {
        geocoderRequest.position.lat = geocoderRequest.position.lat || 0.0;
        geocoderRequest.position.lng = geocoderRequest.position.lng || 0.0;
    }
    var pluginExec = function() {
        cordova.exec(function(results) {
            if (typeof callback === "function") {
                callback(results);
            }
        }, function(error) {
            if (typeof callback === "function") {
                callback([], error);
            }
        }, "Geocoder", 'geocode', [geocoderRequest]);
    };

    pluginExec();
};

/*****************************************************************************
 * Watch dog timer for child elements
 *****************************************************************************/
var _mapInstance = new App();

window._watchDogTimer = null;
_mapInstance.addEventListener("keepWatching_changed", function(oldValue, newValue) {
    if (newValue !== true) {
        return;
    }
    var prevSize = null;
    var children;
    var prevChildrenCnt = 0;
    var divSize, childCnt = 0;
    if (window._watchDogTimer) {
        clearInterval(window._watchDogTimer);
    }

    function init() {
        window._watchDogTimer = window.setInterval(function() {
            myFunc();
        }, _mapInstance.getWatchDogTimer());
    }

    function myFunc() {
        var div = module.exports.Map.get("div");
        if (div) {
            children = getAllChildren(div);
            childCnt = children.length;
            if (childCnt != prevChildrenCnt) {
                onMapResize();
                prevChildrenCnt = childCnt;
                watchDogTimer = setTimeout(myFunc, 100);
                return;
            }
            prevChildrenCnt = childCnt;
            divSize = common.getDivRect(div);
            if (prevSize) {
                if (divSize.left != prevSize.left ||
                    divSize.top != prevSize.top ||
                    divSize.width != prevSize.width ||
                    divSize.height != prevSize.height) {
                    onMapResize();
                }
            }
            prevSize = divSize;
        }
        div = null;
        divSize = null;
        childCnt = null;
        children = null;
        clearInterval(window._watchDogTimer);
        init();
    }
    init();
});

_mapInstance.addEventListener("keepWatching_changed", function(oldValue, newValue) {
    if (newValue !== false) {
        return;
    }
    if (window._watchDogTimer) {
        clearInterval(window._watchDogTimer);
    }
    window._watchDogTimer = null;
});

/*****************************************************************************
 * geometry Encode / decode points
 * http://jsfiddle.net/8nzg7tta/
 *****************************************************************************/
//decode function
function decodePath(encoded, precision) {
    precision = precision || 5;
    precision = Math.pow(10, -precision);
    var len = encoded.length,
        index = 0,
        lat = 0,
        lng = 0,
        array = [];
    while (index < len) {
        var b, shift = 0,
            result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        var dlat = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lat += dlat;
        shift = 0;
        result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        var dlng = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lng += dlng;
        array.push(new plugin.google.maps.LatLng(lat * precision, lng * precision));
    }
    return array;
}

//encode functions
function encodePath(points) {
    var plat = 0;
    var plng = 0;
    var encoded_points = "";

    for (var i = 0; i < points.length; ++i) {
        encoded_points += encodePoint(plat, plng, points[i].lat, points[i].lng);
        plat = points[i].lat;
        plng = points[i].lng;
    }

    return encoded_points;
}

function encodePoint(plat, plng, lat, lng) {
    var late5 = Math.round(lat * 1e5);
    var plate5 = Math.round(plat * 1e5);

    var lnge5 = Math.round(lng * 1e5);
    var plnge5 = Math.round(plng * 1e5);

    dlng = lnge5 - plnge5;
    dlat = late5 - plate5;

    return encodeSignedNumber(dlat) + encodeSignedNumber(dlng);
}

function encodeSignedNumber(num) {
    var sgn_num = num << 1;

    if (num < 0) {
        sgn_num = ~(sgn_num);
    }

    return (encodeNumber(sgn_num));
}

function encodeNumber(num) {
    var encodeString = "";

    while (num >= 0x20) {
        encodeString += (String.fromCharCode((0x20 | (num & 0x1f)) + 63));
        num >>= 5;
    }

    encodeString += (String.fromCharCode(num + 63));
    return encodeString;
}

/*****************************************************************************
 * Name space
 *****************************************************************************/
module.exports = {
    event: {
        MAP_CLICK: 'click',
        MAP_LONG_CLICK: 'long_click',
        MY_LOCATION_CHANGE: 'my_location_change', // for Android
        MY_LOCATION_BUTTON_CLICK: 'my_location_button_click',
        INDOOR_BUILDING_FOCUSED: 'indoor_building_focused',
        INDOOR_LEVEL_ACTIVATED: 'indoor_level_activated',
        CAMERA_CHANGE: 'camera_change',
        CAMERA_IDLE: 'camera_idle', //for iOS
        MAP_READY: 'map_ready',
        MAP_LOADED: 'map_loaded', //for Android
        MAP_WILL_MOVE: 'will_move', //for iOS
        MAP_CLOSE: 'map_close',
        MARKER_CLICK: 'click',
        OVERLAY_CLICK: 'overlay_click',
        INFO_CLICK: 'info_click',
        MARKER_DRAG: 'drag',
        MARKER_DRAG_START: 'drag_start',
        MARKER_DRAG_END: 'drag_end'
    },
    Animation: {
        BOUNCE: 'BOUNCE',
        DROP: 'DROP'
    },

    //BaseClass: BaseClass,
    Map: _mapInstance,
    LatLng: LatLng,
    LatLngBounds: LatLngBounds,
    Marker: Marker,
    MapTypeId: {
        'NORMAL': 'MAP_TYPE_NORMAL',
        'ROADMAP': 'MAP_TYPE_NORMAL',
        'SATELLITE': 'MAP_TYPE_SATELLITE',
        'HYBRID': 'MAP_TYPE_HYBRID',
        'TERRAIN': 'MAP_TYPE_TERRAIN',
        'NONE': 'MAP_TYPE_NONE'
    },
    external: externalService,
    Geocoder: Geocoder,
    geometry: {
        encoding: {
            decodePath: decodePath,
            encodePath: encodePath
        }
    }
};

cordova.addConstructor(function() {
    if (!window.Cordova) {
        window.Cordova = cordova;
    };
    //window.plugin = window.plugin || {};
    //window.plugin.google = window.plugin.google || {};
    //window.plugin.google.maps = window.plugin.google.maps || module.exports;
});
window.addEventListener("orientationchange", function() {
    setTimeout(onMapResize, 1000);
});


function getAllChildren(root) {
    var list = [];
    var clickable;
    var style, displayCSS, opacityCSS, visibilityCSS;
    var search = function(node) {
        while (node != null) {
            if (node.nodeType == 1) {
                style = window.getComputedStyle(node);
                visibilityCSS = style.getPropertyValue('visibility');
                displayCSS = style.getPropertyValue('display');
                opacityCSS = style.getPropertyValue('opacity');
                if (displayCSS !== "none" && opacityCSS > 0 && visibilityCSS != "hidden") {
                    clickable = node.getAttribute("data-clickable");
                    if (clickable &&
                        clickable.toLowerCase() === "false" &&
                        node.hasChildNodes()) {
                        Array.prototype.push.apply(list, getAllChildren(node));
                    } else {
                        list.push(node);
                    }
                }
            }
            node = node.nextSibling;
        }
    };
    for (var i = 0; i < root.childNodes.length; i++) {
        search(root.childNodes[i]);
    }
    return list;
}


document.addEventListener("deviceready", function() {
    document.removeEventListener("deviceready", arguments.callee);
    plugin.google.maps.Map.isAvailable();
});
