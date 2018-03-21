/*****************************************************************************
 * LocationService class
 *****************************************************************************/
 var LatLng = require('./LatLng');

var LocationService = function(exec) {

  return {
    getMyLocation: function(params, success_callback, error_callback) {
      var self = this;
      var args = [params || {}, success_callback || null, error_callback];
      if (typeof args[0] === "function") {
          args.unshift({});
      }
      params = args[0];
      success_callback = args[1];
      error_callback = args[2];

      params.enableHighAccuracy = params.enableHighAccuracy === true;
      var successHandler = function(location) {
          if (typeof success_callback === "function") {
              location.latLng = new LatLng(location.latLng.lat, location.latLng.lng);
              success_callback.call(self, location);
          }
      };
      var errorHandler = function(result) {
          if (typeof error_callback === "function") {
              error_callback.call(self, result);
          }
      };
      exec.call({
        _isReady: true
      }, successHandler, errorHandler, 'LocationService', 'getMyLocation', [params], {sync: true});
    }
  };
};

/**
 // TODO:
LocationService.prototype.followMyPosition = function(params, success_callback, error_callback) {
  var self = this;
  var args = [params || {}, success_callback || null, error_callback];
  if (typeof args[0] === "function") {
      args.unshift({});
  }
  self.on('currentPosition_changed', success_callback);
  var successHandler = function(location) {
      location.latLng = new LatLng(location.latLng.lat, location.latLng.lng);
      if (typeof success_callback === "function") {
          success_callback.call(self, location);
      }
      self.set('currentPosition', location);
  };
  var errorHandler = function(result) {
      if (typeof error_callback === "function") {
          error_callback.call(self, result);
      }
  };
  exec.call({
    _isReady: true
  }, successHandler, errorHandler, 'CordovaGoogleMaps', 'followMyPosition', [params], {sync: true});
};

LocationService.prototype.clearFollowing = function() {
  var self = this;
  self.off('currentPosition_changed');
  exec.call({
    _isReady: true
  }, successHandler, errorHandler, 'CordovaGoogleMaps', 'clearFollowing', [params], {sync: true});
};
**/

module.exports = LocationService;
