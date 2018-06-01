

var utils = require('cordova/utils');
var PluginMap = require('cordova-plugin-googlemaps.PluginMap'),
    event = require('cordova-plugin-googlemaps.event');

var MAP_CNT = 0;
var MAPS = {};
var saltHash = Math.floor(Math.random() * Date.now());
var getMapQueue = [];

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

    var secureStripeScript = document.createElement('script');
    //secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js?key=' + API_KEY_FOR_BROWSER);
    secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js');
    secureStripeScript.addEventListener("load", function() {
      API_LOADED = true;
      console.log("google maps api is loaded");

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


var CordovaGoogleMaps = {
  getMap: function(onSuccess, onError, args) {
    var mapId = args[0];
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
