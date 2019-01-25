/*****************************************************************************
 * LocationService class
 *****************************************************************************/
var LatLng = require('./LatLng');

var LocationService = function(exec) {
  function _errorHandler(err) {
    console.error(err);
  }
  return {
    hasPermission: function(callback, errorCallback) {
      var self = this;

      var resolver = function(resolve, reject) {
        exec.call({
          _isReady: true
        },
        function(hasPermission) {
          resolve.call(self, hasPermission === 1);
        },
        reject.bind(self), 'PluginLocationService', 'hasPermission', [], {sync: true});
      };

      var errorHandler = function(result) {
        if (typeof errorCallback === 'function') {
          errorCallback.call(self, result);
        } else {
          (self.errorHandler || _errorHandler).call(self, result);
        }
      };
      if (typeof callback === 'function') {
        resolver(callback, errorHandler);
        return self;
      } else {
        return new Promise(resolver);
      }
    },
    getMyLocation: function(params, success_callback, errorCallback) {
      var self = this;
      var args = [params || {}, success_callback || null, errorCallback];
      if (typeof args[0] === 'function') {
        args.unshift({});
      }
      params = args[0];
      success_callback = args[1];
      errorCallback = args[2];


      var resolver = function(resolve, reject) {
        exec.call({
          _isReady: true
        },
        function(location) {
          location.latLng = new LatLng(location.latLng.lat, location.latLng.lng);
          resolve.call(self, location);
        },
        reject.bind(self), 'PluginLocationService', 'getMyLocation', [params], {sync: true});
      };

      params.enableHighAccuracy = params.enableHighAccuracy === true;
      var errorHandler = function(result) {
        if (typeof errorCallback === 'function') {
          errorCallback.call(self, result);
        } else {
          (self.errorHandler || _errorHandler).call(self, result);
        }
      };

      if (typeof success_callback === 'function') {
        resolver(success_callback, errorHandler);
        return self;
      } else {
        return new Promise(resolver);
      }
    }
  };
};

/**
 // TODO:
LocationService.prototype.followMyPosition = function(params, success_callback, errorCallback) {
  var self = this;
  var args = [params || {}, success_callback || null, errorCallback];
  if (typeof args[0] === 'function') {
      args.unshift({});
  }
  self.on('currentPosition_changed', success_callback);
  var successHandler = function(location) {
      location.latLng = new LatLng(location.latLng.lat, location.latLng.lng);
      if (typeof success_callback === 'function') {
          success_callback.call(self, location);
      }
      self.set('currentPosition', location);
  };
  var errorHandler = function(result) {
      if (typeof errorCallback === 'function') {
          errorCallback.call(self, result);
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
