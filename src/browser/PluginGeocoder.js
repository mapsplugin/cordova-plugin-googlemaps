

var utils = require('cordova/utils');
var event = require('cordova-plugin-googlemaps.event');
var BaseArrayClass = require('cordova-plugin-googlemaps.BaseArrayClass');

var geocoder = null;
var lastRequestTime = 0;
var QUEUE = new BaseArrayClass();
var totalCnt = 0;
QUEUE.on('insert_at', function() {
  if (QUEUE.getLength() === 1) {
    this.trigger('next');
  }
});
QUEUE.one('insert_at', function() {
  geocoder = new google.maps.Geocoder();
  this.trigger('next');
});
QUEUE.on('next', function() {
  var self = QUEUE;
  if (!geocoder || self._executing || QUEUE.getLength() === 0) {
    return;
  }
  if (Date.now() - lastRequestTime < 300) {
    setTimeout(function() {
      self.trigger('next');
    }, 300 + Math.floor(Math.random() * 200));
    return;
  }
  lastRequestTime = Date.now();
  self._executing = true;

  var cmd = QUEUE.removeAt(0, true);
  geocoder.geocode(cmd.geocoderRequest, function(results, status) {
    switch(status) {
      case google.maps.GeocoderStatus.ERROR:
        cmd.onError('[geocoding] Cannot connect to Google servers');
        return;
      case google.maps.GeocoderStatus.INVALID_REQUEST:
        cmd.onError('[geocoding] Invalid request for geocoder');
        return;
      case google.maps.GeocoderStatus.OVER_QUERY_LIMIT:
        QUEUE.insertAt(0, cmd);
        console.warn('[geocoding] Due to the OVER_QUERY_LIMIT error, wait 3 sec, then try again.');
        setTimeout(function() {
          self._executing = false;
          self.trigger('next');
        }, 3000 + Math.floor(Math.random() * 200));
        return;
      case google.maps.GeocoderStatus.REQUEST_DENIED:
        cmd.onError('[geocoding] Google denited your geocoding request.');
        return;
      case google.maps.GeocoderStatus.UNKNOWN_ERROR:
        cmd.onError('[geocoding] There was an unknown error. Please try again.');
        return;
    }

    var pluginResults = results.map(function(geocoderResult) {
      var result = {
        'position': {
          'lat': geocoderResult.geometry.location.lat(),
          'lng': geocoderResult.geometry.location.lng()
        },
        extra: {
          lines: []
        }
      };
      if (geocoderResult.place_id) {
        result.extra.place_id = geocoderResult.place_id;
      }
      if (geocoderResult.plus_code) {
        result.extra.plus_code = geocoderResult.plus_code;
      }
      if (geocoderResult.types) {
        result.extra.types = geocoderResult.types;
      }

      var administrative_area = [];
      var sublocality_area = [];
      var idx;
      geocoderResult.address_components.forEach(function(addrComp) {
        result.extra.lines.push(addrComp.long_name);
        if (!result.locality && addrComp.types.indexOf("locality") > -1) {
          result.locality = addrComp.short_name;
        }
        if (addrComp.types.indexOf("administrative_area_level_1") > -1 ||
            addrComp.types.indexOf("administrative_area_level_2") > -1 ||
            addrComp.types.indexOf("administrative_area_level_3") > -1 ||
            addrComp.types.indexOf("administrative_area_level_4") > -1) {
          addrComp.types.forEach(function(type) {
            if (type.indexOf("administrative_area_level_") === 0) {
              var idx = parseInt(type.replace("administrative_area_level_", ""), 10);
              administrative_area[idx - 1] = addrComp.long_name;
            }
          });
        }
        if (addrComp.types.indexOf("sublocality_level_1") > -1 ||
            addrComp.types.indexOf("sublocality_level_2") > -1 ||
            addrComp.types.indexOf("sublocality_level_3") > -1 ||
            addrComp.types.indexOf("sublocality_level_4") > -1 ||
            addrComp.types.indexOf("sublocality_level_5") > -1) {
          addrComp.types.forEach(function(type) {
            if (type.indexOf("sublocality_level_") === 0) {
              var idx = parseInt(type.replace("sublocality_level_", ""), 10);
              sublocality_area[idx - 1] = addrComp.long_name;
            }
          });
        }
        if (!result.country && addrComp.types.indexOf("country") > -1) {
          result.country = addrComp.long_name;
          result.countryCode = addrComp.short_name;
        }
        if (!result.postalCode && addrComp.types.indexOf("postal_code") > -1) {
          result.postalCode = addrComp.long_name;
        }
        if (!result.postalCode && addrComp.types.indexOf("postal_code") > -1) {
          result.postalCode = addrComp.long_name;
        }
        if (!result.thoroughfare && addrComp.types.indexOf("street_address") > -1) {
          result.thoroughfare = addrComp.long_name;
        }
      });

      if (administrative_area.length > 0) {
        result.adminArea = administrative_area.shift();
        result.subAdminArea = administrative_area.join(",");
      }
      if (sublocality_area.length > 0) {
        result.subLocality = sublocality_area.join(",");
      }
      //result.extra = geocoderResult.address_components; // for debug

      return result;
    });

    self._executing = false;
    cmd.onSuccess({
      'idx': cmd.pluginRequest.idx,
      'results': pluginResults
    });

    // Insert delay to prevent the OVER_QUERY_LIMIT error.
    var delay = 300 + Math.floor(Math.random() * 200);
    setTimeout(function() {
      self._executing = false;
      self.trigger('next');
    }, delay);
  });
});

module.exports = {
  'geocode': function(onSuccess, onError, args) {
    var request = args[0];
    var geocoderRequest = {};

    if (!request.position && request.address) {
      //---------------------
      // Geocoding
      //---------------------
      if (request.bounds && Array.isArray(request.bounds)) {
        var bounds = new google.maps.LatLngBounds();
        request.bounds.forEach(function(position) {
          bounds.extend(position);
        });
        geocoderRequest.bounds = bounds;
      }
      geocoderRequest.address = request.address;
    }
    if (request.position && !request.address) {
      geocoderRequest.location = request.position;
    }
    QUEUE.push({
      'geocoderRequest': geocoderRequest,
      'pluginRequest': request,
      'onSuccess': onSuccess,
      'onError': onError
    });
  }
};


require('cordova/exec/proxy').add('PluginGeocoder', module.exports);
