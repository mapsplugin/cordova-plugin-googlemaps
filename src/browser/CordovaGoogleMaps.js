


var PluginMap = require('cordova-plugin-googlemaps.PluginMap'),
  PluginStreetViewPanorama = require('cordova-plugin-googlemaps.PluginStreetViewPanorama'),
  event = require('cordova-plugin-googlemaps.event'),
  Environment = require('cordova-plugin-googlemaps.PluginEnvironment');

var MAPS = {};

var API_LOADED_STATUS = 0; // 0: not loaded, 1: loading, 2: completed

document.addEventListener('load_googlemaps', function() {
  var envOptions = Environment._getEnv();
  var API_KEY_FOR_BROWSER;
  if (envOptions) {
    if (location.protocol === 'https:') {
      API_KEY_FOR_BROWSER = envOptions.API_KEY_FOR_BROWSER_RELEASE;
    } else {
      API_KEY_FOR_BROWSER = envOptions.API_KEY_FOR_BROWSER_DEBUG;
    }
  }
  API_LOADED_STATUS = 1;

  var secureStripeScript = document.createElement('script');
  if (API_KEY_FOR_BROWSER && API_KEY_FOR_BROWSER.length > 35) {
    secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js?key=' + API_KEY_FOR_BROWSER);
  } else {
    // for development only
    secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js');
  }
  secureStripeScript.addEventListener('load', function() {
    API_LOADED_STATUS = 2;

    var mKeys = Object.keys(MAPS);
    mKeys.forEach(function(mkey) {
      var map = MAPS[mkey];
      if (!map.get('isGoogleReady')) {
        map.trigger('googleready');
      }
    });
  });

  secureStripeScript.addEventListener('error', function(error) {
    console.log('Can not load the Google Maps JavaScript API v3');
    console.log(error);

    var mKeys = Object.keys(MAPS);
    mKeys.forEach(function(mkey) {
      var map = MAPS[mkey];
      if (map) {
        map.trigger('load_error');
      }
    });
  });

  document.getElementsByTagName('head')[0].appendChild(secureStripeScript);

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
    // memory cleanup
    var mapIDs = Object.keys(MAPS);
    mapIDs.forEach(function(mapId) {
      var eles = Array.from(document.querySelectorAll('*'));
      eles = eles.filter(function(e) {
        return e.__pluginMapId === mapId;
      });
      if (eles.length === 0) {
        if (MAPS[mapId]) {
          MAPS[mapId].destroy();
        }
        MAPS[mapId] = undefined;
        delete MAPS[mapId];
      }
    });

    var meta = args[0],
      mapId = meta.__pgmId;
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
    pluginMap.one('load_error', onError);

    // Does this app already load the google maps library?
    API_LOADED_STATUS = (window.google && window.google.maps) ? 2 : API_LOADED_STATUS;

    switch(API_LOADED_STATUS) {
    case 0:
      cordova.fireDocumentEvent('load_googlemaps', []);
      break;
    case 2:
      pluginMap.trigger('googleready');
      break;
    }
  },
  removeMap: function(onSuccess, onError, args) {
    var mapId = args[0];
    var pluginMap = MAPS[mapId];
    if (pluginMap) {
      var map = pluginMap.get('map');
      google.maps.event.clearInstanceListeners(map);
      var mapDiv = map.getDiv();
      if (mapDiv) {
        mapDiv.parentNode.removeChild(mapDiv);
        pluginMap.set('map', undefined);
      }
    }
    pluginMap.destroy();
    pluginMap = null;
    MAPS[mapId] = undefined;
    delete MAPS[mapId];
    onSuccess();
  },

  getPanorama: function(onSuccess, onError, args) {
    // memory cleanup
    var mapIDs = Object.keys(MAPS);
    mapIDs.forEach(function(mapId) {
      var eles = Array.from(document.querySelectorAll('*'));
      eles = eles.filter(function(e) {
        return e.__pluginMapId === mapId;
      });
      if (eles.length === 0) {
        if (MAPS[mapId]) {
          MAPS[mapId].destroy();
        }
        MAPS[mapId] = undefined;
        delete MAPS[mapId];
      }
    });

    var meta = args[0],
      mapId = meta.__pgmId;
    args[0] = mapId;
    args.unshift(this);

    var pluginStreetView = new (PluginStreetViewPanorama.bind.apply(PluginStreetViewPanorama, args));
    MAPS[mapId] = pluginStreetView;
    var dummyObj = {};
    var keys = Object.getOwnPropertyNames(PluginStreetViewPanorama.prototype).filter(function (p) {
      return typeof PluginStreetViewPanorama.prototype[p] === 'function';
    });
    keys.forEach(function(key) {
      dummyObj[key] = pluginStreetView[key].bind(pluginStreetView);
    });
    require('cordova/exec/proxy').add(mapId, dummyObj);

    pluginStreetView.one(event.PANORAMA_READY, onSuccess);
    pluginStreetView.one('load_error', onError);

    // Does this app already load the google maps library?
    API_LOADED_STATUS = (window.google && window.google.maps) ? 2 : API_LOADED_STATUS;

    switch(API_LOADED_STATUS) {
    case 0:
      cordova.fireDocumentEvent('load_googlemaps', []);
      break;
    case 2:
      pluginStreetView.trigger('googleready');
      break;
    }
  },
};

require('cordova/exec/proxy').add('CordovaGoogleMaps', CordovaGoogleMaps);
