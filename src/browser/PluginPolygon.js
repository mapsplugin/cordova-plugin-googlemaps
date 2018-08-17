


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginPolygon(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    writable: false
  });
}

utils.extend(PluginPolygon, BaseClass);

PluginPolygon.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    polygonId = 'polygon_' + args[2],
    pluginOptions = args[1];

  var polygonOpts = {
    'overlayId': polygonId,
    'map': map,
    'paths': new google.maps.MVCArray()
  };

  if (pluginOptions.points) {
    var strokePath = new google.maps.MVCArray();
    pluginOptions.points.forEach(function(point) {
      strokePath.push(new google.maps.LatLng(point.lat, point.lng));
    });
    polygonOpts.paths.push(strokePath);
  }
  if (Array.isArray(pluginOptions.strokeColor)) {
    polygonOpts.strokeColor = 'rgb(' + pluginOptions.strokeColor[0] + ',' + pluginOptions.strokeColor[1] + ',' + pluginOptions.strokeColor[2] + ')';
    polygonOpts.strokeOpacity = pluginOptions.strokeColor[3] / 256;
  }
  if (Array.isArray(pluginOptions.fillColor)) {
    polygonOpts.fillColor = 'rgb(' + pluginOptions.fillColor[0] + ',' + pluginOptions.fillColor[1] + ',' + pluginOptions.fillColor[2] + ')';
    polygonOpts.fillOpacity = pluginOptions.fillColor[3] / 256;
  }
  if ('width' in pluginOptions) {
    polygonOpts.strokeWeight = pluginOptions.width;
  }
  if ('zIndex' in pluginOptions) {
    polygonOpts.zIndex = pluginOptions.zIndex;
  }
  if ('visible' in pluginOptions) {
    polygonOpts.visible = pluginOptions.visible;
  }
  if ('geodesic' in pluginOptions) {
    polygonOpts.geodesic = pluginOptions.geodesic;
  }
  if ('clickable' in pluginOptions) {
    polygonOpts.clickable = pluginOptions.clickable;
  }

  var polygon = new google.maps.Polygon(polygonOpts);
  polygon.addListener('click', function(polyMouseEvt) {
    self._onPolygonEvent.call(self, polygon, polyMouseEvt);
  });

  self.pluginMap.objects[polygonId] = polygon;
  self.pluginMap.objects['polygon_property_' + polygonId] = polygonOpts;

  onSuccess({
    'id': polygonId
  });
};
PluginPolygon.prototype.setPointAt = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    var index = args[1];
    var latLng = new google.maps.LatLng(args[2].lat, args[2].lng);
    var opts = self.pluginMap.objects['polygon_property_' + overlayId];
    var strokePath = opts.paths.getAt(0);
    strokePath.setAt(index, latLng);
  }
  onSuccess();
};

PluginPolygon.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    google.maps.event.clearInstanceListeners(polygon);
    polygon.setMap(null);
    polygon = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

PluginPolygon.prototype._onPolygonEvent = function(polygon, polyMouseEvt) {
  var self = this,
    mapId = self.pluginMap.id;
  if (mapId in plugin.google.maps) {
    plugin.google.maps[mapId]({
      'evtName': event.POLYGON_CLICK,
      'callback': '_onOverlayEvent',
      'args': [polygon.overlayId, new plugin.google.maps.LatLng(polyMouseEvt.latLng.lat(), polyMouseEvt.latLng.lng())]
    });
  }

};
module.exports = PluginPolygon;
