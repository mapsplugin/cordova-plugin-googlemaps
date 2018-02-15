var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    event = require('cordova-plugin-googlemaps.event'),
    common = require('cordova-plugin-googlemaps.Common');

var Map = require('cordova-plugin-googlemaps.Map'),
  BaseClass = require('cordova-plugin-googlemaps.BaseClass');

var cordova_exec = require('cordova/exec');

var commandQueue = [];
var _isWaitMethod = null;
var _isExecuting = false;
var _executingCnt = 0;
var MAX_EXECUTE_CNT = 10;
var _lastGetMapExecuted = 0;
var _isResizeMapExecuting = false;
var _stopRequested = false;

var MAP_CNT = 0;
var INTERVAL_TIMER = null;
var MAPS = {};
var saltHash = Math.floor(Math.random() * Date.now());

function execCmd(success, error, pluginName, methodName, args, execOptions) {
  execOptions = execOptions || {};
  if (this._isRemoved && !execOptions.remove) {
    // Ignore if the instance is already removed.
    console.error("[ignore]" + pluginName + "." + methodName + ", because removed.");
    return true;
  }
  if (!this._isReady) {
    // Ignore if the instance is not ready.
    console.error("[ignore]" + pluginName + "." + methodName + ", because it's not ready.");
    return true;
  }
  var self = this;
  commandQueue.push({
    "execOptions": execOptions,
    "args": [function() {
      console.log("success: " + methodName);
      if (methodName === "resizeMap") {
        _isResizeMapExecuting = false;
      }
      if (!_stopRequested && success) {
        var results = [];
        for (var i = 0; i < arguments.length; i++) {
          results.push(arguments[i]);
        }
        common.nextTick(function() {
          success.apply(self,results);
        });
      }

      var delay = 0;
      if (methodName === _isWaitMethod) {
        // Prevent device crash when the map.getMap() executes multiple time in short period
        if (_isWaitMethod === "getMap" && Date.now() - _lastGetMapExecuted < 1500) {
          delay = 1500;
        }
        _lastGetMapExecuted = Date.now();
        _isWaitMethod = null;
      }
      setTimeout(function() {
        _executingCnt--;
        common.nextTick(_exec);
      }, delay);
    }, function() {
      console.log("error: " + methodName);
      if (methodName === "resizeMap") {
        _isResizeMapExecuting = false;
      }
      if (!_stopRequested && error) {
        var results = [];
        for (var i = 0; i < arguments.length; i++) {
          results.push(arguments[i]);
        }
        common.nextTick(function() {
          error.apply(self,results);
        });
      }

      if (methodName === _isWaitMethod) {
        _isWaitMethod = null;
      }
      _executingCnt--;
      common.nextTick(_exec);
    }, pluginName, methodName, args]
  });

  //console.log("commandQueue.length: " + commandQueue.length, commandQueue);
  if (_isExecuting || _executingCnt >= MAX_EXECUTE_CNT ) {
    return;
  }
  common.nextTick(_exec);
}
function _exec() {
  //console.log("commandQueue.length: " + commandQueue.length);
  if (_isExecuting || _executingCnt >= MAX_EXECUTE_CNT || _isWaitMethod || commandQueue.length === 0) {
    return;
  }
  _isExecuting = true;

  var methodName;
  while (commandQueue.length > 0 && _executingCnt < MAX_EXECUTE_CNT) {
    if (!_stopRequested) {
      _executingCnt++;
    }
    var commandParams = commandQueue.shift();
    methodName = commandParams.args[3];
    //console.log("target: " + methodName);
    if (methodName === "resizeMap") {
      if (_isResizeMapExecuting) {
        _executingCnt--;
        continue;
      }
      _isResizeMapExecuting = true;
    }
    if (_stopRequested && (!commandParams.execOptions.remove || methodName !== "clear")) {
      _executingCnt--;
      continue;
    }
    console.log("start: " + commandParams.args[2] + "." + methodName);
    if (commandParams.execOptions.sync) {
      _isWaitMethod = methodName;
      cordova_exec.apply(this, commandParams.args);
      break;
    }
    cordova_exec.apply(this, commandParams.args);
  }
  //console.log("commandQueue.length: " + commandQueue.length);
  _isExecuting = false;

}

module.exports = {
  event: event,
  BaseClass: BaseClass,
  Map: {
    getMap: function(div, mapOptions) {
      var mapId, elem, elemId;
      if (common.isDom(div)) {
        mapId = div.getAttribute("__pluginMapId");
        if (!mapOptions || mapOptions.visible !== false) {
          // Add gray color until the map is displayed.
          div.style.backgroundColor = "rgba(200, 200, 200, 0.5)";
        }
      }

      if (mapId && MAPS[mapId].getDiv() !== div) {
      //console.log("--->different mapdiv = " + mapId, MAPS[mapId].getDiv(), div);
        elem = MAPS[mapId].getDiv();
        while(elem && elem.nodeType === Node.ELEMENT_NODE) {
          elemId = elem.getAttribute("__pluginDomId");
          if (elemId && elemId in domPositions) {
            domPositions[elemId].containMapIDs = domPositions[elemId].containMapIDs || {};
            delete domPositions[elemId].containMapIDs[mapId];
            if ((Object.keys(domPositions[elemId].containMapIDs).length) < 1) {
              delete domPositions[elemId];
            }
          }
          elem = elem.parentNode;
        }
        MAPS[mapId].remove();
        mapId = undefined;
      }
      if (mapId && mapId in MAPS) {
        //--------------------------------------------------
        // Backward compatibility for v1
        //
        // If the div is already recognized as map div,
        // return the map instance
        //--------------------------------------------------
        return MAPS[mapId];
      } else {
        mapId = "map_" + MAP_CNT + "_" + saltHash;
      }
      var map = new Map(mapId, execCmd);

      if (common.isDom(div)) {
        div.setAttribute("__pluginMapId", mapId);

        elemId = common.getPluginDomId(div);

        //console.log("--->getMap (start)", JSON.parse(JSON.stringify(domPositions)));
        cordova_exec(function() {
          cordova_exec(function() {
            map.getMap.apply(map, args);
          }, null, 'CordovaGoogleMaps', 'putHtmlElements', [domPositions]);
        }, null, 'CordovaGoogleMaps', 'resume', []);
        //resetTimer({force: true});
      } else {
        map.getMap.apply(map, args);
      }


      var args = [mapId];
      for (var i = 0; i < arguments.length; i++) {
        args.push(arguments[i]);
      }
      map.getMap.apply(map, args);
      return map;
    }
  }
};

cordova.addConstructor(function() {
  if (!window.Cordova) {
      window.Cordova = cordova;
  }
  window.plugin = window.plugin || {};
  window.plugin.google = window.plugin.google || {};
  window.plugin.google.maps = window.plugin.google.maps || module.exports;
});
