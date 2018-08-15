
var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    event = require('cordova-plugin-googlemaps.event'),
    common = require('cordova-plugin-googlemaps.Common');

var Map = require('cordova-plugin-googlemaps.Map'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  BaseArrayClass = require('cordova-plugin-googlemaps.BaseArrayClass');

var cordova_exec = require('cordova/exec');

var execCmd = require("cordova-plugin-googlemaps.commandQueueExecutor");
var cordovaGoogleMaps = new (require('cordova-plugin-googlemaps.js_CordovaGoogleMaps'))(execCmd);

module.exports = {
  event: event,
  Animation: {
      BOUNCE: 'BOUNCE',
      DROP: 'DROP'
  },
  BaseClass: BaseClass,
  BaseArrayClass: BaseArrayClass,
  Map: {
    getMap: cordovaGoogleMaps.getMap.bind(cordovaGoogleMaps)
  },
  StreetView: {
    getPanorama: cordovaGoogleMaps.getPanorama.bind(cordovaGoogleMaps)
  },
  HtmlInfoWindow: require('cordova-plugin-googlemaps.HtmlInfoWindow'),
  LatLng: require('cordova-plugin-googlemaps.LatLng'),
  LatLngBounds: require('cordova-plugin-googlemaps.LatLngBounds'),
  MapTypeId: require('cordova-plugin-googlemaps.MapTypeId'),
  environment: require('cordova-plugin-googlemaps.Environment'),
  Geocoder: require('cordova-plugin-googlemaps.Geocoder')(execCmd),
  LocationService: require('cordova-plugin-googlemaps.LocationService')(execCmd),
  geometry: {
      encoding: require('cordova-plugin-googlemaps.encoding'),
      spherical: require('cordova-plugin-googlemaps.spherical'),
      poly: require('cordova-plugin-googlemaps.poly')
  }
};


cordova.addConstructor(function() {
  if (!window.Cordova) {
      window.Cordova = cordova;
  }
  window.plugin = window.plugin || {};
  window.plugin.google = window.plugin.google || {};
  window.plugin.google.maps = window.plugin.google.maps || module.exports;
});
