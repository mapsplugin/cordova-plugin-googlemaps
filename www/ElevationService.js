
/*****************************************************************************
 * ElevationService class
 *****************************************************************************/
var LatLng = require('./LatLng');

var ElevationService = function(exec) {
  function _errorHandler(err) {
    console.error(err);
  }
  return {
    getElevationAlongPath: function(request, callback, errorCallback) {
      var self = this;

      if (!request) {
        return errorHandler('getElevationAlongPath needs request parameter');
      }
      if (!request.samples) {
        return errorHandler('getElevationAlongPath needs request.samples parameter');
      }
      if (typeof request.samples !== 'number' ||
          request.samples < 2 || request.samples > 512) {
        return errorHandler('getElevationAlongPath needs request.samples must be a value between 2 and 512 inclusive');
      }

      var path = request.path;
      if (request.path && request.path.type === 'BaseArrayClass') {
        path = request.path.getArray();
      }
      for (var i = 0; i < path.length; i++) {
        path[i] = {
          lat: path[i].lat,
          lng: path[i].lng
        };
      }
      if (path instanceof Array && !Array.isArray(request.path)) {
        return errorHandler('getElevationAlongPath needs request.path must be an ILatLng array');
      }


      var params = {
        'path': path,
        samples: request.samples
      };

      var resolver = function (resolve, reject) {
        exec.call({
          _isReady: true
        }, function (_results) {
          resolve(_results.results);
        }, reject, 'PluginElevationService', 'getElevationAlongPath', [params]);
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

    getElevationForLocations: function(request, callback, errorCallback) {
      var self = this;

      if (!request) {
        return errorHandler('getElevationForLocations needs request parameter');
      }

      var locations = request.locations;
      if (request.locations && request.locations.type === 'BaseArrayClass') {
        locations = request.locations.getArray();
      }
      for (var i = 0; i < locations.length; i++) {
        locations[i] = {
          lat: locations[i].lat,
          lng: locations[i].lng
        };
      }
      if (locations instanceof Array && !Array.isArray(request.locations)) {
        return errorHandler('getElevationAlongPath needs request.locations must be an ILatLng array');
      }


      var params = {
        'locations': locations
      };

      var resolver = function (resolve, reject) {
        exec.call({
          _isReady: true
        }, function (_results) {
          resolve(_results.results);
        }, reject, 'PluginElevationService', 'getElevationForLocations', [params]);
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
    }
  };
};


module.exports = ElevationService;
