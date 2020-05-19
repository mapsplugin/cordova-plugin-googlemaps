

/*****************************************************************************
 * DirectionsService class
 *****************************************************************************/
function _cnv_DirectionsRoute(route) {
  if (route.legs) {
    route.legs = route.legs.map(function(leg) {
      return _cnv_DirectionsLeg(leg);
    });
  }
  return route;
}

function _cnv_DirectionsLeg(leg) {
  if (leg.arrival_time) {
    leg.arrival_time.value = new Date(leg.arrival_time.value);
  }
  if (leg.departure_time) {
    leg.departure_time.value = new Date(leg.departure_time.value);
  }
  if (leg.steps) {
    leg.steps = leg.steps.map(function(step) {
      return _cnv_DirectionsStep(step);
    });
  }
  return leg;
}

function _cnv_DirectionsStep(step) {
  if (step.steps) {
    step.steps = step.steps.map(function(s) {
      return _cnv_DirectionsStep(s);
    });
  }
  if (!step.instructions && step.html_instructions) {
    step.instructions = step.html_instructions;
    delete step.html_instructions;
  }
  if (step.transit) {
    if (step.transit.arrival_time) {
      step.transit.arrival_time.value = new Date(step.transit.arrival_time.value);
    }
    if (step.transit.departure_time) {
      step.transit.departure_time.value = new Date(step.transit.departure_time.value);
    }
  }

  return step;
}

function _cnv_DirectionsRequestLocation(position) {
  if (typeof position === 'string') {
    position = {
      'type': 'string',
      'value': position
    };
  } else if (position.lat && position.lng) {
    position = {
      'type': 'location',
      'value': {
        'lat': position.lat,
        'lng': position.lng
      }
    };
  } else {
    var place = {};
    if (position.location) {
      place.location = {
        'lat': position.location.lat,
        'lng': position.location.lng
      };
    }
    if (position.placeId) {
      place.placeId = position.placeId;
    }
    if (position.query) {
      place.query = position.query;
    }
    position = {
      'type': 'place',
      'value': place
    };
  }
  return position;
}

var DirectionsService = function(exec) {

  return {
    route: function(request, callback, errorCallback) {
      var self = this;

      if (!request) {
        throw new Error('route needs request parameter');
      }
      if (!request.destination) {
        throw new Error('route() needs request.destination parameter');
      }
      if (!request.origin) {
        throw new Error('route() needs request.origin parameter');
      }
      if (!request.travelMode) {
        throw new Error('route() needs request.travelMode parameter');
      }

      request.origin = _cnv_DirectionsRequestLocation(request.origin);
      request.destination = _cnv_DirectionsRequestLocation(request.destination);

      if (request.drivingOptions && typeof request.drivingOptions.departureTime === 'date') {
        request.drivingOptions.departureTime = request.drivingOptions.departureTime.getTime();
      }
      if (request.transitOptions && typeof request.transitOptions.departureTime === 'date') {
        request.transitOptions.departureTime = request.transitOptions.departureTime.getTime();
      }
      if (request.transitOptions && typeof request.transitOptions.arrivalTime === 'date') {
        request.transitOptions.arrivalTime = request.transitOptions.arrivalTime.getTime();
      }
      if (request.waypoints) {
        request.waypoints = request.waypoints.map(function(waypoint) {
          if (waypoint.lat && waypoint.lng) {
            waypoint = {
              stopover: false,
              location: {
                'type': 'location',
                'value': waypoint
              }
            };
          } else if (waypoint.location) {
            waypoint.location = _cnv_DirectionsRequestLocation(waypoint.location);
          }
          return waypoint;
        });
      }

      var resolver = function (resolve, reject) {
        exec.call({
          _isReady: true
        }, function (result) {
          result = result || {};
          result.routes = result.routes.map(function(route) {
            return _cnv_DirectionsRoute(route);
          });
          result.request = request;
          resolve(result);
        }, reject, 'PluginDirectionsService', 'route', [request]);
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


module.exports = DirectionsService;
