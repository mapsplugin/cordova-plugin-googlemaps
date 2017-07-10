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

  var idxCount = markerClusterOptions.markers.getLength();

  var self = this;
  Object.defineProperty(self, "maxZoomLevel", {
    value: markerClusterOptions.maxZoomLevel,
    writable: false
  });
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

  self.addMarker = function(markerOptions) {
    idxCount++;
    var resolution = self.get("resolution");

    markerOptions = common.markerOptionsFilter(markerOptions);
    var geocell = geomodel.getGeocell(markerOptions.position.lat, markerOptions.position.lng, 9);

    var marker = new Marker(map, "marker_" + idxCount, markerOptions, "markercluster");
    marker.set("isAdded", true);
    marker.set("geocell", geocell);
    marker.set("position", markerOptions.position);
    self._markers.push(marker);

    // Add or pdate the cluster

    var geocell = geocell.substr(0, resolution + 1)
    var cluster = self.getClusterByGeocellAndResolution(geocell, resolution);
    cluster.addMarkers([marker]);
    var clusterOpts = {
      "count": cluster.getItemLength(),
      "title": geocell
    };
    var clusterIcon = self.getClusterIcon(cluster);
    if (!clusterIcon) {
      clusterIcon = markerOptions.icon;
      clusterOpts.position = markerOptions.position;
      clusterOpts.id = "marker_" + idxCount;
      clusterOpts.title = "marker_" + idxCount;
      cluster.setMode(cluster.NO_CLUSTER_MODE);
    } else {
      clusterOpts.id = geocell;
      clusterOpts.position = cluster.getCenter();
      clusterOpts.icon = clusterIcon;
      clusterOpts.geocell = geocell;
      clusterOpts.title = geocell;
      cluster.setMode(cluster.CLUSTER_MODE);
    }
    marker.setTitle(clusterOpts.title);
    var clusters = [clusterOpts];

    exec(null, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
      "resolution": resolution,
      "new_or_update": clusters,
      "delete": []
    }]);
  };

  map.on(event.CAMERA_MOVE_END, self.redraw.bind(self));

  self.redraw();

  return self;
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

MarkerCluster.prototype.getClusterByClusterId = function(clusterId) {
  var self = this,
    resolution = self.get("resolution");

  if (!self._clusters[resolution]) {
    return null;
  }

  var tmp = clusterId.split(";");
  clusterId = tmp[0];
  var cluster = self._clusters[resolution][clusterId];
  return cluster;
};


MarkerCluster.prototype.redraw = function() {
  var self = this,
    map = self.map,
    currentZoomLevel = Math.floor(self.map.getCameraZoom()),
    prevResolution = self.get("resolution");

  currentZoomLevel = currentZoomLevel < 0 ? 0 : currentZoomLevel;
  self.set("zoom", currentZoomLevel);

  var resolution = 1;
  resolution = currentZoomLevel > 3 ? 2 : resolution;
  resolution = currentZoomLevel > 5 ? 3 : resolution;
  resolution = currentZoomLevel > 7 ? 4 : resolution;
  resolution = currentZoomLevel > 9 ? 5 : resolution;
  resolution = currentZoomLevel > 11 ? 6 : resolution;
  resolution = currentZoomLevel > 13 ? 7 : resolution;
  resolution = currentZoomLevel > 15 ? 8 : resolution;

  //----------------------------------------------------------------
  // Calculates geocells of the current viewport
  //----------------------------------------------------------------
  var visibleRegion = map.getVisibleRegion();
  var swCell = geomodel.getGeocell(visibleRegion.southwest.lat, visibleRegion.southwest.lng, resolution);
  var neCell = geomodel.getGeocell(visibleRegion.northeast.lat, visibleRegion.northeast.lng, resolution);

  if (currentZoomLevel > self.maxZoomLevel) {
    resolution = -1;
  }
  self.set("resolution", resolution);

  //----------------------------------------------------------------
  // Remove the clusters that is in outside of the visible region
  //----------------------------------------------------------------
  self._clusters[resolution] = self._clusters[resolution] || {};
  var deleteClusters = [];
  var cellLen = resolution + 1;
  var keys;
  if (prevResolution === -1) {
    self._markers.forEach(function(marker) {
      if (!marker.get("isAdded")) {
        return;
      }
      if (resolution !== -1 || !visibleRegion.contains(marker.getPosition())) {
        marker.set("isAdded", false);
        deleteClusters.push(marker.getId());
      }
    });
  } else if (resolution === prevResolution) {

    keys = Object.keys(self._clusters[prevResolution]);
    keys.forEach(function(geocell) {
      var cluster = self._clusters[prevResolution][geocell];
      var bounds = cluster.getBounds();

      if (!visibleRegion.contains(bounds.northeast) &&
        !visibleRegion.contains(bounds.southwest)) {

          if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
            cluster.getMarkers().forEach(function(marker, idx) {
              deleteClusters.push(marker.getId());
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
          cluster.getMarkers().forEach(function(marker, idx) {
            deleteClusters.push(marker.getId());
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

    if (resolution !== -1) {
      //------------------
      // Create clusters
      //------------------
      var prepareClusters = {};
      self._markers.forEach(function(marker) {
        if (marker.get("isAdded")) {
          return;
        }
        if (!visibleRegion.contains(marker.get("position"))) {
          return;
        }
        var geocell = marker.get("geocell").substr(0, resolution + 1);
        prepareClusters[geocell] = prepareClusters[geocell] || [];
        prepareClusters[geocell].push(marker);
      });

      //------------------------------------------
      // Merge close geocells
      //------------------------------------------
      keys = Object.keys(prepareClusters);
      keys.forEach(function(geocell) {
        var cluster = self.getClusterByGeocellAndResolution(geocell, resolution);
        cluster.addMarkers(prepareClusters[geocell]);

        var icon = self.getClusterIcon(cluster),
            clusterOpts = {
              "count": cluster.getItemLength(),
              "position": cluster.bounds.getCenter(),
              "id": geocell,
              "geocell": geocell
            };

        if (icon) {
          clusterOpts.icon = icon;
          if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
            cluster.getMarkers().forEach(function(marker, idx) {
              deleteClusters.push(marker.getId());
            });
          }
          cluster.setMode(cluster.CLUSTER_MODE);
          clusters.push(clusterOpts);
          return;
        }

        cluster.getMarkers().forEach(function(marker, idx) {
          marker.setTitle(marker.getId());
          var markerOptions = marker.getOptions();
          markerOptions.count = 1;
          clusters.push(markerOptions);
        });
        cluster.setMode(cluster.NO_CLUSTER_MODE);
      });
    } else {
      self._markers.forEach(function(marker) {
        if (marker.get("isAdded")) {
          return;
        }
        if (visibleRegion.contains(marker.getPosition())) {
          var markerOptions = marker.getOptions();
          markerOptions.count = 1;
          marker.setTitle(marker.getId());
          marker.set("isAdded", true);
          clusters.push(markerOptions);
        }
      });
    }
  }

  exec(null, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
    "resolution": resolution,
    "new_or_update": clusters,
    "delete": deleteClusters
  }]);


};
MarkerCluster.prototype.getClusterIcon = function(cluster) {
  var self = this,
      hit,
      clusterCnt = cluster.getItemLength();

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
      return self.icons[i];
    }
  }
  return null;
};

MarkerCluster.prototype.getClusterByGeocellAndResolution = function(geocell, resolution) {
  var self = this;
  geocell = geocell.substr(0, resolution + 1);

  var cluster = self._clusters[resolution][geocell];
  if (!cluster) {
    cluster = new Cluster(geocell);
    self._clusters[resolution][geocell] = cluster;
  }
  return cluster;
};

module.exports = MarkerCluster;
