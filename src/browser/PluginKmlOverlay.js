

var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  fromXML = require('cordova-plugin-googlemaps.fromXML');

function PluginKmlOverlay(pluginMap) {
  var self = this;
  BaseClass.apply(self);
  Object.defineProperty(self, "pluginMap", {
    value: pluginMap,
    writable: false
  });
}

utils.extend(PluginKmlOverlay, BaseClass);

PluginKmlOverlay.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    map = self.pluginMap.get('map'),
    markerId = 'marker_' + args[2],
    pluginOptions = args[1];

  (new Promise(function(resolve, reject) {
    var xhr = createCORSRequest('GET', pluginOptions.url, true);
    if (xhr) {
      xhr.onreadystatechange = function() {
        try {
          if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
              resolve(xhr.responseText);
            } else {
              reject(xhr);
            }
          }
        } catch (e) {
          reject(e.description);
        }
      };
      xhr.send();
    }
  }))
  .then(function(result) {
    console.log(fromXML(result));
  })
  .catch(function(error) {
    console.error(error);
  });

};

module.exports = PluginKmlOverlay;

function createCORSRequest(method, url, asynch) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
        // XHR for Chrome/Firefox/Opera/Safari.
        xhr.open(method, url, true);
        // xhr.setRequestHeader('MEDIBOX', 'login');
        // xhr.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
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
