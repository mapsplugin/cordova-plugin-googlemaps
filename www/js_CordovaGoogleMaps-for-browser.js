

if (!window.Promise) {
  window.Promise = require('cordova-plugin-googlemaps.Promise');
}

var common = require('cordova-plugin-googlemaps.Common'),
  Map = require('cordova-plugin-googlemaps.Map'),
  StreetViewPanorama = require('cordova-plugin-googlemaps.StreetViewPanorama');


function nativeCallback(params) {
  var args = params.args || [];
  args.unshift(params.evtName);

  this[params.callback].apply(this, args);
}

function CordovaGoogleMaps(execCmd) {
  var self = this;

  self.execCmd = execCmd;

  // random unique number
  self.saltHash = Math.floor(Math.random() * Date.now());

  // Hold map instances.
  self.MAPS = {};
  self.MAP_CNT = 0;

  var removeMapDiv = function(node) {
    if (node.hasAttribute('__pluginmapid') && !node.parentNode) {
      var mapId = node.getAttribute('__pluginmapid');
      var map = self.MAPS[mapId];
      if (map) {
        map.remove();
        delete self.MAPS[mapId];
      }
    } else {
      var childNodes = Array.prototype.slice.call(node.childNodes);
      childNodes.forEach(function(child) {
        if (child.outerHTML && child.outerHTML.indexOf('__pluginmapid') > -1) {
          removeMapDiv(child);
        }
      });
    }
  };

  //------------------------------------------------------------------------------
  // Using MutationObserver, observe only added/removed or style changed elements
  //------------------------------------------------------------------------------
  var observer = new MutationObserver(function(mutations) {
    common.nextTick(function() {
      var mutationRecords = Array.prototype.slice.call(mutations, 0);
      mutationRecords.forEach(function(record) {
        if (record.removedNodes.length > 0) {
          record.removeNodes = Array.prototype.slice.call(record.removedNodes, 0);
          record.removeNodes.forEach(function(node) {
            if (node.outerHTML && node.outerHTML.indexOf('__pluginmapid') > -1) {
              removeMapDiv(node);
            }
          });
        }
      });
    });
  });
  observer.observe(document.body.parentElement, {
    childList: true,
    subtree: true
  });
}

CordovaGoogleMaps.prototype.getMap = function(div, mapOptions) {

  //----------------------------------------------------------------------------
  // This procedure return a map instance.
  //   - usage 1
  //       plugin.google.maps.Map.getMap(options?) returns a map instance.
  //
  //   - usage 2
  //       plugin.google.maps.Map.getMap(mapDiv, options?) returns a map instance.
  //       The generated map follows the mapDiv position and size automatically.
  //
  //   - usage 3 (not good way)
  //       In order to keep the backward compatibility for v1,
  //       if the mapDiv has already a map, returns the map instance for the map div.
  //----------------------------------------------------------------------------
  var self = this,
    mapId;

  if (common.isDom(div)) {
    mapId = div.getAttribute('__pluginMapId');

    // Wow, the app specifies the map div that has already another map,
    // but the app try to create new map.
    // In this case, remove the old map instance automatically.
    if (mapId && self.MAPS[mapId].getDiv() !== div) {
      var oldMap = self.MAPS[mapId];
      if (oldMap.infoWindowLayer) {
        var oldDiv = oldMap.getDiv();
        oldDiv.removeChild(oldMap.infoWindowLayer);
      }
      oldMap.remove();
      oldMap = undefined;
      mapId = undefined;
    }

    if (mapId && mapId in self.MAPS) {
      // Usage 3
      //    If the map div has already a map,
      //    return the map instance.
      return self.MAPS[mapId];
    }

  }
  if (!mapId) {
    mapId = 'map_' + self.MAP_CNT + '_' + self.saltHash;
  }
  // Create a map instance.
  var map = new Map(mapId, self.execCmd);
  window.plugin.google.maps[mapId] = nativeCallback.bind(map);

  // If the map is removed, clean up the information.
  map.one('remove', self._remove.bind(self, mapId));
  self.MAP_CNT++;
  self.isThereAnyChange = true;

  if (div instanceof Promise) {
    // This hack code for @ionic-native/google-maps
    div.then(function(params) {
      self.MAPS[mapId] = map;
      params = params || [];
      params.unshift(map);
      postMapInit.apply(self, params);
    });
  } else {
    // Normal code flow
    self.MAPS[mapId] = map;
    postMapInit.call(self, map, div, mapOptions);
  }

  return map;
};

CordovaGoogleMaps.prototype.getPanorama = function(div, streetViewOptions) {
  var self = this;
  var mapId = 'streetview_' + self.MAP_CNT + '_' + self.saltHash;

  // Create a panorama instance.
  var panorama = new StreetViewPanorama(mapId, self.execCmd);

  // Catch all events for this map instance, then pass to the instance.
  // (Don't execute this native callback from your code)
  window.plugin.google.maps[mapId] = nativeCallback.bind(panorama);

  self.MAP_CNT++;
  panorama.one('remove', self._remove.bind(self, mapId));
  if (div instanceof Promise) {
    // This hack code for @ionic-native/google-maps
    div.then(function(params) {
      self.MAPS[mapId] = panorama;
      params = params || [];
      params.unshift(panorama);
      postPanoramaInit.apply(self, params);
    });
  } else {
    // Normal code flow
    self.MAPS[mapId] = panorama;
    postPanoramaInit.call(self, panorama, div, streetViewOptions);
  }

  return panorama;
};

CordovaGoogleMaps.prototype._remove = function(mapId) {
  var self = this;
  var map = self.MAPS[mapId];
  if (map) {

    var div = map.getDiv();
    if (!div) {
      div = document.querySelector('[__pluginMapId="' + mapId + '"]');
    }
    if (div) {
      div.removeAttribute('__pluginMapId');
    }

    self.MAPS[mapId].destroy();
  }
  delete self.MAPS[mapId];
  map = undefined;
};

function postPanoramaInit(panorama, div, options) {
  var self = this;
  var mapId = panorama.getId();
  self.isThereAnyChange = true;

  if (!common.isDom(div)) {
    console.error('[GoogleMaps] You need to specify a dom element(such as <div>) for this method', div);
    return;
  }
  // If the given div is not fully ready, wait a little
  if (!common.shouldWatchByNative(div)) {
    setTimeout(function() {
      common.nextTick(postPanoramaInit.bind(self, panorama, div, options));
    }, 50);
    return;
  }
  if (div.offsetWidth < 100 || div.offsetHeight < 100) {
    console.error('[GoogleMaps] Minimum container dimention is 100x100 in pixels.', div);
    return;
  }

  // If the mapDiv is specified,
  // the native side needs to know the map div position
  // before creating the map view.
  div.setAttribute('__pluginMapId', mapId);

  if (div.offsetWidth < 100 || div.offsetHeight < 100) {
    console.error('[GoogleMaps] Minimum container dimention is 100x100 in pixels.', div);
    return;
  }
  var args = Array.prototype.slice.call(arguments, 0);
  args.unshift({
    __pgmId: mapId
  });

  // If the mapDiv is specified,
  // the native side needs to know the map div position
  // before creating the map view.
  div.setAttribute('__pluginMapId', mapId);

  panorama.getPanorama.apply(panorama, args);
}

function postMapInit(map, div, options) {
  var self = this;
  var mapId = map.getId();
  var args = [];

  if (common.isDom(div)) {
    // If the given div is not fully ready, wait a little
    if (!common.shouldWatchByNative(div)) {
      setTimeout(function() {
        common.nextTick(postMapInit.bind(self, map, div, options));
      }, 50);
      return;
    }
    if (div.offsetWidth < 100 || div.offsetHeight < 100) {
      console.error('[GoogleMaps] Minimum container dimention is 100x100 in pixels.', div);
      return;
    }
    // If the mapDiv is specified,
    // the native side needs to know the map div position
    // before creating the map view.
    div.setAttribute('__pluginMapId', mapId);

    args.push({
      __pgmId: mapId,
      depth: 0
    });
    args.push(div);
    if (options) {
      args.push(options);
    }
    map.getMap.apply(map, args);
  } else {
    args.push({
      __pgmId: mapId,
      depth: 0
    });
    args.push(null);
    if (options) {
      args.push(options);
    }
    map.getMap.apply(map, args);
  }
}


module.exports = CordovaGoogleMaps;
