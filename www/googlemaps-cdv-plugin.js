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
var KmlOverlay = require('./KmlOverlay');
var encoding = require('./encoding');
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
    viewportTag.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no');
})();

/*****************************************************************************
 * Add event lister to all html nodes under the <body> tag.
 *****************************************************************************/
(function() {
  var prevDomPositions = {};
  var prevChildrenCnt = 0;
  var idlingCnt = 0;

  var baseDom = document.createElement("div");
  baseDom.style.width = "1px";
  baseDom.style.height = "1px";
  baseDom.style.position = "absolute";
  baseDom.style.zIndex = 9999;
  baseDom.style.visibility = "hidden";
  document.body.insertBefore(baseDom, document.body.firstChild);
  var baseRect;
  var isChecking = false;

  function putHtmlElements() {
      if (isChecking || Object.keys(MAPS).length === 0) {
        return;
      }
      isChecking = true;
      baseRect = common.getDivRect(baseDom);
      var children = common.getAllChildren(document.body);
      var bodyRect = common.getDivRect(document.body);

      if (children.length === 0) {
          children = null;
          isChecking = false;
          return;
      }

      var domPositions = {};
      var size, elemId, i, child, parentNode;
      var shouldUpdate = false;

      children.unshift(document.body);
      if (children.length !== prevChildrenCnt) {
          shouldUpdate = true;
      }
      prevChildrenCnt = children.length;
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
          idlingCnt++;
          if (idlingCnt === 2) {
              var mapIDs = Object.keys(MAPS);
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
      for (i = 0; i < children.length; i++) {
          child = children[i];
          elemId = child.getAttribute("__pluginDomId");
          domPositions[elemId].offsetX = domPositions[elemId].size.left - baseRect.left;
          domPositions[elemId].offsetY = domPositions[elemId].size.top - baseRect.top;
      }
      cordova.exec(function() {
          prevDomPositions = domPositions;

          var mapIDs = Object.keys(MAPS);
          mapIDs.forEach(function(mapId) {
              MAPS[mapId].refreshLayout();
          });
          setTimeout(putHtmlElements, 50);
          isChecking = false;
      }, null, 'CordovaGoogleMaps', 'putHtmlElements', [domPositions]);
      child = null;
      parentNode = null;
      elemId = null;
      children = null;
  }
  setTimeout(putHtmlElements, 50);

  // This is the special event that is fired by the google maps plugin
  // (Not generic plugin)
  function resetTimer() {
    idlingCnt = 0;
    setTimeout(putHtmlElements, 0);
  }
  document.addEventListener("touch_start", resetTimer);
  window.addEventListener("orientationchange", resetTimer);

}());

//setTimeout(function() {
  // Webkit redraw mandatory
  // http://stackoverflow.com/a/3485654/697856
  document.body.style.backgroundColor = "rgba(0,0,0,0)";
  //document.body.style.display='none';
  document.body.offsetHeight;
  //document.body.style.display='';
//}, 0);


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
