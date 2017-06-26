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
var Cluster = function(geocell, markerRefs, resolution) {
  BaseClass.call(this);

  markerRefs = common.createMvcArray(markerRefs);

  markerRefs.forEach(function(markerRef) {
    markerRef.set("isAdded", true);
  });

  var self = this;
  Object.defineProperty(self, "geocell", {
    value: geocell,
    writable: false
  });
/*
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
*/
  Object.defineProperty(self, "type", {
    value: "Cluster",
    writable: false
  });
  Object.defineProperty(self, "bounds", {
    value: geomodel.computeBox(geocell),
    writable: false
  });
  Object.defineProperty(self, "_markerRefs", {
    value: markerRefs,
    writable: false
  });

  self.set("resolution", resolution);

/*
  map.addMarker({
    'position': self.bounds.getCenter(),
    'title': geocell,
    'icon': "https://mt.google.com/vt/icon/text=" + markerRefs.getLength() + "&psize=16&font=fonts/arialuni_t.ttf&color=ff330000&name=icons/spotlight/spotlight-waypoint-b.png&ax=44&ay=48&scale=1"
  }, function(marker) {
    self.set("marker", marker);

    self.one("resolution_changed", marker.remove.bind(marker));
  });

  map.addPolygon({
    'points': [
      this.bounds.northeast,
      {lat: this.bounds.northeast.lat, lng: this.bounds.southwest.lng},
      this.bounds.southwest,
      {lat: this.bounds.southwest.lat, lng: this.bounds.northeast.lng}
    ],
    'strokeColor' : 'blue',
    'strokeWidth': 2,
    'fillColor': 'transparent',
    'noCache': true
  }, function(polygon) {
    self.set("polygon", polygon);
    self.one("resolution_changed", polygon.remove.bind(polygon));
  });
*/

};

utils.extend(Cluster, BaseClass);

Cluster.prototype.getPluginName = function() {
  return this.map.getId() + "-Cluster";
};
Cluster.prototype.getBounds = function() {
  return this.bounds;
};
Cluster.prototype.getCenter = function() {
  return this.bounds.getCenter();
};

Cluster.prototype.addMarkers = function(markerRefs) {
  var self = this;
  markerRefs.forEach(function(markerRef) {
    if (self._markerRefs.indexOf(markerRef) > -1) {
      self._markerRefs.push(markerRef);
    }
  });
};
/*
Cluster.prototype.getMap = function() {
  return this.map;
};
*/
Cluster.prototype.remove = function() {
  this.set("isRemoved", true);
  this._markerRefs.forEach(function(markerRef) {
    markerRef.set("isAdded", false);
  });
/*
  this.get("marker").remove();
  this.get("polygon").remove();
*/
  this.off();

};
Cluster.prototype.getItemLength = function() {
  return this._markerRefs.getLength();
};

module.exports = Cluster;
