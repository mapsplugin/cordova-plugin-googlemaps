(function(window){
  const PLUGIN_NAME = 'GoogleMaps';
  var MARKERS = {};
 
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
    return self;
  };
  var App = function() {
    BaseClass.apply(this);
    Object.defineProperty(self, "type", {
      value: "Map",
      writable: false
    });
  };
  App.prototype = new BaseClass();
  
  
  App.prototype.errorHandler = function(msg) {
    if (!msg)
      return
    
    console.error(msg);
    if (this.trigger) {
      this.trigger('error', msg);
    } else {
      alert(msg);
    }
    return false;
  };
  
  /*
   * Callback from Native
   */
  App.prototype._onMarkerEvent = function(eventName, hashCode) {
    var marker = MARKERS[hashCode] || null;
    if (marker) {
      marker.trigger(eventName, marker, this);
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

  
  App.prototype.getMap = function(params) {
    params = params || {};
    var self = this;
    cordova.exec(function() {
      setTimeout(function() {
        self.trigger('map_ready', self);
      }, 100);
    }, self.errorHandler, PLUGIN_NAME, 'getMap', [params]);
    return self;
  };
  
  App.prototype.getLicenseInfo = function(callback) {
    cordova.exec(function(txt) {
      callback(null, txt);
    }, self.errorHandler, PLUGIN_NAME, 'getLicenseInfo');
  };
  App.prototype.showDialog = function() {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'showDialog', []);
  };
  
  
  App.prototype.setCenter = function(latLng) {
    this.set('center', latLng);
    cordova.exec(null, self.errorHandler,
      PLUGIN_NAME, 'exec', ['Map.setCenter', latLng.lat, latLng.lng]);
  };
  
  App.prototype.setZoom = function(zoom) {
    this.set('zoom', zoom);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setZoom', zoom]);
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
    if (mapTypeId != plugin.google.maps.MapTypeId[mapTypeId.replace("MAP_TYPE_", '')]) {
      return errorHandler("Invalid MapTypeId was specified.");
    }
    this.set('mapTypeId', mapTypeId);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setMapTypeId', mapTypeId]);
  };
 
  /**
   * @desc Change the map view angle
   * @param {Number} tilt  The angle
   */
  App.prototype.setTilt = function(tilt) {
    this.set('tilt', tilt);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setTilt', tilt]);
  };
 
  /**
   * @desc Open the map dialog
   */
  App.prototype.showDialog = function() {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'showDialog', []);
  };
 
 
  /**
   * @desc   Move the map camera with animation
   * @params {CameraPosition} cameraPosition New camera position
   * @params {Number} [durationMs = 1000] Animate duration
   * @params {Function} [callback] This callback is involved when the animation is completed.
   */
  App.prototype.animateCamera = function(cameraPosition, durationMs, callback) {
    var myCallback = null,
        self = this,
        params = ['Map.animateCamera'],
        lastParam;
 
    params.push(cameraPosition);
 
    myCallback = typeof durationMs == "function" ? durationMs : myCallback;
    myCallback = typeof callback == "function" ? callback : myCallback;
 
    if (typeof durationMs === "number") {
      params.push(durationMs);
    }
 
    cordova.exec(myCallback, self.errorHandler, PLUGIN_NAME, 'exec', params);
  };
  /**
   * @desc   Move the map camera without animation
   * @params {CameraPosition} cameraPosition New camera position
   * @params {Number} [durationMs = 1000] Animate duration
   * @params {Function} [callback] This callback is involved when the animation is completed.
   */
  App.prototype.moveCamera = function(cameraPosition, callback) {
    var argsLength = arguments.length;
    cordova.exec(function() {
      callback();
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.moveCamera', cameraPosition]);
  };
  
  App.prototype.setMyLocationEnabled = function(enabled) {
    enabled = parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setMyLocationEnabled', enabled]);
  };
  App.prototype.setIndoorEnabled = function(enabled) {
    enabled = parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setIndoorEnabled', enabled]);
  };
  App.prototype.setTrafficEnabled = function(enabled) {
    enabled = parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setTrafficEnabled', enabled]);
  };
  App.prototype.setCompassEnabled = function(enabled) {
    enabled = parseBoolean(enabled);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.setCompassEnabled', enabled]);
  };
  App.prototype.getMyLocation = function(callback) {
    cordova.exec(function(location) {
      if (typeof callback === "function") {
        location.latLng = new LatLng(location.latLng.lat, location.latLng.lng);
        callback(location);
      }
    
    }, self.errorHandler, PLUGIN_NAME, 'getMyLocation', []);
  };
 
  /**
   * Return the current position of the camera
   * @return {CameraPosition}
   */
  App.prototype.getCameraPosition = function(callback) {
    cordova.exec(function(camera) {
      if (typeof callback === "function") {
        camera.target = new LatLng(camera.target.lat, camera.target.lng);
        callback(camera);
      }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Map.getCameraPosition']);
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
    markerOptions.icon = markerOptions.icon || "";
    markerOptions.snippet = markerOptions.snippet || "";
    markerOptions.title = markerOptions.title || "";
    markerOptions.visible = markerOptions.visible || true;
    markerOptions.flat = markerOptions.flat || false;
    markerOptions.rotation = markerOptions.rotation || 0;
    markerOptions.opacity = parseFloat("" + markerOptions.opacity, 10) || 1;
 
    cordova.exec(function(result) {
      var marker = new Marker(result.id, markerOptions);
      markerOptions.hashCode = result.hashCode;
      MARKERS[result.id] = marker;
      
      if (typeof markerOptions.markerClick === "function") {
        marker.on(plugin.google.maps.event.MARKER_CLICK, markerOptions.markerClick);
      }
      if (typeof markerOptions.infoClick === "function") {
        marker.on(plugin.google.maps.event.INFO_CLICK, markerOptions.infoClick);
      }
      if (typeof callback === "function") {
        callback.call(window, marker, self);
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
    circleOptions.visible = circleOptions.visible || true;
    circleOptions.zIndex = circleOptions.zIndex || 0.0;
    circleOptions.radius = circleOptions.radius || 1;
 
    cordova.exec(function(circleId) {
      var circle = new Circle(circleId, circleOptions);
      if (callback) {
        callback.call(window, circle, self);
      }
    },self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.createCircle', circleOptions]);
  };
  //-------------
  // Polyline
  //-------------
  App.prototype.addPolyline = function(polylineOptions, callback) {
    var self = this;
    polylineOptions.points = polylineOptions.points || [];
    polylineOptions.color = HTMLColor2RGBA(polylineOptions.color || "#FF0000");
    polylineOptions.width = polylineOptions.width || 10;
    polylineOptions.visible = polylineOptions.visible || true;
    polylineOptions.zIndex = polylineOptions.zIndex || 0.0;
    polylineOptions.geodesic = polylineOptions.geodesic || false;
    
    cordova.exec(function(polylineId) {
      var polyline = new Polyline(polylineId, polylineOptions);
      if (callback) {
        callback.call(window, polyline, self);
      }
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.createPolyline', polylineOptions]);
  };
  //-------------
  // Polygon
  //-------------
  App.prototype.addPolygon = function(polygonOptions, callback) {
    var self = this;
    polygonOptions.points = polygonOptions.points || [];
    polygonOptions.strokeColor = HTMLColor2RGBA(polygonOptions.strokeColor || "#FF0000");
    polygonOptions.fillColor = HTMLColor2RGBA(polygonOptions.fillColor || "#000000");
    polygonOptions.strokeWidth = polygonOptions.strokeWidth || 10;
    polygonOptions.visible = polygonOptions.visible || true;
    polygonOptions.zIndex = polygonOptions.zIndex || 0.0;
    polygonOptions.geodesic = polygonOptions.geodesic || false;
    
    cordova.exec(function(polygonId) {
      var polygon = new Polygon(polygonId, polygonOptions);
      if (callback) {
        callback.call(window, polygon, self);
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
      return;
    }
    tilelayerOptions.visible = tilelayerOptions.visible || true;
    tilelayerOptions.zIndex = tilelayerOptions.zIndex || 0;
    tilelayerOptions.width = tilelayerOptions.width || 256;
    tilelayerOptions.height = tilelayerOptions.height || 256;
    
    cordova.exec(function(tileOverlayId) {
      var tileOverlay = new TileOverlay(tileOverlayId, tilelayerOptions);
      if (callback) {
        callback.call(window, tileOverlay, self);
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
    groundOverlayOptions.visible = groundOverlayOptions.visible || true;
    groundOverlayOptions.zIndex = groundOverlayOptions.zIndex || 0;
    groundOverlayOptions.bounds = groundOverlayOptions.bounds || [];
    
    var pluginExec = function() {
      cordova.exec(function(groundOverlayId) {
        var groundOverlay = new GroundOverlay(groundOverlayId, groundOverlayOptions);
        if (callback) {
          callback.call(window, groundOverlay, self);
        }
      }, self.errorHandler, PLUGIN_NAME, 'exec', ['GroundOverlay.createGroundOverlay', groundOverlayOptions]);
    };
    
    pluginExec();
    
    
  };
  //-------------
  // Geocoding
  //-------------
  App.prototype.geocode = function(geocoderRequest, callback) {
    var self = this;
    geocoderRequest = geocoderRequest || {};
    
    var pluginExec = function() {
      cordova.exec(function(results) {
        callback(results);
      }, self.errorHandler, PLUGIN_NAME, 'exec', ['Geocoder.createGeocoder', geocoderRequest]);
    };
    
    pluginExec();
  };
  /********************************************************************************
   * @name CameraPosition
   * @class This class represents new camera position
   * @property {LatLng} target The location where you want to show
   * @property {Number} [tilt] View angle
   * @property {Number} [zoom] Zoom level
   * @property {Number} [bearing] Map orientation
   *******************************************************************************/
  var CameraPosition = function(params) {
    var self = this;
    self.zoom = params.zoom;
    self.tilt = params.tilt;
    self.target = params.target;
    self.bearing = params.bearing;
    self.hashCode = params.hashCode;
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
  var Marker = function(id, markerOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("position", markerOptions.position);
    self.set("anchor", markerOptions.anchor);
    self.set("draggable", markerOptions.draggable);
    self.set("icon", markerOptions.icon);
    self.set("snippet", markerOptions.snippet);
    self.set("title", markerOptions.title);
    self.set("visible", markerOptions.visible);
    self.set("flat", markerOptions.flat);
    self.set("opacity", markerOptions.opacity);
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
  };
  Marker.prototype = new BaseClass();
  
  Marker.prototype.isVisible = function() {
    return this.get('visible');
  };

  
  Marker.prototype.getPosition = function(callback) {
    cordova.exec(function(latlng) {
      callback(new LatLng(latlng.lat, latlng.lng));
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.getPosition', this.getId()]);
  };
  Marker.prototype.getId = function() {
    return this.id;
  };
  Marker.prototype.getHashCode = function() {
    return this.hashCode;
  };
  
  Marker.prototype.remove = function(callback) {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.remove', this.getId()]);
  };
  Marker.prototype.setAlpha = function(alpha) {
    this.set('alpha');
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setAlpha', this.getId(), alpha]);
  };
  Marker.prototype.getAlpha = function() {
    return this.get('alpha');
  };
  Marker.prototype.setAnchor = function(anchorU, anchorV) {
    this.set('anchor', [anchorU, anchorV]);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setAnchor', this.getId(), anchorU, anchorV]);
  };
  Marker.prototype.setDraggable = function(draggable) {
    draggable = parseBoolean(draggable);
    this.set('draggable', draggable);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setDraggable', this.getId(), draggable]);
  };;
  Marker.prototype.isDraggable = function() {
    return this.get('draggable');
  };
  Marker.prototype.setFlat = function(flat) {
    flat = parseBoolean(flat);
    this.set('flat', flat);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setFlat', this.getId(), flat]);
  };
  Marker.prototype.setIcon = function(url) {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setIcon', this.getId(), url]);
  };
  Marker.prototype.setTitle = function(title) {
    this.set('title', title);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setTitle', this.getId(), title]);
  };
  Marker.prototype.setVisible = function(visible) {
    this.set('visible', visible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setVisible', this.getId(), visible]);
  };
  Marker.prototype.getTitle = function() {
    return this.get('title');
  };
  Marker.prototype.setSnippet = function(snippet) {
    this.set('snippet', snippet);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setSnippet', this.getId(), snippet]);
  };
  Marker.prototype.getSnippet = function() {
    return this.get('snippet');
  };
  Marker.prototype.setRotation = function(rotation) {
    this.set('rotation', rotation);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.setRotation', this.getId(), rotation]);
  };
  Marker.prototype.getRotation = function() {
    return this.get('rotation');
  };
  Marker.prototype.showInfoWindow = function() {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.showInfoWindow', this.getId()]);
  };
  Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.hideInfoWindow', this.getId()]);
  };
  Marker.prototype.isInfoWindowShown = function(callback) {
    cordova.exec(function(isVisible) {
      isVisible = parseparseBoolean(isVisible);
      callback(isVisible);
    }, self.errorHandler, PLUGIN_NAME, 'exec', ['Marker.isInfoWindowShown', this.getId()]);
  };
  Marker.prototype.isVisible = function() {
    return this.get("visible");
  };

  
  
  /*****************************************************************************
   * Circle Class
   *****************************************************************************/
  var Circle = function(circleId, circleOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("center", circleOptions.center);
    self.set("fillColor", circleOptions.fillColor);
    self.set("radius", circleOptions.radius);
    self.set("strokeColor", circleOptions.strokeColor);
    self.set("strokeWidth", circleOptions.strokeWidth);
    self.set("visible", circleOptions.visible);
    self.set("zIndex", circleOptions.zIndex);
    Object.defineProperty(self, "id", {
      value: circleId,
      writable: false
    });
    Object.defineProperty(self, "type", {
      value: "Circle",
      writable: false
    });
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
  Circle.prototype.getVisible = function() {
    return this.get('visible');
  };
  Circle.prototype.remove = function() {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.remove', this.getId()]);
  };
  Circle.prototype.setCenter = function(center) {
    this.set('center', center);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setCenter', this.getId(), center.lat, center.lng]);
  };
  Circle.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setFillColor', this.getId(), HTMLColor2RGBA(color)]);
  };
  Circle.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setStrokeColor', this.getId(), HTMLColor2RGBA(color)]);
  };
  Circle.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setStrokeWidth', this.getId(), width]);
  };
  Circle.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setVisible', this.getId(), visible]);
  };
  Circle.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setZIndex', this.getId(), zIndex]);
  };
  Circle.prototype.setRadius = function(radius) {
    this.set('radius', radius);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Circle.setRadius', this.getId(), radius]);
  };
  /*****************************************************************************
   * Polyline Class
   *****************************************************************************/
  var Polyline = function(polylineId, polylineOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("points", polylineOptions.points);
    self.set("color", polylineOptions.color);
    self.set("width", polylineOptions.width);
    self.set("visible", polylineOptions.visible);
    self.set("zIndex", polylineOptions.zIndex);
    self.set("geodesic", polylineOptions.geodesic);
    Object.defineProperty(self, "id", {
      value: polylineId,
      writable: false
    });
    Object.defineProperty(self, "type", {
      value: "Polyline",
      writable: false
    });
  };
  
  Polyline.prototype = new BaseClass();
  
  Polyline.prototype.getId = function() {
    return this.id;
  };

  Polyline.prototype.setPoints = function(points) {
    this.set('points', points);
    var i,
        path = [];
    for (i = 0; i < path.length; i++) {
      path.push({
        "lat": points[i].lat,
        "lng": points[i].lng
      });
    }
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setPoints', this.getId(), path]);
  };
  Polyline.prototype.getPoints = function() {
    return this.get("points");
  };
  Polyline.prototype.setColor = function(color) {
    this.set('color', color);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setColor', this.getId(), HTMLColor2RGBA(color)]);
  };
  Polyline.prototype.getColor = function() {
    return this.get('color');
  };
  Polyline.prototype.setWidth = function(width) {
    this.set('width', width);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setWidth', this.getId(), width]);
  };
  Polyline.prototype.getWidth = function() {
    return this.get('width');
  };
  Polyline.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setVisible', this.getId(), visible]);
  };
  Polyline.prototype.getVisible = function() {
    return this.get('visible');
  };
  Polyline.prototype.setGeodesic = function(geodesic) {
    geodesic = parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setGeodesic', this.getId(), geodesic]);
  };
  Polyline.prototype.getGeodesic = function() {
    return this.get('geodesic');
  };
  Polyline.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.setZIndex', this.getId(), zIndex]);
  };
  Polyline.prototype.getZIndex = function() {
    return this.get('zIndex');
  };
  Polyline.prototype.remove = function() {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.remove', this.getId()]);
  };

  /*****************************************************************************
   * Polygon Class
   *****************************************************************************/
  var Polygon = function(polygonId, polygonOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("points", polygonOptions.points);
    self.set("fillColor", polygonOptions.fillColor);
    self.set("strokeColor", polygonOptions.strokeColor);
    self.set("strokeWidth", polygonOptions.strokeWidth);
    self.set("visible", polygonOptions.visible);
    self.set("zIndex", polygonOptions.zIndex);
    self.set("geodesic", polygonOptions.geodesic);
    Object.defineProperty(self, "id", {
      value: polygonId,
      writable: false
    });
    Object.defineProperty(self, "type", {
      value: "Polygon",
      writable: false
    });
  };
  
  Polygon.prototype = new BaseClass();
  
  Polygon.prototype.getId = function() {
    return this.id;
  };
  Polygon.prototype.setPoints = function(points) {
    this.set('points', points);
    var i,
        path = [];
    for (i = 0; i < path.length; i++) {
      path.push({
        "lat": points[i].lat,
        "lng": points[i].lng
      });
    }
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setPoints', this.getId(), path]);
  };
  Polygon.prototype.getPoints = function() {
    return this.get("points");
  };
  Polygon.prototype.setFillColor = function(color) {
    this.set('fillColor', color);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setFillColor', this.getId(), HTMLColor2RGBA(color)]);
  };
  Polygon.prototype.getFillColor = function() {
    return this.get('fillColor');
  };
  Polygon.prototype.setStrokeColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setStrokeColor', this.getId(), HTMLColor2RGBA(color)]);
  };
  Polygon.prototype.getStrokeColor = function() {
    return this.get('strokeColor');
  };
  Polygon.prototype.setStrokeWidth = function(width) {
    this.set('strokeWidth', width);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setStrokeWidth', this.getId(), width]);
  };
  Polygon.prototype.getStrokeWidth = function() {
    return this.get('strokeWidth');
  };
  Polygon.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setVisible', this.getId(), visible]);
  };
  Polygon.prototype.getVisible = function() {
    return this.get('visible');
  };
  Polygon.prototype.setGeodesic = function(geodesic) {
    geodesic = parseBoolean(geodesic);
    this.set('geodesic', geodesic);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setGeodesic', this.getId(), geodesic]);
  };
  Polygon.prototype.getGeodesic = function() {
    return this.get('geodesic');
  };
  Polygon.prototype.setZIndex = function(zIndex) {
    this.set('zIndex', zIndex);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polygon.setZIndex', this.getId(), zIndex]);
  };
  Polyline.prototype.getZIndex = function() {
    return this.get('zIndex');
  };
  Polyline.prototype.remove = function() {
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['Polyline.remove', this.getId()]);
  };
 
  /*****************************************************************************
   * TileOverlay Class
   *****************************************************************************/
  var TileOverlay = function(tileOverlayId, tileOverlayOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("visible", tileOverlayOptions.visible);
    self.set("zIndex", tileOverlayOptions.zIndex);
    Object.defineProperty(self, "id", {
      value: tileOverlayId,
      writable: false
    });
    Object.defineProperty(self, "type", {
      value: "TileOverlay",
      writable: false
    });
  };
  
  TileOverlay.prototype = new BaseClass();
  
  TileOverlay.prototype.getId = function() {
    return this.id;
  };
  TileOverlay.prototype.getZIndex = function() {
    return this.zIndex;
  };
  TileOverlay.prototype.setZIndex = function(zIndex) {
    zIndex = parseBoolean(zIndex);
    this.set('zIndex', zIndex);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setZIndex', this.getId(), zIndex]);
  };
  TileOverlay.prototype.setVisible = function(visible) {
    visible = parseBoolean(visible);
    this.set('visible', visible);
    cordova.exec(null, self.errorHandler, PLUGIN_NAME, 'exec', ['TileOverlay.setVisible', this.getId(), visible]);
  };
  TileOverlay.prototype.getVisible = function() {
    return this.get('visible');
  };
  
  /*****************************************************************************
   * GroundOverlay Class
   *****************************************************************************/
  var GroundOverlay = function(groundOverlayId, groundOverlayOptions) {
    BaseClass.apply(this);
    
    var self = this;
    self.set("visible", groundOverlayOptions.visible || true);
    self.set("zIndex", groundOverlayOptions.zIndex || 0);
    self.set("opacity", groundOverlayOptions.opacity || 1);
    self.set("points", groundOverlayOptions.points || undefined);
    self.set("bounds", groundOverlayOptions.bounds || []);
    self.set("width", groundOverlayOptions.width || undefined);
    self.set("height", groundOverlayOptions.height || undefined);
    self.set("anchor", groundOverlayOptions.anchor || [0, 0]);
    self.set("bearing", groundOverlayOptions.bearing || 0);
    Object.defineProperty(self, "id", {
      value: groundOverlayId,
      writable: false
    });
    Object.defineProperty(self, "type", {
      value: "GroundOverlay",
      writable: false
    });
  };
  
  GroundOverlay.prototype = new BaseClass();
  
  GroundOverlay.prototype.getId = function() {
    return this.id;
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
    var alpha = Math.floor(255 * 0.75),
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
        if (valueType == CSSPrimitiveValue.CSS_RGBCOLOR) {
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
 
  /*****************************************************************************
   * Name space
   *****************************************************************************/
  window.plugin = window.plugin || {};
  window.plugin.google = window.plugin.google || {};
  window.plugin.google.maps = window.plugin.google.maps || {};
  window.plugin.google.maps.event = {
    MAP_CLICK: 'click',
    MAP_LONG_CLICK: 'long_click',
    MY_LOCATION_CHANGE: 'my_location_change', // for Android
    MY_LOCATION_BUTTON_CLICK: 'my_location_button_click',
    CAMERA_CHANGE: 'camera_change',
    MAP_READY: 'map_ready',
    MAP_LOADED: 'map_loaded', //for Android
    MAP_WILL_MOVE: 'will_move', //for iOS
    MARKER_CLICK: 'click',
    INFO_CLICK: 'info_click',
    MARKER_DRAG: 'drag',
    MARKER_DRAG_START: 'drag_start',
    MARKER_DRAG_END: 'drag_end'
  };
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
