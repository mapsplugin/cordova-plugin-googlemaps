

var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginKmlOverlay(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    enumerable: false,
    writable: false
  });
}

utils.extend(PluginKmlOverlay, BaseClass);

PluginKmlOverlay.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    markerId = 'marker_' + args[2],
    pluginOptions = args[1];

  var markerOpts = {
    'overlayId': markerId,
    'position': pluginOptions.position,
    'map': map,
    'disableAutoPan': pluginOptions.disableAutoPan === true,
    'draggable': pluginOptions.draggable === true
  };
  var iconSize = null;
  if (pluginOptions.animation) {
    markerOpts.animation = google.maps.Animation[pluginOptions.animation.toUpperCase()];
  }
  if (pluginOptions.icon) {
    var icon = pluginOptions.icon;
    markerOpts.icon = {};
    if (typeof pluginOptions.icon === 'string') {
      // Specifies path or url to icon image
      markerOpts.icon = pluginOptions.icon;
    } else if (typeof pluginOptions.icon === 'object') {
      if (Array.isArray(pluginOptions.icon.url)) {
        // Specifies color name or rule
        markerOpts.icon = {
          'path': 'm12 0c-4.4183 2.3685e-15 -8 3.5817-8 8 0 1.421 0.3816 2.75 1.0312 3.906 0.1079 0.192 0.221 0.381 0.3438 0.563l6.625 11.531 6.625-11.531c0.102-0.151 0.19-0.311 0.281-0.469l0.063-0.094c0.649-1.156 1.031-2.485 1.031-3.906 0-4.4183-3.582-8-8-8zm0 4c2.209 0 4 1.7909 4 4 0 2.209-1.791 4-4 4-2.2091 0-4-1.791-4-4 0-2.2091 1.7909-4 4-4z',
          'fillColor': 'rgb(' + pluginOptions.icon.url[0] + ',' + pluginOptions.icon.url[1] + ',' + pluginOptions.icon.url[2] + ')',
          'fillOpacity': pluginOptions.icon.url[3] / 256,
          'scale': 1.3,
          'strokeWeight': 0,
          'anchor': new google.maps.Point(12, 27)
        };
        iconSize = {
          'width': 22,
          'height': 28
        };
      } else {
        markerOpts.icon.url = pluginOptions.icon.url;
        if (pluginOptions.icon.size) {
          markerOpts.icon.scaledSize = new google.maps.Size(icon.size.width, icon.size.height);
          iconSize = icon.size;
        }
      }
    }

    if (icon.anchor) {
      markerOpts.icon.anchor = new google.maps.Point(icon.anchor[0], icon.anchor[1]);
    }
  }
  if (!markerOpts.icon ||
      !markerOpts.icon.url && !markerOpts.icon.path) {
    // default marker
    markerOpts.icon = {
      'path': 'm12 0c-4.4183 2.3685e-15 -8 3.5817-8 8 0 1.421 0.3816 2.75 1.0312 3.906 0.1079 0.192 0.221 0.381 0.3438 0.563l6.625 11.531 6.625-11.531c0.102-0.151 0.19-0.311 0.281-0.469l0.063-0.094c0.649-1.156 1.031-2.485 1.031-3.906 0-4.4183-3.582-8-8-8zm0 4c2.209 0 4 1.7909 4 4 0 2.209-1.791 4-4 4-2.2091 0-4-1.791-4-4 0-2.2091 1.7909-4 4-4z',
      'fillColor': 'rgb(255, 0, 0)',
      'fillOpacity': 1,
      'scale': 1.3,
      'strokeWeight': 0,
      'anchor': new google.maps.Point(12, 27)
    };
    iconSize = {
      'width': 22,
      'height': 28
    };
  }
  var marker = new google.maps.Marker(markerOpts);
  marker.addListener('click', self._onMarkerEvent.bind(self, marker, event.MARKER_CLICK));
  marker.addListener('dragstart', self._onMarkerEvent.bind(self, marker, event.MARKER_DRAG_START));
  marker.addListener('drag', self._onMarkerEvent.bind(self, marker, event.MARKER_DRAG));
  marker.addListener('dragend', self._onMarkerEvent.bind(self, marker, event.MARKER_DRAG_END));

  if (pluginOptions.title || pluginOptions.snippet) {
    var html = [];
    if (pluginOptions.title) {
      html.push(pluginOptions.title);
    }
    if (pluginOptions.snippet) {
      html.push('<small>' + pluginOptions.snippet + '</small>');
    }
    marker.set('content', html.join('<br>'));
    marker.addListener('click', onMarkerClick);
  }

  self.pluginMap.objects[markerId] = marker;
  self.pluginMap.objects['marker_property_' + markerId] = markerOpts;

  if (iconSize) {
    onSuccess({
      'id': markerId,
      'width': iconSize.width,
      'height': iconSize.height
    });
  } else {
    var img = new Image();
    img.onload = function() {
      console.log(markerId, img.width, img.height);
      onSuccess({
        'id': markerId,
        'width': img.width,
        'height': img.height
      });
    };
    img.onerror = function() {
      onSuccess({
        'id': markerId,
        'width': 20,
        'height': 42
      });
    };
    if (typeof markerOpts.icon === "string") {
      img.src = markerOpts.icon;
    } else {
      img.src = markerOpts.icon.url;
    }
  }
};
PluginKmlOverlay.prototype.setTitle = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var title = args[1];
  var marker = self.pluginMap.objects[overlayId];
  marker.set('content', title);
  onSuccess();
};

PluginKmlOverlay.prototype.showInfoWindow = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    onMarkerClick.call(marker);
  }
  onSuccess();
};
PluginKmlOverlay.prototype.setPosition = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.setPosition({'lat': args[1], 'lng': args[2]});
  }
  onSuccess();
};

PluginKmlOverlay.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    google.maps.event.clearInstanceListeners(marker);
    marker.setMap(null);
    marker = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

PluginKmlOverlay.prototype._onMarkerEvent = function(marker, evtName) {
  var self = this,
    mapId = self.pluginMap.id;
console.log(mapId, evtName);
  if (mapId in plugin.google.maps) {
    var latLng = marker.getPosition();
    plugin.google.maps[mapId]({
      'evtName': evtName,
      'callback': '_onMarkerEvent',
      'args': [marker.overlayId, new LatLng(latLng.lat(), latLng.lng())]
    });
  }

};
module.exports = PluginKmlOverlay;

var infoWnd = null;
function onMarkerClick() {
  if (!infoWnd) {
    infoWnd = new google.maps.InfoWindow();
  }
  var marker = this;
  var maxWidth = marker.getMap().getDiv().offsetWidth * 0.7;
  var content = marker.get('content');
  infoWnd.setOptions({
    content: content,
    disableAutoPan: marker.disableAutoPan,
    maxWidth: maxWidth
  });
  infoWnd.open(marker.getMap(), marker);
}
