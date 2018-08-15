


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginCircle(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    enumerable: false,
    writable: false
  });
}

utils.extend(PluginCircle, BaseClass);

PluginCircle.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    circleId = 'circle_' + args[2],
    pluginOptions = args[1];

  var circleOpts = {
    'overlayId': circleId,
    'map': map
  };

  circleOpts.center = pluginOptions.center;
  circleOpts.radius = pluginOptions.radius;

  if (Array.isArray(pluginOptions.strokeColor)) {
    circleOpts.strokeColor = 'rgb(' + pluginOptions.strokeColor[0] + ',' + pluginOptions.strokeColor[1] + ',' + pluginOptions.strokeColor[2] + ')';
    circleOpts.strokeOpacity = pluginOptions.strokeColor[3] / 256;
  }
  if (Array.isArray(pluginOptions.fillColor)) {
    circleOpts.fillColor = 'rgb(' + pluginOptions.fillColor[0] + ',' + pluginOptions.fillColor[1] + ',' + pluginOptions.fillColor[2] + ')';
    circleOpts.fillOpacity = pluginOptions.fillColor[3] / 256;
  }
  if ('width' in pluginOptions) {
    circleOpts.strokeWeight = pluginOptions.width;
  }
  if ('zIndex' in pluginOptions) {
    circleOpts.zIndex = pluginOptions.zIndex;
  }
  if ('visible' in pluginOptions) {
    circleOpts.visible = pluginOptions.visible;
  }
  if ('clickable' in pluginOptions) {
    circleOpts.clickable = pluginOptions.clickable;
  }

  var circle = new google.maps.Circle(circleOpts);
  circle.addListener('click', function(polyMouseEvt) {
    self._onCircleEvent.call(self, circle, polyMouseEvt);
  });

  self.pluginMap.objects[circleId] = circle;
  self.pluginMap.objects['circle_property_' + circleId] = circleOpts;

  onSuccess({
    'id': circleId
  });
};
PluginCircle.prototype.setRadius = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var radius = args[1];
  var circle = self.pluginMap.objects[overlayId];
  if (circle) {
    var index = args[1];
    circle.setRadius(radius);
  }
  onSuccess();
};

PluginCircle.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var circle = self.pluginMap.objects[overlayId];
  if (circle) {
    google.maps.event.clearInstanceListeners(circle);
    circle.setMap(null);
    circle = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

PluginCircle.prototype._onCircleEvent = function(circle, polyMouseEvt) {
  var self = this,
    mapId = self.pluginMap.id;
  if (mapId in plugin.google.maps) {
    plugin.google.maps[mapId]({
      'evtName': event.CIRCLE_CLICK,
      'callback': '_onOverlayEvent',
      'args': [circle.overlayId, new plugin.google.maps.LatLng(polyMouseEvt.latLng.lat(), polyMouseEvt.latLng.lng())]
    });
  }

};
module.exports = PluginCircle;
