/* global cordova, plugin, CSSPrimitiveValue */
var PLUGIN_NAME = 'GoogleMaps';
var MARKERS = {};
var KML_LAYERS = {};
var OVERLAYS = {};

/**
 * Google Maps model.
 */
var BaseClass = function() {
    var self = this;
    var _vars = {};
    var _listeners = {};

    self.empty = function() {
        for (var key in Object.keys(_vars)) {
            _vars[key] = null;
            delete _vars[key];
        }
    };

    self.deleteFromObject = function(object, type) {
        if (object === null) return object;
        for(var index in Object.keys(object)) {
            var key = Object.keys(object)[index];
            if (typeof object[key] === 'object') {
               object[key] = self.deleteFromObject(object[key], type);
            } else if (typeof object[key] === type) {
               delete object[key];
            }
        }
        return object;
    };

    self.get = function(key) {
        return key in _vars ? _vars[key] : null;
    };
    self.set = function(key, value) {
        if (_vars[key] !== value) {
            self.trigger(key + "_changed", _vars[key], value);
        }
        _vars[key] = value;
    };

    self.trigger = function(eventName) {
        var args = [];
        for (var i = 1; i < arguments.length; i++) {
            args.push(arguments[i]);
        }
        var event = document.createEvent('Event');
        event.initEvent(eventName, false, false);
        event.mydata = args;
        event.myself = self;
        document.dispatchEvent(event);
    };
    self.on = function(eventName, callback) {
        _listeners[eventName] = _listeners[eventName] || [];

        var listener = function(e) {
            if (!e.myself || e.myself !== self) {
                return;
            }
            callback.apply(self, e.mydata);
        };
        document.addEventListener(eventName, listener, false);
        _listeners[eventName].push({
            'callback': callback,
            'listener': listener
        });
    };
    self.addEventListener = self.on;

    self.off = function(eventName, callback) {
        var i;
        if (typeof eventName === "string") {
            if (eventName in _listeners) {

                if (typeof callback === "function") {
                    for (i = 0; i < _listeners[eventName].length; i++) {
                        if (_listeners[eventName][i].callback === callback) {
                            document.removeEventListener(eventName, _listeners[eventName][i].listener);
                            _listeners[eventName].splice(i, 1);
                            break;
                        }
                    }
                } else {
                    for (i = 0; i < _listeners[eventName].length; i++) {
                        document.removeEventListener(eventName, _listeners[eventName][i].listener);
                    }
                    delete _listeners[eventName];
                }
            }
        } else {
            //Remove all event listeners except 'keepWatching_changed'
            var eventNames = Object.keys(_listeners);
            for (i = 0; i < eventNames.length; i++) {
                eventName = eventNames[i];
                if ( eventName !== 'keepWatching_changed' ) {
                    for (var j = 0; j < _listeners[eventName].length; j++) {
                        document.removeEventListener(eventName, _listeners[eventName][j].listener);
                    }
                    delete _listeners[eventName];
                }
            }
            _listeners = {};
        }
    };

    self.removeEventListener = self.off;


    self.one = function(eventName, callback) {
        _listeners[eventName] = _listeners[eventName] || [];

        var listener = function(e) {
            if (!e.myself || e.myself !== self) {
                return;
            }
            callback.apply(self, e.mydata);
            self.off(eventName, callback);
        };
        document.addEventListener(eventName, listener, false);
        _listeners[eventName].push({
            'callback': callback,
            'listener': listener
        });
    };
    self.addEventListenerOnce = self.one;

    self.errorHandler = function(msg) {
        if (msg) {
            console.error(msg);
            self.trigger('error', msg);
        }
        return false;
    };

    return self;
};
var App = function() {
    BaseClass.apply(this);
    Object.defineProperty(this, "type", {
        value: "Map",
        writable: false
    });
};
App.prototype = new BaseClass();

//-------------
// Cluster
//-------------

App.prototype.updateCluster = function(callback) {
    var self = this;
    cordova.exec(function(result) {
        if (callback) {
            callback();
        };
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['GoogleMapsClusterViewController.updateCluster']);
};

/*
 * Callback from Native
 */

App.prototype._onClusterEvent = function(eventName, obj) {
    if (isNaN(obj)) {
        this.trigger.apply(this, ['cluster_click', JSON.parse(obj)]);
    }
};

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
                arguments[i] = parseBoolean(arguments[i]);
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

    if (!isDom(div)) {
        params = div;
        params = params || {};
        params.backgroundColor = params.backgroundColor || '#ffffff';
        params.backgroundColor = HTMLColor2RGBA(params.backgroundColor);
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
        params.backgroundColor = HTMLColor2RGBA(params.backgroundColor);
        if (params.camera && params.camera.latLng) {
          params.camera.target = params.camera.latLng;
          delete params.camera.latLng;
        }
        args.push(params);

        self.set("div", div);
        args.push(getDivRect(div));
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
                size: getDivRect(element)
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
        options.backgroundColor = HTMLColor2RGBA(options.backgroundColor);
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
    enabled = parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setMyLocationEnabled', enabled]);
};
App.prototype.setIndoorEnabled = function(enabled) {
    enabled = parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setIndoorEnabled', enabled]);
};
App.prototype.setTrafficEnabled = function(enabled) {
    enabled = parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setTrafficEnabled', enabled]);
};
App.prototype.setCompassEnabled = function(enabled) {
    var self = this;
    enabled = parseBoolean(enabled);
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
    isVisible = parseBoolean(isVisible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setVisible', [isVisible]);
};
App.prototype.setClickable = function(isClickable) {
    var self = this;
    isClickable = parseBoolean(isClickable);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'pluginLayer_setClickable', [isClickable]);
};

App.prototype.setBackgroundColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'pluginLayer_setBackGroundColor', [HTMLColor2RGBA(color)]);
};


App.prototype.setDebuggable = function(debug) {
    var self = this;
    debug = parseBoolean(debug);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'pluginLayer_setDebuggable', [debug]);
};

/**
 * Sets the preference for whether all gestures should be enabled or disabled.
 */
App.prototype.setAllGesturesEnabled = function(enabled) {
    var self = this;
    enabled = parseBoolean(enabled);
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
    var size = getDivRect(target);
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
    if (isDom(div) === false || currentDiv !== div) {
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

    if (isDom(div)) {
        var children = getAllChildren(div);;
        self.set("div", div);
        args.push(getDivRect(div));
        var elements = [];
        var elemId;
        var clickable;

        for (var i = 0; i < children.length; i++) {
            element = children[i];
            if (element.nodeType != 1) {
                continue;
            }
            clickable = element.getAttribute("data-clickable");
            if (clickable && parseBoolean(clickable) == false) {
                continue;
            }
            elemId = element.getAttribute("__pluginDomId");
            if (!elemId) {
                elemId = "pgm" + Math.floor(Math.random() * Date.now()) + i;
                element.setAttribute("__pluginDomId", elemId);
            }
            elements.push({
                id: elemId,
                size: getDivRect(element)
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
    if (pixel.length == 2 && Array.isArray(pixel)) {
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
    markerOptions.params = markerOptions.params || {};
    if ("styles" in markerOptions) {
        markerOptions.styles = typeof markerOptions.styles === "object" ? markerOptions.styles : {};

        if ("color" in markerOptions.styles) {
            markerOptions.styles.color = HTMLColor2RGBA(markerOptions.styles.color || "#000000");
        }
    }
    if (markerOptions.icon && isHTMLColorString(markerOptions.icon)) {
        markerOptions.icon = HTMLColor2RGBA(markerOptions.icon);
    }


    var markerClick = markerOptions.markerClick;
    var infoClick = markerOptions.infoClick;

    cordova.exec(function(result) {
        markerOptions.hashCode = result.hashCode;
        var marker = new Marker(self, result.id, markerOptions);

        MARKERS[result.id] = marker;
        OVERLAYS[result.id] = marker;

        if (typeof markerClick === "function") {
            marker.on(plugin.google.maps.event.MARKER_CLICK, markerClick);
        }
        if (typeof infoClick === "function") {
            marker.on(plugin.google.maps.event.INFO_CLICK, infoClick);
        }
        if (typeof callback === "function") {
            callback.call(self, marker, self);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.createMarker', self.deleteFromObject(markerOptions,'function')]);
};


//-------------
// Circle
//-------------
App.prototype.addCircle = function(circleOptions, callback) {
    var self = this;
    circleOptions.center = circleOptions.center || {};
    circleOptions.center.lat = circleOptions.center.lat || 0.0;
    circleOptions.center.lng = circleOptions.center.lng || 0.0;
    circleOptions.strokeColor = HTMLColor2RGBA(circleOptions.strokeColor || "#FF0000", 0.75);
    circleOptions.fillColor = HTMLColor2RGBA(circleOptions.fillColor || "#000000", 0.75);
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
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.createCircle', self.deleteFromObject(circleOptions,'function')]);
};
//-------------
// Polyline
//-------------
App.prototype.addPolyline = function(polylineOptions, callback) {
    var self = this;
    polylineOptions.points = polylineOptions.points || [];
    polylineOptions.color = HTMLColor2RGBA(polylineOptions.color || "#FF000080", 0.75);
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
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.createPolyline', self.deleteFromObject(polylineOptions,'function')]);
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
    polygonOptions.strokeColor = HTMLColor2RGBA(polygonOptions.strokeColor || "#FF000080", 0.75);
    if (polygonOptions.fillColor) {
        polygonOptions.fillColor = HTMLColor2RGBA(polygonOptions.fillColor, 0.75);
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
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.createPolygon', self.deleteFromObject(polygonOptions,'function')]);
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
Marker.prototype = new BaseClass();

Marker.prototype.isVisible = function() {
    return this.get('visible');
};


Marker.prototype.getPosition = function(callback) {
    var self = this;
    cordova.exec(function(latlng) {
        if (typeof callback === "function") {
            callback.call(self, new LatLng(latlng.lat, latlng.lng));
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.getPosition', this.getId()]);
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
    }, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setAnimation', this.getId(), self.deleteFromObject(animation,'function')]);
};

Marker.prototype.remove = function(callback) {
    var self = this;
    self.set("keepWatching", false);
    delete MARKERS[this.id];
    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.remove', this.getId()]);
    this.off();
};
Marker.prototype.setDisableAutoPan = function(disableAutoPan) {
    disableAutoPan = parseBoolean(disableAutoPan);
    this.set('disableAutoPan', disableAutoPan);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setDisableAutoPan', this.getId(), disableAutoPan]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setOpacity', this.getId(), opacity]);
};
Marker.prototype.setZIndex = function(zIndex) {
    if (typeof zIndex === 'undefined') {
        return false;
    }
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setZIndex', this.getId(), zIndex]);
};
Marker.prototype.getOpacity = function() {
    return this.get('opacity');
};
Marker.prototype.setIconAnchor = function(anchorX, anchorY) {
    this.set('anchor', [anchorX, anchorY]);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setIconAnchor', this.getId(), anchorX, anchorY]);
};
Marker.prototype.setInfoWindowAnchor = function(anchorX, anchorY) {
    this.set('anchor', [anchorX, anchorY]);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setInfoWindowAnchor', this.getId(), anchorX, anchorY]);
};
Marker.prototype.setDraggable = function(draggable) {
    draggable = parseBoolean(draggable);
    this.set('draggable', draggable);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setDraggable', this.getId(), draggable]);
};
Marker.prototype.isDraggable = function() {
    return this.get('draggable');
};
Marker.prototype.setFlat = function(flat) {
    flat = parseBoolean(flat);
    this.set('flat', flat);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setFlat', this.getId(), flat]);
};
Marker.prototype.setIcon = function(url) {
    if (url && isHTMLColorString(url)) {
        url = HTMLColor2RGBA(url);
    }
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setIcon', this.getId(), url]);
};
Marker.prototype.setTitle = function(title) {
    if (!title) {
        console.log('missing value for title');
        return false;
    }
    this.set('title', String(title));
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setTitle', this.getId(), title]);
};
Marker.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setVisible', this.getId(), visible]);
};
Marker.prototype.getTitle = function() {
    return this.get('title');
};
Marker.prototype.setSnippet = function(snippet) {
    this.set('snippet', snippet);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setSnippet', this.getId(), snippet]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setRotation', this.getId(), rotation]);
};
Marker.prototype.getRotation = function() {
    return this.get('rotation');
};
Marker.prototype.showInfoWindow = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.showInfoWindow', this.getId()]);
};
Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.hideInfoWindow', this.getId()]);
};
Marker.prototype.isInfoWindowShown = function(callback) {
    var self = this;
    cordova.exec(function(isVisible) {
        isVisible = parseBoolean(isVisible);
        if (typeof callback === "function") {
            callback.call(self, isVisible);
        }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.isInfoWindowShown', this.getId()]);
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
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setPosition', this.getId(), position.lat, position.lng]);
};


/*****************************************************************************
 * Circle Class
 *****************************************************************************/
var Circle = function(map, circleId, circleOptions) {
    BaseClass.apply(this);

    var self = this;
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: circleId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "Circle",
        writable: false
    });

    var ignores = ["map", "id", "type"];
    for (var key in circleOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, circleOptions[key]);
        }
    }
};

Circle.prototype = new BaseClass();

Circle.prototype.getMap = function() {
    return this.map;
};
Circle.prototype.getId = function() {
    return this.id;
};
Circle.prototype.getCenter = function() {
    return this.get('center');
};
Circle.prototype.getRadius = function() {
    return this.get('radius');
};
Circle.prototype.getStrokeColor = function() {
    return this.get('strokeColor');
};
Circle.prototype.getStrokeWidth = function() {
    return this.get('strokeWidth');
};
Circle.prototype.getZIndex = function() {
    return this.get('zIndex');
};
Circle.prototype.getVisible = function() {
    return this.get('visible');
};
Circle.prototype.remove = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.remove', this.getId()]);
    this.off();
};
Circle.prototype.setCenter = function(center) {
    this.set('center', center);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setCenter', this.getId(), center.lat, center.lng]);
};
Circle.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setFillColor', this.getId(), HTMLColor2RGBA(color, 0.75)]);
};
Circle.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setStrokeColor', this.getId(), HTMLColor2RGBA(color, 0.75)]);
};
Circle.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setStrokeWidth', this.getId(), width]);
};
Circle.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setVisible', this.getId(), visible]);
};
Circle.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setZIndex', this.getId(), zIndex]);
};
Circle.prototype.setRadius = function(radius) {
    this.set('radius', radius);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setRadius', this.getId(), radius]);
};
/*****************************************************************************
 * Polyline Class
 *****************************************************************************/
var Polyline = function(map, polylineId, polylineOptions) {
    BaseClass.apply(this);

    var self = this;
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: polylineId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "Polyline",
        writable: false
    });

    var ignores = ["map", "id", "type"];
    for (var key in polylineOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, polylineOptions[key]);
        }
    }
};

Polyline.prototype = new BaseClass();

Polyline.prototype.getId = function() {
    return this.id;
};

Polyline.prototype.setPoints = function(points) {
    this.set('points', points);
    var i,
        path = [];
    for (i = 0; i < points.length; i++) {
        path.push({
            "lat": points[i].lat,
            "lng": points[i].lng
        });
    }
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setPoints', this.getId(), path]);
};
Polyline.prototype.getPoints = function() {
    return this.get("points");
};
Polyline.prototype.setColor = function(color) {
    this.set('color', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setColor', this.getId(), HTMLColor2RGBA(color, 0.75)]);
};
Polyline.prototype.getColor = function() {
    return this.get('color');
};
Polyline.prototype.setWidth = function(width) {
    this.set('width', width);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setWidth', this.getId(), width]);
};
Polyline.prototype.getWidth = function() {
    return this.get('width');
};
Polyline.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setVisible', this.getId(), visible]);
};
Polyline.prototype.getVisible = function() {
    return this.get('visible');
};
Polyline.prototype.setGeodesic = function(geodesic) {
    geodesic = parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setGeodesic', this.getId(), geodesic]);
};
Polyline.prototype.getGeodesic = function() {
    return this.get('geodesic');
};
Polyline.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setZIndex', this.getId(), zIndex]);
};
Polyline.prototype.getZIndex = function() {
    return this.get('zIndex');
};
Polyline.prototype.remove = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.remove', this.getId()]);
    this.off();
};

Polyline.prototype.getMap = function() {
    return this.map;
};
/*****************************************************************************
 * Polygon Class
 *****************************************************************************/
var Polygon = function(map, polygonId, polygonOptions) {
    BaseClass.apply(this);

    var self = this;
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: polygonId,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "Polygon",
        writable: false
    });
    var ignores = ["map", "id", "type"];
    for (var key in polygonOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, polygonOptions[key]);
        }
    }
};

Polygon.prototype = new BaseClass();

Polygon.prototype.getMap = function() {
    return this.map;
};
Polygon.prototype.getId = function() {
    return this.id;
};
Polygon.prototype.setPoints = function(points) {
    this.set('points', points);
    var i,
        path = [];
    for (i = 0; i < points.length; i++) {
        path.push({
            "lat": points[i].lat,
            "lng": points[i].lng
        });
    }
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setPoints', this.getId(), path]);
};
Polygon.prototype.getPoints = function() {
    return this.get("points");
};
Polygon.prototype.setHoles = function(holes) {
    this.set('holes', holes);
    holes = holes || [];
    if (holes.length > 0 && !Array.isArray(holes[0])) {
      holes = [holes];
    }
    holes = holes.map(function(hole) {
      if (!Array.isArray(hole)) {
        return [];
      }
      return hole.map(function(latLng) {
        return {lat: latLng.lat, lng: latLng.lng};
      });
    });
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setHoles', this.getId(), holes]);
};
Polygon.prototype.getHoles = function() {
    return this.get("holes");
};
Polygon.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setFillColor', this.getId(), HTMLColor2RGBA(color, 0.75)]);
};
Polygon.prototype.getFillColor = function() {
    return this.get('fillColor');
};
Polygon.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setStrokeColor', this.getId(), HTMLColor2RGBA(color, 0.75)]);
};
Polygon.prototype.getStrokeColor = function() {
    return this.get('strokeColor');
};
Polygon.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setStrokeWidth', this.getId(), width]);
};
Polygon.prototype.getStrokeWidth = function() {
    return this.get('strokeWidth');
};
Polygon.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setVisible', this.getId(), visible]);
};
Polygon.prototype.getVisible = function() {
    return this.get('visible');
};
Polygon.prototype.setGeodesic = function(geodesic) {
    geodesic = parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setGeodesic', this.getId(), geodesic]);
};
Polygon.prototype.getGeodesic = function() {
    return this.get('geodesic');
};
Polygon.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setZIndex', this.getId(), zIndex]);
};
Polygon.prototype.getZIndex = function() {
    return this.get('zIndex');
};
Polygon.prototype.remove = function() {
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.remove', this.getId()]);
    this.off();
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
    fadeIn = parseBoolean(fadeIn);
    this.set('fadeIn', fadeIn);
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setFadeIn', this.getId(), fadeIn]);
};
TileOverlay.prototype.getFadeIn = function() {
    return this.get('fadeIn');
};
TileOverlay.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
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
//---------------------------
// Convert HTML color to RGB
//---------------------------
function isHTMLColorString(inputValue) {
    if (!inputValue || typeof inputValue !== "string") {
        return false;
    }
    if (inputValue.match(/^#[0-9A-F]{3}$/i) ||
        inputValue.match(/^#[0-9A-F]{4}$/i) ||
        inputValue.match(/^#[0-9A-F]{6}$/i) ||
        inputValue.match(/^#[0-9A-F]{8}$/i) ||
        inputValue.match(/^rgba?\([\d,.\s]+\)$/) ||
        inputValue.match(/^hsla?\([\d%,.\s]+\)$/)) {
        return true;
    }

    inputValue = inputValue.toLowerCase();
    return inputValue in HTML_COLORS;
}

function HTMLColor2RGBA(colorValue, defaultOpacity) {
    defaultOpacity = !defaultOpacity ? 1.0 : defaultOpacity;
    if(colorValue instanceof Array) {
        return colorValue;
    }
    if (colorValue === "transparent" || !colorValue) {
        return [0, 0, 0, 0];
    }
    var alpha = Math.floor(255 * defaultOpacity),
        matches,
        result = {
            r: 0,
            g: 0,
            b: 0
        };
    var colorStr = colorValue.toLowerCase();
    if (colorStr in HTML_COLORS) {
        colorStr = HTML_COLORS[colorStr];
    }
    if (colorStr.match(/^#([0-9A-F]){3}$/i)) {
        matches = colorStr.match(/([0-9A-F])/ig);

        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            alpha
        ];
    }

    if (colorStr.match(/^#[0-9A-F]{4}$/i)) {
        alpha = colorStr.substr(4, 1);
        alpha = parseInt(alpha + alpha, 16);

        matches = colorStr.match(/([0-9A-F])/ig);
        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            alpha
        ];
    }

    if (colorStr.match(/^#[0-9A-F]{6}$/i)) {
        matches = colorStr.match(/([0-9A-F]{2})/ig);
        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            alpha
        ];
    }
    if (colorStr.match(/^#[0-9A-F]{8}$/i)) {
        matches = colorStr.match(/([0-9A-F]{2})/ig);

        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            parseInt(matches[3], 16)
        ];
    }
    // convert rgb(), rgba()
    if (colorStr.match(/^rgba?\([\d,.\s]+\)$/)) {
        matches = colorStr.match(/([\d.]+)/g);
        alpha = matches.length == 4 ? Math.floor(parseFloat(matches[3]) * 256) : alpha;
        return [
            parseInt(matches[0], 10),
            parseInt(matches[1], 10),
            parseInt(matches[2], 10),
            alpha
        ];
    }


    // convert hsl(), hsla()
    if (colorStr.match(/^hsla?\([\d%,.\s]+\)$/)) {
        matches = colorStr.match(/([\d%.]+)/g);
        alpha = matches.length == 4 ? Math.floor(parseFloat(matches[3]) * 256) : alpha;
        var rgb = HLStoRGB(matches[0], matches[1], matches[2]);
        rgb.push(alpha);
        return rgb;
    }

    console.log("Warning: '" + colorValue + "' is not available. The overlay is drew by black.");
    return [0, 0, 0, alpha];
}

/**
 * http://d.hatena.ne.jp/ja9/20100907/1283840213
 */
function HLStoRGB(h, l, s) {
    var r, g, b; // 0..255

    while (h < 0) {
        h += 360;
    }
    h = h % 360;

    // In case of saturation = 0
    if (s == 0) {
        // RGB are the same as V
        l = Math.round(l * 255);
        return [l, l, l];
    }

    var m2 = (l < 0.5) ? l * (1 + s) : l + s - l * s,
        m1 = l * 2 - m2,
        tmp;

    tmp = h + 120;
    if (tmp > 360) {
        tmp = tmp - 360;
    }

    if (tmp < 60) {
        r = (m1 + (m2 - m1) * tmp / 60);
    } else if (tmp < 180) {
        r = m2;
    } else if (tmp < 240) {
        r = m1 + (m2 - m1) * (240 - tmp) / 60;
    } else {
        r = m1;
    }

    tmp = h;
    if (tmp < 60) {
        g = m1 + (m2 - m1) * tmp / 60;
    } else if (tmp < 180) {
        g = m2;
    } else if (tmp < 240) {
        g = m1 + (m2 - m1) * (240 - tmp) / 60;
    } else {
        g = m1;
    }

    tmp = h - 120;
    if (tmp < 0) {
        tmp = tmp + 360;
    }
    if (tmp < 60) {
        b = m1 + (m2 - m1) * tmp / 60;
    } else if (tmp < 180) {
        b = m2;
    } else if (tmp < 240) {
        b = m1 + (m2 - m1) * (240 - tmp) / 60;
    } else {
        b = m1;
    }
    return [Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)];
}

function parseBoolean(boolValue) {
    return typeof(boolValue) === "string" && boolValue.toLowerCase() === "true" ||
        boolValue === true ||
        boolValue === 1;
}

function isDom(element) {
    return !!element &&
        typeof element === "object" &&
        "getBoundingClientRect" in element;
}

function getPageRect() {
    var doc = document.documentElement;

    var pageWidth = window.innerWidth ||
        document.documentElement.clientWidth ||
        document.body.clientWidth,
        pageHeight = window.innerHeight ||
        document.documentElement.clientHeight ||
        document.body.clientHeight;
    var pageLeft = (window.pageXOffset || doc.scrollLeft) - (doc.clientLeft || 0);
    var pageTop = (window.pageYOffset || doc.scrollTop) - (doc.clientTop || 0);

    return {
        'width': pageWidth,
        'height': pageHeight,
        'left': pageLeft,
        'top': pageTop
    };
}

function getDivRect(div) {
    if (!div) {
        return;
    }

    var pageRect = getPageRect();

    var rect = div.getBoundingClientRect();
    var divRect = {
        'left': rect.left + pageRect.left,
        'top': rect.top + pageRect.top,
        'width': rect.width,
        'height': rect.height
    };

    return divRect;
}

function onMapResize(event) {
    var self = window.plugin.google.maps.Map;
    var div = self.get("div");
    if (!div) {
        return;
    }
    if (isDom(div) === false) {
        self.set("div", null);
        cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setDiv', []);
    } else {
        var args = [];
        var element, elements = [];
        var children = getAllChildren(div);
        var elemId, clickable;

        args.push(getDivRect(div));
        for (var i = 0; i < children.length; i++) {
            element = children[i];
            if (element.nodeType != 1) {
                continue;
            }
            clickable = element.getAttribute("data-clickable");
            if (clickable && parseBoolean(clickable) == false) {
                continue;
            }
            elemId = element.getAttribute("__pluginDomId");
            if (!elemId) {
                elemId = "pgm" + Math.floor(Math.random() * Date.now()) + i;
                element.setAttribute("__pluginDomId", elemId);
            }
            elements.push({
                id: elemId,
                size: getDivRect(element)
            });
        }
        args.push(elements);
        //alert(JSON.stringify(getDivRect(div), null, 4));
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
            divSize = getDivRect(div);
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

    BaseClass: BaseClass,
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
    window.plugin = window.plugin || {};
    window.plugin.google = window.plugin.google || {};
    window.plugin.google.maps = window.plugin.google.maps || module.exports;
});
window.addEventListener("orientationchange", function() {
    setTimeout(onMapResize, 1000);
});


function getAllChildren(root) {
    var list = [];
    var clickable;
    var style, displayCSS, opacityCSS, visibilityCSS, node, clickableSize;

    var allClickableElements = Array.prototype.slice.call(root.querySelectorAll(':not([data-clickable="false"])'));
    var clickableElements =  allClickableElements.filter(function(i) {return i != root;});

    for (var i = 0; i < clickableElements.length; i++) {
        node = clickableElements[i];
        if (node.nodeType == 1){
          style = window.getComputedStyle(node);
          visibilityCSS = style.getPropertyValue('visibility');
          displayCSS = style.getPropertyValue('display');
          opacityCSS = style.getPropertyValue('opacity');
          heightCSS = style.getPropertyValue('height')
          widthCSS = style.getPropertyValue('width')
          clickableSize = (heightCSS != "0px" && widthCSS != "0px" && node.clientHeight > 0 && node.clientWidth > 0);
          if (displayCSS !== "none" && opacityCSS > 0 && visibilityCSS != "hidden" && clickableSize) {
            list.push(node);
          }
        }
    }

    return list;
}


document.addEventListener("deviceready", function() {
    document.removeEventListener("deviceready", arguments.callee);
    plugin.google.maps.Map.isAvailable();
});

var HTML_COLORS = {
    "aliceblue": "#f0f8ff",
    "antiquewhite": "#faebd7",
    "aqua": "#00ffff",
    "aquamarine": "#7fffd4",
    "azure": "#f0ffff",
    "beige": "#f5f5dc",
    "bisque": "#ffe4c4",
    "black": "#000000",
    "blanchedalmond": "#ffebcd",
    "blue": "#0000ff",
    "blueviolet": "#8a2be2",
    "brown": "#a52a2a",
    "burlywood": "#deb887",
    "cadetblue": "#5f9ea0",
    "chartreuse": "#7fff00",
    "chocolate": "#d2691e",
    "coral": "#ff7f50",
    "cornflowerblue": "#6495ed",
    "cornsilk": "#fff8dc",
    "crimson": "#dc143c",
    "cyan": "#00ffff",
    "darkblue": "#00008b",
    "darkcyan": "#008b8b",
    "darkgoldenrod": "#b8860b",
    "darkgray": "#a9a9a9",
    "darkgrey": "#a9a9a9",
    "darkgreen": "#006400",
    "darkkhaki": "#bdb76b",
    "darkmagenta": "#8b008b",
    "darkolivegreen": "#556b2f",
    "darkorange": "#ff8c00",
    "darkorchid": "#9932cc",
    "darkred": "#8b0000",
    "darksalmon": "#e9967a",
    "darkseagreen": "#8fbc8f",
    "darkslateblue": "#483d8b",
    "darkslategray": "#2f4f4f",
    "darkslategrey": "#2f4f4f",
    "darkturquoise": "#00ced1",
    "darkviolet": "#9400d3",
    "deeppink": "#ff1493",
    "deepskyblue": "#00bfff",
    "dimgray": "#696969",
    "dimgrey": "#696969",
    "dodgerblue": "#1e90ff",
    "firebrick": "#b22222",
    "floralwhite": "#fffaf0",
    "forestgreen": "#228b22",
    "fuchsia": "#ff00ff",
    "gainsboro": "#dcdcdc",
    "ghostwhite": "#f8f8ff",
    "gold": "#ffd700",
    "goldenrod": "#daa520",
    "gray": "#808080",
    "grey": "#808080",
    "green": "#008000",
    "greenyellow": "#adff2f",
    "honeydew": "#f0fff0",
    "hotpink": "#ff69b4",
    "indianred ": "#cd5c5c",
    "indigo  ": "#4b0082",
    "ivory": "#fffff0",
    "khaki": "#f0e68c",
    "lavender": "#e6e6fa",
    "lavenderblush": "#fff0f5",
    "lawngreen": "#7cfc00",
    "lemonchiffon": "#fffacd",
    "lightblue": "#add8e6",
    "lightcoral": "#f08080",
    "lightcyan": "#e0ffff",
    "lightgoldenrodyellow": "#fafad2",
    "lightgray": "#d3d3d3",
    "lightgrey": "#d3d3d3",
    "lightgreen": "#90ee90",
    "lightpink": "#ffb6c1",
    "lightsalmon": "#ffa07a",
    "lightseagreen": "#20b2aa",
    "lightskyblue": "#87cefa",
    "lightslategray": "#778899",
    "lightslategrey": "#778899",
    "lightsteelblue": "#b0c4de",
    "lightyellow": "#ffffe0",
    "lime": "#00ff00",
    "limegreen": "#32cd32",
    "linen": "#faf0e6",
    "magenta": "#ff00ff",
    "maroon": "#800000",
    "mediumaquamarine": "#66cdaa",
    "mediumblue": "#0000cd",
    "mediumorchid": "#ba55d3",
    "mediumpurple": "#9370db",
    "mediumseagreen": "#3cb371",
    "mediumslateblue": "#7b68ee",
    "mediumspringgreen": "#00fa9a",
    "mediumturquoise": "#48d1cc",
    "mediumvioletred": "#c71585",
    "midnightblue": "#191970",
    "mintcream": "#f5fffa",
    "mistyrose": "#ffe4e1",
    "moccasin": "#ffe4b5",
    "navajowhite": "#ffdead",
    "navy": "#000080",
    "oldlace": "#fdf5e6",
    "olive": "#808000",
    "olivedrab": "#6b8e23",
    "orange": "#ffa500",
    "orangered": "#ff4500",
    "orchid": "#da70d6",
    "palegoldenrod": "#eee8aa",
    "palegreen": "#98fb98",
    "paleturquoise": "#afeeee",
    "palevioletred": "#db7093",
    "papayawhip": "#ffefd5",
    "peachpuff": "#ffdab9",
    "peru": "#cd853f",
    "pink": "#ffc0cb",
    "plum": "#dda0dd",
    "powderblue": "#b0e0e6",
    "purple": "#800080",
    "rebeccapurple": "#663399",
    "red": "#ff0000",
    "rosybrown": "#bc8f8f",
    "royalblue": "#4169e1",
    "saddlebrown": "#8b4513",
    "salmon": "#fa8072",
    "sandybrown": "#f4a460",
    "seagreen": "#2e8b57",
    "seashell": "#fff5ee",
    "sienna": "#a0522d",
    "silver": "#c0c0c0",
    "skyblue": "#87ceeb",
    "slateblue": "#6a5acd",
    "slategray": "#708090",
    "slategrey": "#708090",
    "snow": "#fffafa",
    "springgreen": "#00ff7f",
    "steelblue": "#4682b4",
    "tan": "#d2b48c",
    "teal": "#008080",
    "thistle": "#d8bfd8",
    "tomato": "#ff6347",
    "turquoise": "#40e0d0",
    "violet": "#ee82ee",
    "wheat": "#f5deb3",
    "white": "#ffffff",
    "whitesmoke": "#f5f5f5",
    "yellow": "#ffff00",
    "yellowgreen": "#9acd32"
};
