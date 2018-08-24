var utils = require('cordova/utils');
var PluginMap = require('cordova-plugin-googlemaps.PluginMap'),
    PluginStreetViewPanorama = require('cordova-plugin-googlemaps.PluginStreetViewPanorama'),
    event = require('cordova-plugin-googlemaps.event'),
    BaseClass = require('cordova-plugin-googlemaps.BaseClass');

var MAP_CNT = 0;
var MAPS = {};

var API_LOADED_STATUS = 0; // 0: not loaded, 1: loading, 2: completed

// code: https://stackoverflow.com/q/32912732/697856
function createCORSRequest(method, url, asynch) {
  var xhr = new XMLHttpRequest();
  if ("withCredentials" in xhr) {
    // XHR for Chrome/Firefox/Opera/Safari.
    xhr.open(method, url, asynch);
    // xhr.setRequestHeader('MEDIBOX', 'login');
    xhr.setRequestHeader('Content-Type', 'application/xml; charset=UTF-8');
  } else if (typeof XDomainRequest != "undefined") {
    // XDomainRequest for IE.
    xhr = new XDomainRequest();
    xhr.open(method, url, asynch);
  } else {
    // CORS not supported.
    xhr = null;
  }
  return xhr;
}

document.addEventListener("load_googlemaps", function(evt) {
  var params = evt[0] || {};
  API_LOADED_STATUS = 1;

  (new Promise(function(resolve, reject) {
    if (params.API_KEY_FOR_BROWSER) {
      resolve("<variables name='API_KEY_FOR_BROWSER' value='" + params.API_KEY_FOR_BROWSER + "' >");
    } else {
      //-----------------
      // Read XML file
      //-----------------

      var link = document.createElement("a");
      link.href = './config.xml';
      var url = link.protocol+"//"+link.host+link.pathname;

      var xhr = createCORSRequest('GET', url, true);
      if (xhr) {
        xhr.onreadystatechange = function() {
          try {
            if (xhr.readyState === 4) {
              if (xhr.status === 200) {
                resolve(xhr.responseText);
              } else {
                resolve("");
              }
            }
          } catch (e) {
            resolve("");
          }
        };
        xhr.onerror = function(e) {
          resolve("");
        };
        xhr.send();
      }
    }
  }))
  .then(function(configFile) {
    var API_KEY_FOR_BROWSER = params.API_KEY_FOR_BROWSER || null;

    if (configFile.indexOf("API_KEY_FOR_BROWSER") > -1) {
      var matches = configFile.match(/name\s*?=\s*?[\"\']API_KEY_FOR_BROWSER[\"\'][^>]+>/i);
      if (matches) {
        var line = matches[0];
        matches = line.match(/value\s*?=\s*?[\"\'](.*?)[\"\']/i);
        if (matches) {
          API_KEY_FOR_BROWSER = matches[1];
        }
      }
    }

    var secureStripeScript = document.createElement('script');
    if (API_KEY_FOR_BROWSER) {
      secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js?key=' + API_KEY_FOR_BROWSER);
    } else {
      // for development only
      secureStripeScript.setAttribute('src','https://maps.googleapis.com/maps/api/js');
    }
    secureStripeScript.addEventListener("load", function() {
      API_LOADED_STATUS = 2;

      var mKeys = Object.keys(MAPS);
      mKeys.forEach(function(mkey) {
        var map = MAPS[mkey];
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
      mapId = meta.id,
      params = args[1];
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
        cordova.fireDocumentEvent('load_googlemaps', [params]);
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
      mapId = meta.id,
      params = args[1];
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
        cordova.fireDocumentEvent('load_googlemaps', [params]);
        break;
      case 2:
        pluginStreetView.trigger("googleready");
        break;
    }
  },
};

require('cordova/exec/proxy').add('CordovaGoogleMaps', CordovaGoogleMaps);
