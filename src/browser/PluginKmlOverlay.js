
var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  parser = require('cordova-plugin-googlemaps.fromXML');

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
    var rootElement = parser(result);
    console.log(rootElement);
    var kmlParser = new KmlParserClass();

    kmlParser.parseXml(rootElement["?xml"], "kml");
  })
  .catch(function(error) {
    console.error(error);
  });

};

module.exports = PluginKmlOverlay;

const KML_TAG = [
  "NOT_SUPPORTED",
  "kml",
  "style",
  "styleurl",
  "stylemap",
  "schema",
  "coordinates"
];

function KmlParserClass() {
  var self = this;
  self.styleHolder = {}
  self.schemaHolder = {};
}

KmlParserClass.prototype.parseXml = function(rootElement, tagName) {
  var result = {};
  var styleId, schemaId, txt, attrName;
  var i;
  tagName = tagName.toLowerCase();

  var childNode;
  var styles, schema, extendedData;
  var children = [];
  var styleIDs = [];

  console.log("--->tagName = " + tagName + "(" + rootElement + ")");
  result.tagName = tagName;

  var attributes = Object.keys(rootElement);
  attributes = attributes.filter(function(keyName) {
    return /^\@/.test(keyName);
  });
  attributes.forEach(function(attrName) {
    result[attrName] = rootElement[attrName];
  });

  switch (tagName) {
    case "styleurl":
      styleId = tbxml.textForElement(rootElement);
      result.putString("styleId", styleId);
      break;
    default:

  }
  console.log(result);
};


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
