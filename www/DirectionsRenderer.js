

var utils = require('cordova/utils'),
  event = require('./event'),
  BaseClass = require('./BaseClass'),
  BaseArrayClass = require('./BaseArrayClass'),
  VisibleRegion = require('./VisibleRegion'),
  spherical = require('./spherical'),
  encoding = require('./encoding');

var _decodePolyline = function(property) {
  if (typeof property === 'object' && property.points) {
    return encoding.decodePath(property.points);
  } else if (typeof property === 'string') {
    return encoding.decodePath(property);
  }
};

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
  Object.defineProperty(self, 'waypoints', {
    value: new BaseArrayClass(),
    writable: false
  });
  Object.defineProperty(self, 'markers', {
    value: new BaseArrayClass(),
    writable: false
  });
  Object.defineProperty(self, 'waypoints', {
    value: new BaseArrayClass(),
    writable: false
  });

  Object.defineProperty(self, 'pathList', {
    value: new BaseArrayClass(),
    writable: false
  });
  Object.defineProperty(self, 'pathCollection', {
    value: new BaseArrayClass(),
    writable: false
  });
  self.set('options', options);
  self.set('draggable', options.draggable || false);
  self.set('requestingFlag', false);
  self.set('routeIndex', options.routeIndex || 0, false);

  self.on('panel_changed', self._panel_changed.bind(self));
  self.set('panel', options.panel);

  self.on('routeIndex_changed', self._redrawRoute.bind(self));
  self.on('overview_path_changed', self._redrawPolyline.bind(self));
  self.waypoints.on('insert_at', self._waypoint_created.bind(self));
  self.waypoints.on('remove_at', self._waypoint_removed.bind(self));
  self.pathList.on('insert_at', self._pathList_created.bind(self));
  self.pathList.on('remove_at', self._pathList_removed.bind(self));
  self.pathList.on('set_at', self._pathList_updated.bind(self));

  self.trigger('routeIndex_changed');
};

utils.extend(DirectionsRenderer, BaseClass);

DirectionsRenderer.prototype.getId = function () {
  return this.__pgmId;
};

DirectionsRenderer.prototype._panel_changed = function(oldDivId, newDivId) {
  var self = this;
  if (oldDivId) {
    var oldDiv = document.getElementById(oldDivId);
    if (oldDiv) {
      oldDivId.innerHTML = '';
    }
  }
  if (newDivId) {
    var options = self.get('options');

    var newDiv = document.getElementById(newDivId);
    if (newDiv && options.directions.routes.length) {


      var routeIndex = self.get('routeIndex');
      var shadowRoot = newDiv.attachShadow({mode: 'open'});

      var style = document.createElement('style');
      shadowRoot.appendChild(style);
      style.innerHTML = `
        div.summary1 {
          color: green;
          font-size: 1.5em;
          padding-top: 5%;
          padding-bottom: 2%;
        }
        div.summary2 {
          color: green;
          font-size: 1em;
        }
        ul.steps {
          list-style-type: none;
          padding: 0%;
        }
        li.step {
          padding: 0.5em;
          border-bottom: 1px solid #ccc;
        }
        li:last-child {
          border-bottom: none;
        }
      `;

      var container = document.createElement('div');
      container.style.position = 'relative';
      container.style.overflowY = 'scroll';
      container.style.width = '90%';
      container.style.height = '100%';
      container.style.padding = '0% 5%';
      shadowRoot.appendChild(container);

      var summary = document.createElement('div');
      summary.style.width = '100%';


      var summary1 = document.createElement('div');
      summary1.classList.add('summary1');
      summary1.innerHTML = [
        options.directions.routes[routeIndex].legs[0].duration.text,
        options.directions.routes[routeIndex].legs[0].distance.text
      ].join(' / ');
      summary.appendChild(summary1);

      var eta = new Date(Date.now() + options.directions.routes[routeIndex].legs[0].duration.value);
      var summary2 = document.createElement('div');
      summary2.classList.add('summary2');
      summary2.innerHTML = eta;
      summary.appendChild(summary2);

      container.appendChild(summary);

      var ul = document.createElement('ul');
      ul.classList.add('steps');

      options.directions.routes[routeIndex].legs[0].steps.forEach(function(step) {
        var stepLi = document.createElement('li');
        stepLi.classList.add('step');

        var stepSummary = document.createElement('div');
        stepSummary.style.width = '100%';
        stepSummary.innerHTML = [
          step.duration.text,
          step.distance.text,
        ].join(" / ");
        stepLi.appendChild(stepSummary);

        var stepInstruction = document.createElement('div');
        stepInstruction.style.width = '100%';
        stepInstruction.innerHTML = step.instructions;
        stepLi.appendChild(stepInstruction);

        ul.appendChild(stepLi);
      });
      newDiv.innerHTML = '';
      container.appendChild(ul);
    }
  }
};

DirectionsRenderer.prototype._pathList_created = function(index) {
  var self = this;
  var path = self.pathList.getAt(index);
  var polyline = self.map.addPolyline({
    'points': path,
    'color': '#AA00FF',
    'width': 10
  });
  self.pathCollection.push(polyline);
};

DirectionsRenderer.prototype._pathList_updated = function(index) {
  var self = this;
  var polyline = self.pathCollection.getAt(index);
  var path = self.pathList.getAt(index);
  polyline.setPoints(path);
};

DirectionsRenderer.prototype._pathList_removed = function(index) {
  var self = this;
  var polyline = self.pathCollection.removeAt(index);
  polyline.remove();
};

DirectionsRenderer.prototype._waypoint_created = function(index) {
  var self = this;
  var position = self.waypoints.getAt(index);
  var marker = self.map.addMarker({
    'position': position,
    'idx': index,
    'title': `idx=${index}`
  });
  self.bindTo('draggable', marker);
  marker.on('marker_drag_end', self._onWaypointMoved.bind(self, marker));
  self.markers.push(marker);
};

DirectionsRenderer.prototype._waypoint_removed = function(index) {
  var self = this;
  var marker = self.markers.removeAt(index);
  marker.remove();
};

DirectionsRenderer.prototype._onWaypointMoved = function(marker) {
  var self = this;
  var position = marker.getPosition();
  var index = marker.get('idx');
  self.waypoints.setAt(index, position);

  var requestingFlag = self.get('requestingFlag');
  if (requestingFlag) return;
  self.set('requestingFlag', true);

  var n = self.waypoints.getLength();
  var routeIndex = self.get('routeIndex');

  var options = self.get('options');
  var points = [];
  if (index == 0) {
    points.push(self.waypoints.getAt(0));
    points.push(self.waypoints.getAt(1));
  } else if (index == n - 1) {
    points.push(self.waypoints.getAt(n - 2));
    points.push(self.waypoints.getAt(n - 1));
  } else {
    points.push(self.waypoints.getAt(index - 1));
    points.push(self.waypoints.getAt(index));
    points.push(self.waypoints.getAt(index + 1));
  }


  plugin.google.maps.DirectionsService.route({
    'origin': points.shift(),
    'destination': points.pop(),
    'travelMode': options.directions.request.travelMode || 'DRIVING',
    'waypoints': points
  }, function(result) {

    // Redraw the polyline
    options.directions.routes = result.routes;
    var route = options.directions.routes[routeIndex];
    var leg = route.legs[0];

    var currentPath = [];
      console.log(`index = ${index}`);
    if (index == 0) {
      leg.steps.forEach(function(step) {
        currentPath = currentPath.concat(_decodePolyline(step.polyline));
      });
      self.pathList.setAt(0, currentPath);
    } else if (index == n - 1) {
      leg.steps.forEach(function(step) {
        currentPath = currentPath.concat(_decodePolyline(step.polyline));
      });
      self.pathList.setAt(index - 1, currentPath);
    } else {
      var i;
      for (i = 0; i < leg.steps.length; i++) {
        currentPath = currentPath.concat(_decodePolyline(leg.steps[i].polyline));
        if (spherical.computeDistanceBetween(position, leg.steps[i].end_location) < 5) {
          break;
        }
      }
      self.pathList.setAt(index - 1, currentPath);

      currentPath = [];
      while (i < leg.steps.length) {
        currentPath = currentPath.concat(_decodePolyline(leg.steps[i++].polyline));
      }
      self.pathList.setAt(index, currentPath);
    }



    // prevent the OVER_QUERY_LIMIT
    setTimeout(function() {
      self.set('requestingFlag', false);
    }, 1000);
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
  var routeIndex = self.get('routeIndex');

  if (newIdx < 0 || newIdx > n - 1) {
    return;
  }
  var stepOverlays = new BaseArrayClass();
  var route = options.directions.routes[routeIndex];

  var waypointsRef = self.waypoints;
  waypointsRef.empty();

  var currentPath = [];
  var idx = 0;
  var lastPosition = route.legs[0].steps[0].start_location;
  currentPath = currentPath.concat(_decodePolyline(route.legs[0].steps[0].polyline));
  waypointsRef.push(lastPosition);
  var n = route.legs[0].steps.length;
  route.legs[0].steps.forEach(function(step, i) {
    if (i == 0) return;

    var pos = step.end_location;
    currentPath = currentPath.concat(_decodePolyline(step.polyline));
    if (spherical.computeDistanceBetween(lastPosition, pos) < 100 && idx < n) {
      return;
    }
    lastPosition = pos;

    self.pathList.push(currentPath.slice(0));
    currentPath = [];
    waypointsRef.push(pos);
  });
  if (currentPath.length > 0) {
    self.pathList.push(currentPath.slice(0));
  }

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
