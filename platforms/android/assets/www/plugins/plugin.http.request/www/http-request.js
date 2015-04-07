cordova.define("plugin.http.request.phonegap-http-requst", function(require, exports, module) { (function(window) {
  
  function HttpRequest() {
    var self = this;
    var methods = ["post", "get"];
    methods.forEach(function(method) {
      self[method] = function(url, params, callback) {
        self.request_(method, url, params, callback);
      };
    });
  }
  
  HttpRequest.prototype.postJSON = function(url, params, callback) {
    if (params && !callback) {
      callback = arguments[1];
      params = null;
    }
    this.request_('post', url, params, function(err, response) {
      if (err) {
        callback(err, response);
        return;
      }
      try {
        response = JSON.parse(response);
        callback(null, response);
      } catch (e) {
        callback(e, response);
      }
    });
  };
  HttpRequest.prototype.getJSON = function(url, params, callback) {
    if (params && !callback) {
      callback = arguments[1];
      params = null;
    }
    this.request_('get', url, params, function(err, response) {
      if (err) {
        callback(err, response);
        return;
      }
      try {
        response = JSON.parse(response);
        callback(null, response);
      } catch (e) {
        callback(e, response);
      }
    });
  };
  HttpRequest.prototype.request_ = function(method, url, params, callback) 
  {
    var args = [],
        key,
        queryParams = "",
        _callback = callback || params;
    args.push(method);
    args.push(url);
    if (typeof params == 'object') {
      if (method.toLowerCase() == 'get') {
        for (key in params) {
          queryParams += key + '=' + params[key] + '&';
        }
        if (url.indexOf('?') > -1) {
          url += '&' + queryParams;
        } else {
          url += '?' + queryParams;
        }
        url = url.replace(/&$/, '');
      } else {
        args.push(params);
      }
    }
    
    return cordova.exec(function(response) {
      _callback(null, response);
    }, function(error) {
      _callback(error);
    }, 'HttpRequest', 'execute', args);
  };
  
  // Plug in to Cordova
  cordova.addConstructor(function() {
    if (!window.Cordova) {
      window.Cordova = cordova;
    };
    if(!window.plugin) {
      window.plugin = {};
    }
    window.plugin.HttpRequest = HttpRequest;
  });
})(window);
});
