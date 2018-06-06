

var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginPolyline(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    enumerable: false,
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
    'position': pluginOptions.position,
    'map': map,
    'disableAutoPan': pluginOptions.disableAutoPan === true,
    'draggable': pluginOptions.draggable === true
  };
  var iconSize = null;
  if (pluginOptions.animation) {
    polylineOpts.animation = google.maps.Animation[pluginOptions.animation.toUpperCase()];
  }
  if (pluginOptions.icon) {
    var icon = pluginOptions.icon;
    polylineOpts.icon = {};
    if (typeof pluginOptions.icon === 'string') {
      // Specifies path or url to icon image
      polylineOpts.icon = pluginOptions.icon;
    } else if (typeof pluginOptions.icon === 'object') {
      if (Array.isArray(pluginOptions.icon.url)) {
        // Specifies color name or rule
        polylineOpts.icon = {
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
        polylineOpts.icon.url = pluginOptions.icon.url;
        if (pluginOptions.icon.size) {
          polylineOpts.icon.scaledSize = new google.maps.Size(icon.size.width, icon.size.height);
          iconSize = icon.size;
        }
      }
    }

    if (icon.anchor) {
      polylineOpts.icon.anchor = new google.maps.Point(icon.anchor[0], icon.anchor[1]);
    }
  }
  if (!polylineOpts.icon ||
      !polylineOpts.icon.url && !polylineOpts.icon.path) {
    // default polyline
    polylineOpts.icon = {
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
  var polyline = new google.maps.polyline(polylineOpts);
  polyline.addListener('click', self._onpolylineEvent.bind(self, polyline, event.polyline_CLICK));
  polyline.addListener('dragstart', self._onpolylineEvent.bind(self, polyline, event.polyline_DRAG_START));
  polyline.addListener('drag', self._onpolylineEvent.bind(self, polyline, event.polyline_DRAG));
  polyline.addListener('dragend', self._onpolylineEvent.bind(self, polyline, event.polyline_DRAG_END));

  if (pluginOptions.title || pluginOptions.snippet) {
    var html = [];
    if (pluginOptions.title) {
      html.push(pluginOptions.title);
    }
    if (pluginOptions.snippet) {
      html.push('<small>' + pluginOptions.snippet + '</small>');
    }
    polyline.set('content', html.join('<br>'));
    polyline.addListener('click', onpolylineClick);
  }

  self.pluginMap.objects[polylineId] = polyline;
  self.pluginMap.objects['polyline_property_' + polylineId] = polylineOpts;

  if (iconSize) {
    onSuccess({
      'id': polylineId,
      'width': iconSize.width,
      'height': iconSize.height
    });
  } else {
    var img = new Image();
    img.onload = function() {
      console.log(polylineId, img.width, img.height);
      onSuccess({
        'id': polylineId,
        'width': img.width,
        'height': img.height
      });
    };
    img.onerror = function() {
      onSuccess({
        'id': polylineId,
        'width': 20,
        'height': 42
      });
    };
    if (typeof polylineOpts.icon === "string") {
      img.src = polylineOpts.icon;
    } else {
      img.src = polylineOpts.icon.url;
    }
  }
};
PluginPolyline.prototype.setTitle = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var title = args[1];
  var polyline = self.pluginMap.objects[overlayId];
  polyline.set('content', title);
  onSuccess();
};

PluginPolyline.prototype.showInfoWindow = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polyline = self.pluginMap.objects[overlayId];
  if (polyline) {
    onpolylineClick.call(polyline);
  }
  onSuccess();
};
PluginPolyline.prototype.setPosition = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polyline = self.pluginMap.objects[overlayId];
  if (polyline) {
    polyline.setPosition({'lat': args[1], 'lng': args[2]});
  }
  onSuccess();
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

PluginPolyline.prototype._onpolylineEvent = function(polyline, evtName) {
  var self = this,
    mapId = self.pluginMap.id;
console.log(mapId, evtName);
  if (mapId in plugin.google.maps) {
    var latLng = polyline.getPosition();
    plugin.google.maps[mapId]({
      'evtName': evtName,
      'callback': '_onpolylineEvent',
      'args': [polyline.overlayId, new LatLng(latLng.lat(), latLng.lng())]
    });
  }

};
module.exports = PluginPolyline;

var infoWnd = null;
function onpolylineClick() {
  if (!infoWnd) {
    infoWnd = new google.maps.InfoWindow();
  }
  var polyline = this;
  var maxWidth = polyline.getMap().getDiv().offsetWidth * 0.7;
  var content = polyline.get('content');
  infoWnd.setOptions({
    content: content,
    disableAutoPan: polyline.disableAutoPan,
    maxWidth: maxWidth
  });
  infoWnd.open(polyline.getMap(), polyline);
}
