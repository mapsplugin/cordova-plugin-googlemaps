var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    common = require('./Common'),
    BaseClass = require('./BaseClass'),
    event = require('./event'),
    BaseArrayClass = require('./BaseArrayClass'),
    HtmlInfoWindow = require('./HtmlInfoWindow');

/*****************************************************************************
 * KmlOverlay Class
 *****************************************************************************/
var exec;
var KmlOverlay = function(map, kmlId, camera, overlays) {
    BaseClass.apply(this);
    console.log(overlays);

    var self = this;
    self._overlays = [];
    //self.set("visible", kmlOverlayOptions.visible === undefined ? true : kmlOverlayOptions.visible);
    //self.set("zIndex", kmlOverlayOptions.zIndex || 0);
    Object.defineProperty(self, "_isReady", {
        value: true,
        writable: false
    });
    Object.defineProperty(self, "type", {
        value: "KmlOverlay",
        writable: false
    });
    Object.defineProperty(self, "id", {
        value: kmlId,
        writable: false
    });
    Object.defineProperty(self, "map", {
        value: map,
        writable: false
    });
    Object.defineProperty(self, "camera", {
        value: camera,
        writable: false
    });
    Object.defineProperty(self, "overlays", {
        value: overlays,
        writable: false
    });
    function templateRenderer(html, marker) {
      var extendedData = marker.get("extendeddata");
      var values = null;
      if (extendedData) {
        var schemaUrl = null;
        var children;
        if (extendedData.children[0].tagName === "schemedata") {
          schemaUrl = extendedData.children[0].schemaUrl;
          children = extendData.children[0].children;
        } else {
          children = extendData.children;
        }
        values = {};
        children.forEach(function(child) {
          values[child.name] = child.value;
        });
      }

      return html.replace(/\$[\{\[](.+?)[\}\]]/g, function(match, name) {
        if (values && name in values) {
          return values[name] || "";
        }
        return marker.get(name) || "";
      });
    }
    function parseBalloonStyle(balloonStyle) {
      var css = {};
      var hasBgColor = false;
      var keys = Object.keys(balloonStyle);
      keys.forEach(function(key) {
        switch(key.toLowerCase()) {
          case "bgcolor":
            hasBgColor = true;
            ballon.setBackgroundColor(common.kmlColorToCSS(balloonStyle[key]));
            break;
          case "textcolor":
            css.color = common.kmlColorToCSS(balloonStyle[key]);
            break;
        }
      });
      if (!hasBgColor) {
        ballon.setBackgroundColor("white");
      }
      return css;
    }

    var ballon = new HtmlInfoWindow();
    var onMarkerClick = function(position, marker) {
      var html = [
        "<div style='font-weight: 500; font-size: medium; margin-bottom: 0em'>${name}</div>",
        "<div style='font-weight: 300; font-size: small; font-family: Roboto,Arial,sans-serif;white-space:normal'>${description}</div>"
      ].join("");
      var styles = null;
      if (marker.get("balloonstyle")) {
        styles = parseBalloonStyle(marker.get("balloonstyle"));
      }
      ballon.setContent(templateRenderer(html, marker), styles);
      ballon.open(marker);
    };


    var seekOverlays = function(overlay) {
      if (overlay.type === "Marker") {
        overlay.on(event.MARKER_CLICK, onMarkerClick);
      } else if (overlay instanceof BaseArrayClass) {
        overlay.forEach(seekOverlays);
      }
    };

    overlays.forEach(seekOverlays);
/*
    var ignores = ["map", "id", "hashCode", "type"];
    for (var key in kmlOverlayOptions) {
        if (ignores.indexOf(key) === -1) {
            self.set(key, kmlOverlayOptions[key]);
        }
    }
*/
};

utils.extend(KmlOverlay, BaseClass);

KmlOverlay.prototype.getPluginName = function() {
    return this.map.getId() + "-kmloverlay";
};

KmlOverlay.prototype.getHashCode = function() {
    return this.hashCode;
};

KmlOverlay.prototype.getOverlays = function() {
    return this._overlays;
};
KmlOverlay.prototype.getMap = function() {
    return this.map;
};
KmlOverlay.prototype.getId = function() {
    return this.id;
};

KmlOverlay.prototype.remove = function(callback) {
    var self = this;
    if (self._isRemoved) {
      return;
    }
    Object.defineProperty(self, "_isRemoved", {
        value: true,
        writable: false
    });


    var removeChildren = function(children, cb) {
      if (!children || !utils.isArray(children)) {
        return cb();
      }

      var baseArray = new BaseArrayClass(children);
      baseArray.forEach(function(child, next) {
        if ('remove' in child &&
          typeof child.remove === 'function') {
          child.remove(next);
          return;
        }
        if ('children' in child &&
          utils.isArray(child.children)) {
          removeChildren(child.children, next);
        } else {
          next();
        }
      }, cb);
    };

    removeChildren(self.overlays, function() {
      self.destroy();
      if (typeof callback === "function") {
          callback.call(self);
      }
    });
};


module.exports = KmlOverlay;
