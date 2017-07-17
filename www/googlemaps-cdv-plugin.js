/* global cordova, plugin, CSSPrimitiveValue */
var MAP_CNT = 0;

var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
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
var MarkerCluster = require('./MarkerCluster');
var geomodel = require('./geomodel');

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
    viewportTag.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no');
})();

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

  var isChecking = false;
  var cacheDepth = {};
  var navDecorBlocker = document.createElement("style");
  navDecorBlocker.setAttribute("type", "text/css");
  navDecorBlocker.innerText = [
    "._gmaps_cdv_ .nav-decor {",
    "   background-color: rgba(0,0,0,0) !important;",
    "   background: rgba(0,0,0,0) !important;",
    "   display:none !important;",
    "}"
  ].join("");
  document.head.appendChild(navDecorBlocker);
  var doNotTraceTags = [
    "svg"
  ];

  function putHtmlElements() {
      var mapIDs = Object.keys(MAPS);
      if (isChecking) {
        return;
      }
      if (mapIDs.length === 0) {
        cordova.exec(null, null, 'CordovaGoogleMaps', 'clearHtmlElements', []);
        return;
      }
      isChecking = true;

      //-------------------------------------------
      // If there is no visible map, stop checking
      //-------------------------------------------
      var visibleMapDivList, i, mapId, map;
      if (window.document.querySelectorAll) {
        // Android 4.4 and above
        visibleMapList = mapIDs.filter(function(mapId) {
          var map = MAPS[mapId];
          return (map && map.getVisible() && map.getDiv() && common.shouldWatchByNative(map.getDiv()));
        });
      } else {
        // for older versions than Android 4.4
        visibleMapList = [];
        for (i = 0; i < mapIDs.length; i++) {
          mapId = mapIDs[i];
          map = MAPS[mapId];
          if (map && map.getVisible() && map.getDiv() && common.shouldWatchByNative(map.getDiv())) {
            visibleMapList.push(mapId);
          }
        }
      }
      if (visibleMapList.length === 0) {
        idlingCnt++;
        if (idlingCnt < 10) {
          setTimeout(putHtmlElements, idlingCnt < 5 ? 50 : 200);
        }
        isChecking = false;
        return;
      }

      //-------------------------------------------
      // Should the plugin update the map positions?
      //-------------------------------------------
      var domPositions = {};
      var shouldUpdate = false;
      var doNotTrace = false;
      var traceDomTree = function(element, domIdx) {
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

          // Stores dom bounds and depth
          domPositions[elemId] = {
            size: common.getDivRect(element),
            depth: depth,
            zIndex: zIndex
          };

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
            for (var i = 0; i < element.childNodes.length; i++) {
              traceDomTree(element.childNodes[i], domIdx + i + 1);
            }
          }
        }

      };
      traceDomTree(document.body, 0);

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
          if (idlingCnt < 10) {
            setTimeout(putHtmlElements, idlingCnt < 5 ? 50 : 200);
          }
          isChecking = false;
          return;
      }
      idlingCnt = 0;

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

      cordova.exec(function() {
          prevDomPositions = domPositions;
          mapIDs.forEach(function(mapId) {
              if (mapId in MAPS) {
                  MAPS[mapId].refreshLayout();
              }
          });
          setTimeout(putHtmlElements, 25);
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
    delete cacheDepth;
    cacheZIndex = {};
    setTimeout(putHtmlElements, 0);
  }

  document.addEventListener("deviceready", resetTimer);
  document.addEventListener("plugin_touch", resetTimer);
  window.addEventListener("orientationchange", resetTimer);

  // Catches the backbutton event
  // https://github.com/apache/cordova-android/blob/55d7cf38654157187c4a4c2b8784191acc97c8ee/bin/templates/project/assets/www/cordova.js#L1796-L1802
  var APP_PLUGIN_NAME = Number(require('cordova').platformVersion.split('.')[0]) >= 4 ? 'CoreAndroid' : 'App';
  document.addEventListener("backbutton", function() {
    // Executes the browser back history action
    exec(null, null, APP_PLUGIN_NAME, "backHistory", []);
    resetTimer();

    // For other plugins, fire the `plugin_buckbutton` event instead of the `backbutton` evnet.
    cordova.fireDocumentEvent('plugin_backbutton', {});
  }, false);

}());

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

    BaseClass: BaseClass,
    BaseArrayClass: BaseArrayClass,
    Map: {
        getMap: function(div) {
            var mapId;
            if (common.isDom(div)) {
              mapId = div.getAttribute("__pluginMapId");
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
        geomodel: geomodel
    },
    MarkerCluster: MarkerCluster
};

cordova.addConstructor(function() {
    if (!window.Cordova) {
        window.Cordova = cordova;
    }
    window.plugin = window.plugin || {};
    window.plugin.google = window.plugin.google || {};
    window.plugin.google.maps = window.plugin.google.maps || module.exports;
    document.addEventListener("deviceready", function() {
        document.removeEventListener("deviceready", arguments.callee);
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
    });
});
