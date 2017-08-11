/* global cordova, plugin, CSSPrimitiveValue */
var cordova_exec = require('cordova/exec');
var isSuspended = false;
if (!cordova) {
  document.addEventListener("deviceready", function() {
    isSuspended = true;
    cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
  }, {
    once: true
  });
} else {
  var MAP_CNT = 0;

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
  var Geocoder = require('./Geocoder');
  var ExternalService = require('./ExternalService');
  var Environment = require('./Environment');
  var MapTypeId = require('./MapTypeId');

  var INTERVAL_TIMER = null;
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
    viewportTag.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no, viewport-fit=cover');
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
      setTimeout(arguments.callee, 25);
      return;
    }

    //setTimeout(function() {
      // Webkit redraw mandatory
      // http://stackoverflow.com/a/3485654/697856
      document.body.style.backgroundColor = "rgba(0,0,0,0)";
      //document.body.style.display='none';
      document.body.offsetHeight;
      //document.body.style.display='';
    //}, 0);

    var prevDomPositions = {};
    var prevChildrenCnt = 0;
    var idlingCnt = -1;
    var longIdlingCnt = -1;

    var isChecking = false;
    var cacheDepth = {};
    document.head.appendChild(navDecorBlocker);
    var doNotTraceTags = [
      "svg", "p", "pre", "script", "style"
    ];

    function putHtmlElements() {
      var mapIDs = Object.keys(MAPS);
      if (isChecking) {
        return;
      }
      if (mapIDs.length === 0) {
        cordova_exec(null, null, 'CordovaGoogleMaps', 'clearHtmlElements', []);
        return;
      }
      cordova_exec(null, null, 'CordovaGoogleMaps', 'resumeResizeTimer', []);
      isChecking = true;

      //-------------------------------------------
      // If there is no visible map, stop checking
      //-------------------------------------------
      var visibleMapDivList, i, mapId, map;
      visibleMapList = [];
      for (i = 0; i < mapIDs.length; i++) {
        mapId = mapIDs[i];
        map = MAPS[mapId];
        if (map && map.getVisible() && map.getDiv() && common.shouldWatchByNative(map.getDiv())) {
          visibleMapList.push(mapId);
        }
      }
      if (visibleMapList.length === 0) {
        idlingCnt++;
        if (!isSuspended) {
          cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
          isSuspended = true;
        }
        //if (idlingCnt < 5) {
        //  setTimeout(putHtmlElements, 50);
        //}
        isChecking = false;
        return;
      }
      if (isSuspended) {
        isSuspended = false;
        cordova_exec(null, null, 'CordovaGoogleMaps', 'resume', []);
      }

      //-------------------------------------------
      // Should the plugin update the map positions?
      //-------------------------------------------
      var domPositions = {};
      var shouldUpdate = false;
      var doNotTrace = false;

      var traceDomTree = function(element, domIdx, parentRect) {
        doNotTrace = false;

        if (common.shouldWatchByNative(element)) {

          // Generates a __pluginDomId
          var elemId = element.getAttribute("__pluginDomId");
          if (!elemId) {
              elemId = "pgm" + Math.floor(Math.random() * Date.now());
              element.setAttribute("__pluginDomId", elemId);
          }

          // get dom depth
          var depth;
          var zIndex = common.getZIndex(element);
          if (elemId in cacheDepth &&
              elemId in prevDomPositions &&
              prevDomPositions[elemId].zIndex === zIndex) {
              depth = cacheDepth[elemId];
          } else {
              depth = common.getDomDepth(element, domIdx);
              cacheDepth[elemId] = depth;
          }

          // Calculate dom clickable region
          var rect = common.getDivRect(element);
          rect.right = rect.left + rect.width;
          rect.bottom = rect.top + rect.height;
          rect.overflowX_hidden = common.getStyle(element, "overflow-x") === "hidden";
          rect.overflowY_hidden = common.getStyle(element, "overflow-y") === "hidden";
          if (rect.overflowX_hidden && (rect.left !== parentRect.left || rect.width !== parentRect.width)) {
            if (rect.left < parentRect.left) {
              if (rect.right > parentRect.right) {
                rect.width = parentRect.width;
                rect.left = parentRect.left;
              } else {
                rect.width = rect.width + rect.left - parentRect.left;
                rect.left = parentRect.left;
              }
            } else if (rect.right > parentRect.right) {
              if (rect.left > parentRect.left) {
                rect.width = rect.width + parentRect.right - rect.right;
              } else {
                rect.width = parentRect.width;
              }
            }
            rect.right = rect.left + rect.width;
          }

          if (rect.overflowY_hidden && (rect.top !== parentRect.top || rect.height !== parentRect.height)) {
            if (rect.top < parentRect.top) {
              if (rect.bottom > parentRect.bottom) {
                rect.height = parentRect.height;
                rect.top = parentRect.top;
              } else {
                rect.height = rect.height + rect.top - parentRect.top;
                rect.top = parentRect.top;
              }
            } else if (rect.bottom > parentRect.bottom) {
              if (rect.top > parentRect.top) {
                rect.height = rect.height + parentRect.bottom - rect.bottom;
              } else {
                rect.height = parentRect.height;
              }
            }
            rect.bottom = rect.top + rect.height;
          }
          // Stores dom bounds and depth
          domPositions[elemId] = {
            size: rect,
            depth: depth,
            zIndex: zIndex
          };
          parentRect = rect;
          parentRect.elemId = elemId;

          if (!shouldUpdate) {
            if (elemId in prevDomPositions) {
              if (domPositions[elemId].size.left !== prevDomPositions[elemId].size.left ||
                  domPositions[elemId].size.top !== prevDomPositions[elemId].size.top ||
                  domPositions[elemId].size.width !== prevDomPositions[elemId].size.width ||
                  domPositions[elemId].size.height !== prevDomPositions[elemId].size.height ||
                  domPositions[elemId].depth !== prevDomPositions[elemId].depth) {
                  shouldUpdate = true;
              }
            } else {
              shouldUpdate = true;
            }
          }
        } else {
          if (element.nodeType === Node.ELEMENT_NODE) {
            if (element.hasAttribute("__pluginDomId")) {
                shouldUpdate = true;
                element.removeAttribute("__pluginDomId");
            }
            if (doNotTraceTags.indexOf(element.tagName.toLowerCase()) > -1) {
              doNotTrace = true;
            }
          } else {
            doNotTrace = true;
          }
        }
        if (!doNotTrace && element.nodeType === Node.ELEMENT_NODE) {
          if (element.childNodes.length > 0) {
            var child;
            for (var i = 0; i < element.childNodes.length; i++) {
              child = element.childNodes[i];
              if (child.nodeType !== Node.ELEMENT_NODE ||
                doNotTraceTags.indexOf(child.tagName.toLowerCase()) > -1 ||
                common.getStyle(child, "display") === "none") {
                continue;
              }
              traceDomTree(child, domIdx + i + 1, parentRect);
            }
          }
        }
      };
      var bodyRect = common.getDivRect(document.body);
      bodyRect.right = bodyRect.left + bodyRect.width;
      bodyRect.bottom = bodyRect.top + bodyRect.heihgt;

      traceDomTree(document.body, 0, bodyRect);

      // If some elements has been removed, should update the positions
      var elementCnt = Object.keys(domPositions).length;
      var prevElementCnt = Object.keys(prevDomPositions).length;
      if (elementCnt !== prevElementCnt) {
        shouldUpdate = true;
      }

      if (!shouldUpdate && idlingCnt > -1) {
        idlingCnt++;
        if (idlingCnt === 2) {
          mapIDs.forEach(function(mapId) {
              MAPS[mapId].refreshLayout();
          });
        }
        if (idlingCnt > 2) {
          cordova_exec(null, null, 'CordovaGoogleMaps', 'pauseResizeTimer', []);
        }
        // Stop timer when user does not touch the app and no changes are occurred during 1500ms.
        // (50ms * 5times + 200ms * 5times).
        // This save really the battery life significantly.
        if (idlingCnt < 10) {
          if (idlingCnt === 8) {
            cordova.fireDocumentEvent("ecocheck", {});
          }
          setTimeout(putHtmlElements, idlingCnt < 5 ? 50 : 200);
        }
        isChecking = false;
        return;
      }
      idlingCnt = 0;
      longIdlingCnt = 0;

      // If the map div is not displayed (such as display='none'),
      // ignore the map temporally.
      mapIDs.forEach(function(mapId) {
        var div = MAPS[mapId].getDiv();
        if (div) {
          var elemId = div.getAttribute("__pluginDomId");
          if (elemId && !(elemId in domPositions)) {

            // Is the map div removed?
            if (window.document.querySelector) {
              var ele = document.querySelector("[__pluginDomId='" + elemId + "']");
              if (!ele) {
                // If no div element, remove the map.
                MAPS[mapId].remove();
              }
            }

            domPositions[elemId] = {
              size: {
                top: 10000,
                left: 0,
                width: 100,
                height: 100
              },
              depth: 0
            };
          }
        }
      });

      cordova_exec(function() {
        prevDomPositions = domPositions;
        mapIDs.forEach(function(mapId) {
            if (mapId in MAPS) {
                MAPS[mapId].refreshLayout();
            }
        });
        setTimeout(putHtmlElements, 50);
        isChecking = false;
      }, null, 'CordovaGoogleMaps', 'putHtmlElements', [domPositions]);
      child = null;
      parentNode = null;
      elemId = null;
      children = null;
    }

    // This is the special event that is fired by the google maps plugin
    // (Not generic plugin)
    function resetTimer() {
      idlingCnt = -1;
      longIdlingCnt = -1;
      cacheDepth = {};
      cacheZIndex = {};
      putHtmlElements();
    }

    var intervalTimer = null;
    document.addEventListener("ecocheck", function() {
      if (intervalTimer || idlingCnt < 8) {
        return;
      }

      // In order to detect the DOM nodes that are inserted very later,
      // monitoring HTML elements every 1 sec.
      intervalTimer = setInterval(function() {
        if (idlingCnt > 8) {
          idlingCnt = 9;
          // If no update in 10 sec,
          // the plugin belives no more update.
          if (longIdlingCnt < 10) {
            longIdlingCnt++;
            putHtmlElements();
          } else {
            clearInterval(intervalTimer);
            cordova_exec(null, null, 'CordovaGoogleMaps', 'pauseResizeTimer', []);
            intervalTimer = null;
          }
        }
      }, 1000);
    });

    document.addEventListener("deviceready", putHtmlElements, {
      once: true
    });
    document.addEventListener("plugin_touch", resetTimer);
    window.addEventListener("orientationchange", resetTimer);

    function onBackButton() {
      // Request stop all tasks.
      _stopRequested = true;
      if (_isWaitMethod && _executingCnt > 0) {
        // Wait until all tasks currently running are stopped.
        setTimeout(arguments.callee, 100);
        return;
      }
      // Executes the browser back history action
      // Since the cordova can not exit from app sometimes, handle the backbutton action in CordovaGoogleMaps
      cordova_exec(null, null, 'CordovaGoogleMaps', "backHistory", []);

      if (cordova) {
        resetTimer();
        // For other plugins, fire the `plugin_buckbutton` event instead of the `backbutton` evnet.
        cordova.fireDocumentEvent('plugin_backbutton', {});
      }
    }
    document.addEventListener("backbutton", onBackButton, false);

  }());

  /*****************************************************************************
   * Name space
   *****************************************************************************/
  module.exports = {
    event: event,
    Animation: {
        BOUNCE: 'BOUNCE',
        DROP: 'DROP'
    },

    BaseClass: BaseClass,
    BaseArrayClass: BaseArrayClass,
    Map: {
        getMap: function(div, mapOptions) {
            var mapId;
            if (common.isDom(div)) {
              mapId = div.getAttribute("__pluginMapId");
              if (!mapOptions || mapOptions.visible !== false) {
                // Add gray color until the map is displayed.
                div.style.backgroundColor = "rgba(255, 30, 30, 0.5);";
              }
            }
            if (mapId in MAPS) {
              //--------------------------------------------------
              // Backward compatibility for v1
              //
              // If the div is already recognized as map div,
              // return the map instance
              //--------------------------------------------------
              return MAPS[mapId];
            } else {
              mapId = "map_" + MAP_CNT + "_" + saltHash;
            }
            if (common.isDom(div)) {
              div.setAttribute("__pluginMapId", mapId);
            }

            var map = new Map(mapId, execCmd);

            // Catch all events for this map instance, then pass to the instance.
            document.addEventListener(mapId, nativeCallback.bind(map));
            /*
                    map.showDialog = function() {
                      showDialog(mapId).bind(map);
                    };
            */
            map.one('remove', function() {
                document.removeEventListener(mapId, nativeCallback);
                MAPS[mapId].destroy();
                delete MAPS[mapId];
                map = undefined;
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
    HtmlInfoWindow: HtmlInfoWindow,
    LatLng: LatLng,
    LatLngBounds: LatLngBounds,
    Marker: Marker,
    MapTypeId: MapTypeId,
    external: ExternalService,
    environment: Environment,
    Geocoder: Geocoder,
    geometry: {
        encoding: encoding,
        spherical: spherical
    }
  };

  cordova.addConstructor(function() {
      if (!window.Cordova) {
          window.Cordova = cordova;
      }
      window.plugin = window.plugin || {};
      window.plugin.google = window.plugin.google || {};
      window.plugin.google.maps = window.plugin.google.maps || module.exports;
      document.addEventListener("deviceready", function() {
          // workaround for issue on android-19: Cannot read property 'maps' of undefined
          if (!window.plugin) { console.warn('re-init window.plugin'); window.plugin = window.plugin || {}; }
          if (!window.plugin.google) { console.warn('re-init window.plugin.google'); window.plugin.google = window.plugin.google || {}; }
          if (!window.plugin.google.maps) { console.warn('re-init window.plugin.google.maps'); window.plugin.google.maps = window.plugin.google.maps || module.exports; }

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
        setTimeout(function() {
          success.apply(self,results);
        }, 0);
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
        _exec();
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
        setTimeout(function() {
          error.apply(self,results);
        }, 0);
      }

      if (methodName === _isWaitMethod) {
        _isWaitMethod = null;
      }
      _executingCnt--;
      _exec();
    }, pluginName, methodName, args]
  });

  //console.log("commandQueue.length: " + commandQueue.length);
  if (_isExecuting || _executingCnt >= MAX_EXECUTE_CNT || commandQueue.length > 1) {
    return;
  }
  _exec();
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
    if (methodName === "resizeMap") {
      if (_isResizeMapExecuting) {
        _executingCnt--;
        continue;
      }
      _isResizeMapExecuting = true;
    }
    if (_stopRequested && (methodName !== "remove" || methodName !== "clear")) {
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
