

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
  } else if (utils.isArray(property)) {
    return property;
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
  options = options || {};
  self.set('options', options);
  self.set('markerOptions', options.markerOptions);

  self.set('directions', options.directions || {
    'routes': []
  });
  self.set('draggable', options.draggable || false);
  self.set('requestingFlag', false);
  self.set('routeIndex', options.routeIndex || 0, false);

  self.on('directions_changed', self._redraw_panel.bind(self));
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

DirectionsRenderer.prototype.getRouteIndex = function () {
  return this.get('routeIndex') || 0;
};
DirectionsRenderer.prototype.getDirections = function () {
  return this.get('directions') || {};
};

DirectionsRenderer.prototype._panel_changed = function(oldDivId, newDivId) {
  var self = this;
  if (oldDivId) {
    var oldDiv = document.getElementById(oldDivId);
    if (oldDiv) {
      oldDivId.innerHTML = '';
    }
  }
  self._redraw_panel();
};

DirectionsRenderer.prototype._redraw_panel = function() {
  var self = this;

  var newDivId = self.get('panel');
  if (newDivId) {
    var directions = self.get('directions');

    var newDiv = document.getElementById(newDivId);
    if (newDiv && directions.routes.length) {

      newDiv.style.position = 'relative';

      var routeIndex = self.get('routeIndex');
      var frame = document.createElement('div');
      frame.style.position = 'absolute';
      frame.style.left = '0px';
      frame.style.top = '0px';
      frame.style.bottom = '0px';
      frame.style.right = '0px';
      frame.style.width = '100%';
      frame.style.height = '100%';
      var shadowRoot = frame.attachShadow({mode: 'open'});

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
        directions.routes[routeIndex].legs[0].duration.text,
        directions.routes[routeIndex].legs[0].distance.text
      ].join(' / ');
      summary.appendChild(summary1);

      var eta = new Date(Date.now() + directions.routes[routeIndex].legs[0].duration.value);
      var summary2 = document.createElement('div');
      summary2.classList.add('summary2');
      summary2.innerHTML = eta;
      summary.appendChild(summary2);

      var copyrights = document.createElement('div');
      copyrights.style.fontSize = '0.8em';
      copyrights.style.color = 'white';
      copyrights.style.padding = '0.5em 0em';
      copyrights.style.textAlign = 'right';
      copyrights.innerHTML = directions.routes[routeIndex].copyrights;
      summary.appendChild(copyrights);

      container.appendChild(summary);

      var ul = document.createElement('ul');
      ul.classList.add('steps');

      directions.routes[routeIndex].legs[0].steps.forEach(function(step) {
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
      container.appendChild(ul);
      newDiv.innerHTML = '';
      newDiv.appendChild(frame);
    }
  }
};

DirectionsRenderer.prototype._pathList_created = function(index) {
  var self = this;
  var path = self.pathList.getAt(index);

  var polylineOpts = Object.create(self.get('polylineOptions') || {});
  delete polylineOpts.points;

  polylineOpts.color = polylineOpts.color || '#0000FF';
  polylineOpts.width = ('width' in polylineOpts) ? polylineOpts.width : 5;
  polylineOpts.points = path;

  var polyline = self.map.addPolyline(polylineOpts);
  self.pathCollection.push(polyline);
};

DirectionsRenderer.prototype._pathList_updated = function(index) {
  var self = this;
  var polyline = self.pathCollection.getAt(index);
  var path = self.pathList.getAt(index);
  polyline.setPoints(path);

  // for test
  //polyline.setStrokeColor(`rgb(${index * 63}, ${index * 63}, ${index * 63})`);

};

DirectionsRenderer.prototype._pathList_removed = function(index) {
  var self = this;
  var polyline = self.pathCollection.removeAt(index);
  polyline.remove();
};

DirectionsRenderer.prototype._waypoint_created = function(index) {
  var self = this;
  var position = self.waypoints.getAt(index);
  var markerOpts = Object.create(self.get('markerOptions') || {});
  markerOpts.position = position;
  markerOpts.idx = index;
  markerOpts.icon = markerOpts.icon || {};
  // if (typeof markerOpts.icon === 'object' && !markerOpts.icon.url) {
  //   markerOpts.icon.url = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAAAmJLR0QA/4ePzL8AAAAHdElNRQfkBRMFExpRC/42AAACY0lEQVRIx6WVv0tbYRSGny80BpWYQTBLO7Q4pAXlWqzU3UDjov+B7VSSdknc/AMcdG2HdjKboy5NwWC3WlooRdFmkBbiEBXkkpgovQm+HfwRr97EK55vuXz3vs85F855jxF3i8Ad9dxr/cpRhWOgkx46TKuvjNcv1PSdPBscUAeC9DLAGCN0Gx+Auj7xkX2e8px+IkCZbb7xkz5ekyB4FSLXKSkpS/PakRAVlVRSRULsaF6WkirJrXBV8EdvCTHLY7OnHCsUqAA9xIiTIGp+a4Z/vOOR8aygpIReyVZDC7IUcCUKyNKCGrL1UglXFRcPjpKalK2q0goJjxNSRlXZmlRSjq4BlmRpSw2lZTzlCBll1NCmLC1fBVQ1rnmJbIvszSqyEnMaV1UuwKqGtaNdWW3lCFnaVVHDWj0DnLVyniHumxzrN7buOjkemCHyl2fB0QajQJ6TGwEn5IFRNnB0AahwQD+HKvganwKH6ueASrOCI+pEqFH2BShTI0Kd4ybA+BJ6RwCgkyBluon4kkTopkyQriagh162CZuYL0CMsNmml3AT0GEGWAPiPgwqQBxYY/DcZJqNVLxVI31xN9IIfSwSNWlCbfOHSBM1i/Tx7PzqHL0sS5tqKNN2mKZbDZNwlNKEbFWVaTnO06rJ1oRSXuN8aihTstVQ1tNQsmrI1tQVQ3FZ2l+9oYNZnpg9fWaFAmUgQow4L4iaLc3g8J6H3pZ2WkVKluZUvGaqRc3JUqq9qQLUleMD+wwxesnWv/Krha23WCw/yLN+abEMMsYIXX4WSzMcHXIEdBG+7Wq7Tdx5O/8H1JclvcjQgdMAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjAtMDUtMTlUMDU6MTk6MjArMDA6MDAKWm6xAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIwLTA1LTE5VDA1OjE5OjIwKzAwOjAwewfWDQAAAABJRU5ErkJggg==';
  //   markerOpts.icon.size = {
  //     'width': 24,
  //     'height': 24
  //   };
  //   markerOpts.icon.anchor = [12, 12];
  // }

  var marker = self.map.addMarker(markerOpts);
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

  var directions = self.get('directions');
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

  directions = directions || {
    request: {
      travelMod: 'DRIVING'
    }
  };

  plugin.google.maps.DirectionsService.route({
    'origin': points.shift(),
    'destination': points.pop(),
    'travelMode': directions.request.travelMode || 'DRIVING',
    'waypoints': points
  }, function(result) {

    // Redraw the polyline
    self.set('directions', result);
    var route = result.routes[routeIndex];
    var leg = route.legs[0];

    // var currentPath = [];
    // leg.steps.forEach(function(step) {
    //   currentPath = currentPath.concat(_decodePolyline(step.polyline));
    // });
    var currentPath = _decodePolyline(route.overview_polyline);

    if (index == 0) {
      self.pathList.setAt(0, currentPath);
    } else if (index == n - 1) {
      self.pathList.setAt(index - 1, currentPath);
    } else {

      // Find the middle point (= marker point)
      var closestDist = Math.pow(2, 32) - 1;
      var closestIdx = 0;
      var left = 0;
      var right = currentPath.length;
      while (left < right) {
        var idx = Math.floor((left + right) / 2);
        var howFar = spherical.computeDistanceBetween(position, currentPath[idx]);
        if (howFar < closestDist) {
          closestDist = howFar;
          closestIdx = idx;
          var howFarLeft = spherical.computeDistanceBetween(position, currentPath[Math.floor((left + idx) / 2)]);
          var howFarRight = spherical.computeDistanceBetween(position, currentPath[Math.floor((right + idx) / 2)]);
          if (howFarLeft < howFarRight && howFarLeft < closestDist) {
            right = idx;
          } else if (howFarLeft > howFarRight && howFarRight < closestDist) {
            left = idx;
          } else {

            // The middle point should be in the range of left and right.
            idx = left;
            while (idx < right) {
              var howFar = spherical.computeDistanceBetween(position, currentPath[idx]);
              if (howFar < closestDist) {
                closestDist = howFar;
                closestIdx = idx;
              }
              idx++;
            }

            break;
          }
        } else {
          break;
        }
      }

      var twoPath = [
        currentPath.slice(0, closestIdx),
        currentPath.slice(closestIdx)
      ];
      twoPath[0].push(position);
      twoPath[1].unshift(position);
      self.pathList.setAt(index - 1, twoPath[0]);
      self.pathList.setAt(index, twoPath[1]);
    }



    // prevent the OVER_QUERY_LIMIT
    setTimeout(function() {
      self.set('requestingFlag', false);
    }, 1000);
  });
};

DirectionsRenderer.prototype.setOptions = function (options) {
  var self = this;
  options = options || {};
  self.set('options', options);
  self.set('routeIndex', options.routeIndex || 0);
  self.set('directions', options.directions || {
    'routes': [],
    'request': 'DRIVING'
  });

};
DirectionsRenderer.prototype.setRouteIndex = function(index) {
  var self = this;
  self.set('routeIndex', index);
};

DirectionsRenderer.prototype._redrawRoute = function(oldIdx, newIdx) {
  var self = this;
  var options = self.get('options');
  var n = self.get('directions').routes.length;
  var routeIndex = self.get('routeIndex');

  if (newIdx < 0 || newIdx > n - 1) {
    return;
  }
  var stepOverlays = new BaseArrayClass();
  var route = self.get('directions').routes[routeIndex];

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
