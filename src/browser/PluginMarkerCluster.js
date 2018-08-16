




var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  PluginMarker = require('cordova-plugin-googlemaps.PluginMarker'),
  Thread = require('cordova-plugin-googlemaps.Thread'),
  LatLng = require('cordova-plugin-googlemaps.LatLng');

var STATUS = {
  'WORKING': 0,
  'CREATED': 1,
  'DELETED': 2
};

function PluginMarkerCluster(pluginMap) {
  var self = this;
  PluginMarker.call(self, pluginMap);

  Object.defineProperty(self, "pluginMarkers", {
    value: {},
    enumerable: true,
    writable: false
  });
  Object.defineProperty(self, "waitCntManager", {
    value: {},
    enumerable: true,
    writable: false
  });
  Object.defineProperty(self, "debugFlags", {
    value: {},
    enumerable: true,
    writable: false
  });
  Object.defineProperty(self, "deleteMarkers", {
    value: [],
    enumerable: true,
    writable: false
  });
}

utils.extend(PluginMarkerCluster, PluginMarker);

PluginMarkerCluster.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    //map = self.pluginMap.get('map'),
    params = args[1],
    hashCode = args[2],
    positionList = params.positionList;

  //---------------------------------------------
  // Calculate geocell hashCode on multi thread
  //---------------------------------------------
  var tasks = [];
  while(positionList.length > 0) {
    var list = positionList.splice(0, 10);

    tasks.push(new Promise(function(resolve, reject) {

      var thread = new Thread(function(params) {



        var GEOCELL_GRID_SIZE = 4;
        var GEOCELL_ALPHABET = "0123456789abcdef";

        function getGeocell(lat, lng, resolution) {
          var north = 90.0,
            south = -90.0,
            east = 180.0,
            west = -180.0,
            subcell_lng_span, subcell_lat_span,
            x, y, cell = [];

          while(cell.length < resolution + 1) {
            subcell_lng_span = (east - west) / GEOCELL_GRID_SIZE;
            subcell_lat_span = (north - south) / GEOCELL_GRID_SIZE;

            x = Math.min(Math.floor(GEOCELL_GRID_SIZE * (lng - west) / (east - west)), GEOCELL_GRID_SIZE - 1);
            y = Math.min(Math.floor(GEOCELL_GRID_SIZE * (lat - south) / (north - south)), GEOCELL_GRID_SIZE - 1);
            cell.push(_subdiv_char(x, y));

            south += subcell_lat_span * y;
            north = south + subcell_lat_span;

            west += subcell_lng_span * x;
            east = west + subcell_lng_span;
          }
          return cell.join("");
        }
        function _subdiv_char(posX, posY) {
          return GEOCELL_ALPHABET.charAt(
            (posY & 2) << 2 |
            (posX & 2) << 1 |
            (posY & 1) << 1 |
            (posX & 1) << 0);
        }






        return params.positionList.map(function(position) {
          return getGeocell(position.lat, position.lng, params.resolution);
        });
      });

      thread
        .once({
          'positionList': list,
          'resolution': 12
        })
        .done(resolve)
        .fail(reject);
    }));
  }

  Promise.all(tasks)
    .then(function(results) {
      var id = "markerclister_" + hashCode;
      self.debugFlags[id] = params.debug;

      var result = {
        'geocellList': Array.prototype.concat.apply([], results),
        'hashCode': hashCode,
        'id': id
      };

      onSuccess(result);
    })
    .catch(onError);

};


PluginMarkerCluster.prototype.redrawClusters = function(onSuccess, onError, args) {
  var self = this;

  var updateClusterIDs = [],
    changeProperties = {};
    clusterId = args[0],
    isDebug = self.debugFlags[clusterId],
    params = args[1];

console.log(params);

  if ('new_or_update' in params) {

    //---------------------------
    // Determine new or update
    //---------------------------
    var new_or_updateCnt = params.new_or_update.length;

    params.new_or_update.forEach(function(clusterData, i) {
      var positionJSON = clusterData.position,
        markerId = clusterData.__pgmId,
        clusterId_markerId = clusterId + "-" + markerId;

      // Save the marker properties
      self.pluginMap.objects['marker_property_' + clusterId_markerId] = clusterData;

      // Set the WORKING status flag
      self.pluginMarkers[clusterId_markerId] = STATUS.WORKING;
      updateClusterIDs.push(clusterId_markerId);

      // Prepare the marker properties for addMarker()
      var properties = {
        'lat': positionJSON.lat,
        'lng': positionJSON.lng,
        'id': clusterId_markerId
      };
      if ('title' in clusterData) {
        properties.title = clusterData.title;
      }

      if ('icon' in clusterData) {
        var iconObj = clusterData.icon,
          iconProperties = {},
          icon,
          label;
        if (typeof iconObj === "string") {
          iconProperties.url = iconObj;
          properties.icon = iconProperties;
        } else if (typeof iconObj === "object") {
          if (clusterData.isClusterIcon) {
            if (iconObj.label) {
              label = iconObj.label;
              if (isDebug) {
                label.text = markerId;
              } else {
                label.text = clusterData.count;
              }
            } else {
              label = {};
              if (isDebug) {
                label.text = markerId;
              } else {
                label.fontSize = 15;
                label.bold = true;
                label.text = clusterData.count;
              }
            }
            iconProperties.label = label;
          }
          if ('anchor' in iconObj) {
            iconProperties.anchor = iconObj.anchor;
          }
          if ('infoWindowAnchor' in iconObj) {
            iconProperties.anchor = iconObj.infoWindowAnchor;
          }
          properties.icon = iconProperties;
        }
      }
      changeProperties[clusterId_markerId] = properties;
    });

    //---------------------------
    // mapping markers on the map
    //---------------------------
    var allResults = {};

    //---------------
    // new or update
    //---------------
    var tasks = [];
    var keys = Object.keys(updateClusterIDs);
    self.waitCntManager[clusterId] = keys.length;
    keys.forEach(function(clusterId_markerId, currentCnt) {
      self.pluginMarkers[clusterId_markerId] = STATUS.WORKING;
      isNew = !(clusterId_markerId in self.pluginMap.objects);

      // Get the marker properties
      var markerProperties = changeProperties[clusterId_markerId],
        properties;
      if (clusterId_markerId.indexOf('-marker_') > -1) {
        //-------------------
        // regular marker
        //-------------------
        if (isNew) {
          properties = self.pluginMap.objects["marker_property_" + clusterId_markerId];
          tasks.push(new Promise(function(resolve, reject) {

            PluginMarker.prototype._create.call(self, clusterId_markerId, properties, function(marker, properties) {
              if (self.pluginMarkers[clusterId_markerId] === STATUS.DELETED) {
                PluginMarker.prototype._removeMarker.call(self, marker);
                delete self.pluginMarkers[clusterId_markerId];
                resolve(null);
              } else {
                self.pluginMarkers[clusterId_markerId] = STATUS.CREATED;
                resolve(properties);
              }
            });
          }));

        }
      }
    });
    console.log(tasks);
  }
};

module.exports = PluginMarkerCluster;
