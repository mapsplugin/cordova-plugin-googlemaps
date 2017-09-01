var argscheck = require('cordova/argscheck'),
  utils = require('cordova/utils'),
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
var exec;
var MarkerCluster = function(map, markerClusterId, markerClusterOptions, _exec) {
  exec = _exec;
  BaseClass.call(this);

  var idxCount = Object.keys(markerClusterOptions.markerMap).length + 1;

  var self = this;
  Object.defineProperty(self, "maxZoomLevel", {
    value: markerClusterOptions.maxZoomLevel,
    writable: false
  });
  Object.defineProperty(self, "_clusterBounds", {
    value: new BaseArrayClass(),
    writable: false
  });
  Object.defineProperty(self, "_geocellBounds", {
    value: {},
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
    value: markerClusterId,
    writable: false
  });
  Object.defineProperty(self, "MAX_RESOLUTION", {
    value: 11,
    writable: false
  });
  Object.defineProperty(self, "OUT_OF_RESOLUTION", {
    value: 999,
    writable: false
  });
  Object.defineProperty(self, "boundsDraw", {
    value: markerClusterOptions.boundsDraw === true,
    writable: false
  });

  if (self.boundsDraw) {
    self.map.addPolygon({
      visible: false,
      points: [
        {lat: 0, lng: 0},
        {lat: 0, lng: 0},
        {lat: 0, lng: 0},
        {lat: 0, lng: 0}
      ],
      strokeWidth: 1
    }, function(polygon) {
      self.set("polygon", polygon);
    });
  }
  self.taskQueue = [];
  self._stopRequest = false;
  self._isRemove = false;
  self._isWorking = false;

  //----------------------------------------------------
  // If a marker has been removed,
  // remove it from markerClusterOptions.markers also.
  //----------------------------------------------------
/*
  var onRemoveMarker = function() {
    var marker = this;
    var idx = markerClusterOptions.markers.indexOf(marker);
    if (idx > self.OUT_OF_RESOLUTION) {
      markerClusterOptions.markers.removeAt(idx);
    }
  };
  var keys = Object.keys(self._markerMap);
  keys.forEach(function(markerId) {
    var marker = self._markerMap[markerId];
    marker.one(markerOpts.id + "_remove", onRemoveMarker);
  });
*/

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

  self.addMarker = function(markerOptions, skipRedraw) {
    idxCount++;
    var resolution = self.get("resolution");

    markerOptions = common.markerOptionsFilter(markerOptions);
    var geocell = geomodel.getGeocell(markerOptions.position.lat, markerOptions.position.lng, self.MAX_RESOLUTION + 1);

    var markerId = markerClusterId + "-" + (markerOptions.id || "marker_" + idxCount);
    markerOptions.id = markerId;
    markerOptions._cluster = {
      isRemoved: false,
      isAdded: false,
      geocell: geocell,
      _marker: null
    };

    //var marker = new Marker(self, markerId, markerOptions, "markercluster");
    //marker._cluster.isAdded = false;
    //marker.set("geocell", geocell, true);
    //marker.set("position", markerOptions.position, true);
    self._markerMap[markerId] = markerOptions;
    if (skipRedraw) {
      return;
    }
    self.redraw(true);
  };
  self.addMarkers = function(markers) {
    if (utils.isArray(markers) || Array.isArray(markers)) {
      for (var i = 0; i < markers.length; i++) {
        self.addMarker(markers[i], true);
      }
      self.redraw(true);
    }
  };

  map.on(event.CAMERA_MOVE_END, self._onCameraMoved.bind(self));
  window.addEventListener("orientationchange", self._onCameraMoved.bind(self));

  self.on("cluster_click", self.onClusterClicked);
  self.on("nextTask", function(){
    self._isWorking = false;
    if (self._stopRequest ||
        self._isRemove || self.taskQueue.length === 0) {
      return;
    }
    sel.redraw.call(self);
  });

  self.redraw.call(self, true);

  if (self.debug) {
    self.debugTimer = setInterval(function() {
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
  if (this._isRemove) {
    return null;
  }
  var self = this;
  var polygon = self.get("polygon");
  var bounds = cluster.getBounds();
  if (self.boundsDraw) {
    polygon.setPoints([
      bounds.southwest,
      {lat: bounds.northeast.lat, lng: bounds.southwest.lng},
      bounds.northeast,
      {lat: bounds.southwest.lat, lng: bounds.northeast.lng}
    ]);
    polygon.setVisible(true);
  }
  this.map.animateCamera({
    target: cluster.getBounds(),
    duration: 500
  }, function() {
    if (self.boundsDraw) {
      setTimeout(function() {
        polygon.setVisible(false);
      }, 500);
    }
  });
};

MarkerCluster.prototype._onCameraMoved = function() {
  var self = this;

  if (self._isRemove || self._stopRequest) {
    return null;
  }

  self.redraw({
    force: false
  });

};

MarkerCluster.prototype.remove = function() {
  var self = this;
  self._stopRequest = self.hashCode;
  if (self._isRemove) {
    return;
  }
  if (self.debug) {
    clearInterval(self.debugTimer);
    self.self.debugTimer = undefined;
  }
  self._redraw = function(){};

  if (self._isWorking) {
    setTimeout(arguments.callee.bind(self), 20);
    return;
  }

  var resolution = self.get("resolution"),
    activeMarkerId = self.map.get("active_marker_id"),
    deleteClusters = [];

  self.trigger("remove");
  self.taskQueue = [];
  self._isRemove = true;

  if (resolution === self.OUT_OF_RESOLUTION) {
    while (self._clusters[resolution].length > 0) {
      markerOpts = self._clusters[resolution].shift();
      deleteClusters.push(markerOpts.id);
      if (markerOpts.id === activeMarkerId) {
        var marker = markerOpts._cluster.marker;
        if (!marker) {
          marker = self._createMarker(markerOpts);
          markerOpts._cluster.marker = marker;
        }
        marker.trigger(event.INFO_CLOSE);
        marker.hideInfoWindow();
      }
    }
  } else if (self._clusters[resolution]) {
    var keys = Object.keys(self._clusters[resolution]);
    keys.forEach(function(geocell) {
      var cluster = self._clusters[resolution][geocell];
      var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
      if (noClusterMode) {
        cluster.getMarkers().forEach(function(markerOpts, idx) {
          if (markerOpts.id === activeMarkerId) {
            var marker = markerOpts._cluster.marker;
            if (!marker) {
              marker = self._createMarker(markerOpts);
              markerOpts._cluster.marker = marker;
            }
            marker.trigger(event.INFO_CLOSE);
            marker.hideInfoWindow();
          }
          deleteClusters.push(markerOpts.id);
        });
      }
      if (!noClusterMode) {
        deleteClusters.push(self.id + "-" + geocell);
      }
      cluster.remove();
    });
  }
  exec(null, self.errorHandler, self.getPluginName(), 'remove', [self.getId()], {sync: true});

  if (self.boundsDraw && self.get("polygon")) {
    self.get("polygon").remove();
  }
  self.off();

};
MarkerCluster.prototype.removeMarkerById = function(markerId) {
  if (self._isRemove) {
    return null;
  }
  var self = this;
  if (markerId.indexOf(self.id + "-") === -1) {
    markerId = self.id + "-" + markerId;
  }
  var markerOpts = self._markerMap[markerId];
  if (!markerOpts) {
    return;
  }
  markerOpts._cluster.isRemoved = true;
  var marker = markerOpts._cluster.marker;

  var resolutionList = Object.keys(self._clusters);
  var resolution, geocellKey, cluster;
  for (var i = 0; i < resolutionList.length; i++) {
    resolution = parseInt(resolutionList[i], 10);
    geocellKey = markerOpts._cluster.geocell.substr(0, resolution + 1);
    if (geocellKey in self._clusters[resolution]) {
      cluster = self._clusters[resolution][geocellKey];
      if (cluster) {
        cluster.removeMarker(markerOpts);
      }
    }
  }

  var isAdded = markerOpts._cluster.isAdded;
  if (markerOpts._cluster.marker) {
    marker.remove();
    marker.destroy();
  }
  markerOpts._cluster.marker = undefined;
  delete self._markerMap[markerId];
  if (isAdded) {
    exec(null, null, self.getPluginName(), 'redrawClusters', [self.getId(), {
      "delete": [markerId]
    }], {sync: true});
  }
};
MarkerCluster.prototype.getMarkerById = function(markerId) {
  var self = this;
  if (self._isRemove) {
    return null;
  }
  if (markerId.indexOf(self.id + "-") === -1) {
    markerId = self.id + "-" + markerId;
  }
  var markerOpts = self._markerMap[markerId];
  if (!markerOpts) {
    return null;
  }
  var marker = markerOpts._cluster.marker;
  if (!marker) {
    marker = self._createMarker(markerOpts);
    markerOpts._cluster.marker = marker;
  }
  return marker;
};

MarkerCluster.prototype.getClusterByClusterId = function(clusterId) {
  var self = this;

  if (self._isRemove) {
    return null;
  }
  var resolution = self.get("resolution");

  if (!self._clusters[resolution]) {
    return null;
  }

  var tmp = clusterId.split(";");
  clusterId = tmp[0];
  var cluster = self._clusters[resolution][clusterId];
  return cluster;
};


MarkerCluster.prototype.redraw = function(params) {
  var self = this;
  if (self._isRemove || self._stopRequest) {
    return null;
  }

  self.taskQueue.push(params);
  if (self.debug) {
    console.log("self.taskQueue.push = " + self.taskQueue.length);
  }
  if (self._isRemove || self._stopRequest || self.taskQueue.length > 1) {
    return;
  }
  if (self.debug) {
    self._clusterBounds.forEach(function(polyline, cb) {
      polyline.remove();
      cb();
    }, function() {
      self._clusterBounds.empty();
      var taskParams = self.taskQueue.pop();
      self.taskQueue.length = 0;
      var visibleRegion = self.map.getVisibleRegion();
      self._redraw.call(self, {
        visibleRegion: visibleRegion,
        force: taskParams.force
      });
    });
  } else {
    var taskParams = self.taskQueue.pop();
    self.taskQueue.length = 0;

    var visibleRegion = self.map.getVisibleRegion();
    self._redraw.call(self, {
      visibleRegion: visibleRegion,
      force: taskParams.force
    });
  }
};
MarkerCluster.prototype._redraw = function(params) {
  var self = this;

  if (self._isRemove || self._stopRequest || self._isWorking) {
    return null;
  }
  self._isWorking = true;
  var map = self.map,
    currentZoomLevel = self.map.getCameraZoom(),
    prevResolution = self.get("resolution");

  currentZoomLevel = currentZoomLevel < 0 ? 0 : currentZoomLevel;
  self.set("zoom", currentZoomLevel);

  var resolution = 1;
  resolution = self.maxZoomLevel > 3 && currentZoomLevel > 3 ? 2 : resolution;
  resolution = self.maxZoomLevel > 5 && currentZoomLevel > 5 ? 3 : resolution;
  resolution = self.maxZoomLevel > 7 && currentZoomLevel > 7 ? 4 : resolution;
  resolution = self.maxZoomLevel > 9 && currentZoomLevel > 9 ? 5 : resolution;
  resolution = self.maxZoomLevel > 11 && currentZoomLevel > 11 ? 6 : resolution;
  resolution = self.maxZoomLevel > 13 && currentZoomLevel > 13 ? 7 : resolution;
  resolution = self.maxZoomLevel > 15 && currentZoomLevel > 15 ? 8 : resolution;
  resolution = self.maxZoomLevel > 17 && currentZoomLevel > 17 ? 9 : resolution;
  resolution = self.maxZoomLevel > 19 && currentZoomLevel > 19 ? 10 : resolution;
  resolution = self.maxZoomLevel > 21 && currentZoomLevel > 21 ? 11 : resolution;

  //------------------------------------------------------------------------
  // If the current viewport contains the previous viewport,
  // and also the same resolution,
  // skip this task except the params.force = true (such as addMarker)
  //------------------------------------------------------------------------

  var cellLen = resolution + 1;
  var prevSWcell = self.get("prevSWcell");
  var prevNEcell = self.get("prevNEcell");

  var distanceA = spherical.computeDistanceBetween(params.visibleRegion.farRight, params.visibleRegion.farLeft);
  var distanceB = spherical.computeDistanceBetween(params.visibleRegion.farRight, params.visibleRegion.nearRight);
  params.clusterDistance = Math.min(distanceA, distanceB) / 4;
  var expandedRegion = params.visibleRegion;

  var swCell = geomodel.getGeocell(expandedRegion.southwest.lat, expandedRegion.southwest.lng, cellLen);
  var neCell = geomodel.getGeocell(expandedRegion.northeast.lat, expandedRegion.northeast.lng, cellLen);

  if (!params.force &&
    prevSWcell === swCell &&
    prevNEcell === neCell) {
    self.trigger("nextTask");
    return;
  }
  var nwCell = geomodel.getGeocell(expandedRegion.northeast.lat, expandedRegion.southwest.lng, cellLen);
  var seCell = geomodel.getGeocell(expandedRegion.southwest.lat, expandedRegion.northeast.lng, cellLen);

  if (currentZoomLevel > self.maxZoomLevel || resolution === 0) {
    resolution = self.OUT_OF_RESOLUTION;
  }
  self.set("resolution", resolution);
  //console.log("--->prevResolution = " + prevResolution + ", resolution = " + resolution);

  var targetMarkers = [];

  if (self._isRemove || self._stopRequest) {
    self._isWorking = false;
    return;
  }
  //----------------------------------------------------------------
  // Remove the clusters that is in outside of the visible region
  //----------------------------------------------------------------
  if (resolution !== self.OUT_OF_RESOLUTION) {
    self._clusters[resolution] = self._clusters[resolution] || {};
  } else {
    self._clusters[resolution] = self._clusters[resolution] || [];
  }
  var deleteClusters = {};
  var keys;
  var ignoreGeocells = [];
  var allowGeocells = [swCell, neCell, nwCell, seCell];

  var pos, prevCell = "", cell;
  var coners = [
    expandedRegion.northeast,
    {lat: expandedRegion.northeast.lat, lng: expandedRegion.southwest.lng},
    expandedRegion.southwest,
    {lat: expandedRegion.southwest.lat, lng: expandedRegion.northeast.lng},
    expandedRegion.northeast
  ];
  for (var j = 0; j < 4; j++) {
    for (var i = 0.25; i < 1; i+= 0.25) {
      pos = plugin.google.maps.geometry.spherical.interpolate(coners[j], coners[j + 1], i);

      cell = geomodel.getGeocell(pos.lat, pos.lng, cellLen);
      if (allowGeocells.indexOf(cell) === -1) {
        allowGeocells.push(cell);
      }
    }
  }

  //console.log("---->548");
  var activeMarkerId = self.map.get("active_marker_id");
  if (prevResolution === self.OUT_OF_RESOLUTION) {
    if (resolution === self.OUT_OF_RESOLUTION) {
      //console.log("---->552");
      //--------------------------------------
      // Just camera move, no zoom changed
      //--------------------------------------
      keys = Object.keys(self._markerMap);
      keys.forEach(function(markerId) {
        var markerOpts = self._markerMap[markerId];
        if (self._isRemove ||
            self._stopRequest ||
            markerOpts._cluster.isRemoved ||
            markerOpts._cluster.isAdded) {
          return;
        }

        var geocell = markerOpts._cluster.geocell.substr(0, cellLen);
        if (ignoreGeocells.indexOf(geocell) > -1) {
          return;
        }

        if (allowGeocells.indexOf(geocell) > -1) {
          targetMarkers.push(markerOpts);
          return;
        }

        if (expandedRegion.contains(markerOpts.position)) {
          allowGeocells.push(geocell);
          targetMarkers.push(markerOpts);
        } else {
          ignoreGeocells.push(geocell);
        }
      });
    } else {
      //--------------
      // zoom out
      //--------------
      while(self._clusters[self.OUT_OF_RESOLUTION].length > 0) {
        markerOpts = self._clusters[self.OUT_OF_RESOLUTION].shift();
        self._markerMap[markerOpts.id]._cluster.isAdded = false;
        deleteClusters[markerOpts.id] = 1;

        if (self._markerMap[markerOpts.id].id === activeMarkerId) {
          var marker = self._markerMap[markerOpts.id]._cluster.marker;
          if (marker) {
            marker.trigger(event.INFO_CLOSE);
            marker.hideInfoWindow();
          }
        }
        if (self.debug) {
          console.log("---> (js:489)delete:" + markerOpts.id);
        }
      }
      keys = Object.keys(self._markerMap);
      keys.forEach(function(markerId) {
        var markerOpts = self._markerMap[markerId];
        if (self._isRemove ||
            self._stopRequest ||
            markerOpts._cluster.isRemoved) {
          return;
        }

        var geocell = markerOpts._cluster.geocell.substr(0, cellLen);
        if (ignoreGeocells.indexOf(geocell) > -1) {
          return;
        }

        if (allowGeocells.indexOf(geocell) > -1) {
          targetMarkers.push(markerOpts);
          return;
        }

        if (expandedRegion.contains(markerOpts.position)) {
          allowGeocells.push(geocell);
          targetMarkers.push(markerOpts);
        } else {
          ignoreGeocells.push(geocell);
        }
      });
    }

  } else if (resolution === prevResolution) {
    //console.log("--->prevResolution(" + prevResolution + ") == resolution(" + resolution + ")");
    //--------------------------------------
    // Just camera move, no zoom changed
    //--------------------------------------

    keys = Object.keys(self._clusters[prevResolution]);
    keys.forEach(function(geocell) {
      if (self._isRemove || self._stopRequest) {
        return;
      }
      var cluster = self._clusters[prevResolution][geocell];
      var bounds = cluster.getBounds();


      if (!self._isRemove &&
        !expandedRegion.contains(bounds.northeast) &&
        !expandedRegion.contains(bounds.southwest)) {
          ignoreGeocells.push(geocell);

          if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
            cluster.getMarkers().forEach(function(markerOpts, idx) {
              deleteClusters[markerOpts.id] = 1;
              self._markerMap[markerOpts.id]._cluster.isAdded = false;
              if (self.debug) {
                console.log("---> (js:534)delete:" + markerOpts.id);
              }
              if (markerOpts.id === activeMarkerId) {
                var marker = markerOpts._cluster.marker;
                if (!marker) {
                  marker = self._createMarker(markerOpts);
                  markerOpts._cluster.marker = marker;
                }
                marker.trigger(event.INFO_CLOSE);
                marker.hideInfoWindow();
              }
            });
          } else {
            deleteClusters[self.id + "-" + geocell] = 1;
            if (self.debug) {
              console.log("---> (js:549)delete:" + geocell);
            }
          }
          cluster.remove();
          delete self._clusters[resolution][geocell];
      }
    });
    keys = Object.keys(self._markerMap);
    keys.forEach(function(markerId) {
      var markerOpts = self._markerMap[markerId];
      var geocell = markerOpts._cluster.geocell.substr(0, cellLen);
      if (self._isRemove ||
          self._stopRequest ||
          markerOpts._cluster.isRemoved ||
          ignoreGeocells.indexOf(geocell) > -1 ||
          markerOpts._cluster.isAdded) {
        return;
      }

      if (allowGeocells.indexOf(geocell) > -1) {
        targetMarkers.push(markerOpts);
        return;
      }

      if (expandedRegion.contains(markerOpts.position)) {
        targetMarkers.push(markerOpts);
        allowGeocells.push(geocell);
      } else {
        ignoreGeocells.push(geocell);
        self._markerMap[markerOpts.id]._cluster.isAdded = false;
      }
    });

  } else if (prevResolution in self._clusters) {
    //console.log("--->prevResolution(" + prevResolution + ") != resolution(" + resolution + ")");

    if (prevResolution < resolution) {
      //--------------
      // zooming in
      //--------------
      keys = Object.keys(self._clusters[prevResolution]);
      keys.forEach(function(geocell) {
        if (self._isRemove) {
          return;
        }
        var cluster = self._clusters[prevResolution][geocell];
        var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
        cluster.getMarkers().forEach(function(markerOpts, idx) {
          if (self._isRemove ||
              self._stopRequest) {
            return;
          }
          self._markerMap[markerOpts.id]._cluster.isAdded = false;
          //targetMarkers.push(markerOpts);
          if (noClusterMode) {
            if (self.debug) {
              console.log("---> (js:581)delete:" + markerOpts.id);
            }
            if (markerOpts.id === activeMarkerId) {
              var marker = markerOpts._cluster.marker;
              if (!marker) {
                marker = self._createMarker(markerOpts);
                markerOpts._cluster.marker = marker;
              }
              marker.trigger(event.INFO_CLOSE);
              marker.hideInfoWindow();
            }
            deleteClusters[markerOpts.id] = 1;
          }
        });
        if (!noClusterMode) {
          if (self.debug) {
            console.log("---> (js:597)delete:" + geocell);
          }
          deleteClusters[self.id + "-" + geocell] = 1;
        }
        cluster.remove();
      });
    } else {
      //--------------
      // zooming out
      //--------------
      keys = Object.keys(self._clusters[prevResolution]);
      keys.forEach(function(geocell) {
        if (self._stopRequest ||
            self._isRemove) {
          return;
        }
        var cluster = self._clusters[prevResolution][geocell];
        var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
        cluster.getMarkers().forEach(function(markerOpts, idx) {
          self._markerMap[markerOpts.id]._cluster.isAdded = false;
          if (noClusterMode) {
            if (self.debug) {
              console.log("---> (js:614)delete:" + markerOpts.id);
            }
            if (markerOpts.id === activeMarkerId) {
              var marker = markerOpts._cluster.marker;
              if (!marker) {
                marker = self._createMarker(markerOpts);
                self._markerMap[markerOpts.id]._cluster.marker = marker;
              }
              marker.trigger(event.INFO_CLOSE);
              marker.hideInfoWindow();
            }
            deleteClusters[markerOpts.id] = 1;
          }
          self._markerMap[markerOpts.id] = markerOpts;
        });
        if (!noClusterMode) {
          deleteClusters[self.id + "-" + geocell] = 1;
          if (self.debug) {
            console.log("---> (js:632)delete:" + self.id + "-" + geocell);
          }
        }
        cluster.remove();

        geocell = geocell.substr(0, cellLen);
      });
    }
    keys = Object.keys(self._markerMap);
    keys.forEach(function(markerId) {
      if (self._stopRequest ||
          self._isRemove) {
        return;
      }
      var markerOpts = self._markerMap[markerId];
      var geocell = markerOpts._cluster.geocell.substr(0, cellLen);
      if (markerOpts._cluster.isRemoved ||
          ignoreGeocells.indexOf(geocell) > -1) {
        self._markerMap[markerOpts.id]._cluster.isAdded = false;
        return;
      }
      if (markerOpts._cluster.isAdded) {
        return;
      }

      if (allowGeocells.indexOf(geocell) > -1) {
        targetMarkers.push(markerOpts);
        return;
      }

      if (expandedRegion.contains(markerOpts.position)) {
        targetMarkers.push(markerOpts);
        allowGeocells.push(geocell);
      } else {
        ignoreGeocells.push(geocell);
        self._markerMap[markerOpts.id]._cluster.isAdded = false;
      }
    });
    delete self._clusters[prevResolution];
  } else {
    //console.log("-----> initialize");
    keys = Object.keys(self._markerMap);
    keys.forEach(function(markerId) {
      if (self._stopRequest ||
          self._isRemove) {
        return;
      }
      var markerOpts = self._markerMap[markerId];
      var geocell = markerOpts._cluster.geocell.substr(0, cellLen);
      if (markerOpts._cluster.isRemoved ||
        ignoreGeocells.indexOf(geocell) > -1) {
        return;
      }

      if (allowGeocells.indexOf(geocell) > -1) {
        targetMarkers.push(markerOpts);
        return;
      }
      if (expandedRegion.contains(markerOpts.position)) {
        targetMarkers.push(markerOpts);
        allowGeocells.push(geocell);
      } else {
        ignoreGeocells.push(geocell);
      }
    });
  }

  if (self._stopRequest ||
      self._isRemove) {
    self._isWorking = false;
    return;
  }
  if (self.debug) {
    console.log("targetMarkers = " + targetMarkers.length);
  }

  //--------------------------------
  // Pick up markers are containted in the current viewport.
  //--------------------------------
  var new_or_update_clusters = [];

  if (params.force ||
    resolution == self.OUT_OF_RESOLUTION ||
    resolution !== prevResolution ||
    prevSWcell !== swCell ||
    prevNEcell !== neCell) {

    self.set("prevSWcell", swCell);
    self.set("prevNEcell", neCell);

    if (resolution !== self.OUT_OF_RESOLUTION) {

      //------------------
      // Create clusters
      //------------------
      var prepareClusters = {};
      targetMarkers.forEach(function(markerOpts) {
        if (markerOpts._cluster.isAdded) {
          if (self.debug) {
            console.log("isAdded", markerOpts);
          }
          return;
        }
        var geocell = markerOpts._cluster.geocell.substr(0, resolution + 1);
        prepareClusters[geocell] = prepareClusters[geocell] || [];
        prepareClusters[geocell].push(markerOpts);
      });

      if (self.debug) {
        console.log("prepareClusters = ", prepareClusters, targetMarkers);
      }

      //------------------------------------------
      // Create/update clusters
      //------------------------------------------
      keys = Object.keys(prepareClusters);

      var sortedClusters = [];
      keys.forEach(function(geocell) {
        var cluster = self.getClusterByGeocellAndResolution(geocell, resolution);
        cluster.addMarkers(prepareClusters[geocell]);

        cluster._markerCenter = cluster.getBounds().getCenter();
        //cluster._distanceFrom0 = spherical.computeDistanceBetween({lat: 0, lng: 0}, cluster._markerCenter);
        sortedClusters.push(cluster);
      });

      sortedClusters = sortedClusters.sort(function(a, b) {
        return a.geocell.localeCompare(b.geocell);
      });

      //-------------------------
      // Union close clusters
      //-------------------------
      var cluster, anotherCluster, distance;
      var unionedMarkers = [];
      i = 0;
      var tmp, hit = false;
      while (i < sortedClusters.length) {
        cluster = sortedClusters[i];
        hit = false;
        for (var j = i + 1; j < sortedClusters.length; j++) {
          anotherCluster = sortedClusters[j];
          distance = spherical.computeDistanceBetween(cluster._markerCenter, anotherCluster._markerCenter);
          if (distance < params.clusterDistance) {
            if (self.debug) {
              console.log("---> (js:763)delete:" + anotherCluster.geocell);
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
            cluster.getMarkers().forEach(function(markerOpts, idx) {
              deleteClusters[markerOpts.id] = 1;
              if (self.debug) {
                console.log("---> (js:800)delete:" + markerOpts.id);
              }
            });
          }
          if (self.debug) {
            console.log("---> (js:805)add:" + clusterOpts.id, icon);
            var geocell = clusterOpts.geocell.substr(0, cellLen);
            var bounds = self._geocellBounds[geocell] || geomodel.computeBox(geocell);
            self._geocellBounds[geocell] = bounds;
            self.map.addPolyline({
              color: "blue",
              points: [
                bounds.southwest,
                {lat: bounds.southwest.lat, lng: bounds.northeast.lng},
                bounds.northeast,
                {lat: bounds.northeast.lat, lng: bounds.southwest.lng},
                bounds.southwest
              ]
            }, function(polyline) {
              self._clusterBounds.push(polyline);
            });
          }
          cluster.setMode(cluster.CLUSTER_MODE);
          new_or_update_clusters.push(clusterOpts);
          return;
        }

        cluster.getMarkers().forEach(function(markerOpts, idx) {
          if (!markerOpts._cluster.isAdded) {
            return;
          }
          delete deleteClusters[markerOpts.id];
          markerOpts.isClusterIcon = false;
          if (self.debug) {
            console.log("---> (js:831)add:" + markerOpts.id + ", isAdded = " + markerOpts._cluster.isAdded);
            markerOpts.title= markerOpts.id;
          }
          new_or_update_clusters.push(markerOpts);
        });
        cluster.setMode(cluster.NO_CLUSTER_MODE);
      });
    } else {
      cellLen = swCell.length;
      var allowGeocell = [];
      var ignoreGeocell = [];
      targetMarkers.forEach(function(markerOpts) {
        if (markerOpts._cluster.isAdded) {
          return;
        }
        markerOpts.isClusterIcon = false;
        if (self.debug) {
          console.log("---> (js:859)add:" + markerOpts.id);
          markerOpts.title= markerOpts.id;
        }
        delete deleteClusters[markerOpts.id];
        self._markerMap[markerOpts.id]._cluster.isAdded = true;
        new_or_update_clusters.push(markerOpts);
        self._clusters[self.OUT_OF_RESOLUTION].push(markerOpts);
      });
    }
  }
  var delete_clusters = Object.keys(deleteClusters);

  if (self._stopRequest ||
      new_or_update_clusters.length === 0 && delete_clusters.length === 0) {
    self.trigger("nextTask");
    return;
  }

  if (self._stopRequest ||
      self._isRemove) {
    self._isWorking = false;
    return;
  }

  exec(function() {
    self.trigger("nextTask");
  }, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
    "resolution": resolution,
    "new_or_update": new_or_update_clusters,
    "delete": delete_clusters
  }], {sync: true});
/*
    console.log({
                    "resolution": resolution,
                    "new_or_update": new_or_update_clusters,
                    "delete": delete_clusters
                  });
*/
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

MarkerCluster.prototype._createMarker = function(markerOpts) {
  var markerId = markerOpts.id;
  var self = this;
  var marker = new Marker(self.getMap(), markerId, markerOpts, "MarkerCluster", exec);
  function updateProperty(prevValue, newValue, key) {
    self._markerMap[markerId][key] = newValue;
  }
  marker.on("title_changed", updateProperty);
  marker.on("snippet_changed", updateProperty);
  marker.on("animation_changed", updateProperty);
  marker.on("infoWindowAnchor_changed", updateProperty);
  marker.on("opacity_changed", updateProperty);
  marker.on("zIndex_changed", updateProperty);
  marker.on("visible_changed", updateProperty);
  marker.on("draggable_changed", updateProperty);
  marker.on("position_changed", updateProperty);
  marker.on("rotation_changed", updateProperty);
  marker.on("flat_changed", updateProperty);
  marker.on("icon_changed", updateProperty);
  marker.one(markerId + "_remove", function() {
    self.removeMarkerById(markerId);
  });
  return marker;
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
