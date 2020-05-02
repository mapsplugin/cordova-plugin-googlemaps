if (!cordova) {
  document.addEventListener('deviceready', function () {
    require('cordova/exec')(null, null, 'CordovaGoogleMaps', 'pause', []);
  }, {
    once: true
  });
} else {
  var common = require('./Common');

  cordova.addConstructor(function () {
    if (!window.Cordova) {
      window.Cordova = cordova;
    }
    window.plugin = window.plugin || {};
    window.plugin.google = window.plugin.google || {};
    window.plugin.google.maps = window.plugin.google.maps || module.exports;

    document.addEventListener('deviceready', function () {
      // workaround for issue on android-19: Cannot read property 'maps' of undefined
      if (!window.plugin) {
        console.warn('re-init window.plugin');
        window.plugin = window.plugin || {};
      }
      if (!window.plugin.google) {
        console.warn('re-init window.plugin.google');
        window.plugin.google = window.plugin.google || {};
      }
      if (!window.plugin.google.maps) {
        console.warn('re-init window.plugin.google.maps');
        window.plugin.google.maps = window.plugin.google.maps || module.exports;
      }

      cordova.exec(null, function (message) {
        alert(message);
      }, 'PluginEnvironment', 'isAvailable', ['']);
    }, {
      once: true
    });
  });

  var execCmd = require('./commandQueueExecutor');
  var cordovaGoogleMaps = new(require('./js_CordovaGoogleMaps'))(execCmd);

  (new Promise(function (resolve) {
    var wait = function () {
      if (document.body) {
        wait = undefined;
        cordovaGoogleMaps.trigger('start');
        resolve();
      } else {
        setTimeout(wait, 50);
      }
    };
    wait();

  })).then(function () {
    // The pluginInit.js must execute before loading HTML is completed.
    require('./pluginInit')();

    common.nextTick(function () {
      // If the developer needs to recalculate the DOM tree graph,
      // use `cordova.fireDocumentEvent('plugin_touch')`
      document.addEventListener('plugin_touch', cordovaGoogleMaps.invalidate.bind(cordovaGoogleMaps));

      // Repositioning 30 times when the device orientaion is changed.
      window.addEventListener('orientationchange', cordovaGoogleMaps.followMaps.bind(cordovaGoogleMaps, {
        target: document.body
      }));

      // If <body> is not ready yet, wait 25ms, then execute this function again.
      // if (!document.body || !document.body.firstChild) {
      //   common.nextTick(arguments.callee, 25);
      //   return;
      // }

      document.addEventListener('transitionstart', cordovaGoogleMaps.followMaps.bind(cordovaGoogleMaps), {
        capture: true
      });
      document.body.parentNode.addEventListener('transitionend', cordovaGoogleMaps.onTransitionEnd.bind(cordovaGoogleMaps), {
        capture: true
      });
      // document.body.addEventListener('transitionend', function(e) {
      //   if (!e.target.hasAttribute('__pluginDomId')) {
      //     return;
      //   }
      //   cordovaGoogleMaps.invalidateN(5);
      // }, true);

      // If the `scroll` event is ocurred on the observed element,
      // adjust the position and size of the map view
      document.body.parentNode.addEventListener('scroll', cordovaGoogleMaps.followMaps.bind(cordovaGoogleMaps), true);
      window.addEventListener('resize', function () {
        cordovaGoogleMaps.transforming = true;
        cordovaGoogleMaps.onTransitionFinish.call(cordovaGoogleMaps);
      }, true);

    });
  });

  /*****************************************************************************
   * Name space
   *****************************************************************************/
  /** @namespace plugin.google.maps */

  module.exports = {
    event: require('./event'),
    Animation: {
      BOUNCE: 'BOUNCE',
      DROP: 'DROP'
    },

    BaseClass: require('./BaseClass'),
    BaseArrayClass: require('./BaseArrayClass'),

    /** @namespace plugin.google.maps.Map */
    Map: {
      /**
       * @function getMap
       * @memberof plugin.google.maps.Map
       * @static
       */
      getMap: cordovaGoogleMaps.getMap.bind(cordovaGoogleMaps)
    },
    /** @namespace plugin.google.maps.StreetView */
    StreetView: {
      /**
       * @function getPanorama
       * @memberof plugin.google.maps.StreetView
       * @static
       */
      getPanorama: cordovaGoogleMaps.getPanorama.bind(cordovaGoogleMaps),
      /**
       * @readonly
       * @enum {string}
       * @memberof plugin.google.maps.StreetView
       * @static
       */
      Source: {
        /**
         * Search panorama inside and outdoor
         */
        DEFAULT: 'DEFAULT',

        /**
         * Search panorama inside and outdoor
         */
        OUTDOOR: 'OUTDOOR'
      }
    },
    HtmlInfoWindow: require('./HtmlInfoWindow'),
    LatLng: require('./LatLng'),
    LatLngBounds: require('./LatLngBounds'),
    MapTypeId: require('./MapTypeId'),
    environment: require('./Environment'),
    Geocoder: require('./Geocoder')(execCmd),
    LocationService: require('./LocationService')(execCmd),
    ElevationService: require('./ElevationService')(execCmd),
    DirectionsService: require('./DirectionsService')(execCmd),
    geometry: {
      encoding: require('./encoding'),
      spherical: require('./spherical'),
      poly: require('./poly')
    }
  };
}
