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
var Environment = require('./Environment');
var MapTypeId = require('./MapTypeId');

var _global = new BaseClass();
var MAPS = {};
var saltHash = Math.floor(Math.random() * Date.now());

/*****************************************************************************
 * To prevent strange things happen,
 * disable the changing of viewport zoom level by double clicking.
 * This code has to run before the device ready event.
 *****************************************************************************/
(function() {
    var viewportTag = null;
    var metaTags = document.getElementsByTagName('meta');
    for (var i = 0; i < metaTags.length; i++) {
        if (metaTags[i].getAttribute('name') === "viewport") {
            viewportTag = metaTags[i];
            break;
        }
    }
    if (!viewportTag) {
        viewportTag = document.createElement("meta");
        viewportTag.setAttribute('name', 'viewport');
    }
    viewportTag.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no');
})();

/*****************************************************************************
 * Add event lister to all html nodes under the <body> tag.
 *****************************************************************************/
(function() {
    var prevDomPositions = {};

    function putHtmlElements() {
        var children = common.getAllChildren(document.body);
        if (children.length === 0) {
            children = null;
            return;
        }

        var shouldUpdate = false;
        var domPositions = {};
        var size, elemId, i, child, parentNode;


        children.unshift(document.body);
        for (i = 0; i < children.length; i++) {
            child = children[i];
            elemId = child.getAttribute("__pluginDomId");
            if (!elemId) {
                elemId = "pgm" + Math.floor(Math.random() * Date.now());
                child.setAttribute("__pluginDomId", elemId);
            }
            domPositions[elemId] = common.getDomInfo(child);
            if (!shouldUpdate) {
                if (elemId in prevDomPositions) {
                    if (domPositions[elemId].size.left !== prevDomPositions[elemId].size.left ||
                        domPositions[elemId].size.top !== prevDomPositions[elemId].size.top ||
                        domPositions[elemId].size.width !== prevDomPositions[elemId].size.width ||
                        domPositions[elemId].size.height !== prevDomPositions[elemId].size.height) {
                        shouldUpdate = true;
                    }
                } else {
                    shouldUpdate = true;
                }
            }
        }
        if (!shouldUpdate) {
            return;
        }


        for (i = 0; i < children.length; i++) {
            child = children[i];
            parentNode = child.parentNode;

            domPositions[elemId].parentNode = parentNode.getAttribute("__pluginDomId");
        }
        cordova.exec(function() {
            prevDomPositions = domPositions;
        }, null, 'GoogleMaps', 'putHtmlElements', [domPositions]);
        child = null;
        parentNode = null;
        elemId = null;
        children = null;
    }
    setInterval(putHtmlElements, 50);
}());

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

function onMapResize(event) {
    //console.log("---> onMapResize");
    var mapIDs = Object.keys(MAPS);
    mapIDs.forEach(function(mapId) {
        MAPS[mapId].refreshLayout();
    });
}

function nativeCallback(params) {
    var args = params.args || [];
    args.unshift(params.evtName);
    this[params.callback].apply(this, args);
}

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
            var mapId = "map_" + MAP_CNT + "_" + saltHash;
            var map = new Map(mapId);

            // Catch all events for this map instance, then pass to the instance.
            document.addEventListener(mapId, nativeCallback.bind(map));
            /*
                    map.showDialog = function() {
                      showDialog(mapId).bind(map);
                    };
            */
            map.one('remove', function() {
                document.removeEventListener(mapId, nativeCallback);
                MAPS[mapId].clear();
                delete MAPS[mapId];
                map = null;
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
    environment: Environment,
    Geocoder: Geocoder,
    geometry: {
        encoding: encoding
    }
};

// for Android
window.addEventListener("beforeunload", function() {
    cordova.exec(null, null, 'GoogleMaps', 'unload', ['']);
});

// for iOS
window.addEventListener("pagehide", function() {
    cordova.exec(null, null, 'GoogleMaps', 'unload', ['']);
});

window.addEventListener("orientationchange", function() {
    //console.log("---> orientationchange");
    setTimeout(onMapResize, 1000);
});

document.addEventListener("deviceready", function() {
    document.removeEventListener("deviceready", arguments.callee);

    //------------------------------------------------------------------------
    // If Google Maps Android API v2 is not available,
    // display the warning alert.
    //------------------------------------------------------------------------
    cordova.exec(null, function(message) {
        alert(message);
    }, 'Environment', 'isAvailable', ['']);
});
