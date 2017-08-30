
(function() {
  var utils = require('cordova/utils');
  var PluginMap = require('cordova-plugin-googlemaps.PluginMap');

  var MAP_CNT = 0;
  var MAPS = {};
  var saltHash = Math.floor(Math.random() * Date.now());
  var getMapQueue = [];

  var API_LOADED = false;
  var confighelper = require("cordova/confighelper");
  confighelper.readConfig(function(configs) {
    // Get API key from config.xml
    var API_KEY_FOR_BROWSER = configs.getPreferenceValue("API_KEY_FOR_BROWSER");

    var secureStripeScript = document.createElement('script');
    secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js?key=' + API_KEY_FOR_BROWSER);
    secureStripeScript.addEventListener("load", function() {
      API_LOADED = true;
      console.log("google maps api is loaded");

      var maps = Object.values(MAPS);
      maps.forEach(function(map) {
        map.trigger("googleready");
        console.log(map);
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


  var CordovaGoogleMaps = {
    getMap: function(onSuccess, onError, args) {
      console.log("HelloWorld", args);
      var mapId = args[0];
      var pluginMap = new PluginMap(args);
      MAPS[mapId] = pluginMap;
      console.log(pluginMap);

      onSuccess();
    }
  };

  require('cordova/exec/proxy').add('CordovaGoogleMaps', CordovaGoogleMaps);

})();
