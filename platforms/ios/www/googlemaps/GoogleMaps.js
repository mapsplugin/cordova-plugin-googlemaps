(function(window){
  const SERVICE = 'GoogleMaps';
  var MARKERS = new Backbone.Collection;
  var CIRCLES = new Backbone.Collection;
  
  /**
   * Google Maps model.
   */
  var App = Backbone.Model.extend({
    'initialize': function() {
      this.set({
        "hello": "world"
      });
    }
  });
  _.extend(App,Backbone.Events);
  
  var errorHandler = function(msg) {
    if (!msg)
      return
    
    alert(msg);
    App.trigger('error', msg);
    return false;
  }
  
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
      SERVICE, 'GoogleMap_setCenter', [latLng.lat(), latLng.lng()]);
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
      cameraPosition.lat = cameraPosition.target.lat();
      cameraPosition.lng = cameraPosition.target.lng();
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
      cameraPosition.lat = cameraPosition.target.lat();
      cameraPosition.lng = cameraPosition.target.lng();
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
  //-------------
  // Marker
  //-------------
  App.prototype.addMarker = function(markerOptions, callback) {
    var self = this;
    markerOptions.lat = markerOptions.position.lat();
    markerOptions.lng = markerOptions.position.lng();
    
    cordova.exec(function(result) {
      markerOptions.id = result.id
      markerOptions.hashCode = result.hashCode;
      var marker = new Marker(markerOptions);
      MARKERS.add(marker);
      
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
  
  App.prototype._onMarkerClick = function(markerId) {
    var marker = MARKERS.findWhere({
      'id': markerId
    });
    if (marker) {
      marker.trigger('marker_click', marker);
    }
  };
  
  App.prototype._onInfoWndClick = function(markerId) {
    var marker = MARKERS.findWhere({
      'id': markerId
    });
    if (marker) {
      marker.trigger('info_click', marker);
    }
  };
  App.prototype._onMapClick = function(pointStr) {
    var point = pointStr.split(',');
    var latlng = new LatLng(parseFloat(point[0], 10), parseFloat(point[1], 10));
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
    circleOptions.lat = circleOptions.center.lat();
    circleOptions.lng = circleOptions.center.lng();
    circleOptions.strokeColor = circleOptions.strokeColor;
    circleOptions.fillColor = circleOptions.fillColor;
    
    cordova.exec(function(result) {
      circleOptions.id = result.id
      circleOptions.hashCode = result.hashCode;
      var circle = new Circle(circleOptions);
      CIRCLES.add(circle);
      
      if (callback) {
        callback(circle);
      }
    }, errorHandler, SERVICE, 'GoogleMap_addCircle', [circleOptions]);
  };
  
  /**
   * LatLng Model
   */
  var LatLng = Backbone.Model.extend({
    'constructor' : function(lat, lng) {
      Backbone.Model.call(this, {
        'lat': lat,
        'lng': lng
      });
    }
  });
  LatLng.prototype.lat = function() {
    return this.get('lat');
  }
  LatLng.prototype.lng = function() {
    return this.get('lng');
  }
  LatLng.prototype.toString = function() {
    return this.get('lat').toFixed(6) + "," + this.get('lng').toFixed(6);
  }
  
  /**
   * Marker Model
   */
  var Marker = Backbone.Model.extend({
    'initialize': function(markerOptions) {
      this.set({
        'position': markerOptions.position || null,
        'anchor': markerOptions.anchor || [0.5, 0.5],
        'draggable' : markerOptions.draggable || false,
        'icon': markerOptions.icon || null,
        'snippet': markerOptions.snippet || "",
        'title': markerOptions.title || "",
        'visible': markerOptions.visible || true
      });
    }
  });
  _.extend(Marker, Backbone.Events);
  
  Marker.prototype.getId = function() {
    return this.get('id');
  };
  Marker.prototype.getPosition = function(callback) {
    cordova.exec(function(latlng) {
      callback(new LatLng(latlng[0], latlng[1]));
    }, errorHandler, SERVICE, 'Marker_getPosition', [this.id]);
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
    cordova.exec(null, errorHandler, SERVICE, 'Marker_hideInfoWindow', [this.id]);
  };
  Marker.prototype.isDraggable = function() {
    return this.get('draggable');
  };
  Marker.prototype.isInfoWindowShown = function(callback) {
    cordova.exec(function(isVisible) {
      callback(isVisible == 1);
    }, errorHandler, SERVICE, 'Marker_isInfoWindowShown', [this.id]);
  };
  Marker.prototype.isVisible = function() {
    return this.get('visible');
  };
  Marker.prototype.remove = function(callback) {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_remove', [this.id]);
  };
  Marker.prototype.setAnchor = function(anchorU, anchorV) {
    this.set('anchor', [anchorU, anchorV]);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setAnchor', [this.id, anchorU, anchorV]);
  };
  Marker.prototype.setDraggable = function(draggable) {
    draggable = Boolean(draggable);
    this.set('draggable', draggable);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setDraggable', [this.id, draggable]);
  };
  Marker.prototype.setIcon = function(url) {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setIcon', [this.id, url]);
  };
  Marker.prototype.setSnippet = function(snippet) {
    this.set('snippet', snippet);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setSnippet', [this.id, snippet]);
  };
  Marker.prototype.showInfoWindow = function() {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_showInfoWindow', [this.id]);
  };
  
  
  /**
   * Circle Model
   */
  var Circle = Backbone.Model.extend({
    'initialize': function(circleOptions) {
      this.set({
        'center': circleOptions.center || null,
        'fillColor': circleOptions.fillColor || "#00000000",
        'radius' : circleOptions.radius || 0,
        'strokeColor': circleOptions.strokeColor || "#FF000000",
        'strokeWidth': circleOptions.width || 10,
        'visible': circleOptions.visible || true,
        'zIndex': circleOptions.zIndex || 0.0
      });
    }
  });
  _.extend(Circle, Backbone.Events);
  
  Circle.prototype.getId = function() {
    return this.get('id');
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
    cordova.exec(null, errorHandler, SERVICE, 'Circle_remove', [this.id]);
  };
  Circle.prototype.setCenter = function(center) {
    this.set('center', center);
    cordova.exec(null, errorHandler, SERVICE, 'Circle_setCenter', [this.id, center.lat(), center.lng()]);
  };
  Marker.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    cordova.exec(null, errorHandler, SERVICE, 'Marker_setSnippet', [this.id, color]);
  };
  Marker.prototype.showInfoWindow = function() {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_showInfoWindow', [this.id]);
  };
  Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, errorHandler, SERVICE, 'Marker_hideInfoWindow', [this.id]);
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
  }
  
})(window);
