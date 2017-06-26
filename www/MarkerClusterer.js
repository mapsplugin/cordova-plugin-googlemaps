var argscheck = require('cordova/argscheck'),
  utils = require('cordova/utils'),
  exec = require('cordova/exec'),
  common = require('./Common'),
  event = require('./event'),
  geomodel = require('./geomodel'),
  LatLng = require('./LatLng'),
  LatLngBounds = require('./LatLngBounds'),
  Marker = require('./Marker'),
  Cluster = require('./Cluster'),
  spherical = require('./spherical'),
  BaseClass = require('./BaseClass'),
  BaseArrayClass = require('./BaseArrayClass');

/*****************************************************************************
 * MarkerClusterer Class
 *****************************************************************************/
var MarkerClusterer = function(map, id, markerClusterOptions) {
  BaseClass.call(this);

  var self = this;
  Object.defineProperty(self, "_clusters", {
    value: {},
    writable: false
  });
  Object.defineProperty(self, "_markers", {
    value: new BaseArrayClass(),
    writable: false
  });
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "MarkerClusterer",
    writable: false
  });
  Object.defineProperty(self, "id", {
      value: id,
      writable: false
  });

  var visibleRegion = map.getVisibleRegion();
  self.set("prevBounds", visibleRegion.latLngBounds);

  //---------------------------------
  // Creates marker refereces
  //---------------------------------
  markerClusterOptions.markers.forEach(function(markerOpts, idx) {
    var markerRef = new BaseClass();
    markerRef.set("isAdded", false);
    markerRef.set("id", markerRef.id || "marker-" + idx);
    markerRef.set("position", new LatLng(markerOpts.position.lat, markerOpts.position.lng));
    self._markers.push(markerRef);
  });


  map.on(event.CAMERA_MOVE_END, self.onCameraMoveEnd.bind(self));

  self.onCameraMoveEnd();
};

utils.extend(MarkerClusterer, BaseClass);

MarkerClusterer.prototype.getPluginName = function() {
  return this.map.getId() + "-markerclusterer";
};
MarkerClusterer.prototype.getId = function() {
    return this.id;
};
MarkerClusterer.prototype.getMap = function() {
    return this.map;
};
MarkerClusterer.prototype.getHashCode = function() {
    return this.hashCode;
};

MarkerClusterer.prototype.onCameraMoveEnd = function() {
  var self = this;

  var prevZoom = self.get("zoom");
  var currentZoomLevel = Math.floor(self.map.getCameraZoom());
  currentZoomLevel = currentZoomLevel < 0 ? 0 : currentZoomLevel;
  self.set("zoom", currentZoomLevel);

  self.redraw();
};


MarkerClusterer.prototype.redraw = function() {
  var self = this,
    map = self.map,
    mapDiv = map.getDiv(),
    currentZoomLevel = self.get("zoom"),
    prevResolution = self.get("resolution");

  var resolution = 1;
  resolution = currentZoomLevel > 3 ? 2 : resolution;
  resolution = currentZoomLevel > 5 ? 3 : resolution;
  resolution = currentZoomLevel > 7 ? 4 : resolution;
  resolution = currentZoomLevel > 9 ? 5 : resolution;
  resolution = currentZoomLevel > 11 ? 6 : resolution;
  resolution = currentZoomLevel > 13 ? 7 : resolution;
  resolution = currentZoomLevel > 15 ? 8 : resolution;
  self.set("resolution", resolution);

  var label = document.getElementById("label");
  label.innerHTML = "<b>zoom = " + self.get("zoom") + ", resolution = " + resolution + "</b>";

  //----------------------------------------------------------------
  // Remove the clusters that is in outside of the visible region
  //----------------------------------------------------------------
  var removeIdxes = [],
    visibleRegion = map.getVisibleRegion(),
    extendedBounds = visibleRegion.latLngBounds;

  self._clusters[resolution] = self._clusters[resolution] || {};
  var deleteClusters = [];
  var cellLen = resolution + 1;
  if (resolution === prevResolution) {
    var keys = Object.keys(self._clusters[resolution]);
    keys.forEach(function(geocell) {
      var bounds = self._clusters[resolution][geocell].getBounds();
      if (!extendedBounds.contains(bounds.northeast) &&
        !extendedBounds.contains(bounds.southwest)) {
          self._clusters[resolution][geocell].remove();
          deleteClusters.push(geocell);
          delete self._clusters[resolution][geocell];
      }
    });
  } else if (prevResolution in self._clusters) {
    var keys = Object.keys(self._clusters[prevResolution]);
    keys.forEach(function(geocell) {
        self._clusters[prevResolution][geocell].remove();
    });
    deleteClusters = keys;
    delete self._clusters[prevResolution];
  }

  //--------------------------------
  // Calculate the extended bounds
  //--------------------------------
  var prevBounds = self.get("prevBounds");
  var clusters = [];

  if (resolution !== prevResolution ||
    !prevBounds.contains(visibleRegion.nearLeft) ||
    !prevBounds.contains(visibleRegion.nearRight) ||
    !prevBounds.contains(visibleRegion.farLeft) ||
    !prevBounds.contains(visibleRegion.farRight)) {

/*
    //debug
    if (self.get('polyline')) {
      self.get('polyline').remove();
    }

    map.addPolyline({
      'points': [
        extendedBounds.northeast,
        {lat: extendedBounds.northeast.lat, lng: extendedBounds.southwest.lng},
        extendedBounds.southwest,
        {lat: extendedBounds.southwest.lat, lng: extendedBounds.northeast.lng},
        extendedBounds.northeast
      ],
      'color' : 'blue',
      'width': 2
    }, function(polyline) {
      self.set("polyline", polyline);
    });
*/



    self.set("prevBounds", extendedBounds);

    var cacheKey = "geocell_" + resolution;
    var prepareClusters = {};
    self._markers.forEach(function(markerRef) {

      // If the marker is in cluster, skip it.
      if (markerRef.get("isAdded")) {
        return;
      }

      // If the marker is in outside of the visible region, skip it.
      var position = markerRef.get("position");
      if (!extendedBounds.contains(position)) {
        markerRef.set("isAdded", false);
        return;
      }

      // Calcute geocell
      var geocell = markerRef.get(cacheKey);
      if (!geocell) {
        geocell = geomodel.getGeocell(position.lat, position.lng, resolution);
        markerRef.set(cacheKey, geocell);
      }
      prepareClusters[geocell] = prepareClusters[geocell] || [];
      prepareClusters[geocell].push(markerRef);
    });

    //------------------------------------------
    // Merge close geocells
    //------------------------------------------
    keys = Object.keys(prepareClusters);
    keys.forEach(function(geocell) {
      var cluster = self._clusters[resolution][geocell];
      if (cluster) {
        cluster.addMarkers.bind(cluster, prepareClusters[geocell]);
      } else {
        cluster = new Cluster(geocell, prepareClusters[geocell], resolution);
        self._clusters[resolution][geocell] = cluster;
      }
      clusters.push({
        "geocell": geocell,
        "count": cluster.getItemLength()
      });
    });
  }

  exec(null, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
    "resolution": resolution,
    "new_or_update": clusters,
    "delete": deleteClusters
  }]);

};

module.exports = MarkerClusterer;
