var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    Marker = require('./Marker'),
    geomodel = require('./geomodel'),
    BaseClass = require('./BaseClass'),
    LatLngBounds = require('./LatLngBounds'),
    BaseArrayClass = require('./BaseArrayClass');

/*****************************************************************************
 * Cluster Class
 *****************************************************************************/
var Cluster = function(geocell) {
  var obj = {};

  var self = this;
  Object.defineProperty(self, "geocell", {
    value: geocell,
    writable: false
  });

  Object.defineProperty(self, "type", {
    value: "Cluster",
    writable: false
  });
  Object.defineProperty(self, "_markerRefs", {
    value: new BaseArrayClass(),
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
  return this._markerRefs.getArray();
};

Cluster.prototype.addMarkers = function(markerRefs) {
  var self = this;
  var bounds = this.get("bounds") || new LatLngBounds(markerRefs[0].getPosition(), markerRefs[0].getPosition());

  markerRefs.forEach(function(markerRef) {
    if (self._markerRefs.indexOf(markerRef) === -1) {
      markerRef.set("isAdded", true);
      self._markerRefs.push(markerRef);
      bounds.extend(markerRef.getPosition());
    }
  });

  this.set("bounds", bounds);
};
Cluster.prototype.setMode = function(mode) {
  this.set("mode", mode);
};
Cluster.prototype.getMode = function() {
  return this.get("mode");
};

Cluster.prototype.remove = function() {
  this.set("isRemoved", true);
  this._markerRefs.forEach(function(markerRef) {
    markerRef.set("isAdded", false);
  });

};
Cluster.prototype.getItemLength = function() {
  return this._markerRefs.getLength();
};

module.exports = Cluster;
