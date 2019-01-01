

var utils = require('cordova/utils'),
  common = require('./Common'),
  event = require('./event'),
  geomodel = require('./geomodel'),
  Marker = require('./Marker'),
  Cluster = require('./Cluster'),
  spherical = require('./spherical'),
  Overlay = require('./Overlay'),
  BaseArrayClass = require('./BaseArrayClass');

/*****************************************************************************
 * MarkerCluster Class
 *****************************************************************************/
var exec;
var MarkerCluster = function (map, markerClusterOptions, _exec) {
  exec = _exec;
  Overlay.call(this, map, markerClusterOptions, 'MarkerCluster', _exec);

  var self = this;
  Object.defineProperty(self, 'maxZoomLevel', {
    value: markerClusterOptions.maxZoomLevel,
    writable: false
  });
  Object.defineProperty(self, '_markerMap', {
    enumerable: false,
    value: {},
    writable: false
  });
  Object.defineProperty(self, '_clusterBounds', {
    enumerable: false,
    value: new BaseArrayClass(),
    writable: false
  });
  Object.defineProperty(self, '_geocellBounds', {
    enumerable: false,
    value: {},
    writable: false
  });
  Object.defineProperty(self, '_clusters', {
    enumerable: false,
    value: {},
    writable: false
  });
  Object.defineProperty(self, 'debug', {
    value: markerClusterOptions.debug === true,
    writable: false
  });
  Object.defineProperty(self, 'MAX_RESOLUTION', {
    enumerable: false,
    value: 11,
    writable: false
  });
  Object.defineProperty(self, 'OUT_OF_RESOLUTION', {
    enumerable: false,
    value: 999,
    writable: false
  });
  Object.defineProperty(self, 'boundsDraw', {
    value: markerClusterOptions.boundsDraw === true,
    writable: false
  });

  if (self.boundsDraw) {
    self.map.addPolygon({
      visible: false,
      points: [
        {
          lat: 0,
          lng: 0
        },
        {
          lat: 0,
          lng: 0
        },
        {
          lat: 0,
          lng: 0
        },
        {
          lat: 0,
          lng: 0
        }
      ],
      strokeWidth: 1
    }, function (polygon) {
      self.set('polygon', polygon);
    });
  }
  self.taskQueue = [];
  self._stopRequest = false;
  self._isWorking = false;

  var icons = markerClusterOptions.icons;
  if (icons.length > 0 && !icons[0].min) {
    icons[0].min = 2;
  }

  for (var i = 0; i < icons.length; i++) {
    if (!icons[i]) {
      continue;
    }
    if (icons[i].anchor &&
      typeof icons[i].anchor === 'object' &&
      'x' in icons[i].anchor &&
      'y' in icons[i].anchor) {
      icons[i].anchor = [icons[i].anchor.x, icons[i].anchor.y];
    }
    if (icons[i].infoWindowAnchor &&
      typeof icons[i].infoWindowAnchor === 'object' &&
      'x' in icons[i].infoWindowAnchor &&
      'y' in icons[i].infoWindowAnchor) {
      icons[i].infoWindowAnchor = [icons[i].infoWindowAnchor.x, icons[i].infoWindowAnchor.anchor.y];
    }
    if (icons[i].label &&
      common.isHTMLColorString(icons[i].label.color)) {
      icons[i].label.color = common.HTMLColor2RGBA(icons[i].label.color);
    }
  }

  Object.defineProperty(self, 'icons', {
    value: icons,
    writable: false
  });

  self.addMarker = function (markerOptions, skipRedraw) {

    markerOptions = common.markerOptionsFilter(markerOptions);
    var geocell = geomodel.getGeocell(markerOptions.position.lat, markerOptions.position.lng, self.MAX_RESOLUTION + 1);

    markerOptions._cluster = {
      isRemoved: false,
      isAdded: false,
      geocell: geocell
    };

    var marker = self._createMarker(markerOptions);
    var markerId = marker.__pgmId.split(/\-/)[1];
    self._markerMap[markerId] = marker;
    if (skipRedraw || !self._isReady) {
      return marker;
    }
    self._triggerRedraw({
      force: true
    });
    return marker;
  };
  self.addMarkers = function (markers) {
    var results = [];
    if (utils.isArray(markers) || Array.isArray(markers)) {
      for (var i = 0; i < markers.length; i++) {
        results.push(self.addMarker(markers[i], true));
      }
      if (!self._isReady) {
        return results;
      }
      self._triggerRedraw({
        force: true
      });
    }
    return results;
  };

  map.on(event.CAMERA_MOVE_END, self._onCameraMoved.bind(self));
  window.addEventListener('orientationchange', self._onCameraMoved.bind(self));

  self.on('cluster_click', self.onClusterClicked);
  self.on('nextTask', function () {
    self._isWorking = false;
    if (self._stopRequest || self._isRemoved ||
      self.taskQueue.length === 0 || !self._isReady) {
      return;
    }
    self._triggerRedraw.call(self);
  });

  // self._triggerRedraw.call(self, {
  //   force: true
  // });

  if (self.debug) {
    self.debugTimer = setInterval(function () {
      console.log('self.taskQueue.push = ' + self.taskQueue.length);
    }, 5000);
  }

  return self;
};

utils.extend(MarkerCluster, Overlay);

MarkerCluster.prototype.onClusterClicked = function (cluster) {
  if (this._isRemoved) {
    return null;
  }
  var self = this;
  var polygon = self.get('polygon');
  var bounds = cluster.getBounds();
  if (self.boundsDraw) {
    polygon.setPoints([
      bounds.southwest,
      {
        lat: bounds.northeast.lat,
        lng: bounds.southwest.lng
      },
      bounds.northeast,
      {
        lat: bounds.southwest.lat,
        lng: bounds.northeast.lng
      }
    ]);
    polygon.setVisible(true);
  }
  var zoomLevel = computeZoom(cluster.getBounds(), self.map.getDiv());
  zoomLevel += zoomLevel === self.map.get('camera_zoom') ? 1 : 0;
  self.map.animateCamera({
    target: cluster.getBounds().getCenter(),
    zoom: zoomLevel,
    duration: 500
  }, function () {
    if (self.boundsDraw) {
      setTimeout(function () {
        polygon.setVisible(false);
      }, 500);
    }
  });
};

Object.defineProperty(MarkerCluster.prototype, '_onCameraMoved', {
  value: function () {
    var self = this;

    if (self._isRemoved || self._stopRequest || !self._isReady) {
      return null;
    }

    self._triggerRedraw({
      force: false
    });

  },
  enumerable: false,
  writable: false
});

MarkerCluster.prototype.remove = function (callback) {
  var self = this;
  self._stopRequest = self.hashCode;
  if (self._isRemoved) {
    if (typeof callback === 'function') {
      return;
    } else {
      return Promise.resolve();
    }
  }
  if (self.debug) {
    clearInterval(self.debugTimer);
    self.self.debugTimer = undefined;
  }

  if (self._isWorking) {
    setTimeout(arguments.callee.bind(self), 20);
    return;
  }

  var keys;
  var resolution = self.get('resolution'),
    activeMarker = self.map.get('active_marker'),
    deleteClusters = [];

  self.taskQueue = [];
  Object.defineProperty(self, '_isRemoved', {
    value: true,
    writable: false
  });
  self.trigger('remove');

  var activeMarkerId = activeMarker ? activeMarker.getId() : null;
  var marker;
  if (resolution === self.OUT_OF_RESOLUTION) {
    while (self._clusters[resolution].length > 0) {
      marker = self._clusters[resolution].shift();
      deleteClusters.push(marker.__pgmId);
      if (marker.__pgmId === activeMarkerId) {
        marker.trigger(event.INFO_CLOSE);
        marker.hideInfoWindow();
      }
    }
  } else if (self._clusters[resolution]) {
    keys = Object.keys(self._clusters[resolution]);
    keys.forEach(function (geocell) {
      var cluster = self._clusters[resolution][geocell];
      var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
      if (noClusterMode) {
        cluster.getMarkers().forEach(function (marker) {
          if (marker.__pgmId === activeMarkerId) {
            marker.trigger(event.INFO_CLOSE);
            marker.hideInfoWindow();
          }
          deleteClusters.push(marker.__pgmId);
        });
      }
      if (!noClusterMode) {
        deleteClusters.push(geocell);
      }
      cluster.remove();
    });
  }

  var resolver = function (resolve, reject) {
    self.exec.call(self,
      resolve.bind(self),
      reject.bind(self),
      self.getPluginName(), 'remove', [self.getId()], {
        sync: true,
        remove: true
      });
  };

  var answer;
  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    answer = new Promise(resolver);
  }

  keys = Object.keys(self._markerMap);
  keys.forEach(function (markerId) {
    self._markerMap[markerId].remove(function() {
      self._markerMap[markerId].destroy();
      self._markerMap[markerId] = undefined;
      delete self._markerMap[markerId];
    });
  });
  if (self.boundsDraw && self.get('polygon')) {
    self.get('polygon').remove();
  }
  self.off();

  return answer;
};


Object.defineProperty(MarkerCluster.prototype, '_removeMarkerById', {
  enumerable: false,
  value: function (markerId) {
    var self = this;
    if (self._isRemoved) {
      return null;
    }
    //if (markerId.indexOf(self.__pgmId + '-') === -1) {
    //}
    var marker = self._markerMap[markerId];
    if (!marker) {
      return;
    }
    var isAdded = marker.get('_cluster').isAdded;
    var resolutionList = Object.keys(self._clusters);
    var resolution, geocellKey, cluster;
    for (var i = 0; i < resolutionList.length; i++) {
      resolution = parseInt(resolutionList[i], 10);
      geocellKey = marker.get('_cluster').geocell.substr(0, resolution + 1);
      if (geocellKey in self._clusters[resolution]) {
        cluster = self._clusters[resolution][geocellKey];
        if (cluster) {
          cluster.removeMarker(marker);
        }
      }
    }

    marker.remove();
    marker.destroy();
    delete self._markerMap[markerId];
    if (isAdded) {
      self.exec.call(self, null, null, self.getPluginName(), 'redrawClusters', [self.getId(), {
        'delete': [markerId]
      }], {
        sync: true
      });
    }
  },
  writable: false
});

MarkerCluster.prototype.getMarkerById = function (markerId) {
  var self = this;
  if (self._isRemoved) {
    return null;
  }
  return self._markerMap[markerId];
};

Object.defineProperty(MarkerCluster.prototype, '_getClusterByClusterId', {
  enumerable: false,
  value: function (clusterId) {
    var self = this;

    if (self._isRemoved) {
      return null;
    }
    var resolution = self.get('resolution');

    if (!self._clusters[resolution]) {
      return null;
    }

    return self._clusters[resolution][clusterId];
  },
  writable: false
});


Object.defineProperty(MarkerCluster.prototype, '_triggerRedraw', {
  enumerable: false,
  value: function (params) {
    var self = this;
    if (self._isRemoved || self._stopRequest || !self._isReady) {
      return null;
    }

    self.taskQueue.push(params);
    if (self.debug) {
      console.log('self.taskQueue.push = ' + self.taskQueue.length);
    }
    if (self._isRemoved || self._stopRequest || self.taskQueue.length > 1) {
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
  },
  writable: false
});

Object.defineProperty(MarkerCluster.prototype, '_redraw', {
  enumerable: false,
  value: function (params) {
    var self = this;

    if (self._isRemoved || self._stopRequest || self._isWorking || !self._isReady) {
      return null;
    }
    self._isWorking = true;
    var currentZoomLevel = self.map.getCameraZoom(),
      prevResolution = self.get('resolution');

    currentZoomLevel = currentZoomLevel < 0 ? 0 : currentZoomLevel;
    self.set('zoom', currentZoomLevel);

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
    var prevSWcell = self.get('prevSWcell');
    var prevNEcell = self.get('prevNEcell');

    var distanceA = spherical.computeDistanceBetween(params.visibleRegion.farRight, params.visibleRegion.farLeft);
    var distanceB = spherical.computeDistanceBetween(params.visibleRegion.farRight, params.visibleRegion.nearRight);
    params.clusterDistance = Math.min(distanceA, distanceB) / 4;
    var expandedRegion = params.visibleRegion;

    var swCell = geomodel.getGeocell(expandedRegion.southwest.lat, expandedRegion.southwest.lng, cellLen);
    var neCell = geomodel.getGeocell(expandedRegion.northeast.lat, expandedRegion.northeast.lng, cellLen);

    if (!params.force &&
      prevSWcell === swCell &&
      prevNEcell === neCell) {
      self.trigger('nextTask');
      return;
    }
    var nwCell = geomodel.getGeocell(expandedRegion.northeast.lat, expandedRegion.southwest.lng, cellLen);
    var seCell = geomodel.getGeocell(expandedRegion.southwest.lat, expandedRegion.northeast.lng, cellLen);

    if (currentZoomLevel > self.maxZoomLevel || resolution === 0) {
      resolution = self.OUT_OF_RESOLUTION;
    }
    self.set('resolution', resolution);
    //console.log('--->prevResolution = ' + prevResolution + ', resolution = ' + resolution);

    var targetMarkers = [];

    if (self._isRemoved || self._stopRequest) {
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
    var keys, i, j;
    var ignoreGeocells = [];
    var allowGeocells = [swCell, neCell, nwCell, seCell];

    var pos, cell;
    var coners = [
      expandedRegion.northeast,
      {
        lat: expandedRegion.northeast.lat,
        lng: expandedRegion.southwest.lng
      },
      expandedRegion.southwest,
      {
        lat: expandedRegion.southwest.lat,
        lng: expandedRegion.northeast.lng
      },
      expandedRegion.northeast
    ];
    for (j = 0; j < 4; j++) {
      for (i = 0.25; i < 1; i += 0.25) {
        pos = window.plugin.google.maps.geometry.spherical.interpolate(coners[j], coners[j + 1], i);

        cell = geomodel.getGeocell(pos.lat, pos.lng, cellLen);
        if (allowGeocells.indexOf(cell) === -1) {
          allowGeocells.push(cell);
        }
      }
    }

    //console.log('---->548');
    var activeMarker = self.map.get('active_marker');
    var activeMarkerId = activeMarker ? activeMarker.getId() : null;
    if (prevResolution === self.OUT_OF_RESOLUTION) {
      if (resolution === self.OUT_OF_RESOLUTION) {
        //--------------------------------------
        // Just camera move, no zoom changed
        //--------------------------------------
        keys = Object.keys(self._markerMap);
        keys.forEach(function (markerId) {
          var marker = self._markerMap[markerId];
          if (self._isRemoved ||
            self._stopRequest ||
            !marker.isVisible() ||
            marker.get('_cluster').isRemoved ||
            marker.get('_cluster').isAdded) {
            return;
          }

          var geocell = marker.get('_cluster').geocell.substr(0, cellLen);
          if (ignoreGeocells.indexOf(geocell) > -1) {
            return;
          }

          if (allowGeocells.indexOf(geocell) > -1) {
            targetMarkers.push(marker);
            return;
          }

          if (expandedRegion.contains(marker.get('position'))) {
            allowGeocells.push(geocell);
            targetMarkers.push(marker);
          } else {
            ignoreGeocells.push(geocell);
          }
        });
      } else {
        //--------------
        // zoom out
        //--------------
        var marker;
        var markerId;
        while (self._clusters[self.OUT_OF_RESOLUTION].length > 0) {
          marker = self._clusters[self.OUT_OF_RESOLUTION].shift();
          marker.get('_cluster').isAdded = false;
          markerId = marker.__pgmId.split(/\-/)[1];
          deleteClusters[markerId] = 1;

          if (marker.__pgmId === activeMarkerId) {
            marker.trigger(event.INFO_CLOSE);
            marker.hideInfoWindow();
          }
          if (self.debug) {
            console.log('---> (js:489)delete:' + markerId);
          }
        }
        keys = Object.keys(self._markerMap);
        keys.forEach(function (markerId) {
          var marker = self._markerMap[markerId];
          if (self._isRemoved ||
            !marker.isVisible() ||
            self._stopRequest ||
            marker.get('_cluster').isRemoved) {
            return;
          }

          var geocell = marker.get('_cluster').geocell.substr(0, cellLen);
          if (ignoreGeocells.indexOf(geocell) > -1) {
            return;
          }

          if (allowGeocells.indexOf(geocell) > -1) {
            targetMarkers.push(marker);
            return;
          }

          if (expandedRegion.contains(marker.get('position'))) {
            allowGeocells.push(geocell);
            targetMarkers.push(marker);
          } else {
            ignoreGeocells.push(geocell);
          }
        });
      }

    } else if (resolution === prevResolution) {
      //console.log('--->prevResolution(' + prevResolution + ') == resolution(' + resolution + ')');
      //--------------------------------------
      // Just camera move, no zoom changed
      //--------------------------------------

      keys = Object.keys(self._clusters[prevResolution]);
      keys.forEach(function (geocell) {
        if (self._isRemoved || self._stopRequest) {
          return;
        }
        var cluster = self._clusters[prevResolution][geocell];
        var bounds = cluster.getBounds();

        if (!self._isRemoved &&
          !expandedRegion.contains(bounds.northeast) &&
          !expandedRegion.contains(bounds.southwest)) {
          ignoreGeocells.push(geocell);

          if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
            cluster.getMarkers().forEach(function (marker) {
              var markerId = marker.__pgmId.split(/\-/)[1];
              deleteClusters[markerId] = 1;
              marker.get('_cluster').isAdded = false;
              if (self.debug) {
                console.log('---> (js:534)delete:' + markerId);
              }
              if (marker.__pgmId === activeMarkerId) {
                marker.trigger(event.INFO_CLOSE);
                marker.hideInfoWindow();
              }
            });
          } else {
            deleteClusters[geocell] = 1;
            if (self.debug) {
              console.log('---> (js:549)delete:' + geocell);
            }
          }
          cluster.remove();
          delete self._clusters[resolution][geocell];
        }
      });
      keys = Object.keys(self._markerMap);
      keys.forEach(function (markerId) {
        var marker = self._markerMap[markerId];
        var geocell = marker.get('_cluster').geocell.substr(0, cellLen);
        if (self._isRemoved ||
          self._stopRequest ||
          marker.get('_cluster').isRemoved ||
          !marker.isVisible() ||
          ignoreGeocells.indexOf(geocell) > -1 ||
          marker.get('_cluster').isAdded) {
          return;
        }

        if (allowGeocells.indexOf(geocell) > -1) {
          targetMarkers.push(marker);
          return;
        }

        if (expandedRegion.contains(marker.get('position'))) {
          targetMarkers.push(marker);
          allowGeocells.push(geocell);
        } else {
          ignoreGeocells.push(geocell);
          marker.get('_cluster').isAdded = false;
        }
      });

    } else if (prevResolution in self._clusters) {
      //console.log('--->prevResolution(' + prevResolution + ') != resolution(' + resolution + ')');

      if (prevResolution < resolution) {
        //--------------
        // zooming in
        //--------------
        keys = Object.keys(self._clusters[prevResolution]);
        keys.forEach(function (geocell) {
          if (self._isRemoved) {
            return;
          }
          var cluster = self._clusters[prevResolution][geocell];
          var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
          cluster.getMarkers().forEach(function (marker) {
            if (self._isRemoved ||
              self._stopRequest) {
              return;
            }
            var markerId = marker.__pgmId.split(/\-/)[1];
            marker.get('_cluster').isAdded = false;
            //targetMarkers.push(markerOpts);
            if (noClusterMode) {
              if (self.debug) {
                console.log('---> (js:581)delete:' + markerId);
              }
              if (marker.__pgmId === activeMarkerId) {
                marker.trigger(event.INFO_CLOSE);
                marker.hideInfoWindow();
              }
              deleteClusters[markerId] = 1;
            }
          });
          if (!noClusterMode) {
            if (self.debug) {
              console.log('---> (js:597)delete:' + geocell);
            }
            deleteClusters[geocell] = 1;
          }
          cluster.remove();
        });
      } else {
        //--------------
        // zooming out
        //--------------
        keys = Object.keys(self._clusters[prevResolution]);
        keys.forEach(function (geocell) {
          if (self._stopRequest ||
            self._isRemoved) {
            return;
          }
          var cluster = self._clusters[prevResolution][geocell];
          var noClusterMode = cluster.getMode() === cluster.NO_CLUSTER_MODE;
          cluster.getMarkers().forEach(function (marker) {
            marker.get('_cluster').isAdded = false;
            var markerId = marker.__pgmId.split(/\-/)[1];
            if (noClusterMode) {
              if (self.debug) {
                console.log('---> (js:614)delete:' + markerId);
              }
              if (marker.__pgmId === activeMarkerId) {
                marker.trigger(event.INFO_CLOSE);
                marker.hideInfoWindow();
              }
              deleteClusters[markerId] = 1;
            }
          });
          if (!noClusterMode) {
            deleteClusters[geocell] = 1;
            if (self.debug) {
              console.log('---> (js:632)delete:' + geocell);
            }
          }
          cluster.remove();

          geocell = geocell.substr(0, cellLen);
        });
      }
      keys = Object.keys(self._markerMap);
      keys.forEach(function (markerId) {
        if (self._stopRequest ||
          self._isRemoved) {
          return;
        }
        var marker = self._markerMap[markerId];
        var geocell = marker.get('_cluster').geocell.substr(0, cellLen);
        if (marker.get('_cluster').isRemoved ||
          ignoreGeocells.indexOf(geocell) > -1) {
          marker.get('_cluster').isAdded = false;
          return;
        }
        if (marker.get('_cluster').isAdded) {
          return;
        }

        if (allowGeocells.indexOf(geocell) > -1) {
          targetMarkers.push(marker);
          return;
        }

        if (expandedRegion.contains(marker.get('position'))) {
          targetMarkers.push(marker);
          allowGeocells.push(geocell);
        } else {
          ignoreGeocells.push(geocell);
          marker.get('_cluster').isAdded = false;
        }
      });
      delete self._clusters[prevResolution];
    } else {
      //console.log('-----> initialize');
      keys = Object.keys(self._markerMap);
      keys.forEach(function (markerId) {
        if (self._stopRequest ||
          self._isRemoved) {
          return;
        }
        var marker = self._markerMap[markerId];
        var geocell = marker.get('_cluster').geocell.substr(0, cellLen);
        if (marker.get('_cluster').isRemoved ||
          ignoreGeocells.indexOf(geocell) > -1) {
          return;
        }

        if (allowGeocells.indexOf(geocell) > -1) {
          targetMarkers.push(marker);
          return;
        }
        if (expandedRegion.contains(marker.get('position'))) {
          targetMarkers.push(marker);
          allowGeocells.push(geocell);
        } else {
          ignoreGeocells.push(geocell);
        }
      });
    }

    if (self._stopRequest ||
      self._isRemoved) {
      self._isWorking = false;
      return;
    }
    if (self.debug) {
      console.log('targetMarkers = ' + targetMarkers.length);
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

      self.set('prevSWcell', swCell);
      self.set('prevNEcell', neCell);

      if (resolution !== self.OUT_OF_RESOLUTION) {

        //------------------
        // Create clusters
        //------------------
        var prepareClusters = {};
        targetMarkers.forEach(function (marker) {
          if (marker.get('_cluster').isAdded) {
            if (self.debug) {
              console.log('isAdded', marker);
            }
            return;
          }
          var geocell = marker.get('_cluster').geocell.substr(0, resolution + 1);
          prepareClusters[geocell] = prepareClusters[geocell] || [];
          prepareClusters[geocell].push(marker);

          if (marker && marker.__pgmId === activeMarkerId) {
            marker.trigger(event.INFO_CLOSE);
            marker.hideInfoWindow();
          }
        });

        if (self.debug) {
          console.log('prepareClusters = ', prepareClusters, targetMarkers);
        }

        //------------------------------------------
        // Create/update clusters
        //------------------------------------------
        keys = Object.keys(prepareClusters);

        var sortedClusters = [];
        keys.forEach(function (geocell) {
          var cluster = self.getClusterByGeocellAndResolution(geocell, resolution);
          cluster.addMarkers(prepareClusters[geocell]);

          cluster._markerCenter = cluster.getBounds().getCenter();
          //cluster._distanceFrom0 = spherical.computeDistanceBetween({lat: 0, lng: 0}, cluster._markerCenter);
          sortedClusters.push(cluster);
        });

        sortedClusters = sortedClusters.sort(function (a, b) {
          return a.geocell.localeCompare(b.geocell);
        });

        //-------------------------
        // Union close clusters
        //-------------------------
        var cluster, anotherCluster, distance;
        var unionedMarkers = [];
        i = 0;
        while (i < sortedClusters.length) {
          cluster = sortedClusters[i];
          for (j = i + 1; j < sortedClusters.length; j++) {
            anotherCluster = sortedClusters[j];
            distance = spherical.computeDistanceBetween(cluster._markerCenter, anotherCluster._markerCenter);
            if (distance < params.clusterDistance) {
              if (self.debug) {
                console.log('---> (js:763)delete:' + anotherCluster.geocell);
              }
              cluster.addMarkers(anotherCluster.getMarkers());
              deleteClusters[anotherCluster.getId()] = 1;
              delete self._clusters[resolution][anotherCluster.geocell];
              self._clusters[resolution][cluster.geocell] = cluster;
              i = j;
            } else {
              break;
            }
          }
          i++;
          cluster._markerCnt = cluster.getItemLength();
          unionedMarkers.push(cluster);
        }

        unionedMarkers.forEach(function (cluster) {

          var icon = self.getClusterIcon(cluster),
            clusterOpts = {
              'count': cluster.getItemLength(),
              'position': cluster.getBounds().getCenter(),
              '__pgmId': cluster.getId()
            };

          if (self.debug) {
            clusterOpts.geocell = cluster.geocell;
          }

          if (icon) {
            clusterOpts.icon = icon;
            clusterOpts.isClusterIcon = true;
            if (cluster.getMode() === cluster.NO_CLUSTER_MODE) {
              cluster.getMarkers().forEach(function (marker) {
                var markerId = marker.__pgmId.split(/\-/)[1];
                deleteClusters[markerId] = 1;
                if (self.debug) {
                  console.log('---> (js:800)delete:' + markerId);
                }
              });
            }
            if (self.debug) {
              console.log('---> (js:805)add:' + clusterOpts.__pgmId, icon);
              var geocell = clusterOpts.geocell.substr(0, cellLen);
              var bounds = self._geocellBounds[geocell] || geomodel.computeBox(geocell);
              self._geocellBounds[geocell] = bounds;
              self.map.addPolyline({
                color: 'blue',
                points: [
                  bounds.southwest,
                  {
                    lat: bounds.southwest.lat,
                    lng: bounds.northeast.lng
                  },
                  bounds.northeast,
                  {
                    lat: bounds.northeast.lat,
                    lng: bounds.southwest.lng
                  },
                  bounds.southwest
                ]
              }, function (polyline) {
                self._clusterBounds.push(polyline);
              });
            }
            cluster.setMode(cluster.CLUSTER_MODE);
            new_or_update_clusters.push(clusterOpts);
            return;
          }

          cluster.getMarkers().forEach(function (marker) {
            if (!marker.get('_cluster').isAdded) {
              return;
            }
            var markerId = marker.__pgmId.split(/\-/)[1];
            delete deleteClusters[markerId];
            marker.get('_cluster').isClusterIcon = false;
            if (self.debug) {
              console.log('---> (js:831)add:' + markerId + ', isAdded = ' + marker.get('_cluster').isAdded);
              marker.set('title', markerId, true);
            }

            var opts = marker.getOptions();
            opts.__pgmId = markerId;
            new_or_update_clusters.push(opts);
          });
          cluster.setMode(cluster.NO_CLUSTER_MODE);
        });
      } else {
        cellLen = swCell.length;
        targetMarkers.forEach(function (marker) {
          if (marker.get('_cluster').isAdded) {
            return;
          }
          var markerId = marker.__pgmId.split(/\-/)[1];
          marker.get('_cluster').isClusterIcon = false;
          if (self.debug) {
            console.log('---> (js:859)add:' + markerId);
            marker.set('title', markerId, true);
          }
          delete deleteClusters[markerId];
          marker.get('_cluster').isAdded = true;
          new_or_update_clusters.push(marker.getOptions());
          self._clusters[self.OUT_OF_RESOLUTION].push(marker);
        });
      }
    }
    var delete_clusters = Object.keys(deleteClusters);

    if (self._stopRequest ||
      new_or_update_clusters.length === 0 && delete_clusters.length === 0) {
      self.trigger('nextTask');
      return;
    }

    if (self._stopRequest ||
      self._isRemoved) {
      self._isWorking = false;
      return;
    }

    self.exec.call(self, function (allResults) {
      var markerIDs = Object.keys(allResults);
      markerIDs.forEach(function (markerId) {
        if (!self._markerMap[markerId]) {
          return;
        }
        var marker = self._markerMap[markerId];
        var size = allResults[markerId];
        if (typeof marker.get('icon') === 'string') {
          marker.set('icon', {
            'url': marker.get('icon'),
            'size': size,
            'anchor': [size.width / 2, size.height]
          }, true);
        } else {
          var icon = marker.get('icon') || {};
          icon.size = icon.size || size;
          icon.anchor = icon.anchor || [size.width / 2, size.height];
          self._markerMap[markerId].set('icon', icon, true);
        }
        marker.set('infoWindowAnchor', marker.get('infoWindowAnchor') || [marker.get('icon').size.width / 2, 0], true);
      });
      self.trigger('nextTask');
    }, self.errorHandler, self.getPluginName(), 'redrawClusters', [self.getId(), {
      'resolution': resolution,
      'new_or_update': new_or_update_clusters,
      'delete': delete_clusters
    }], {
      sync: true
    });
    /*
        console.log({
                        'resolution': resolution,
                        'new_or_update': new_or_update_clusters,
                        'delete': delete_clusters
                      });
    */
  }
});

MarkerCluster.prototype.getClusterIcon = function (cluster) {
  var self = this,
    hit,
    clusterCnt = cluster.getItemLength();

  for (var i = 0; i < self.icons.length; i++) {
    hit = false;
    if ('min' in self.icons[i]) {
      if (clusterCnt >= self.icons[i].min) {
        if ('max' in self.icons[i]) {
          hit = (clusterCnt <= self.icons[i].max);
        } else {
          hit = true;
        }
      }
    } else {
      if ('max' in self.icons[i]) {
        hit = (clusterCnt <= self.icons[i].max);
      }
    }
    if (hit) {
      return self.icons[i];
    }
  }
  return null;
};

MarkerCluster.prototype._createMarker = function (markerOpts) {
  var self = this;
  var markerId = self.getId() + '-marker_' + Math.floor(Date.now() * Math.random());
  var marker = new Marker(self.getMap(), markerOpts, exec, {
    className: 'MarkerCluster',
    __pgmId: markerId
  });
  marker._privateInitialize(markerOpts);
  delete marker._privateInitialize;

  // Recalulate geocell if marker position is changed.
  marker.onThrottled('position_changed', function(ignore, newPosition) {
    marker.get('_cluster').geocell = geomodel.getGeocell(newPosition.lat, newPosition.lng, self.MAX_RESOLUTION + 1);
  }, 500);
  marker.one(marker.getId() + '_remove', function () {
    self._removeMarkerById(markerId);
  });
  return marker;
};

MarkerCluster.prototype.getClusterByGeocellAndResolution = function (geocell, resolution) {
  var self = this;
  geocell = geocell.substr(0, resolution + 1);

  var cluster = self._clusters[resolution][geocell];
  if (!cluster) {
    cluster = new Cluster(geocell, geocell);
    self._clusters[resolution][geocell] = cluster;
  }
  return cluster;
};

/*
 * http://jsfiddle.net/john_s/BHHs8/6/
 */
function latRad(lat) {
  var sin = Math.sin(lat * Math.PI / 180);
  var radX2 = Math.log((1 + sin) / (1 - sin)) / 2;
  return Math.max(Math.min(radX2, Math.PI), -Math.PI) / 2;
}

function zoom(mapPx, worldPx, fraction) {
  return Math.log(mapPx / worldPx / fraction) / Math.LN2;
}

function computeZoom(bounds, mapDim) {
  var tileSize = cordova.platformId === 'browser' ? 256 : 512;
  var ZOOM_MAX = 21;

  var ne = bounds.northeast;
  var sw = bounds.southwest;

  var latFraction = (latRad(ne.lat) - latRad(sw.lat)) / Math.PI;

  var lngDiff = ne.lng - sw.lng;
  var lngFraction = ((lngDiff < 0) ? (lngDiff + 360) : lngDiff) / 360;

  var latZoom = zoom(mapDim.offsetHeight, tileSize, latFraction);
  var lngZoom = zoom(mapDim.offsetWidth, tileSize, lngFraction);

  return Math.min(latZoom, lngZoom, ZOOM_MAX);
}

module.exports = MarkerCluster;
