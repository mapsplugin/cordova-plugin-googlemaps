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

    var prevDomPositions = {};
    var idlingCnt = -1;
    var longIdlingCnt = -1;
    var MIN_MAP_DEPTH = -1;
    var MAX_MAP_DEPTH = -1;

    var isChecking = false;
    var cacheDepth = {};
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

      mapIDs.forEach(function(mapId) {
        var map = MAPS[mapId];
        if (map && map.getVisible() && map.getDiv() && common.shouldWatchByNative(map.getDiv())) {
          var mapDiv = map.getDiv();
          var divId = mapDiv.getAttribute("__pluginDomId");
          mapRects[divId] = {
            size: mapDiv.getBoundingClientRect(),
            depth: cacheDepth[divId]
          };
          if (!changed && prevMapRects && (divId in prevMapRects) && (
            prevMapRects[divId].size.left !== mapRects[divId].size.left ||
            prevMapRects[divId].size.top !== mapRects[divId].size.top ||
            prevMapRects[divId].size.width !== mapRects[divId].size.width ||
            prevMapRects[divId].size.height !== mapRects[divId].size.height ||
            prevMapRects[divId].depth !== mapRects[divId].depth)) {
            changed = true;
          }
        }
      });
      prevMapRects = mapRects;
      if (changed || opts.force) {
        cordova_exec(null, null, 'CordovaGoogleMaps', 'updateMapPositionOnly', [mapRects]);
      }
    }

    document.body.addEventListener("transitionend", function(e) {
      if (e.target.hasAttribute("__pluginDomId")) {
        var elemId = e.target.getAttribute("__pluginDomId");
        removeDomTree(e.target, {
          keepDomId: true
        });
      }
      resetTimer({force: true});
    }, true);

    document.body.addEventListener("scroll", function(e) {
      if (e.target.hasAttribute("__pluginDomId")) {
        var elemId = e.target.getAttribute("__pluginDomId");
        var depth = cacheDepth[elemId];
        if (depth > MIN_MAP_DEPTH) {
          return;
        }
      }
      if (scrollEndTimer) {
        clearTimeout(scrollEndTimer);
      }
      scrollEndTimer = setTimeout(onScrollEnd, 100);
      followMapDivPositionOnly();
    }, true);
    function onScrollEnd() {
      isThereAnyChange = true;
      idlingCnt = -1;
      longIdlingCnt = -1;
      common.nextTick(putHtmlElements);
    }

    function removeDomTree(node, options) {
      options = options || {};
      var children = node.querySelectorAll('[__pluginDomId]');
      if (children && children.length > 0) {
        children.forEach(function(child) {
          var elemId = child.getAttribute('__pluginDomId');
          if (!options.keepDomId) {
            child.removeAttribute('__pluginDomId');
            if (child.hasAttribute('__pluginMapId')) {
              // If no div element, remove the map.
              var mapId = child.getAttribute('__pluginMapId');
              if (mapId in MAPS) {
                MAPS[mapId].remove();
              }
            }
          }
          common._removeCacheById(elemId);
          delete domPositions[elemId];
          delete prevDomPositions[elemId];
          delete cacheDepth[elemId];
          delete prevFinal[elemId];
        });
      }
      if (node.hasAttribute("__pluginDomId")) {
        var elemId = node.getAttribute('__pluginDomId');
        if (!options.keepDomId) {
          node.removeAttribute('__pluginDomId');
          if (node.hasAttribute('__pluginMapId')) {
            // If no div element, remove the map.
            var mapId = child.getAttribute('__pluginMapId');
            if (mapId in MAPS) {
              MAPS[mapId].remove();
            }
          }
        }
        common._removeCacheById(elemId);
        delete domPositions[elemId];
        delete prevDomPositions[elemId];
        delete cacheDepth[elemId];
        delete prevFinal[elemId];
      }
    }
    //----------------------------------------------
    // Observe styles and childList (if possible)
    //----------------------------------------------
    var isThereAnyChange = true;
    var isMutationObserver = typeof MutationObserver === "function";
    (function() {
      if (!isMutationObserver) {
        return;
      }
      var observer = new MutationObserver(function(mutations) {
        common.nextTick(function() {
          mutations.forEach(function(mutation) {
            var targetCnt = 0;
            if (mutation.type === "childList") {
              if (mutation.addedNodes) {
                mutation.addedNodes.forEach(function(node) {
                  if (node.nodeType !== Node.ELEMENT_NODE) {
                    return;
                  }
                  targetCnt++;
                  setDomId(node);
                });
              }
              if (mutation.removedNodes) {
                mutation.removedNodes.forEach(function(node) {
                  if (node.nodeType !== Node.ELEMENT_NODE || !node.hasAttribute("__pluginDomId")) {
                    return;
                  }
                  targetCnt++;
                  removeDomTree(node);
                });
              }
              if (targetCnt > 0) {
                isThereAnyChange = true;
                idlingCnt = -1;
                longIdlingCnt = -1;
                common.nextTick(putHtmlElements);
              }
            } else {
              if (mutation.target.nodeType !== Node.ELEMENT_NODE) {
                return;
              }
              if (mutation.target.hasAttribute("__pluginDomId")) {
                removeDomTree(mutation.target, {
                  keepDomId: true
                });
              }
              isThereAnyChange = true;
              idlingCnt = -1;
              longIdlingCnt = -1;
              common.nextTick(putHtmlElements);
              // var elemId = mutation.target.getAttribute("__pluginDomId");
              // console.log('style', elemId, common.shouldWatchByNative(mutation.target), mutation);
            }

          });
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
      var elemId = element.getAttribute("__pluginDomId");
      if (!elemId) {
        elemId = "pgm" + Math.floor(Math.random() * Date.now());
        element.setAttribute("__pluginDomId", elemId);
      }
      for (var i = 0; i < element.children.length; i++) {
        setDomId(element.children[i]);
      }
    }


    //----------------------------------------------
    // Send the DOM hierarchy to native side
    //----------------------------------------------
    var domPositions = {};
    var shouldUpdate = false;
    var doNotTrace = false;
    var prevFinal = {};
    var checkRequested = false;

    function putHtmlElements() {
      var mapIDs = Object.keys(MAPS);
      if (isChecking) {
        checkRequested = true;
        return;
      }
      checkRequested = false;
      if (!isThereAnyChange || mapIDs.length === 0) {
        if (!isSuspended) {
          cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
        }
        if (mapIDs.length === 0) {
          // If no map, release the JS heap memory
          domPositions = undefined;
          prevDomPositions = undefined;
          cacheDepth = undefined;
          prevFinal = undefined;
          domPositions = {};
          prevDomPositions = {};
          cacheDepth = {};
          prevFinal = {};
          common._clearInternalCache();
        }
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
      for (i = 0; i < mapIDs.length; i++) {
        mapId = mapIDs[i];
        map = MAPS[mapId];
        if (map &&
          map.getVisible() && map.getClickable() && map.getDiv() && common.shouldWatchByNative(map.getDiv())) {
          touchableMapList.push(mapId);
        }
      }
      if (idlingCnt > -1 && touchableMapList.length === 0) {
        idlingCnt++;
        if (!isSuspended) {
          cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
          isSuspended = true;
          isThereAnyChange = false;
        }
        isChecking = false;
        return;
      }

      if (checkRequested) {
        setTimeout(function() {
          isChecking = false;
          common.nextTick(putHtmlElements);
        }, 50);
        return;
      }
      //-------------------------------------------
      // Should the plugin update the map positions?
      //-------------------------------------------

      var bodyRect = common.getDivRect(document.body);
      bodyRect.right = bodyRect.left + bodyRect.width;
      bodyRect.bottom = bodyRect.top + bodyRect.heihgt;

      common._clearInternalCache();
      traceDomTree(document.body, 0, bodyRect, 0, 0, 0);

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
        // Stop timer when user does not touch the app and no changes are occurred during 1500ms.
        // (50ms * 5times + 200ms * 5times).
        // This save really the battery life significantly.
        if (!isMutationObserver && idlingCnt < 10) {
          if (idlingCnt === 8) {
            cordova.fireDocumentEvent("ecocheck", {});
          }
          setTimeout(function() {
            common.nextTick(putHtmlElements);
          }, idlingCnt < 5 ? 50 : 200);
        }
        isThereAnyChange = false;
        isChecking = false;
        cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
        return;
      }
      idlingCnt = 0;
      longIdlingCnt = 0;

      // If the map div is not displayed (such as display='none'),
      // ignore the map temporally.
      var minMapDepth = 9999999;
      var maxMapDepth = -1;
      var stopFlag = false;
      var mapElemIDs = [];
      mapIDs.forEach(function(mapId) {
        var div = MAPS[mapId].getDiv();
        if (div) {
          var elemId = div.getAttribute("__pluginDomId");
          if (elemId) {
            if (elemId in domPositions) {
              mapElemIDs.push(elemId);
              minMapDepth = Math.min(minMapDepth, domPositions[elemId].depth);
              maxMapDepth = Math.max(maxMapDepth, domPositions[elemId].depth);

              div = div.parentNode;
              while(div) {
                children = div.children;
                for (var i = 0; i < children.length; i++) {
                  elemId = children[i].getAttribute("__pluginDomId");
                  if (elemId in domPositions) {
                    domPositions[elemId].parent = true;
                  }
                }
                div = div.parentNode;
              }


            } else {
              // Is the map div removed?
              if (window.document.querySelector) {
                var ele = document.querySelector("[__pluginMapId='" + mapId + "']");
                if (!ele) {
                  // If no div element, remove the map.
                  if (mapId in MAPS) {
                    MAPS[mapId].remove();
                    return;
                  }
                }
              }
              stopFlag = true;
            }
          }
        }
      });
      if (stopFlag) {
        // There is no map size information (maybe timining?)
        // Try again.
        isThereAnyChange = true;
        setTimeout(function() {
          isChecking = false;
          common.nextTick(putHtmlElements);
        }, 50);
        return;
      }
      MIN_MAP_DEPTH = minMapDepth;
      MAX_MAP_DEPTH = maxMapDepth;

      //-----------------------------------------------------------------
      // Ignore the elements that their z-index is smaller than map div
      //-----------------------------------------------------------------
      var finalDomPositions;
      if (touchableMapList.length === 0) {
        finalDomPositions = domPositions;
      } else {
        finalDomPositions = common.quickfilter(domPositions, minMapDepth, mapElemIDs);
      }
      var prevKeys = Object.keys(prevFinal);
      var currentKeys = Object.keys(finalDomPositions);
      if (prevKeys.length === currentKeys.length) {
        var diff = prevKeys.filter(function(prevKey) {
          return currentKeys.indexOf(prevKey) === -1;
        });
        if (diff.length === 0) {
          if (checkRequested || isThereAnyChange) {
            isChecking = false;
            common.nextTick(putHtmlElements);
            return;
          }
          if (!isSuspended) {
            cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
          }
          isSuspended = true;
          isChecking = false;
          return;
        }
      }
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
        cordova_exec(null, null, 'CordovaGoogleMaps', 'resume', []);
        isSuspended = false;
      }
  //console.log("--->putHtmlElements to native (start)", JSON.stringify(finalDomPositions, null, 2));
      cordova_exec(function() {
        prevDomPositions = domPositions;
        if (!isMutationObserver) {
          isThereAnyChange = true;
          setTimeout(function() {
            isChecking = false;
            common.nextTick(putHtmlElements);
          }, 50);
        } else {
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
        }
      }, null, 'CordovaGoogleMaps', 'putHtmlElements', [finalDomPositions]);
      child = null;
      parentNode = null;
      elemId = null;
      children = null;
    }

    function traceDomTree(element, domIdx, parentRect, parentZIndex, parentDepth, zIndexSolt) {
      var zIndex = parentZIndex;
      doNotTrace = false;
      var depth = 1;
      var elemId;

      if (common.shouldWatchByNative(element)) {

        // Generates a __pluginDomId
        elemId = element.getAttribute("__pluginDomId");
        if (!elemId) {
          elemId = "pgm" + Math.floor(Math.random() * Date.now());
          element.setAttribute("__pluginDomId", elemId);
        }

        // get dom depth
        zIndex = common.getZIndex(element, zIndexSolt);
        if (zIndex === 0) {
          zIndexSolt++;
        }
        zIndex = zIndex / Math.pow(zIndexSolt + 0.1, zIndexSolt + 0.1);
        zIndex += parentZIndex;
        var rect;
        if (elemId in cacheDepth &&
            elemId in prevDomPositions &&
            prevDomPositions[elemId].zIndex === zIndex) {
            depth = cacheDepth[elemId];
        } else {
            if (parentDepth > MAX_MAP_DEPTH && MAX_MAP_DEPTH !== -1) {
              depth = parentDepth;
            } else {
              depth = common.getDomDepth(element, domIdx, zIndex);
            }
            cacheDepth[elemId] = depth;
        }
        // Calculate dom clickable region
        //if (parentDepth > MAX_MAP_DEPTH && MAX_MAP_DEPTH !== -1) {
        //  rect = parentRect;
        //} else {
          rect = common.getClickableRect(element, parentRect);
        //}

        // Stores dom bounds and depth
        domPositions[elemId] = {
          size: rect,
          depth: depth,
          zIndex: zIndex,
          ignore: element.className.indexOf("_gmaps_cdv_") !== -1
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
          //if (element.hasAttribute("__pluginDomId")) {
          //    shouldUpdate = true;
          //    element.removeAttribute("__pluginDomId");
          //}
          if (doNotTraceTags.indexOf(element.tagName.toLowerCase()) > -1) {
            doNotTrace = true;
          } else {
            var pointerEventsCSS = common.getStyle(element, 'pointer-events');
            if (pointerEventsCSS === "none") {

              // Generates a __pluginDomId
              elemId = element.getAttribute("__pluginDomId");
              if (!elemId) {
                elemId = "pgm" + Math.floor(Math.random() * Date.now());
                element.setAttribute("__pluginDomId", elemId);
              }

              // get dom depth
              zIndex = common.getZIndex(element, zIndexSolt);
              if (zIndex === 0) {
                zIndexSolt++;
              }
              zIndex = zIndex / Math.pow(zIndexSolt + 0.1, zIndexSolt + 0.1);
              zIndex += parentZIndex;
              if (elemId in cacheDepth &&
                  elemId in prevDomPositions &&
                  prevDomPositions[elemId].zIndex === zIndex) {
                  depth = cacheDepth[elemId];
              } else {
                  depth = common.getDomDepth(element, domIdx, zIndex);
                  cacheDepth[elemId] = depth;
              }
            }
          }
          if (doNotTrace) {
            removeDomTree(element, {
              keepDomId: true
            });
          }
        } else {
          doNotTrace = true;
        }
      }
      if (!doNotTrace && element.nodeType === Node.ELEMENT_NODE) {
        if (element.children.length > 0) {
          var child;
          for (var i = 0; i < element.children.length; i++) {
            child = element.children[i];
            if (child.nodeType !== Node.ELEMENT_NODE ||
              doNotTraceTags.indexOf(child.tagName.toLowerCase()) > -1 ||
              common.getStyle(child, "display") === "none") {
              continue;
            }
            traceDomTree(child, domIdx + i + 1, parentRect, zIndex, depth, zIndexSolt);
          }
        }
      }
    }
    // This is the special event that is fired by the google maps plugin
    // (Not generic plugin)
    function resetTimer(opts) {
      opts = opts || {};
      if (!isMutationObserver || opts.force) {
        idlingCnt = -1;
        longIdlingCnt = -1;
        isSuspended = false;
        if (!isMutationObserver) {
          cacheDepth = {};
          cacheZIndex = {};
        }
        cordova_exec(null, null, 'CordovaGoogleMaps', 'resume', []);
        isThereAnyChange = true;
      }
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
      var cnt = 10;
      resetTimer({force: true});
      var timer = setInterval(function() {
        cnt--;
        if (cnt > 0 && !isSuspended) {
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
              var mapId;
              if (common.isDom(div)) {
                mapId = div.getAttribute("__pluginMapId");
                if (!mapOptions || mapOptions.visible !== false) {
                  // Add gray color until the map is displayed.
                  div.style.backgroundColor = "rgba(200, 200, 200, 0.5)";
                }
              }
              if (mapId && MAPS[mapId].getDiv() !== div) {
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
              if (common.isDom(div)) {
                div.setAttribute("__pluginMapId", mapId);

                var elemId = div.getAttribute("__pluginDomId");
                if (!elemId) {
                  elemId = "pgm" + Math.floor(Math.random() * Date.now());
                  div.setAttribute("__pluginDomId", elemId);
                }

                var dummyInfo = {};
                dummyInfo[elemId] = {
                  size: div.getBoundingClientRect(),
                  depth: 0.01
                };
//console.log(dummyInfo);
                cordova_exec(null, null, 'CordovaGoogleMaps', 'updateMapPositionOnly', [dummyInfo]);
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
                  var div = map.getDiv();
                  if (!div) {
                    div = document.querySelector("[__pluginMapId='" + mapId + "']");
                  }
                  if (div) {
                    div.removeAttribute('__pluginMapId');
                  }
                  MAPS[mapId].destroy();
                  delete MAPS[mapId];
                  map = undefined;

                  if ((Object.keys(MAPS)).length === 0) {
                    // If no map, release the JS heap memory
                    domPositions = undefined;
                    prevDomPositions = undefined;
                    cacheDepth = undefined;
                    prevFinal = undefined;
                    domPositions = {};
                    prevDomPositions = {};
                    cacheDepth = {};
                    prevFinal = {};
                    common._clearInternalCache();

                    isSuspended = true;
                    cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
                  }
              });
              map.on('touchevent', followMapDivPositionOnly);
              MAP_CNT++;
              MAPS[mapId] = map;
              isThereAnyChange = true;

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
  if (this._isRemoved && !execOptions.remove || !this._isReady) {
    // Ignore if the instance is already removed, or the instance is not ready.
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
