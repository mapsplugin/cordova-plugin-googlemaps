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
    if(colorValue instanceof Array) {
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
    if (s == 0) {
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
    return !!element &&
        typeof element === "object" &&
        "getBoundingClientRect" in element;
}

function getPageRect() {
    var doc = document.documentElement;

    var pageWidth = window.innerWidth ||
        document.documentElement.clientWidth ||
        document.body.clientWidth;
    var pageHeight = window.innerHeight ||
        document.documentElement.clientHeight ||
        document.body.clientHeight;
    var pageLeft = (window.pageXOffset || doc.scrollLeft) - (doc.clientLeft || 0);
    var pageTop = (window.pageYOffset || doc.scrollTop) - (doc.clientTop || 0);

    return {
        'width': pageWidth,
        'height': pageHeight,
        'left': pageLeft,
        'top': pageTop
    };
}

function getDivRect(div) {
    if (!div) {
        return;
    }
    var scrollBarWidth = 16;
    var ratio = ((document.body.clientWidth + scrollBarWidth) / window.innerWidth);

    // The number "8" indicates the default marginTop in pixel of the <body> tag.
    // If you specify "document.body.style.margin=0;",
    // div.getBoundingClientRect() returns incorrect top position.
    // In order to fix it, the code needs to consider the body offset.

    var bodyOffsets = document.body.getBoundingClientRect();
    var pageRect = getPageRect();
    var rect = div.getBoundingClientRect();
    var divRect = {
        'left': rect.left + pageRect.left,
        'top': rect.top + pageRect.top + (bodyOffsets.top - 8),
        'width': rect.width,
        'height': rect.height
    };

    return {
          left : divRect.left * ratio,
          top : divRect.top * ratio,
          width : divRect.width * ratio,
          height : divRect.height * ratio
        };
}

function getAllChildren(root) {
    var list = [];
    var clickable;
    var style, displayCSS, opacityCSS, visibilityCSS;
    var search = function(node) {
        while (node != null) {
            if (node.nodeType == 1) {
                style = window.getComputedStyle(node);
                visibilityCSS = style.getPropertyValue('visibility');
                displayCSS = style.getPropertyValue('display');
                opacityCSS = style.getPropertyValue('opacity');
                if (displayCSS !== "none" && opacityCSS > 0 && visibilityCSS != "hidden") {
                    clickable = node.getAttribute("data-clickable");
                    if (clickable &&
                        clickable.toLowerCase() === "false" &&
                        node.hasChildNodes()) {
                        Array.prototype.push.apply(list, getAllChildren(node));
                    } else {
                        list.push(node);
                    }
                }
            }
            node = node.nextSibling;
        }
    };
    for (var i = 0; i < root.childNodes.length; i++) {
        search(root.childNodes[i]);
    }
    return list;
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


module.exports = {
  getDivRect: getDivRect,
  getPageRect: getPageRect,
  getAllChildren: getAllChildren,
  isDom: isDom,
  parseBoolean: parseBoolean,
  HLStoRGB: HLStoRGB,
  HTMLColor2RGBA: HTMLColor2RGBA,
  isHTMLColorString: isHTMLColorString
};
