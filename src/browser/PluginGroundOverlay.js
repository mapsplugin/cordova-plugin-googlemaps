


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginGroundOverlay(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    writable: false
  });
}

utils.extend(PluginGroundOverlay, BaseClass);

PluginGroundOverlay.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    groundoverlayId = 'groundoverlay_' + args[2],
    pluginOptions = args[1];

  self._createOverlay(map, groundoverlayId, pluginOptions);

  onSuccess({
    'id': groundoverlayId
  });
};
PluginGroundOverlay.prototype.setImage = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    url = args[1],
    map = self.pluginMap.get('map'),
    groundoverlay = self.pluginMap.objects[overlayId]
    pluginOptions = self.pluginMap.objects['groundoverlay_property_' + overlayId];

  if (groundoverlay) {
    google.maps.event.clearInstanceListeners(groundoverlay);
    groundoverlay.setMap(null);
    groundoverlay = undefined;

    pluginOptions.url = url;

    self._createOverlay(map, overlayId, pluginOptions);
  }


  onSuccess();
};

PluginGroundOverlay.prototype._createOverlay = function(map, groundoverlayId, pluginOptions) {
  var self = this;

  var groundoverlayOpts = {
    'overlayId': groundoverlayId,
    'map': map
  };

  groundoverlayOpts.url = pluginOptions.url;

  var bounds = new google.maps.LatLngBounds();
  pluginOptions.bounds.forEach(function(latLng) {
    bounds.extend(latLng);
  });
  groundoverlayOpts.bounds = bounds;

  if ('zIndex' in pluginOptions) {
    console.warn('Google Maps JS API v3 does not support z-index property for GroundOverlay');
  }
  if ('opacity' in pluginOptions) {
    groundoverlayOpts.opacity = pluginOptions.opacity;
  }
  if ('visible' in pluginOptions) {
    groundoverlayOpts.visible = pluginOptions.visible;
  }
  if ('clickable' in pluginOptions) {
    groundoverlayOpts.clickable = pluginOptions.clickable;
  }

  var groundoverlay = new google.maps.GroundOverlay(groundoverlayOpts.url, groundoverlayOpts.bounds, groundoverlayOpts);
  groundoverlay.addListener('click', function(polyMouseEvt) {
    self._onGroundOverlayEvent.call(self, groundoverlay, polyMouseEvt);
  });

  self.pluginMap.objects[groundoverlayId] = groundoverlay;
  self.pluginMap.objects['groundoverlay_property_' + groundoverlayId] = pluginOptions;

  return groundoverlay;
};

PluginGroundOverlay.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {
    google.maps.event.clearInstanceListeners(groundOverlay);
    groundOverlay.setMap(null);
    groundOverlay = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
    delete self.pluginMap.objects['groundoverlay_property_' + overlayId];
  }
  onSuccess();
};

PluginGroundOverlay.prototype._onGroundOverlayEvent = function(groundoverlay, mouseEvt) {
  var self = this,
    mapId = self.pluginMap.id;
  if (mapId in plugin.google.maps) {
    plugin.google.maps[mapId]({
      'evtName': event.GROUND_OVERLAY_CLICK,
      'callback': '_onOverlayEvent',
      'args': [groundoverlay.overlayId, new plugin.google.maps.LatLng(mouseEvt.latLng.lat(), mouseEvt.latLng.lng())]
    });
  }

};
module.exports = PluginGroundOverlay;
