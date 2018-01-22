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
    var zoomScale = parseFloat(window.devicePixelRatio);
    zoomScale = 1;
    var callbackTable = {};
    var listenerMgr = {
      one: function(target, eventName, callback) {
        callbackTable[target.hashCode] = callbackTable[target.hashCode] || {};
        callbackTable[target.hashCode][eventName] = callbackTable[target.hashCode][eventName] || [];
        callbackTable[target.hashCode][eventName].push(callback);
        target.one.call(target, eventName, callback);
      },
      on: function(target, eventName, callback) {
        callbackTable[target.hashCode] = callbackTable[target.hashCode] || {};
        callbackTable[target.hashCode][eventName] = callbackTable[target.hashCode][eventName] || [];
        callbackTable[target.hashCode][eventName].push(callback);
        target.on.call(target, eventName, callback);
      },
      bindTo: function(srcObj, srcField, dstObj, dstField, noNotify) {
        var eventName = srcField + "_changed";
        dstField = dstField || srcField;
        var callback = function(oldValue, newValue) {
          dstObj.set(dstField, newValue, noNotify);
        };
        callbackTable[dstObj.hashCode] = callbackTable[dstObj.hashCode] || {};
        callbackTable[dstObj.hashCode][eventName] = callbackTable[dstObj.hashCode][eventName] || [];
        callbackTable[dstObj.hashCode][eventName].push(callback);
        srcObj.on.call(srcObj, eventName, callback);
      },
      off: function(target, eventName) {
        if (!target || !eventName ||
          !(target.hashCode in callbackTable) ||
          !(eventName in callbackTable[target.hashCode])) {
          return;
        }
        callbackTable[target.hashCode][eventName].forEach(function(listener) {
          target.off.call(target, eventName, listener);
        });
        delete callbackTable[target.hashCode][eventName];
      }
    };


    Object.defineProperty(self, "_hook", {
        value: listenerMgr,
        writable: false
    });


    var frame = document.createElement("div");
    frame.style.overflow="visible";
    frame.style.position="absolute";
    frame.style.display = "inline-block";
    frame.classList.add('pgm-html-info-frame');
    self.set("frame", frame);

    var anchorDiv = document.createElement("div");
    anchorDiv.setAttribute("class", "pgm-anchor");
    anchorDiv.style.overflow="visible";
    anchorDiv.style.position="absolute";
    //anchorDiv.style.display = "inline-block";
    anchorDiv.style.width = '1px';
    anchorDiv.style.height = '1px';
    //anchorDiv.style.border = "1px solid green";
    anchorDiv.style.overflow = "visible";

    anchorDiv.style.transition = "transform 0s ease";
    anchorDiv.style['will-change'] = "transform";

    anchorDiv.style['-webkit-backface-visibility'] = 'hidden';
    anchorDiv.style['-webkit-perspective'] = 1000;
    anchorDiv.style['-webkit-transition'] = "-webkit-transform 0s ease";

    anchorDiv.appendChild(frame);
    self.set("anchor", anchorDiv);


    var contentBox = document.createElement("div");
    contentBox.style.display = "inline-block";
    contentBox.style.padding = "5px";
    contentBox.classList.add('pgm-html-info-content-box');

    var contentFrame = document.createElement("div");
    contentFrame.style.display = "block";
    contentFrame.style.position = "relative";
    contentFrame.style.backgroundColor = "white";
    contentFrame.style.border = "1px solid rgb(204, 204, 204)";
    contentFrame.style.left = "0px";
    contentFrame.style.right = "0px";
    contentFrame.style.zIndex = "1";  // In order to set higher depth than the map div certainly
    contentFrame.classList.add('pgm-html-info-content-frame');
    frame.appendChild(contentFrame);
    contentFrame.appendChild(contentBox);

    var tailFrame = document.createElement("div");
    tailFrame.style.position = "relative";
    tailFrame.style.top = "-1px";
    tailFrame.style.zIndex = 100;
    tailFrame.classList.add('pgm-html-info-tail-frame');
    frame.appendChild(tailFrame);

    var tailLeft = document.createElement("div");
    /*
    tailLeft.style.position = "absolute";
    tailLeft.style.marginLeft = "-15px";
    tailLeft.style.left = "50%";
    tailLeft.style.top = "0px";
    tailLeft.style.height = "15px";
    tailLeft.style.width = "16px";
    tailLeft.style.overflow = "hidden";
    tailLeft.style.borderWidth = "0px";
    */
    tailLeft.classList.add('pgm-html-info-tail-left');

    tailLeft.style.position = "absolute";
    tailLeft.style.left = "50%";
    tailLeft.style.height = "0px";
    tailLeft.style.width = "0px";
    tailLeft.style.marginLeft = "-15px";
    tailLeft.style.borderWidth = "15px 15px 0px";
    tailLeft.style.borderColor = "rgb(204, 204, 204) transparent transparent";
    tailLeft.style.borderStyle = "solid";
    tailFrame.appendChild(tailLeft);

    /*
    var tailLeftCover = document.createElement("div");
    tailLeftCover.style.position = "absolute";
    tailLeftCover.style.backgroundColor = "white";
    tailLeftCover.style.transform = "skewX(45deg)";
    tailLeftCover.style.transformOrigin = "0px 0px 0px";
    tailLeftCover.style.left = "0px";
    tailLeftCover.style.height = "15px";
    tailLeftCover.style.width = "15px";
    tailLeftCover.style.top = "0px";
    tailLeftCover.style.zIndex = 1;
    tailLeftCover.style.borderLeft = "1px solid rgb(204, 204, 204)";
    tailLeft.classList.add('pgm-html-info-tail-left-cover');
    tailLeft.appendChild(tailLeftCover);
    */


    var tailRight = document.createElement("div");
    /*
    tailRight.style.position = "absolute";
    tailRight.style.left = "50%";
    tailRight.style.top = "0px";
    tailRight.style.height = "15px";
    tailRight.style.width = "16px";
    tailRight.style.overflow = "hidden";
    tailRight.style.borderWidth = "0px";
    */
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
    tailRight.classList.add('pgm-html-info-tail-right');
    tailFrame.appendChild(tailRight);
/*
    var tailRightCover = document.createElement("div");
    tailRightCover.style.position = "absolute";
    tailRightCover.style.backgroundColor = "white";
    tailRightCover.style.transform = "skewX(-45deg)";
    tailRightCover.style.transformOrigin = "0px 0px 0px";
    tailRightCover.style.left = "0px";
    tailRightCover.style.height = "15px";
    tailRightCover.style.width = "15px";
    tailRightCover.style.top = "0px";
    tailRightCover.style.zIndex = 2;
    tailRightCover.style.borderRight = "1px solid rgb(204, 204, 204)";
    tailRightCover.classList.add('pgm-html-info-tail-right-cover');
    tailRight.appendChild(tailRightCover);
*/
    var eraseBorder = document.createElement("div");
    eraseBorder.style.position = "absolute";
    eraseBorder.style.zIndex = 3;
    eraseBorder.style.backgroundColor = "white";
    eraseBorder.style.width = "27px";
    eraseBorder.style.height = "2px";
    eraseBorder.style.top = "-1px";
    eraseBorder.style.left = "50%";
    eraseBorder.style.marginLeft = "-13px";
    eraseBorder.classList.add('pgm-html-info-tail-erase-border');
    tailFrame.appendChild(eraseBorder);

    var calculate = function() {

      var marker = self.get("marker");
      var map = marker.getMap();

      var div = map.getDiv();

      var frame = self.get("frame");
      var contentFrame = frame.firstChild;
      var contentBox = contentFrame.firstChild;
      contentBox.style.minHeight = "50px";
      contentBox.style.width = (div.offsetWidth * 0.7) + "px";


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

      var cssOptions = self.get("cssOptions");
      if (cssOptions && typeof cssOptions === "object") {
        var keys = Object.keys(cssOptions);
        keys.forEach(function(key) {
          contentBox.style.setProperty(key, cssOptions[key]);
        });
      }
      // Insert the contents to this HTMLInfoWindow
      if (!anchorDiv.parentNode) {
          div.appendChild(anchorDiv);
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
      //console.log("contentWidth = " + contentBox.offsetWidth + ", contentsHeight = " + contentsHeight);

      var infoOffset = {
        x : 31,
        y : 31
      };
      var iconSize = {
        width: 62,
        height: 110
      };

      // If there is no specification with `anchor` property,
      // the values {x: 0.5, y: 1} are specified by native APIs.
      // For the case, anchor values are {x: 0} in JS.
      var anchor = {
        x: 15,
        y: 15
      };

      anchor.x /= zoomScale;
      anchor.y /= zoomScale;
      infoOffset.x /= zoomScale;
      infoOffset.y /= zoomScale;
      iconSize.width /= zoomScale;
      iconSize.height /= zoomScale;

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

          if (Array.isArray(icon.anchor)) {
            anchor.x = icon.anchor[0];
            anchor.y = icon.anchor[1];
          }
      }

      var infoWindowAnchor = marker.get("infoWindowAnchor");
      if (utils.isArray(infoWindowAnchor)) {
        infoOffset.x = infoWindowAnchor[0];
        infoOffset.y = infoWindowAnchor[1];
      }
      infoOffset.x = infoOffset.x / iconSize.width;
      infoOffset.x = infoOffset.x > 1 ? 1 : infoOffset.x;
      infoOffset.x = infoOffset.x < 0 ? 0 : infoOffset.x;
      infoOffset.y = infoOffset.y / iconSize.height;
      infoOffset.y = infoOffset.y > 1 ? 1 : infoOffset.y;
      infoOffset.y = infoOffset.y < 0 ? 0 : infoOffset.y;
      infoOffset.y *= iconSize.height;
      infoOffset.x *= iconSize.width;

      anchor.x = anchor.x / iconSize.width;
      anchor.x = anchor.x > 1 ? 1 : anchor.x;
      anchor.x = anchor.x < 0 ? 0 : anchor.x;
      anchor.y = anchor.y / iconSize.height;
      anchor.y = anchor.y > 1 ? 1 : anchor.y;
      anchor.y = anchor.y < 0 ? 0 : anchor.y;
      anchor.y *= iconSize.height;
      anchor.x *= iconSize.width;



      //console.log("contentsSize = " + contentsWidth + ", " + contentsHeight);
      //console.log("iconSize = " + iconSize.width + ", " + iconSize.height);
      //console.log("infoOffset = " + infoOffset.x + ", " + infoOffset.y);

      var frameBorder = parseInt(common.getStyle(contentFrame, "border-left-width").replace(/[^\d]/g, ""), 10);
      //var offsetX = (contentsWidth + frameBorder + anchor.x ) * 0.5 + (iconSize.width / 2  - infoOffset.x);
      //var offsetY = contentsHeight + anchor.y - (frameBorder * 2) - infoOffset.y + 15;
      var offsetX = -(iconSize.width / 2);
      var offsetY = -iconSize.height;
      anchorDiv.style.width = iconSize.width + "px";
      anchorDiv.style.height = iconSize.height + "px";

      self.set("offsetX", offsetX);
      self.set("offsetY", offsetY);


      frame.style.bottom = (iconSize.height - infoOffset.y)+ "px";
      frame.style.left = ((-contentsWidth) / 2 + infoOffset.x)  + "px";


      //console.log("frameLeft = " + frame.style.left );
      var point = map.get("infoPosition");
      anchorDiv.style.visibility = "hidden";
      var x = point.x + self.get("offsetX");
      var y = point.y + self.get("offsetY");
      anchorDiv.style['-webkit-transform'] = "translate3d(" + x + "px, " + y + "px, 0px)";
      anchorDiv.style.transform = "translate3d(" + x + "px, " + y + "px, 0px)";
      anchorDiv.style.visibility = "visible";
      self.trigger("infoPosition_changed", "", point);
      self.trigger(event.INFO_OPEN);
    };

    self._hook.on(self, "infoPosition_changed", function(ignore, point) {
      var x = point.x + self.get("offsetX");
      var y = point.y + self.get("offsetY");
      anchorDiv.style['-webkit-transform'] = "translate3d(" + x + "px, " + y + "px, 0px)";
      anchorDiv.style.transform = "translate3d(" + x + "px, " + y + "px, 0px)";
    });

    self._hook.on(self, "infoWindowAnchor_changed", calculate);

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
      self._hook.off(marker, "isInfoWindowVisible_changed");
    }
    if (!self.isInfoWindowShown() || !marker) {
      return;
    }
    self.set("isInfoWindowVisible", false);
    marker.set("isInfoWindowVisible", false);
    marker.set("infoWindow", undefined);
    this.set('marker', undefined);

    var map = marker.getMap();
    self._hook.off(marker.getMap(), "map_clear");
    self._hook.off(marker, "infoPosition_changed");
    self._hook.off(marker, "icon_changed");
    //self._hook.off(self, "infoWindowAnchor_changed");
    self._hook.off(marker, event.INFO_CLOSE);  //This event listener is assigned in the open method. So detach it.
    self.trigger(event.INFO_CLOSE);
    map.set("active_marker_id", null);

    //var div = map.getDiv();
    var anchorDiv = self.get("anchor");
    if (anchorDiv && anchorDiv.parentNode) {
      anchorDiv.parentNode.removeChild(anchorDiv);

      // Remove the contents from this HTMLInfoWindow
      var contentFrame = anchorDiv.firstChild.firstChild;
      var contentBox = contentFrame.firstChild;
      contentBox.innerHTML = "";
    }
};

HTMLInfoWindow.prototype.setContent = function(content, cssOptions) {
    var self = this;
    var prevContent = self.get("content");
    self.set("content", content);
    self.set("cssOptions", cssOptions);
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
    self._hook.on(marker, "icon_changed", function() {
      self.trigger.call(self, "infoWindowAnchor_changed");
    });
    self.set("isInfoWindowVisible", true);
    self._hook.on(marker, "isInfoWindowVisible_changed", function(prevValue, newValue) {
      if (newValue === false) {
        self.close.call(self);
      }
    });

    map.fromLatLngToPoint(marker.getPosition(), function(point) {
        map.set("infoPosition", {x: point[0], y: point[1]});

        self._hook.bindTo(map, "infoPosition", self);
        self._hook.bindTo(marker, "infoWindowAnchor", self);
        self._hook.bindTo(marker, "icon", self);
        self._hook.one(marker.getMap(), "map_clear", self.close.bind(self));
        self._hook.one(marker, event.INFO_CLOSE, self.close.bind(self));
        self.set("marker", marker);
        map.set("active_marker_id", marker.getId());
        self.trigger.call(self, "infoWindowAnchor_changed");
    });
};

HTMLInfoWindow.prototype.setBackgroundColor = function(backgroundColor) {
  this.get("frame").children[0].style.backgroundColor = backgroundColor;
  this.get("frame").children[1].children[0].style.borderColor = backgroundColor + " rgba(0,0,0,0) rgba(0,0,0,0)";
  this.get("frame").children[1].children[1].style.borderColor = backgroundColor + " rgba(0,0,0,0) rgba(0,0,0,0)";
  this.get("frame").children[1].children[2].style.backgroundColor = backgroundColor;
};

module.exports = HTMLInfoWindow;
