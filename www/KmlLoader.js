var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    BaseArrayClass = require('./BaseArrayClass'),
    LatLngBounds = require('./LatLngBounds');

/*****************************************************************************
 * KmlLoader Class
 *****************************************************************************/
var KmlLoader = function(map, exec, options) {
  BaseClass.apply(this);

  var self = this;
  self._overlays = [];
  //self.set("visible", KmlLoaderOptions.visible === undefined ? true : KmlLoaderOptions.visible);
  //self.set("zIndex", KmlLoaderOptions.zIndex || 0);
  Object.defineProperty(self, "_isReady", {
      value: true,
      writable: false
  });
  Object.defineProperty(self, "type", {
      value: "KmlLoader",
      writable: false
  });
  Object.defineProperty(self, "map", {
      value: map,
      writable: false
  });
  Object.defineProperty(self, "exec", {
      value: exec,
      writable: false
  });
  Object.defineProperty(self, "options", {
      value: options,
      writable: false
  });
  Object.defineProperty(self, "kmlUrl", {
      value: options.url,
      writable: false
  });
  Object.defineProperty(self, "camera", {
      value: {
        target: []
      },
      writable: false
  });
};

utils.extend(KmlLoader, BaseClass);

KmlLoader.prototype.parseKmlFile = function(callback) {
  var self = this;

  self.exec.call(self, function(kmlData) {
    console.log(JSON.parse(JSON.stringify(kmlData))); // for debug
    Object.defineProperty(self, "kmlStyles", {
      value: kmlData.styles,
      writable: false
    });
    Object.defineProperty(self, "kmlSchemas", {
      value: kmlData.schemas,
      writable: false
    });

    var placeMarks = new BaseArrayClass(kmlData.root.children);
    placeMarks.map(function(placeMark, cb) {
      self.kmlTagProcess.call(self, {
        child: placeMark,
        attrHolder: {},
        styles: {
          children: []
        }
      }, cb);
    }, function(placeMarkOverlays) {
      placeMarkOverlays = placeMarkOverlays.filter(function(overlay) {
        return !!overlay;
      });
      callback.call(self, self.camera, placeMarkOverlays);
    });
  }, self.map.errorHandler, self.map.id, 'loadPlugin', ['KmlOverlay', {
    url: self.options.url
  }]);
};

KmlLoader.prototype.kmlTagProcess = function(params, callback) {
  var self = this;

  if (params.child.styleIDs) {
    //---------------------------
    // Read styles if specified
    //---------------------------
    var styleIDs = new BaseArrayClass(params.child.styleIDs);
    styleIDs.map(function(styleId, cb) {
      self.getStyleById.call(self, styleId, cb);
    }, function(styleSets) {

      //-----------------------------------
      // Merge styles with parent styles,
      //-----------------------------------
      var merged = {};
      styleSets.unshift(params.styles);
      styleSets.forEach(function(styleSet) {
        styleSet.children.forEach(function(element) {
          merged[element.tagName] = merged[element.tagName] || {};
          var names = Object.keys(element);
          names.forEach(function(name) {
            merged[element.tagName][name] = element[name];
          });
        });
      });
      var result = {
        children: []
      };
      var tagNames = Object.keys(merged);
      tagNames.forEach(function(tagName) {
        result.children.push(merged[tagName]);
      });
      params.styles = result;
      //------------------------
      // then process the tag.
      //------------------------
      self.parseKmlTag.call(self, params, callback);

    });
  } else {
    //-------------------------------------------------
    // No styleID is specified, just process the tag
    //-------------------------------------------------
    self.parseKmlTag.call(self, params, callback);
  }
};

KmlLoader.prototype.getObjectById = function(requestId, targetProp, callback) {
  var self = this;

  if (!requestId) {
    return callback.call(self, {children: []});
  }
  var results = {};
  var i, result, child;
  if (requestId.indexOf("http://") === 0 ||
    requestId.indexOf("https://") === 0 ||
    requestId.indexOf(".kml") !== -1) {

    if (requestId.indexOf("://") === -1) {
      requestId = self.kmlUrl.replace(/\/[^\/]+$/, "/") + requestId;
    }
    //---------------------------
    // Load additional kml file
    //---------------------------
    var requestUrl = requestId.replace(/\#.*$/, "");
    requestId = requestId.replace(/^.*?\#/, "#");

    if (requestId in self[targetProp]) {
      results = self[targetProp][requestUrl] || {};
      results = results[requestId];
      for (i = 0; i < results.children.length; i++) {
        child = results.children[i];
        if (child.tagName === "pair" && child.key === "normal") {
          return self.getObjectById.call(self, child.styleIDs[0], targetProp, callback);
        }
      }
      return callback.call(self, results);
    }

    self.exec.call(self, function(kmlData) {
      self[targetProp][requestUrl] = kmlData.results;

      var results = self[targetProp][requestId];
      for (var i = 0; i < results.children.length; i++) {
        child = results.children[i];
        if (child.tagName === "pair" && child.key === "normal") {
          return self.getObjectById.call(self, child.styleIDs[0], targetProp, callback);
        }
      }
      callback.call(self, results);
    }, function() {
      callback.call(self, {children: []});
    }, self.map.id, 'loadPlugin', ['KmlOverlay', {
      url: requestUrl
    }]);
    return;
  }

  if (requestId in self[targetProp] === false) {
    return {children: []};
  }
  results = self[targetProp][requestId];
  var style, j;

  var mvcArray = new BaseArrayClass(results.styleIDs || []);
  mvcArray.map(function(styleId, next) {
    self.getObjectById.call(self, requestId, targetProp, next);
  }, function(resultSets) {

    //-----------------------------------
    // Merge results with parent results,
    //-----------------------------------
    var merged;
    if (resultSets.length > 0) {
      merged = {};
      resultSets.unshift(results);
      resultSets.forEach(function(resultset) {
        resultset.children.forEach(function(element) {
          merged[element.tagName] = merged[element.tagName] || {};
          var names = Object.keys(element);
          names.forEach(function(name) {
            if (name === "styleIDs") {
              return;
            }
            merged[element.tagName][name] = element[name];
          });
        });
      });
      var result = {
        children: []
      };
      var tagNames = Object.keys(merged);
      tagNames.forEach(function(tagName) {
        result.children.push(merged[tagName]);
      });
    } else {
      merged = results;
    }
    merged.children = merged.children.filter(function(style) {
      return (style.tagName !== "pair" || style.key !== "highlight");
    });


    // for (i = 0; i < merged.children.length; i++) {
    //   style = merged.children[i];
    //   if (style.tagName === "pair" && style.key === "normal") {
    //     return getObjectById.call(self, style.styleIDs[0], targetProp, callback);
    //   }
    // }

    callback.call(self, merged);
  });

};

KmlLoader.prototype.getStyleById = function(requestId, callback) {
  this.getObjectById.call(this, requestId, "kmlStyles", callback);
};

KmlLoader.prototype.getSchemaById = function(requestId, callback) {
  this.getObjectById.call(this, requestId, "kmlSchemas", callback);
};

KmlLoader.prototype.parseKmlTag = function(params, callback) {
  var self = this;
//console.log(params.child.tagName, params);
  switch (params.child.tagName) {
    case "folder":
    case "placemark":
    case "document":
    case "multigeometry":
      self.parseContainerTag.call(self, {
        placeMark: params.child,
        styles: params.styles,
        attrHolder: {}
      }, callback);
      break;

    case "point":
      self.parsePointTag.call(self, {
        child: params.child,
        placeMark: params.placeMark,
        styles: params.styles,
        attrHolder: params.attrHolder
      }, callback);
      break;
    case "polygon":
      self.parsePolygonTag.call(self, {
        child: params.child,
        placeMark: params.placeMark,
        styles: params.styles,
        attrHolder: params.attrHolder
      }, callback);
      break;
    case "linestring":
      self.parseLineStringTag.call(self, {
        child: params.child,
        placeMark: params.placeMark,
        styles: params.styles,
        attrHolder: params.attrHolder
      }, callback);
      break;

    case "groundoverlay":
      self.parseGroundOverlayTag.call(self, {
        child: params.child,
        placeMark: params.placeMark,
        styles: params.styles,
        attrHolder: params.attrHolder
      }, callback);
      break;
    case "networklink":
      self.parseNetworkLinkTag.call(self, {
        child: params.child,
        placeMark: params.placeMark,
        styles: params.styles,
        attrHolder: params.attrHolder
      }, callback);
      break;
/*
    case "camera":
      self.parseCameraTag.call(self, {
        child: params.child,
      }, callback);
      break;
*/
    default:
      //console.log("[error] kml parse error: '" +  params.child.tagName + "' is not available for this plugin");
      params.attrHolder[params.child.tagName] = params.child;
      callback();
  }
};

KmlLoader.prototype.parseContainerTag = function(params, callback) {
  var self = this;

  var keys = Object.keys(params.placeMark);
  keys = keys.filter(function(key) {
    return key !== "children";
  });

  //--------------------------------------------------------
  // Generate overlays or load another files...etc
  //--------------------------------------------------------
  var children = new BaseArrayClass(params.placeMark.children);
  children.map(function(child, cb) {

    //-------------------------
    // Copy parent information
    //-------------------------
    keys.forEach(function(key) {
      if (key in child === false) {
        child[key] = params.placeMark[key];
      }
    });
    //-------------------------
    // read a child element
    //-------------------------
    self.kmlTagProcess.call(self, {
      child: child,
      placeMark: params.placeMark,
      styles: params.styles,
      attrHolder: params.attrHolder
    }, cb);
  }, function(overlays) {
    overlays = overlays.filter(function(overlay) {
      return !!overlay;
    });
    //console.log(overlays, params.attrHolder);
    var attrNames = Object.keys(params.attrHolder);

    if (params.placeMark.tagName === "placemark") {
      attrNames.forEach(function(name) {
        switch(name) {
          case "extendeddata":
            overlays[0].set(name, params.attrHolder[name]);
            break;
          default:
            overlays[0].set(name, params.attrHolder[name].value);
            break;
        }
      });

      Object.defineProperty(overlays[0], "tagName", {
        value: "placemark",
        writable: false
      });
      callback.call(self, overlays[0]);
    } else {
      var container = new BaseArrayClass(overlays);
      Object.defineProperty(container, "tagName", {
          value: params.placeMark.tagName,
          writable: false
      });
      attrNames.forEach(function(name) {
        switch(name) {
          case "extendeddata":
            container.set(name, params.attrHolder[name]);
            break;
          default:
            container.set(name, params.attrHolder[name].value);
            break;
        }
      });
      callback.call(self, container);
    }
  });
};

KmlLoader.prototype.parsePointTag = function(params, callback) {
  var self = this;

  //--------------
  // add a marker
  //--------------
  var markerOptions = {};
  params.styles.children.forEach(function(child) {
    switch (child.tagName) {
      case "iconstyle":
        child.children.forEach(function(style) {
          switch (style.tagName) {
            case "hotspot":
              markerOptions.icon = markerOptions.icon || {};
              markerOptions.icon.hotspot = style;
              break;
            case "heading":
              markerOptions.icon = markerOptions.icon || {};
              markerOptions.icon.rotation = style;
              break;
            case "icon":
              markerOptions.icon = markerOptions.icon || {};
              markerOptions.icon.url = style.href;
              break;
            case "balloonstyle":
              markerOptions.balloonstyle = style.balloonstyle;
              break;
          }
        });
        break;
      default:

    }
  });

  var ignoreProperties = ["coordinates", "styleIDs", "children"];
  var properties = Object.keys(params.attrHolder);
  properties = properties.forEach(function(pName) {
    if (ignoreProperties.indexOf(pName) === -1) {
      markerOptions[pName] = params.child[pName];
    }
  });

  if (params.child.children) {
    params.child.children.forEach(function(child) {
      if (child.tagName === "coordinates") {
        markerOptions.position = child.coordinates[0];
      }
    });
  }
  markerOptions.position = markerOptions.position || {
    lat: 0,
    lng: 0
  };

  //console.log("markerOptions", JSON.parse(JSON.stringify(markerOptions)));
  console.log(self);
  self.camera.target.push(markerOptions.position);
  self.map.addMarker(markerOptions, callback);
};

KmlLoader.prototype.parsePolygonTag = function(params, callback) {
  var self = this;

  //console.log('polygonPlacemark', params);
  //--------------
  // add a polygon
  //--------------
  var polygonOptions = {
    fill: true,
    outline: true,
    holes: []
  };
  params.child.children.forEach(function(element) {
    var coordinates;
    switch (element.tagName) {
      case "outerboundaryis":
        if (element.children.length === 1) {
          switch(element.children[0].tagName) {
            case "linearring":
              coordinates = element.children[0].children[0].coordinates;
              break;
            case "coordinates":
              coordinates = element.children[0].coordinates;
              break;
          }
          coordinates.forEach(function(latLng) {
            self.camera.target.push(latLng);
          });
          polygonOptions.points = coordinates;
        }
        break;
      case "innerboundaryis":
        switch(element.children[0].tagName) {
          case "linearring":
            coordinates = element.children[0].children[0].coordinates;
            break;
          case "coordinates":
            coordinates = element.children[0].coordinates;
            break;
        }
        polygonOptions.holes.push(coordinates);
        break;
    }
  });

  params.styles.children.forEach(function(style) {
    var keys;
    switch (style.tagName) {
      case "polystyle":
        style.children.forEach(function(node) {
          switch(node.tagName) {
            case "color":
              polygonOptions.fillColor = common.kmlColorToRGBA(node.value);
              break;
            case "fill":
              polygonOptions.fill = node.value === "1";
              break;
            case "outline":
              polygonOptions.outline = node.value === "1";
              break;
          }
        });
        break;


      case "linestyle":
        style.children.forEach(function(node) {
          switch(node.tagName) {
            case "color":
              polygonOptions.strokeColor = common.kmlColorToRGBA(node.value);
              break;
            case "width":
              polygonOptions.strokeWidth = parseInt(node.value);
              break;
          }
        });
        break;
    }
  });

  if (polygonOptions.fill === false) {
    polygonOptions.fillColor = [0, 0, 0, 0];
  } else {
    polygonOptions.fillColor = polygonOptions.fillColor || [255, 255, 255, 255];
  }
  if (polygonOptions.outline === false) {
    delete polygonOptions.strokeColor;
    polygonOptions.strokeWidth = 0;
  }

  //console.log('polygonOptions', polygonOptions);
  self.map.addPolygon(polygonOptions, callback);

};

KmlLoader.prototype.parseLineStringTag = function(params, callback) {
  var self = this;
  //--------------
  // add a polyline
  //--------------
  var polylineOptions = {
    points: []
  };
  if (params.child.children) {
    params.child.children.forEach(function(child) {
      if (child.tagName === "coordinates") {
        child.coordinates.forEach(function(latLng) {
          self.camera.target.push(latLng);
          polylineOptions.points.push(latLng);
        });
      }
    });
  }

  params.styles.children.forEach(function(style) {
    switch (style.tagName) {
      case "linestyle":
        var keys = Object.keys(style);
        keys.forEach(function(key) {
          switch(key) {
            case "color":
              polylineOptions.color = common.kmlColorToRGBA(style.color);
              break;
            case "width":
              polylineOptions.width = parseInt(style.width);
              break;
          }
        });
        break;
      default:

    }
  });

  var ignoreProperties = ["coordinates", "styleIDs"];
  var properties = Object.keys(params.child);
  properties = properties.forEach(function(pName) {
    if (ignoreProperties.indexOf(pName) === -1) {
      polylineOptions[pName] = params.child[pName];
    }
  });

  console.log('polylinePlacemark', polylineOptions);

  self.map.addPolyline(polylineOptions, callback);

};

KmlLoader.prototype.parseGroundOverlayTag = function(map, params, callback) {
  var self = this;

  //--------------
  // add a ground overlay
  //--------------
  params.child.url = null;
  params.child.bounds = [];

  params.child.children.forEach(function(child) {
    switch (child.tagName) {
      case "icon":
        params.child.url = child.href;
        if (params.child.url && params.child.url.indexOf("://") === -1) {
          var requestUrl = self.kmlUrl.replace(/\?.*$/, "");
          requestUrl = requestUrl.replace(/\#.*$/, "");
          requestUrl = requestUrl.replace(/[^\/]*$/, "");
          params.child.url = requestUrl + params.child.url;
        }
        break;
      case "latlonbox":
        if ("rotation" in child) {
          params.child.bearing = parseFloat(child.rotation);
        }
        var ne = {lat: parseFloat(child.north), lng: parseFloat(child.east)};
        var sw = {lat: parseFloat(child.south), lng: parseFloat(child.west)};
        params.child.bounds.push(ne);
        params.child.bounds.push(sw);
        self.camera.target.push(ne);
        self.camera.target.push(sw);
        break;
      default:
    }
  });
  //delete params.child.children;

  self.map.addGroundOverlay(params.child, callback);
};

KmlLoader.prototype.parseNetworkLinkTag = function(params, callback) {
  var self = this;

  var link = null;
  if (params.child.children) {
    for (var i = 0; i < params.child.children.length; i++) {
      if (params.child.children[i].tagName === "link") {
        link = params.child.children[i];
        break;
      }
    }
  }
  if (!link) {
    return callback.call(self, child);
  }
  link.children.forEach(function(child) {
    link[child.tagName] = child.value;
  });
  link.href = link.href || "";

  if (link.href.indexOf("://") === -1 && link.href.substr(0, 1) !== "/") {
    var a = document.createElement("a");
    a.href = self.kmlUrl;
    link.href = a.protocol + "//" + a.host + ":" + a.port + a.pathname.replace(/\/[^\/]+$/, "") + "/" + link.href;
    a = null;
  }

  var loader = new KmlLoader(self.map, self.exec, {
    url: link.href
  });
  loader.parseKmlFile(function(camera, placeMarkOverlays) {
    self.camera.target.push.apply(self.camera.target, camera.target);
    if ("zoom" in camera) {
      self.camera.zoom = camera.zoom;
    }
    if ("tilt" in camera) {
      self.camera.tilt = camera.tilt;
    }
    if ("heading" in camera) {
      self.camera.tilt = camera.heading;
    }
    callback.call(self, placeMarkOverlays);
  });
};

KmlLoader.prototype.parseCameraTag = function(params, callback) {
  var self = this;

  if ("latitude" in params.child && "longitude" in params.child) {
    self.camera.target.push({
      lat: parseFloat(params.child.latitude),
      lng: parseFloat(params.child.longitude)
    });
  }
  if ("heading" in params.child) {
    self.camera.bearing = parseInt(params.child.heading);
  }
  if ("tilt" in params.child) {
    self.camera.tilt = parseInt(params.child.tilt);
  }
  // <range> is not available.
  // if ("range" in params.child) {
  //   self.camera.tilt = parseInt(params.child.tilt);
  // }
  self.callback(self);
};





module.exports = KmlLoader;
