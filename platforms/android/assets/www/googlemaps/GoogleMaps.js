(function(window){
  const SERVICE = 'GoogleMaps';
  var MARKERS = {};
  var CIRCLES = {};
  
  /**
   * Google Maps model.
   */
  var BaseClass = function() {
    var self = this;
    var _vars = {};
    var _listeners = {};
    
    self.get = function(key) {
      return key in _vars ? _vars[key] : null;
    };
    self.set = function(key, value) {
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
      var listener = document.addEventListener(eventName, function (e) {
        if (!e.myself || e.myself !== self) {
          return;
        }
        callback.apply(self, e.mydata);
      }, false);
      _listeners[eventName].push(listener);
    };
  };
  var App = function() {
    BaseClass.apply(this);
    this.type = "Map";
  };
  App.prototype = new BaseClass();
  
  
  var errorHandler = function(msg) {
    if (!msg)
      return
    
    alert(msg);
    App.trigger('error', msg);
    return false;
  };
  
  App.prototype.getMap = function() {
    var self = this;
    cordova.exec(function() {
      setTimeout(function() {
        self.trigger('map_ready', self);
      }, 100);
    }, errorHandler, SERVICE, 'GoogleMap_getMap', []);
    return self;
  };
  
  App.prototype.getLicenseInfo = function(callback) {
    cordova.exec(function(txt) {
      callback(null, txt);
    }, errorHandler, SERVICE, 'getLicenseInfo', []);
  };
  App.prototype.show = function(callback) {
    cordova.exec(null, errorHandler, SERVICE, 'show', []);
  };
  
  
  App.prototype.setCenter = function(latLng) {
    this.set('center', latLng);
    cordova.exec(null, errorHandler,
      SERVICE, 'GoogleMap_setCenter', [latLng.lat, latLng.lng]);
  };
  
  App.prototype.setZoom = function(zoom) {
    this.set('zoom', zoom);
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_setZoom', [zoom]);
  };
  App.prototype.setMapTypeId = function(mapTypeId) {
    if (mapTypeId != plugin.google.maps.MapTypeId[mapTypeId.replace("MAP_TYPE_", '')]) {
      return errorHandler("Invalid MapTypeId was specified.");
    }
    this.set('mapTypeId', mapTypeId);
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_setMapTypeId', [mapTypeId]);
  };
  App.prototype.setTilt = function(tilt) {
    this.set('tilt', tilt);
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_setTilt', [tilt]);
  };
  App.prototype.show = function() {
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_show', []);
  };
  App.prototype.animateCamera = function(cameraPosition, durationMs, callback) {
    var argsLength = arguments.length;
    if (cameraPosition.target) {
      cameraPosition.lat = cameraPosition.target.lat;
      cameraPosition.lng = cameraPosition.target.lng;
    }
    var params = [cameraPosition];
    if (argsLength === 3) {
      params.push(durationMs);
    }
    cordova.exec(function() {
      if (typeof callback === "function" && argsLength === 3) {
        callback();
      } else if (typeof durationMs === "function"  && argsLength === 2) {
        durationMs();
      }
    }, errorHandler, SERVICE, 'GoogleMap_animateCamera', params);
  };
  App.prototype.moveCamera = function(cameraPosition, callback) {
    var argsLength = arguments.length;
    if (cameraPosition.target) {
      cameraPosition.lat = cameraPosition.target.lat;
      cameraPosition.lng = cameraPosition.target.lng;
    }
    cordova.exec(function() {
      callback();
    }, errorHandler, SERVICE, 'GoogleMap_moveCamera', [cameraPosition]);
  };
  
  App.prototype.setMyLocationEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_setMyLocationEnabled', [enabled]);
  };
  App.prototype.setIndoorEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_setIndoorEnabled', [enabled]);
  };
  App.prototype.setTrafficEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_setTrafficEnabled', [enabled]);
  };
  App.prototype.setCompassEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, SERVICE, 'GoogleMap_setCompassEnabled', [enabled]);
  };
  //-------------
  // Marker
  //-------------
  App.prototype.addMarker = function(markerOptions, callback) {
    var self = this;
    markerOptions.lat = markerOptions.position.lat;
    markerOptions.lng = markerOptions.position.lng;
    
    cordova.exec(function(hashCode) {
      var marker = new Marker(hashCode, markerOptions);
      MARKERS[hashCode] = marker;
      
      if (typeof markerOptions.markerClick === "function") {
        marker.on('marker_click', markerOptions.markerClick);
      }
      if (typeof markerOptions.infoClick === "function") {
        marker.on('info_click', markerOptions.infoClick);
      }
      
      if (callback) {
        callback(marker);
      }
    }, errorHandler, SERVICE, 'GoogleMap_addMarker', [markerOptions]);
  };
  
  App.prototype._onMarkerClick = function(hashCode) {
    var marker = MARKERS[hashCode] || null;
    if (marker) {
      marker.trigger('marker_click', this, marker);
    }
  };
  
  App.prototype._onInfoWndClick = function(hashCode) {
    var marker = MARKERS[hashCode] || null;
    if (marker) {
      marker.trigger('info_click', this, marker);
    }
  };
  App.prototype._onMapClick = function(pointStr) {
    var point = pointStr.split(',');
    var latlng = new LatLng(parseFloat(point[0], 10), parseFloat(point[1], 10));
    console.log('--->_onMapClick');
    console.log(pointStr);
    this.trigger('click', latlng);
  };
  App.prototype._onMapLongClick = function(pointStr) {
    var point = pointStr.split(',');
    var latlng = new LatLng(parseFloat(point[0], 10), parseFloat(point[1], 10));
    this.trigger('long_click', latlng);
  };
  
  //-------------
  // Circle
  //-------------
  App.prototype.addCircle = function(circleOptions, callback) {
    var self = this;
    circleOptions.lat = circleOptions.center.lat;
    circleOptions.lng = circleOptions.center.lng;
    circleOptions.strokeColor = circleOptions.strokeColor;
    circleOptions.fillColor = circleOptions.fillColor;
    
    cordova.exec(function(circleId) {
      var circle = new Circle(circleId, circleOptions);
      CIRCLES[circleId] = circle;
      
      if (callback) {
        callback(circle);
      }
    }, errorHandler, SERVICE, 'GoogleMap_addCircle', [circleOptions]);
  };
  
  /**
   * LatLng Model
   */
  var LatLng = function(latitude, longitude) {
    var self = this;
    self.lat = parseFloat(latitude || 0, 10);
    self.lng = parseFloat(longitude || 0, 10);
    self.toString = function() {
      return self.lat.toFixed(6) + "," + self.lng.toFixed(6);
    };
  };
  
  /**
   * Marker Model
   */
  var Marker = function(hashCode, markerOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("position", markerOptions.position || null);
    self.set("anchor", markerOptions.anchor || [0.5, 0.5]);
    self.set("draggable", markerOptions.draggable || false);
    self.set("icon", markerOptions.icon || null);
    self.set("snippet", markerOptions.snippet || "");
    self.set("title", markerOptions.title || "");
    self.set("visible", markerOptions.visible || true);
    self.set("hashCode", hashCode);
    self.type = "Marker";
  };
  Marker.prototype = new BaseClass();
  
  Marker.prototype.getPosition = function(callback) {
    cordova.exec(function(latlng) {
      callback(new LatLng(latlng[0], latlng[1]));
    }, errorHandler, SERVICE, 'Marker_getPosition', [this.get("hashCode")]);
  };
  Marker.prototype.getTitle = function() {
    return this.get('title');
  };
  Marker.prototype.getSnippet = function() {
    return this.get('snippet');
  };
  Marker.prototype.hashCode = function() {
    return this.get('hashCode');
  };
  Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_hideInfoWindow', [this.get("hashCode")]);
  };
  Marker.prototype.isDraggable = function() {
    return this.get('draggable');
  };
  Marker.prototype.isInfoWindowShown = function(callback) {
    cordova.exec(function(isVisible) {
      callback(isVisible == 1);
    }, errorHandler, SERVICE, 'Marker_isInfoWindowShown', [this.get("hashCode")]);
  };
  Marker.prototype.isVisible = function() {
    return this.get('visible');
  };
  Marker.prototype.remove = function(callback) {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_remove', [this.get("hashCode")]);
  };
  Marker.prototype.setAnchor = function(anchorU, anchorV) {
    this.set('anchor', [anchorU, anchorV]);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setAnchor', [this.get("hashCode"), anchorU, anchorV]);
  };
  Marker.prototype.setDraggable = function(draggable) {
    draggable = Boolean(draggable);
    this.set('draggable', draggable);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setDraggable', [this.get("hashCode"), draggable]);
  };
  Marker.prototype.setIcon = function(url) {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setIcon', [this.get("hashCode"), url]);
  };
  Marker.prototype.setTitle = function(title) {
    this.set('title', title);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setTitle', [this.get("hashCode"), title]);
  };
  Marker.prototype.setSnippet = function(snippet) {
    this.set('snippet', snippet);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setSnippet', [this.get("hashCode"), snippet]);
  };
  Marker.prototype.showInfoWindow = function() {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_showInfoWindow', [this.get("hashCode")]);
  };
  Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_hideInfoWindow', [this.get("hashCode")]);
  };
  
  
  /**
   * Circle Model
   */
  var Circle = function(circleId, circleOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("center", circleOptions.center || null);
    self.set("fillColor", circleOptions.fillColor || "#00000000");
    self.set("radius", circleOptions.radius || 0);
    self.set("strokeColor", circleOptions.strokeColor || "#FF000000");
    self.set("strokeWidth", circleOptions.width || 10);
    self.set("visible", circleOptions.visible || true);
    self.set("zIndex", circleOptions.zIndex || 0.0);
    self.set("id", circleId);
    self.type = "Circle";
  };
  
  Circle.prototype = new BaseClass();
  
  Circle.prototype.getId = function() {
    return this.id;
  };
  Circle.prototype.getCenter = function(callback) {
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
  Circle.prototype.remove = function() {
    cordova.exec(null, errorHandler, SERVICE, 'Circle_remove', [this.get('id')]);
  };
  Circle.prototype.setCenter = function(center) {
    this.set('center', center);
    cordova.exec(null, errorHandler, SERVICE, 'Circle_setCenter', [this.get('id'), center.lat, center.lng]);
  };
  
  window.plugin = window.plugin || {};
  window.plugin.google = window.plugin.google || {};
  window.plugin.google.maps = window.plugin.google.maps || {};
  window.plugin.google.maps.Map = new App();
  window.plugin.google.maps.LatLng = LatLng;
  window.plugin.google.maps.Marker = Marker;
  window.plugin.google.maps.MapTypeId = {
    'NORMAL': 'MAP_TYPE_NORMAL',
    'ROADMAP': 'MAP_TYPE_NORMAL',
    'SATELLITE': 'MAP_TYPE_SATELLITE',
    'HYBRID': 'MAP_TYPE_HYBRID',
    'TERRAIN': 'MAP_TYPE_TERRAIN',
    'NONE': 'MAP_TYPE_NONE'
  };
  
})(window);
