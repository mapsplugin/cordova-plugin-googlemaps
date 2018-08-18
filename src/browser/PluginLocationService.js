

var utils = require('cordova/utils');
var event = require('cordova-plugin-googlemaps.event');
var BaseClass = require('cordova-plugin-googlemaps.BaseClass');

var LOCATION_ERROR = {
  '1': 'service_denied',
  '2': 'not_available',
  '3': 'timeout'
};

module.exports = {
  'getMyLocation': function(onSuccess, onError, args) {

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(function(position) {
        onSuccess({
          'latLng': {
            'lat': position.coords.latitude,
            'lng': position.coords.longitude
          },
          'elapsedRealtimeNanos': 0,
          'time': position.timestamp,
          'accuracy': position.coords.accuracy,
          'altitude': position.coords.altitude,
          'speed': position.coords.speed,
          'bearing': position.coords.heading,
          'provider': 'geolocationapi',
          'hashCode': 'dummy',
          'status': true
        });
      }, function(error) {
        onError({
          'status': false,
          'error_code': LOCATION_ERROR[error.code],
          'error_message': error.message
        });
      }, {
        'enableHighAccuracy': true
      });
    } else {
      onError({
        'status': false,
        'error_code': 'not_available',
        'error_message': 'Since this device does not have any location provider, this app can not detect your location.'
      });
    }
  }
};


require('cordova/exec/proxy').add('PluginLocationService', module.exports);
