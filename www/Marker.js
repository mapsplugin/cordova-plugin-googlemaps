var argscheck = require('cordova/argscheck'),
  utils = require('cordova/utils'),
  common = require('./Common'),
  LatLng = require('./LatLng'),
  event = require('./event'),
  Overlay = require('./Overlay');

/*****************************************************************************
 * Marker Class
 *****************************************************************************/
var Marker = function(map, id, markerOptions, className, _exec) {
  Overlay.call(this, map, id, className, _exec);

  var self = this;

  if (markerOptions && markerOptions.position) {
    self.set('position', markerOptions.position);
  }

  //-----------------------------------------------
  // Sets the initialize option to each property
  //-----------------------------------------------
  var ignores = ["map", "id", "hashCode", "type"];
  for (var key in markerOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, markerOptions[key]);
    }
  }

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on(event.MARKER_CLICK, function() {
    self.showInfoWindow.apply(self);
  });

  self.on("position_changed", function() {
    var position = self.get("position");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setPosition', [self.getId(), position.lat, position.lng]);
  });
  self.on("rotation_changed", function() {
    var rotation = self.get("rotation");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setRotation', [self.getId(), rotation]);
  });
  self.on("snippet_changed", function() {
    var snippet = self.get("snippet");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setSnippet', [self.getId(), snippet]);
  });
  self.on("visible_changed", function() {
    var visible = self.get("visible");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
  });
  self.on("title_changed", function() {
    var title = self.get("title");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setTitle', [self.getId(), title]);
  });
  self.on("icon_changed", function() {
    var icon = self.get("icon");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setIcon', [self.getId(), icon]);
  });
  self.on("flat_changed", function() {
    var flat = self.get("flat");
    flat = flat === true;
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setFlat', [self.getId(), flat]);
  });
  self.on("draggable_changed", function() {
    var draggable = self.get("draggable");
    draggable = draggable === true;
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setDraggable', [self.getId(), draggable]);
  });
  self.on("anchor_changed", function() {
    var anchor = self.get("anchor");
    if (!anchor) {
      return;
    }
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setIconAnchor', [self.getId(), anchor[0], anchor[1]]);
  });
  self.on("infoWindowAnchor_changed", function() {
    var anchor = self.get("infoWindowAnchor");
    if (!anchor) {
      return;
    }
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setInfoWindowAnchor', [self.getId(), anchor[0], anchor[1]]);
  });
  self.on("zIndex_changed", function() {
    var zIndex = self.get("zIndex");
    if (zIndex === null || zIndex === undefined) {
      return;
    }
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setZIndex', [self.getId(), zIndex]);
  });
  self.on("opacity_changed", function() {
    var opacity = self.get("opacity");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setOpacity', [self.getId(), opacity]);
  });
  self.on("disableAutoPan_changed", function() {
    var disableAutoPan = self.get("disableAutoPan");
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setDisableAutoPan', [self.getId(), disableAutoPan]);
  });

};

utils.extend(Marker, Overlay);

Marker.prototype._privateInitialize = function(markerOptions) {
  var self = this;
  //-----------------------------------------------
  // Sets the initialize option to each property
  //-----------------------------------------------
  var ignores = ["map", "id", "hashCode", "type"];
  for (var key in markerOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, markerOptions[key], true);
    }
  }

  //-----------------------------------------------
  // Trigger internal command queue
  //-----------------------------------------------
  Object.defineProperty(self, "_isReady", {
    value: true,
    writable: false
  });
  self.exec("nop");
};
Marker.prototype.remove = function(callback) {
  var self = this;
  if (self._isRemoved) {
    return;
  }
  Object.defineProperty(self, "_isRemoved", {
    value: true,
    writable: false
  });
  self.trigger(event.INFO_CLOSE); // close open infowindow, otherwise it will stay
  self.trigger(self.id + "_remove");
  self.exec.call(self, function() {
    self.destroy();
    if (typeof callback === "function") {
      callback.call(self);
    }
  }, self.errorHandler, self.getPluginName(), 'remove', [self.getId()], {
    remove: true
  });

};

Marker.prototype.getOptions = function() {
  var self = this;
  return {
    "id": self.getId(),
    "position": self.getPosition(),
    "disableAutoPan": self.get("disableAutoPan"),
    "opacity": self.get("opacity"),
    "icon": self.get("icon"),
    "zIndex": self.get("zIndex"),
    "anchor": self.get("anchor"),
    "infoWindowAnchor": self.get("infoWindowAnchor"),
    "draggable": self.get("draggable"),
    "title": self.getTitle(),
    "snippet": self.getSnippet(),
    "visible": self.get("visible"),
    "rotation": self.getRotation()
  };
};
Marker.prototype.getPosition = function() {
  var position = this.get('position');
  if (!(position instanceof LatLng)) {
    return new LatLng(position.lat, position.lng);
  }
  return position;
};
Marker.prototype.getId = function() {
  return this.id;
};
Marker.prototype.getMap = function() {
  return this.map;
};
Marker.prototype.getHashCode = function() {
  return this.hashCode;
};

Marker.prototype.setAnimation = function(animation, callback) {
  var self = this;

  animation = animation || null;
  if (!animation) {
    return;
  }
  this.set("animation", animation);
  self.exec.call(self, function() {
    if (typeof callback === "function") {
      callback.call(self);
    }
  }, this.errorHandler, self.getPluginName(), 'setAnimation', [this.getId(), animation]);
  return this;
};

Marker.prototype.setDisableAutoPan = function(disableAutoPan) {
  disableAutoPan = common.parseBoolean(disableAutoPan);
  this.set('disableAutoPan', disableAutoPan);
  return this;
};
Marker.prototype.setOpacity = function(opacity) {
  if (!opacity && opacity !== 0) {
    console.log('opacity value must be int or double');
    return false;
  }
  this.set('opacity', opacity);
  return this;
};
Marker.prototype.setZIndex = function(zIndex) {
  if (typeof zIndex === 'undefined') {
    return false;
  }
  this.set('zIndex', zIndex);
  return this;
};
Marker.prototype.getOpacity = function() {
  return this.get('opacity');
};
Marker.prototype.setIconAnchor = function(anchorX, anchorY) {
  this.set('anchor', [anchorX, anchorY]);
  return this;
};
Marker.prototype.setInfoWindowAnchor = function(anchorX, anchorY) {
  this.set('infoWindowAnchor', [anchorX, anchorY]);
  return this;
};
Marker.prototype.setDraggable = function(draggable) {
  draggable = common.parseBoolean(draggable);
  this.set('draggable', draggable);
  return this;
};
Marker.prototype.isDraggable = function() {
  return this.get('draggable');
};
Marker.prototype.setFlat = function(flat) {
  flat = common.parseBoolean(flat);
  this.set('flat', flat);
  return this;
};
Marker.prototype.setIcon = function(url) {
  if (url && common.isHTMLColorString(url)) {
    url = common.HTMLColor2RGBA(url);
  }
  this.set('icon', url);
  return this;
};
Marker.prototype.setTitle = function(title) {
  if (!title) {
    console.log('missing value for title');
    return this;
  }
  title = "" + title; // Convert to strings mandatory
  this.set('title', title);
  return this;
};
Marker.prototype.setVisible = function(visible) {
  visible = common.parseBoolean(visible);
  this.set('visible', visible);
  if (!visible && this.map.get("active_marker_id") === this.id) {
    this.map.set("active_marker_id", undefined);
  }
  return this;
};
Marker.prototype.getTitle = function() {
  return this.get('title');
};
Marker.prototype.setSnippet = function(snippet) {
  this.set('snippet', snippet);
  return this;
};
Marker.prototype.getSnippet = function() {
  return this.get('snippet');
};
Marker.prototype.setRotation = function(rotation) {
  if (typeof rotation !== "number") {
    console.log('missing value for rotation');
    return false;
  }
  this.set('rotation', rotation);
  return this;
};
Marker.prototype.getRotation = function() {
  return this.get('rotation');
};
Marker.prototype.showInfoWindow = function() {
  //if (!this.get("title") && !this.get("snippet") ||
  //    this.get("isInfoWindowVisible")) {
  if (!this.get("title") && !this.get("snippet")) {
    return;
  }
  this.set("isInfoWindowVisible", true);
  this.map.set("active_marker_id", this.id);
  self.exec.call(this, null, this.errorHandler, this.getPluginName(), 'showInfoWindow', [this.getId()], {
    sync: true
  });
  return this;
};
Marker.prototype.hideInfoWindow = function() {
  if (this.map.get("active_marker_id") === this.id) {
    this.map.set("active_marker_id", null);
  }
  if (this.get("isInfoWindowVisible")) {
    this.set("isInfoWindowVisible", false);
    self.exec.call(this, null, this.errorHandler, this.getPluginName(), 'hideInfoWindow', [this.getId()], {
      sync: true
    });
  }
  return this;
};
Marker.prototype.isInfoWindowShown = function() {
  return this.get("isInfoWindowVisible") === true;
};
Marker.prototype.isVisible = function() {
  return this.get("visible") === true;
};

Marker.prototype.setPosition = function(position) {
  if (!position) {
    console.log('missing value for position');
    return false;
  }
  this.set('position', position);
  return this;
};

module.exports = Marker;
