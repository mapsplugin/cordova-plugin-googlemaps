

var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginPolyline(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    writable: false
  });
}

utils.extend(PluginPolyline, BaseClass);

PluginPolyline.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    polylineId = 'polyline_' + args[2],
    pluginOptions = args[1];

  var polylineOpts = {
    'overlayId': polylineId,
    'map': map,
    'path': new google.maps.MVCArray()
  };

  if (pluginOptions.points) {
    pluginOptions.points.forEach(function(point) {
      polylineOpts.path.push(new google.maps.LatLng(point.lat, point.lng));
    });
  }
  if (Array.isArray(pluginOptions.color)) {
    polylineOpts.strokeColor = 'rgb(' + pluginOptions.color[0] + ',' + pluginOptions.color[1] + ',' + pluginOptions.color[2] + ')';
    polylineOpts.strokeOpacity = pluginOptions.color[3] / 256;
  }
  if ('width' in pluginOptions) {
    polylineOpts.strokeWeight = pluginOptions.width;
  }
  if ('zIndex' in pluginOptions) {
    polylineOpts.zIndex = pluginOptions.zIndex;
  }
  if ('visible' in pluginOptions) {
    polylineOpts.visible = pluginOptions.visible;
  }
  if ('geodesic' in pluginOptions) {
    polylineOpts.geodesic = pluginOptions.geodesic;
  }
  if ('clickable' in pluginOptions) {
    polylineOpts.clickable = pluginOptions.clickable;
  }

  var polyline = new google.maps.Polyline(polylineOpts);
  polyline.addListener('click', function(polyMouseEvt) {
    self._onPolylineEvent.call(self, polyline, polyMouseEvt);
  });

  self.pluginMap.objects[polylineId] = polyline;
  self.pluginMap.objects['polyline_property_' + polylineId] = polylineOpts;

  onSuccess({
    'id': polylineId
  });
};
PluginPolyline.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polyline = self.pluginMap.objects[overlayId];
  if (polyline) {
    google.maps.event.clearInstanceListeners(polyline);
    polyline.setMap(null);
    polyline = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

PluginPolyline.prototype.setPointAt = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polyline = self.pluginMap.objects[overlayId];
  if (polyline) {
    var index = args[1];
    var latLng = new google.maps.LatLng(args[2].lat, args[2].lng);
    var opts = self.pluginMap.objects['polyline_property_' + overlayId];
    opts.path.setAt(index, latLng);
  }
  onSuccess();
};

PluginPolyline.prototype._onPolylineEvent = function(polyline, polyMouseEvt) {
  var self = this,
    mapId = self.pluginMap.id;
  if (mapId in plugin.google.maps) {
    plugin.google.maps[mapId]({
      'evtName': event.POLYLINE_CLICK,
      'callback': '_onOverlayEvent',
      'args': [polyline.overlayId, new plugin.google.maps.LatLng(polyMouseEvt.latLng.lat(), polyMouseEvt.latLng.lng())]
    });
  }

};
module.exports = PluginPolyline;
