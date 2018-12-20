
var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass');

function displayGrayMap(container) {
  var gmErrorContent = document.querySelector('.gm-err-container');
  var gmnoprint = document.querySelector('.gmnoprint');
  if (!gmErrorContent && !gmnoprint) {
    container.innerHTML = [
      '<div style="position: absolute; left: 0; top: 0; width: 100%; height: 100%; background-color:rgb(229, 227, 223)">',
      '<div style="text-align: center; position: absolute; left: 0; top: 0; bottom: 0; right: 0; width: 80%; height: 100px; margin: auto; color: #616161">',
      '<img src="https://maps.gstatic.com/mapfiles/api-3/images/icon_error.png"><br>',
      '<h3>Can not display panorama.<br>Check the developer console.</h3>',
      '</div>',
      '</div>'
    ].join('\n');
  }
}

function PluginStreetViewPanorama(panoramaId, options) {
  var self = this;
  BaseClass.apply(this);
  var panoramaDiv = document.querySelector('[__pluginMapId=\'' + panoramaId + '\']');
  panoramaDiv.style.backgroundColor = 'rgb(229, 227, 223)';
  var container = document.createElement('div');
  container.style.userSelect='none';
  container.style['-webkit-user-select']='none';
  container.style['-moz-user-select']='none';
  container.style['-ms-user-select']='none';
  panoramaDiv.style.position = 'relative';
  container.style.position = 'absolute';
  container.style.top = 0;
  container.style.bottom = 0;
  container.style.right = 0;
  container.style.left = 0;
  container.style.zIndex = 0;
  panoramaDiv.insertBefore(container, panoramaDiv.firstElementChild);

  self.set('isGoogleReady', false);
  self.set('container', container);
  self.PLUGINS = {};

  Object.defineProperty(self, '__pgmId', {
    value: panoramaId,
    writable: false
  });

  self.one('googleready', function() {
    self.set('isGoogleReady', true);

    var service = new google.maps.StreetViewService();
    self.set('service', service);
    new Promise(function(resolve, reject) {
      if (options.camera) {
        var request = {};
        if (typeof options.camera.target === 'string') {
          request.pano = options.camera.target;
        } else {
          request.location = options.camera.target;
          request.radius = options.camera.radius | 50;
          request.source = options.camera.source === 'OUTDOOR' ?
            google.maps.StreetViewSource.OUTDOOR : google.maps.StreetViewSource.DEFAULT;
        }
        var timeoutError = setTimeout(function() {
          self.trigger('load_error');
          displayGrayMap(panoramaDiv);
          reject();
        }, 3000);

        service.getPanorama(request, function(data, status) {
          clearTimeout(timeoutError);
          if (status === google.maps.StreetViewStatus.OK) {
            resolve(data.location.pano);
          } else {
            resolve(null);
          }
        });

      } else {
        resolve(null);
      }
    })
    .then(function(panoId) {

      var stOptions = {
        'addressControl': options.controls.streetNames,
        'showRoadLabels': options.controls.streetNames,
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
          pov = pov || {};
          pov.heading = options.camera.bearing;
        }
        if (pov) {
          stOptions.pov = pov;
        }
      }

      google.maps.event.addDomListener(container, 'click', function(evt) {
        var pov = panorama.getPov();
        var clickInfo = {
          'orientation': {
            'bearing': pov.heading,
            'tilt': pov.pitch
          },
          'point': [evt.clientX, evt.clientY]
        };
        if (self.__pgmId in plugin.google.maps) {
          plugin.google.maps[self.__pgmId]({
            'evtName': event.PANORAMA_CLICK,
            'callback': '_onPanoramaEvent',
            'args': [clickInfo]
          });
        }
      });
      var panorama = new google.maps.StreetViewPanorama(container, stOptions);
      self.set('panorama', panorama);

      self.trigger(event.PANORAMA_READY);
      panorama.addListener('position_changed', self._onPanoChangedEvent.bind(self, panorama));
      panorama.addListener('pov_changed', self._onCameraEvent.bind(self, panorama));
      panorama.addListener('zoom_changed', self._onCameraEvent.bind(self, panorama));

      if (!panoId) {
        self._onPanoChangedEvent(null);
      }
    });


  });
}

utils.extend(PluginStreetViewPanorama, BaseClass);

PluginStreetViewPanorama.prototype.setPanningGesturesEnabled = function(onSuccess, onError, args) {
  var self = this;
  var panorama = self.get('panorama');
  var boolValue = args[0] === true;

  if (panorama) {
    panorama.setOptions({
      'panControl': boolValue
    });
  }

  onSuccess();
};

PluginStreetViewPanorama.prototype.setZoomGesturesEnabled = function(onSuccess, onError, args) {
  var self = this;
  var panorama = self.get('panorama');
  var boolValue = args[0] === true;

  if (panorama) {
    panorama.setOptions({
      'zoomControl': boolValue,
      'scrollwheel': boolValue,
    });
  }

  onSuccess();
};


PluginStreetViewPanorama.prototype.setNavigationEnabled = function(onSuccess, onError, args) {
  var self = this;
  var panorama = self.get('panorama');
  var boolValue = args[0] === true;

  if (panorama) {
    panorama.setOptions({
      'linksControl': boolValue
    });
  }

  onSuccess();
};

PluginStreetViewPanorama.prototype.setStreetNamesEnabled = function(onSuccess, onError, args) {
  var self = this;
  var panorama = self.get('panorama');
  var boolValue = args[0] === true;

  if (panorama) {
    panorama.setOptions({
      'addressControl': boolValue,
      'showRoadLabels': boolValue
    });
  }

  onSuccess();
};
PluginStreetViewPanorama.prototype.setStreetNamesEnabled = function(onSuccess, onError, args) {
  var self = this;
  var panorama = self.get('panorama');
  var boolValue = args[0] === true;

  if (panorama) {
    panorama.setOptions({
      'visible': boolValue
    });
  }

  onSuccess();
};

PluginStreetViewPanorama.prototype.setPosition = function(onSuccess, onError, args) {
  var self = this;
  var panorama = self.get('panorama');
  var camera = args[0];

  if (panorama) {
    var service = self.get('service');

    new Promise(function(resolve, reject) {
      var request = {};
      if (typeof camera.target === 'string') {
        request.pano = camera.target;
      } else {
        request.location = camera.target;
        request.radius = camera.radius | 50;
        request.source = camera.source === 'OUTDOOR' ?
          google.maps.StreetViewSource.OUTDOOR : google.maps.StreetViewSource.DEFAULT;
      }
      var timeoutError = setTimeout(function() {
        reject('timeout error');
      }, 3000);

      service.getPanorama(request, function(data, status) {
        clearTimeout(timeoutError);
        if (status === google.maps.StreetViewStatus.OK) {
          resolve(data.location.pano);
        } else {
          resolve(null);
        }
      });
    })
    .then(function(panoId) {
      panorama.setPano(panoId);
      if (!panoId) {
        self._onPanoChangedEvent(null);
      }
      onSuccess();
    })
    .catch(onError);
  } else {
    onError('panorama has been already removed.');
  }

};

PluginStreetViewPanorama.prototype.setPov = function(onSuccess, onError, args) {
  var self = this;
  var panorama = self.get('panorama');
  var povRequest = args[0];

  if (panorama) {
    var options = {
      pov: panorama.getPov()
    };
    options.pov = options.pov || {
      heading: 0,
      pitch: 0
    };

    if ('bearing' in povRequest) {
      options.pov.heading = povRequest.bearing;
    }
    if ('tilt' in povRequest) {
      options.pov.pitch = povRequest.tilt;
    }
    if ('zoom' in povRequest) {
      options.zoom = povRequest.zoom;
    }
    panorama.setOptions(options);
  }

  onSuccess();
};


PluginStreetViewPanorama.prototype._onCameraEvent = function(panorama) {
  var self = this;
  var pov = panorama.getPov();
  var camera = {
    'bearing': pov.heading,
    'tilt': pov.pitch,
    'zoom': panorama.getZoom()
  };
  if (self.__pgmId in plugin.google.maps) {
    plugin.google.maps[self.__pgmId]({
      'evtName': event.PANORAMA_CAMERA_CHANGE,
      'callback': '_onPanoramaCameraChange',
      'args': [camera]
    });
  }
};
PluginStreetViewPanorama.prototype._onPanoChangedEvent = function(panorama) {
  var self = this;
  var locationInfo = null;

  if (panorama) {
    var location = panorama.getLocation();

    locationInfo = {
      'panoId': location.pano,
      'latLng': {
        'lat': location.latLng.lat(),
        'lng': location.latLng.lng()
      }
    };

    var links = panorama.getLinks() || [];
    if (links) {
      locationInfo.links = links.map(function(link) {
        return {
          'panoId': link.pano,
          'bearing': link.heading
        };
      });
    }
  }
  if (self.__pgmId in plugin.google.maps) {
    plugin.google.maps[self.__pgmId]({
      'evtName': event.PANORAMA_LOCATION_CHANGE,
      'callback': '_onPanoramaLocationChange',
      'args': [locationInfo]
    });
  }
};

module.exports = PluginStreetViewPanorama;
