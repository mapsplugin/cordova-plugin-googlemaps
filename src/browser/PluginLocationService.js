

var utils = require('cordova/utils');
var event = require('cordova-plugin-googlemaps.event');
var BaseClass = require('cordova-plugin-googlemaps.BaseClass');


module.exports = {
  'getMyLocation': function(onSuccess, onError, args) {

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(function(position) {
        onSuccess({
          'latLng': {
            'lat': position.latitude,
            'lng': position.longitude
          },
          'elapsedRealtimeNanos': 0,
          'time': position.timestamp,
          'accuracy': position.accuracy,
          'altitude': position.altitude,
          'speed': position.speed,
          'bearing': position.heading,
          'provider': 'geolocationapi',
          'hashCode': 'dummy',
          'status': true
        });
      }, function(error) {
        onError({
          'status': false,
          'error_code': LOCATION_ERROR[error.code],
          'error_message': LOCATION_ERROR_MSG[error.code]
        });
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
