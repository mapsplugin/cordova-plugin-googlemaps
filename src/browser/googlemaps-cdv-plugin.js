
(function() {
  var execCmd = require('cordova/exec');
  var common = require('cordova-plugin-googlemaps.Common'),
    event = require('cordova-plugin-googlemaps.event'),
    MapTypeId = require('cordova-plugin-googlemaps.MapTypeId'),
    LatLng = require('cordova-plugin-googlemaps.LatLng'),
    LatLngBounds = require('cordova-plugin-googlemaps.LatLngBounds'),
    Environment = require('cordova-plugin-googlemaps.Environment'),
    Geocoder = require('cordova-plugin-googlemaps.Geocoder'),
    BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
    BaseArrayClass = require('cordova-plugin-googlemaps.BaseArrayClass'),
    encoding = require('cordova-plugin-googlemaps.encoding'),
    spherical = require('cordova-plugin-googlemaps.spherical'),
    HtmlInfoWindow = require('cordova-plugin-googlemaps.HtmlInfoWindow'),
    Map = require('cordova-plugin-googlemaps.Map');
  var saltHash = Math.floor(Math.random() * Date.now());
  var MAP_CNT = 0;
  var MAPS = {};

  module.exports = {
    event: event,
    MapTypeId: MapTypeId,
    LatLng: LatLng,
    LatLngBounds: LatLngBounds,
    HtmlInfoWindow: HtmlInfoWindow,
    environment: Environment,
    Geocoder: Geocoder,
    Animation: {
      BOUNCE: 'BOUNCE',
      DROP: 'DROP'
    },
    geometry: {
      encoding: encoding,
      spherical: spherical
    },
    Map: {
      getMap: function(div, mapOptions) {
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
        var map = new Map(mapId, execCmd);
        MAPS[mapId] = map;
        var args = [mapId];
        for (var i = 0; i < arguments.length; i++) {
            args.push(arguments[i]);
        }
        map.getMap.apply(map, args);
        return map;
      }
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
        if (!window.plugin) { console.warn('re-init window.plugin'); window.plugin = window.plugin || {}; }
        if (!window.plugin.google) { console.warn('re-init window.plugin.google'); window.plugin.google = window.plugin.google || {}; }
        if (!window.plugin.google.maps) { console.warn('re-init window.plugin.google.maps'); window.plugin.google.maps = window.plugin.google.maps || module.exports; }
    }, {
      once: true
    });
  });
})();
