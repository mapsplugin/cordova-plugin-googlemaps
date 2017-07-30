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

  var idxCount = Object.keys(markerClusterOptions.markerMap) + 1;

  var self = this;
  Object.defineProperty(self, "maxZoomLevel", {
    value: markerClusterOptions.maxZoomLevel,
    writable: false
  });
  Object.defineProperty(self, "_clusters", {
    value: {},
    writable: false
  });
  Object.defineProperty(self, "_markerMap", {
    value: markerClusterOptions.markerMap,
    writable: false
  });
  Object.defineProperty(self, "debug", {
    value: markerClusterOptions.debug === true,
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
  self.taskQueue = [];
  self._isRemove = false;

  //----------------------------------------------------
  // If a marker has been removed,
  // remove it from markerClusterOptions.markers also.
  //----------------------------------------------------
  var onRemoveMarker = function() {
    var marker = this;
    var idx = markerClusterOptions.markers.indexOf(marker);
    if (idx > -1) {
      markerClusterOptions.markers.removeAt(idx);
    }
  };
  var keys = Object.keys(self._markerMap);
  keys.forEach(function(markerId) {
    var marker = self._markerMap[markerId];
    marker.one(marker.getId() + "_remove", onRemoveMarker);
  });

  var icons = markerClusterOptions.icons;
  if (icons.length > 0 && !icons[0].min) {
    icons[0].min = 2;
  }

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

    var markerId = markerOptions.id || "marker_" + idxCount;
    var marker = new Marker(self, markerId, markerOptions, "markercluster");
    marker.set("isAdded", false, true);
    marker.set("geocell", geocell, true);
    marker.set("position", markerOptions.position, true);
    self._markerMap[markerId] = marker;
    self._onCameraMoved(true);
  };

  map.on(event.CAMERA_MOVE_END, self._onCameraMoved.bind(self));

  self.on("cluster_click", self.onClusterClicked);
  self.on("nextTask", function(){
    if (self.taskQueue.length === 0) {
      return;
    }
    sel.redraw();
  });

  self._onCameraMoved(false);

  if (self.debug) {
    setInterval(function() {
      console.log("self.taskQueue.push = " + self.taskQueue.length);
    }, 5000);
  }
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

MarkerCluster.prototype.onClusterClicked = function(cluster) {
  this.map.animateCamera({
    target: cluster.getBounds(),
    duration: 500
  });
};

MarkerCluster.prototype._onCameraMoved = function(force) {
  var self = this;
  var visibleRegion = self.map.getVisibleRegion();
  self.map.fromLatLngToPoint(visibleRegion.southwest, function(swPosition) {
    swPosition[0] = swPosition[0] + 40;

    // Get the positiion that are far from 40 px
    self.map.fromPointToLatLng(swPosition, function(position) {
      var clusterDistance = spherical.computeDistanceBetween(visibleRegion.southwest, position); // distance in meters
      self.redraw(clusterDistance, force);
    });
  });
};

MarkerCluster.prototype.remove = function() {
  var self = this;
  if (self._isRemove) {
    return;
  }

  var resolution = self.get("resolution"),
    activeMarkerId = self.map.get("active_marker_id"),
    deleteClusters = [];

  self.trigger("remove");
  self.taskQueue = [];
  self._isRemove = true;

  var keys = Object.keys(self._clusters[resolution]);
  keys.forEach(function(geocell) {
    var cluster = self._clusters[resolution][geocell];
    var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
    if (noClusterMode) {
      cluster.getMarkers().forEach(function(marker, idx) {
        if (marker.getId() === activeMarkerId) {
          marker.trigger(event.INFO_CLOSE);
          marker.hideInfoWindow();
        }
        deleteClusters.push(marker.getId());
      });
    }
    if (!noClusterMode) {
      deleteClusters.push(self.id + "-" + geocell);
    }
    cluster.remove();
  });
  exec(null, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
    "resolution": resolution,
    "new_or_update": [],
    "delete": deleteClusters
  }]);

};
MarkerCluster.prototype.getMarkerById = function(markerId) {
  var self = this;
  return self._markerMap[markerId];
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


MarkerCluster.prototype.redraw = function(clusterDistance, force) {
  var self = this;

  self.taskQueue.push([clusterDistance, force]);
  if (self.debug) {
    console.log("self.taskQueue.push = " + self.taskQueue.length);
  }
  if (self._isRemove || self.taskQueue.length > 1) {
    return;
  }
  var taskParams = self.taskQueue.shift();
  self._redraw.apply(self, taskParams);
};
MarkerCluster.prototype._redraw = function(clusterDistance, force) {
  var self = this,
    map = self.map,
    currentZoomLevel = Math.floor(self.map.getCameraZoom()),
    prevResolution = self.get("resolution");

  if (self._isRemove) {
    return;
  }

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
  resolution = currentZoomLevel > 17 ? 9 : resolution;
  resolution = currentZoomLevel > 19 ? 10 : resolution;
  resolution = currentZoomLevel > 21 ? 11 : resolution;

  //----------------------------------------------------------------
  // Calculates geocells of the current viewport
  //----------------------------------------------------------------
  var visibleRegion = map.getVisibleRegion();
  var expandedRegion = new LatLngBounds();
  expandedRegion.extend(spherical.computeOffset(visibleRegion.farLeft, clusterDistance, 315));
  expandedRegion.extend(spherical.computeOffsetOrigin(visibleRegion.nearRight, clusterDistance, 315));

  var swCell = geomodel.getGeocell(expandedRegion.southwest.lat, expandedRegion.southwest.lng, resolution);
  var neCell = geomodel.getGeocell(expandedRegion.northeast.lat, expandedRegion.northeast.lng, resolution);

  if (currentZoomLevel > self.maxZoomLevel) {
    resolution = -1;
  }
  self.set("resolution", resolution);

  var targetMarkers = [];

  //----------------------------------------------------------------
  // Remove the clusters that is in outside of the visible region
  //----------------------------------------------------------------
  self._clusters[resolution] = self._clusters[resolution] || {};
  var deleteClusters = {};
  var cellLen = resolution + 1;
  var keys;
  var ignoreGeocells = [];
  var allGeocells = [];
  var activeMarkerId = self.map.get("active_marker_id");
  if (prevResolution === -1) {
    if (resolution === -1) {
      self.trigger("nextTask");
      return;
    }
    keys = Object.keys(self._markerMap);
    keys.forEach(function(markerId) {
      var marker = self._markerMap[markerId];
      var geocell = marker.get("geocell").substr(0, cellLen);
      //if (!marker.get("isAdded")) {
      //  return;
      //}
      if (ignoreGeocells.indexOf(geocell) === -1) {
        if (!expandedRegion.contains(marker.getPosition())) {
          marker.set("isAdded", false, true);
          ignoreGeocells.push(geocell);
        } else {
          allGeocells.push(geocell);
        }
      }
        //deleteClusters[marker.getId()] = 1;
    });
    keys = Object.keys(self._markerMap);
    keys.forEach(function(markerId) {
      var marker = self._markerMap[markerId];
      var geocell = marker.get("geocell");
      if (allGeocells.indexOf(geocell) > -1) {
        targetMarkers.push(marker);
      }
    });

  } else if (resolution === prevResolution) {

    keys = Object.keys(self._clusters[prevResolution]);
    keys.forEach(function(geocell) {
      var cluster = self._clusters[prevResolution][geocell];
      var bounds = cluster.getBounds();

      if (!expandedRegion.contains(bounds.northeast) &&
        !expandedRegion.contains(bounds.southwest)) {
          ignoreGeocells.push(geocell);

          if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
            cluster.getMarkers().forEach(function(marker, idx) {
              deleteClusters[marker.getId()] = 1;
              if (self.debug) {
                console.log("---> (js)delete:" + marker.getId());
              }
              if (marker.getId() === activeMarkerId) {
                marker.trigger(event.INFO_CLOSE);
                marker.hideInfoWindow();
              }
            });
          } else {
            deleteClusters[self.id + "-" + geocell] = 1;
            if (self.debug) {
              console.log("---> (js)delete:" + geocell);
            }
          }
          cluster.remove();
          delete self._clusters[resolution][geocell];
      }
    });
    keys = Object.keys(self._markerMap);
    keys.forEach(function(markerId) {
      var marker = self._markerMap[markerId];
      var geocell = marker.get("geocell").substr(0, cellLen);
      if (ignoreGeocells.indexOf(geocell) === -1) {
        targetMarkers.push(marker);
      }
    });

  } else if (prevResolution in self._clusters) {

    keys = Object.keys(self._clusters[prevResolution]);
    if (prevResolution < resolution) {
      //--------------
      // zooming in
      //--------------
      keys.forEach(function(geocell) {
        var cluster = self._clusters[prevResolution][geocell];
        var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
        cluster.getMarkers().forEach(function(marker, idx) {
          marker.set("isAdded", false, true);
          targetMarkers.push(marker);
          if (noClusterMode) {
            if (self.debug) {
              console.log("---> (js)delete:" + marker.getId());
            }
            if (marker.getId() === activeMarkerId) {
              marker.trigger(event.INFO_CLOSE);
              marker.hideInfoWindow();
            }
            deleteClusters[marker.getId()] = 1;
          }
        });
        if (!noClusterMode) {
          if (self.debug) {
            console.log("---> (js)delete:" + geocell);
          }
          deleteClusters[self.id + "-" + geocell] = 1;
        }
        cluster.remove();
      });
    } else {
      //--------------
      // zooming out
      //--------------
      keys.forEach(function(geocell) {
        var cluster = self._clusters[prevResolution][geocell];
        var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
        cluster.getMarkers().forEach(function(marker, idx) {
          marker.set("isAdded", false, true);
          if (noClusterMode) {
            if (self.debug) {
              console.log("---> (js)delete:" + marker.getId());
            }
            if (marker.getId() === activeMarkerId) {
              marker.trigger(event.INFO_CLOSE);
              marker.hideInfoWindow();
            }
            deleteClusters[marker.getId()] = 1;
          }
        });
        if (!noClusterMode) {
          deleteClusters[self.id + "-" + geocell] = 1;
        }
        cluster.remove();

        geocell = geocell.substr(0, cellLen);
        allGeocells.push(geocell);
      });
      keys = Object.keys(self._markerMap);
      keys.forEach(function(markerId) {
        var marker = self._markerMap[markerId];
        var geocell = marker.get("geocell").substr(0, cellLen);
        if (allGeocells.indexOf(geocell) > -1) {
          targetMarkers.push(marker);
          return;
        }
        if (ignoreGeocells.indexOf(geocell) === -1) {
          var bounds = geomodel.computeBox(geocell);
          if (expandedRegion.contains(bounds.northeast) ||
            expandedRegion.contains(bounds.southwest)) {
            targetMarkers.push(marker);
            allGeocells.push(geocell);
          } else {
            ignoreGeocells.push(geocell);
          }
        }
      });
      delete self._clusters[prevResolution];
    }
  } else {
    keys = Object.keys(self._markerMap);
    keys.forEach(function(markerId) {
      var marker = self._markerMap[markerId];
      var geocell = marker.get("geocell").substr(0, cellLen);
      if (allGeocells.indexOf(geocell) > -1) {
        targetMarkers.push(marker);
        return;
      }
      if (ignoreGeocells.indexOf(geocell) === -1) {
        var bounds = geomodel.computeBox(geocell);
        if (expandedRegion.contains(bounds.northeast) ||
          expandedRegion.contains(bounds.southwest)) {
          targetMarkers.push(marker);
          allGeocells.push(geocell);
        } else {
          ignoreGeocells.push(geocell);
        }
      }
    });
  }

  if (self.debug) {
    console.log("targetMarkers = " + targetMarkers.length);
  }

  //--------------------------------
  // Pick up markers are containted in the current viewport.
  //--------------------------------
  var prevSWcell = self.get("prevSWcell");
  var prevNEcell = self.get("prevNEcell");
  var new_or_update_clusters = [];

  if (force ||
    resolution !== prevResolution ||
    prevSWcell !== swCell ||
    prevNEcell !== neCell) {

    self.set("prevSWcell", swCell);
    self.set("prevNEcell", neCell);

    if (resolution !== -1) {
      //------------------
      // Create clusters
      //------------------
      var prepareClusters = {};
      targetMarkers.forEach(function(marker) {
        if (marker.get("isAdded")) {
          return;
        }
        if (!expandedRegion.contains(marker.get("position"))) {
          return;
        }
        var geocell = marker.get("geocell").substr(0, resolution + 1);
        prepareClusters[geocell] = prepareClusters[geocell] || [];
        prepareClusters[geocell].push(marker);
      });

      //------------------------------------------
      // Create/update clusters
      //------------------------------------------
      keys = Object.keys(prepareClusters);
      var sortedClusters = [];
      keys.forEach(function(geocell) {
        var cluster = self.getClusterByGeocellAndResolution(geocell, resolution);
        cluster.addMarkers(prepareClusters[geocell]);

        cluster._markerCenter = cluster.getBounds().getCenter();
        cluster._distanceFrom0 = spherical.computeDistanceBetween({lat: 0, lng: 0}, cluster._markerCenter);
        sortedClusters.push(cluster);
      });
      sortedClusters = sortedClusters.sort(function(a, b) {
        return a._distanceFrom0 - b._distanceFrom0;
      });

      //-------------------------
      // Union close clusters
      //-------------------------
      var cluster, anotherCluster;
      var unionedMarkers = [];
      i = 0;
      var tmp, hit = false;
      while (i < sortedClusters.length) {
        cluster = sortedClusters[i];
        hit = false;
        for (var j = i + 1; j < sortedClusters.length; j++) {
          anotherCluster = sortedClusters[j];
          var distance = spherical.computeDistanceBetween(cluster._markerCenter, anotherCluster._markerCenter);
          if (distance < clusterDistance) {
            if (self.debug) {
              console.log("---> (js)delete:" + anotherCluster.geocell);
            }
            cluster.addMarkers(anotherCluster.getMarkers());
            deleteClusters[anotherCluster.getId()] = 1;
            delete self._clusters[resolution][anotherCluster.geocell];
            self._clusters[resolution][cluster.geocell] = cluster;
            i = j;
          } else {
            hit = true;
            break;
          }
        }
        i++;
        cluster._markerCnt= cluster.getItemLength();
        unionedMarkers.push(cluster);
      }

      unionedMarkers.forEach(function(cluster) {

        var icon = self.getClusterIcon(cluster),
            clusterOpts = {
              "count": cluster.getItemLength(),
              "position": cluster.getBounds().getCenter(),
              "id": cluster.getId()
            };

            if (self.debug) {
              clusterOpts.geocell = cluster.geocell;
            }

        if (icon) {
          clusterOpts.icon = icon;
          clusterOpts.isClusterIcon = true;
          if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
            cluster.getMarkers().forEach(function(marker, idx) {
              deleteClusters[marker.getId()] = 1;
              if (self.debug) {
                console.log("---> (js)delete:" + marker.getId());
              }
            });
          }
          if (self.debug) {
            console.log("---> (js)add:" + clusterOpts.id);
          }
          cluster.setMode(cluster.CLUSTER_MODE);
          new_or_update_clusters.push(clusterOpts);
          return;
        }

        cluster.getMarkers().forEach(function(marker, idx) {
          delete deleteClusters[marker.getId()];
          var markerOptions = marker.getOptions();
          markerOptions.isClusterIcon = false;
          if (self.debug) {
            console.log("---> (js)add:" + marker.getId());
          }
          new_or_update_clusters.push(markerOptions);
        });
        cluster.setMode(cluster.NO_CLUSTER_MODE);
      });
    } else {
      targetMarkers.forEach(function(marker) {
        if (marker.get("isAdded")) {
          return;
        }
        if (expandedRegion.contains(marker.getPosition())) {
          var markerOptions = marker.getOptions();
          markerOptions.isClusterIcon = false;
          if (self.debug) {
            console.log("---> (js)add:" + marker.getId());
          }
          delete deleteClusters[marker.getId()];
          marker.set("isAdded", true, true);
          new_or_update_clusters.push(markerOptions);
        }
      });
    }
  }
  var delete_clusters = Object.keys(deleteClusters);
  if (new_or_update_clusters.length === 0 && delete_clusters === 0) {
    self.trigger("nextTask");
    return;
  }
  if (self._isRemove) {
    return;
  }
  exec(function() {
    self.trigger("nextTask");
  }, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
    "resolution": resolution,
    "new_or_update": new_or_update_clusters,
    "delete": delete_clusters
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
    cluster = new Cluster(self.id + "-" +geocell, geocell);
    self._clusters[resolution][geocell] = cluster;
  }
  return cluster;
};

module.exports = MarkerCluster;
