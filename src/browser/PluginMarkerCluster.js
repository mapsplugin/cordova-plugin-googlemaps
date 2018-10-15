var utils = require('cordova/utils'),
  event = require('cordova-plugin-googlemaps.event'),
  PluginMarker = require('cordova-plugin-googlemaps.PluginMarker'),
  Thread = require('cordova-plugin-googlemaps.Thread'),
  LatLng = require('cordova-plugin-googlemaps.LatLng'),
  BaseArrayClass = require('cordova-plugin-googlemaps.BaseArrayClass');

var STATUS = {
  'WORKING': 0,
  'CREATED': 1,
  'DELETED': 2
};

function PluginMarkerCluster(pluginMap) {
  var self = this;
  PluginMarker.call(self, pluginMap);

  Object.defineProperty(self, 'pluginMarkers', {
    value: {},
    writable: false
  });
  Object.defineProperty(self, 'debugFlags', {
    value: {},
    writable: false
  });

  var deleteMarkers = new BaseArrayClass();
  Object.defineProperty(self, 'deleteMarkers', {
    value: deleteMarkers,
    writable: false
  });

  deleteMarkers.on('insert_at', function() {
    var key = deleteMarkers.removeAt(0);
    var marker = self.pluginMap.objects[key];
    if (marker) {
      self._removeMarker(marker);
    }

    self.pluginMap.objects[key] = undefined;
    self.pluginMap.objects['marker_property_' + key] = undefined;
    self.pluginMarkers[key] = undefined;

    delete self.pluginMap.objects[key];
    delete self.pluginMap.objects['marker_property_' + key];
    delete self.pluginMarkers[key];
    self.pluginMarkers[key] = STATUS.DELETED;
  });
}

utils.extend(PluginMarkerCluster, PluginMarker);

PluginMarkerCluster.prototype._create = function(onSuccess, onError, args) {
  var self = this,
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
        var GEOCELL_ALPHABET = '0123456789abcdef';

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
          return cell.join('');
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
      var id = 'markerclister_' + hashCode;
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
    changeProperties = {},
    clusterId = args[0],
    isDebug = self.debugFlags[clusterId],
    params = args[1],
    map = self.pluginMap.get('map');

  if ('new_or_update' in params) {

    //---------------------------
    // Determine new or update
    //---------------------------
    params.new_or_update.forEach(function(clusterData) {
      var positionJSON = clusterData.position,
        markerId = clusterData.__pgmId,
        clusterId_markerId = clusterId + '-' + markerId;

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
          label;
        if (typeof iconObj === 'string') {
          iconProperties.url = iconObj;
          properties.icon = iconProperties;
        } else if (typeof iconObj === 'object') {
          iconProperties = iconObj;
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
      changeProperties[clusterId_markerId] =  JSON.parse(JSON.stringify(properties));
    });

    if (updateClusterIDs.length === 0) {
      self.deleteProcess(clusterId, params);
      onSuccess({});
      return;
    }

    //---------------------------
    // mapping markers on the map
    //---------------------------
    var allResults = {};

    //---------------
    // new or update
    //---------------
    var tasks = [];
    updateClusterIDs.forEach(function(clusterId_markerId) {
      self.pluginMarkers[clusterId_markerId] = STATUS.WORKING;
      var isNew = !(clusterId_markerId in self.pluginMap.objects);

      // Get the marker properties
      var markerProperties = changeProperties[clusterId_markerId],
        properties, marker;

      if (clusterId_markerId.indexOf('-marker_') > -1) {
        //-------------------
        // regular marker
        //-------------------
        if (isNew) {
          properties = self.pluginMap.objects['marker_property_' + clusterId_markerId];

          tasks.push(new Promise(function(resolve) {

            self.__create.call(self, clusterId_markerId, {
              'position': properties.position,
              'icon': properties.icon,
              'disableAutoPan': properties.disableAutoPan
            }, function(marker, properties) {
              if (markerProperties.title) {
                marker.set('title', markerProperties.title);
              }
              if (markerProperties.snippet) {
                marker.set('snippet', markerProperties.snippet);
              }
              if (self.pluginMarkers[clusterId_markerId] === STATUS.DELETED) {
                self._removeMarker.call(self, marker);
                delete self.pluginMarkers[clusterId_markerId];
                resolve();
              } else {
                self.pluginMarkers[clusterId_markerId] = STATUS.CREATED;
                allResults[clusterId_markerId.split('-')[1]] = {
                  'width': properties.width,
                  'height': properties.height
                };
                resolve();
              }
            });
          }));

        } else {
          marker = self.pluginMap.objects[clusterId_markerId];
          //----------------------------------------
          // Set the title and snippet properties
          //----------------------------------------
          if (markerProperties.title) {
            marker.set('title', markerProperties.title);
          }
          if (markerProperties.snippet) {
            marker.set('snippet', markerProperties.snippet);
          }
          if (self.pluginMarkers[clusterId_markerId] === STATUS.DELETED) {
            self._removeMarker.call(self, marker);
            delete self.pluginMarkers[clusterId_markerId];
          } else {
            self.pluginMarkers[clusterId_markerId] = STATUS.CREATED;
          }

        }
      } else {
        //--------------------------
        // cluster icon
        //--------------------------
        if (isNew) {
          // If the requested id is new location, create a marker
          marker = newClusterIcon({
            'map': map,
            'position': {
              'lat': markerProperties.lat,
              'lng': markerProperties.lng
            },
            'overlayId': clusterId_markerId,
            'opacity': 0
          });
          marker.addListener('click', self.onClusterEvent.bind(self, 'cluster_click', marker));

          // Store the marker instance with markerId
          self.pluginMap.objects[clusterId_markerId] = marker;

        } else {
          marker = self.pluginMap.objects[clusterId_markerId];
        }
        //----------------------------------------
        // Set the title and snippet properties
        //----------------------------------------
        if (markerProperties.title) {
          marker.set('title', markerProperties.title);
        }
        if (markerProperties.snippet) {
          marker.set('snippet', markerProperties.snippet);
        }
        if (markerProperties.icon) {
          var icon = markerProperties.icon;

          tasks.push(new Promise(function(resolve) {

            self.setIconToClusterMarker.call(self, clusterId_markerId, marker, icon)
              .then(function() {
                //--------------------------------------
                // Marker was updated
                //--------------------------------------
                marker.setVisible(true);
                self.pluginMarkers[clusterId_markerId] = STATUS.CREATED;
                resolve();
              })
              .catch(function(error) {
                //--------------------------------------
                // Could not read icon for some reason
                //--------------------------------------
                if (marker.get('overlayId')) {
                  self._removeMarker.call(self, marker);
                }
                self.pluginMarkers[clusterId_markerId] = STATUS.DELETED;

                console.warn(error.getMessage());
                self.deleteMarkers.push(clusterId_markerId);
                resolve();
              });

          }));

        } else {
          //--------------------
          // No icon for marker
          //--------------------
          self.pluginMarkers[clusterId_markerId] = STATUS.CREATED;
        }
      }
    });
    Promise.all(tasks).then(function() {
      self.deleteProcess(clusterId, params);
      onSuccess(allResults);
    }).catch(onError);
  }
};

PluginMarkerCluster.prototype.deleteProcess = function(clusterId, params) {
  var self = this;
  if (!params.delete || params.delete.length === 0) {
    return;
  }
  params.delete.forEach(function(key) {
    self.deleteMarkers.push(clusterId + '-' + key);
  });
};

PluginMarkerCluster.prototype.setIconToClusterMarker = function(markerId, marker, iconProperty) {
  var self = this;
  return new Promise(function(resolve, reject) {

    if (self.pluginMarkers[markerId] === STATUS.DELETED) {
      self._removeMarker.call(self, marker);
      delete self.pluginMap.objects[markerId];
      delete self.pluginMap.objects['marker_property_' + markerId];

      delete self.pluginMarkers[markerId];
      reject('marker has been removed');
      return;
    }
    self.setIcon_.call(self, marker, iconProperty)
      .then(function() {
        if (self.pluginMarkers[markerId] === STATUS.DELETED) {
          self._removeMarker.call(self, marker);
          delete self.pluginMap.objects[markerId];
          delete self.pluginMap.objects['marker_property_' + markerId];
          self.pluginMarkers.remove(markerId);
          resolve();
          return;
        }
        marker.setVisible(true);
        self.pluginMarkers[markerId] = STATUS.CREATED;
        resolve();
      });
  });

};

PluginMarkerCluster.prototype.remove = function(onSuccess, onError, args) {
  var self = this,
    clusterId = args[0],
    keys = Object.keys(self.pluginMarkers);

  keys.forEach(function(key) {
    if (key.indexOf(clusterId) === 0) {
      self.pluginMarkers[key] = STATUS.DELETED;
      delete self.pluginMap.objects[key];
      delete self.pluginMap.objects['marker_property_' + key];
    }
  });
  onSuccess();
};

PluginMarkerCluster.prototype.onClusterEvent = function(evtName, marker) {
  var self = this,
    mapId = self.pluginMap.id;
  var overlayId = marker.get('overlayId');
  var tmp = overlayId.split('-');
  var clusterId = tmp[0];
  var markerId = tmp[1];
  var latLng = marker.getPosition();
  if (mapId in plugin.google.maps) {
    if (self.pluginMap.activeMarker) {
      self.onMarkerEvent(event.INFO_CLOSE, self.pluginMap.activeMarker);
    }
    plugin.google.maps[mapId]({
      'evtName': evtName,
      'callback': '_onClusterEvent',
      'args': [clusterId, markerId, new LatLng(latLng.lat(), latLng.lng())]
    });
  }
};


module.exports = PluginMarkerCluster;

function newClusterIcon(options) {

  // https://github.com/apache/cordova-js/blob/c75e8059114255d1dbae1ede398e6626708ee9f3/src/common/utils.js#L167
  if (ClusterIconClass.__super__ !== google.maps.OverlayView.prototype) {
    var key, defined = {};
    for (key in ClusterIconClass.prototype) {
      defined[key] = ClusterIconClass.prototype[key];
    }
    utils.extend(ClusterIconClass, google.maps.OverlayView);
    for (key in defined) {
      ClusterIconClass.prototype[key] = defined[key];
    }
  }

  return new ClusterIconClass(options);
}
function ClusterIconClass(options) {
  google.maps.OverlayView.apply(this);
  var self = this;
  //-----------------------------------------
  // Create a canvas to draw label
  //-----------------------------------------
  var canvas = document.createElement('canvas');
  canvas.width = 50;
  canvas.height = 50;
  canvas.style.visibility = 'hidden';
  canvas.style.position = 'absolute';
  canvas.style.zIndex = -100;
  canvas.setAttribute('id', 'canvas_' + options.overlayId);
  self.set('canvas', canvas);

  //-----------------------------------------
  // Create two markers for icon and label
  //-----------------------------------------
  var iconMarker = new google.maps.Marker({
    'clickable': false,
    'icon': options.icon,
    'zIndex': 0,
    'opacity': 0
  });
  var labelMarker = new google.maps.Marker({
    'clickable': true,
    'zIndex': 1,
    'icon': self.get('label'),
    'opacity': 0
  });
  labelMarker.addListener('click', function() {
    google.maps.event.trigger(self, 'click');
  });
  self.set('iconMarker', iconMarker);
  self.set('labelMarker', labelMarker);
  self.set('opacity', 0);

  iconMarker.bindTo('opacity', labelMarker);
  iconMarker.bindTo('visible', labelMarker);
  iconMarker.bindTo('position', labelMarker);
  iconMarker.bindTo('map', labelMarker);
  self.bindTo('opacity', iconMarker);
  self.bindTo('visible', iconMarker);
  self.bindTo('map', iconMarker);
  self.bindTo('position', iconMarker);
  self.set('labelMarkerAnchor', new google.maps.Point(canvas.width / 2, canvas.height / 2));

  self.addListener('icon_changed', function() {
    var icon = self.get('icon');
    if (typeof icon === 'string') {
      icon = {
        'url': icon
      };
    }

    var iconUrl = icon.url;
    if (typeof icon === 'object') {
      if (typeof icon.size === 'object' &&
          icon.size.width && icon.size.height) {
        icon.anchor = new google.maps.Point(icon.size.width / 2, icon.size.height / 2);
        iconMarker.setIcon(icon);
        return;
      }
    }
    var img = new Image();
    img.onload = function() {
      icon.size = new google.maps.Size(img.width, img.height);
      icon.scaledSize = new google.maps.Size(img.width, img.height);
      icon.anchor = new google.maps.Point(img.width / 2, img.height / 2);
      self.set('labelMarkerAnchor', new google.maps.Point(img.width / 2, img.height / 2));
      iconMarker.setIcon(icon);
    };
    img.onerror = function(e) {
      console.error(e);
    };
    img.src = iconUrl;
  });

  //debug
  //var positionConfirmMarker = new google.maps.Marker();
  //labelMarker.bindTo('position', positionConfirmMarker);
  //labelMarker.bindTo('map', positionConfirmMarker);

  for (var key in options) {
    self.set(key, options[key]);
  }

}

ClusterIconClass.prototype.onAdd = function() {
  var self = this;
  var canvas = self.get('canvas');
  self.get('map').getDiv().appendChild(canvas);
};
ClusterIconClass.prototype.onRemove = function() {
  var self = this;
  var canvas = self.get('canvas');
  self.set('map', null);
  google.maps.event.clearInstanceListeners(self.get('iconMarker'));
  google.maps.event.clearInstanceListeners(self.get('labelMarker'));
  var parent = canvas.parentNode;
  if (parent) {
    parent.removeChild(canvas);
  }
  canvas = undefined;
};
ClusterIconClass.prototype.draw = function() {
  var self = this,
    icon = self.get('icon');
  if (typeof icon === 'string') {
    icon = {
      'url': icon
    };
  }
  icon.label = icon.label || {};
  if (self.get('prevText') === icon.label.text) {
    return;
  }
  self.set('prevText', icon.label.text);
  self.get('labelMarker').set('opacity', 0);

  (new Promise(function(resolve) {
    var iconUrl = icon.url;
    if (typeof icon === 'object') {
      if (typeof icon.size === 'object' &&
          icon.size.width && icon.size.height) {
        return resolve(icon.size);
      }
    }
    var img = new Image();
    img.onload = function() {
      var newIconInfo = {
        width: img.width,
        height: img.height
      };
      icon.size = newIconInfo;
      self.set('labelMarkerAnchor', new google.maps.Point(newIconInfo.width / 2, newIconInfo.height / 2));
      self.set('icon', icon, true);
      resolve(newIconInfo);
    };
    img.onerror = function() {
      var newIconInfo = {
        width: img.width,
        height: img.height
      };
      icon.size = newIconInfo;
      self.set('icon', icon, true);
      resolve(newIconInfo);
    };
    img.src = iconUrl;
  }))
    .then(function(iconSize) {
      var canvas = self.get('canvas'),
        ctx = canvas.getContext('2d');
      canvas.width = iconSize.width;
      canvas.height = iconSize.height;

      var labelOptions =  icon.label || {
        fontSize: 10,
        bold: false,
        italic: false
      };
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      // debug
      //ctx.fillStyle="#FF000077";
      //ctx.fillRect(0, 0, canvas.width, canvas.height);

      if (labelOptions.text) {
        var fontStyles = [];

        if ('color' in labelOptions) {
          ctx.fillStyle = [
            'rgba(',
            labelOptions.color[0],
            ',',
            labelOptions.color[1],
            ',',
            labelOptions.color[2],
            ',',
            labelOptions.color[3] / 255,
            ')'].join('');
        } else {
          ctx.fillStyle = 'black';
        }

        if (labelOptions.italic === true) {
          fontStyles.push('italic');
        }
        if (labelOptions.bold === true) {
          fontStyles.push('bold');
        }

        fontStyles.push(parseInt(labelOptions.fontSize || '10', 10) + 'px');

        fontStyles.push('Arial');

        ctx.font = fontStyles.join(' ');
        ctx.textBaseline = 'middle';
        ctx.textAlign = 'center';
        ctx.fillText(labelOptions.text, iconSize.width / 2, iconSize.height / 2);
        // debug
        //ctx.fillText(selfId.split("-")[1], iconSize.width / 2, iconSize.height / 2);

      }

      self.get('labelMarker').set('icon', {
        'url': canvas.toDataURL(),
        'anchor': self.get('labelMarkerAnchor')
      });
      setTimeout(function() {
        self.set('opacity', 1);
      }, 10);
    });


};
ClusterIconClass.prototype.setIcon = function(icon) {
  var self = this;
  self.set('icon', icon);
};
ClusterIconClass.prototype.setVisible = function(visible) {
  var self = this;
  self.set('visible', visible);
};
ClusterIconClass.prototype.setPosition = function(position) {
  var self = this;
  self.set('position', position);
};

ClusterIconClass.prototype.getPosition = function() {
  var self = this;
  return self.get('position');
};

ClusterIconClass.prototype.setMap = function(map) {
  var self = this;
  self.set('map', map);
};
