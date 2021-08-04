var event = require('cordova-plugin-googlemaps-beta.event'),
  BaseClass = require('cordova-plugin-googlemaps-beta.BaseClass'),
  BaseArrayClass = require('cordova-plugin-googlemaps-beta.BaseArrayClass'),
  execCmd = require('cordova-plugin-googlemaps-beta.commandQueueExecutor'),
  cordovaGoogleMaps = new(require('cordova-plugin-googlemaps-beta.js_CordovaGoogleMaps'))(execCmd);

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
    getPanorama: cordovaGoogleMaps.getPanorama.bind(cordovaGoogleMaps),
    Source: {
      DEFAULT: 'DEFAULT',
      OUTDOOR: 'OUTDOOR'
    }
  },
  HtmlInfoWindow: require('cordova-plugin-googlemaps-beta.HtmlInfoWindow'),
  LatLng: require('cordova-plugin-googlemaps-beta.LatLng'),
  LatLngBounds: require('cordova-plugin-googlemaps-beta.LatLngBounds'),
  MapTypeId: require('cordova-plugin-googlemaps-beta.MapTypeId'),
  environment: require('cordova-plugin-googlemaps-beta.Environment'),
  Geocoder: require('cordova-plugin-googlemaps-beta.Geocoder')(execCmd),
  ElevationService: require('cordova-plugin-googlemaps-beta.ElevationService')(execCmd),
  DirectionsService: require('cordova-plugin-googlemaps-beta.DirectionsService')(execCmd),
  LocationService: require('cordova-plugin-googlemaps-beta.LocationService')(execCmd),
  geometry: {
    encoding: require('cordova-plugin-googlemaps-beta.encoding'),
    spherical: require('cordova-plugin-googlemaps-beta.spherical'),
    poly: require('cordova-plugin-googlemaps-beta.poly')
  }
};

cordova.addConstructor(function () {
  if (!window.Cordova) {
    window.Cordova = cordova;
  }
  window.plugin = window.plugin || {};
  window.plugin.google = window.plugin.google || {};
  window.plugin.google.maps = window.plugin.google.maps || module.exports;
});
