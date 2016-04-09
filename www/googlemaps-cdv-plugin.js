/* global cordova, plugin, CSSPrimitiveValue */
var MARKERS = {};
var KML_LAYERS = {};
var OVERLAYS = {};

var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    event = require('./event'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    Map = require('./Map'),
    LatLng = require('./LatLng'),
    LatLngBounds = require('./LatLngBounds'),
    Location = require('./Location'),
    CameraPosition = require('./CameraPosition'),
    Marker = require('./Marker'),
    Circle = require('./Circle'),
    Polyline = require('./Polyline'),
    Polygon = require('./Polygon'),
    TileOverlay = require('./TileOverlay'),
    GroundOverlay = require('./GroundOverlay'),
    encoding = require('./encoding'),
    Geocoder = require('./Geocoder'),
    ExternalService = require('./ExternalService'),
    KmlOverlay = require('./KmlOverlay'),
    MapTypeId = require('./MapTypeId');


/*****************************************************************************
 * KmlOverlay class method
*****************************************************************************/

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
 * Map class methods
 *****************************************************************************/
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

    cordova.exec(function(result) {
        markerOptions.hashCode = result.hashCode;
        var marker = new Marker(self, result.id, markerOptions);

        MARKERS[result.id] = marker;
        OVERLAYS[result.id] = marker;

        if (typeof markerClick === "function") {
            marker.on(event.MARKER_CLICK, markerClick);
        }
        if (typeof infoClick === "function") {
            marker.on(event.INFO_CLICK, infoClick);
        }
        if (typeof callback === "function") {
            callback.call(self, marker, self);
        }
    }, self.errorHandler, 'Marker', 'create', [self.deleteFromObject(markerOptions,'function')]);
};

Marker.prototype.remove = function(callback) {
    var self = this;
    self.set("keepWatching", false);
    delete MARKERS[this.id];
    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, self.errorHandler, 'Marker', 'remove', [this.getId()]);
    self.off();
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
        OVERLAYS[result.id] = circle;
        if (typeof circleOptions.onClick === "function") {
            circle.on(event.OVERLAY_CLICK, circleOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, circle, self);
        }
    }, self.errorHandler, 'Circle', 'create', [self.deleteFromObject(circleOptions,'function')]);
};
Circle.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
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
        OVERLAYS[result.id] = polyline;
        if (typeof callback === "function") {
            callback.call(self, polyline, self);
        }
    }, self.errorHandler, 'Polyline', 'create', [self.deleteFromObject(polylineOptions,'function')]);
};

Polyline.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
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
        OVERLAYS[result.id] = polygon;
        if (typeof polygonOptions.onClick === "function") {
            polygon.on(event.OVERLAY_CLICK, polygonOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, polygon, self);
        }
    }, self.errorHandler, "Polygon", 'create', [self.deleteFromObject(polygonOptions,'function')]);
};

Polygon.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
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
        OVERLAYS[result.id] = tileOverlay;
        if (typeof callback === "function") {
            callback.call(self, tileOverlay, self);
        }
    }, self.errorHandler, 'TileOverlay', 'create', [self.deleteFromObject(tilelayerOptions,'function')]);
};
TileOverlay.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
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
        OVERLAYS[result.id] = groundOverlay;
        if (typeof groundOverlayOptions.onClick === "function") {
            groundOverlay.on(event.OVERLAY_CLICK, groundOverlayOptions.onClick);
        }
        if (typeof callback === "function") {
            callback.call(self, groundOverlay, self);
        }
    }, self.errorHandler, 'GroundOverlay', 'create', [self.deleteFromObject(groundOverlayOptions,'function')]);

};

GroundOverlay.prototype.remove = function() {
    delete OVERLAYS[result.id];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
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
    OVERLAYS[kmlId] = kmlOverlay;
    KML_LAYERS[kmlId] = kmlOverlay;

    cordova.exec(function(kmlId) {
        if (typeof callback === "function") {
            callback.call(self, kmlOverlay, self);
        }
    }, self.errorHandler, 'KmlOverlay', 'create', [self.deleteFromObject(kmlOverlayOptions,'function')]);

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

    clearObj(OVERLAYS);
    clearObj(MARKERS);
    clearObj(KML_LAYERS);

    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self);
        }
    }, self.errorHandler, 'GoogleMaps', 'clear', []);
};

/*****************************************************************************
 * Private functions
 *****************************************************************************/

 /*
  * Callback from Native
  */

 Map.prototype._onMarkerEvent = function(eventName, hashCode) {
     var marker = MARKERS[hashCode] || null;
     if (marker) {
         marker.trigger(eventName, marker);
     }
 };

 Map.prototype._onOverlayEvent = function(eventName, hashCode) {
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
Map.prototype._onKmlEvent = function(eventName, objectType, kmlLayerId, result, options) {
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
Map.prototype._onMapEvent = function(eventName) {
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
Map.prototype._onMyLocationChange = function(params) {
    var location = new Location(params);
    this.trigger('my_location_change', location, this);
};
/**
 * Callback from Native
 */
Map.prototype._onCameraEvent = function(eventName, params) {
    var cameraPosition = new CameraPosition(params);
    this.trigger(eventName, cameraPosition, this);
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
        cordova.exec(null, self.errorHandler, 'GoogleMaps', 'setDiv', []);
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
        //alert(JSON.stringify(common.getDivRect(div), null, 4));
        cordova.exec(null, null, 'GoogleMaps', 'resizeMap', args);
    }

}


/**
 * @desc Recalculate the position of HTML elements
 */
Map.prototype.refreshLayout = function() {
    onMapResize(undefined, false);
};


function onBackbutton() {
    _mapInstance.closeDialog();
}

/**
 * @desc Open the map dialog
 */
Map.prototype.showDialog = function() {
    document.addEventListener("backbutton", onBackbutton, false);
    cordova.exec(null, this.errorHandler, 'GoogleMaps', 'showDialog', []);
};

/**
 * @desc Close the map dialog
 */
Map.prototype.closeDialog = function() {
    document.removeEventListener("backbutton", onBackbutton, false);
    cordova.exec(null, this.errorHandler, 'GoogleMaps', 'closeDialog', []);
};

/*****************************************************************************
 * Watch dog timer for child elements
 *****************************************************************************/
var _mapInstance = new Map();

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
            children = common.getAllChildren(div);
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
 * Name space
 *****************************************************************************/
module.exports = {
    event: event,
    Animation: {
        BOUNCE: 'BOUNCE',
        DROP: 'DROP'
    },

    //BaseClass: BaseClass,
    Map: _mapInstance,
    LatLng: LatLng,
    LatLngBounds: LatLngBounds,
    Marker: Marker,
    MapTypeId: MapTypeId,
    external: ExternalService,
    Geocoder: Geocoder,
    geometry: {
        encoding: encoding
    }
};

window.addEventListener("orientationchange", function() {
    setTimeout(onMapResize, 1000);
});

document.addEventListener("deviceready", function() {
    document.removeEventListener("deviceready", arguments.callee);
    plugin.google.maps.Map.isAvailable();
});
