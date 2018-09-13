


var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  Spherical = require('cordova-plugin-googlemaps.spherical'),
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

  var groundoverlayOpts = {
    'overlayId': groundoverlayId,
    'map': map
  };

  var bounds = new google.maps.LatLngBounds();
  pluginOptions.bounds.forEach(function(latLng) {
    bounds.extend(latLng);
  });

  if ('bearing' in pluginOptions) {
    groundoverlayOpts.bearing = pluginOptions.bearing;
  }
  if ('zIndex' in pluginOptions) {
    groundoverlayOpts.zIndex = pluginOptions.zIndex;
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

  var groundoverlay = newGroundOverlay(pluginOptions.url, bounds, groundoverlayOpts);
  groundoverlay.addListener('click', function(polyMouseEvt) {
    self._onGroundOverlayEvent.call(self, groundoverlay, polyMouseEvt);
  });

  self.pluginMap.objects[groundoverlayId] = groundoverlay;
  self.pluginMap.objects['groundoverlay_property_' + groundoverlayId] = pluginOptions;


  onSuccess({
    'id': groundoverlayId
  });
};
PluginGroundOverlay.prototype.setImage = function(onSuccess, onError, args) {
  var self = this,
    overlayId = args[0],
    url = args[1],
    groundoverlay = self.pluginMap.objects[overlayId];

  if (groundoverlay) {
    groundoverlay.setImage(url);
  }

  onSuccess();
};

PluginGroundOverlay.prototype.setBearing = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {
    groundOverlay.setBearing(args[1]);
  }
  onSuccess();
};

PluginGroundOverlay.prototype.setOpacity = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {
    groundOverlay.setOpacity(args[1]);
  }
  onSuccess();
};

PluginGroundOverlay.prototype.setBounds = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {

    var points = args[1];
    var bounds = new google.maps.LatLngBounds();
    points.forEach(function(latLng) {
      bounds.extend(latLng);
    });
    groundOverlay.setBounds(bounds);
  }
  onSuccess();
};

PluginGroundOverlay.prototype.setZIndex = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var zIndex = args[1];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {
    groundOverlay.setZIndex(zIndex);
  }
  onSuccess();
};

PluginGroundOverlay.prototype.setClickable = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {
    groundOverlay.setClickable(args[1]);
  }
  onSuccess();
};
PluginGroundOverlay.prototype.setVisible = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {
    groundOverlay.setVisible(args[1]);
  }
  onSuccess();
};

PluginGroundOverlay.prototype.remove = function(onSuccess, onError, args) {
  var self = this;
  var overlayId = args[0];
  var groundOverlay = self.pluginMap.objects[overlayId];
  if (groundOverlay) {
    google.maps.event.clearInstanceListeners(groundOverlay);
    groundOverlay.setMap(null);
    groundOverlay.remove();
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
      'args': [groundoverlay.overlayId, new LatLng(mouseEvt.latLng.lat(), mouseEvt.latLng.lng())]
    });
  }

};
module.exports = PluginGroundOverlay;

function newGroundOverlay(url, bounds, options) {

  // https://github.com/apache/cordova-js/blob/c75e8059114255d1dbae1ede398e6626708ee9f3/src/common/utils.js#L167
  if (CustomGroundOverlay.__super__ !== google.maps.OverlayView.prototype) {
    var key, defined = {};
    for (key in CustomGroundOverlay.prototype) {
      defined[key] = CustomGroundOverlay.prototype[key];
    }
    utils.extend(CustomGroundOverlay, google.maps.OverlayView);
    for (key in defined) {
      CustomGroundOverlay.prototype[key] = defined[key];
    }
  }

  return new CustomGroundOverlay(url, bounds, options);
}

function CustomGroundOverlay(url, bounds, options) {
  google.maps.OverlayView.apply(this);
  var self = this;

  //------------------------------------------------------------
  // Create an img element, then put it on map as GroundOverlay
  //------------------------------------------------------------
  var img = new Image();
  img.src = url;
  img.style.position = "absolute";
  img.style.WebkitTransformOrigin = "50% 50%";
  img.style.MozTransformOrigin = "50% 50%";
  img.style.transformOrigin = "50% 50%";
  self.set("img", img);

  self.set("url", url);
  self.addListener("url_changed", function() {
    img.src = self.get('url');
  });

  var path = new google.maps.MVCArray([
    new google.maps.LatLng(0, 0),
    new google.maps.LatLng(0, 0),
    new google.maps.LatLng(0, 0),
    new google.maps.LatLng(0, 0)
  ]);
  self.set('path', path);

  self.addListener('bearing_changed', self.updateVertixes);
  self.addListener('bounds_changed', self.updateVertixes);
  self.addListener('opacity_changed', function() {
    img.style.opacity = self.get('opacity');
  });

  //---------------------------------------------------------
  // Create a transparent rectangle above the groundOverlay,
  // then catch click event
  //---------------------------------------------------------
  var touchPolygon = new google.maps.Polygon({
    'map': options.map,
    'paths': path,
    'clickable': true,
    'fillColor': 'red',
    'fillOpacity': 0,
    'strokeOpacity': 0,
    'zIndex': options.zIndex || 0
  });
  self.addListener('visible_changed', function() {
    img.style.visibility = self.get('visible') ? 'visible' : 'hidden';
    touchPolygon.setVisible(self.get('visible'));
  });

  self.addListener('zIndex_changed', function() {
    touchPolygon.setOptions({
      'zIndex': self.get('zIndex')
    });
    img.style.zIndex = self.get('zIndex');
  });

  touchPolygon.addListener('click', function(evt) {
    if (self.get('clickable')) {
      google.maps.event.trigger(self, 'click', {
        'latLng': evt. latLng
      });
    }
  });
  self.set('touchPolygon', touchPolygon);

  var markers = [];
  for (var i = 0; i < 4; i++) {
    markers.push(new google.maps.Marker({
      'position': {'lat': 0, 'lng': 0},
      'map': options.map
    }));
  }
  self.set('markers', markers);


  //---------------------------------------------------------
  // Apply option values
  //---------------------------------------------------------
  for (var key in options) {
    self.set(key, options[key]);
  }
  self.set('bounds', bounds);
}
CustomGroundOverlay.prototype.updateVertixes = function() {
  var self = this;
  var degree = self.get('bearing') || 0;

  // Get center
  var bounds = self.get('bounds');
  if (!bounds) {
    return;
  }
  var center = bounds.getCenter(),
    centerPos = {
      'lat': center.lat(),
      'lng': center.lng()
    };

  // original NorthEast
  var orgNE = bounds.getNorthEast(),
    orgSW = bounds.getSouthWest(),
    orgNEpos = {
      'lat': orgNE.lat(),
      'lng': orgNE.lng()
    };
  var orgDrgreeNE = Spherical.computeHeading(centerPos, orgNEpos);
  var orgDrgreeSE = Spherical.computeHeading(centerPos, {
    'lat': orgSW.lat(),
    'lng': orgNE.lng()
  });

  var distanceCenter2NE = Spherical.computeDistanceBetween(orgNEpos, centerPos);
  // new NorthEast
  var newNE = Spherical.computeOffsetOrigin(centerPos, distanceCenter2NE, degree + orgDrgreeNE);

  // new SourthEast
  var newSE = Spherical.computeOffsetOrigin(centerPos, distanceCenter2NE, degree + orgDrgreeSE);
  // new SourthWest
  var newSW = Spherical.computeOffsetOrigin(centerPos, distanceCenter2NE, degree + orgDrgreeNE + 180);
  // new SouthEast
  var newNW = Spherical.computeOffsetOrigin(centerPos, distanceCenter2NE, degree + orgDrgreeSE + 180);

  // Caclulate bounds
  var path = self.get('path');
  path.setAt(0, new google.maps.LatLng(newNE.lat, newNE.lng));
  path.setAt(1, new google.maps.LatLng(newSE.lat, newSE.lng));
  path.setAt(2, new google.maps.LatLng(newSW.lat, newSW.lng));
  path.setAt(3, new google.maps.LatLng(newNW.lat, newNW.lng));

  self.draw();
};

CustomGroundOverlay.prototype.remove = function() {
  var self = this;
  self.set('img', undefined);
  google.maps.event.clearInstanceListeners(self);
  google.maps.event.clearInstanceListeners(self.get('touchPolygon'));
};

CustomGroundOverlay.prototype.setClickable = function(clickable) {
  var self = this;
  self.set('clickable', clickable);
};

CustomGroundOverlay.prototype.setOpacity = function(opacity) {
  var self = this;
  self.get('img').style.opacity = opacity;
};

CustomGroundOverlay.prototype.setBearing = function(bearing) {
  var self = this;
  self.set("bearing", bearing);
};

CustomGroundOverlay.prototype.setBounds = function(bounds) {
  var self = this;
  self.set("bounds", bounds);
};

CustomGroundOverlay.prototype.setOpacity = function(opacity) {
  var self = this;
  self.set("opacity", opacity);
};

CustomGroundOverlay.prototype.setZIndex = function(zIndex) {
  var self = this;
  self.set("zIndex", zIndex);
};

CustomGroundOverlay.prototype.setVisible = function(visible) {
  var self = this;
  self.set("visible", visible);
};

CustomGroundOverlay.prototype.setImage = function(url) {
  var self = this;
  self.set("url", url);
};

CustomGroundOverlay.prototype.getBounds = function() {
  var self = this;
  return new google.maps.LatLngBounds(
    self.get("sw"), self.get("ne")
  );
};
CustomGroundOverlay.prototype.draw = function() {
  var self = this;
    projection = self.getProjection();
  if (!projection) {
    return;
  }
  var bounds = self.get('bounds'),
    center = bounds.getCenter(),
    img = self.get("img");

  // Calculate positions
  var swPx = projection.fromLatLngToDivPixel(bounds.getSouthWest()),
    nePx = projection.fromLatLngToDivPixel(bounds.getNorthEast());

  img.style.left = swPx.x + "px";
  img.style.top = nePx.y + "px";
  img.style.width = (nePx.x - swPx.x) + "px";
  img.style.height = (swPx.y - nePx.y) + "px";
  img.style.transform = 'rotate(' + self.get('bearing') + 'deg)';
  img.style.WebkitTransform = 'rotate(' + self.get('bearing') + 'deg)';
  img.style.MozTransform = 'rotate(' + self.get('bearing') + 'deg)';
};

CustomGroundOverlay.prototype.onAdd = function() {
  var self = this;
  self.set('mapPane', self.getPanes().mapPane);
  self.getPanes().mapPane.appendChild(self.get("img"));
};

CustomGroundOverlay.prototype.onRemove = function() {
  var self = this;
  self.get('mapPane').removeChild(self.get("img"));
  google.maps.event.clearInstanceListeners(self.get('img'));
};
