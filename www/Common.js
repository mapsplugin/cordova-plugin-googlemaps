var BaseArrayClass = require('./BaseArrayClass');
var utils = require("cordova/utils");

var resolvedPromise = typeof Promise == 'undefined' ? null : Promise.resolve();
var nextTick = resolvedPromise ? function(fn) { resolvedPromise.then(fn); } : function(fn) { setTimeout(fn); };

//---------------------------
// Convert HTML color to RGB
//---------------------------
function isHTMLColorString(inputValue) {
    if (!inputValue || typeof inputValue !== "string") {
        return false;
    }
    if (inputValue.match(/^#[0-9A-F]{3}$/i) ||
        inputValue.match(/^#[0-9A-F]{4}$/i) ||
        inputValue.match(/^#[0-9A-F]{6}$/i) ||
        inputValue.match(/^#[0-9A-F]{8}$/i) ||
        inputValue.match(/^rgba?\([\d,.\s]+\)$/) ||
        inputValue.match(/^hsla?\([\d%,.\s]+\)$/)) {
        return true;
    }

    inputValue = inputValue.toLowerCase();
    return inputValue in HTML_COLORS;
}

function HTMLColor2RGBA(colorValue, defaultOpacity) {
    defaultOpacity = !defaultOpacity ? 1.0 : defaultOpacity;
    if (colorValue instanceof Array) {
        return colorValue;
    }
    if (colorValue === "transparent" || !colorValue) {
        return [0, 0, 0, 0];
    }
    var alpha = Math.floor(255 * defaultOpacity),
        matches,
        result = {
            r: 0,
            g: 0,
            b: 0
        };
    var colorStr = colorValue.toLowerCase();
    if (colorStr in HTML_COLORS) {
        colorStr = HTML_COLORS[colorStr];
    }
    if (colorStr.match(/^#([0-9A-F]){3}$/i)) {
        matches = colorStr.match(/([0-9A-F])/ig);

        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            alpha
        ];
    }

    if (colorStr.match(/^#[0-9A-F]{4}$/i)) {
        alpha = colorStr.substr(4, 1);
        alpha = parseInt(alpha + alpha, 16);

        matches = colorStr.match(/([0-9A-F])/ig);
        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            alpha
        ];
    }

    if (colorStr.match(/^#[0-9A-F]{6}$/i)) {
        matches = colorStr.match(/([0-9A-F]{2})/ig);
        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            alpha
        ];
    }
    if (colorStr.match(/^#[0-9A-F]{8}$/i)) {
        matches = colorStr.match(/([0-9A-F]{2})/ig);

        return [
            parseInt(matches[0], 16),
            parseInt(matches[1], 16),
            parseInt(matches[2], 16),
            parseInt(matches[3], 16)
        ];
    }
    // convert rgb(), rgba()
    if (colorStr.match(/^rgba?\([\d,.\s]+\)$/)) {
        matches = colorStr.match(/([\d.]+)/g);
        alpha = matches.length == 4 ? Math.floor(parseFloat(matches[3]) * 256) : alpha;
        return [
            parseInt(matches[0], 10),
            parseInt(matches[1], 10),
            parseInt(matches[2], 10),
            alpha
        ];
    }


    // convert hsl(), hsla()
    if (colorStr.match(/^hsla?\([\d%,.\s]+\)$/)) {
        matches = colorStr.match(/([\d%.]+)/g);
        alpha = matches.length == 4 ? Math.floor(parseFloat(matches[3]) * 256) : alpha;
        var rgb = HLStoRGB(matches[0], matches[1], matches[2]);
        rgb.push(alpha);
        return rgb;
    }

    console.log("Warning: '" + colorValue + "' is not available. The overlay is drew by black.");
    return [0, 0, 0, alpha];
}

/**
 * http://d.hatena.ne.jp/ja9/20100907/1283840213
 */
function HLStoRGB(h, l, s) {
    var r, g, b; // 0..255

    while (h < 0) {
        h += 360;
    }
    h = h % 360;

    // In case of saturation = 0
    if (s === 0) {
        // RGB are the same as V
        l = Math.round(l * 255);
        return [l, l, l];
    }

    var m2 = (l < 0.5) ? l * (1 + s) : l + s - l * s,
        m1 = l * 2 - m2,
        tmp;

    tmp = h + 120;
    if (tmp > 360) {
        tmp = tmp - 360;
    }

    if (tmp < 60) {
        r = (m1 + (m2 - m1) * tmp / 60);
    } else if (tmp < 180) {
        r = m2;
    } else if (tmp < 240) {
        r = m1 + (m2 - m1) * (240 - tmp) / 60;
    } else {
        r = m1;
    }

    tmp = h;
    if (tmp < 60) {
        g = m1 + (m2 - m1) * tmp / 60;
    } else if (tmp < 180) {
        g = m2;
    } else if (tmp < 240) {
        g = m1 + (m2 - m1) * (240 - tmp) / 60;
    } else {
        g = m1;
    }

    tmp = h - 120;
    if (tmp < 0) {
        tmp = tmp + 360;
    }
    if (tmp < 60) {
        b = m1 + (m2 - m1) * tmp / 60;
    } else if (tmp < 180) {
        b = m2;
    } else if (tmp < 240) {
        b = m1 + (m2 - m1) * (240 - tmp) / 60;
    } else {
        b = m1;
    }
    return [Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)];
}

function parseBoolean(boolValue) {
    return typeof(boolValue) === "string" && boolValue.toLowerCase() === "true" ||
        boolValue === true ||
        boolValue === 1;
}

function isDom(element) {
    return element &&
        element.nodeType === Node.ELEMENT_NODE &&
        element instanceof SVGElement === false &&
        typeof element.getBoundingClientRect === "function";
}

function getDivRect(div) {
    if (!div) {
        return;
    }
    var rect;
    if (div === document.body) {
      rect = div.getBoundingClientRect();
      rect.left = Math.max(rect.left, window.pageOffsetX);
      rect.top = Math.max(rect.top, window.pageOffsetY);
      rect.width = Math.max(rect.width, window.innerWidth);
      rect.height = Math.max(rect.height, window.innerHeight);
      rect.right = rect.left + rect.width;
      rect.bottom = rect.top + rect.height;
    } else {
      rect = div.getBoundingClientRect();
      if ("right" in rect === false) {
        rect.right = rect.left + rect.width;
      }
      if ("bottom" in rect === false) {
        rect.bottom = rect.top + rect.height;
      }
    }
    return {
      left: rect.left,
      top: rect.top,
      width: rect.width,
      height: rect.height,
      right: rect.right,
      bottom: rect.bottom
    };
}

var ignoreTags = [
  "pre", "textarea", "p", "form", "input", "caption", "canvas", "svg"
];


function shouldWatchByNative(node) {
  if (!node || node.nodeType !== Node.ELEMENT_NODE || !node.parentNode || node instanceof SVGElement) {
    if (node === document.body) {
      return true;
    }
    return false;
  }

  var tagName = node.tagName.toLowerCase();
  if (ignoreTags.indexOf(tagName) > -1) {
    return false;
  }

  var classNames = (node.className || "").split(" ");
  if (classNames.indexOf("_gmaps_cdv_") > -1) {
    return true;
  }

  var visibilityCSS = getStyle(node, 'visibility');
  var displayCSS = getStyle(node, 'display');

  // Do not check this at here.
  //var pointerEventsCSS = getStyle(node, 'pointer-events');

  //-----------------------------------------
  // no longer check the opacity property,
  // because the app might start changing the opacity later.
  //-----------------------------------------
  //var opacityCSS = getStyle(node, 'opacity');
  //opacityCSS = /^[\d.]+$/.test(opacityCSS + "") ? opacityCSS : 1;

  //-----------------------------------------
  // no longer check the clickable size,
  // because HTML still can display larger element inside one small element.
  //-----------------------------------------
  // var clickableSize = (
  //   node.offsetHeight > 0 && node.offsetWidth > 0 ||
  //   node.clientHeight > 0 && node.clientWidth > 0);
  return displayCSS !== "none" &&
    visibilityCSS !== "hidden";
}


// Get z-index order
// http://stackoverflow.com/a/24136505
var internalCache = {};
function _clearInternalCache() {
  internalCache = undefined;
  internalCache = {};
}
function _removeCacheById(elemId) {
  delete internalCache[elemId];
}
function getZIndex(dom) {
    if (dom === document.body) {
      internalCache = undefined;
      internalCache = {};
    }
    if (!dom) {
      return 0;
    }

    var z = 0;
    if (window.getComputedStyle) {
      z = document.defaultView.getComputedStyle(dom, null).getPropertyValue('z-index');
    }
    if (dom.currentStyle) {
        z = dom.currentStyle['z-index'];
    }
    var elemId = dom.getAttribute("__pluginDomId");
    var parentNode = dom.parentNode;
    var parentZIndex = 0;
    if (parentNode && parentNode.nodeType === Node.ELEMENT_NODE) {
      var parentElemId = parentNode.getAttribute("__pluginDomId");
      if (parentElemId in internalCache) {
        parentZIndex = internalCache[parentElemId];
      } else {
        parentZIndex = getZIndex(dom.parentNode);
        internalCache[parentElemId] = parentZIndex;
      }
    }

    var isInherit = false;
    if (z === "unset" || z === "initial") {
      z = 0;
    } else if (z === "auto" || z === "inherit") {
      z = 0;
      isInherit = true;
    } else {
      z = parseInt(z);
    }
    //dom.setAttribute("__ZIndex", z);
    internalCache[elemId] = z + parentZIndex;
    return {
      isInherit: isInherit,
      z: z
    };
}

// Get CSS value of an element
// http://stackoverflow.com/a/1388022
function getStyle(element, styleProperty)
{
    if (window.getComputedStyle) {
        return document.defaultView.getComputedStyle(element,null).getPropertyValue(styleProperty);
    } else if (element.currentStyle) {
      return element.currentStyle[styleProperty];
    }
    return;
}

function getDomInfo(dom, idx) {
    return {
        size: getDivRect(dom),
        depth: getDomDepth(dom, idx)
    };
}

var HTML_COLORS = {
    "aliceblue": "#f0f8ff",
    "antiquewhite": "#faebd7",
    "aqua": "#00ffff",
    "aquamarine": "#7fffd4",
    "azure": "#f0ffff",
    "beige": "#f5f5dc",
    "bisque": "#ffe4c4",
    "black": "#000000",
    "blanchedalmond": "#ffebcd",
    "blue": "#0000ff",
    "blueviolet": "#8a2be2",
    "brown": "#a52a2a",
    "burlywood": "#deb887",
    "cadetblue": "#5f9ea0",
    "chartreuse": "#7fff00",
    "chocolate": "#d2691e",
    "coral": "#ff7f50",
    "cornflowerblue": "#6495ed",
    "cornsilk": "#fff8dc",
    "crimson": "#dc143c",
    "cyan": "#00ffff",
    "darkblue": "#00008b",
    "darkcyan": "#008b8b",
    "darkgoldenrod": "#b8860b",
    "darkgray": "#a9a9a9",
    "darkgrey": "#a9a9a9",
    "darkgreen": "#006400",
    "darkkhaki": "#bdb76b",
    "darkmagenta": "#8b008b",
    "darkolivegreen": "#556b2f",
    "darkorange": "#ff8c00",
    "darkorchid": "#9932cc",
    "darkred": "#8b0000",
    "darksalmon": "#e9967a",
    "darkseagreen": "#8fbc8f",
    "darkslateblue": "#483d8b",
    "darkslategray": "#2f4f4f",
    "darkslategrey": "#2f4f4f",
    "darkturquoise": "#00ced1",
    "darkviolet": "#9400d3",
    "deeppink": "#ff1493",
    "deepskyblue": "#00bfff",
    "dimgray": "#696969",
    "dimgrey": "#696969",
    "dodgerblue": "#1e90ff",
    "firebrick": "#b22222",
    "floralwhite": "#fffaf0",
    "forestgreen": "#228b22",
    "fuchsia": "#ff00ff",
    "gainsboro": "#dcdcdc",
    "ghostwhite": "#f8f8ff",
    "gold": "#ffd700",
    "goldenrod": "#daa520",
    "gray": "#808080",
    "grey": "#808080",
    "green": "#008000",
    "greenyellow": "#adff2f",
    "honeydew": "#f0fff0",
    "hotpink": "#ff69b4",
    "indianred ": "#cd5c5c",
    "indigo  ": "#4b0082",
    "ivory": "#fffff0",
    "khaki": "#f0e68c",
    "lavender": "#e6e6fa",
    "lavenderblush": "#fff0f5",
    "lawngreen": "#7cfc00",
    "lemonchiffon": "#fffacd",
    "lightblue": "#add8e6",
    "lightcoral": "#f08080",
    "lightcyan": "#e0ffff",
    "lightgoldenrodyellow": "#fafad2",
    "lightgray": "#d3d3d3",
    "lightgrey": "#d3d3d3",
    "lightgreen": "#90ee90",
    "lightpink": "#ffb6c1",
    "lightsalmon": "#ffa07a",
    "lightseagreen": "#20b2aa",
    "lightskyblue": "#87cefa",
    "lightslategray": "#778899",
    "lightslategrey": "#778899",
    "lightsteelblue": "#b0c4de",
    "lightyellow": "#ffffe0",
    "lime": "#00ff00",
    "limegreen": "#32cd32",
    "linen": "#faf0e6",
    "magenta": "#ff00ff",
    "maroon": "#800000",
    "mediumaquamarine": "#66cdaa",
    "mediumblue": "#0000cd",
    "mediumorchid": "#ba55d3",
    "mediumpurple": "#9370db",
    "mediumseagreen": "#3cb371",
    "mediumslateblue": "#7b68ee",
    "mediumspringgreen": "#00fa9a",
    "mediumturquoise": "#48d1cc",
    "mediumvioletred": "#c71585",
    "midnightblue": "#191970",
    "mintcream": "#f5fffa",
    "mistyrose": "#ffe4e1",
    "moccasin": "#ffe4b5",
    "navajowhite": "#ffdead",
    "navy": "#000080",
    "oldlace": "#fdf5e6",
    "olive": "#808000",
    "olivedrab": "#6b8e23",
    "orange": "#ffa500",
    "orangered": "#ff4500",
    "orchid": "#da70d6",
    "palegoldenrod": "#eee8aa",
    "palegreen": "#98fb98",
    "paleturquoise": "#afeeee",
    "palevioletred": "#db7093",
    "papayawhip": "#ffefd5",
    "peachpuff": "#ffdab9",
    "peru": "#cd853f",
    "pink": "#ffc0cb",
    "plum": "#dda0dd",
    "powderblue": "#b0e0e6",
    "purple": "#800080",
    "rebeccapurple": "#663399",
    "red": "#ff0000",
    "rosybrown": "#bc8f8f",
    "royalblue": "#4169e1",
    "saddlebrown": "#8b4513",
    "salmon": "#fa8072",
    "sandybrown": "#f4a460",
    "seagreen": "#2e8b57",
    "seashell": "#fff5ee",
    "sienna": "#a0522d",
    "silver": "#c0c0c0",
    "skyblue": "#87ceeb",
    "slateblue": "#6a5acd",
    "slategray": "#708090",
    "slategrey": "#708090",
    "snow": "#fffafa",
    "springgreen": "#00ff7f",
    "steelblue": "#4682b4",
    "tan": "#d2b48c",
    "teal": "#008080",
    "thistle": "#d8bfd8",
    "tomato": "#ff6347",
    "turquoise": "#40e0d0",
    "violet": "#ee82ee",
    "wheat": "#f5deb3",
    "white": "#ffffff",
    "whitesmoke": "#f5f5f5",
    "yellow": "#ffff00",
    "yellowgreen": "#9acd32"
};

function defaultTrueOption(value) {
    return value === undefined ? true : value === true;
}

function createMvcArray(array) {
    if (!array) {
      return new BaseArrayClass();
    }
    if (array.type === "BaseArrayClass") {
      return array;
    }

    var mvcArray;
    if (array.type === "LatLngBounds") {
      array = [
          array.southwest,
          {lat: array.northeast.lat, lng: array.southwest.lng},
          array.northeast,
          {lat: array.southwest.lat, lng: array.northeast.lng},
          array.southwest
        ];
      array = array.map(getLatLng);
    }

    if (array && typeof array.getArray === "function") {
        mvcArray = new BaseArrayClass(array.getArray());
        array.on('set_at', function(index) {
            var value = array.getAt(index);
            value = "position" in value ? value.getPosition() : value;
            mvcArray.setAt(index, value);
        });
        array.on('insert_at', function(index) {
            var value = array.getAt(index);
            value = "position" in value ? value.getPosition() : value;
            mvcArray.insertAt(index, value);
        });
        array.on('remove_at', function(index) {
            mvcArray.removeAt(index);
        });

    } else {
        mvcArray = new BaseArrayClass(!!array ? array.slice(0) : undefined);
    }
    return mvcArray;
}

function getLatLng(target) {
  return "getPosition" in target ? target.getPosition() : {
    "lat": parseFloat(target.lat, 10),
    "lng": parseFloat(target.lng, 10)
  };
}
function convertToPositionArray(array) {
  array = array || [];

  if (!utils.isArray(array)) {
    if (array.type === "LatLngBounds") {
      array = [
        array.southwest,
        {lat: array.northeast.lat, lng: array.southwest.lng},
        array.northeast,
        {lat: array.southwest.lat, lng: array.northeast.lng},
        array.southwest
      ];
    } else if (array && typeof array.getArray === "function") {
      array = array.getArray();
    } else {
      array = [array];
    }
  }

  array = array.map(getLatLng);

  return array;
}

function markerOptionsFilter(markerOptions) {
  markerOptions = markerOptions || {};

  markerOptions.animation = markerOptions.animation || undefined;
  markerOptions.position = markerOptions.position || {};
  markerOptions.position.lat = markerOptions.position.lat || 0.0;
  markerOptions.position.lng = markerOptions.position.lng || 0.0;
  markerOptions.draggable = markerOptions.draggable === true;
  markerOptions.icon = markerOptions.icon || undefined;
  markerOptions.zIndex = markerOptions.zIndex || 0;
  markerOptions.snippet = typeof(markerOptions.snippet) === "string" ? markerOptions.snippet : undefined;
  markerOptions.title = typeof(markerOptions.title) === "string" ? markerOptions.title : undefined;
  markerOptions.visible = defaultTrueOption(markerOptions.visible);
  markerOptions.flat = markerOptions.flat === true;
  markerOptions.rotation = markerOptions.rotation || 0;
  markerOptions.opacity = markerOptions.opacity === 0 ? 0 : (parseFloat("" + markerOptions.opacity, 10) || 1);
  markerOptions.disableAutoPan = markerOptions.disableAutoPan === true;
  markerOptions.noCache = markerOptions.noCache === true; //experimental
  if (typeof markerOptions.icon === "object") {
    if ("anchor" in markerOptions.icon &&
      !Array.isArray(markerOptions.icon.anchor) &&
      "x" in markerOptions.icon.anchor &&
      "y" in markerOptions.icon.anchor) {
      markerOptions.icon.anchor = [markerOptions.icon.anchor.x, markerOptions.icon.anchor.y];
    }
  }

  if ("infoWindowAnchor" in markerOptions &&
    !Array.isArray(markerOptions.infoWindowAnchor) &&
    "x" in markerOptions.infoWindowAnchor &&
    "y" in markerOptions.infoWindowAnchor) {
    markerOptions.infoWindowAnchor = [markerOptions.infoWindowAnchor.x, markerOptions.infoWindowAnchor.anchor.y];
  }

  if ("style" in markerOptions && !("styles" in markerOptions)) {
    markerOptions.styles = markerOptions.style;
    delete markerOptions.style;
  }
  if ("styles" in markerOptions) {
      markerOptions.styles = typeof markerOptions.styles === "object" ? markerOptions.styles : {};

      if ("color" in markerOptions.styles) {
          markerOptions.styles.color = HTMLColor2RGBA(markerOptions.styles.color || "#000000");
      }
  }
  if (markerOptions.icon && isHTMLColorString(markerOptions.icon)) {
      markerOptions.icon = HTMLColor2RGBA(markerOptions.icon);
  }
  if (markerOptions.icon && markerOptions.icon.label &&
    isHTMLColorString(markerOptions.icon.label.color)) {
      markerOptions.icon.label.color = HTMLColor2RGBA(markerOptions.icon.label.color);
  }
  return markerOptions;
}

function quickfilter(domPositions, mapElemIDs) {
  //console.log("before", JSON.parse(JSON.stringify(domPositions)));
  var keys = Object.keys(domPositions);

  var tree = {};
  mapElemIDs.forEach(function(mapElemId) {
    var size = domPositions[mapElemId].size;
    var mapRect = {
      left: size.left,
      top: size.top,
      right: size.left + size.width,
      bottom: size.top + size.height
    };

    tree[mapElemId] = domPositions[mapElemId];

    keys.forEach(function(elemId) {
      if (domPositions[elemId].ignore) {
        return;
      }
      var domSize = {
        left: domPositions[elemId].size.left,
        top: domPositions[elemId].size.top,
        right: domPositions[elemId].size.left + domPositions[elemId].size.width,
        bottom: domPositions[elemId].size.bottom + domPositions[elemId].size.height
      };
      if (
          (domSize.left >= mapRect.left && domSize.left <= mapRect.right) ||
          (domSize.right >= mapRect.left && domSize.right <= mapRect.right) ||
          (domSize.top >= mapRect.top && domSize.top <= mapRect.bottom) ||
          (domSize.bottom >= mapRect.top && domSize.bottom <= mapRect.bottom)
        ) {
        tree[elemId] = domPositions[elemId];
      }
    });
  });

  //console.log("after", JSON.parse(JSON.stringify(tree)));
  return tree;
}

function getPluginDomId(element) {
  // Generates a __pluginDomId
  if (!element || !shouldWatchByNative(element)) {
    return;
  }
  var elemId = element.getAttribute("__pluginDomId");
  if (!elemId) {
    if (element === document.body) {
      elemId = "root";
    } else {
      elemId = "pgm" + Math.floor(Math.random() * Date.now());
    }
    element.setAttribute("__pluginDomId", elemId);
  }
  return elemId;
}

// Add hashCode() method
// https://stackoverflow.com/a/7616484/697856
function hashCode(text) {
  var hash = 0, i, chr;
  if (text.length === 0) return hash;
  for (i = 0; i < text.length; i++) {
    chr   = text.charCodeAt(i);
    hash  = ((hash << 5) - hash) + chr;
    hash |= 0; // Convert to 32bit integer
  }
  return hash;
}

function createEvent(eventName, properties) {
  var evt;
  if (typeof CustomEvent === 'function') {
    evt = new CustomEvent(eventName, {
      bubbles: true,
      detail: properties || null
    });
  } else {
    evt = document.createEvent('Event');
    evt.initEvent(eventName, true, false);
    Object.keys(properties).forEach(function(key) {
      if (!(key in properties)) {
        evt[key] = properties[key];
      }
    });
  }
  return evt;
}

module.exports = {
    _clearInternalCache: _clearInternalCache,
    _removeCacheById: _removeCacheById,
    getZIndex: getZIndex,
    getDivRect: getDivRect,
    getDomInfo: getDomInfo,
    isDom: isDom,
    parseBoolean: parseBoolean,
    HLStoRGB: HLStoRGB,
    HTMLColor2RGBA: HTMLColor2RGBA,
    isHTMLColorString: isHTMLColorString,
    defaultTrueOption: defaultTrueOption,
    createMvcArray: createMvcArray,
    getStyle: getStyle,
    convertToPositionArray: convertToPositionArray,
    getLatLng: getLatLng,
    shouldWatchByNative: shouldWatchByNative,
    markerOptionsFilter: markerOptionsFilter,
    quickfilter: quickfilter,
    nextTick: nextTick,
    getPluginDomId: getPluginDomId,
    hashCode: hashCode,
    createEvent: createEvent
};

if (cordova && cordova.platformId === "browser") {
  require('cordova/exec/proxy').add('common', module.exports);
}
