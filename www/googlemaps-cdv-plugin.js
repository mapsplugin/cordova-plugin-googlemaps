/* global cordova, plugin, CSSPrimitiveValue */
var cordova_exec = require('cordova/exec');
var isSuspended = false;
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
  var poly = require('./poly');
  var Geocoder = require('./Geocoder');
  var Geolocation = require('./Geolocation');
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

    //setTimeout(function() {
      // Webkit redraw mandatory
      // http://stackoverflow.com/a/3485654/697856
      document.body.style.backgroundColor = "rgba(0,0,0,0)";
      //document.body.style.display='none';
      document.body.offsetHeight;
      //document.body.style.display='';
    //}, 0);

    var isChecking = false;
    document.head.appendChild(navDecorBlocker);
    var doNotTraceTags = [
      "svg", "p", "pre", "script", "style"
    ];

    var followPositionTimer = null;
    var followPositionTimerCnt = 0;
    var prevMapRects = {};
    var scrollEndTimer = null;
    function followMapDivPositionOnly(opts) {
      opts = opts || {};
      var mapRects = {};
      var mapIDs = Object.keys(MAPS);
      var changed = false;
      var mapId, map;
      for (var i = 0; i < mapIDs.length; i++) {
        mapId = mapIDs[i];
        map = MAPS[mapId];
        if (map && map.getVisible() && map.getDiv() && common.shouldWatchByNative(map.getDiv())) {
          var mapDiv = map.getDiv();
          var divId = mapDiv.getAttribute("__pluginDomId");
          mapRects[divId] = {
            size: common.getDivRect(mapDiv),
            zIndex: common.getZIndex(mapDiv)
          };
          if (!changed && prevMapRects && (divId in prevMapRects) && (
            prevMapRects[divId].size.left !== mapRects[divId].size.left ||
            prevMapRects[divId].size.top !== mapRects[divId].size.top ||
            prevMapRects[divId].size.width !== mapRects[divId].size.width ||
            prevMapRects[divId].size.height !== mapRects[divId].size.height ||
            prevMapRects[divId].zIndex !== mapRects[divId].zIndex)) {
            changed = true;
          }
        }
      }
      prevMapRects = mapRects;
      if (changed || opts.force) {
        cordova_exec(null, null, 'CordovaGoogleMaps', 'updateMapPositionOnly', [mapRects]);
      }
    }

    document.body.addEventListener("transitionend", function(e) {
      setTimeout(function() {
        common.nextTick(function() {
          if (e.target.hasAttribute("__pluginDomId")) {
            //console.log("transitionend", e.target.getAttribute("__pluginDomId"));
            var isMapChild = false;
            var ele = e.target;
            while(!isMapChild && ele && ele.nodeType === Node.ELEMENT_NODE) {
              isMapChild = ele.hasAttribute("__pluginMapId");
              ele = ele.parentNode;
            }
            traceDomTree(e.target, e.target.getAttribute("__pluginDomId"), isMapChild);

            isSuspended = true;
            isThereAnyChange = true;
            isChecking = false;
            resetTimer({force: true});
          }
        });
      }, 100);
    }, true);

    document.body.addEventListener("scroll", function(e) {
      if (scrollEndTimer) {
        clearTimeout(scrollEndTimer);
      }
      scrollEndTimer = setTimeout(onScrollEnd, 100);
      followMapDivPositionOnly();
    }, true);
    function onScrollEnd() {
      isThereAnyChange = true;
      common.nextTick(putHtmlElements);
    }

    function removeDomTree(node) {
      if (!node || !node.querySelectorAll) {
        return;
      }
      var elemId, mapId;
      var children = node.querySelectorAll('[__pluginDomId]');
      if (children && children.length > 0) {
        var isRemoved = node._isRemoved;
        var child;
        for (var i = 0; i < children.length; i++) {
          child = children[i];
          elemId = child.getAttribute('__pluginDomId');
          if (isRemoved) {
            child.removeAttribute('__pluginDomId');
            if (child.hasAttribute('__pluginMapId')) {
              // If no div element, remove the map.
              mapId = child.getAttribute('__pluginMapId');
//console.log("---->no map div, elemId = " + elemId + ", mapId = " + mapId);
              if (mapId in MAPS) {
                MAPS[mapId].remove();
              }
            }
            delete domPositions[elemId];
          }
          common._removeCacheById(elemId);
        }
      }
      if (node.hasAttribute("__pluginDomId")) {
        elemId = node.getAttribute('__pluginDomId');
        if (node._isRemoved) {
          node.removeAttribute('__pluginDomId');
          if (node.hasAttribute('__pluginMapId')) {
            // If no div element, remove the map.
            mapId = node.getAttribute('__pluginMapId');
            if (mapId in MAPS) {
//console.log("---> map.remove() = " + elemId);
              MAPS[mapId].remove();
            }
          }
          delete domPositions[elemId];
        }
        common._removeCacheById(elemId);
      }
    }
    //----------------------------------------------
    // Observe styles and children
    //----------------------------------------------
    var isThereAnyChange = true;
    (function() {

      var observer = new MutationObserver(function(mutations) {
        common.nextTick(function() {
          var i, mutation, targetCnt, node, j, elemId;
          for (j = 0; j < mutations.length; j++) {
            mutation = mutations[j];
            targetCnt = 0;
            if (mutation.type === "childList") {
              if (mutation.addedNodes) {
                for (i = 0; i < mutation.addedNodes.length; i++) {
                  node = mutation.addedNodes[i];
                  if (node.nodeType !== Node.ELEMENT_NODE) {
                    continue;
                  }
                  targetCnt++;
                  setDomId(node);
                }
              }
              if (mutation.removedNodes) {
                for (i = 0; i < mutation.removedNodes.length; i++) {
                  node = mutation.removedNodes[i];
                  if (node.nodeType !== Node.ELEMENT_NODE || !node.hasAttribute("__pluginDomId")) {
                    continue;
                  }
                  targetCnt++;
                  node._isRemoved = true;
                  removeDomTree(node);
                }
              }
            } else {
              if (mutation.target.nodeType !== Node.ELEMENT_NODE) {
                return;
              }
              if (mutation.target.hasAttribute("__pluginDomId")) {
                traceDomTree(mutation.target, mutation.target.getAttribute("__pluginDomId"), false);
              }
              elemId = mutation.target.getAttribute("__pluginDomId");
              //console.log('style', elemId, common.shouldWatchByNative(mutation.target), mutation);
            }

          }
          isThereAnyChange = true;
          common.nextTick(putHtmlElements);
        });
      });
      observer.observe(document.body.parentElement, {
        attributes : true,
        childList: true,
        subtree: true,
        attributeFilter: ['style', 'class']
      });

    })();

    function setDomId(element) {
      common.getPluginDomId(element);
      if (element.children) {
        for (var i = 0; i < element.children.length; i++) {
          common.getPluginDomId(element.children[i]);
        }
      }
    }


    //----------------------------------------------
    // Send the DOM hierarchy to native side
    //----------------------------------------------
    var domPositions = {};
    var shouldUpdate = false;
    var doNotTrace = false;
    var checkRequested = false;

    function putHtmlElements() {
      var mapIDs = Object.keys(MAPS);
      if (isChecking) {
        checkRequested = true;
        return;
      }
      checkRequested = false;
      if (!isThereAnyChange) {
        if (!isSuspended) {
          //console.log("-->pause(320)");
          cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
        }

        //console.log("-->isSuspended = true");
        isSuspended = true;
        isThereAnyChange = false;
        isChecking = false;
        return;
      }
      isChecking = true;

      //-------------------------------------------
      // If there is no visible map, stop checking
      //-------------------------------------------
      var touchableMapList, i, mapId, map;
      touchableMapList = [];
      mapIDs = Object.keys(MAPS);
      for (i = 0; i < mapIDs.length; i++) {
        mapId = mapIDs[i];
        map = MAPS[mapId];
        if (map &&
          map.getVisible() && map.getClickable() && map.getDiv() && common.shouldWatchByNative(map.getDiv())) {
          touchableMapList.push(mapId);
        }
      }
      if (touchableMapList.length === 0) {
//console.log("--->touchableMapList.length = 0");
        if (!isSuspended) {
//        console.log("-->pause, isSuspended = true");
          cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
          isSuspended = true;
          isThereAnyChange = false;
        }
        isChecking = false;
        return;
      }

      if (checkRequested) {
//console.log("--->checkRequested");
        setTimeout(function() {
          isChecking = false;
          common.nextTick(putHtmlElements);
        }, 50);
        return;
      }
      //-------------------------------------------
      // Should the plugin update the map positions?
      //-------------------------------------------

      common._clearInternalCache();
      common.getPluginDomId(document.body);
      traceDomTree(document.body, "root", false);

      // If the map div is not displayed (such as display='none'),
      // ignore the map temporally.
      var stopFlag = false;
      var mapElemIDs = [];
      mapIDs = Object.keys(MAPS);
      (function() {
        var ele, mapId, div, elemId;
        for (var i = 0; i < mapIDs.length; i++) {
          mapId = mapIDs[i];
          div = MAPS[mapId].getDiv();
          if (div) {
            elemId = div.getAttribute("__pluginDomId");
            if (elemId) {
              if (elemId in domPositions) {
                mapElemIDs.push(elemId);
              } else {
                // Is the map div removed?
                ele = document.querySelector("[__pluginMapId='" + mapId + "']");
                if (!ele) {
                  // If no div element, remove the map.
                  if (mapId in MAPS) {
                    MAPS[mapId].remove();
                    return;
                  }
                  stopFlag = true;
                }
              }
            } else {
              // the map div is removed
              if (mapId in MAPS) {
                MAPS[mapId].remove();
                return;
              }
              stopFlag = true;
            }
          // } else {
          //   // the map div is removed
          //   console.log("mapId = " + mapId + " is already removed");
          //   if (mapId in MAPS) {
          //     MAPS[mapId].remove();
          //     return;
          //   }
          //   stopFlag = true;
          }
        }
      })();
      if (stopFlag) {
        // There is no map information (maybe timining?)
        // Try again.
        isThereAnyChange = true;
        setTimeout(function() {
          isChecking = false;
          common.nextTick(putHtmlElements);
        }, 50);
        return;
      }

      //-----------------------------------------------------------------
      // Ignore the elements that their z-index is smaller than map div
      //-----------------------------------------------------------------
      if (checkRequested) {
        setTimeout(function() {
          isChecking = false;
          common.nextTick(putHtmlElements);
        }, 50);
        return;
      }
      //-----------------------------------------------------------------
      // Pass information to native
      //-----------------------------------------------------------------
      if (isSuspended) {
        //console.log("-->resume(470)");
        cordova_exec(null, null, 'CordovaGoogleMaps', 'resume', []);
        isSuspended = false;
      }
      //console.log("--->putHtmlElements to native (start)", JSON.parse(JSON.stringify(domPositions)));
      cordova_exec(function() {
        //console.log("--->putHtmlElements to native (done)");
        if (checkRequested) {
          setTimeout(function() {
            isChecking = false;
            common.nextTick(putHtmlElements);
          }, 50);
          return;
        }
        isChecking = false;
        isThereAnyChange = false;
        isSuspended = true;
        cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
      }, null, 'CordovaGoogleMaps', 'putHtmlElements', [domPositions]);
      child = null;
      parentNode = null;
      elemId = null;
      children = null;
    }



    function traceDomTree(element, elemId, isMapChild) {
      if (doNotTraceTags.indexOf(element.tagName.toLowerCase()) > -1 ||
        !common.shouldWatchByNative(element)) {
        removeDomTree(element);
        return;
      }

      // Get the z-index CSS
      var zIndex = common.getZIndex(element);

      // Calculate dom clickable region
      var rect = common.getDivRect(element);

      // Stores dom information
      var isCached = elemId in domPositions;
      domPositions[elemId] = {
        pointerEvents: common.getStyle(element, 'pointer-events'),
        isMap: element.hasAttribute("__pluginMapId"),
        size: rect,
        zIndex: zIndex,
        overflowX: common.getStyle(element, "overflow-x"),
        overflowY: common.getStyle(element, "overflow-y"),
        children: [],
        containMapIDs: (isCached ? domPositions[elemId].containMapIDs : {})
      };
      var containMapCnt = (Object.keys(domPositions[elemId].containMapIDs)).length;
      isMapChild = isMapChild || domPositions[elemId].isMap;
      if ((containMapCnt > 0 || isMapChild || domPositions[elemId].pointerEvents === "none") && element.children.length > 0) {
        var child;
        for (var i = 0; i < element.children.length; i++) {
          child = element.children[i];
          if (doNotTraceTags.indexOf(child.tagName.toLowerCase()) > -1 ||
            !common.shouldWatchByNative(child)) {
            continue;
          }

          var childId = common.getPluginDomId(child);
          domPositions[elemId].children.push(childId);
          traceDomTree(child, childId, isMapChild);
        }
      }
    }

    // This is the special event that is fired by the google maps plugin
    // (Not generic plugin)
    function resetTimer(opts) {
      opts = opts || {};

      common.nextTick(function() {
        putHtmlElements();
        if (opts.force) {
          followMapDivPositionOnly(opts);
        }
      });
    }

    document.addEventListener("deviceready", putHtmlElements, {
      once: true
    });
    document.addEventListener("plugin_touch", resetTimer);
    window.addEventListener("orientationchange", function() {
      var cnt = 30;
      resetTimer({force: true});
      var timer = setInterval(function() {
        cnt--;
        if (cnt > 0) {
          followMapDivPositionOnly();
        } else {
          clearInterval(timer);
        }
      }, 50);
    });

    //----------------------------------------------------
    // Stop all executions if the page will be closed.
    //----------------------------------------------------
    function stopExecution() {
      // Request stop all tasks.
      _stopRequested = true;
/*
      if (_isWaitMethod && _executingCnt > 0) {
        // Wait until all tasks currently running are stopped.
        common.nextTick(arguments.callee, 100);
        return;
      }
*/
    }
    window.addEventListener("unload", stopExecution);

    //--------------------------------------------
    // Hook the backbutton of Android action
    //--------------------------------------------
    var anotherBackbuttonHandler = null;
    function onBackButton(e) {
      common.nextTick(putHtmlElements);
      if (anotherBackbuttonHandler) {
        // anotherBackbuttonHandler must handle the page moving transaction.
        // The plugin does not take care anymore if another callback is registered.
        anotherBackbuttonHandler(e);
      } else {
        cordova_exec(null, null, 'CordovaGoogleMaps', 'backHistory', []);
      }
    }
    document.addEventListener("backbutton", onBackButton);

    var _org_addEventListener = document.addEventListener;
    var _org_removeEventListener = document.removeEventListener;
    document.addEventListener = function(eventName, callback) {
      var args = Array.prototype.slice.call(arguments, 0);
      if (eventName.toLowerCase() !== "backbutton") {
        _org_addEventListener.apply(this, args);
        return;
      }
      if (!anotherBackbuttonHandler) {
        anotherBackbuttonHandler = callback;
      }
    };
    document.removeEventListener = function(eventName, callback) {
      var args = Array.prototype.slice.call(arguments, 0);
      if (eventName.toLowerCase() !== "backbutton") {
        _org_removeEventListener.apply(this, args);
        return;
      }
      if (anotherBackbuttonHandler === callback) {
        anotherBackbuttonHandler = null;
      }
    };



    /*****************************************************************************
     * Name space
     *****************************************************************************/
    var singletonGeolocation = new Geolocation(execCmd);
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
              var mapId, elem, elemId;
              if (common.isDom(div)) {
                mapId = div.getAttribute("__pluginMapId");
                if (!mapOptions || mapOptions.visible !== false) {
                  // Add gray color until the map is displayed.
                  div.style.backgroundColor = "rgba(200, 200, 200, 0.5)";
                }
              }
              if (mapId && MAPS[mapId].getDiv() !== div) {
              //console.log("--->different mapdiv = " + mapId, MAPS[mapId].getDiv(), div);
                elem = MAPS[mapId].getDiv();
                while(elem && elem.nodeType === Node.ELEMENT_NODE) {
                  elemId = elem.getAttribute("__pluginDomId");
                  if (elemId && elemId in domPositions) {
                    domPositions[elemId].containMapIDs = domPositions[elemId].containMapIDs || {};
                    delete domPositions[elemId].containMapIDs[mapId];
                    if ((Object.keys(domPositions[elemId].containMapIDs).length) < 1) {
                      delete domPositions[elemId];
                    }
                  }
                  elem = elem.parentNode;
                }
                MAPS[mapId].remove();
                mapId = undefined;
              }
              if (mapId && mapId in MAPS) {
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

              var map = new Map(mapId, execCmd);

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
                  //console.log("--->removeMap mapId = " + mapId);

                  var keys = Object.keys(domPositions);
                  var elemId;
                  for (var i = 0; i < keys.length; i++) {
                    elemId = keys[i];
                    domPositions[elemId].containMapIDs = domPositions[elemId].containMapIDs || {};
                    delete domPositions[elemId].containMapIDs[mapId];
                    if ((Object.keys(domPositions[elemId].containMapIDs)).length < 1) {
                      delete domPositions[elemId];
                    }
                  }
                  MAPS[mapId].destroy();
                  delete MAPS[mapId];
                  map = undefined;

                  if ((Object.keys(MAPS)).length === 0) {
                    common._clearInternalCache();

                    isSuspended = true;
                    cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
                  }
              });
              MAP_CNT++;
              MAPS[mapId] = map;
              isSuspended = false;
              isThereAnyChange = true;
              isChecking = false;



                var args = [mapId];
                for (var i = 0; i < arguments.length; i++) {
                    args.push(arguments[i]);
                }

              if (common.isDom(div)) {
                div.setAttribute("__pluginMapId", mapId);

                elemId = common.getPluginDomId(div);
    //console.log("---> map.getMap() = " + elemId + ", mapId = " + mapId);

                elem = div;
                var isCached;
                while(elem && elem.nodeType === Node.ELEMENT_NODE) {
                  elemId = common.getPluginDomId(elem);
                  isCached = elemId in domPositions;
                  domPositions[elemId] = {
                    pointerEvents: common.getStyle(elem, 'pointer-events'),
                    isMap: false,
                    size: common.getDivRect(elem),
                    zIndex: common.getZIndex(elem),
                    children: [],
                    overflowX: common.getStyle(elem, "overflow-x"),
                    overflowY: common.getStyle(elem, "overflow-y"),
                    containMapIDs: (isCached ? domPositions[elemId].containMapIDs : {})
                  };
                  domPositions[elemId].containMapIDs[mapId] = 1;
                  elem = elem.parentNode;
                }

                elemId = common.getPluginDomId(div);
                domPositions[elemId].isMap = true;

                //console.log("--->getMap (start)", JSON.parse(JSON.stringify(domPositions)));
                cordova_exec(function() {
                  cordova_exec(function() {
                    map.getMap.apply(map, args);
                  }, null, 'CordovaGoogleMaps', 'putHtmlElements', [domPositions]);
                }, null, 'CordovaGoogleMaps', 'resume', []);
                //resetTimer({force: true});
              } else {
                map.getMap.apply(map, args);
              }






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
      Geolocation: singletonGeolocation,
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
  var mapIDs = Object.keys(MAPS);
  for (var i = 0; i< mapIDs.length; i++) {
    mapId = mapIDs[i];
    MAPS[mapId].refreshLayout();
  }
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
          success.apply(self,results);
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
          error.apply(self,results);
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
  if (_isExecuting || _executingCnt >= MAX_EXECUTE_CNT ) {
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
