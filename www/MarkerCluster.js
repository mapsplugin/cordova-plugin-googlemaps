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
 * MarkerCluster Class
 *****************************************************************************/
var MarkerCluster = function(map, id, markerClusterOptions) {
  BaseClass.call(this);

  var self = this;
  Object.defineProperty(self, "_clusters", {
    value: {},
    writable: false
  });
  Object.defineProperty(self, "_markers", {
    value: markerClusterOptions.markers,
    writable: false
  });
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "MarkerCluster",
    writable: false
  });
  Object.defineProperty(self, "id", {
      value: id,
      writable: false
  });

  var icons = markerClusterOptions.icons;
  for (var i = 0; i < icons.length; i++) {
    if (!icons[i]) {
      continue;
    }
    if (icons[i].anchor &&
      typeof icons[i].anchor === "object" &&
      "x" in icons[i].anchor &&
      "y" in icons[i].anchor) {
      icons[i].anchor = [icons[i].anchor.x, icons[i].anchor.y];
    }
    if (icons[i].infoWindowAnchor &&
      typeof icons[i].infoWindowAnchor === "object" &&
      "x" in icons[i].infoWindowAnchor &&
      "y" in icons[i].infoWindowAnchor) {
      icons[i].infoWindowAnchor = [icons[i].infoWindowAnchor.x, icons[i].infoWindowAnchor.anchor.y];
    }
    if (icons[i].label &&
      common.isHTMLColorString(icons[i].label.color)) {
        icons[i].label.color = common.HTMLColor2RGBA(icons[i].label.color);
    }
  }

  Object.defineProperty(self, "icons", {
      value: icons,
      writable: false
  });

  map.on(event.CAMERA_MOVE_END, self.onCameraMoveEnd.bind(self));

  self.onCameraMoveEnd();
};

utils.extend(MarkerCluster, BaseClass);

MarkerCluster.prototype.getPluginName = function() {
  return this.map.getId() + "-markercluster";
};
MarkerCluster.prototype.getId = function() {
    return this.id;
};
MarkerCluster.prototype.getMap = function() {
    return this.map;
};
MarkerCluster.prototype.getHashCode = function() {
    return this.hashCode;
};

MarkerCluster.prototype.onCameraMoveEnd = function() {
  var self = this;

  var prevZoom = self.get("zoom");
  var currentZoomLevel = Math.floor(self.map.getCameraZoom());
  currentZoomLevel = currentZoomLevel < 0 ? 0 : currentZoomLevel;
  self.set("zoom", currentZoomLevel);

  self.redraw();
};


MarkerCluster.prototype.redraw = function() {
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

  //----------------------------------------------------------------
  // Calculates geocells of the current viewport
  //----------------------------------------------------------------
  var visibleRegion = map.getVisibleRegion();
  var swCell = geomodel.getGeocell(visibleRegion.southwest.lat, visibleRegion.southwest.lng, resolution);
  var neCell = geomodel.getGeocell(visibleRegion.northeast.lat, visibleRegion.northeast.lng, resolution);
  var swCellBounds = geomodel.computeBox(swCell),
    neCellBounds = geomodel.computeBox(neCell);
  var extendedBounds = new LatLngBounds(
    swCellBounds.southwest,
    swCellBounds.northeast,
    neCellBounds.southwest,
    neCellBounds.northeast
  );
  /*
  map.addPolyline({
    points: [
      visibleRegion.southwest,
      {lat: visibleRegion.southwest.lat, lng: visibleRegion.northeast.lng},
      visibleRegion.northeast,
      {lat: visibleRegion.northeast.lat, lng: visibleRegion.southwest.lng},
      visibleRegion.southwest
    ],
    color: "blue"
  });
  */

  //----------------------------------------------------------------
  // Remove the clusters that is in outside of the visible region
  //----------------------------------------------------------------
  self._clusters[resolution] = self._clusters[resolution] || {};
  var deleteClusters = [];
  var cellLen = resolution + 1;
  var keys;
  if (resolution === prevResolution) {

    keys = Object.keys(self._clusters[resolution]);
    keys.forEach(function(geocell) {
      var cluster = self._clusters[resolution][geocell];
      var bounds = cluster.getBounds();

      if (!visibleRegion.contains(bounds.northeast) &&
        !visibleRegion.contains(bounds.southwest)) {

          if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
            cluster._markerRefs.forEach(function(mRef, idx) {
              deleteClusters.push(geocell + "-" + idx);
            });
          } else {
            deleteClusters.push(geocell);
          }
          cluster.remove();
          delete self._clusters[resolution][geocell];
      }
    });

  } else if (prevResolution in self._clusters) {
    keys = Object.keys(self._clusters[prevResolution]);
    keys.forEach(function(geocell) {
        cluster = self._clusters[prevResolution][geocell];
        if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
          cluster._markerRefs.forEach(function(mRef, idx) {
            deleteClusters.push(geocell + "-" + idx);
          });
        } else {
          deleteClusters.push(geocell);
        }
        cluster.remove();
    });
    delete self._clusters[prevResolution];
  }

  //--------------------------------
  // Pick up markers are containted in the current viewport.
  //--------------------------------
  var prevSWcell = self.get("prevSWcell");
  var prevNEcell = self.get("prevNEcell");
  var clusters = [];

  if (resolution !== prevResolution ||
    prevSWcell !== swCell ||
    prevNEcell !== neCell) {

    self.set("prevSWcell", swCell);
    self.set("prevNEcell", neCell);

    var prepareClusters = {};
    self._markers.forEach(function(markerRef) {
      if (markerRef.get("isAdded")) {
        return;
      }
      if (!visibleRegion.contains(markerRef.get("position"))) {
        return;
      }
      var geocell = markerRef.get("geocell").substr(0, resolution + 1);
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
        cluster = new Cluster(geocell, prepareClusters[geocell]);
        self._clusters[resolution][geocell] = cluster;
      }
      var hit,
          clusterCnt = cluster.getItemLength(),
          clusterOpts = {
            "geocell": geocell,
            "count": clusterCnt,
            "position": cluster.bounds.getCenter()
          };

      for (var i = 0; i < self.icons.length; i++) {
        hit = false;
        if ("min" in self.icons[i]) {
          if (clusterCnt >= self.icons[i].min) {
            if ("max" in self.icons[i]) {
              hit = (clusterCnt <= self.icons[i].max);
            } else {
              hit = true;
            }
          }
        } else {
          if ("max" in self.icons[i]) {
            hit = (clusterCnt <= self.icons[i].max);
          }
        }
        if (hit) {
          clusterOpts.icon = self.icons[i];
          break;
        }
      }
      if (hit) {
        if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
          cluster._markerRefs.forEach(function(mRef, idx) {
            deleteClusters.push(geocell + "-" + idx);
          });
        }
        cluster.setMode(cluster.CLUSTER_MODE);
        /*
        var nextLevels = {};
        var max = 0, maxGeocell;
        cluster._markerRefs.forEach(function(mRef) {
          var geocell = mRef.get("geocell").substr(0, resolution + 2);
          nextLevels[geocell] = nextLevels[geocell] || 0;
          nextLevels[geocell]++;
          if (nextLevels[geocell] > max) {
            max = nextLevels[geocell];
            maxGeocell = geocell;
          }
        });
        var bounds = geomodel.computeBox(maxGeocell);
        clusterOpts.position = bounds.getCenter();
        */
        clusters.push(clusterOpts);
        return;
      }
      cluster._markerRefs.forEach(function(mRef, idx) {
        var markerOpts = {
          "count": 1,
          "position": mRef.get("position"),
          "icon": mRef.get("icon"),
          "geocell": clusterOpts.geocell + "-" + idx
        };
        clusters.push(markerOpts);
      });
      cluster.setMode(cluster.NO_CLUSTER_MODE);
    });
  }

  exec(null, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
    "resolution": resolution,
    "new_or_update": clusters,
    "delete": deleteClusters
  }]);


};

module.exports = MarkerCluster;
