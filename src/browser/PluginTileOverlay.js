


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function TileOverlay(mapId, hashCode, options) {
  var self = this,
    tileSize = 256,
    tileCaches = {};

  var mapType = {
    name: hashCode,
    mapTypeId: hashCode,

    getTileFromCache: function(cacheId) {
      return tileCaches[cacheId];
    },

    opacity: 'opacity' in options ? options.opacity : 1,

    tileSize: new google.maps.Size(tileSize, tileSize),

    getTile: function(coord, zoom, owner) {

      var cacheId = [mapId, hashCode, coord.x, coord.y, zoom].join("-");
      if (cacheId in tileCaches) {
        return tileCaches[cacheId];
      } else {
        var div = document.createElement('div');
        div.style.width = tileSize + 'px';
        div.style.height = tileSize + 'px';
        if (options.debug) {
          div.style.borderLeft = "1px solid red";
          div.style.borderTop = "1px solid red";
          div.style.color = 'red';
          div.style.fontSize = '12px';
          div.style.padding = '1em';
          div.innerHTML = "x = " + coord.x + ", y = " + coord.y + ", zoom = " + zoom;
        }
        div.setAttribute('cacheId', cacheId);
        tileCaches[cacheId] = div;

        options.getTile(coord.x, coord.y, zoom, cacheId);
        return div;
      }
    },

    releaseTile: function(div) {
      var cacheId = div.getAttribute('cacheId');
      delete tileCaches[cacheId];
    }
  };

  return mapType;
}


function PluginTileOverlay(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    enumerable: false,
    writable: false
  });

}

utils.extend(PluginTileOverlay, BaseClass);

PluginTileOverlay.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    hashCode = args[2],
    tileoverlayId = 'tileoverlay_' + hashCode,
    pluginOptions = args[1],
    mapId = self.pluginMap.id,
    getTileEventName = mapId + "-" + hashCode + "-tileoverlay";


  pluginOptions.getTile = function(x, y, zoom, urlCallbackId) {
    cordova.fireDocumentEvent(getTileEventName, {
      key: urlCallbackId,
      x: x,
      y: y,
      zoom: zoom
    });
  };

  var tileoverlay = new TileOverlay(self.pluginMap.id, hashCode, pluginOptions);

  map.overlayMapTypes.push(tileoverlay);

  self.pluginMap.objects[tileoverlayId] = tileoverlay;
  self.pluginMap.objects['tileoverlay_property_' + tileoverlayId] = pluginOptions;

  onSuccess({
    'id': tileoverlayId
  });
};


PluginTileOverlay.prototype.onGetTileUrlFromJS = function(onSuccess, onError, args) {

  var self = this,
    cacheId = args[1],
    tileUrl = args[2];

  var tmp = cacheId.split(/\-/),
    hashCode = tmp[1],
    tileoverlayId = 'tileoverlay_' + hashCode;

  var tileLayer = self.pluginMap.objects[tileoverlayId];

  if (tileLayer.getTileFromCache(cacheId)) {
    tileLayer.getTileFromCache(cacheId).style.backgroundImage = "url('" + tileUrl + "')";
  }
  onSuccess();
};

PluginTileOverlay.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var tileoverlay = self.pluginMap.objects[overlayId];
  if (tileoverlay) {
    google.maps.event.clearInstanceListeners(tileoverlay);
    tileoverlay.setMap(null);
    tileoverlay = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

module.exports = PluginTileOverlay;
