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
var KmlOverlay = function(map, kmlId, camera, kmlData, kmlOverlayOptions) {
    BaseClass.apply(this);

    var self = this;
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
    Object.defineProperty(self, "kmlData", {
        value: kmlData,
        writable: false
    });

    function templateRenderer(html, marker) {
      var extendedData = marker.get("extendeddata");

      return html.replace(/\$[\{\[](.+?)[\}\]]/gi, function(match, name) {
        var text = "";
        if (marker.get(name)) {
          text = marker.get(name).value || "";
        }
        if (extendedData && text) {
          text = text.replace(/\$[\{\[](.+?)[\}\]]/gi, function(match1, name1) {
            return extendedData[name1.toLowerCase()] || "";
          });
        }
        return text;
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

    self.set("clickable", kmlOverlayOptions.clickable);

    var ballon = new HtmlInfoWindow();
    var onOverlayClick = function(position, overlay) {
      if (!self.get("clickable")) {
        return;
      }
      self.trigger(event.KML_CLICK, overlay, position);

      if (kmlOverlayOptions.suppressInfoWindows) {
        return;
      }

      var description = overlay.get("description");
      if (!description && overlay.get("extendeddata")) {
        var extendeddata = overlay.get("extendeddata");
        var keys = Object.keys(extendeddata);
        var table = [];
        keys.forEach(function(key) {
          if (extendeddata[key] !== "" && extendeddata[key] !== null && extendeddata[key] !== undefined) {
            table.push("<tr><th>" + key + "</th><td>" + extendeddata[key] + "</td></tr>");
          }
        });
        if (table.length > 0) {
          table.unshift("<table border='1'>");
          table.push("</table>");
          description = {
            value: table.join("")
          };
          overlay.set("description", description);
        }
      }



      var html = [];
      var result;
      var descriptionTxt = "";
      if (description && description.value) {
        descriptionTxt = description.value;
      }
      if (description && (descriptionTxt.indexOf("<html>") > -1 || descriptionTxt.indexOf("script") > -1)) {
        var text = templateRenderer(descriptionTxt, overlay);
        // create a sandbox
        if (text.indexOf("<html") === -1) {
          text = "<html><body>" + text + "</body></html>";
        }
        result = document.createElement("div");
        if (overlay.get('name')) {
          var name = document.createElement("div");
          name.style.fontWeight = 500;
          name.style.fontSize = "medium";
          name.style.marginBottom = 0;
          name.innerText = overlay.get('name') || "";
          result.appendChild(name);
        }
        if (overlay.get('snippet')) {
          var snippet = document.createElement("div");
          snippet.style.fontWeight = 300;
          snippet.style.fontSize = "small";
          snippet.style.whiteSpace = "normal";
          snippet.style.fontFamily = "Roboto,Arial,sans-serif";
          snippet.innerText = overlay.get('snippet').value || "";
          result.appendChild(snippet);
        }

        var iframe = document.createElement('iframe');
        iframe.sandbox = "allow-scripts allow-same-origin";
        iframe.frameBorder = "no";
        iframe.scrolling = "yes";
        iframe.style.overflow = "hidden";
        iframe.addEventListener('load', function() {
          iframe.contentWindow.document.open();
          iframe.contentWindow.document.write(text);
          iframe.contentWindow.document.close();
        }, {
          once: true
        });
        result.appendChild(iframe);

      } else {
        if (overlay.get("name")) {
          html.push("<div style='font-weight: 500; font-size: medium; margin-bottom: 0em'>${name}</div>");
        }
        if (overlay.get("_snippet")) {
          html.push("<div style='font-weight: 300; font-size: small; font-family: Roboto,Arial,sans-serif;'>${_snippet}</div>");
        }
        if (overlay.get("description")) {
          html.push("<div style='font-weight: 300; font-size: small; font-family: Roboto,Arial,sans-serif;white-space:normal'>${description}</div>");
        }
        var prevMatchedCnt = 0;
        result = html.join("");
        var matches = result.match(/\$[\{\[].+?[\}\]]/gi);
        while(matches && matches.length !== prevMatchedCnt) {
          prevMatchedCnt = matches.length;
          result = templateRenderer(result, overlay);
          matches = result.match(/\$[\{\[].+?[\}\]]/gi);
        }
      }
      var styles = null;
      if (overlay.get("balloonstyle")) {
        styles = parseBalloonStyle(overlay.get("balloonstyle"));
      }
      styles = styles || {};
      styles.overflow = "scroll";
      styles["max-width"] = (map.getDiv().offsetWidth * 0.8) + "px";
      styles["max-height"] = (map.getDiv().offsetHeight * 0.6) + "px";

      ballon.setContent(result, styles);
      var marker = map.get("invisible_dot");
      if (overlay.type === "Marker") {
        marker.setVisible(false);
        ballon.open(overlay);
      } else {
        marker.setPosition(position);
        marker.setVisible(true);
        marker.off(event.MARKER_CLICK);
        map.animateCamera({
          target: position,
          duration: 300
        }, function() {
          marker.setAnimation(plugin.google.maps.Animation.DROP, function() {
            marker.on(event.MARKER_CLICK, function() {
              ballon.open(marker);
            });
            marker.trigger(event.MARKER_CLICK);
          });
        });

      }
    };

    var eventNames = {
      "Marker": event.MARKER_CLICK,
      "Polyline": event.POLYLINE_CLICK,
      "Polygon": event.POLYGON_CLICK,
      "GroundOverlay": event.GROUND_OVERLAY_CLICK
    };
    var seekOverlays = function(overlay) {
      if (overlay instanceof BaseArrayClass) {
        overlay.forEach(seekOverlays);
      } else if (Array.isArray(overlay)) {
        (new BaseArrayClass(overlay)).forEach(seekOverlays);
      } else if (overlay instanceof BaseClass && overlay.type in eventNames) {
        overlay.on(eventNames[overlay.type], onOverlayClick);
      }
    };

    kmlData.forEach(seekOverlays);

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

KmlOverlay.prototype.getDefaultViewport = function() {
    return this.camera;
};
KmlOverlay.prototype.getKmlData = function() {
    return this.kmlData;
};
KmlOverlay.prototype.getMap = function() {
    return this.map;
};
KmlOverlay.prototype.getId = function() {
    return this.id;
};

KmlOverlay.prototype.setClickable = function(clickable) {
  this.set('clickable', clickable);
  return this;
};

KmlOverlay.prototype.getClickable = function() {
  return this.get('clickable');
};

KmlOverlay.prototype.getVisible = function() {
  return this.get('visible');
};

KmlOverlay.prototype.setVisible = function(visible) {
  var self = this;
  if (self._isRemoved) {
    return;
  }

  this.set('visible', visible);

  var applyChildren = function(children) {
    children.forEach(function(child) {
      if ('setVisible' in child &&
        typeof child.setVisible === 'function') {
        child.setVisible(visible);
        return;
      }
      if (child instanceof BaseArrayClass) {
        applyChildren(child);
        return;
      }
    });
  };

  applyChildren(self.kmlData);
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
      children.forEach(function(child, next) {
        if ('remove' in child &&
          typeof child.remove === 'function') {
          child.remove(next);
          return;
        }
        if (child instanceof BaseArrayClass) {
          removeChildren(child, next);
          return;
        }
        next();
      }, cb);
    };

    removeChildren(self.kmlData, function() {
      self.destroy();
      if (typeof callback === "function") {
          callback.call(self);
      }
    });
};


module.exports = KmlOverlay;
