


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginPolygon(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    writable: false
  });
}

utils.extend(PluginPolygon, BaseClass);

PluginPolygon.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    polygonId = 'polygon_' + args[2],
    pluginOptions = args[1];

  var polygonOpts = {
    'overlayId': polygonId,
    'map': map,
    'paths': new google.maps.MVCArray()
  };

  if (pluginOptions.points) {
    var strokePath = new google.maps.MVCArray();
    pluginOptions.points.forEach(function(point) {
      strokePath.push(new google.maps.LatLng(point.lat, point.lng));
    });
    polygonOpts.paths.push(strokePath);


    if (Array.isArray(pluginOptions.holes)) {
      pluginOptions.holes.forEach(function(hole) {
        var holeMvc = new google.maps.MVCArray();
        hole.forEach(function(vertix) {
          holeMvc.push(new google.maps.LatLng(vertix.lat, vertix.lng));
        });
        polygonOpts.paths.push(holeMvc);
      });
    }
  }
  if (Array.isArray(pluginOptions.strokeColor)) {
    polygonOpts.strokeColor = 'rgb(' + pluginOptions.strokeColor[0] + ',' + pluginOptions.strokeColor[1] + ',' + pluginOptions.strokeColor[2] + ')';
    polygonOpts.strokeOpacity = pluginOptions.strokeColor[3] / 256;
  }
  if (Array.isArray(pluginOptions.fillColor)) {
    polygonOpts.fillColor = 'rgb(' + pluginOptions.fillColor[0] + ',' + pluginOptions.fillColor[1] + ',' + pluginOptions.fillColor[2] + ')';
    polygonOpts.fillOpacity = pluginOptions.fillColor[3] / 256;
  }
  if ('width' in pluginOptions) {
    polygonOpts.strokeWeight = pluginOptions.width;
  }
  if ('zIndex' in pluginOptions) {
    polygonOpts.zIndex = pluginOptions.zIndex;
  }
  if ('visible' in pluginOptions) {
    polygonOpts.visible = pluginOptions.visible;
  }
  if ('geodesic' in pluginOptions) {
    polygonOpts.geodesic = pluginOptions.geodesic;
  }
  if ('clickable' in pluginOptions) {
    polygonOpts.clickable = pluginOptions.clickable;
  }

  var polygon = new google.maps.Polygon(polygonOpts);
  polygon.addListener('click', function(polyMouseEvt) {
    self._onPolygonEvent.call(self, polygon, polyMouseEvt);
  });

  self.pluginMap.objects[polygonId] = polygon;

  onSuccess({
    'id': polygonId
  });
};

PluginPolygon.prototype.setFillColor = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var fillColor = args[1];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {

    if (Array.isArray(fillColor)) {
      polygon.setOptions({
        'fillColor': 'rgb(' + fillColor[0] + ',' + fillColor[1] + ',' + fillColor[2] + ')',
        'fillOpacity': fillColor[3] / 256
      });
    }
  }
  onSuccess();
};

PluginPolygon.prototype.setStrokeColor = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  var strokeColor = args[1];
  if (polygon) {
    if (Array.isArray(strokeColor)) {
      polygon.setOptions({
        'strokeColor': 'rgb(' + strokeColor[0] + ',' + strokeColor[1] + ',' + strokeColor[2] + ')',
        'strokeOpacity': strokeColor[3] / 256
      });
    }
  }
  onSuccess();
};

PluginPolygon.prototype.setStrokeWidth = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var width = args[1];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    polygon.setOptions({
      'strokeWeight': width
    });
  }
  onSuccess();
};


PluginPolygon.prototype.setZIndex = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var zIndex = args[1];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    polygon.setOptions({
      'zIndex': zIndex
    });
  }
  onSuccess();
};

PluginPolygon.prototype.setPoints = function(onSuccess, onError, args) {
  var self = this,
    polygonId = args[0],
    positionList = args[1],
    polygon = self.pluginMap.objects[polygonId];
  if (polygon) {
    //------------------------
    // Update the points list
    //------------------------
    polygon.setPath(positionList);
  }
  onSuccess();
};

PluginPolygon.prototype.setClickable = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var clickable = args[1];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    polygon.setOptions({
      'clickable': clickable === true
    });
  }
  onSuccess();
};

PluginPolygon.prototype.setVisible = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    polygon.setVisible(args[1]);
  }
  onSuccess();
};

PluginPolygon.prototype.setGeodesic = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    polygon.setOptions({
      'geodesic': args[1] === true
    });
  }
  onSuccess();
};
PluginPolygon.prototype.insertPointAt = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    var index = args[1];
    var latLng = new google.maps.LatLng(args[2].lat, args[2].lng);
    polygon.getPath().insertAt(index, latLng);
  }
  onSuccess();
};
PluginPolygon.prototype.setPointAt = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    var index = args[1];
    var latLng = new google.maps.LatLng(args[2].lat, args[2].lng);
    polygon.getPath().setAt(index, latLng);
  }
  onSuccess();
};
PluginPolygon.prototype.removePointAt = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    var index = args[1];
    polygon.getPath().removeAt(index, latLng);
  }
  onSuccess();
};

PluginPolygon.prototype.setHoles = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    var paths = polygon.getPaths(),
      holeList = args[1],
      newPaths = new google.maps.MVCArray([paths.getAt(0)]);

    holeList.forEach(function(holeVertixes) {
      var hole = new google.maps.MVCArray();
      holeVertixes.forEach(function(vertix) {
        hole.push(new google.maps.LatLng(vertix.lat, vertix.lng));
      });
      newPaths.push(hole);
    });
    polygon.setPaths(newPaths);
  }
  onSuccess();
};

PluginPolygon.prototype.insertPointOfHoleAt = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    holeIndex = args[1] + 1,  // idx=0 is for outter vertixes
    pointIndex = args[2],
    position = args[3],
    polygon = self.pluginMap.objects[overlayId];

  if (polygon) {
    var index = args[1];
    var latLng = new google.maps.LatLng(position.lat, position.lng);
    var paths = polygon.getPaths();
    var hole = null;
    if (holeIndex < paths.getLength()) {
      hole = paths.getAt(holeIndex);
    }
    if (!hole) {
      hole = new google.maps.MVCArray();
      paths.push(hole);
    }
    hole.insertAt(pointIndex, latLng);
  }
  onSuccess();
};

PluginPolygon.prototype.setPointOfHoleAt = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    holeIndex = args[1] + 1,
    pointIndex = args[2],
    position = args[3],
    polygon = self.pluginMap.objects[overlayId];

  if (polygon) {
    var index = args[1];
    var latLng = new google.maps.LatLng(position.lat, position.lng);
    polygon.getPaths().getAt(holeIndex).setAt(pointIndex, latLng);
  }
  onSuccess();
};

PluginPolygon.prototype.removePointOfHoleAt = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    holeIndex = args[1] + 1,
    pointIndex = args[2],
    polygon = self.pluginMap.objects[overlayId];

  if (polygon) {
    polygon.getPaths().getAt(holeIndex).removeAt(pointIndex);
  }
  onSuccess();
};


PluginPolygon.prototype.insertHoleAt = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    holeIndex = args[1] + 1,
    vertixes = args[2],
    polygon = self.pluginMap.objects[overlayId];

  if (polygon) {
    var hole = new google.maps.MVCArray();
    vertixes.forEach(function(vertix) {
      hole.push(new google.maps.LatLng(vertix.lat, vertix.lng));
    });
    polygon.getPaths().insertAt(holeIndex, hole);
  }
  onSuccess();
};



PluginPolygon.prototype.setHoleAt = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    holeIndex = args[1] + 1,
    vertixes = args[2],
    polygon = self.pluginMap.objects[overlayId];

  if (polygon) {
    var hole = new google.maps.MVCArray();
    vertixes.forEach(function(vertix) {
      hole.push(new google.maps.LatLng(vertix.lat, vertix.lng));
    });
    polygon.getPaths().setAt(holeIndex, hole);
  }
  onSuccess();
};


PluginPolygon.prototype.removeHoleAt = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    holeIndex = args[1] + 1,
    polygon = self.pluginMap.objects[overlayId];

  if (polygon) {
    polygon.getPaths().removeAt(holeIndex);
  }
  onSuccess();
};
PluginPolygon.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var polygon = self.pluginMap.objects[overlayId];
  if (polygon) {
    google.maps.event.clearInstanceListeners(polygon);
    polygon.setMap(null);
    polygon = undefined;
    self.pluginMap.objects[overlayId] = undefined;
    delete self.pluginMap.objects[overlayId];
  }
  onSuccess();
};

PluginPolygon.prototype._onPolygonEvent = function(polygon, polyMouseEvt) {
  var self = this,
    mapId = self.pluginMap.id;
  if (mapId in plugin.google.maps) {
    plugin.google.maps[mapId]({
      'evtName': event.POLYGON_CLICK,
      'callback': '_onOverlayEvent',
      'args': [polygon.overlayId, new plugin.google.maps.LatLng(polyMouseEvt.latLng.lat(), polyMouseEvt.latLng.lng())]
    });
  }

};
module.exports = PluginPolygon;
