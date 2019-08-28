


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass');

function PluginHeatmap(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, 'pluginMap', {
    value: pluginMap,
    writable: false
  });
}

utils.extend(PluginHeatmap, BaseClass);

PluginHeatmap.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    heatmapId = 'heatmap_' + args[2],
    pluginOptions = args[1];

  var heatmapOpts = {
    'overlayId': heatmapId,
    'map': map,
    'data' : pluginOptions.data
  };
  
  if ('zIndex' in pluginOptions) {
    heatmapOpts.zIndex = pluginOptions.zIndex;
  }
  if ('visible' in pluginOptions) {
    heatmapOpts.visible = pluginOptions.visible;
  }
  if ('clickable' in pluginOptions) {
    heatmapOpts.clickable = pluginOptions.clickable;
  }

  var heatmap = new google.maps.Heatmap(heatmapOpts);
  /*circle.addListener('click', function(polyMouseEvt) {
    self._onCircleEvent.call(self, circle, polyMouseEvt);
  });*/

  self.pluginMap.objects[heatmapId] = heatmap;

  onSuccess({
    '__pgmId': heatmapId
  });
};

PluginHeatmap.prototype.setData = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var data = args[1];
  var heatmap = self.pluginMap.objects[overlayId];
  if (heatmap) {
    heatmap.setData(data);
  }
  onSuccess();
};

PluginHeatmap.prototype.setZIndex = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var zIndex = args[1];
  var heatmap = self.pluginMap.objects[overlayId];
  if (heatmap) {
    heatmap.setOptions({
      'zIndex': zIndex
    });
  }
  onSuccess();
};

PluginHeatmap.prototype.setVisible = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var visible = args[1];
  var heatmap = self.pluginMap.objects[overlayId];
  if (heatmap) {
    heatmap.setVisible(visible === true);
  }
  onSuccess();
};

PluginHeatmap.prototype.setClickable = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var clickable = args[1];
  var heatmap = self.pluginMap.objects[overlayId];
  if (heatmap) {
    heatmap.setOptions({
      'clickable': clickable === true
    });
  }
  onSuccess();
};

PluginHeatmap.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var heatmap = self.pluginMap.objects[overlayId];
  if (heatmap) {
    google.maps.event.clearInstanceListeners(heatmap);
    heatmap.remove();
    circle = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

PluginHeatmap.prototype._onCircleEvent = function(circle, polyMouseEvt) {
  var self = this,
    mapId = self.pluginMap.__pgmId;
  if (mapId in plugin.google.maps) {
    plugin.google.maps[mapId]({
      'evtName': event.HEATMAP_CLICK,
      'callback': '_onOverlayEvent',
      'args': [circle.overlayId, new plugin.google.maps.LatLng(polyMouseEvt.latLng.lat(), polyMouseEvt.latLng.lng())]
    });
  }

};
module.exports = PluginHeatmap;
