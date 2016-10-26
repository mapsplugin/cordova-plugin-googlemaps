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


    var box = document.createElement("div");
    box.style.backgroundColor="white";
    box.style.overflow="visible";
    box.style.border="1px solid red";
    box.style.position="absolute";
    box.style.display = "none";
    box.style.display = "inline-block";
    this.set("box", box);


    this.on("infoPosition_changed", function(ignore, point) {
      box.style.left = (point.x - 10) + "px";
      box.style.top = (point.y - 60)  + "px";
    });

};

utils.extend(InfoWindow, BaseClass);

InfoWindow.prototype.close = function(marker) {
  console.log("--->close");
    var map = marker.getMap();
    map.off("infoPosition_changed");
    var map = marker.getMap();
    var div = map.getDiv();
    var box = this.get("box");
    div.removeChild(box);
    box.removeChild(box.firstChild);
};

InfoWindow.prototype.open = function(marker) {
  console.log("--->open");
    var map = marker.getMap();
    map.bindTo("infoPosition", this);
    var infoPosition = map.get("infoPosition");
      console.log(infoPosition);

    var div = map.getDiv();
    var divSize = common.getDivRect(div);

    var content = document.createElement("div");
    content.style.display = "inline-block";
    content.style.maxHeight = (divSize.height / 3).toFixed(0) + "px";
    content.style.maxWidth = (divSize.width / 3).toFixed(0) + "px";
    var title = marker.get("title");
    if (typeof title === "string") {
      content.style.whiteSpace="nowrap";
      content.innerHTML = title;
    } else {
      content.appendChild(title);
    }
    var box = this.get("box");
    box.appendChild(content);
    div.appendChild(box);
    box.style.width = (content.clientWidth + 20) + "px"
    box.style.height = (content.clientHeight + 20) + "px"
};

module.exports = InfoWindow;
