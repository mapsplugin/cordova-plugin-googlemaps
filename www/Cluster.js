var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    Marker = require('./Marker'),
    geomodel = require('./geomodel'),
    BaseClass = require('./BaseClass'),
    BaseArrayClass = require('./BaseArrayClass');

/*****************************************************************************
 * Cluster Class
 *****************************************************************************/
var Cluster = function(geocell) {
  BaseClass.call(this);

  var self = this;
  Object.defineProperty(self, "geocell", {
    value: geocell,
    writable: false
  });

  Object.defineProperty(self, "type", {
    value: "Cluster",
    writable: false
  });
  Object.defineProperty(self, "bounds", {
    value: geomodel.computeBox(geocell),
    writable: false
  });
  Object.defineProperty(self, "_markerRefs", {
    value: new BaseArrayClass(),
    writable: false
  });

};

utils.extend(Cluster, BaseClass);

Cluster.prototype.NO_CLUSTER_MODE = 1;
Cluster.prototype.CLUSTER_MODE = 2;

Cluster.prototype.getPluginName = function() {
  return this.map.getId() + "-cluster";
};
Cluster.prototype.getBounds = function() {
  return this.bounds;
};
Cluster.prototype.getCenter = function() {
  return this.bounds.getCenter();
};

Cluster.prototype.getMarkers = function() {
  return this._markerRefs.getArray();
};

Cluster.prototype.addMarkers = function(markerRefs) {
  var self = this;
  markerRefs.forEach(function(markerRef) {
    if (self._markerRefs.indexOf(markerRef) === -1) {
      markerRef.set("isAdded", true);
      self._markerRefs.push(markerRef);
    }
  });
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
  this.off();

};
Cluster.prototype.getItemLength = function() {
  return this._markerRefs.getLength();
};

module.exports = Cluster;
