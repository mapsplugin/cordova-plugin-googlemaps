

var utils = require('cordova/utils');
var PluginMap = require('cordova-plugin-googlemaps.PluginMap'),
    PluginStreetViewPanorama = require('cordova-plugin-googlemaps.PluginStreetViewPanorama'),
    event = require('cordova-plugin-googlemaps.event'),
    BaseClass = require('cordova-plugin-googlemaps.BaseClass');

var MAP_CNT = 0;
var MAPS = {};

var API_LOADED_STATUS = 0; // 0: not loaded, 1: loading, 2: completed

document.addEventListener("load_googlemaps", function() {
  API_LOADED_STATUS = 1;

  var confighelper = require("cordova/confighelper");

  var flag = false;
  confighelper.readConfig(function(configs) {
    if (flag) {
      return;
    }
    flag = true;
    // Get API key from config.xml
    var API_KEY_FOR_BROWSER = configs.getPreferenceValue("API_KEY_FOR_BROWSER");

    // API_LOADED = true;
    // var maps = Object.values(MAPS);
    // maps.forEach(function(map) {
    //   map.trigger("googleready");
    // });
    // return;

    var secureStripeScript = document.createElement('script');
    if (API_KEY_FOR_BROWSER) {
      secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js?key=' + API_KEY_FOR_BROWSER);
    } else {
      // for development only
      secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js');
    }
    secureStripeScript.addEventListener("load", function() {
      API_LOADED_STATUS = 2;

      var maps = Object.values(MAPS);
      maps.forEach(function(map) {
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

    switch(API_LOADED_STATUS) {
      case 0:
        cordova.fireDocumentEvent('load_googlemaps', []);
        break;
      case 2:
        pluginMap.trigger("googleready");
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
        var container = mapDiv.parentNode.removeChild(mapDiv);
        container = null;
        mapDiv = null;
        pluginMap.set('map', undefined);
      }
      map = null;
    }
    pluginMap.destroy();
    pluginMap = null;
    MAPS[mapId] = undefined;
    delete MAPS[mapId];
  },

  getPanorama: function(onSuccess, onError, args) {
    var meta = args[0],
      mapId = meta.id;
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

    switch(API_LOADED_STATUS) {
      case 0:
        cordova.fireDocumentEvent('load_googlemaps', []);
        break;
      case 2:
        pluginStreetView.trigger("googleready");
        break;
    }
  },
};

require('cordova/exec/proxy').add('CordovaGoogleMaps', CordovaGoogleMaps);
