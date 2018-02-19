/* global cordova, plugin, CSSPrimitiveValue */
var cordova_exec = require('cordova/exec');
if (typeof Array.prototype.forEach !== "function") {
  (function() {
    Array.prototype.forEach = function(fn, thisArg) {
      thisArg = thisArg || this;
      for (var i = 0; i < this.length; i++) {
        fn.call(thisArg, this[i], i, this);
      }
    };
  })();
}
if (typeof Array.prototype.filter !== "function") {
  (function() {
    Array.prototype.filter = function(fn, thisArg) {
      thisArg = thisArg || this;
      var results = [];
      for (var i = 0; i < this.length; i++) {
        if (fn.call(thisArg, this[i], i, this) === true) {
          results.push(this[i]);
        }
      }
      return results;
    };
  })();
}
if (typeof Array.prototype.map !== "function") {
  (function() {
    Array.prototype.map = function(fn, thisArg) {
      thisArg = thisArg || this;
      var results = [];
      for (var i = 0; i < this.length; i++) {
        results.push(fn.call(thisArg, this[i], i, this));
      }
      return results;
    };
  })();
}

if (!cordova) {
  document.addEventListener("deviceready", function() {
    cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
  }, {
    once: true
  });
} else {

  var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    event = require('./event'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    BaseArrayClass = require('./BaseArrayClass');

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
  var HtmlInfoWindow = require('./HtmlInfoWindow');
  var KmlOverlay = require('./KmlOverlay');
  var encoding = require('./encoding');
  var spherical = require('./spherical');
  var poly = require('./poly');
  var Geocoder = require('./Geocoder');
  var LocationService = require('./LocationService');
  var Environment = require('./Environment');
  var MapTypeId = require('./MapTypeId');

  var MAPS = {};

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

    var viewportTagContent = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no';

    // Detect if iOS device
    if (/(iPhone|iPod|iPad)/i.test(window.navigator.userAgent)) {
      // Get iOS major version
      var iosVersion = parseInt((window.navigator.userAgent).match(/OS (\d+)_(\d+)_?(\d+)? like Mac OS X/i)[1]);
      // Detect if device is running >iOS 11
      // iOS 11's UIWebView and WKWebView changes the viewport behaviour to render viewport without the status bar. Need to override with "viewport-fit: cover" to include the status bar.
      if (iosVersion >= 11) {
        viewportTagContent += ', viewport-fit=cover';
      }
    }

    // Update viewport tag attribute
    viewportTag.setAttribute('content', viewportTagContent);
  })();

  /*****************************************************************************
   * Prevent background, background-color, background-image properties
   *****************************************************************************/
  var navDecorBlocker = document.createElement("style");
  navDecorBlocker.setAttribute("type", "text/css");
  navDecorBlocker.innerText = [
    "html, body, ._gmaps_cdv_ {",
    "   background-image: url() !important;",
    "   background: rgba(0,0,0,0) url() !important;",
    "   background-color: rgba(0,0,0,0) !important;",
    "}",
    "._gmaps_cdv_ .nav-decor {",
    "   background-color: rgba(0,0,0,0) !important;",
    "   background: rgba(0,0,0,0) !important;",
    "   display:none !important;",
    "}"
  ].join("");
  document.head.appendChild(navDecorBlocker);

  /*****************************************************************************
   * Add event lister to all html nodes under the <body> tag.
   *****************************************************************************/
  (function() {
    if (!document.body || !document.body.firstChild) {
      common.nextTick(arguments.callee, 25);
      return;
    }

    document.body.style.backgroundColor = "rgba(0,0,0,0)";
    document.body.offsetHeight;

    document.head.appendChild(navDecorBlocker);

    //----------------------------------------------
    // Send the DOM hierarchy to native side
    //----------------------------------------------
    var domPositionsByMap = {};

    function traceDomTree(mapId, element, elemId) {

      // Get the z-index CSS
      var zIndex = common.getZIndex(element);

      // Calculate dom clickable region
      var rect = common.getDivRect(element);

      // Stores dom information
      domPositionsByMap[mapId][elemId] = {
        pointerEvents: common.getStyle(element, 'pointer-events'),
        isMap: element.hasAttribute("__pluginMapId"),
        size: rect,
        zIndex: zIndex,
        overflowX: common.getStyle(element, "overflow-x"),
        overflowY: common.getStyle(element, "overflow-y"),
        children: [],
        containMapIDs: {}
      };

      var child;
      for (var i = 0; i < element.children.length; i++) {
        child = element.children[i];
        if (!common.shouldWatchByNative(child)) {
          continue;
        }

        var childId = common.getPluginDomId(child);
        domPositionsByMap[mapId][elemId].children.push(childId);
        traceDomTree(mapId, child, childId);
      }
    }

    function buildAllDomPositions() {
      var domPositions = {};
      Object.keys(domPositionsByMap).forEach(function(mapId) {
        Object.assign(domPositions, domPositionsByMap[mapId]);
      });
      return domPositions;
    }

    //----------------------------------------------------
    // Stop all executions if the page will be closed.
    //----------------------------------------------------
    function stopExecution() {
      // Request stop all tasks.
      _stopRequested = true;
    }
    window.addEventListener("unload", stopExecution);

    /*****************************************************************************
     * Name space
     *****************************************************************************/
    var singletonLocationService = new LocationService(execCmd);
    module.exports = {
      event: event,
      Animation: {
        BOUNCE: 'BOUNCE',
        DROP: 'DROP'
      },

      BaseClass: BaseClass,
      BaseArrayClass: BaseArrayClass,
      Map: {
        // removeMap: function (mapId, callback) {
        //   domPositionsByMap[mapId] = {};
        //   var map = MAPS[mapId];
        //   delete MAPS[mapId];
        //   return map.remove(callback);
        // },
        updateDomPositions: function(mapDiv) {
          var mapId = mapDiv.getAttribute('__pluginMapId');
          var mapElemId = common.getPluginDomId(mapDiv);

          domPositionsByMap[mapId] = {};

          traceDomTree(mapId, mapDiv, mapElemId);

          // Send updated domPositions to native
          cordova_exec(function() {
            // Success callback
          }, function(error) {
            // Error callback
          }, 'CordovaGoogleMaps', 'putHtmlElements', [buildAllDomPositions()]);
        },
        getMap: function(div, mapOptions) {

          var mapId = div.getAttribute('id') + Math.random();
          div.setAttribute("__pluginMapId", mapId);
          var map = new Map(mapId, execCmd);

          MAPS[mapId] = map;

          // Catch all events for this map instance, then pass to the instance.
          document.addEventListener(mapId, nativeCallback.bind(map));

          map.one('remove', function() {
            document.removeEventListener(mapId, nativeCallback);

            var div = map.getDiv();
            if (!div) {
              div = document.querySelector("[__pluginMapId='" + mapId + "']");
            }
            if (div) {
              div.removeAttribute('__pluginMapId');
            }

            delete domPositionsByMap[mapId];

            MAPS[mapId].destroy();
            delete MAPS[mapId];
            map = undefined;

            if ((Object.keys(MAPS)).length === 0) {
              common._clearInternalCache();
              cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
            }
          });

          var args = [mapId];
          for (var i = 0; i < arguments.length; i++) {
            args.push(arguments[i]);
          }

          var elemId = common.getPluginDomId(div);

          domPositionsByMap[mapId] = {};
          domPositionsByMap[mapId][elemId] = {
            pointerEvents: common.getStyle(div, 'pointer-events'),
            isMap: true,
            size: common.getDivRect(div),
            zIndex: common.getZIndex(div),
            children: [],
            overflowX: common.getStyle(div, "overflow-x"),
            overflowY: common.getStyle(div, "overflow-y"),
            containMapIDs: {
              elemId: 1
            }
          };


          cordova_exec(function() {
            cordova_exec(function() {
              map.getMap.apply(map, args);
            }, null, 'CordovaGoogleMaps', 'putHtmlElements', [buildAllDomPositions()]);
          }, null, 'CordovaGoogleMaps', 'resume', []);

          return map;
        }
      },
      HtmlInfoWindow: HtmlInfoWindow,
      LatLng: LatLng,
      LatLngBounds: LatLngBounds,
      Marker: Marker,
      MapTypeId: MapTypeId,
      environment: Environment,
      Geocoder: Geocoder,
      LocationService: singletonLocationService,
      geometry: {
        encoding: encoding,
        spherical: spherical,
        poly: poly
      }
    };

  }());

  cordova.addConstructor(function() {
    if (!window.Cordova) {
      window.Cordova = cordova;
    }
    window.plugin = window.plugin || {};
    window.plugin.google = window.plugin.google || {};
    window.plugin.google.maps = window.plugin.google.maps || module.exports;
    document.addEventListener("deviceready", function() {
      // workaround for issue on android-19: Cannot read property 'maps' of undefined
      if (!window.plugin) {
        console.warn('re-init window.plugin');
        window.plugin = window.plugin || {};
      }
      if (!window.plugin.google) {
        console.warn('re-init window.plugin.google');
        window.plugin.google = window.plugin.google || {};
      }
      if (!window.plugin.google.maps) {
        console.warn('re-init window.plugin.google.maps');
        window.plugin.google.maps = window.plugin.google.maps || module.exports;
      }

      // Check the Google Maps Android API v2 if the device platform is Android.
      if (/Android/i.test(window.navigator.userAgent)) {
        //------------------------------------------------------------------------
        // If Google Maps Android API v2 is not available,
        // display the warning alert.
        //------------------------------------------------------------------------
        cordova.exec(null, function(message) {
          alert(message);
        }, 'Environment', 'isAvailable', ['']);
      }
    }, {
      once: true
    });
  });
}

/*****************************************************************************
 * Private functions
 *****************************************************************************/

function nativeCallback(params) {
  var args = params.args || [];
  args.unshift(params.evtName);
  this[params.callback].apply(this, args);
}

/*****************************************************************************
 * Command queue mechanism
 * (Save the number of method executing at the same time)
 *****************************************************************************/
var commandQueue = [];
var _isWaitMethod = null;
var _isExecuting = false;
var _executingCnt = 0;
var MAX_EXECUTE_CNT = 10;
var _lastGetMapExecuted = 0;
var _isResizeMapExecuting = false;
var _stopRequested = false;

function execCmd(success, error, pluginName, methodName, args, execOptions) {
  execOptions = execOptions || {};
  if (this._isRemoved && !execOptions.remove) {
    // Ignore if the instance is already removed.
    console.error("[ignore]" + pluginName + "." + methodName + ", because removed.");
    return true;
  }
  if (!this._isReady) {
    // Ignore if the instance is not ready.
    console.error("[ignore]" + pluginName + "." + methodName + ", because it's not ready.");
    return true;
  }
  var self = this;
  commandQueue.push({
    "execOptions": execOptions,
    "args": [function() {
      //console.log("success: " + methodName);
      if (methodName === "resizeMap") {
        _isResizeMapExecuting = false;
      }
      if (!_stopRequested && success) {
        var results = [];
        for (var i = 0; i < arguments.length; i++) {
          results.push(arguments[i]);
        }
        common.nextTick(function() {
          success.apply(self, results);
        });
      }

      var delay = 0;
      if (methodName === _isWaitMethod) {
        // Prevent device crash when the map.getMap() executes multiple time in short period
        if (_isWaitMethod === "getMap" && Date.now() - _lastGetMapExecuted < 1500) {
          delay = 1500;
        }
        _lastGetMapExecuted = Date.now();
        _isWaitMethod = null;
      }
      setTimeout(function() {
        _executingCnt--;
        common.nextTick(_exec);
      }, delay);
    }, function() {
      //console.log("error: " + methodName);
      if (methodName === "resizeMap") {
        _isResizeMapExecuting = false;
      }
      if (!_stopRequested && error) {
        var results = [];
        for (var i = 0; i < arguments.length; i++) {
          results.push(arguments[i]);
        }
        common.nextTick(function() {
          error.apply(self, results);
        });
      }

      if (methodName === _isWaitMethod) {
        _isWaitMethod = null;
      }
      _executingCnt--;
      common.nextTick(_exec);
    }, pluginName, methodName, args]
  });

  //console.log("commandQueue.length: " + commandQueue.length, commandQueue);
  if (_isExecuting || _executingCnt >= MAX_EXECUTE_CNT) {
    return;
  }
  common.nextTick(_exec);
}

function _exec() {
  //console.log("commandQueue.length: " + commandQueue.length);
  if (_isExecuting || _executingCnt >= MAX_EXECUTE_CNT || _isWaitMethod || commandQueue.length === 0) {
    return;
  }
  _isExecuting = true;

  var methodName;
  while (commandQueue.length > 0 && _executingCnt < MAX_EXECUTE_CNT) {
    if (!_stopRequested) {
      _executingCnt++;
    }
    var commandParams = commandQueue.shift();
    methodName = commandParams.args[3];
    //console.log("target: " + methodName);
    if (methodName === "resizeMap") {
      if (_isResizeMapExecuting) {
        _executingCnt--;
        continue;
      }
      _isResizeMapExecuting = true;
    }
    if (_stopRequested && (!commandParams.execOptions.remove || methodName !== "clear")) {
      _executingCnt--;
      continue;
    }
    //console.log("start: " + methodName);
    if (commandParams.execOptions.sync) {
      _isWaitMethod = methodName;
      cordova_exec.apply(this, commandParams.args);
      break;
    }
    cordova_exec.apply(this, commandParams.args);
  }
  //console.log("commandQueue.length: " + commandQueue.length);
  _isExecuting = false;

}
