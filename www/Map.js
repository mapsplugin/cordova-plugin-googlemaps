var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    LatLng = require('./LatLng'),
    LatLngBounds = require('./LatLngBounds'),
    MapTypeId = require('./MapTypeId'),
    event = require('./event');

var Marker = require('./Marker');
var Circle = require('./Circle');
var Polyline = require('./Polyline');
var Polygon = require('./Polygon');
var TileOverlay = require('./TileOverlay');
var GroundOverlay = require('./GroundOverlay');
var KmlOverlay = require('./KmlOverlay');
var CameraPosition = require('./CameraPosition');


/**
 * Google Maps model.
 */
var Map = function(id) {
    var self = this;
    BaseClass.apply(self);

    self.MARKERS = {};
    self.KML_LAYERS = {};
    self.OVERLAYS = {};

    Object.defineProperty(self, "type", {
        value: "Map",
        writable: false
    });

    Object.defineProperty(self, "id", {
        value: id,
        writable: false
    });

};

utils.extend(Map, BaseClass);

Map.prototype.getId = function() {
    return this.id;
};

/**
 * @desc Recalculate the position of HTML elements
 */
Map.prototype.refreshLayout = function(event) {
    var self = this;
    var div = self.get("div");
    if (!div) {
        return;
    }
    if (common.isDom(div) === false) {
        self.set("div", null);
        cordova.exec(null, self.errorHandler, self.id, 'setDiv', []);
    } else {
        var args = [];
        var element, elements = [];
        var children = common.getAllChildren(div);
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
        //console.log("----> mapId = " + self.getId() + " / " + JSON.stringify(common.getDivRect(div), null, 4));
        cordova.exec(null, null, self.id, 'resizeMap', args);
    }
};

Map.prototype.getMap = function(mapId, div, params) {
    var self = this,
        args = [mapId];

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
            currentDiv.removeEventListener("DOMNodeRemoved", common._remove_child.bind(self));

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

        div.addEventListener("DOMNodeRemoved", common._remove_child.bind(self));
        div.addEventListener("DOMNodeInserted", common._append_child.bind(self));

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
            //self.refreshLayout();
            self.trigger(event.MAP_READY, self);
        }, 100);
    }, self.errorHandler, 'GoogleMaps', 'getMap', args);
    return self;
};


Map.prototype.getLicenseInfo = function(callback) {
    var self = this;
    cordova.exec(function(txt) {
        callback.call(self, txt);
    }, self.errorHandler, 'GoogleMaps', 'getLicenseInfo', []);
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
    cordova.exec(null, this.errorHandler, this.id, 'setOptions', [this.deleteFromObject(options,'function')]);
};

Map.prototype.setCenter = function(latLng) {
    this.set('center', latLng);
    cordova.exec(null, this.errorHandler, this.id, 'setCenter', [latLng.lat, latLng.lng]);
};

Map.prototype.setZoom = function(zoom) {
    this.set('zoom', zoom);
    cordova.exec(null, this.errorHandler, this.id, 'setZoom', [zoom]);
};
Map.prototype.panBy = function(x, y) {
    x = parseInt(x, 10);
    y = parseInt(y, 10);
    cordova.exec(null, this.errorHandler, this.id, 'panBy', [x, y]);
};

/**
 * Clears all markup that has been added to the map,
 * including markers, polylines and ground overlays.
 */
Map.prototype.clear = function(callback) {
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

    clearObj(self.OVERLAYS);
    clearObj(self.MARKERS);
    clearObj(self.KML_LAYERS);

    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, self.errorHandler, this.id, 'clear', []);
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
    cordova.exec(null, this.errorHandler, this.id, 'setMapTypeId', [mapTypeId]);
};

/**
 * @desc Change the map view angle
 * @param {Number} tilt  The angle
 */
Map.prototype.setTilt = function(tilt) {
    this.set('tilt', tilt);
    cordova.exec(null, this.errorHandler, this.id, 'setTilt', [tilt]);
};


/**
 * @desc Open the map dialog
 */
Map.prototype.showDialog = function() {
    document.addEventListener("backbutton", common._onBackbutton, false);
    document.addEventListener('map_close', function() {
        document.removeEventListener("backbutton", onBackbutton, false);
        document.removeEventListener('map_close', arguments.callee, false);
    }, false);
    cordova.exec(null, this.errorHandler, 'GoogleMaps', 'showDialog', []);
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

    // TODO: fix the below shoddy code...
    if (!cameraPosition.hasOwnProperty('zoom')) {
        self.getZoom(function(zoom) {
            cameraPosition.zoom = zoom;
        });
    }

    // TODO: fix the below shoddy code...
    if (!cameraPosition.hasOwnProperty('tilt')) {
        self.getTilt(function(tilt) {
            cameraPosition.tilt = tilt;
        });
    }

    // TODO: fix the below shoddy code...
    if (!cameraPosition.hasOwnProperty('bearing')) {
        self.getBearing(function(bearing) {
            cameraPosition.bearing = bearing;
        });
    }

    // TODO: fix the below shoddy code...
    var self = this;
    setTimeout(function() {
        cordova.exec(function() {
            if (typeof callback === "function") {
                callback.call(self);
            }
        }, self.errorHandler, self.id, 'animateCamera', [self.deleteFromObject(cameraPosition,'function')]);
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

    // TODO: fix the below shoddy code...
    if (!cameraPosition.hasOwnProperty('zoom')) {
        self.getZoom(function(zoom) {
            cameraPosition.zoom = zoom;
        });
    }

    // TODO: fix the below shoddy code...
    if (!cameraPosition.hasOwnProperty('tilt')) {
        self.getTilt(function(tilt) {
            cameraPosition.tilt = tilt;
        });
    }

    // TODO: fix the below shoddy code...
    if (!cameraPosition.hasOwnProperty('bearing')) {
        self.getBearing(function(bearing) {
            cameraPosition.bearing = bearing;
        });
    }

    // TODO: fix the below shoddy code...
    setTimeout(function() {
        cordova.exec(function() {
            if (typeof callback === "function") {
                callback.call(self);
            }
        }, self.errorHandler, self.id, 'moveCamera', [self.deleteFromObject(cameraPosition,'function')]);
    }.bind(self), 10);

};

Map.prototype.setMyLocationEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, self.id, 'setMyLocationEnabled', [enabled]);
};
Map.prototype.setIndoorEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, self.id, 'setIndoorEnabled', [enabled]);
};
Map.prototype.setTrafficEnabled = function(enabled) {
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, this.errorHandler, self.id, 'setTrafficEnabled', [enabled]);
};
Map.prototype.setCompassEnabled = function(enabled) {
    var self = this;
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, self.id, 'setCompassEnabled', [enabled]);
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
    cordova.exec(successHandler, errorHandler, 'GoogleMaps', 'getMyLocation', [self.deleteFromObject(params,'function')]);
};
Map.prototype.getFocusedBuilding = function(callback) {
    var self = this;
    cordova.exec(callback, this.errorHandler, self.id, 'getFocusedBuilding', []);
};
Map.prototype.setVisible = function(isVisible) {
    var self = this;
    isVisible = common.parseBoolean(isVisible);
    cordova.exec(null, self.errorHandler, self.id, 'setVisible', [isVisible]);
};
Map.prototype.setClickable = function(isClickable) {
    var self = this;
    isClickable = common.parseBoolean(isClickable);
    cordova.exec(null, self.errorHandler, 'GoogleMaps', 'pluginLayer_setClickable', [isClickable]);
};

Map.prototype.setBackgroundColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, 'GoogleMaps', 'pluginLayer_setBackGroundColor', [common.HTMLColor2RGBA(color)]);
};


Map.prototype.setDebuggable = function(debug) {
    var self = this;
    debug = common.parseBoolean(debug);
    cordova.exec(null, self.errorHandler, 'GoogleMaps', 'pluginLayer_setDebuggable', [debug]);
};

/**
 * Sets the preference for whether all gestures should be enabled or disabled.
 */
Map.prototype.setAllGesturesEnabled = function(enabled) {
    var self = this;
    enabled = common.parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, self.id, 'setAllGesturesEnabled', [enabled]);
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
    }, self.errorHandler, self.id, 'getCameraPosition', []);
};


Map.prototype.getZoom = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.zoom);
        }
    }, self.errorHandler, self.id, 'getCameraPosition', []);
};

Map.prototype.getTilt = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.tilt);
        }
    }, self.errorHandler, self.id, 'getCameraPosition', []);
};

Map.prototype.getBearing = function(callback) {
    var self = this;
    cordova.exec(function(camera) {
        if (typeof callback === "function") {
            callback.call(self, camera.bearing);
        }
    }, self.errorHandler, self.id, 'getCameraPosition', []);
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
    self.trigger("remove");
    self.set('div', undefined);
    self.set("keepWatching", false);
    self.clear();
    self.empty();
    self.off();
    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, self.errorHandler, self.id, 'remove', []);
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
    }, 'GoogleMaps', 'isAvailable', ['']);
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
    }, self.errorHandler, self.id, 'toDataURL', [self.deleteFromObject(params,'function')]);
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
            currentDiv.removeEventListener("DOMNodeRemoved", common._remove_child.bind(self));

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

        div.addEventListener("DOMNodeRemoved", common._remove_child.bind(self));
        div.addEventListener("DOMNodeInserted", common._append_child.bind(self));

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
            self.set("keepWatching", true);
    }
    cordova.exec(null, self.errorHandler, 'GoogleMaps', 'setDiv', self.deleteFromObject(args,'function'));
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
    }, self.errorHandler, self.id, 'getVisibleRegion', []);
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
        }, self.errorHandler, self.id, 'fromLatLngToPoint', [latLng.lat, latLng.lng]);
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
        }, self.errorHandler, self.id, 'fromPointToLatLng', [pixel[0], pixel[1]]);
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
    }, self.errorHandler, this.id, 'setPadding', [padding]);
};


//-------------
// KML Layer
//-------------
Map.prototype.addKmlOverlay = function(kmlOverlayOptions, callback) {
    var self = this;
    kmlOverlayOptions = kmlOverlayOptions || {};
    kmlOverlayOptions.url = kmlOverlayOptions.url || null;
    kmlOverlayOptions.preserveViewport = kmlOverlayOptions.preserveViewport  === true;
    kmlOverlayOptions.animation = kmlOverlayOptions.animation === undefined ? true : kmlOverlayOptions.animation;

    var kmlId = "kml" + (Math.random() * 9999999);
    kmlOverlayOptions.kmlId = kmlId;

    var kmlOverlay = new KmlOverlay(self, kmlId, kmlOverlayOptions);
    self.OVERLAYS[kmlId] = kmlOverlay;
    self.KML_LAYERS[kmlId] = kmlOverlay;

    cordova.exec(function(kmlId) {
        if (typeof callback === "function") {
            callback.call(self, kmlOverlay, self);
        }
    }, self.errorHandler, self.id, 'loadPlugin', ['KmlOverlay', self.deleteFromObject(kmlOverlayOptions,'function')]);

};

//-------------
// Ground overlay
//-------------
Map.prototype.addGroundOverlay = function(groundOverlayOptions, callback) {
    var self = this;
    groundOverlayOptions = groundOverlayOptions || {};
    groundOverlayOptions.url = groundOverlayOptions.url || null;
    groundOverlayOptions.visible = groundOverlayOptions.visible === undefined ? true : groundOverlayOptions.visible;
    groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 1;
    groundOverlayOptions.bounds = groundOverlayOptions.bounds || [];

    cordova.exec(function(result) {
        var groundOverlay = new GroundOverlay(self, result.id, groundOverlayOptions);
        self.OVERLAYS[result.id] = groundOverlay;
        if (typeof groundOverlayOptions.onClick === "function") {
            groundOverlay.on(event.OVERLAY_CLICK, groundOverlayOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, groundOverlay, self);
        }
    }, self.errorHandler, self.id, 'loadPlugin', ['GroundOverlay', self.deleteFromObject(groundOverlayOptions,'function')]);

};

//-------------
// Tile overlay
//-------------
Map.prototype.addTileOverlay = function(tilelayerOptions, callback) {
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
        self.OVERLAYS[result.id] = tileOverlay;
        if (typeof callback === "function") {
            callback.call(self, tileOverlay, self);
        }
    }, self.errorHandler, self.id, 'loadPlugin', ['TileOverlay', self.deleteFromObject(tilelayerOptions,'function')]);
};

//-------------
// Polygon
//-------------
Map.prototype.addPolygon = function(polygonOptions, callback) {
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
        self.OVERLAYS[result.id] = polygon;
        if (typeof polygonOptions.onClick === "function") {
            polygon.on(event.OVERLAY_CLICK, polygonOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, polygon, self);
        }
    }, self.errorHandler, self.id, 'loadPlugin', ["Polygon", self.deleteFromObject(polygonOptions,'function')]);
};

//-------------
// Polyline
//-------------
Map.prototype.addPolyline = function(polylineOptions, callback) {
    var self = this;
    polylineOptions.points = polylineOptions.points || [];
    polylineOptions.color = common.HTMLColor2RGBA(polylineOptions.color || "#FF000080", 0.75);
    polylineOptions.width = polylineOptions.width || 10;
    polylineOptions.visible = polylineOptions.visible === undefined ? true : polylineOptions.visible;
    polylineOptions.zIndex = polylineOptions.zIndex || 4;
    polylineOptions.geodesic = polylineOptions.geodesic  === true;

    cordova.exec(function(result) {
        var polyline = new Polyline(self, result.id, polylineOptions);
        self.OVERLAYS[result.id] = polyline;
        if (typeof callback === "function") {
            callback.call(self, polyline, self);
        }
    }, self.errorHandler, self.id, 'loadPlugin', ['Polyline', self.deleteFromObject(polylineOptions,'function')]);
};

//-------------
// Circle
//-------------
Map.prototype.addCircle = function(circleOptions, callback) {
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
        self.OVERLAYS[result.id] = circle;
        if (typeof circleOptions.onClick === "function") {
            circle.on(event.OVERLAY_CLICK, circleOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, circle, self);
        }
    }, self.errorHandler, self.id, 'loadPlugin', ['Circle', self.deleteFromObject(circleOptions,'function')]);
};

//-------------
// Marker
//-------------
Map.prototype.addMarker = function(markerOptions, callback) {
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

    var markerClick = markerOptions.markerClick;
    var infoClick = markerOptions.infoClick;

console.log("mapId = " + self.id);
    cordova.exec(function(result) {
        markerOptions.hashCode = result.hashCode;
        var marker = new Marker(self, result.id, markerOptions);

        self.MARKERS[result.id] = marker;
        self.OVERLAYS[result.id] = marker;

        if (typeof markerClick === "function") {
            marker.on(event.MARKER_CLICK, markerClick);
        }
        if (typeof infoClick === "function") {
            marker.on(event.INFO_CLICK, infoClick);
        }
        if (typeof callback === "function") {
            callback.call(self, marker, self);
        }
    }, self.errorHandler, self.id, 'loadPlugin', ['Marker', self.deleteFromObject(markerOptions,'function')]);
};

/*****************************************************************************
 * Callbacks from the native side
 *****************************************************************************/
 Map.prototype._onMapEvent = function(eventName) {
    var args = [eventName];
    for (var i = 1; i < arguments.length; i++) {
        args.push(arguments[i]);
    }
    this.trigger.apply(this, args);
 };

Map.prototype._onMarkerEvent = function(eventName, markerId) {
    var self = this;
    var marker = self.MARKERS[markerId] || null;
    if (marker) {
        marker.trigger(eventName, marker);
    }
};


Map.prototype._onOverlayEvent = function(eventName, hashCode) {
   var self = this;
   var overlay = self.OVERLAYS[hashCode] || null;
   if (overlay) {
       var args = [eventName, overlay];
       for (var i = 2; i < arguments.length; i++) {
           args.push(arguments[i]);
       }
       overlay.trigger.apply(this, args);
   }
};

Map.prototype._onKmlEvent = function(eventName, objectType, kmlLayerId, result, options) {
    var self = this;
    var kmlLayer = self.KML_LAYERS[kmlLayerId] || null;
    if (!kmlLayer) {
      return;
    }
    var args = [eventName];
    if (eventName === "add") {
        var overlay = null;

        switch ((objectType + "").toLowerCase()) {
            case "marker":
                overlay = new Marker(self, result.id, options);
                self.MARKERS[result.id] = overlay;
                args.push({
                    "type": "Marker",
                    "object": overlay
                });
                overlay.on(event.MARKER_CLICK, function() {
                    kmlLayer.trigger(event.OVERLAY_CLICK, overlay, overlay.getPosition());
                });
                break;

            case "polygon":
                overlay = new Polygon(self, result.id, options);
                args.push({
                    "type": "Polygon",
                    "object": overlay
                });

                overlay.on(event.OVERLAY_CLICK, function(latLng) {
                    kmlLayer.trigger(event.OVERLAY_CLICK, overlay, latLng);
                });
                break;

            case "polyline":
                overlay = new Polyline(self, result.id, options);
                args.push({
                    "type": "Polyline",
                    "object": overlay
                });
                overlay.on(event.OVERLAY_CLICK, function(latLng) {
                    kmlLayer.trigger(event.OVERLAY_CLICK, overlay, latLng);
                });
                break;
        }
        if (overlay) {
            self.OVERLAYS[result.id] = overlay;
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
};

Map.prototype._onCameraEvent = function(eventName, params) {
    var cameraPosition = new CameraPosition(params);
    this.trigger(eventName, cameraPosition, this);
};
module.exports = Map;
