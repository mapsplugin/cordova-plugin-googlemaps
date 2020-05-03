

var BaseArrayClass = require('cordova-plugin-googlemaps.BaseArrayClass');

var service = null;
var lastRequestTime = 0;
var QUEUE = new BaseArrayClass();
QUEUE.on('insert_at', function() {
  if (!window.google || !window.google.maps) {
    return;
  }
  if (QUEUE.getLength() === 1) {
    this.trigger('next');
  }
});
QUEUE.one('insert_at', function() {

  if (!window.google || !window.google.maps) {
    setTimeout(arguments.callee.bind(this), 100);
    return;
  }
  service = new google.maps.ElevationService();
  this.trigger('next');
});
QUEUE.on('next', function() {
  var self = QUEUE;
  if (!service || self._executing || QUEUE.getLength() === 0) {
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
  var methods = {
    'getElevationAlongPath': service.getElevationAlongPath,
    'getElevationForLocations': service.getElevationForLocations
  };

  var execMethod = methods[cmd.method];
  execMethod.call(service, cmd.elevationRequest, function(result, status) {
    switch(status) {
    case google.maps.ElevationStatus.INVALID_ERROR:
      self._executing = false;
      cmd.onError('[elevation] The request was invalid');
      return;
    case google.maps.ElevationStatus.OVER_QUERY_LIMIT:
      QUEUE.insertAt(0, cmd);
      console.warn('[elevation] Due to the OVER_QUERY_LIMIT error, wait 3 sec, then try again.');
      setTimeout(function() {
        self._executing = false;
        self.trigger('next');
      }, 3000 + Math.floor(Math.random() * 200));
      return;
    case google.maps.ElevationStatus.REQUEST_DENIED:
      self._executing = false;
      cmd.onError('[elevation] Google denited your elevation request.');
      return;
    case google.maps.ElevationStatus.UNKNOWN_ERROR:
      self._executing = false;
      cmd.onError('[elevation] There was an unknown error. Please try again.');
      return;
    }

    var pluginResults = [];
    for (var i = 0; i < result.length; i++) {
      pluginResults.push({
        'elevation': result[i].elevation,
        'location': result[i].location.toJSON(),
        'resolution': result[i].resolution
      });
    }

    self._executing = false;
    cmd.onSuccess({
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
  'getElevationAlongPath': function(onSuccess, onError, args) {
    var request = args[0];
    var params = {};
    params.samples = request.samples;
    params.path = [];
    for (var i = 0; i < request.path.length; i++) {
      params.path.push(request.path[i]);
    }

    QUEUE.push({
      'method': 'getElevationAlongPath',
      'elevationRequest': params,
      'pluginRequest': request,
      'onSuccess': onSuccess,
      'onError': onError
    });
  },

  'getElevationForLocations': function(onSuccess, onError, args) {
    var request = args[0];
    var params = {};
    params.locations = [];
    for (var i = 0; i < request.locations.length; i++) {
      params.locations.push(request.locations[i]);
    }

    QUEUE.push({
      'method': 'getElevationForLocations',
      'elevationRequest': params,
      'pluginRequest': request,
      'onSuccess': onSuccess,
      'onError': onError
    });
  }
};


require('cordova/exec/proxy').add('PluginElevationService', module.exports);
