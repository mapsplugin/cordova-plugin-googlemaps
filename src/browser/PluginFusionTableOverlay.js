
var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginFusionTableOverlay(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, 'pluginMap', {
    value: pluginMap,
    enumerable: false,
    writable: false
  });

}

utils.extend(PluginFusionTableOverlay, BaseClass);

PluginFusionTableOverlay.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    fusionTableOverlayId = args[2],
    pluginOptions = args[1],
    mapId = self.pluginMap.__pgmId;

  var fusionTableOpts = {
    'map': map,
    'query': {
      'select': pluginOptions.query.select,
      'from': pluginOptions.query.from
    }
  };
  if (pluginOptions.where) {
    fusionTableOpts.query.where = pluginOptions.query.where;
  }
  if (pluginOptions.orderBy) {
    fusionTableOpts.query.orderBy = pluginOptions.query.orderBy;
  } else if (pluginOptions.offset) {
    fusionTableOpts.query.offset = pluginOptions.query.offset;
  }
  if (pluginOptions.limit) {
    fusionTableOpts.query.limit = pluginOptions.query.limit;
  }

  var fusionTableOverlay = new google.maps.FusionTablesLayer(fusionTableOpts);

  fusionTableOverlay.addListener('click', function(mouseEvent) {

    if (mapId in plugin.google.maps) {
      plugin.google.maps[mapId]({
        'evtName': event.FUSION_TABLE_CLICK,
        'callback': '_onOverlayEvent',
        'args': [fusionTableOverlayId, {
          'infoWindowHtml': mouseEvent.infoWindowHtml,
          'latLng': new LatLng(mouseEvent.latLng.lat(), mouseEvent.latLng.lng()),
          'row': mouseEvent.row
        }]
      });
    }
  });

  self.pluginMap.objects[fusionTableOverlayId] = fusionTableOverlay;

  onSuccess({
    '__pgmId': fusionTableOverlayId
  });
};


PluginFusionTableOverlay.prototype.setVisible = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    overlayId = args[0],
    fusionTableOverlay = self.pluginMap.objects[overlayId];

  if (fusionTableOverlay) {
    fusionTableOverlay.setMap(args[1] ? map : null);
  }
  onSuccess();
};

PluginFusionTableOverlay.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var fusionTableOverlay = self.pluginMap.objects[overlayId];
  if (fusionTableOverlay) {
    google.maps.event.clearInstanceListeners(fusionTableOverlay);
    fusionTableOverlay.setMap(null);
    fusionTableOverlay = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

module.exports = PluginFusionTableOverlay;
