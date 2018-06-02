
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
  BaseClass: BaseClass,
  BaseArrayClass: BaseArrayClass,
  Map: {
    getMap: cordovaGoogleMaps.getMap.bind(cordovaGoogleMaps)
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
