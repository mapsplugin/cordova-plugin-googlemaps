

var BaseArrayClass = require('cordova-plugin-googlemaps.BaseArrayClass');

function _cnv_DirectionsRoute(route) {
  if (route.bounds) {
    route.bounds = {
      'southwest' : {
        'lat': route.bounds.getSouthWest().lat(),
        'lng': route.bounds.getSouthWest().lng()
      },
      'northeast' : {
        'lat': route.bounds.getNorthEast().lat(),
        'lng': route.bounds.getNorthEast().lng()
      }
    };
  }
  if (route.overview_path) {
    route.overview_path = route.overview_path.map(function(latLng) {
      return {
        'lat': latLng.lat(),
        'lng': latLng.lng()
      };
    });
  }
  if (route.legs) {
    route.legs = route.legs.map(_cnv_DirectionsLeg);
  }
  return route;
}

function _cnv_DirectionsLeg(leg) {
  if (leg.arrival_time) {
    leg.arrival_time.value = leg.arrival_time.value.getTime();
  }
  if (leg.departure_time) {
    leg.departure_time.value = leg.departure_time.value.getTime();
  }
  if (leg.end_location) {
    leg.end_location = {
      'lat': leg.end_location.lat(),
      'lng': leg.end_location.lng()
    };
  }
  if (leg.start_location) {
    leg.start_location = {
      'lat': leg.start_location.lat(),
      'lng': leg.start_location.lng()
    };
  }
  if (leg.steps) {
    leg.steps = leg.steps.map(_cnv_DirectionsStep);
  }
  if (leg.via_waypoints) {
    leg.via_waypoints = leg.via_waypoints.map(function(latLng) {
      return {
        'lat': latLng.lat(),
        'lng': latLng.lng()
      };
    });
  }
  return leg;
}

function _cnv_DirectionsStep(step) {
  if (step.start_location) {
    step.start_location = {
      'lat': step.start_location.lat(),
      'lng': step.start_location.lng()
    };
  }
  if (step.end_location) {
    step.end_location = {
      'lat': step.end_location.lat(),
      'lng': step.end_location.lng()
    };
  }
  if (step.path) {
    step.path = step.path.map(function(latLng) {
      return {
        'lat': latLng.lat(),
        'lng': latLng.lng()
      };
    });
  }
  if (step.steps) {
    step.steps = step.steps.map(_cnv_DirectionsStep);
  }
  if (step.transit) {
    step.steps = _cnv_TransitDetails(step.transit);
  }
  return step;
}
function _cnv_TransitDetails(transit) {

  if (transit.arrival_stop) {
    transit.arrival_stop.location = {
      'lat': transit.arrival_stop.location.lat(),
      'lng': transit.arrival_stop.location.lng()
    };
  }
  if (transit.departure_stop) {
    transit.departure_stop.location = {
      'lat': transit.departure_stop.location.lat(),
      'lng': transit.departure_stop.location.lng()
    };
  }
  if (transit.arrival_time) {
    transit.arrival_time.value = transit.arrival_time.value.getTime();
  }
  if (transit.departure_time) {
    transit.departure_time.value = transit.departure_time.value.getTime();
  }
  return transit;
}

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
  service = new google.maps.DirectionsService();
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

  service.route(cmd.request, function(result, status) {
    switch(status) {
    case google.maps.DirectionsStatus.INVALID_REQUEST:
      self._executing = false;
      cmd.onError('[directions] The DirectionsRequest provided was invalid.');
      return;
    case google.maps.DirectionsStatus.MAX_WAYPOINTS_EXCEEDED:
      self._executing = false;
      cmd.onError('[directions] Too many DirectionsWaypoints were provided in the DirectionsRequest.');
      return;
    case google.maps.DirectionsStatus.NOT_FOUND:
      self._executing = false;
      cmd.onError('[directions] At least one of the origin, destination, or waypoints could not be geocoded.');
      return;
    case google.maps.DirectionsStatus.OVER_QUERY_LIMIT:
      QUEUE.insertAt(0, cmd);
      console.warn('[directions] Due to the OVER_QUERY_LIMIT error, wait 3 sec, then try again.');
      setTimeout(function() {
        self._executing = false;
        self.trigger('next');
      }, 3000 + Math.floor(Math.random() * 200));
      return;
    case google.maps.DirectionsStatus.REQUEST_DENIED:
      self._executing = false;
      cmd.onError('[directions] Google denited your directions request.');
      return;
    case google.maps.DirectionsStatus.UNKNOWN_ERROR:
      self._executing = false;
      cmd.onError('[directions] There was an unknown error. Please try again.');
      return;
    case google.maps.DirectionsStatus.ZERO_RESULTS:
      self._executing = false;
      cmd.onError('[directions] No route could be found between the origin and destination.');
      return;
    }

    if (result.routes) {
      result.routes = result.routes.map(_cnv_DirectionsRoute);
    }

    self._executing = false;
    cmd.onSuccess(result);

    // Insert delay to prevent the OVER_QUERY_LIMIT error.
    var delay = 300 + Math.floor(Math.random() * 200);
    setTimeout(function() {
      self._executing = false;
      self.trigger('next');
    }, delay);
  });

});

function _decode_DirectionsRequestLocation(position) {
  if (position.type === 'string') {
    return position.value;
  } else if (position.type === 'location') {
    return {
      'lat': position.value.lat,
      'lng': position.value.lng
    };
  } else {
    var place = {};
    if (position.value.location) {
      place.location = position.value.location;
    }
    if (position.value.placeId) {
      place.placeId = position.value.placeId;
    }
    if (position.value.query) {
      place.query = position.value.query;
    }
    return place;
  }
}

module.exports = {
  'route': function(onSuccess, onError, args) {
    var request = args[0];

    request.origin = _decode_DirectionsRequestLocation(request.origin);
    request.destination = _decode_DirectionsRequestLocation(request.destination);
    if (request.waypoints) {
      request.waypoints = request.waypoints.map(function(point) {
        point.location = _decode_DirectionsRequestLocation(point.location);
        return point;
      });
    }

    if (request.drivingOptions && typeof request.drivingOptions.departureTime === 'number') {
      request.drivingOptions.departureTime = new Date(request.drivingOptions.departureTime);
    }
    if (request.transitOptions && typeof request.transitOptions.departureTime === 'number') {
      request.transitOptions.departureTime = new Date(request.transitOptions.departureTime);
    }
    if (request.transitOptions && typeof request.transitOptions.arrivalTime === 'number') {
      request.transitOptions.arrivalTime = new Date(request.transitOptions.arrivalTime);
    }

    QUEUE.push({
      'request': request,
      'onSuccess': onSuccess,
      'onError': onError
    });
  }
};


require('cordova/exec/proxy').add('PluginDirectionsService', module.exports);
