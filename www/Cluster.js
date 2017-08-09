var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    common = require('./Common'),
    Marker = require('./Marker'),
    geomodel = require('./geomodel'),
    BaseClass = require('./BaseClass'),
    LatLngBounds = require('./LatLngBounds');

/*****************************************************************************
 * Cluster Class
 *****************************************************************************/
var Cluster = function(id, geocell) {
  var obj = {};

  var self = this;
  Object.defineProperty(self, "id", {
    value: id,
    writable: false
  });
  Object.defineProperty(self, "geocell", {
    value: geocell,
    writable: false
  });

  Object.defineProperty(self, "type", {
    value: "Cluster",
    writable: false
  });
  Object.defineProperty(self, "_markerOptsArray", {
    value: [],
    writable: false
  });


  Object.defineProperty(self, "set", {
    value: function(key, value) {
      obj[key] = value;
    },
    writable: false
  });

  Object.defineProperty(self, "get", {
    value: function(key) {
      return obj[key];
    },
    writable: false
  });
};

Cluster.prototype.NO_CLUSTER_MODE = 1;
Cluster.prototype.CLUSTER_MODE = 2;

Cluster.prototype.getPluginName = function() {
  return this.map.getId() + "-cluster";
};
Cluster.prototype.getBounds = function() {
  return this.get("bounds");
};
/*
Cluster.prototype.getBounds = function() {
  var bounds = this.get("bounds");
  if (!bounds) {
    bounds = geomodel.computeBox(this.geocell);
    this.set("bounds", bounds);
  }
  return bounds;
};
*/
Cluster.prototype.getCenter = function() {
  return this.getBounds().getCenter();
};

Cluster.prototype.getMarkers = function() {
  return this._markerOptsArray;
};

Cluster.prototype.addMarkers = function(markerRefs) {
  var self = this;
  var bounds = this.get("bounds") || new LatLngBounds(markerRefs[0].position, markerRefs[0].position);

  markerRefs.forEach(function(markerOpts) {
    if (self._markerOptsArray.indexOf(markerOpts) === -1) {
      markerOpts._cluster.isAdded = true;
      self._markerOptsArray.push(markerOpts);
      bounds.extend(markerOpts.position);
    }
  });

  this.set("bounds", bounds);
};
Cluster.prototype.getId = function() {
  return this.id;
};
Cluster.prototype.setMode = function(mode) {
  this.set("mode", mode);
};
Cluster.prototype.getMode = function() {
  return this.get("mode");
};
Cluster.prototype.removeMarker = function(markerOpts) {

  var idx = this._markerOptsArray.indexOf(markerOpts);
  if (idx !== -1) {
    this._markerOptsArray.splice(idx, 1);
  }
};

Cluster.prototype.remove = function() {
  this.set("isRemoved", true);
  this._markerOptsArray.forEach(function(markerOpts) {
    markerOpts._cluster.isAdded = false;
  });
};
Cluster.prototype.getItemLength = function() {
  return this._markerOptsArray.length;
};

module.exports = Cluster;
