
var utils = require('cordova/utils'),
 event = require('cordova-plugin-googlemaps.event'),
 BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
 LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginFusionTableOverlay(pluginMap) {
 var self = this;
 BaseClass.apply(self);
 Object.defineProperty(self, "pluginMap", {
   value: pluginMap,
   enumerable: false,
   writable: false
 });

}

utils.extend(PluginFusionTableOverlay, BaseClass);

PluginFusionTableOverlay.prototype._create = function(onSuccess, onError, args) {
 var self = this,
   map = self.pluginMap.get('map'),
   hashCode = args[2],
   fusionTableOverlayId = 'FusionTableOverlay_' + hashCode,
   pluginOptions = args[1],
   mapId = self.pluginMap.id;

 var fusionTableOpts = {
   'map': map,
   'query': {
     'select': pluginOptions.select,
     'from': pluginOptions.from
   }
 };
 if (pluginOptions.where) {
   fusionTableOpts.query.where = pluginOptions.where;
 }
 if (pluginOptions.orderBy) {
   fusionTableOpts.query.orderBy = pluginOptions.orderBy;
 } else if (pluginOptions.offset) {
   fusionTableOpts.query.offset = pluginOptions.offset;
 }
 if (pluginOptions.limit) {
   fusionTableOpts.query.limit = pluginOptions.limit;
 }

 var fusionTableOverlay = new google.maps.FusionTablesLayer(fusionTableOpts);

 self.pluginMap.objects[fusionTableOverlayId] = fusionTableOverlay;

 onSuccess({
   'id': fusionTableOverlayId
 });
};


PluginFusionTableOverlay.prototype.setVisible = function(onSuccess, onError, args) {
 var self = this,
   map = self.pluginMap.get('map'),
   overlayId = args[0],
   fusionTableOverlay = self.pluginMap.objects[overlayId];

 if (fusionTableOverlay) {
   FusionTableOverlay.setMap(args[1] ? map : null);
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
