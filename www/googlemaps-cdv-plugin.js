/* global cordova, plugin, CSSPrimitiveValue */
if (!cordova) {
  document.addEventListener("deviceready", function() {
    isSuspended = true;
    require('cordova/exec')(null, null, 'CordovaGoogleMaps', 'pause', []);
  }, {
    once: true
  });
} else {
  var common = require('./Common');

  // The pluginInit.js must execute before loading HTML is completed.
  require("./pluginInit")();

  (function() {
    // If <body> is not ready yet, wait 25ms, then execute this function again.
    if (!document.body || !document.body.firstChild) {
      common.nextTick(arguments.callee, 25);
      return;
    }
    var execCmd = require("./commandQueueExecutor");
    var cordovaGoogleMaps = new (require('./CordovaGoogleMaps'))(execCmd);

    // If the `transitionend` event is ocurred on the observed element,
    // adjust the position and size of the map view
    document.body.addEventListener("transitionend", function(e) {
      if (!e.target.hasAttribute("__pluginDomId")) {
        return;
      }
      cordovaGoogleMaps.invalidateN(5);
    }, true);

    // If the `scroll` event is ocurred on the observed element,
    // adjust the position and size of the map view
    var scrollEndTimer = null;
    document.body.addEventListener("scroll", function(e) {
      if (scrollEndTimer) {
        clearTimeout(scrollEndTimer);
      }
      scrollEndTimer = setTimeout(onScrollEnd, 100);
      cordovaGoogleMaps.followMapDivPositionOnly.call(cordovaGoogleMaps);
    }, true);

    function onScrollEnd() {
      cordovaGoogleMaps.invalidate.call(cordovaGoogleMaps, {force: true});
    }

    // When the app is initialized,
    // create the DOM hierarchy tree graph at once.
    document.addEventListener("deviceready", onScrollEnd, {
      once: true
    });

    // If the developer needs to recalculate the DOM tree graph,
    // use `cordova.fireDocumentEvent('plugin_touch')`
    document.addEventListener("plugin_touch", cordovaGoogleMaps.invalidate.bind(cordovaGoogleMaps));

    // Repositioning 30 times when the device orientaion is changed.
    window.addEventListener("orientationchange", function() {
      cordovaGoogleMaps.invalidateN(30);
    });


    /*****************************************************************************
     * Name space
     *****************************************************************************/
    module.exports = {
      event: require('./event'),
      Animation: {
          BOUNCE: 'BOUNCE',
          DROP: 'DROP'
      },

      BaseClass: require('./BaseClass'),
      BaseArrayClass: require('./BaseArrayClass'),
      Map: {
        getMap: cordovaGoogleMaps.getMap.bind(cordovaGoogleMaps)
      },
      HtmlInfoWindow: require('./HtmlInfoWindow'),
      LatLng: require('./LatLng'),
      LatLngBounds: require('./LatLngBounds'),
      MapTypeId: require('./MapTypeId'),
      environment: require('./Environment'),
      Geocoder: require('./Geocoder')(execCmd),
      LocationService: require('./LocationService')(execCmd),
      geometry: {
          encoding: require('./encoding'),
          spherical: require('./spherical'),
          poly: require('./poly')
      }
    };

  }());


  cordova.addConstructor(function() {
      if (!window.Cordova) {
          window.Cordova = cordova;
      }
      window.plugin = window.plugin || {};
      window.plugin.google = window.plugin.google || {};
      window.plugin.google.maps = window.plugin.google.maps || module.exports;
      document.addEventListener("deviceready", function() {
          // workaround for issue on android-19: Cannot read property 'maps' of undefined
          if (!window.plugin) { console.warn('re-init window.plugin'); window.plugin = window.plugin || {}; }
          if (!window.plugin.google) { console.warn('re-init window.plugin.google'); window.plugin.google = window.plugin.google || {}; }
          if (!window.plugin.google.maps) { console.warn('re-init window.plugin.google.maps'); window.plugin.google.maps = window.plugin.google.maps || module.exports; }

          // Check the Google Maps Android API v2 if the device platform is Android.
          if (/Android/i.test(window.navigator.userAgent)) {
              //------------------------------------------------------------------------
              // If Google Maps Android API v2 is not available,
              // display the warning alert.
              //------------------------------------------------------------------------
              cordova.exec(null, function(message) {
                  alert(message);
              }, 'Environment', 'isAvailable', ['']);
          }
      }, {
        once: true
      });
  });
}
