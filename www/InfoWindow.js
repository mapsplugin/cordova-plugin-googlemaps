var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * InfoWindow Class
 *****************************************************************************/
var InfoWindow = function() {
    BaseClass.apply(this);

/*

<div style="position: relative; margin-top: -1px;">
<div style="position: absolute; left: 50%; height: 0px; width: 0px; margin-left: -15px; border-width: 15px 15px 0px; border-color: rgb(204, 204, 204) transparent transparent; border-style: solid;"></div>
<div style="position: absolute; left: 50%; height: 0px; width: 0px; border-color: rgb(255, 255, 255) transparent transparent; border-style: solid; border-top-width: 14px; border-left-width: 14px; border-right-width: 14px; margin-left: -14px;"></div></div>
*/

    var frame = document.createElement("div");
    frame.style.overflow="visible";
    frame.style.position="absolute";
    frame.style.display = "inline-block";
    this.set("frame", frame);

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

    this.on("infoPosition_changed", function(ignore, point) {
      var width = this.get("width");
      var height = this.get("height");
      frame.style.left = (point.x - width / 2) + "px";
      frame.style.top = (point.y - height - 60)  + "px";
    });

};

utils.extend(InfoWindow, BaseClass);

InfoWindow.prototype.close = function(marker) {
    var map = marker.getMap();
    map.off("infoPosition_changed");
    var map = marker.getMap();
    var div = map.getDiv();
    var frame = this.get("frame");
    div.removeChild(frame);

    // Remove the contents from this infoWindow
    var contentFrame = frame.firstChild;
    var content = contentFrame.firstChild;
    content.innerHTML = "";
};

InfoWindow.prototype.open = function(marker) {
    var map = marker.getMap();
    map.bindTo("infoPosition", this);
    var infoPosition = map.get("infoPosition");

    var div = map.getDiv();
    var divSize = common.getDivRect(div);

    var frame = this.get("frame");
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
    var frame = this.get("frame");
    div.appendChild(frame);

    // Adjust the infoWindow size
    var width = contentBox.clientWidth;
    this.set("width", width);
    var height = contentBox.clientHeight;
    this.set("height", height);
    contentFrame.style.width = width + "px";
    contentFrame.style.height = height + "px";
    frame.style.width = width + "px";
    frame.style.height = (height + 15) + "px";
};

module.exports = InfoWindow;
