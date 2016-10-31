var utils = require('cordova/utils'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * InfoWindow Class
 *****************************************************************************/
var InfoWindow = function() {
    var self = this;
    BaseClass.apply(self);


    var frame = document.createElement("div");
    frame.style.overflow="visible";
    frame.style.position="absolute";
    frame.style.display = "inline-block";
    self.set("frame", frame);

    var content = document.createElement("div");
    content.style.display = "inline-block";
    content.style.padding = "5px";

    var contentFrame = document.createElement("div");
    contentFrame.style.display = "block";
    contentFrame.style.position = "relative";
    contentFrame.style.backgroundColor = "white";
    contentFrame.style.border = "1px solid rgb(204, 204, 204)";
    contentFrame.style.left = "0px";
    contentFrame.style.right = "0px";
    frame.appendChild(contentFrame);
    contentFrame.appendChild(content);

    var tailFrame = document.createElement("div");
    tailFrame.style.position = "relative";
    tailFrame.style.marginTop = "-1px";
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

      // Display the title property
      // (ignore the snippet propert)
      var title = marker.get("title");
      if (typeof title === "string") {
          contentBox.style.whiteSpace="nowrap";
          contentBox.innerHTML = title;
      } else {
          contentBox.appendChild(title);
      }

      // Insert the contents to this infoWindow
      if (!frame.parentNode) {
          div.appendChild(frame);
      }

      // Adjust the infoWindow size
      var contentsWidth = contentBox.clientWidth;
      self.set("contentsWidth", contentsWidth);
      var contentsHeight = contentBox.clientHeight;
      self.set("contentsHeight", contentsHeight );
      contentFrame.style.width = contentsWidth + "px";
      contentFrame.style.height = contentsHeight + "px";
      frame.style.width = contentsWidth + "px";
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

    self.on("infoPosition_changed", function(ignore, point) {

        var x = point.x - self.get("offsetX");
        var y = point.y - self.get("offsetY");
        //console.log("offset = " + x + ", " + y);

        frame.style.left = x + "px";
        frame.style.top =  y + "px";
    });
    self.on("infoWindowAnchor_changed", calculate);
    self.on("icon_changed", calculate);

};

utils.extend(InfoWindow, BaseClass);

InfoWindow.prototype.close = function(marker) {
    var map = marker.getMap();
    map.off("infoPosition_changed");
    marker.off("icon_changed");
    marker.off("infoWindowAnchor_changed");
    var map = marker.getMap();
    var div = map.getDiv();
    var frame = this.get("frame");
    div.removeChild(frame);
    this.set('marker', undefined);

    // Remove the contents from this infoWindow
    var contentFrame = frame.firstChild;
    var content = contentFrame.firstChild;
    content.innerHTML = "";
};

InfoWindow.prototype.open = function(marker) {
    var map = marker.getMap();
    map.bindTo("infoPosition", this);
    marker.bindTo("infoWindowAnchor", this);
    marker.bindTo("icon", this);
    this.set("marker", marker);
    this.trigger("infoWindowAnchor_changed");
};

module.exports = InfoWindow;
