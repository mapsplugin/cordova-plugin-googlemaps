
var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginMarker(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, 'pluginMap', {
    value: pluginMap,
    writable: false
  });
  Object.defineProperty(self, 'infoWnd', {
    value: null,
    writable: true
  });
}

utils.extend(PluginMarker, BaseClass);

PluginMarker.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    markerId = 'marker_' + args[2],
    pluginOptions = args[1];
  self.__create.call(self, markerId, pluginOptions, function(marker, properties) {
    onSuccess(properties);
  }, onError);
};

/*eslint-disable no-unused-vars*/
PluginMarker.prototype.__create = function(markerId, pluginOptions, onSuccess, onError) {
/*eslint-enable no-unused-vars*/
  var self = this,
    map = self.pluginMap.get('map');
  var markerOpts = {
    'overlayId': markerId,
    'position': pluginOptions.position,
    'disableAutoPan': pluginOptions.disableAutoPan,
    'draggable': pluginOptions.draggable,
    'visible': pluginOptions.visible
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
      markerOpts.icon.url = pluginOptions.icon;
    } else if (typeof pluginOptions.icon === 'object') {
      if (Array.isArray(pluginOptions.icon.url)) {
        // Specifies color name or rule
        markerOpts.icon = {
          'path': 'm12 0c-4.4183 2.3685e-15 -8 3.5817-8 8 0 1.421 0.3816 2.75 1.0312 3.906 0.1079 0.192 0.221 0.381 0.3438 0.563l6.625 11.531 6.625-11.531c0.102-0.151 0.19-0.311 0.281-0.469l0.063-0.094c0.649-1.156 1.031-2.485 1.031-3.906 0-4.4183-3.582-8-8-8zm0 4c2.209 0 4 1.7909 4 4 0 2.209-1.791 4-4 4-2.2091 0-4-1.791-4-4 0-2.2091 1.7909-4 4-4z',
          'fillColor': 'rgb(' + pluginOptions.icon.url[0] + ',' + pluginOptions.icon.url[1] + ',' + pluginOptions.icon.url[2] + ')',
          'fillOpacity': pluginOptions.icon.url[3] / 255,
          'scale': 1.3,
          'strokeWeight': 1,
          'strokeColor': 'rgb(255, 255, 255)',
          'strokeOpacity': 0.65,
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
      if ('anchor' in icon && Array.isArray(icon.anchor)) {
        markerOpts.icon.anchor = new google.maps.Point(icon.anchor[0], icon.anchor[1]);
      }
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
      'strokeWeight': 1,
      'strokeColor': 'rgb(255, 255, 255)',
      'strokeOpacity': 0.65,
      'anchor': new google.maps.Point(12, 27)
    };
    iconSize = {
      'width': 22,
      'height': 28
    };
  }

  if ('zIndex' in pluginOptions) {
    markerOpts.zIndex = pluginOptions.zIndex;
  }
  markerOpts.map = map;
  var marker = new google.maps.Marker(markerOpts);
  marker.addListener('click', self.onMarkerClickEvent.bind(self, event.MARKER_CLICK, marker), {passive: true});
  marker.addListener('dragstart', self.onMarkerEvent.bind(self, event.MARKER_DRAG_START, marker));
  marker.addListener('drag', self.onMarkerEvent.bind(self, event.MARKER_DRAG, marker));
  marker.addListener('dragend', self.onMarkerEvent.bind(self, event.MARKER_DRAG_END, marker));

  if (pluginOptions.title) {
    marker.set('title', pluginOptions.title);
  }
  if (pluginOptions.snippet) {
    marker.set('snippet', pluginOptions.snippet);
  }


  self.pluginMap.objects[markerId] = marker;
  self.pluginMap.objects['marker_property_' + markerId] = markerOpts;

  if (iconSize) {
    onSuccess(marker, {
      '__pgmId': markerId,
      'width': iconSize.width,
      'height': iconSize.height
    });
  } else {
    var markerIcon = marker.getIcon();
    if (markerIcon && markerIcon.size) {
      onSuccess({
        '__pgmId': markerId,
        'width': markerIcon.size.width,
        'height': markerIcon.size.height
      });
    } else {
      var img = new Image();
      img.onload = function() {
        onSuccess(marker, {
          '__pgmId': markerId,
          'width': img.width,
          'height': img.height
        });
      };
      img.onerror = function(error) {
        console.warn(error.getMessage());
        onSuccess(marker, {
          '__pgmId': markerId,
          'width': 20,
          'height': 42
        });
      };
      if (typeof markerOpts.icon === 'string') {
        img.src = markerOpts.icon;
      } else {
        img.src = markerOpts.icon.url;
      }
    }
  }

  setTimeout(function() {
    marker.setAnimation(null);
  }, 500);
};
PluginMarker.prototype._removeMarker = function(marker) {
  marker.setMap(null);
  marker = undefined;
};

PluginMarker.prototype.setDisableAutoPan = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var disableAutoPan = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.set('disableAutoPan', disableAutoPan);
  }
  onSuccess();
};
PluginMarker.prototype.setFlat = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var flat = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.set('flat', flat);
  }
  onSuccess();
};
PluginMarker.prototype.setVisible = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var visible = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.setVisible(visible);
  }
  onSuccess();
};
PluginMarker.prototype.setAnimation = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var animation = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.setAnimation(google.maps.Animation[animation]);
    setTimeout(function() {
      marker.setAnimation(null);
    }, 500);
  }
  onSuccess();
};
PluginMarker.prototype.setRotation = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var rotation = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    var icon = marker.getIcon();
    if (icon && icon.path) {
      icon.rotation = rotation;
      marker.setIcon(icon);
    }
  }
  onSuccess();
};
PluginMarker.prototype.setDraggable = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.setDraggable(args[1]);
  }
  onSuccess();
};
PluginMarker.prototype.setInfoWindowAnchor = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    var icon = marker.getIcon();
    var anchorX = args[1];
    anchorX = anchorX - icon.size.width / 2;
    var anchorY = args[2];
    anchorY = anchorY - icon.size.height / 2;
    marker.setOptions({
      'anchorPoint': new google.maps.Point(anchorX, anchorY)
    });
    if (self.infoWnd) {
      self._showInfoWindow.call(self, marker);
    }
  }
  onSuccess();
};
PluginMarker.prototype.setTitle = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var title = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.set('title', title);
  }
  onSuccess();
};

PluginMarker.prototype.setSnippet = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var snippet = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.set('snippet', snippet);
  }
  onSuccess();
};

PluginMarker.prototype.setOpacity = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var opacity = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.setOpacity(opacity);
  }
  onSuccess();
};

PluginMarker.prototype.setIconAnchor = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var anchorX = args[1],
    anchorY = args[2];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    var icon = marker.getIcon();
    if (typeof icon === 'string') {
      icon = {
        'url': icon
      };
    }
    icon.anchor = new google.maps.Point(anchorX, anchorY);
    marker.setIcon(icon);
  }
  onSuccess();
};
PluginMarker.prototype.setZIndex = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var zIndex = args[1];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.setZIndex(zIndex);
  }
  onSuccess();
};
PluginMarker.prototype.showInfoWindow = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    if (self.pluginMap.activeMarker && self.pluginMap.activeMarker !== marker) {
      self.onMarkerClickEvent(event.INFO_CLICK, self.pluginMap.activeMarker);
    }
    self.pluginMap.activeMarker = marker;
    self._showInfoWindow.call(self, marker);
  }
  onSuccess();
};
PluginMarker.prototype.hideInfoWindow = function(onSuccess) {
  var self = this;
  if (self.infoWnd) {
    google.maps.event.trigger(self.infoWnd, 'closeclick');
    self.infoWnd.close();
    self.infoWnd = null;
  }
  onSuccess();
};
PluginMarker.prototype.setIcon = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];

  self.setIcon_(marker, args[1])
    .then(onSuccess)
    .catch(onError);
};
PluginMarker.prototype.setIcon_ = function(marker, iconOpts) {
  return new Promise(function(resolve) {
    if (marker) {
      if (Array.isArray(iconOpts)) {
        // Specifies color name or rule
        iconOpts = {
          'path': 'm12 0c-4.4183 2.3685e-15 -8 3.5817-8 8 0 1.421 0.3816 2.75 1.0312 3.906 0.1079 0.192 0.221 0.381 0.3438 0.563l6.625 11.531 6.625-11.531c0.102-0.151 0.19-0.311 0.281-0.469l0.063-0.094c0.649-1.156 1.031-2.485 1.031-3.906 0-4.4183-3.582-8-8-8zm0 4c2.209 0 4 1.7909 4 4 0 2.209-1.791 4-4 4-2.2091 0-4-1.791-4-4 0-2.2091 1.7909-4 4-4z',
          'fillColor': 'rgb(' + iconOpts[0] + ',' + iconOpts[1] + ',' + iconOpts[2] + ')',
          'fillOpacity': iconOpts[3] / 256,
          'scale': 1.3,
          'strokeWeight': 0,
          'anchor': new google.maps.Point(12, 27)
        };
      } else if (typeof iconOpts === 'object') {

        if (typeof iconOpts.size === 'object') {
          iconOpts.size = new google.maps.Size(iconOpts.size.width, iconOpts.size.height, 'px', 'px');
          iconOpts.scaledSize = iconOpts.size;
        }
        if (Array.isArray(iconOpts.anchor)) {
          iconOpts.anchor = new google.maps.Point(iconOpts.anchor[0], iconOpts.anchor[1]);
        }
      }

      marker.setIcon(iconOpts);
    }
    resolve();
  });
};
PluginMarker.prototype.setPosition = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    marker.setPosition({'lat': args[1], 'lng': args[2]});
  }
  onSuccess();
};

PluginMarker.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var marker = self.pluginMap.objects[overlayId];
  if (marker) {
    google.maps.event.clearInstanceListeners(marker);
    marker.setMap(null);
    marker = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
    delete self.pluginMap.objects['marker_property_' + overlayId];
  }
  onSuccess();
};

PluginMarker.prototype.onMarkerEvent = function(evtName, marker) {
  var self = this,
    mapId = self.pluginMap.__pgmId;

  if (mapId in plugin.google.maps) {
    var latLng = marker.getPosition();
    plugin.google.maps[mapId]({
      'evtName': evtName,
      'callback': '_onMarkerEvent',
      'args': [marker.overlayId, new LatLng(latLng.lat(), latLng.lng())]
    });
  }

};

PluginMarker.prototype._showInfoWindow = function(marker) {
  var self = this;
  if (!self.infoWnd) {
    self.infoWnd = new google.maps.InfoWindow({
      'pixelOffset': new google.maps.Size(0, 0)
    });
  }
  var container = document.createElement('div');
  if (self.pluginMap.activeMarker && self.pluginMap.activeMarker !== marker) {
    self.onMarkerClickEvent(event.INFO_CLICK, self.pluginMap.activeMarker);
  }
  self.pluginMap.activeMarker = marker;
  self.pluginMap._syncInfoWndPosition.call(self);
  var maxWidth = marker.getMap().getDiv().offsetWidth * 0.7;
  var html = [];
  if (marker.get('title')) {
    html.push(marker.get('title'));
  }
  if (marker.get('snippet')) {
    html.push('<small>' + marker.get('snippet') + '</small>');
  }
  if (html.length > 0) {
    container.innerHTML = html.join('<br>');
    google.maps.event.addListenerOnce(self.infoWnd, 'domready', function() {
      self.onMarkerClickEvent(event.INFO_OPEN, marker);

      if (container.parentNode) {
        google.maps.event.addDomListener(container.parentNode.parentNode.parentNode, 'click', function() {
          self.onMarkerClickEvent(event.INFO_CLICK, marker);
        }, true);
      }

    });
    self.infoWnd.setOptions({
      content: container,
      disableAutoPan: marker.disableAutoPan,
      maxWidth: maxWidth
    });
    google.maps.event.addListener(self.infoWnd, 'closeclick', function() {
      google.maps.event.clearInstanceListeners(self.infoWnd);
      self.onMarkerClickEvent(event.INFO_CLOSE, marker);
    });
    self.infoWnd.open(marker.getMap(), marker);
  }
};

PluginMarker.prototype.onMarkerClickEvent = function(evtName, marker) {
  var self = this;

  var overlayId = marker.get('overlayId');

  if (self.pluginMap.activeMarker && self.pluginMap.activeMarker !== marker) {
    self.onMarkerEvent(event.INFO_CLOSE, self.pluginMap.activeMarker);
  }
  self.pluginMap.activeMarker = marker;
  if (marker.get('disableAutoPan') === false) {
    self.pluginMap.get('map').panTo(marker.getPosition());
  }
  if (overlayId.indexOf('markercluster_') > -1) {
    self.onClusterEvent(evtName, marker);
  } else {
    self.onMarkerEvent(evtName, marker);
  }

};

module.exports = PluginMarker;
