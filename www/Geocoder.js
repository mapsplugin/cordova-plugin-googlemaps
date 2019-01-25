

/*****************************************************************************
 * Geocoder class
 *****************************************************************************/
var common = require('./Common'),
  utils = require('cordova/utils');

var Geocoder = function (exec) {

  return {
    geocode: function (geocoderRequest, callback) {

      geocoderRequest = geocoderRequest || {};
      var requestProperty = null;
      if ('position' in geocoderRequest && !('address' in geocoderRequest)) {
        requestProperty = 'position';
      } else if ('address' in geocoderRequest && !('position' in geocoderRequest)) {
        requestProperty = 'address';
      }
      if (!requestProperty) {
        var error = new Error('Invalid request');
        if (typeof callback === 'function') {
          callback([], error);
        } else {
          return Promise.reject(error);
        }
      }

      if (geocoderRequest[requestProperty] instanceof Array || Array.isArray(geocoderRequest[requestProperty])) {
        //-------------------------
        // Geocoder.geocode({
        //   address: [
        //    'Kyoto, Japan',
        //    'Tokyo, Japan'
        //   ]
        // })
        //-------------------------

        var mvcResults = common.createMvcArray();
        for (var i = 0; i < requestCnt; i++) {
          mvcResults.push(-1, true);
        }

        // Execute geocoder.geocode() when a new request is instearted.
        var requestCnt = geocoderRequest[requestProperty].length;
        var requests = common.createMvcArray();
        requests.on('insert_at', function (idx) {
          var request = requests.getAt(idx);
          request.idx = idx;
          exec.call({
            _isReady: true
          }, function (result) {

            mvcResults.insertAt(result.idx, result.results);
            if (mvcResults.getLength() === requestCnt) {
              var tmp = mvcResults.getArray();
              var tmp2 = tmp.filter(function (ele) {
                return ele === -1;
              });
              if (tmp2.length === 0) {
                // Notifies `finish` event when all results are received.
                mvcResults.trigger('finish');
              }
            }

          }, function (error) {
            mvcResults.trigger('error', error);
          }, 'PluginGeocoder', 'geocode', [request]);
        });

        var request;
        var baseRequest = utils.clone(geocoderRequest);
        delete baseRequest[requestProperty];
        for (i = 0; i < requestCnt; i++) {
          request = utils.clone(baseRequest);
          request[requestProperty] = geocoderRequest[requestProperty][i];
          request.idx = i;
          if (requestProperty === 'position') {
            request.position.lat = request.position.lat || 0.0;
            request.position.lng = request.position.lng || 0.0;
          }
          requests.push(request);
        }
        if (typeof callback === 'function') {
          callback(mvcResults);
        } else {
          return Promise.resolve(mvcResults);
        }
      } else {
        //-------------------------
        // Geocoder.geocode({
        //   address: 'Kyoto, Japan'
        // })
        //-------------------------
        if (requestProperty === 'position') {
          geocoderRequest.position.lat = geocoderRequest.position.lat || 0.0;
          geocoderRequest.position.lng = geocoderRequest.position.lng || 0.0;
        }
        geocoderRequest.idx = -1;

        var resolver1 = function (resolve, reject) {
          exec.call({
            _isReady: true
          }, function (_results) {
            resolve(_results.results);
          }, reject, 'PluginGeocoder', 'geocode', [geocoderRequest]);
        };

        if (typeof callback === 'function') {
          resolver1(callback, function (error) {
            callback([], error);
          });
        } else {
          return new Promise(resolver1);
        }

      }
    }

  };
};

module.exports = Geocoder;
