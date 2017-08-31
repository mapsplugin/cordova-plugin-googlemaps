
(function() {
  var utils = require('cordova/utils'),
    BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
    event = require('cordova-plugin-googlemaps.event');

  function PluginMap(args) {
    BaseClass.call(this);
    var self = this;
    var mapId = args[0];

    Object.defineProperty(this, "mapId", {
      value: mapId,
      writable: false
    });


    require('cordova/exec/proxy').add(mapId, this);


    self.set("isGoogleReady", false);

    this.one("googleready", function() {
      self.set("isGoogleReady", true);

      if (args.length === 3) {
        var mapOptions = args[1];
        var pluginId = args[2];
        var mapDiv = document.querySelector("[__plugindomid='" + pluginId + "']");
        var actualMapDiv = document.createElement("div");
        actualMapDiv.style.width = "100%";
        actualMapDiv.style.height = "100%";
        if (mapDiv.children.length > 0) {
          mapDiv.insertBefore(actualMapDiv, mapDiv.children[0]);
        } else {
          mapDiv.appendChild(actualMapDiv);
        }

        var gmap = new google.maps.Map(actualMapDiv, {
          mapTypeId: google.maps.MapTypeId.ROADMAP,
          center: {lat: 0, lng: 0},
          zoom: 0,
          noClear: true,
          disableDefaultUI: false
        });
        self.set("gmap", gmap);

        google.maps.event.addListenerOnce(gmap, "projection_changed", function() {
          self.trigger(event.MAP_READY);
        });
      }
    });

  }

  utils.extend(PluginMap, BaseClass);

  PluginMap.prototype.resizeMap = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setDiv = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setClickable = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setVisible = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.remove = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.getFocusedBuilding = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setCameraTarget = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setCameraTilt = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setCameraBearing = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.animateCamera = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.panBy = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setMyLocationEnabled = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.clear = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setIndoorEnabled = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setTrafficEnabled = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setCompassEnabled = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setMapTypeId = function(onSuccess, onError, args) {
    onSuccess();
  };

  PluginMap.prototype.toDataURL = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.fromLatLngToPoint = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.fromPointToLatLng = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setAllGesturesEnabled = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setPadding = function(onSuccess, onError, args) {
    onSuccess();
  };
  PluginMap.prototype.setActiveMarkerId = function(onSuccess, onError, args) {
    onSuccess();
  };

  module.exports = PluginMap;
})();
