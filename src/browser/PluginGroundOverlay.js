


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

  var groundoverlayOpts = {
    'overlayId': groundoverlayId,
    'map': map
  };

  var bounds = new google.maps.LatLngBounds();
  pluginOptions.bounds.forEach(function(latLng) {
    bounds.extend(latLng);
  });

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
  console.log(groundoverlay);

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
      'args': [groundoverlay.overlayId, mouseEvt.latLng]
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
  var img = new Image();
  img.id="hogehoge";
  img.src = url;
  img.style.position = "absolute";
  self.set("img", img);
  google.maps.event.addDomListener(img, 'click', function(evt) {
    console.log(evt);
    if (self.get('clickable')) {
      var projection = self.getProjection();
      var latLng = projection.fromContainerPixelToLatLng(evt.clientX, evt.clientY);
      google.maps.event.trigger('click', {
        latLng: latLng
      });
    }
  });

  self.set("url", url);
  self.addListener("url_changed", function(prevUrl, newUrl) {
    image.src = newUrl;
  });

  self.set("sw", bounds.getSouthWest());
  self.set("ne", bounds.getNorthEast());
  self.addListener("sw_changed", self.draw);
  self.addListener("ne_changed", self.draw);

  self.addListener('opacity_changed', function() {
    img.style.opacity = self.get('opacity');
  });
  self.addListener('zIndex_changed', function() {
    img.style.zIndex = self.get('zIndex');
  });


  for (var key in options) {
    self.set(key, options[key]);
  }
}

CustomGroundOverlay.prototype.setOpacity = function(opacity) {
  var self = this;
  self.get('img').style.opacity = opacity;
};

CustomGroundOverlay.prototype.setBounds = function(bounds) {
  var self = this;
  self.set("sw", bounds.getSouthWest());
  self.set("ne", bounds.getNorthEast());
};

CustomGroundOverlay.prototype.getBounds = function() {
  var self = this;
  return new google.maps.LatLngBounds(
    self.get("sw"), self.get("ne")
  );
};
CustomGroundOverlay.prototype.draw = function() {
  var self = this,
    projection = self.getProjection();

  // Calculate positions
  var nePx = projection.fromLatLngToDivPixel(self.get("ne")),
    swPx = projection.fromLatLngToDivPixel(self.get("sw"));

  var img = self.get("img");
  img.style.left = swPx.x + "px";
  img.style.top = nePx.x + "px";
  img.style.width = (nePx.x - swPx.x) + "px";
  img.style.height = (swPx.y - nePx.y) + "px";
  console.log(img.style.left, img.style.top, img.style.width, img.style.height);
};

CustomGroundOverlay.prototype.onAdd = function() {
  var self = this;
  self.getPanes().overlayMouseTarget.appendChild(self.get("img"));
};

CustomGroundOverlay.prototype.onRemove = function() {
  var self = this;
  self.getPanes().mapPane.removeChild(self.get("img"));
  google.maps.event.clearInstanceListeners(self.get('img'));
};
