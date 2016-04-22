/* global cordova, plugin, CSSPrimitiveValue */
var MAP_CNT = 0;

var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    event = require('./event'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

var Map = require('./Map');
var LatLng = require('./LatLng');
var LatLngBounds = require('./LatLngBounds');
var Location = require('./Location');
var CameraPosition = require('./CameraPosition');
var Marker = require('./Marker');
var Circle = require('./Circle');
var Polyline = require('./Polyline');
var Polygon = require('./Polygon');
var TileOverlay = require('./TileOverlay');
var GroundOverlay = require('./GroundOverlay');
var KmlOverlay = require('./KmlOverlay');
var encoding = require('./encoding');
var Geocoder = require('./Geocoder');
var ExternalService = require('./ExternalService');
var MapTypeId = require('./MapTypeId');

var _global = new BaseClass();
var MAPS = {};

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

Circle.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
};
Polyline.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
};
Polygon.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
};

TileOverlay.prototype.remove = function() {
    delete OVERLAYS[this.getId()];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
};

GroundOverlay.prototype.remove = function() {
    delete OVERLAYS[result.id];
    cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'remove', [this.getId()]);
    this.off();
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
    var mapIDs = Object.keys(MAPS);
    mapIDs.forEach(function(mapId) {
      mapIDs[mapId].refreshLayout();
    });
}





function onBackbutton() {
    closeDialog();
}

/**
 * @desc Open the map dialog
 */
 function showDialog(mapId) {
     document.addEventListener("backbutton", onBackbutton, false);
     document.addEventListener('map_close', function() {
         document.removeEventListener("backbutton", onBackbutton, false);
         document.removeEventListener('map_close', arguments.callee, false);
     }, false);
     cordova.exec(null, this.errorHandler, 'GoogleMaps', 'showDialog', [mapId]);
 }

/**
 * @desc Close the map dialog
 */
function closeDialog() {
    cordova.exec(null, this.errorHandler, 'GoogleMaps', 'closeDialog', []);
};

/*****************************************************************************
 * Watch dog timer for child elements
 *****************************************************************************/

window._watchDogTimer = null;
_global.addEventListener("keepWatching_changed", function(oldValue, newValue) {
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
        }, _global.getWatchDogTimer());
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

_global.addEventListener("keepWatching_changed", function(oldValue, newValue) {
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
    Map: {
      getMap: function() {
        var mapId = "map_" + MAP_CNT;
        var map = new Map(mapId);
        map.showDialog = function() {
          showDialog(mapId).bind(map);
        };
        map.one('remove', function() {
          delete MAPS[mapId];
        });
        MAP_CNT++;
        MAPS[mapId] = map;
        var args = [mapId];
        for (var i = 0; i < arguments.length; i++) {
          args.push(arguments[i]);
        }
        map.getMap.apply(map, args);
        return map;
      }
    },
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
    cordova.exec(null, function(message) {
        alert(error);
    }, 'GoogleMaps', 'isAvailable', ['']);
});
