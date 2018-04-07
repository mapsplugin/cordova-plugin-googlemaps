var utils = require('cordova/utils'),
  common = require('./Common'),
  cordova_exec = require('cordova/exec'),
  BaseClass = require('./BaseClass'),
  Map = require('./Map'),
  StreetViewPanorama = require('./StreetViewPanorama');

function CordovaGoogleMaps(execCmd) {
  var self = this;
  BaseClass.apply(this);

  // Ignore checking for thse tags.
  self.doNotTraceTags = [
    "svg", "p", "pre", "script", "style"
  ];

  self.execCmd = execCmd;

  // random unique number
  self.saltHash = Math.floor(Math.random() * Date.now());

  // Hold map instances.
  self.MAPS = {};
  self.MAP_CNT = 0;

  // Hold the DOM hierarchy graph.
  self.domPositions = {};

  // True while the putHtmlElements is working.
  self.isChecking = false;

  // True if the code requests to execute the putHtmlElements while working it.
  self.checkRequested = false;

  // True if some elements are changed, such as added an element.
  self.isThereAnyChange = false;

  // Indicate the native timer is stopped or not.
  self.set("isSuspended", false);

  // Cache for updateMapPositionOnly
  self.prevMapRects = {};

  //------------------------------------------------------------------------------
  // Using MutationObserver, observe only added/removed or style changed elements
  //------------------------------------------------------------------------------
  var observer = new MutationObserver(function(mutations) {
    common.nextTick(function() {
      // Since Android 4.4 passes mutations as "Object", not "Array",
      // use "for" statement instead of "forEach" method.

      var i, mutation, node, j, elemId;
      for (j = 0; j < mutations.length; j++) {
        mutation = mutations[j];
        targetCnt = 0;
        if (mutation.type === "childList") {
          // If some elements are added, check them.
          if (mutation.addedNodes) {
            for (i = 0; i < mutation.addedNodes.length; i++) {
              node = mutation.addedNodes[i];
              if (node.nodeType !== Node.ELEMENT_NODE) {
                continue;
              }
              self.setDomId.call(self, node);
            }
          }

          // If some elements are removed from the DOM tree, remove their information.
          if (mutation.removedNodes) {
            for (i = 0; i < mutation.removedNodes.length; i++) {
              node = mutation.removedNodes[i];
              if (node.nodeType !== Node.ELEMENT_NODE || !node.hasAttribute("__pluginDomId")) {
                continue;
              }
              node._isRemoved = true;
              self.removeDomTree.call(self, node);
            }
          }
        } else {
          // Some attributes are changed.
          // If the element has __pluginDomId, check it.
          if (mutation.target.nodeType !== Node.ELEMENT_NODE) {
            return;
          }
          if (mutation.target.hasAttribute("__pluginDomId")) {
            self.traceDomTree.call(self, mutation.target, mutation.target.getAttribute("__pluginDomId"), false);
          }
        }

      }
      self.isThereAnyChange = true;
      common.nextTick(self.putHtmlElements.bind(self));
    });
  });
  observer.observe(document.body.parentElement, {
    attributes : true,
    childList: true,
    subtree: true,
    attributeFilter: ['style', 'class']
  });

  self.on("isSuspended_changed", function(oldValue, newValue) {
    if (newValue) {
      cordova_exec(null, null, 'CordovaGoogleMaps', 'pause', []);
    } else {
      cordova_exec(null, null, 'CordovaGoogleMaps', 'resume', []);
    }
  });
}

utils.extend(CordovaGoogleMaps, BaseClass);

CordovaGoogleMaps.prototype.traceDomTree = function(element, elemId, isMapChild) {
  //------------------------------------------
  // Create the DOM hierarchy graph
  //------------------------------------------
  var self = this;

  // If the root DOM element should be ignored,
  // remove it from the tree graph.
  if (self.doNotTraceTags.indexOf(element.tagName.toLowerCase()) > -1 ||
    !common.shouldWatchByNative(element)) {
    self.removeDomTree.call(self, element);
    return;
  }

  // Get the z-index CSS
  var zIndexProp = common.getZIndex(element);

  // Stores dom information
  var isCached = elemId in self.domPositions;
  self.domPositions[elemId] = {

    // If `pointer-events:none`, continue the hitTest process
    pointerEvents: common.getStyle(element, 'pointer-events'),

    // Only true if element is mapDiv
    isMap: element.hasAttribute("__pluginMapId"),

    // Calculate dom clickable region
    size: common.getDivRect(element),

    // Calculate actual z-index value
    zIndex: zIndexProp,

    // If `overflow: hidden or scroll`, the native side needs to recalculate actual size
    overflowX: common.getStyle(element, "overflow-x"),
    overflowY: common.getStyle(element, "overflow-y"),

    // Hold the elementId of child elements
    children: [],

    // Hold the list of map id.
    containMapIDs: (isCached ? self.domPositions[elemId].containMapIDs : {})
  };

  // Should this process continue to child elements?
  //   - condition 1:
  //      If the child element contains map, should continue.
  //
  //   - condition 2:
  //      If the element is one of the children of map div,
  //      check all elements, especially for HtmlInfoWindow.
  //
  //   - condition 3:
  //      If the pointer-css is "none", continue the proces,
  //      because the element might be a container of the map.
  //
  //   - condition 4:
  //      If the z-index is "inherit", there might be some elements.
  var containMapCnt = (Object.keys(self.domPositions[elemId].containMapIDs)).length;
  isMapChild = isMapChild || self.domPositions[elemId].isMap;
  if ((containMapCnt > 0 || isMapChild || self.domPositions[elemId].pointerEvents === "none" || zIndexProp.isInherit) && element.children.length > 0) {
    var child;
    for (var i = 0; i < element.children.length; i++) {
      child = element.children[i];
      if (self.doNotTraceTags.indexOf(child.tagName.toLowerCase()) > -1 ||
        !common.shouldWatchByNative(child)) {
        continue;
      }

      var childId = common.getPluginDomId(child);
      self.domPositions[elemId].children.push(childId);
      self.traceDomTree.call(self, child, childId, isMapChild);
    }
  }
};

CordovaGoogleMaps.prototype.setDomId = function(element) {
  //----------------------------------------------------------------------
  // This procedure generates unique ID
  // for all elements under the element, and the element itself.
  //----------------------------------------------------------------------
  common.getPluginDomId(element);
  if (element.children) {
    for (var i = 0; i < element.children.length; i++) {
      common.getPluginDomId(element.children[i]);
    }
  }
};

CordovaGoogleMaps.prototype.putHtmlElements = function() {
  //----------------------------------------------------------------------
  // This procedure generates a DOM hierarchy tree graph from <BODY>.
  //----------------------------------------------------------------------
  var self = this;

  // If this process is working, just checkRequested = true.
  // The putHtmlElements will execute itself if the flag is true.
  if (self.isChecking) {
    self.checkRequested = true;
    return;
  }
  self.checkRequested = false;

  // If no elements are changed after the last checking,
  // stop the native timer in order to save the battery usage.
  if (!self.isThereAnyChange) {
    self.pause();
    return;
  }


  self.isChecking = true;

  // If there is no visible or clickable map,
  // stop checking
  var mapIDs = Object.keys(self.MAPS);
  var touchableMapList = mapIDs.filter(function(mapId) {
    var map = self.MAPS[mapId];
    var isTouchable = (map &&
      map.getVisible() &&
      // map.getClickable() && <-- don't consider this.
      map.getDiv() &&
      common.shouldWatchByNative(map.getDiv()));
    if (isTouchable) {
      var elemId = common.getPluginDomId(map.getDiv());
      var domInfo = self.domPositions[elemId];
      isTouchable = domInfo.size.width * domInfo.size.height > 0;
    }
    map.set("__isAttached", isTouchable);
    return isTouchable;
  });
  if (touchableMapList.length === 0) {
    self.pause();
    return;
  }

  // If there is another check request,
  // DOM tree might be changed.
  // So let's start again.
  if (self.checkRequested) {
    setTimeout(function() {
      self.isChecking = false;
      common.nextTick(self.putHtmlElements.bind(self));
    }, 50);
    return;
  }

  // Since the native side needs to know the "latest" DOM information,
  // clear the DOM cache.
  common._clearInternalCache();

  // Generate the DOM hierarchy tree from <body> tag.
  common.getPluginDomId(document.body);
  self.traceDomTree.call(self, document.body, "root", false);

  // If the map div is not displayed (such as display='none'),
  // ignore the map temporally.
  var stopFlag = false;
  var mapElemIDs = mapIDs.map(function(mapId) {
    var div = self.MAPS[mapId].getDiv();
    if (!div || stopFlag) {
      return;
    }

    // Does this dom is really existed?
    var elemId = div.getAttribute("__pluginDomId");
    if (!elemId) {
      // The map div is removed
      if (mapId in self.MAPS) {
        self.MAPS[mapId].remove();
      }
      stopFlag = true;
      return;
    }

    if (!(elemId in self.domPositions)) {
      // Is the map div removed?
      var ele = document.querySelector("[__pluginMapId='" + mapId + "']");
      if (!ele) {
        // If no div element, remove the map.
        if (mapId in self.MAPS) {
          self.MAPS[mapId].remove();
        }
        stopFlag = true;
      }
      return;
    } else {
      return elemId;
    }

  });

  if (stopFlag) {
    // There is no map information (maybe timining?),
    // or the another check request is waiting,
    // start again.
    self.isThereAnyChange = true;
    setTimeout(function() {
      self.isChecking = false;
      common.nextTick(self.putHtmlElements.bind(self));
    }, 50);
    return;
  }

  //-----------------------------------------------------------------
  // Pass the DOM hierarchy tree graph to native side
  //-----------------------------------------------------------------
  self.resume();

  //console.log("--->putHtmlElements to native (start)", JSON.parse(JSON.stringify(self.domPositions)));
  cordova_exec(function() {
    //console.log("--->putHtmlElements to native (done)");

    // If there is another checking request, try again.
    if (self.checkRequested) {
      setTimeout(function() {
        self.isChecking = false;
        common.nextTick(self.putHtmlElements.bind(self));
      }, 50);
      return;
    }
    self.isChecking = false;
    self.pause();
  }, null, 'CordovaGoogleMaps', 'putHtmlElements', [self.domPositions]);
};


CordovaGoogleMaps.prototype.pause = function() {
  var self = this;

  self.set("isSuspended", true);
  self.isThereAnyChange = false;
  self.isChecking = false;
};

CordovaGoogleMaps.prototype.resume = function() {
  var self = this;
  self.set("isSuspended", false);
};


CordovaGoogleMaps.prototype.removeDomTree = function(node) {
  //----------------------------------------------------------------------------
  // This procedure removes the DOM tree graph from the specified element(node)
  //----------------------------------------------------------------------------
  var self = this;

  if (!node || !node.querySelectorAll) {
    return;
  }

  // Remove the information of children
  // which have the `__pluginDomId` attribute.
  var nodeList = node.querySelectorAll('[__pluginDomId]');
  var children = [];
  for (var i = 0; i < nodeList.length; i++) {
    children.push(nodeList[i]);
  }
  children.push(node);

  var isRemoved = node._isRemoved;
  children.forEach(function(child) {
    var elemId = child.getAttribute('__pluginDomId');

    // If the DOM is removed from the DOM tree,
    // remove the attribute.
    // (Note that the `_isRemoved` flag is set in MutationObserver.)
    if (isRemoved) {
      child.removeAttribute('__pluginDomId');

      // If map div, remove the map also.
      if (child.hasAttribute('__pluginMapId')) {
        mapId = child.getAttribute('__pluginMapId');
        if (mapId in self.MAPS) {
          self.MAPS[mapId].remove();
        }
      }
      delete self.domPositions[elemId];
    }
    common._removeCacheById(elemId);
  });

};

CordovaGoogleMaps.prototype.invalidate = function(opts) {
  //-------------------------------
  // Recheck the DOM positions
  //-------------------------------
  var self = this;
  //console.log("-->invalidate", JSON.parse(JSON.stringify(opts)));

  opts = opts || {};
  if (opts.force) {
    self.isThereAnyChange = true;
  }

  common.nextTick(function() {
    self.resume.call(self);
    self.putHtmlElements.call(self);
    if (opts.force) {
      self.followMapDivPositionOnly.call(self, opts);
    }
  });
};

CordovaGoogleMaps.prototype.followMapDivPositionOnly = function(opts) {
  //----------------------------------------------------------------------------
  // Follow the map div position and size only without the DOM check.
  // This is designed for scrolling.
  //----------------------------------------------------------------------------
  var self = this;

  opts = opts || {};
  var mapRects = {};
  var mapIDs = Object.keys(self.MAPS);
  var changed = false;
  mapIDs.forEach(function(mapId) {
    var map = self.MAPS[mapId];

    if (map &&
        map.getVisible() &&
        map.getDiv() &&
        common.shouldWatchByNative(map.getDiv())) {

      // Obtain only minimum information
      var mapDiv = map.getDiv();
      var divId = mapDiv.getAttribute("__pluginDomId");
      mapRects[divId] = {
        size: common.getDivRect(mapDiv),
        zIndex: common.getZIndex(mapDiv)
      };

      // Is the map moved?
      if (!changed && self.prevMapRects && (divId in self.prevMapRects) && (
        self.prevMapRects[divId].size.left !== mapRects[divId].size.left ||
        self.prevMapRects[divId].size.top !== mapRects[divId].size.top ||
        self.prevMapRects[divId].size.width !== mapRects[divId].size.width ||
        self.prevMapRects[divId].size.height !== mapRects[divId].size.height ||
        self.prevMapRects[divId].zIndex !== mapRects[divId].zIndex)) {
        changed = true;
      }
    }

  });
  self.prevMapRects = mapRects;

  // If changed, move the map views.
  if (changed || opts.force) {
    cordova_exec(null, null, 'CordovaGoogleMaps', 'updateMapPositionOnly', [mapRects]);
  }
};

CordovaGoogleMaps.prototype.invalidateN = function(cnt) {
  var self = this;
  if (self.cnt > 0) {
    return;
  }
  self.cnt = cnt;
  var timer = setInterval(function() {
    common.nextTick(function() {
      self.followMapDivPositionOnly.call(self);
      self.cnt--;
      if (self.cnt === 0) {
        clearInterval(timer);
        self.invalidate.call(self, {force: true});
      }
    });
  }, 50);
};


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
  var self = this;
  var mapId, elem, elemId;

  if (common.isDom(div)) {
    mapId = div.getAttribute("__pluginMapId");

    // If the specified div does not have any map,
    // add gray background-color until the map is displayed.
    if (!mapId && (!mapOptions || mapOptions.visible !== false)) {
      div.style.backgroundColor = "rgba(200, 200, 200, 0.5)";
    }

    // Wow, the app specifies the map div that has already another map,
    // but the app try to create new map.
    // In this case, remove the old map instance automatically.
    if (mapId && self.MAPS[mapId].getDiv() !== div) {
      elem = self.MAPS[mapId].getDiv();
      while(elem && elem.nodeType === Node.ELEMENT_NODE) {
        elemId = elem.getAttribute("__pluginDomId");
        if (elemId && elemId in self.domPositions) {
          self.domPositions[elemId].containMapIDs = self.domPositions[elemId].containMapIDs || {};
          delete self.domPositions[elemId].containMapIDs[mapId];
          if ((Object.keys(self.domPositions[elemId].containMapIDs).length) < 1) {
            delete self.domPositions[elemId];
          }
        }
        elem = elem.parentNode;
      }
      self.MAPS[mapId].remove();
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

  // Catch all events for this map instance, then pass to the instance.
  // (Don't execute this native callback from your code)
  plugin.google.maps[mapId] = nativeCallback.bind(map);

  map.on('__isAttached_changed', function(oldValue, newValue) {
    if (newValue) {
      cordova_exec(null, null, map.id, 'attachToWebView', []);
    } else {
      cordova_exec(null, null, map.id, 'detachFromWebView', []);
    }
  });

  // If the mapDiv is changed, clean up the information for old map div,
  // then add new information for new map div.
  map.on('div_changed', function(oldDiv, newDiv) {
    var elemId, ele;

    if (common.isDom(oldDiv)) {
      oldDiv.removeAttribute('__pluginMapId');
      ele = oldDiv;
      while(ele && ele != document.body.parentNode) {
        elemId = ele.getAttribute('__pluginDomId');
        if (elemId) {
          self.domPositions[elemId].containMapIDs = self.domPositions[elemId].containMapIDs || {};
          delete self.domPositions[elemId].containMapIDs[mapId];
          if ((Object.keys(self.domPositions[elemId].containMapIDs)).length < 1) {
            delete self.domPositions[elemId];
          }
        }
        ele.removeAttribute('__pluginDomId');
        if (ele.classList) {
          ele.classList.remove('_gmaps_cdv_');
        } else if (ele.className) {
          ele.className = ele.className.replace(/_gmaps_cdv_/g, "");
          ele.className = ele.className.replace(/\s+/g, " ");
        }
        ele = ele.parentNode;
      }
    }

    if (common.isDom(newDiv)) {

      elemId = common.getPluginDomId(newDiv);

      elem = newDiv;
      var isCached;
      while(elem && elem.nodeType === Node.ELEMENT_NODE) {
        elemId = common.getPluginDomId(elem);
        isCached = elemId in self.domPositions;
        self.domPositions[elemId] = {
          pointerEvents: common.getStyle(elem, 'pointer-events'),
          isMap: false,
          size: common.getDivRect(elem),
          zIndex: common.getZIndex(elem),
          children: (elemId in self.domPositions ? self.domPositions[elemId].children : []),
          overflowX: common.getStyle(elem, "overflow-x"),
          overflowY: common.getStyle(elem, "overflow-y"),
          containMapIDs: (isCached ? self.domPositions[elemId].containMapIDs : {})
        };
        self.domPositions[elemId].containMapIDs[mapId] = 1;
        elem = elem.parentNode;
      }

      elemId = common.getPluginDomId(newDiv);
      self.domPositions[elemId].isMap = true;
    }
  });

  // If the map is removed, clean up the information.
  map.one('remove', self._remove.bind(self, mapId));
  self.MAP_CNT++;
  self.MAPS[mapId] = map;
  self.isThereAnyChange = true;

  var args = Array.prototype.slice.call(arguments, 0);

  if (common.isDom(div)) {
    // If the mapDiv is specified,
    // the native side needs to know the map div position
    // before creating the map view.
    div.setAttribute("__pluginMapId", mapId);
    elemId = common.getPluginDomId(div);

    elem = div;
    var isCached;
    var zIndexList = [];
    while(elem && elem.nodeType === Node.ELEMENT_NODE) {
      elemId = common.getPluginDomId(elem);
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
    args.unshift({
      id: mapId,
      depth: depth
    });

    elemId = common.getPluginDomId(div);
    self.domPositions[elemId].isMap = true;

    cordova_exec(function() {
      self.resume();
      map.getMap.apply(map, args);
    }, null, 'CordovaGoogleMaps', 'putHtmlElements', [self.domPositions]);
  } else {
    args.unshift({
      id: mapId,
      depth: 0
    });
    map.getMap.apply(map, args);
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
  self.MAPS[mapId] = panorama;
  self.isThereAnyChange = true;

  // If the mapDiv is specified,
  // the native side needs to know the map div position
  // before creating the map view.
  div.setAttribute("__pluginMapId", mapId);
  elemId = common.getPluginDomId(div);

  elem = div;
  var isCached, zIndexList = [];
  while(elem && elem.nodeType === Node.ELEMENT_NODE) {
    elemId = common.getPluginDomId(elem);
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
    self.resume();
    panorama.getPanorama.apply(panorama, args);
  }, null, 'CordovaGoogleMaps', 'putHtmlElements', [self.domPositions]);


  panorama.one('remove', self._remove.bind(self, mapId));

  return panorama;
};

CordovaGoogleMaps.prototype._remove = function(mapId) {
  var self = this;
  console.log(self, mapId);
  delete plugin.google.maps[mapId];
  console.log(self.MAPS);
  var map = self.MAPS[mapId];

  var div = map.getDiv();
  if (!div) {
    div = document.querySelector("[__pluginMapId='" + mapId + "']");
  }
  if (div) {
    div.removeAttribute('__pluginMapId');
  }

  var keys = Object.keys(self.domPositions);
  keys.forEach(function(elemId) {
    self.domPositions[elemId].containMapIDs = self.domPositions[elemId].containMapIDs || {};
    delete self.domPositions[elemId].containMapIDs[mapId];
    if ((Object.keys(self.domPositions[elemId].containMapIDs)).length < 1) {
      delete self.domPositions[elemId];
    }
  });

  self.MAPS[mapId].destroy();
  delete self.MAPS[mapId];
  map = undefined;

  // If the app have no map, stop the native timer.
  if ((Object.keys(self.MAPS)).length === 0) {
    common._clearInternalCache();
    self.pause();
  }
};

function nativeCallback(params) {
  var args = params.args || [];
  args.unshift(params.evtName);
  this[params.callback].apply(this, args);
}


module.exports = CordovaGoogleMaps;
