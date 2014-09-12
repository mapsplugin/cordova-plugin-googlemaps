/* global cordova, plugin, CSSPrimitiveValue */
var PLUGIN_NAME = 'GoogleMaps';
  var MARKERS = {};
  var KML_LAYERS = {};
  var OVERLAYS = {};
 
  /**
 * Google Maps model.
 */
var BaseClass = function() {
  var self = this;
  var _vars = {};
  var _listeners = {};
  
  self.empty = function() {
    for (var key in Object.keys(_vars)) {
      _vars[key] = null;
      delete _vars[key];
    }
  };
  
  self.get = function(key) {
    return key in _vars ? _vars[key] : null;
  };
  self.set = function(key, value) {
    if (_vars[key] !== value) {
      self.trigger(key + "_changed");
    }
    _vars[key] = value;
  };
  
  self.trigger = function(eventName) {
    var args = [];
    for (var i = 1; i < arguments.length; i++) {
      args.push(arguments[i]);
    }
    var event = document.createEvent('Event');
    event.initEvent(eventName, false, false);
    event.mydata = args;
    event.myself = self;
    document.dispatchEvent(event);
  };
  self.on = function(eventName, callback) {
    _listeners[eventName] = _listeners[eventName] || [];
    
    var listener = function (e) {
      if (!e.myself || e.myself !== self) {
        return;
      }
      callback.apply(self, e.mydata);
    };
    document.addEventListener(eventName, listener, false);
    _listeners[eventName].push({
      'callback': callback,
      'listener': listener
    });
  };
  self.addEventListener = self.on;
  
  self.off = function(eventName, callback) {
    var i;
    if (typeof eventName === "string" &&
        eventName in _listeners) {
      
      if (typeof callback === "function") {
        for (i = 0; i < _listeners[eventName].length; i++) {
          if (_listeners[eventName][i].callback === callback) {
            document.removeEventListener(eventName, _listeners[eventName][i].listener);
            _listeners[eventName].splice(i, 1);
            break;
          }
        }
      } else {
        delete _listeners[eventName];
      }
    } else {
      //Remove all event listeners
      var eventNames = Object.keys(_listeners);
      for (i = 0; i < eventNames.length; i++) {
        eventName = eventNames[i];
        for (var j = 0; j < _listeners[eventName].length; j++) {
          document.removeEventListener(eventName, _listeners[eventName][j].listener);
        }
      }
      _listeners = {};
    }
  };
  
  self.removeEventListener = self.off;
  
  
  self.one = function(eventName, callback) {
    _listeners[eventName] = _listeners[eventName] || [];
    
    var listener = function (e) {
      if (!e.myself || e.myself !== self) {
        return;
      }
      callback.apply(self, e.mydata);
      self.off(eventName, callback);
    };
    document.addEventListener(eventName, listener, false);
    _listeners[eventName].push({
      'callback': callback,
      'listener': listener
    });
  };
  self.addEventListenerOnce = self.one;
  
  self.errorHandler = function(msg) {
    console.error(msg);
    self.trigger('error', msg);
    return false;
  };
  
  return self;
};
var App = function() {
  BaseClass.apply(this);
  Object.defineProperty(this, "type", {
    value: "Map",
    writable: false
  });
};
App.prototype = new BaseClass();

/*
 * Callback from Native
 */
App.prototype._onMarkerEvent = function(eventName, hashCode) {
  var marker = MARKERS[hashCode] || null;
  if (marker) {
    marker.trigger(eventName, marker);
  }
};

App.prototype._onKmlEventForIOS = function(kmlLayerId, result, options) {
  var id = result.id,
      objectType = id.replace(/_.*$/, "").toLowerCase(),
      eventName = objectType + "_add";
  this._onKmlEvent(eventName, kmlLayerId, result, options);
};

App.prototype._onOverlayEvent = function(eventName, hashCode) {
  var overlay = OVERLAYS[hashCode] || null;
  if (overlay) {
    var args = [eventName, overlay];
    for (var i = 2; i < arguments.length; i++) {
      args.push(arguments[i]);
    }
    overlay.trigger.apply(this, args);
  }
};
 
/*
 * Callback from Native
 */
App.prototype._onKmlEvent = function(eventName, kmlLayerId, result, options) {
  var kmlLayer = KML_LAYERS[kmlLayerId] || null;
  if (kmlLayer) {
    var self = this;
    var args = [eventName];
    if (eventName.substr(-4, 4) === "_add") {
      var objectType = eventName.replace(/_.*$/, ""),
          overlay = null;
      
      switch(objectType) {
        case "marker":
          overlay = new Marker(self, result.id, options);
          MARKERS[result.id] = overlay;
          args.push({
            "type": "Marker",
            "object": overlay
          });
          overlay.on(plugin.google.maps.event.MARKER_CLICK, function() {
            kmlLayer.trigger(plugin.google.maps.event.OVERLAY_CLICK, overlay, overlay.getPosition());
          });
          break;
          
        case "polygon":
          overlay = new Polygon(self, result.id, options);
          args.push({
            "type": "Polygon",
            "object": overlay
          });
          
          overlay.on(plugin.google.maps.event.OVERLAY_CLICK, function(latLng) {
            kmlLayer.trigger(plugin.google.maps.event.OVERLAY_CLICK, overlay, latLng);
          });
          break;
          
        case "polyline":
          overlay = new Polyline(self, result.id, options);
          args.push({
            "type": "Polyline",
            "object": overlay
          });
          overlay.on(plugin.google.maps.event.OVERLAY_CLICK, function(latLng) {
            kmlLayer.trigger(plugin.google.maps.event.OVERLAY_CLICK, overlay, latLng);
          });
          break;
      }
      if (overlay) {
        OVERLAYS[result.id] = overlay;
        overlay.hashCode = result.hashCode;
        kmlLayer.on("_REMOVE", function() {
          overlay.remove();
          overlay.off();
        });
      }
    } else {
      for (var i = 2; i < arguments.length; i++) {
        args.push(arguments[i]);
      }
    }
    //kmlLayer.trigger.apply(kmlLayer, args);
  }
};

/**
 * Callback from Native
 */
App.prototype._onMapEvent = function(eventName) {
  var args = [eventName];
  for (var i = 1; i < arguments.length; i++) {
    args.push(arguments[i]);
  }
  args.push(this);
  this.trigger.apply(this, args);
};
/**
 * Callback from Native
 */
App.prototype._onMyLocationChange = function(params) {
  var location = new Location(params);
  this.trigger('my_location_change', location, this);
};
/**
 * Callback from Native
 */
App.prototype._onCameraEvent = function(eventName, params) {
  var cameraPosition = new CameraPosition(params);
  this.trigger(eventName, cameraPosition, this);
};


App.prototype.getMap = function(div, params) {
  var self = this,
      args = [];
  
  if (!isDom(div)) {
    params = div;
    params = params || {};
    params.backgroundColor = HTMLColor2RGBA(params.backgroundColor);
    args.push(params);
  } else {
    var children = div.childNodes;
    params = params || {};
    params.backgroundColor = HTMLColor2RGBA(params.backgroundColor);
    args.push(params);
    
    self.set("div", div);
    args.push(getDivSize(div));
    var elements = [];
    var elemId;
    
    for (var i = 0; i < children.length; i++) {
      element = children[i];
      if (element.nodeType != 1) {
        continue;
      }
      elemId = element.getAttribute("__pluginDomId");
      if (!elemId) {
        elemId = "pgm" + Math.floor(Math.random() * Date.now()) + i;
        element.setAttribute("__pluginDomId", elemId);
      }
      elements.push({
        id: elemId,
        size: getDivSize(element)
      });
    }
    args.push(elements);
    
    div.addEventListener("DOMNodeRemoved", _remove_child);
    div.addEventListener("DOMNodeInserted", _append_child);
    
    while(div.parentNode) {
      div.style.backgroundColor = 'rgba(0,0,0,0)';
      div = div.parentNode;
    }
  }
  cordova.exec(function() {
    setTimeout(function() {
      self.refreshLayout();
      self.trigger(plugin.google.maps.event.MAP_READY, self);
    }, 100);
  }, self.errorHandler, PLUGIN_NAME, 'getMap', args);
  return self;
};



App.prototype.getLicenseInfo = function(callback) {
  var self = this;
  cordova.exec(function(txt) {
    callback.call(self, txt);
  }, self.errorHandler, PLUGIN_NAME, 'getLicenseInfo', []);
};

/**
 * @desc Open the map dialog
 */
App.prototype.showDialog = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'showDialog', []);
};

/**
 * @desc Close the map dialog
 */
App.prototype.closeDialog = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'closeDialog', []);
};

App.prototype.setOptions = function(options) {
  options = options || {};
  options.backgroundColor = HTMLColor2RGBA(options.backgroundColor);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setOptions', options]);
};

App.prototype.setCenter = function(latLng) {
  this.set('center', latLng);
  cordova.exec(null, this.errorHandler,
    PLUGIN_NAME, 'exec', ['Map.setCenter', latLng.lat, latLng.lng]);
};

App.prototype.setZoom = function(zoom) {
  this.set('zoom', zoom);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setZoom', zoom]);
  };
 
  /**
 * @desc Change the map type
 * @param {String} mapTypeId   Specifies the one of the follow strings:
 *                               MAP_TYPE_HYBRID
 *                               MAP_TYPE_SATELLITE
 *                               MAP_TYPE_TERRAIN
 *                               MAP_TYPE_NORMAL
 *                               MAP_TYPE_NONE
 */
App.prototype.setMapTypeId = function(mapTypeId) {
  if (mapTypeId !== plugin.google.maps.MapTypeId[mapTypeId.replace("MAP_TYPE_", '')]) {
    return this.errorHandler("Invalid MapTypeId was specified.");
  }
  this.set('mapTypeId', mapTypeId);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setMapTypeId', mapTypeId]);
  };
 
  /**
 * @desc Change the map view angle
 * @param {Number} tilt  The angle
 */
App.prototype.setTilt = function(tilt) {
  this.set('tilt', tilt);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setTilt', tilt]);
  };
 
 
  /**
 * @desc   Move the map camera with animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
App.prototype.animateCamera = function(cameraPosition, callback) {
  if (cameraPosition.target &&
      cameraPosition.target.type === "LatLngBounds") {
      cameraPosition.target = [cameraPosition.target.southwest, cameraPosition.target.northeast];
    }
 
    var self = this;
    cordova.exec(function() {
      if (typeof callback === "function") {
      callback.call(self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.animateCamera', cameraPosition]);
};
/**
 * @desc   Move the map camera without animation
 * @params {CameraPosition} cameraPosition New camera position
 * @params {Function} [callback] This callback is involved when the animation is completed.
 */
App.prototype.moveCamera = function(cameraPosition, callback) {
  if (cameraPosition.target &&
      cameraPosition.target.type === "LatLngBounds") {
    cameraPosition.target = [cameraPosition.target.southwest, cameraPosition.target.northeast];
  }
  var self = this;
  cordova.exec(function() {
    if (typeof callback === "function") {
      callback.call(self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.moveCamera', cameraPosition]);
};

App.prototype.setMyLocationEnabled = function(enabled) {
  enabled = parseBoolean(enabled);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setMyLocationEnabled', enabled]);
};
App.prototype.setIndoorEnabled = function(enabled) {
  enabled = parseBoolean(enabled);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setIndoorEnabled', enabled]);
};
App.prototype.setTrafficEnabled = function(enabled) {
  enabled = parseBoolean(enabled);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Map.setTrafficEnabled', enabled]);
};
App.prototype.setCompassEnabled = function(enabled) {
  var self = this;
  enabled = parseBoolean(enabled);
  cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setCompassEnabled', enabled]);
};
App.prototype.getMyLocation = function(params, success_callback, error_callback) {
  var args = [params || {}, success_callback || null, error_callback ];
  if (typeof args[0] === "function") {
    args.unshift({});
  }
  params = args[0];
  success_callback = args[1];
  error_callback = args[2];
  
  params.enableHighAccuracy = params.enableHighAccuracy || false;
  var self = this;
  var successHandler = function(location) {
    if (typeof success_callback === "function") {
      location.latLng = new LatLng(location.latLng.lat, location.latLng.lng);
      success_callback.call(self, location);
    }
  };
  var errorHandler = function(result) {
    if (typeof error_callback === "function") {
      error_callback.call(self, result);
    }
  };
  cordova.exec(successHandler, errorHandler, PLUGIN_NAME, 'getMyLocation', [params]);
};
App.prototype.setVisible = function(isVisible) {
  var self = this;
  isVisible = parseBoolean(isVisible);
  cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setVisible', [isVisible]);
};
App.prototype.setClickable = function(isClickable) {
  var self = this;
  isClickable = parseBoolean(isClickable);
  cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'pluginLayer_setClickable', [isClickable]);
};

App.prototype.setBackgroundColor = function(color) {
  this.set('strokeColor', color);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'pluginLayer_setBackGroundColor', [HTMLColor2RGBA(color)]);
};
/**
 * Sets the preference for whether all gestures should be enabled or disabled.
 */
App.prototype.setAllGesturesEnabled = function(enabled) {
  var self = this;
  enabled = parseBoolean(enabled);
  cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setAllGesturesEnabled', enabled]);
};
 
  /**
 * Return the current position of the camera
 * @return {CameraPosition}
 */
App.prototype.getCameraPosition = function(callback) {
  var self = this;
  cordova.exec(function(camera) {
    if (typeof callback === "function") {
      camera.target = new LatLng(camera.target.lat, camera.target.lng);
      callback.call(self, camera);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
};

/**
 * Clears all markup that has been added to the map,
 * including markers, polylines and ground overlays.
 */
App.prototype.clear = function(callback) {
  var self = this;
  var overlayIDs = Object.keys(OVERLAYS);
  var overlayId;
  for (var i = 0; i < overlayIDs.length; i++) {
    overlayId = overlayIDs[i];
    OVERLAYS[overlayId].off();
    delete OVERLAYS[overlayId];
  }
  OVERLAYS = {};
  self.off();
  cordova.exec(function() {
    if (typeof callback === "function") {
      callback.call(self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'clear', []);
};

/**
 * Remove the map completely.
 */
App.prototype.remove = function() {
  this.set('div', undefined);
  this.clear();
  self.empty();
  cordova.exec(null, null, PLUGIN_NAME, 'remove', []);
};

App.prototype.refreshLayout = function() {
  onMapResize(undefined, false);
};

App.prototype.isAvailable = function(callback) {
  var self = this;
  cordova.exec(function() {
    if (typeof callback === "function") {
      callback.call(self, true);
    }
  }, function(message) {
    if (typeof callback === "function") {
      callback.call(self, false, message);
    }
  }, PLUGIN_NAME, 'isAvailable', []);
};

App.prototype.toDataURL = function(callback) {
  var self = this;
  cordova.exec(function(image) {
    if (typeof callback === "function") {
      callback.call(self, image);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.toDataURL']);
};

var _append_child = function(event) {
  event = event || window.event;
  event = event || {};
  var target = event.srcElement;
  if (!target || "nodeType" in target == false) {
    return;
  }
  if (target.nodeType != 1) {
    return;
  }
  var size = getDivSize(target);
  var elemId = "pgm" + Math.floor(Math.random() * Date.now());
  target.setAttribute("__pluginDomId", elemId);
  
  cordova.exec(null, null, PLUGIN_NAME, 'pluginLayer_pushHtmlElement', [elemId, size]);
};

var _remove_child = function(event) {
  event = event || window.event;
  event = event || {};
  var target = event.srcElement;
  if (!target || "nodeType" in target == false) {
    return;
  }
  if (target.nodeType != 1) {
    return;
  }
  var elemId = target.getAttribute("__pluginDomId");
  if (!elemId) {
    return;
  }
  target.removeAttribute("__pluginDomId");
  cordova.exec(null, null, PLUGIN_NAME, 'pluginLayer_removeHtmlElement', [elemId]);
};

/**
 * Show the map into the specified div.
 */
App.prototype.setDiv = function(div) {
  var self = this,
      args = [],
      element;
  
  if (isDom(div) === false) {
    div = self.get("div");
    if (div) {
      var children = div.childNodes;
      for (var i = 0; i < children.length; i++) {
        element = children[i];
        if (element.nodeType != 1) {
          continue;
        }
        elemId = element.getAttribute("__pluginDomId");
        element.removeAttribute("__pluginDomId");
      }
      div.removeEventListener("DOMNodeRemoved", _remove_child);
    }
    self.set("div", null);
  } else {
    var children = div.childNodes;
    self.set("div", div);
    args.push(getDivSize(div));
    var elements = [];
    var elemId;
    
    for (var i = 0; i < children.length; i++) {
      element = children[i];
      if (element.nodeType != 1) {
        continue;
      }
      elemId = element.getAttribute("__pluginDomId");
      if (!elemId) {
        elemId = "pgm" + Math.floor(Math.random() * Date.now()) + i;
        element.setAttribute("__pluginDomId", elemId);
      }
      elements.push({
        id: elemId,
        size: getDivSize(element)
      });
    }
    args.push(elements);
    
    div.addEventListener("DOMNodeRemoved", _remove_child);
    div.addEventListener("DOMNodeInserted", _append_child);
    
    while(div.parentNode) {
      div.style.backgroundColor = 'rgba(0,0,0,0)';
      div = div.parentNode;
    }
    setTimeout(function() {
      self.refreshLayout();
    }, 1000);
  }
  cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setDiv', args);
};
 
/**
 * Return the visible region of the map.
 * Thanks @fschmidt
 */
App.prototype.getVisibleRegion = function(callback) {
  var self = this;
  
  cordova.exec(function(result) {
    if (typeof callback === "function") {
      var latLngBounds = new LatLngBounds(result.latLngArray);
      latLngBounds.northeast = new LatLng(result.northeast.lat, result.northeast.lng);
      latLngBounds.southwest = new LatLng(result.southwest.lat, result.southwest.lng);
      callback.call(self, latLngBounds);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getVisibleRegion']);
};
 
//-------------
// Marker
//-------------
App.prototype.addMarker = function(markerOptions, callback) {
  var self = this;
  markerOptions.position = markerOptions.position || {};
  markerOptions.position.lat = markerOptions.position.lat || 0.0;
  markerOptions.position.lng = markerOptions.position.lng || 0.0;
  markerOptions.anchor = markerOptions.anchor || [0.5, 0.5];
  markerOptions.draggable = markerOptions.draggable || false;
  markerOptions.icon = markerOptions.icon || undefined;
  markerOptions.snippet = markerOptions.snippet || undefined;
  markerOptions.title = markerOptions.title || undefined;
  markerOptions.visible = markerOptions.visible === undefined ? true : markerOptions.visible;
  markerOptions.flat = markerOptions.flat || false;
  markerOptions.rotation = markerOptions.rotation || 0;
  markerOptions.opacity = parseFloat("" + markerOptions.opacity, 10) || 1;
  markerOptions.disableAutoPan = markerOptions.disableAutoPan === undefined ? false: markerOptions.disableAutoPan;
  if ("styles" in markerOptions) {
    markerOptions.styles = typeof markerOptions.styles === "object" ? markerOptions.styles : {};
    
    if ("color" in markerOptions.styles) {
      markerOptions.styles.color = HTMLColor2RGBA(markerOptions.styles.color || "#000000");
      }
    }
 
    cordova.exec(function(result) {
      markerOptions.hashCode = result.hashCode;
      var marker = new Marker(self, result.id, markerOptions);
      MARKERS[result.id] = marker;
      OVERLAYS[result.id] = marker;
      
      if (typeof markerOptions.markerClick === "function") {
      marker.on(plugin.google.maps.event.MARKER_CLICK, markerOptions.markerClick);
    }
    if (typeof markerOptions.infoClick === "function") {
      marker.on(plugin.google.maps.event.INFO_CLICK, markerOptions.infoClick);
    }
    if (typeof callback === "function") {
      callback.call(self,  marker, self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.createMarker', markerOptions]);
};


//-------------
// Circle
//-------------
App.prototype.addCircle = function(circleOptions, callback) {
  var self = this;
  circleOptions.center = circleOptions.center || {};
  circleOptions.center.lat = circleOptions.center.lat || 0.0;
  circleOptions.center.lng = circleOptions.center.lng || 0.0;
  circleOptions.strokeColor = HTMLColor2RGBA(circleOptions.strokeColor || "#FF0000");
  circleOptions.fillColor = HTMLColor2RGBA(circleOptions.fillColor || "#000000");
  circleOptions.strokeWidth = circleOptions.strokeWidth || 10;
  circleOptions.visible = circleOptions.visible === undefined ? true : circleOptions.visible;
  circleOptions.zIndex = circleOptions.zIndex || 0.0;
  circleOptions.radius = circleOptions.radius || 1;
 
  cordova.exec(function(result) {
    var circle = new Circle(self, result.id, circleOptions);
    OVERLAYS[result.id] = circle;
    if (typeof circleOptions.onClick === "function") {
      circle.on(plugin.google.maps.event.OVERLAY_CLICK, circleOptions.onClick);
    }
    if (typeof callback === "function") {
      callback.call(self, circle, self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.createCircle', circleOptions]);
};
//-------------
// Polyline
//-------------
App.prototype.addPolyline = function(polylineOptions, callback) {
  var self = this;
  polylineOptions.points = polylineOptions.points || [];
  polylineOptions.color = HTMLColor2RGBA(polylineOptions.color || "#FF000080");
  polylineOptions.width = polylineOptions.width || 10;
  polylineOptions.visible = polylineOptions.visible === undefined ? true : polylineOptions.visible;
  polylineOptions.zIndex = polylineOptions.zIndex || 0.0;
  polylineOptions.geodesic = polylineOptions.geodesic || false;
  
  cordova.exec(function(result) {
    var polyline = new Polyline(self, result.id, polylineOptions);
    OVERLAYS[result.id] = polyline;
    /*if (typeof polylineOptions.onClick === "function") {
      polyline.on(plugin.google.maps.event.OVERLAY_CLICK, polylineOptions.onClick);
    }*/
    if (typeof callback === "function") {
      callback.call(self,  polyline, self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.createPolyline', polylineOptions]);
};
//-------------
// Polygon
//-------------
App.prototype.addPolygon = function(polygonOptions, callback) {
  var self = this;
  polygonOptions.points = polygonOptions.points || [];
  polygonOptions.strokeColor = HTMLColor2RGBA(polygonOptions.strokeColor || "#FF000080");
  if (polygonOptions.fillColor) {
    polygonOptions.fillColor = HTMLColor2RGBA(polygonOptions.fillColor);
  }
  polygonOptions.strokeWidth = polygonOptions.strokeWidth || 10;
  polygonOptions.visible = polygonOptions.visible === undefined ? true : polygonOptions.visible;
  polygonOptions.zIndex = polygonOptions.zIndex || 0.0;
  polygonOptions.geodesic = polygonOptions.geodesic || false;
  
  cordova.exec(function(result) {
    var polygon = new Polygon(self, result.id, polygonOptions);
    OVERLAYS[result.id] = polygon;
    if (typeof polygonOptions.onClick === "function") {
      polygon.on(plugin.google.maps.event.OVERLAY_CLICK, polygonOptions.onClick);
    }
    if (typeof callback === "function") {
      callback.call(self,  polygon, self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.createPolygon', polygonOptions]);
};

//-------------
// Tile overlay
//-------------
App.prototype.addTileOverlay = function(tilelayerOptions, callback) {
  var self = this;
  tilelayerOptions = tilelayerOptions || {};
  tilelayerOptions.tileUrlFormat = tilelayerOptions.tileUrlFormat || null;
  if (typeof tilelayerOptions.tileUrlFormat !== "string") {
    throw new Error("tilelayerOptions.tileUrlFormat should set a string.");
  }
  tilelayerOptions.visible = tilelayerOptions.visible === undefined ? true : tilelayerOptions.visible;
  tilelayerOptions.zIndex = tilelayerOptions.zIndex || 0;
  tilelayerOptions.width = tilelayerOptions.width || 256;
  tilelayerOptions.height = tilelayerOptions.height || 256;
  
  cordova.exec(function(result) {
    var tileOverlay = new TileOverlay(self, result.id, tilelayerOptions);
    OVERLAYS[result.id] = tileOverlay;
    /*
    if (typeof tilelayerOptions.onClick === "function") {
      tileOverlay.on(plugin.google.maps.event.OVERLAY_CLICK, tilelayerOptions.onClick);
    }
    */
    if (typeof callback === "function") {
      callback.call(self,  tileOverlay, self);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.createTileOverlay', tilelayerOptions]);
};
//-------------
// Ground overlay
//-------------
App.prototype.addGroundOverlay = function(groundOverlayOptions, callback) {
  var self = this;
  groundOverlayOptions = groundOverlayOptions || {};
  groundOverlayOptions.url = groundOverlayOptions.url || null;
  groundOverlayOptions.visible = groundOverlayOptions.visible === undefined ? true : groundOverlayOptions.visible;
  groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 0;
  groundOverlayOptions.bounds = groundOverlayOptions.bounds || [];
  
  var pluginExec = function() {
    cordova.exec(function(result) {
      var groundOverlay = new GroundOverlay(self, result.id, groundOverlayOptions);
      OVERLAYS[result.id] = groundOverlay;
      if (typeof groundOverlayOptions.onClick === "function") {
        groundOverlay.on(plugin.google.maps.event.OVERLAY_CLICK, groundOverlayOptions.onClick);
      }
      if (typeof callback === "function") {
        callback.call(self,  groundOverlay, self);
      }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.createGroundOverlay', groundOverlayOptions]);
  };
  
  pluginExec();
  
  
};

//-------------
// KML Layer
//-------------
  App.prototype.addKmlOverlay = function(kmlOverlayOptions, callback) {
    var self = this;
    kmlOverlayOptions = kmlOverlayOptions || {};
    kmlOverlayOptions.url = kmlOverlayOptions.url || null;
    kmlOverlayOptions.preserveViewport = kmlOverlayOptions.preserveViewport || false;
    kmlOverlayOptions.animation = kmlOverlayOptions.animation === undefined ? true : kmlOverlayOptions.animation;
 
    var pluginExec = function() {
      cordova.exec(function(kmlId) {
        var kmlOverlay = new KmlOverlay(self, kmlId, kmlOverlayOptions);
        OVERLAYS[kmlId] = kmlOverlay;
        KML_LAYERS[kmlId] = kmlOverlay;
        if (typeof callback === "function") {
        callback.call(self,  kmlOverlay, self);
      }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['KmlOverlay.createKmlOverlay', kmlOverlayOptions]);
  };
  
  pluginExec();
};
//-------------
// Geocoding
//-------------
App.prototype.geocode = function(geocoderRequest, callback) {
  console.log("Map.geocode will be deprecated. Please use Geocoder.geocode instead.");
  Geocoder.geocode(geocoderRequest, callback);
};
/********************************************************************************
 * @name CameraPosition
 * @class This class represents new camera position
 * @property {LatLng} target The location where you want to show
 * @property {Number} [tilt] View angle
 * @property {Number} [zoom] Zoom level
 * @property {Number} [bearing] Map orientation
 * @property {Number} [duration] The duration of animation
 *******************************************************************************/
var CameraPosition = function(params) {
  var self = this;
  self.zoom = params.zoom;
  self.tilt = params.tilt;
  self.target = params.target;
  self.bearing = params.bearing;
  self.hashCode = params.hashCode;
  self.duration = params.duration;
};
/*****************************************************************************
 * Location Class
 *****************************************************************************/
var Location = function(params) {
  var self = this;
  self.latLng = params.latLng || new LatLng(params.lat || 0, params.lng || 0);
  self.elapsedRealtimeNanos = params.elapsedRealtimeNanos;
  self.time = params.time;
  self.accuracy = params.accuracy || null;
  self.bearing = params.bearing || null;
  self.altitude = params.altitude || null;
  self.speed = params.speed || null;
  self.provider = params.provider;
  self.hashCode = params.hashCode;
};
 
/*******************************************************************************
 * @name LatLng
 * @class This class represents new camera position
 * @param {Number} latitude
 * @param {Number} longitude
 ******************************************************************************/
var LatLng = function(latitude, longitude) {
  var self = this;
  /**
   * @property {Number} latitude
   */
  self.lat = parseFloat(latitude || 0, 10);
 
  /**
   * @property {Number} longitude
   */
  self.lng = parseFloat(longitude || 0, 10);
 
  /**
   * Comparison function.
   * @method
   * @return {Boolean}
   */
  self.equals = function(other) {
    other = other || {};
    return other.lat === self.lat &&
           other.lng === self.lng;
  };
 
  /**
   * @method
   * @return {String} latitude,lontitude
   */
  self.toString = function() {
    return self.lat + "," + self.lng;
  };
 
  /**
   * @method
   * @param {Number}
   * @return {String} latitude,lontitude
   */
  self.toUrlValue = function(precision) {
    precision = precision || 6;
    return self.lat.toFixed(precision) + "," + self.lng.toFixed(precision);
  };
};

/*****************************************************************************
 * Marker Class
 *****************************************************************************/
var Marker = function(map, id, markerOptions) {
  BaseClass.apply(this);
  
  var self = this;
  
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  Object.defineProperty(self, "hashCode", {
    value: markerOptions.hashCode,
    writable: false
  });
  Object.defineProperty(self, "id", {
    value: id,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "Marker",
    writable: false
  });
  
  var ignores = ["hashCode", "id", "hashCode", "type"];
  for (var key in markerOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, markerOptions[key]);
    }
  }
};
Marker.prototype = new BaseClass();

Marker.prototype.isVisible = function() {
  return this.get('visible');
};


Marker.prototype.getPosition = function(callback) {
  var self = this;
  cordova.exec(function(latlng) {
    if (typeof callback === "function") {
      callback.call(self,  new LatLng(latlng.lat, latlng.lng));
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.getPosition', this.getId()]);
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

Marker.prototype.remove = function(callback) {
  var self = this;
  delete MARKERS[this.id];
  cordova.exec(function() {
    if (typeof callback === "function") {
      callback.call(self);
    }
  }, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.remove', this.getId()]);
  this.off();
};
Marker.prototype.setDisableAutoPan = function(disableAutoPan) {
  this.set('disableAutoPan', disableAutoPan);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setDisableAutoPan', this.getId(), disableAutoPan]);
};
Marker.prototype.setOpacity = function(opacity) {
  this.set('opacity', opacity);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setOpacity', this.getId(), opacity]);
};
Marker.prototype.getOpacity = function() {
  return this.get('opacity');
};
Marker.prototype.setIconAnchor = function(anchorX, anchorY) {
  this.set('anchor', [anchorX, anchorY]);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setIconAnchor', this.getId(), anchorX, anchorY]);
};
Marker.prototype.setInfoWindowAnchor = function(anchorX, anchorY) {
  this.set('anchor', [anchorX, anchorY]);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setInfoWindowAnchor', this.getId(), anchorX, anchorY]);
};
Marker.prototype.setDraggable = function(draggable) {
  draggable = parseBoolean(draggable);
  this.set('draggable', draggable);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setDraggable', this.getId(), draggable]);
};
Marker.prototype.isDraggable = function() {
  return this.get('draggable');
};
Marker.prototype.setFlat = function(flat) {
  flat = parseBoolean(flat);
  this.set('flat', flat);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setFlat', this.getId(), flat]);
};
Marker.prototype.setIcon = function(url) {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setIcon', this.getId(), url]);
};
Marker.prototype.setTitle = function(title) {
  this.set('title', title);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setTitle', this.getId(), title]);
};
Marker.prototype.setVisible = function(visible) {
  this.set('visible', visible);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setVisible', this.getId(), visible]);
};
Marker.prototype.getTitle = function() {
  return this.get('title');
};
Marker.prototype.setSnippet = function(snippet) {
  this.set('snippet', snippet);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setSnippet', this.getId(), snippet]);
};
Marker.prototype.getSnippet = function() {
  return this.get('snippet');
};
Marker.prototype.setRotation = function(rotation) {
  this.set('rotation', rotation);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setRotation', this.getId(), rotation]);
};
Marker.prototype.getRotation = function() {
  return this.get('rotation');
};
Marker.prototype.showInfoWindow = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.showInfoWindow', this.getId()]);
};
Marker.prototype.hideInfoWindow = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.hideInfoWindow', this.getId()]);
};
Marker.prototype.isInfoWindowShown = function(callback) {
  var self = this;
  cordova.exec(function(isVisible) {
    isVisible = parseBoolean(isVisible);
    if (typeof callback === "function") {
      callback.call(self, isVisible);
    }
  }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.isInfoWindowShown', this.getId()]);
};
Marker.prototype.isVisible = function() {
  return this.get("visible");
};

Marker.prototype.setPosition = function(position) {
  this.set('position', position);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setPosition', this.getId(), position.lat, position.lng]);
};


/*****************************************************************************
 * Circle Class
 *****************************************************************************/
var Circle = function(map, circleId, circleOptions) {
  BaseClass.apply(this);
  
  var self = this;
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  Object.defineProperty(self, "id", {
    value: circleId,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "Circle",
    writable: false
  });
  
  var ignores = ["map", "id",  "type"];
  for (var key in circleOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, circleOptions[key]);
    }
  }
};

Circle.prototype = new BaseClass();

Circle.prototype.getMap = function() {
  return this.map;
};
Circle.prototype.getId = function() {
  return this.id;
};
Circle.prototype.getCenter = function() {
  return this.get('center');
};
Circle.prototype.getRadius = function() {
  return this.get('radius');
};
Circle.prototype.getStrokeColor = function() {
  return this.get('strokeColor');
};
Circle.prototype.getStrokeWidth = function() {
  return this.get('strokeWidth');
};
Circle.prototype.getZIndex = function() {
  return this.get('zIndex');
};
Circle.prototype.getVisible = function() {
  return this.get('visible');
};
Circle.prototype.remove = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.remove', this.getId()]);
  this.off();
};
Circle.prototype.setCenter = function(center) {
  this.set('center', center);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setCenter', this.getId(), center.lat, center.lng]);
};
Circle.prototype.setFillColor = function(color) {
  this.set('fillColor', color);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setFillColor', this.getId(), HTMLColor2RGBA(color)]);
};
Circle.prototype.setStrokeColor = function(color) {
  this.set('strokeColor', color);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setStrokeColor', this.getId(), HTMLColor2RGBA(color)]);
};
Circle.prototype.setStrokeWidth = function(width) {
  this.set('strokeWidth', width);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setStrokeWidth', this.getId(), width]);
};
Circle.prototype.setVisible = function(visible) {
  visible = parseBoolean(visible);
  this.set('visible', visible);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setVisible', this.getId(), visible]);
};
Circle.prototype.setZIndex = function(zIndex) {
  this.set('zIndex', zIndex);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setZIndex', this.getId(), zIndex]);
};
Circle.prototype.setRadius = function(radius) {
  this.set('radius', radius);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setRadius', this.getId(), radius]);
};
/*****************************************************************************
 * Polyline Class
 *****************************************************************************/
var Polyline = function(map, polylineId, polylineOptions) {
  BaseClass.apply(this);
  
  var self = this;
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  Object.defineProperty(self, "id", {
    value: polylineId,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "Polyline",
    writable: false
  });
  
  var ignores = ["map", "id",  "type"];
  for (var key in polylineOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, polylineOptions[key]);
    }
  }
};

Polyline.prototype = new BaseClass();

Polyline.prototype.getId = function() {
  return this.id;
};

Polyline.prototype.setPoints = function(points) {
  this.set('points', points);
  var i,
      path = [];
  for (i = 0; i < points.length; i++) {
    path.push({
      "lat": points[i].lat,
      "lng": points[i].lng
    });
  }
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setPoints', this.getId(), path]);
};
Polyline.prototype.getPoints = function() {
  return this.get("points");
};
Polyline.prototype.setColor = function(color) {
  this.set('color', color);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setColor', this.getId(), HTMLColor2RGBA(color)]);
};
Polyline.prototype.getColor = function() {
  return this.get('color');
};
Polyline.prototype.setWidth = function(width) {
  this.set('width', width);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setWidth', this.getId(), width]);
};
Polyline.prototype.getWidth = function() {
  return this.get('width');
};
Polyline.prototype.setVisible = function(visible) {
  visible = parseBoolean(visible);
  this.set('visible', visible);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setVisible', this.getId(), visible]);
};
Polyline.prototype.getVisible = function() {
  return this.get('visible');
};
Polyline.prototype.setGeodesic = function(geodesic) {
  geodesic = parseBoolean(geodesic);
  this.set('geodesic', geodesic);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setGeodesic', this.getId(), geodesic]);
};
Polyline.prototype.getGeodesic = function() {
  return this.get('geodesic');
};
Polyline.prototype.setZIndex = function(zIndex) {
  this.set('zIndex', zIndex);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setZIndex', this.getId(), zIndex]);
};
Polyline.prototype.getZIndex = function() {
  return this.get('zIndex');
};
Polyline.prototype.remove = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.remove', this.getId()]);
  this.off();
};

Polyline.prototype.getMap = function() {
  return this.map;
};
/*****************************************************************************
 * Polygon Class
 *****************************************************************************/
var Polygon = function(map, polygonId, polygonOptions) {
  BaseClass.apply(this);
  
  var self = this;
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  Object.defineProperty(self, "id", {
    value: polygonId,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "Polygon",
    writable: false
  });
  var ignores = ["map", "id",  "type"];
  for (var key in polygonOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, polygonOptions[key]);
    }
  }
};

Polygon.prototype = new BaseClass();

Polygon.prototype.getMap = function() {
  return this.map;
};
Polygon.prototype.getId = function() {
  return this.id;
};
Polygon.prototype.setPoints = function(points) {
  this.set('points', points);
  var i,
      path = [];
  for (i = 0; i < points.length; i++) {
    path.push({
      "lat": points[i].lat,
      "lng": points[i].lng
    });
  }
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setPoints', this.getId(), path]);
};
Polygon.prototype.getPoints = function() {
  return this.get("points");
};
Polygon.prototype.setFillColor = function(color) {
  this.set('fillColor', color);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setFillColor', this.getId(), HTMLColor2RGBA(color)]);
};
Polygon.prototype.getFillColor = function() {
  return this.get('fillColor');
};
Polygon.prototype.setStrokeColor = function(color) {
  this.set('strokeColor', color);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setStrokeColor', this.getId(), HTMLColor2RGBA(color)]);
};
Polygon.prototype.getStrokeColor = function() {
  return this.get('strokeColor');
};
Polygon.prototype.setStrokeWidth = function(width) {
  this.set('strokeWidth', width);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setStrokeWidth', this.getId(), width]);
};
Polygon.prototype.getStrokeWidth = function() {
  return this.get('strokeWidth');
};
Polygon.prototype.setVisible = function(visible) {
  visible = parseBoolean(visible);
  this.set('visible', visible);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setVisible', this.getId(), visible]);
};
Polygon.prototype.getVisible = function() {
  return this.get('visible');
};
Polygon.prototype.setGeodesic = function(geodesic) {
  geodesic = parseBoolean(geodesic);
  this.set('geodesic', geodesic);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setGeodesic', this.getId(), geodesic]);
};
Polygon.prototype.getGeodesic = function() {
  return this.get('geodesic');
};
Polygon.prototype.setZIndex = function(zIndex) {
  this.set('zIndex', zIndex);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setZIndex', this.getId(), zIndex]);
};
Polygon.prototype.getZIndex = function() {
  return this.get('zIndex');
};
Polygon.prototype.remove = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.remove', this.getId()]);
    this.off();
  };
 
  /*****************************************************************************
 * TileOverlay Class
 *****************************************************************************/
var TileOverlay = function(map, tileOverlayId, tileOverlayOptions) {
  BaseClass.apply(this);
  
  var self = this;
  Object.defineProperty(self, "id", {
    value: tileOverlayId,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "TileOverlay",
    writable: false
  });
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  var ignores = ["map", "id",  "type"];
  for (var key in tileOverlayOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, tileOverlayOptions[key]);
    }
  }
};

TileOverlay.prototype = new BaseClass();

TileOverlay.prototype.getMap = function() {
  return this.map;
};
TileOverlay.prototype.clearTileCache = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.clearTileCache', this.getId()]);
};
TileOverlay.prototype.getId = function() {
  return this.id;
};
TileOverlay.prototype.getZIndex = function() {
  return this.get("zIndex");
};
TileOverlay.prototype.setZIndex = function(zIndex) {
  this.set('zIndex', zIndex);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setZIndex', this.getId(), zIndex]);
};
TileOverlay.prototype.setFadeIn = function(fadeIn) {
  fadeIn = parseBoolean(fadeIn);
  this.set('fadeIn', fadeIn);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setFadeIn', this.getId(), fadeIn]);
};
TileOverlay.prototype.getFadeIn = function() {
  return this.get('fadeIn');
};
TileOverlay.prototype.setVisible = function(visible) {
  visible = parseBoolean(visible);
  this.set('visible', visible);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setVisible', this.getId(), visible]);
};
TileOverlay.prototype.getVisible = function() {
  return this.get('visible');
};
TileOverlay.prototype.remove = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.remove', this.getId()]);
  this.off();
};

/*****************************************************************************
 * GroundOverlay Class
 *****************************************************************************/
var GroundOverlay = function(map, groundOverlayId, groundOverlayOptions) {
  BaseClass.apply(this);
  
  var self = this;
  groundOverlayOptions.visible = groundOverlayOptions.visible === undefined ? true : groundOverlayOptions.visible;
  groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 0;
  groundOverlayOptions.opacity = groundOverlayOptions.opacity || 1;
  groundOverlayOptions.bounds = groundOverlayOptions.bounds || [];
  groundOverlayOptions.anchor = groundOverlayOptions.anchor || [0, 0];
  groundOverlayOptions.bearing = groundOverlayOptions.bearing || 0;
  Object.defineProperty(self, "id", {
    value: groundOverlayId,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "GroundOverlay",
    writable: false
  });
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  var ignores = ["map", "id",  "type"];
  for (var key in groundOverlayOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, groundOverlayOptions[key]);
    }
  }
};

GroundOverlay.prototype = new BaseClass();

GroundOverlay.prototype.getMap = function() {
  return this.map;
};
GroundOverlay.prototype.getId = function() {
  return this.id;
};
GroundOverlay.prototype.remove = function() {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.remove', this.getId()]);
  this.off();
};

GroundOverlay.prototype.setVisible = function(visible) {
  this.set('visible', visible);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setVisible', this.getId(), visible]);
};

GroundOverlay.prototype.getVisible = function() {
  return this.get('visible');
};

GroundOverlay.prototype.setImage = function(url) {
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setImage', this.getId(), url]);
};

GroundOverlay.prototype.setBounds = function(points) {
  this.set('bounds', points);
  var i,
      bounds = [];
  for (i = 0; i < points.length; i++) {
    bounds.push({
      "lat": points[i].lat,
      "lng": points[i].lng
    });
  }
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setBounds', this.getId(), bounds]);
};

GroundOverlay.prototype.getOpacity = function() {
  return this.get("opacity");
};

GroundOverlay.prototype.getBearing = function() {
  return this.get("bearing");
};

GroundOverlay.prototype.setOpacity = function(opacity) {
  this.set('opacity', opacity);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setOpacity', this.getId(), opacity]);
};
GroundOverlay.prototype.setBearing = function(bearing) {
  this.set('bearing', bearing);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setBearing', this.getId(), bearing]);
};

GroundOverlay.prototype.getZIndex = function() {
  return this.get("zIndex");
};
GroundOverlay.prototype.setZIndex = function(zIndex) {
  this.set('zIndex', zIndex);
  cordova.exec(null, this.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.setZIndex', this.getId(), zIndex]);
};
/*****************************************************************************
 * KmlOverlay Class
 *****************************************************************************/
var KmlOverlay = function(map, kmlOverlayId, kmlOverlayOptions) {
  BaseClass.apply(this);
  
  var self = this;
  self._objects = {};
  //self.set("visible", kmlOverlayOptions.visible === undefined ? true : kmlOverlayOptions.visible);
  //self.set("zIndex", kmlOverlayOptions.zIndex || 0);
  kmlOverlayOptions.animation = kmlOverlayOptions.animation === undefined ? true : kmlOverlayOptions.animation;
  kmlOverlayOptions.preserveViewport = kmlOverlayOptions.preserveViewport || false;
  Object.defineProperty(self, "id", {
    value: kmlOverlayId,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: "KmlOverlay",
    writable: false
  });
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  var ignores = ["map", "id",  "type"];
  for (var key in kmlOverlayOptions) {
    if (ignores.indexOf(key) === -1) {
      self.set(key, kmlOverlayOptions[key]);
    }
  }
};

KmlOverlay.prototype = new BaseClass();

KmlOverlay.prototype.getMap = function() {
  return this.map;
};
KmlOverlay.prototype.getId = function() {
  return this.id;
};
KmlOverlay.prototype.remove = function() {
  var layerId = this.id,
      self = this;
  
  this.trigger("_REMOVE");
  setTimeout(function() {
    delete KML_LAYERS[layerId];
    self.off();
  }, 1000);
};

/*****************************************************************************
 * LatLngBounds Class
 *****************************************************************************/
var LatLngBounds = function() {
  Object.defineProperty(this, "type", {
    value: "LatLngBounds",
    writable: false
  });
  
  var args = [];
  if (arguments.length === 1 &&
      typeof arguments[0] === "object" &&
      "push" in arguments[0]) {
    args = arguments[0];
  } else {
    args = Array.prototype.slice.call(arguments, 0);
  }
  for (var i = 0; i < args.length; i++) {
    if ("lat" in args[i] && "lng" in args[i]) {
      this.extend(args[i]);
    }
  }
};
  
LatLngBounds.prototype.northeast = null;
LatLngBounds.prototype.southwest = null;

LatLngBounds.prototype.toString = function() {
  return "[[" + this.southwest.toString() + "],[" + this.northeast.toString() + "]]";
};
LatLngBounds.prototype.toUrlValue = function(precision) {
  return "[[" + this.southwest.toUrlValue(precision) + "],[" + this.northeast.toUrlValue(precision) + "]]";
};

LatLngBounds.prototype.extend = function(latLng) {
  if ("lat" in latLng && "lng" in latLng) {
    if (!this.southwest && !this.northeast) {
      this.southwest = latLng;
      this.northeast = latLng;
    } else {
      var swLat = Math.min(latLng.lat, this.southwest.lat);
      var swLng = Math.min(latLng.lng, this.southwest.lng);
      var neLat = Math.max(latLng.lat, this.northeast.lat);
      var neLng = Math.max(latLng.lng, this.northeast.lng);
      
      delete this.southwest;
      delete this.northeast;
      this.southwest = new LatLng(swLat, swLng);
      this.northeast = new LatLng(neLat, neLng);
    }
    this[0] = this.southwest;
    this[1] = this.northeast;
  }
};
LatLngBounds.prototype.getCenter = function() {
  return new LatLng(
          (this.southwest.lat + this.northeast.lat) / 2,
          (this.southwest.lng + this.northeast.lng) / 2);
};

LatLngBounds.prototype.contains = function(latLng) {
  if (!("lat" in latLng) || !("lng" in latLng)) {
    return false;
  }
  return (latLng.lat >= this.southwest.lat) && (latLng.lat <= this.northeast.lat) &&
         (latLng.lng >= this.southwest.lng) && (latLng.lng <= this.northeast.lng);
};

/*****************************************************************************
 * Private functions
 *****************************************************************************/
//---------------------------
// Convert HTML color to RGB
//---------------------------
var colorDiv = document.createElement("div");
document.head.appendChild(colorDiv);
 
function HTMLColor2RGBA(colorStr) {
  if (colorStr === "transparent" || !colorStr) {
    return [0, 0, 0, 0];
  }
  var alpha = 255, //Math.floor(255 * 0.75),
      matches,
      compStyle,
      result = {
        r: 0,
        g: 0,
        b: 0
      };
  if (colorStr.match(/^#[0-9A-F]{4}$/i)) {
    alpha = colorStr.substr(4, 1);
    alpha = parseInt(alpha + alpha, 16);
    colorStr = colorStr.substr(0, 4);
  }
  
  if (colorStr.match(/^#[0-9A-F]{8}$/i)) {
    alpha = colorStr.substr(7, 2);
    alpha = parseInt(alpha, 16);
    colorStr = colorStr.substring(0, 7);
  }
  
  // convert rgba() -> rgb()
  if (colorStr.match(/^rgba\([\d,.\s]+\)$/)) {
    matches = colorStr.match(/([\d.]+)/g);
    alpha = Math.floor(parseFloat(matches.pop()) * 256);
    matches = "rgb(" +  matches.join(",") + ")";
  }
    
  // convert hsla() -> hsl()
  if (colorStr.match(/^hsla\([\d%,.\s]+\)$/)) {
    matches = colorStr.match(/([\d%.]+)/g);
    alpha = Math.floor(parseFloat(matches.pop()) * 256);
    matches = "hsl(" +  matches.join(",") + ")";
  }
   
  colorDiv.style.color = colorStr;
  if (window.getComputedStyle) {
    compStyle = window.getComputedStyle(colorDiv, null);
    try {
      var value = compStyle.getPropertyCSSValue ("color");
      var valueType = value.primitiveType;
      if (valueType === CSSPrimitiveValue.CSS_RGBCOLOR) {
        var rgb = value.getRGBColorValue ();
        result.r = rgb.red.getFloatValue (CSSPrimitiveValue.CSS_NUMBER);
        result.g = rgb.green.getFloatValue (CSSPrimitiveValue.CSS_NUMBER);
        result.b = rgb.blue.getFloatValue (CSSPrimitiveValue.CSS_NUMBER);
      }
    } catch (e) {
      console.log("The browser does not support the getPropertyCSSValue method!");
    }
  }
  return [result.r, result.g, result.b, alpha];
}

function parseBoolean(boolValue) {
  return typeof(boolValue) === "string" && boolValue.toLowerCase() === "true" ||
     boolValue === true ||
     boolValue === 1;
}

function isDom(element) {
  return !!element &&
         typeof element === "object" &&
         "getBoundingClientRect" in element;
}
function getDivSize(div) {
  if (!div) {
    return;
  }
  
  var pageWidth = window.innerWidth || 
                  document.documentElement.clientWidth ||
                  document.body.clientWidth,
      pageHeight = window.innerHeight ||
                   document.documentElement.clientHeight ||
                   document.body.clientHeight;

  var doc = document.documentElement;
  var pageLeft = (window.pageXOffset || doc.scrollLeft) - (doc.clientLeft || 0);
  var pageTop = (window.pageYOffset || doc.scrollTop)  - (doc.clientTop || 0);
  
  var rect = div.getBoundingClientRect();
  var divSize = {
    'left': rect.left + pageLeft,
    'top': rect.top + pageTop,
    'width': rect.width,
    'height': rect.height/*,
    'pageWidth': pageWidth,
    'pageHeight': pageHeight,
    'pageLeft': pageLeft,
    'pageTop': pageTop*/
  };
  divSize.width = divSize.width < pageWidth ? divSize.width : pageWidth;
  divSize.height = divSize.height < pageHeight ? divSize.height : pageHeight;
  
  return divSize;
}
function onMapResize(event) {
  var self = window.plugin.google.maps.Map;
  var div = self.get("div");
  if (!div) {
    return;
  }
  if (isDom(div) === false) {
    self.set("div", null);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'setDiv', []);
  } else {
    var args = [];
    var element, elements = [];
    var children = div.childNodes;
    var elemId;
    
    args.push(getDivSize(div));
    for (var i = 0; i < children.length; i++) {
      element = children[i];
      if (element.nodeType != 1) {
        continue;
      }
      elemId = element.getAttribute("__pluginDomId");
      if (!elemId) {
        elemId = "pgm" + Math.floor(Math.random() * Date.now()) + i;
        element.setAttribute("__pluginDomId", elemId);
      }
      elements.push({
        id: elemId,
        size: getDivSize(element)
      });
    }
    args.push(elements);
    cordova.exec(null, null, PLUGIN_NAME, 'resizeMap', args);
  }
  
}
/*****************************************************************************
 * External service
 *****************************************************************************/
var externalService = {};

externalService.launchNavigation = function(params) {
  var self = window.plugin.google.maps.Map;
  params = params || {};
  if (!params.from || !params.to) {
    return;
  }
  if (typeof params.from === "object" && "toUrlValue" in params.from ) {
    params.from = params.from.toUrlValue();
  }
  if (typeof params.to === "object" && "toUrlValue" in params.to ) {
    params.to = params.to.toUrlValue();
  }
  params.from = params.from.replace(/\s+/g, "%20");
  params.to = params.to.replace(/\s+/g, "%20");
  cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['External.launchNavigation', params]);
};
/*****************************************************************************
 * Geocoder class
 *****************************************************************************/
var Geocoder = {};

Geocoder.geocode = function(geocoderRequest, callback) {
  var self = this;
  geocoderRequest = geocoderRequest || {};
  
  if ("position" in geocoderRequest) {
    geocoderRequest.position.lat = geocoderRequest.position.lat || 0.0;
    geocoderRequest.position.lng = geocoderRequest.position.lng || 0.0;
  }
  var pluginExec = function() {
    cordova.exec(function(results) {
      if (typeof callback === "function") {
        callback.call(self,  results);
      }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Geocoder.createGeocoder', geocoderRequest]);
  };
  
  pluginExec();
};

/*****************************************************************************
 * Name space
 *****************************************************************************/
module.exports = {
  event : {
    MAP_CLICK: 'click',
    MAP_LONG_CLICK: 'long_click',
    MY_LOCATION_CHANGE: 'my_location_change', // for Android
    MY_LOCATION_BUTTON_CLICK: 'my_location_button_click',
    CAMERA_CHANGE: 'camera_change',
    CAMERA_IDLE: 'camera_idle', //for iOS
    MAP_READY: 'map_ready',
    MAP_LOADED: 'map_loaded', //for Android
    MAP_WILL_MOVE: 'will_move', //for iOS
    MAP_CLOSE: 'map_close',
    MARKER_CLICK: 'click',
    OVERLAY_CLICK: 'overlay_click',
    INFO_CLICK: 'info_click',
    MARKER_DRAG: 'drag',
    MARKER_DRAG_START: 'drag_start',
    MARKER_DRAG_END: 'drag_end'
  },
  
  BaseClass: BaseClass,
  Map: new App(),
  LatLng: LatLng,
  LatLngBounds: LatLngBounds,
  Marker: Marker,
  MapTypeId: {
    'NORMAL': 'MAP_TYPE_NORMAL',
    'ROADMAP': 'MAP_TYPE_NORMAL',
    'SATELLITE': 'MAP_TYPE_SATELLITE',
    'HYBRID': 'MAP_TYPE_HYBRID',
    'TERRAIN': 'MAP_TYPE_TERRAIN',
    'NONE': 'MAP_TYPE_NONE'
  },
  external: externalService,
  Geocoder: Geocoder
};

cordova.addConstructor(function() {
  if (!window.Cordova) {
    window.Cordova = cordova;
  };
  window.plugin = window.plugin || {};
  window.plugin.google = window.plugin.google || {};
  window.plugin.google.maps = window.plugin.google.maps || module.exports;
});
window.addEventListener("orientationchange", onMapResize);
document.addEventListener("deviceready", function() {
  var prevSize = null;
  var div, divSize;
  var sameCnt = 0;
  setInterval(function() {
    div = module.exports.Map.get("div");
    if (div) {
      divSize = getDivSize(div);
      if (prevSize) {
        if (divSize.left != prevSize.left ||
            divSize.top != prevSize.top ||
            divSize.width != prevSize.width ||
            divSize.height != prevSize.height ) {
          onMapResize();
        }
      }
      prevSize = divSize;
    }
  }, 100);
});
