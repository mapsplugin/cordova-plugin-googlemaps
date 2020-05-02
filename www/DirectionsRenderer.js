

var utils = require('cordova/utils'),
  event = require('./event'),
  BaseClass = require('./BaseClass'),
  BaseArrayClass = require('./BaseArrayClass'),
  VisibleRegion = require('./VisibleRegion'),
  spherical = require('./spherical'),
  encoding = require('./encoding');

/*****************************************************************************
 * DirectionsRenderer Class
 *****************************************************************************/
var DirectionsRenderer = function(map, exec, options) {
  BaseClass.apply(this);

  var self = this;
  //self.set("visible", DirectionsRendererOptions.visible === undefined ? true : DirectionsRendererOptions.visible);
  //self.set("zIndex", DirectionsRendererOptions.zIndex || 0);

  Object.defineProperty(self, '_isReady', {
    value: true,
    writable: false
  });
  Object.defineProperty(self, 'type', {
    value: 'DirectionsRenderer',
    writable: false
  });
  Object.defineProperty(self, 'map', {
    value: map,
    writable: false
  });
  Object.defineProperty(self, 'exec', {
    value: exec,
    writable: false
  });
  Object.defineProperty(self, 'polyline', {
    value: self.map.addPolyline({
      'points': [],
      'color': '#AA00FF',
      'width': 10
    }),
    writable: false
  });
  self.set('overview_path', []);
  Object.defineProperty(self, 'waypoints', {
    value: new BaseArrayClass(),
    writable: false
  });
  Object.defineProperty(self, 'waypointMarkers', {
    value: new BaseArrayClass(),
    writable: false
  });
  self.set('options', options);
  self.set('requestingFlag', false);

  self.on('routeIndex_changed', self._redrawRoute.bind(self));
  self.on('overview_path_changed', self._redrawPolyline.bind(self));
  self.waypoints.on('insert_at', self._waypoint_created.bind(self));
  self.orders = [];
};

utils.extend(DirectionsRenderer, BaseClass);

DirectionsRenderer.prototype.getId = function () {
  return this.__pgmId;
};

DirectionsRenderer.prototype._waypoint_created = function(index) {
  var self = this;
  var position = self.waypoints.getAt(index);
  var marker = self.map.addMarker({
    'position': position,
    'draggable': true,
    'idx': index
  });
  self.orders.push(index);
  marker.on('marker_drag_end', self._onWaypointMoved.bind(self, marker));
  self.waypointMarkers.push(marker);
};

DirectionsRenderer.prototype._onWaypointMoved = function(marker) {
  var self = this;
  var position = marker.getPosition();
  var index = marker.get('idx');
  self.waypoints.setAt(index, position);

  var requestingFlag = self.get('requestingFlag');
  if (requestingFlag) return;
  self.set('requestingFlag', true);

  var options = self.get('options');
  var points = self.waypoints.getArray();
  var start_location = points.shift();
  var end_location = points.pop();

  plugin.google.maps.DirectionsService.route({
    'origin': start_location,
    'destination': end_location,
    'travelMode': options.directions.request.travelMode || 'DRIVING',
    'waypoints': points
  }, function(result) {

    var stepOverlays = new BaseArrayClass();
    var route = result.routes[0];
    // options.directions.routes[routeIdx] = route;

    var overview_path = route.overview_polyline;
    if (typeof overview_path === 'object' && route.overview_polyline.points) {
      overview_path = route.overview_polyline.points;
    }
    var overview_path = encoding.decodePath(overview_path);

    if (!options.preserveViewport) {
      setTimeout(function() {
        self.map.animateCamera({
          'target': route.bounds,
          'duration': 1000
        });
      }, 100);
    }
    // prevent the OVER_QUERY_LIMIT
    setTimeout(function() {
      self.set('requestingFlag', false);
    }, 1000);
    self.set('overview_path', overview_path);
  });
};

DirectionsRenderer.prototype.setRouteIndex = function(index) {
  var self = this;
  self.set('routeIndex', index);
};

DirectionsRenderer.prototype._redrawRoute = function(oldIdx, newIdx) {
  var self = this;
  var options = self.get('options');
  var n = options.directions.routes.length;

  if (newIdx < 0 || newIdx > n - 1) {
    return;
  }
  var stepOverlays = new BaseArrayClass();
  var route = options.directions.routes[newIdx];

  var overview_path = route.overview_polyline;
  if (typeof overview_path === 'object' && route.overview_polyline.points) {
    overview_path = route.overview_polyline.points;
  }
  overview_path = encoding.decodePath(overview_path);
  self.set('overview_path', overview_path);

  var waypointsRef = self.waypoints;
  waypointsRef.empty();

  var idx = 0;
  var lastPosition = route.legs[0].steps[0].start_location;
  waypointsRef.push(lastPosition);
  var n = route.legs[0].steps.length;
  route.legs[0].steps.forEach(function(step) {
    var pos = step.end_location;
    if (spherical.computeDistanceBetween(lastPosition, pos) < 100 && idx < n) {
      return;
    }
    lastPosition = pos;

    waypointsRef.push(pos);
  });

  if (!options.preserveViewport) {
    setTimeout(function() {
      self.map.animateCamera({
        'target': route.bounds,
        'duration': 1000
      });
    }, 100);
  }
};

DirectionsRenderer.prototype._redrawPolyline = function(marker) {
  var self = this;
  var polyline = self.polyline;
  var overview_path = self.get('overview_path');
  polyline.setPoints(overview_path);
};
module.exports = DirectionsRenderer;
