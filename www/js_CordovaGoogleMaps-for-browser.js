

var utils = require('cordova/utils'),
  common = require('cordova-plugin-googlemaps.Common'),
  cordova_exec = require('cordova/exec'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass'),
  Map = require('cordova-plugin-googlemaps.Map'),
  StreetViewPanorama = require('cordova-plugin-googlemaps.StreetViewPanorama');

function CordovaGoogleMaps(execCmd) {
  var self = this;
  BaseClass.apply(this);

  self.execCmd = execCmd;

  // random unique number
  self.saltHash = Math.floor(Math.random() * Date.now());

  // Hold map instances.
  self.MAPS = {};
  self.MAP_CNT = 0;

}

utils.extend(CordovaGoogleMaps, BaseClass);

CordovaGoogleMaps.prototype.getMap = function(div, mapOptions) {
  var self = this;
  var args = Array.prototype.slice.call(arguments, 0);

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
  var mapId, elem, elemId;

  if (common.isDom(div)) {
    mapId = div.getAttribute("__pluginMapId");

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
    mapId = "map_" + self.MAP_CNT + "_" + self.saltHash;
  }
  // Create a map instance.
  var map = new Map(mapId, self.execCmd);

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
  var mapId = "streetview_" + self.MAP_CNT + "_" + self.saltHash;

  // Create a panorama instance.
  var panorama = new StreetViewPanorama(mapId, self.execCmd);

  // Catch all events for this map instance, then pass to the instance.
  // (Don't execute this native callback from your code)
  plugin.google.maps[mapId] = nativeCallback.bind(panorama);

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

  var div = map.getDiv();
  if (!div) {
    div = document.querySelector("[__pluginMapId='" + mapId + "']");
  }
  if (div) {
    div.removeAttribute('__pluginMapId');
  }

  self.MAPS[mapId].destroy();
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
  if (div.offsetWidth < 100 || div.offsetHeight < 100) {
    console.error('[GoogleMaps] Minimum container dimention is 100x100 in pixels.', div);
    return;
  }

  // If the mapDiv is specified,
  // the native side needs to know the map div position
  // before creating the map view.
  div.setAttribute("__pluginMapId", mapId);
  elemId = common.getPluginDomId(div);

  elem = div;
  var isCached, zIndexList = [];
  while(elem && elem.nodeType === Node.ELEMENT_NODE) {
    elemId = common.getPluginDomId(elem);
    if (common.shouldWatchByNative(elem)) {
      isCached = elemId in self.domPositions;
      self.domPositions[elemId] = {
        pointerEvents: common.getStyle(elem, 'pointer-events'),
        isMap: false,
        size: common.getDivRect(elem),
        zIndex: common.getZIndex(elem),
        children: [],
        overflowX: common.getStyle(elem, "overflow-x"),
        overflowY: common.getStyle(elem, "overflow-y"),
        containMapIDs: (isCached ? self.domPositions[elemId].containMapIDs : {})
      };
      zIndexList.unshift(self.domPositions[elemId].zIndex);
      self.domPositions[elemId].containMapIDs[mapId] = 1;
    } else {
      self.removeDomTree.call(self, element);
    }
    elem = elem.parentNode;
  }

  // Calculate the native view z-index
  var depth = 0;
  zIndexList.forEach(function(info, idx) {
    if (!info.isInherit && info.z === 0) {
      depth *= 10;
    }
    depth += (info.z + 1) / (1 << idx) + 0.01;
  });
  depth = Math.floor(depth * 10000);

  elemId = common.getPluginDomId(div);
  self.domPositions[elemId].isMap = true;

  var args = Array.prototype.slice.call(arguments, 0);
  args.unshift({
    id: mapId,
    depth: depth
  });

  cordova_exec(function() {
    panorama.getPanorama.apply(panorama, args);
  }, null, 'CordovaGoogleMaps', 'putHtmlElements', [self.domPositions]);


}

function postMapInit(map, div, options) {
  var self = this;
  var mapId = map.getId();
  var args = [];

  if (common.isDom(div)) {
    if (div.offsetWidth < 100 || div.offsetHeight < 100) {
      console.error('[GoogleMaps] Minimum container dimention is 100x100 in pixels.', div);
      return;
    }
    // If the mapDiv is specified,
    // the native side needs to know the map div position
    // before creating the map view.
    div.setAttribute("__pluginMapId", mapId);

    args.push({
      id: mapId,
      depth: 0
    });
    args.push(div);
    if (options) {
      args.push(options);
    }
    map.getMap.apply(map, args);
  } else {
    args.push({
      id: mapId,
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
