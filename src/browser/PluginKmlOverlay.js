
var InlineWorker = require('cordova-plugin-googlemaps.InlineWorker');

function PluginKmlOverlay(pluginMap) {
  // stub
}

PluginKmlOverlay.prototype._create = function(onSuccess, onError, args) {
  var self = this,
    pluginOptions = args[1];

  if (!/^https?:/.test(location.protocol)) {
    return onError('KmlOverlay is only available on http: or https: protocols.');
  }

  //-------------------------------------
  // Parse the xml file using WebWorker
  //-------------------------------------
  var worker = new InlineWorker(loadKml);
  worker.onmessage = function(evt) {
    console.log('host message', evt.data);
    worker.terminate();
    onSuccess(evt.data);
  };
  worker.onerror = onError;
  var link = document.createElement("a");
  link.href = pluginOptions.url;
  var url = link.protocol+"//"+link.host+link.pathname+link.search;
  worker.postMessage({
    'url': url
  });
};

module.exports = PluginKmlOverlay;

function loadKml(self) {

  // code: https://stackoverflow.com/q/32912732/697856
  var createCORSRequest = function(method, url, asynch) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
      // XHR for Chrome/Firefox/Opera/Safari.
      xhr.open(method, url, asynch);
      // xhr.setRequestHeader('MEDIBOX', 'login');
      xhr.setRequestHeader('Content-Type', 'application/xml; charset=UTF-8');
    } else if (typeof XDomainRequest != "undefined") {
      // XDomainRequest for IE.
      xhr = new XDomainRequest();
      xhr.open(method, url, asynch);
    } else {
      // CORS not supported.
      xhr = null;
    }
    return xhr;
  };


  //---------------------------------------
  // modified fromXML (xml parser)
  //---------------------------------------
  var fromXML = (function() {
    // fromXML
    // https://github.com/kawanet/from-xml
    //
    // The MIT License (MIT)
    //
    // Copyright (c) 2016 Yusuke Kawasaki
    //
    // Permission is hereby granted, free of charge, to any person obtaining a copy
    // of this software and associated documentation files (the "Software"), to deal
    // in the Software without restriction, including without limitation the rights
    // to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    // copies of the Software, and to permit persons to whom the Software is
    // furnished to do so, subject to the following conditions:
    //
    // The above copyright notice and this permission notice shall be included in all
    // copies or substantial portions of the Software.
    //
    // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    // SOFTWARE.

    /**
     * The fromXML() method parses an XML string, constructing the JavaScript
     * value or object described by the string.
     *
     * @function fromXML
     * @param text {String} The string to parse as XML
     * @param [reviver] {Function} If a function, prescribes how the value
     * originally produced by parsing is transformed, before being returned.
     * @returns {Object}
     */

    var UNESCAPE = {
      "&amp;": "&",
      "&lt;": "<",
      "&gt;": ">",
      "&apos;": "'",
      "&quot;": '"'
    };

    var CHILD_NODE_KEY = "#";

    function parseXML(text) {
      var list = String.prototype.split.call(text, /<([^!<>?](?:'[\S\s]*?'|"[\S\s]*?"|[^'"<>])*|!(?:--[\S\s]*?--|\[[^\[\]'"<>]+\[[\S\s]*?]]|DOCTYPE[^\[<>]*?\[[\S\s]*?]|(?:ENTITY[^"<>]*?"[\S\s]*?")?[\S\s]*?)|\?[\S\s]*?\?)>/);
      var length = list.length;

      // root element
      var root = {f: []};
      var elem = root;

      // dom tree stack
      var stack = [];

      for (var i = 0; i < length;) {
        // text node
        var str = list[i++];
        if (str) appendText(str);

        // child node
        var tag = list[i++];
        if (tag) parseNode(tag);
      }

      return root;

      function parseNode(tag) {
        var tagLength = tag.length;
        var firstChar = tag[0];
        if (firstChar === "/") {
          // close tag
          var closed = tag.replace(/^\/|[\s\/].*$/g, "").toLowerCase();
          while (stack.length) {
            var tagName = elem.n && elem.n.toLowerCase();
            elem = stack.pop();
            if (tagName === closed) break;
          }
        // } else if (firstChar === "?") {
        //   // XML declaration
        //   appendChild({n: "?", r: tag.substr(1, tagLength - 2)});
        } else if (firstChar === "!") {
          if (tag.substr(1, 7) === "[CDATA[" && tag.substr(-2) === "]]") {
            // CDATA section
            appendText(tag.substr(8, tagLength - 10));
          } else {
            // comment
            appendChild({n: "!", r: tag.substr(1)});
          }
        } else {
          var child = openTag(tag);
          appendChild(child);
          if (tag[tagLength - 1] === "/") {
            child.c = 1; // emptyTag
          } else {
            stack.push(elem); // openTag
            elem = child;
          }
        }
      }

      function appendChild(child) {
        elem.f.push(child);
      }

      function appendText(str) {
        str = removeSpaces(str);
        if (str) appendChild(unescapeXML(str));
      }
    }

    function openTag(tag) {
      var elem = {f: []};
      tag = tag.replace(/\s*\/?$/, "");
      var pos = tag.search(/[\s='"\/]/);
      if (pos < 0) {
        elem.n = tag;
      } else {
        elem.n = tag.substr(0, pos);
        elem.t = tag.substr(pos);
      }
      return elem;
    }

    function parseAttribute(elem, reviver) {
      if (!elem.t) return;
      var list = elem.t.split(/([^\s='"]+(?:\s*=\s*(?:'[\S\s]*?'|"[\S\s]*?"|[^\s'"]*))?)/);
      var attributes = {};

      list.forEach(function(str) {
        var val;
        str = removeSpaces(str);
        if (!str) return;

        var pos = str.indexOf("=");
        if (pos < 0) {
          // bare attribute
          str = str;
          val = null;
        } else {
          // attribute key/value pair
          val = str.substr(pos + 1).replace(/^\s+/, "");
          str = str.substr(0, pos).replace(/\s+$/, "");

          // quote: foo="FOO" bar='BAR'
          var firstChar = val[0];
          var lastChar = val[val.length - 1];
          if (firstChar === lastChar && (firstChar === "'" || firstChar === '"')) {
            val = val.substr(1, val.length - 2);
          }

          val = unescapeXML(val);
        }
        if (reviver) {
          val = reviver(str, val);
        }
        addAttribute(attributes, str, val);
      });

      return attributes;
    }

    function removeSpaces(str) {
      return str && str.replace(/^\s+|\s+$/g, "");
    }

    function unescapeXML(str) {
      return str.replace(/(&(?:lt|gt|amp|apos|quot|#(?:\d{1,6}|x[0-9a-fA-F]{1,5}));)/g, function(str) {
        if (str[1] === "#") {
          var code = (str[2] === "x") ? parseInt(str.substr(3), 16) : parseInt(str.substr(2), 10);
          if (code > -1) return String.fromCharCode(code);
        }
        return UNESCAPE[str] || str;
      });
    }

    function toObject(elem, reviver) {
      //
      // var raw = elem.r;
      // if (raw) return raw;

      var attributes = parseAttribute(elem, reviver);
      var object;
      var childList = elem.f;
      var childLength = childList.length;

      if (attributes || childLength > 1) {
        // merge attributes and child nodes
        if (typeof attributes === 'object') {
          object = attributes;
          object.line=198;
        } else {
          object = {};
        }
        object.tagName = elem.n;
        childList.forEach(function(child) {
          if ("string" === typeof child) {
            addObject(object, CHILD_NODE_KEY, child);
          } else {
            addObject(object, child.n, toObject(child, reviver));
          }
        });
      } else if (childLength) {
        // the node has single child node but no attribute
        var child = childList[0];
        if ("string" === typeof child) {
          object = {
            'tagName': elem.n,
            'value': child,
            'line': 215
          };
        } else {
          object = toObject(child, reviver);
          if (child.n) {
            object = {
              'tagName': elem.n,
              'value': {
                'children': [object]
              },
              'line': 227
            };
          }
        }
      } else {
        // the node has no attribute nor child node
        object = {
          'tagName': elem.n,
          'value': '',
          'line': 233
        };
      }

      if (reviver) {
        object = reviver(elem.n || "", object);
      }

      return object;
    }

    function addAttribute(object, key, val) {
      if ("undefined" === typeof val) return;
      object.attributes = object.attributes || {};
      object.attributes[key] = val;
    }
    function addObject(object, key, val) {
      if ("undefined" === typeof val) return;
      object.value = object.value || {};
      object.value.children = object.value.children || [];
      if (typeof val === 'object' && val.tagName) {
        object.value.children.push(val);
      } else {
        object.value.children.push({
          'tagName': key,
          'value': val,
          'line': 258
        });
      }
    }

    return function(text, reviver) {
      text = text.replace(/<\?xml[^>]+>/i, "");
      var xmlTree = parseXML(text);
      var result = toObject(xmlTree, reviver);
      result.tagName = "document";
      return result;
    };
  })();

  //---------------------------------------
  // KmlParserClass
  //---------------------------------------
  function KmlParserClass() {
    var _parser = this;
    _parser.styleHolder = {};
    _parser.schemaHolder = {};

    _parser.tagSwitchTables = {
      'styleurl': _parser._styleurl,
      'stylemap': _parser._style,
      'style': _parser._style,
      'schema': _parser._schema,
      'coordinates': _parser._coordinates
    };
  }

  KmlParserClass.prototype.parseXml = function(rootElement) {
    var _parser = this,
      tagName = rootElement.tagName.toLowerCase();



    var _proc = _parser.tagSwitchTables[tagName] || _parser._default;
    //console.log("--->tagName = " + tagName, tagName in _parser.tagSwitchTables ? tagName : '_default');
    var result = _proc.call(_parser, rootElement);
    result.tagName = tagName;
    if (rootElement.attributes) {
      var attrNames = Object.keys(rootElement.attributes);
      attrNames.forEach(function(attrName) {
        result[attrName] = rootElement.attributes[attrName];
      });
    }
    return result;
  };

  KmlParserClass.prototype._styleurl = function(rootElement) {
    return {
      'styleId': rootElement.value
    };
  };


  KmlParserClass.prototype._style = function(rootElement) {
    var _parser = this;

    // Generate a style id for the tag
    var styleId = rootElement.attributes ? rootElement.attributes.id : null;
    if (!styleId) {
      styleId = "__" + Math.floor(Date.now() * Math.random()) + "__";
    }
    var result = {
      'styleId': styleId
    };

    // Store style information into the styleHolder
    var styles = {};
    var children = [];
    if (rootElement.value.children) {
      rootElement.value.children.forEach(function(childNode) {
        var node = _parser.parseXml(childNode);
        if (node.value) {
          styles[node.tagName] = node.value;
        } else {
          children.push(node);
        }
      });
      if (children.length > 0) {
        styles.children = children;
      }
    }
    _parser.styleHolder[styleId] = styles;
    return result;
  };

  KmlParserClass.prototype._schema = function(rootElement) {
    var _parser = this;
    var result = {};

    // Generate a schema id for the tag
    var schemaId = rootElement.attributes ? rootElement.attributes.id : null;
    if (!schemaId) {
      schemaId = "__" + Math.floor(Date.now() * Math.random()) + "__";
    }

    // Store schema information into the schemaHolder.
    var schema = {};
    schema.name = rootElement.attributes ? rootElement.attributes.id : "__" + Math.floor(Date.now() * Math.random()) + "__";
    if (rootElement.value.children) {
      var children = [];
      rootElement.value.children.forEach(function(childNode) {
        var node = _parser.parseXml(childNode);
        if (node) {
          children.push(node);
        }
      });
      if (children.length > 0) {
        schema.children = children;
      }
    }
    _parser.schemaHolder[schemaId] = schema;

    return result;
  };

  KmlParserClass.prototype._coordinates = function(rootElement) {
    var _parser = this;
    var result = {};
    var latLngList = [];

    var txt = rootElement.value;
    txt = txt.replace(/\s+/g, "\n");
    txt = txt.replace(/\n+/g, "\n");
    var lines = txt.split(/\n/);

    lines.forEach(function(line) {
      line = line.replace(/[^0-9,.\\-]/g, "");
      if (line !== "") {
        var tmpArry = line.split(",");
        latLngList.push({
          'lat': parseFloat(tmpArry[1]),
          'lng': parseFloat(tmpArry[0])
        });
      }
    });

    result.coordinates = latLngList;
    return result;
  };

  KmlParserClass.prototype._default = function(rootElement) {
    var _parser = this,
      result = {};

    if (rootElement.value.children) {
      var children = [];
      rootElement.value.children.forEach(function(childNode) {

        var node = _parser.parseXml.call(_parser, childNode);
        if (node) {
          if ('styleId' in node) {
            result.styleIDs = result.styleIDs || [];
            result.styleIDs.push(node.styleId);
          } else if (node.tagName !== 'schema') {
            children.push(node);
          }
        }

      });
      result.children = children;
    } else {
      var value = rootElement.value;
      if (/^-?[0-9]+$/.test(value)) {
        result.value = parseInt(value, 10);
      } else if (/^-?[0-9\.]+$/.test(value)) {
        result.value = parseFloat(value, 10);
      } else {
        result.value = value;
      }
    }
    return result;
  };


  self.onmessage = function(evt) {
    var params = evt.data;
    //------------------------------------------
    // Load & parse kml file in WebWorker
    //------------------------------------------
    (new Promise(function(resolve, reject) {
      //-----------------
      // Read XML file
      //-----------------
console.log(params);
      var xhr = createCORSRequest('GET', params.url, true);
      if (xhr) {
        xhr.onreadystatechange = function() {
          try {
            if (xhr.readyState === 4) {
              if (xhr.status === 200) {
                resolve(xhr.responseText);
              } else {
                reject(xhr);
              }
            }
          } catch (e) {
            reject(e.description);
          }
        };
        xhr.send();
      }
    }))
    .then(function(xmlTxt) {
      //-----------------
      // Parse it
      //-----------------
      var doc = fromXML(xmlTxt);
      var parser = new KmlParserClass();
      var root = parser.parseXml(doc);

      var result = {
        'schemas': parser.schemaHolder,
        'styles': parser.styleHolder,
        'root': root
      };
      postMessage(result);
    });

  };
}
