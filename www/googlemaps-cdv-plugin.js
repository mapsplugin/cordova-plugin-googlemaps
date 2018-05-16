/* global cordova, plugin, CSSPrimitiveValue */
if (!cordova) {
  document.addEventListener("deviceready", function() {
    require('cordova/exec')(null, null, 'CordovaGoogleMaps', 'pause', []);
  }, {
    once: true
  });
} else {
  var common = require('./Common');
  // The pluginInit.js must execute before loading HTML is completed.
  require("./pluginInit")();

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

      cordova.exec(null, function(message) {
          alert(message);
      }, 'Environment', 'isAvailable', ['']);
    }, {
      once: true
    });
  });

  var execCmd = require("./commandQueueExecutor");
  var cordovaGoogleMaps = new (require('./CordovaGoogleMaps'))(execCmd);

  window.addEventListener("load", function() {
    common.nextTick(function() {
      // If the developer needs to recalculate the DOM tree graph,
      // use `cordova.fireDocumentEvent('plugin_touch')`
      document.addEventListener("plugin_touch", cordovaGoogleMaps.invalidate.bind(cordovaGoogleMaps));

      // Repositioning 30 times when the device orientaion is changed.
      window.addEventListener("orientationchange", followMaps);

      // If <body> is not ready yet, wait 25ms, then execute this function again.
      // if (!document.body || !document.body.firstChild) {
      //   common.nextTick(arguments.callee, 25);
      //   return;
      // }

      // If the `transitionend` event is ocurred on the observed element,
      // adjust the position and size of the map view
      var scrollEndTimer = null;
      var transitionEndTimer = null;
      var transformTargets = {};
      function followMaps(evt) {
        cordovaGoogleMaps.transforming = true;
        var changes = cordovaGoogleMaps.followMapDivPositionOnly.call(cordovaGoogleMaps);
        if (scrollEndTimer) {
          clearTimeout(scrollEndTimer);
          scrollEndTimer = null;
        }
        if (changes) {
          scrollEndTimer = setTimeout(followMaps.bind(this, evt), 100);
        } else {
          setTimeout(onTransitionEnd.bind(this, evt), 100);
        }
      }

      // CSS event `transitionend` is fired even the target dom element is still moving.
      // In order to detect "correct demention after the transform", wait until stable.
      function onTransitionEnd(evt) {
        if (!evt || !evt.target || !evt.target.hasAttribute ||!evt.target.hasAttribute("__pluginDomId")) {
          return;
        }
        var elemId = evt.target.getAttribute("__pluginDomId");
        transformTargets[elemId] = {left: -1, top: -1, right: -1, bottom: -1, finish: false, target: evt.target};
        if (!transitionEndTimer) {
          transitionEndTimer = setTimeout(detectTransitionFinish, 100);
        }
      }
      function detectTransitionFinish() {
        var keys = Object.keys(transformTargets);
        var onFilter = function(elemId) {
          if (transformTargets[elemId].finish) {
            return false;
          }

          var target = transformTargets[elemId].target;
          var divRect = common.getDivRect(target);
          var prevRect = transformTargets[elemId];
          if (divRect.left === prevRect.left &&
              divRect.top === prevRect.top &&
              divRect.right === prevRect.right &&
              divRect.bottom === prevRect.bottom) {
            transformTargets[elemId].finish = true;
          }
          transformTargets[elemId].left = divRect.left;
          transformTargets[elemId].top = divRect.top;
          transformTargets[elemId].right = divRect.right;
          transformTargets[elemId].bottom = divRect.bottom;
          return !transformTargets[elemId].finish;
        };
        var notYetTargets = keys.filter(onFilter);
        onFilter = null;

        if (transitionEndTimer) {
          clearTimeout(transitionEndTimer);
        }
        if (notYetTargets.length === 0) {
          clearTimeout(transitionEndTimer);
          transitionEndTimer = null;
          onTransitionFinish();
        } else {
          transitionEndTimer = setTimeout(detectTransitionFinish, 100);
        }
      }

      function onTransitionFinish() {
        if (!cordovaGoogleMaps.transforming) {
          return;
        }
        cordovaGoogleMaps.transforming = false;
        var changes = cordovaGoogleMaps.followMapDivPositionOnly.call(cordovaGoogleMaps);
        if (changes) {
          scrollEndTimer = setTimeout(onTransitionFinish, 100);
        } else {
          transformTargets = undefined;
          transformTargets = {};
          cordovaGoogleMaps.isThereAnyChange = true;
          cordovaGoogleMaps.checkRequested = false;
          cordovaGoogleMaps.putHtmlElements.call(cordovaGoogleMaps);
          //cordovaGoogleMaps.pause();
          scrollEndTimer = null;
        }
      }

      document.addEventListener("transitionstart", followMaps);
      document.body.addEventListener("transitionend", onTransitionEnd.bind({
        target: document.body
      }));
      // document.body.addEventListener("transitionend", function(e) {
      //   if (!e.target.hasAttribute("__pluginDomId")) {
      //     return;
      //   }
      //   cordovaGoogleMaps.invalidateN(5);
      // }, true);

      // If the `scroll` event is ocurred on the observed element,
      // adjust the position and size of the map view
      document.body.addEventListener("scroll", followMaps, true);

    });
  }, {
    once: true
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
    StreetView: {
      getPanorama: cordovaGoogleMaps.getPanorama.bind(cordovaGoogleMaps),
      Source: {
        DEFAULT: 'DEFAULT',
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
    geometry: {
        encoding: require('./encoding'),
        spherical: require('./spherical'),
        poly: require('./poly')
    }
  };
}
