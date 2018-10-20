var LatLngBounds = require('./LatLngBounds');

/*****************************************************************************
 * Cluster Class
 *****************************************************************************/
var Cluster = function(__pgmId, geocell) {
  var obj = {};

  var self = this;
  Object.defineProperty(self, '__pgmId', {
    value: __pgmId,
    writable: false
  });
  Object.defineProperty(self, 'geocell', {
    value: geocell,
    writable: false
  });

  Object.defineProperty(self, 'type', {
    value: 'Cluster',
    writable: false
  });
  Object.defineProperty(self, '_markerArray', {
    value: [],
    writable: false
  });


  Object.defineProperty(self, 'set', {
    value: function(key, value) {
      obj[key] = value;
    },
    writable: false
  });

  Object.defineProperty(self, 'get', {
    value: function(key) {
      return obj[key];
    },
    writable: false
  });
};

Cluster.prototype.NO_CLUSTER_MODE = 1;
Cluster.prototype.CLUSTER_MODE = 2;

Cluster.prototype.getPluginName = function() {
  return this.map.getId() + '-cluster';
};
Cluster.prototype.getBounds = function() {
  return this.get('bounds');
};
/*
Cluster.prototype.getBounds = function() {
  var bounds = this.get('bounds');
  if (!bounds) {
    bounds = geomodel.computeBox(this.geocell);
    this.set('bounds', bounds);
  }
  return bounds;
};
*/
Cluster.prototype.getCenter = function() {
  return this.getBounds().getCenter();
};

Cluster.prototype.getMarkers = function() {
  return this._markerArray;
};

Cluster.prototype.addMarkers = function(markerRefs) {
  var self = this;
  var bounds = this.get('bounds') || new LatLngBounds(markerRefs[0].get('position'), markerRefs[0].get('position'));

  markerRefs.forEach(function(marker) {
    if (self._markerArray.indexOf(marker) === -1) {
      marker.get('_cluster').isAdded = true;
      self._markerArray.push(marker);
      bounds.extend(marker.get('position'));
    }
  });

  this.set('bounds', bounds);
};
Cluster.prototype.getId = function() {
  return this.__pgmId;
};
Cluster.prototype.setMode = function(mode) {
  this.set('mode', mode);
};
Cluster.prototype.getMode = function() {
  return this.get('mode');
};
Cluster.prototype.removeMarker = function(marker) {

  var idx = this._markerArray.indexOf(marker);
  if (idx !== -1) {
    this._markerArray.splice(idx, 1);
  }
};

Cluster.prototype.remove = function() {
  this.set('isRemoved', true);
  this._markerArray.forEach(function(marker) {
    marker.get('_cluster').isAdded = false;
  });
};
Cluster.prototype.getItemLength = function() {
  return this._markerArray.length;
};

module.exports = Cluster;
