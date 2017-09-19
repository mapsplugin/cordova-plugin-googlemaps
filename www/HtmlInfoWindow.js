var utils = require('cordova/utils'),
    event = require('./event'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * HTMLInfoWindow Class
 *****************************************************************************/
var HTMLInfoWindow = function() {
    var self = this;
    BaseClass.apply(self);


    var frame = document.createElement("div");
    frame.style.overflow="visible";
    frame.style.position="absolute";
    frame.style.display = "inline-block";
    frame.classList.add('gm-html-info-frame');
    self.set("frame", frame);

    var contentBox = document.createElement("div");
    contentBox.style.display = "inline-block";
    contentBox.style.padding = "5px";
    contentBox.classList.add('gm-html-info-content-box');

    var contentFrame = document.createElement("div");
    contentFrame.style.display = "block";
    contentFrame.style.position = "relative";
    contentFrame.style.backgroundColor = "white";
    contentFrame.style.border = "1px solid rgb(204, 204, 204)";
    contentFrame.style.left = "0px";
    contentFrame.style.right = "0px";
    contentFrame.style.zIndex = "1";  // In order to set higher depth than the map div certainly
    contentFrame.classList.add('gm-html-info-content-frame');
    frame.appendChild(contentFrame);
    contentFrame.appendChild(contentBox);

    var tailFrame = document.createElement("div");
    tailFrame.style.position = "relative";
    tailFrame.style.marginTop = "-1px";
    tailFrame.classList.add('gm-html-info-tail-frame');
    frame.appendChild(tailFrame);

    var tailLeft = document.createElement("div");
    tailLeft.style.position = "absolute";
    tailLeft.style.left = "50%";
    tailLeft.style.height = "0px";
    tailLeft.style.width = "0px";
    tailLeft.style.marginLeft = "-15px";
    tailLeft.style.borderWidth = "15px 15px 0px";
    tailLeft.style.borderColor = "rgb(204, 204, 204) transparent transparent";
    tailLeft.style.borderStyle = "solid";
    tailFrame.appendChild(tailLeft);

    var tailRight = document.createElement("div");
    tailRight.style.position = "absolute";
    tailRight.style.left = "50%";
    tailRight.style.height = "0px";
    tailRight.style.width = "0px";
    tailRight.style.marginLeft = "-14px";
    tailRight.style.borderTopWidth = "14px";
    tailRight.style.borderLeftWidth = "14px";
    tailRight.style.borderRightWidth = "14px";
    tailRight.style.borderColor = "rgb(255, 255, 255) transparent transparent";
    tailRight.style.borderStyle = "solid";
    tailFrame.appendChild(tailRight);

    var calculate = function() {

      var marker = self.get("marker");
      var map = marker.getMap();

      var div = map.getDiv();

      var frame = self.get("frame");
      var contentFrame = frame.firstChild;
      var contentBox = contentFrame.firstChild;
      contentBox.style.minWidth = "100px";
      contentBox.style.minHeight = "50px";

      var content = self.get("content");
      if (typeof content === "string") {
          contentBox.style.whiteSpace="pre-wrap";
          contentBox.innerHTML = content;
      } else {
          if (!content) {
            contentBox.innerText = "";
          } else if (content.nodeType === 1) {
            contentBox.innerHTML = "";
            contentBox.appendChild(content);
          } else {
            contentBox.innerText = content;
          }
      }

      // Insert the contents to this HTMLInfoWindow
      if (!frame.parentNode) {
          div.appendChild(frame);
      }

      // Adjust the HTMLInfoWindow size
      var contentsWidth = contentBox.offsetWidth + 10; // padding 5px x 2
      self.set("contentsWidth", contentsWidth);
      var contentsHeight = contentBox.offsetHeight;
      self.set("contentsHeight", contentsHeight );
      contentFrame.style.width = contentsWidth + "px";
      contentFrame.style.height = contentsHeight + "px";
      frame.style.width = contentsWidth  + "px";
      frame.style.height = (contentsHeight+ 15) + "px";

      var infoOffset = {
        y : 0.25,
        x : 0.5
      };

      var iconSize = {
        width: 28,
        height: 60
      };
      var icon = marker.get("icon");
      if (typeof icon === "object") {
          if (typeof icon.size === "object") {
              iconSize.width = icon.size.width;
              iconSize.height = icon.size.height;
          }
          if (typeof icon.url === "string" && icon.url.indexOf("data:image/") === 0) {
              var img = document.createElement("img");
              img.src = icon.url;
              iconSize.width = img.width;
              iconSize.height = img.height;
          }

      }
      var infoWindowAnchor = marker.get("infoWindowAnchor");
      if (utils.isArray(infoWindowAnchor)) {
        infoOffset.x = infoWindowAnchor[0] / icon.size.width;
        infoOffset.x = infoOffset.x > 1 ? 1 : infoOffset.x;
        infoOffset.x = infoOffset.x < 0 ? 0 : infoOffset.x;
        infoOffset.y = infoWindowAnchor[1] / icon.size.height;
        infoOffset.y = infoOffset.y > 1 ? 1 : infoOffset.y;
        infoOffset.y = infoOffset.y < 0 ? 0 : infoOffset.y;
      }
      infoOffset.y *= iconSize.height;
      infoOffset.x = (infoOffset.x - 0.5) * iconSize.width;

      //console.log("contentsSize = " + contentsWidth + ", " + contentsHeight);
      //console.log("iconSize = " + iconSize.width + ", " + iconSize.height);
      //console.log("infoOffset = " + infoOffset.x + ", " + infoOffset.y);

      var offsetX = contentsWidth / 2  - infoOffset.x;
      var offsetY = contentsHeight  - infoOffset.y + iconSize.height;

      self.set("offsetX", offsetX);
      self.set("offsetY", offsetY);

      //console.log("offset = " + self.get("offsetX") + ", " + self.get("offsetY"));
      var infoPosition = map.get("infoPosition");
      self.trigger("infoPosition_changed", "", infoPosition);
    };

    var isInfoOpenFired = false;

    self.on("infoPosition_changed", function(ignore, point) {

        var x = point.x - self.get("offsetX");
        var y = point.y - self.get("offsetY");
        //console.log("offset = " + x + ", " + y);

        frame.style.left = x + "px";
        frame.style.top =  y + "px";

        if (!isInfoOpenFired) {
            isInfoOpenFired = true;
            self.trigger(event.INFO_OPEN);
        }

        cordova.fireDocumentEvent('plugin_touch', {});
    });
    self.on(event.INFO_CLOSE, function() {
        isInfoOpenFired = false;
    });
    self.on("infoWindowAnchor_changed", calculate);
    self.on("icon_changed", calculate);

    self.set("isInfoWindowVisible", false);

};

utils.extend(HTMLInfoWindow, BaseClass);

HTMLInfoWindow.prototype.isInfoWindowShown = function() {
    return this.get("isInfoWindowVisible") === true;
};

HTMLInfoWindow.prototype.close = function() {
    var self = this;

    var marker = self.get("marker");
    if (marker) {
      self.off("isInfoWindowVisible_changed");
    }
    if (!self.isInfoWindowShown() || !marker) {
      return;
    }
    self.set("isInfoWindowVisible", false);
    marker.set("isInfoWindowVisible", false);
    marker.set("infoWindow", undefined);
    this.set('marker', undefined);

    var map = marker.getMap();
    map.off("infoPosition_changed");
    marker.off("icon_changed");
    marker.off("infoWindowAnchor_changed");
    self.trigger(event.INFO_CLOSE);
    marker.off(event.INFO_CLOSE, self.close);  //This event listener is assigned in the open method. So detach it.
    map.set("active_marker_id", null);

    var div = map.getDiv();
    var frame = self.get("frame");
    div.removeChild(frame);

    // Remove the contents from this HTMLInfoWindow
    var contentFrame = frame.firstChild;
    var contentBox = contentFrame.firstChild;
    contentBox.innerHTML = "";
};

HTMLInfoWindow.prototype.setContent = function(content) {
    var self = this;
    var prevContent = self.get("content");
    self.set("content", content);
    var marker = self.get("marker");
    if (content !== prevContent && marker && marker.isInfoWindowShown()) {
      self.trigger("infoWindowAnchor_changed");
    }
};

HTMLInfoWindow.prototype.open = function(marker) {
    if (!marker) {
        return;
    }
    if (marker._objectInstance) {
      // marker is an instance of the ionic-native wrapper plugin.
      marker = marker._objectInstance;
    }

    var map = marker.getMap();
    var self = this,
      markerId = marker.getId();

    marker.set("infoWindow", self);
    marker.set("isInfoWindowVisible", true);
    self.set("isInfoWindowVisible", true);
    marker.one("isInfoWindowVisible_changed", function() {
      self.close.call(self);
    });

    map.fromLatLngToPoint(marker.getPosition(), function(point) {
        map.set("infoPosition", {x: point[0], y: point[1]});

        map.bindTo("infoPosition", self);
        marker.bindTo("infoWindowAnchor", self);
        marker.bindTo("icon", self);
        marker.one(event.INFO_CLOSE, self.close.bind(self));
        self.set("marker", marker);
        map.set("active_marker_id", marker.getId());
        self.trigger.call(self, "infoWindowAnchor_changed");
    });
};

HTMLInfoWindow.prototype.setBackgroundColor = function(backgroundColor) {
  this.get("frame").children[0].style.backgroundColor = backgroundColor;
  this.get("frame").children[1].children[0].style.borderColor = backgroundColor + " rgba(0,0,0,0) rgba(0,0,0,0)";
  this.get("frame").children[1].children[1].style.borderColor = backgroundColor + " rgba(0,0,0,0) rgba(0,0,0,0)";
};

module.exports = HTMLInfoWindow;
