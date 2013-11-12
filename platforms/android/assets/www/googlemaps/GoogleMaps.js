(function(window){
  const PLUGIN_NAME = 'GoogleMaps';
  var MARKERS = {};
  
  /**
   * @name CameraPosition
   * @class This class represents new camera positino
   * @property {LatLng} target The location where you want to show
   * @property {Number} [tilt] View angle
   * @property {Number} [zoom] Zoom level
   * @property {Number} [bearing] Map orientation
   */
 
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
  
  /*
   * Callback from Native
   */
  App.prototype._onMarkerEvent = function(eventName, hashCode) {
    var marker = MARKERS[hashCode] || null;
    if (marker) {
      marker.trigger(eventName, this, marker);
    }
  };
  
  /**
   * Map events listener
   */
  App.prototype._onMapEvent = function(eventName, points) {
    if (points) {
      var latlng = new LatLng(points[0], points[1]);
      this.trigger(eventName, latlng);
    } else {
      this.trigger(eventName);
    }
  };
  /**
   * Callback from Native
   */
  App.prototype._onMyLocationChange = function(params) {
    var location = new Location(params);
    this.trigger('my_location_change', location);
  };
  /**
   * Callback from Native
   */
  App.prototype._onCameraChange = function(params) {
    var cameraPosition = new CameraPosition(params);
    this.trigger('camera_change', cameraPosition);
  };

  
  App.prototype.getMap = function(params) {
    params = params || {};
    var self = this;
    cordova.exec(function() {
      setTimeout(function() {
        self.trigger('map_ready', self);
      }, 100);
    }, errorHandler, PLUGIN_NAME, 'getMap', [params]);
    return self;
  };
  
  App.prototype.getLicenseInfo = function(callback) {
    cordova.exec(function(txt) {
      callback(null, txt);
    }, errorHandler, PLUGIN_NAME, 'getLicenseInfo');
  };
  App.prototype.show = function(callback) {
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'showMap', []);
  };
  
  
  App.prototype.setCenter = function(latLng) {
    this.set('center', latLng);
    cordova.exec(null, errorHandler,
      PLUGIN_NAME, 'exec', ['Map.setCenter', latLng.lat, latLng.lng]);
  };
  
  App.prototype.setZoom = function(zoom) {
    this.set('zoom', zoom);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Map.setZoom', zoom]);
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
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Map.setMapTypeId', mapTypeId]);
  };
 
  /**
   * @desc Change the map view angle
   * @param {Number} tilt  The angle
   */
  App.prototype.setTilt = function(tilt) {
    this.set('tilt', tilt);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Map.setTilt', tilt]);
  };
 
  /**
   * @desc Open the map dialog
   */
  App.prototype.show = function() {
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'showMap', []);
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
 
    if (cameraPosition.target) {
      cameraPosition.lat = cameraPosition.target.lat;
      cameraPosition.lng = cameraPosition.target.lng;
    }
    params.push(cameraPosition);
 
    myCallback = typeof durationMs == "function" ? durationMs : myCallback;
    myCallback = typeof callback == "function" ? callback : myCallback;
 
    if (typeof durationMs === "number") {
      params.push(durationMs);
    }
 
    cordova.exec(myCallback, errorHandler, PLUGIN_NAME, 'exec', params);
  };
  App.prototype.moveCamera = function(cameraPosition, callback) {
    var argsLength = arguments.length;
    if (cameraPosition.target) {
      cameraPosition.lat = cameraPosition.target.lat;
      cameraPosition.lng = cameraPosition.target.lng;
    }
    cordova.exec(function() {
      callback();
    }, errorHandler, PLUGIN_NAME, 'exec', ['Map.moveCamera', cameraPosition]);
  };
  
  App.prototype.setMyLocationEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Map.setMyLocationEnabled', enabled]);
  };
  App.prototype.setIndoorEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Map.setIndoorEnabled', enabled]);
  };
  App.prototype.setTrafficEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Map.setTrafficEnabled', enabled]);
  };
  App.prototype.setCompassEnabled = function(enabled) {
    enabled = Boolean(enabled);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Map.setCompassEnabled', enabled]);
  };
  //-------------
  // Marker
  //-------------
  App.prototype.addMarker = function(markerOptions, callback) {
    var self = this;
    markerOptions.lat = markerOptions.position.lat;
    markerOptions.lng = markerOptions.position.lng;
 
 
    markerOptions.position = markerOptions.position || null;
    markerOptions.anchor = markerOptions.anchor || [0.5, 0.5];
    markerOptions.draggable = markerOptions.draggable || false;
    markerOptions.icon = markerOptions.icon || "";
    markerOptions.snippet = markerOptions.snippet || "";
    markerOptions.title = markerOptions.title || "";
    markerOptions.visible = markerOptions.visible || true;
    markerOptions.flat = markerOptions.flat || false;
    markerOptions.rotation = markerOptions.rotation || 0;
    markerOptions.alpha = parseFloat("" + markerOptions.alpha, 10) || 1;
 
    cordova.exec(function(hashCode) {
      var marker = new Marker(hashCode, markerOptions);
      MARKERS[hashCode] = marker;
      

      if (typeof markerOptions.markerClick === "function") {
        marker.on(plugin.google.maps.event.MARKER_CLICK, markerOptions.markerClick);
      }
      if (typeof markerOptions.infoClick === "function") {
        marker.on(plugin.google.maps.event.INFO_CLICK, markerOptions.infoClick);
      }
      if (typeof callback === "function") {
        callback.call(marker, marker);
      }
    }, errorHandler, PLUGIN_NAME, 'exec', ['Marker.createMarker', markerOptions]);
  };
  
  
  //-------------
  // Circle
  //-------------
  App.prototype.addCircle = function(circleOptions, callback) {
    var self = this;
    circleOptions.lat = circleOptions.center.lat;
    circleOptions.lng = circleOptions.center.lng;
    circleOptions.strokeColor = HTMLColor2RGBA(circleOptions.strokeColor || "#FF0000");
    circleOptions.fillColor = HTMLColor2RGBA(circleOptions.fillColor || "#000000");
    circleOptions.strokeWidth = circleOptions.strokeWidth || 10;
    circleOptions.visible = circleOptions.visible || true;
    circleOptions.zIndex = circleOptions.zIndex || 0.0;
    circleOptions.radius = circleOptions.radius || 1;
 
    
    cordova.exec(function(circleId) {
      var circle = new Circle(circleId, circleOptions);
      
      if (callback) {
        callback(circle);
      }
    }, errorHandler, PLUGIN_NAME, 'exec', ['Circle.createCircle', circleOptions]);
  };
  
  /*****************************************************************************
   * CameraPosition Class
   *****************************************************************************/
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
    self.latLng = new LatLng(params.latitude, params.longitude);
    self.elapsedRealtimeNanos = params.elapsedRealtimeNanos;
    self.time = params.time;
    self.accuracy = params.accuracy || null;
    self.bearing = params.bearing || null;
    self.altitude = params.altitude || null;
    self.speed = params.speed || null;
    self.provider = params.provider;
    self.hashCode = params.hashCode;
  };
  /*****************************************************************************
   * Location Class
   *****************************************************************************/
  var LatLng = function(latitude, longitude) {
    var self = this;
    self.lat = parseFloat(latitude || 0, 10);
    self.lng = parseFloat(longitude || 0, 10);
    self.toString = function() {
      return self.lat.toFixed(6) + "," + self.lng.toFixed(6);
    };
  };
  
  /*****************************************************************************
   * Marker Class
   *****************************************************************************/
  var Marker = function(hashCode, markerOptions) {
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
    self.set("alpha", markerOptions.alpha);
    self.set("hashCode", hashCode);

    self.type = "Marker";
  };
  Marker.prototype = new BaseClass();
  
  
  Marker.prototype.getPosition = function(callback) {
    cordova.exec(function(latlng) {
      callback(new LatLng(latlng[0], latlng[1]));
    }, errorHandler, PLUGIN_NAME, 'exec', ['Marker.getPosition', this.get("hashCode")]);
  };
  Marker.prototype.getAlpha = function() {
    return this.get('alpha');
  };
  Marker.prototype.getTitle = function() {
    return this.get('title');
  };
  Marker.prototype.getSnippet = function() {
    return this.get('snippet');
  };
  Marker.prototype.getHashCode = function() {
    return this.get('hashCode');
  };
  Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.hideInfoWindow', this.get("hashCode")]);
  };
  Marker.prototype.isDraggable = function() {
    return this.get('draggable');
  };
  Marker.prototype.isInfoWindowShown = function(callback) {
    cordova.exec(function(isVisible) {
      callback(isVisible == 1);
    }, errorHandler, PLUGIN_NAME, 'exec', ['Marker.isInfoWindowShown', this.get("hashCode")]);
  };
  Marker.prototype.isVisible = function() {
    return this.get('visible');
  };
  Marker.prototype.remove = function(callback) {
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.remove', this.get("hashCode")]);
  };
  Marker.prototype.setAlpha = function(alpha) {
    this.set('alpha');
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.setAlpha', this.get("hashCode"), alpha]);
  };
  Marker.prototype.setAnchor = function(anchorU, anchorV) {
    this.set('anchor', [anchorU, anchorV]);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.setAnchor', this.get("hashCode"), anchorU, anchorV]);
  };
  Marker.prototype.setDraggable = function(draggable) {
    draggable = Boolean(draggable);
    this.set('draggable', draggable);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.setDraggable', this.get("hashCode"), draggable]);
  };
  Marker.prototype.setFlat = function(flat) {
    flat = Boolean(flat);
    this.set('flat', flat);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.setFlat', this.get("hashCode"), flat]);
  };
  Marker.prototype.setIcon = function(url) {
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.setIcon', this.get("hashCode"), url]);
  };
  Marker.prototype.setTitle = function(title) {
    this.set('title', title);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.setTitle', this.get("hashCode"), title]);
  };
  Marker.prototype.setSnippet = function(snippet) {
    this.set('snippet', snippet);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.setSnippet', this.get("hashCode"), snippet]);
  };
  Marker.prototype.showInfoWindow = function() {
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.showInfoWindow', this.get("hashCode")]);
  };
  Marker.prototype.hideInfoWindow = function() {
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Marker.hideInfoWindow', this.get("hashCode")]);
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
    self.set("strokeWidth", circleOptions.width);
    self.set("visible", circleOptions.visible);
    self.set("zIndex", circleOptions.zIndex);
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
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Circle.remove', this.get('id')]);
  };
  Circle.prototype.setCenter = function(center) {
    this.set('center', center);
    cordova.exec(null, errorHandler, PLUGIN_NAME, 'exec', ['Circle.setCenter', this.get('id'), center.lat, center.lng]);
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
    var alpha = 255,
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
 
  /*****************************************************************************
   * Name space
   *****************************************************************************/
  window.plugin = window.plugin || {};
  window.plugin.google = window.plugin.google || {};
  window.plugin.google.maps = window.plugin.google.maps || {};
  window.plugin.google.maps.event = {
    MAP_CLICK: 'click',
    MAP_LONG_CLICK: 'long_click',
    MY_LOCATION_CHANGE: 'my_location_change',
    MY_LOCATION_BUTTON_CLICK: 'my_location_button_click',
    CAMERA_CHANGE: 'camera_change',
    MAP_READY: 'map_ready',
    MAP_loaded: 'map_loaded',
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
