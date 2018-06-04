


var utils = require('cordova/utils');
var PluginMap = require('cordova-plugin-googlemaps.PluginMap'),
    event = require('cordova-plugin-googlemaps.event'),
    BaseClass = require('cordova-plugin-googlemaps.BaseClass');

var MAP_CNT = 0;
var MAPS = {};

function StaticPoint(x, y) {
  this.x = x;
  this.y = y;
}
function StaticLatLng(lat, lng) {
  this.lat = lat;
  this.lng = lng;
}


// https://gist.github.com/korya/ed5b859f93347c12593f0e1158860d67

// Base tile size used in Google Javascript SDK
const GOOGLE_BASE_TILE_SIZE = 256;

// MercatorProjection implements the Projection interface defined in Google Maps
// Javascript SDK.
//
// Google Maps Javascript SDK docs:
// https://developers.google.com/maps/documentation/javascript/maptypes#WorldCoordinates
//
// For more details about the convertions see
// http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
class MercatorProjection {
  constructor(googleMaps) {
    this.googleMaps = googleMaps;
  }

  fromLatLngToPoint(coord) {
console.log(coord);
    let siny = Math.sin(coord.lat * Math.PI / 180);

    // Truncating to 0.9999 effectively limits latitude to 89.189. This is
    // about a third of a tile past the edge of the world tile.
    siny = Math.min(Math.max(siny, -0.9999), 0.9999);

    return new StaticPoint(
      GOOGLE_BASE_TILE_SIZE * (0.5 + coord.lng / 360),
      GOOGLE_BASE_TILE_SIZE * (0.5 - Math.log((1 + siny) / (1 - siny)) / (4 * Math.PI))
    );
  }

  fromPointToLatLng(point) {
    let n = Math.PI * (1 - 2 * point.y / GOOGLE_BASE_TILE_SIZE);

    return new StaticLatLng(
      Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))) * (180 / Math.PI),
      point.x / GOOGLE_BASE_TILE_SIZE * 360.0 - 180.0
    );
  }
}


function StaticMap(mapDiv, options) {
  var self = this;
  BaseClass.apply(this);
  var center = options.center;
  self.container = mapDiv;
  mapDiv.style.zIndex = -1;
  var img = new Image();
  mapDiv.appendChild(img);

  self.key = "AIzaSyDoGU-q2R8gI3C2YdOheaCLTKl8rHshV9A";

  self.projection = new MercatorProjection();


  self.set('zoom', options.zoom);
  self.set('center', options.center);
  self.set('mapTypeId', options.mapTypeId);
  var isOnce = false;

  var redraw = function() {
    var width = self.container.offsetWidth;
    var height = self.container.offsetHeight;
    self.set('width', width);
    self.set('height', height);

    var zoom = self.get('zoom'),
      center = self.get('center'),
      mapTypeId = self.get('mapTypeId');

    var centerPoint = self.projection.fromLatLngToPoint(center);
    var leftTop = self.projection.fromPointToLatLng(centerPoint.x - width / 2, centerPoint.x - height/2);
    //var rightTop = self.projection.fromPointToLatLng(centerPoint.x + width / 2, centerPoint.x - height/2);
    //var leftBottom = self.projection.fromPointToLatLng(centerPoint.x - width / 2, centerPoint.x + height/2);
    var rightBottom = self.projection.fromPointToLatLng(centerPoint.x + width / 2, centerPoint.x + height/2);


    self.set('bounds', {
      'northEast': leftTop,
      'southWest': rightBottom
    });

    img.onload = function() {
      if (!isOnce) {
        isOnce = true;
        self.trigger('projection_changed');
      }
      self.trigger('idle');
    };
    var gUrl = `https://maps.googleapis.com/maps/api/staticmap?key=${self.key}&center=${center.lat},${center.lng}&size=${width}x${height}&zoom=${zoom}&maptype=${mapTypeId}`;
    img.src = gUrl;

  };

  self.on('center_changed', redraw);
  self.on('zoom_changed', redraw);
  self.on('redraw', redraw);
  self.trigger('redraw');


}

utils.extend(StaticMap, BaseClass);

StaticMap.prototype.getTilt = function() {
  return this.get('tilt');
};
StaticMap.prototype.setTilt = function(tilt) {
  var self = this;
  self.set('tilt', tilt);
};
StaticMap.prototype.panBy = function(x, y) {
  var self = this;

  var centerPoint = self.projection.fromLatLngToPoint(self.get('center'));

  var nextLatLng = self.projection.fromPointToLatLng({
    'x': centerPoint.x + x,
    'y': centerPoint.y + y
  });

  self.set('center', nextLatLng);
};
StaticMap.prototype.panTo = function(center) {
  var self = this;
  self.set('center', center);
};
StaticMap.prototype.getCenter = function() {
  return this.get('center');
};
StaticMap.prototype.setCenter = function(center) {
  var self = this;
  self.set('center', center);
};
StaticMap.prototype.getZoom = function() {
  return this.get('zoom');
};
StaticMap.prototype.setZoom = function(zoom) {
  var self = this;
  self.set('zoom', zoom);
};
StaticMap.prototype.getMapTypeId = function() {
  return this.get('mapTypeId');
};
StaticMap.prototype.setMapTypeId = function(mapTypeId) {
  var self = this;
  self.set('mapTypeId', mapTypeId);
};
StaticMap.prototype.getBounds = function() {
  var self = this,
    bounds = self.get('bounds');

  return {
    'getNorthEast': function() {
      return bounds.northEast;
    },
    'getSouthWest': function() {
      return bounds.southWest;
    }
  };
};
StaticMap.prototype.getHeading = function() {
  return 0;
};
StaticMap.prototype.setHeading = function(heading) {
  var self = this;
  self.set('heading', heading);
};

StaticMap.prototype.addListener = function(evtName, callback) {
  this.addEventListener(evtName, callback);
};


function StaticMarker(options) {
  var self = this;
  BaseClass.apply(this);
  this.set('map', options.map);
  this.set('position', options.position);
}
utils.extend(StaticMarker, BaseClass);
StaticMarker.prototype.setPosition = function() {

};
StaticMarker.prototype.getMap = function() {
  return this.get('map');
};

StaticMarker.prototype.addListener = function(evtName, callback) {
  this.addEventListener(evtName, callback);
};

function StaticInfoWindow() {
  var self = this;
  BaseClass.apply(this);
}
utils.extend(StaticInfoWindow, BaseClass);

StaticInfoWindow.prototype.open = function() {

};

StaticInfoWindow.prototype.setOptions = function() {

};

StaticInfoWindow.prototype.addListener = function(evtName, callback) {
  this.addEventListener(evtName, callback);
};

window.google = {
  maps: {
    Map: StaticMap,
    Marker: StaticMarker,
    InfoWindow: StaticInfoWindow,
    MapTypeId: {
      ROADMAP: 'roadmap',
      SATELLITE: 'satellite',
      HYBRID: 'hybrid',
      TERRAIN: 'terrain',
      NONE: 'none'
    },
    Point: StaticPoint,
    LatLng: StaticLatLng,
    event: {
      addListenerOnce: function(target, evtName, callback) {
        target.one(evtName, callback);
      },
      addListener: function(target, evtName, callback) {
        target.on(evtName, callback);
      },
      trigger: function(target, evtName) {
        target.trigger(evtName);
      }
    }
  }
};


var API_LOADED = false;
document.addEventListener("load_googlemaps", function() {
  var confighelper = require("cordova/confighelper");

  var flag = false;
  confighelper.readConfig(function(configs) {
    if (flag) {
      return;
    }
    flag = true;
    // Get API key from config.xml
    var API_KEY_FOR_BROWSER = configs.getPreferenceValue("API_KEY_FOR_BROWSER");
    if (!API_KEY_FOR_BROWSER) {
      alert("Google Maps API key is required.");
      return;
    }

    API_LOADED = true;
    var maps = Object.values(MAPS);
    maps.forEach(function(map) {
      map.trigger("googleready");
    });
    return;

    var secureStripeScript = document.createElement('script');
    secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js?key=' + API_KEY_FOR_BROWSER);
    //secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js');
    secureStripeScript.addEventListener("load", function() {
      API_LOADED = true;

      var maps = Object.values(MAPS);
      maps.forEach(function(map) {
        console.log(map.get("isGoogleReady"));
        if (!map.get("isGoogleReady")) {
          map.trigger("googleready");
        }
      });
    }, {
      once: true
    });
    secureStripeScript.addEventListener("error", function(error) {
      console.log("Can not load the Google Maps JavaScript API v3");
      console.log(error);
    });
    document.getElementsByTagName('head')[0].appendChild(secureStripeScript);

  }, function(error) {
    console.log(error);
  });
}, {
  once: true
});

var stub = function(onSuccess) {
  onSuccess();
};

var CordovaGoogleMaps = {
  resume: stub,
  pause: stub,
  getMap: function(onSuccess, onError, args) {
    var meta = args[0],
      mapId = meta.id;
    args[0] = mapId;
    args.unshift(this);

    var pluginMap = new (PluginMap.bind.apply(PluginMap, args));
    MAPS[mapId] = pluginMap;
    var dummyObj = {};
    var keys = Object.getOwnPropertyNames(PluginMap.prototype).filter(function (p) {
      return typeof PluginMap.prototype[p] === 'function';
    });
    keys.forEach(function(key) {
      dummyObj[key] = pluginMap[key].bind(pluginMap);
    });
    require('cordova/exec/proxy').add(mapId, dummyObj);

    pluginMap.one(event.MAP_READY, onSuccess);

    if (API_LOADED) {
      pluginMap.trigger("googleready");
    } else {
      cordova.fireDocumentEvent('load_googlemaps', []);
    }
  }
};

require('cordova/exec/proxy').add('CordovaGoogleMaps', CordovaGoogleMaps);
