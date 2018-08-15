
var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

function PluginStreetViewPanorama(panoramaId, options, panoramaDivId) {
  var self = this;
  BaseClass.apply(this);
  var panoramaDiv = document.querySelector("[__pluginMapId='" + panoramaId + "']");
  var container = document.createElement("div");
  container.style.userSelect="none";
  container.style["-webkit-user-select"]="none";
  container.style["-moz-user-select"]="none";
  container.style["-ms-user-select"]="none";
  panoramaDiv.style.position = "relative";
  container.style.position = "absolute";
  container.style.top = 0;
  container.style.bottom = 0;
  container.style.right = 0;
  container.style.left = 0;
  panoramaDiv.insertBefore(container, panoramaDiv.firstElementChild);

  self.set("isGoogleReady", false);
  self.set("container", container);
  self.PLUGINS = {};

  Object.defineProperty(self, "id", {
    value: panoramaId,
    writable: false
  });

  self.one("googleready", function() {
    self.set("isGoogleReady", true);

    var service = new google.maps.StreetViewService();
    new Promise(function(resolve, reject) {
      if (options.camera) {
        var request = {};
        if (typeof options.camera.target === "string") {
          request.pano = options.camera.target;
        } else {
          request.location = options.camera.target;
          request.radius = options.camera.radius | 50;
          request.source = options.camera.source === "OUTDOOR" ?
            google.maps.StreetViewSource.OUTDOOR : google.maps.StreetViewSource.DEFAULT;
        }
        service.getPanorama(request, function(data, status) {
          if (status === google.maps.StreetViewStatus.OK) {
            resolve(data.location.pano);
          } else {
            reject();
          }
        });
      } else {
        resolve();
      }
    })
    .then(function(panoId) {

      var stOptions = {
        'addressControl': options.controls.streetNames,
        'showRoadLabels': options.controls.streetNames,
        'clickToGo': options.controls.navigation,
        'linksControl': options.controls.navigation,
        'panControl': options.gestures.panning,
        'zoomControl': options.gestures.zoom,
        'scrollwheel': options.gestures.zoom,
        'pano': panoId
      };
      if (options.camera) {
        if ('zoom' in options.camera) {
          stOptions.zoom = options.camera.zoom;
        }
        var pov;
        if ('tilt' in options.camera) {
          pov = {};
          pov.pitch = options.camera.tilt;
        }
        if ('bearing' in options.camera) {
          pov = pov | {};
          pov.heading = options.camera.bearing;
        }
        stOptions.pov = pov;
      }
      var panorama = new google.maps.StreetViewPanorama(container, stOptions);
      self.set('panorama', panorama);

      //google.maps.event.addListenerOnce(panorama, "pano_changed", function() {
        self.trigger(event.MAP_READY);
      //});
    });



  });
}

utils.extend(PluginStreetViewPanorama, BaseClass);

module.exports = PluginStreetViewPanorama;
