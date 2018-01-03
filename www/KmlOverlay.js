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
var KmlOverlay = function(map, kmlId, camera, kmlData) {
    BaseClass.apply(this);
    console.log(kmlData);

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
    Object.defineProperty(self, "kmlData", {
        value: kmlData,
        writable: false
    });
    function templateRenderer(html, marker) {
      var extendedData = marker.get("extendeddata");

      return html.replace(/\$[\{\[](.+?)[\}\]]/gi, function(match, name) {
        var text = marker.get(name) || "";
        if (extendedData) {
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

    var ballon = new HtmlInfoWindow();
    var onMarkerClick = function(position, marker) {
console.log(marker);
      var html = [];
      var result;
      var description = marker.get("description") || "";
      if (description.indexOf("<html>") > -1 || description.indexOf("script") > -1) {
        var text = templateRenderer(description, marker);
        // create a sandbox
        if (text.indexOf("<html") === -1) {
          text = "<html><body>" + text + "</body></html>";
        }
        result = document.createElement("div");
        if (marker.get('name')) {
          var name = document.createElement("div");
          name.style.fontWeight = 500;
          name.style.fontSize = "medium";
          name.style.marginBottom = 0;
          name.innerText = marker.get('name') || "";
          result.appendChild(name);
        }
        if (marker.get('_snippet')) {
          var snippet = document.createElement("div");
          snippet.style.fontWeight = 300;
          snippet.style.fontSize = "small";
          snippet.style.whiteSpace = "normal";
          snippet.style.fontFamily = "Roboto,Arial,sans-serif";
          snippet.innerText = marker.get('_snippet') || "";
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
        if (marker.get("name")) {
          html.push("<div style='font-weight: 500; font-size: medium; margin-bottom: 0em'>${name}</div>");
        }
        if (marker.get("_snippet")) {
          html.push("<div style='font-weight: 300; font-size: small; font-family: Roboto,Arial,sans-serif;'>${_snippet}</div>");
        }
        if (marker.get("description")) {
          html.push("<div style='font-weight: 300; font-size: small; font-family: Roboto,Arial,sans-serif;white-space:normal'>${description}</div>");
        }
        var prevMatchedCnt = 0;
        result = html.join("");
        var matches = result.match(/\$[\{\[].+?[\}\]]/gi);
        while(matches && matches.length !== prevMatchedCnt) {
          prevMatchedCnt = matches.length;
          result = templateRenderer(result, marker);
          matches = result.match(/\$[\{\[].+?[\}\]]/gi);
        }
      }
      var styles = null;
      if (marker.get("balloonstyle")) {
        styles = parseBalloonStyle(marker.get("balloonstyle"));
      }
      styles = styles || {};
      styles.overflow = "hidden";

      ballon.setContent(result, styles);
      ballon.open(marker);
    };


    var seekOverlays = function(overlay) {
      if (overlay.type === "Marker") {
        overlay.on(event.MARKER_CLICK, onMarkerClick);
      } else if (typeof overlay.forEach === "function") {
        (new BaseArrayClass(overlay)).forEach(seekOverlays);
      }
    };

console.log(kmlData);
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
