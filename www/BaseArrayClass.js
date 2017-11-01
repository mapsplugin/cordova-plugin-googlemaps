var utils = require('cordova/utils'),
    BaseClass = require('./BaseClass');

function BaseArrayClass(array) {
    BaseClass.apply(this);
    var self = this;
    var _array = [];

    if (array && (array instanceof Array || Array.isArray(array))) {
       for (var i = 0; i < array.length; i++) {
           _array.push(array[i]);
       }
    }

    self.map = function(fn, callback) {
        if (typeof fn !== "function") {
            return;
        }
        var results = [];
        if (typeof fn === "function" && typeof callback !== "function") {
            //------------------------
            // example:
            //    var values = baseArray.forEach(function(item, idx) {
            //       ...
            //       return someValue;
            //    });
            //------------------------
            return _array.map(fn.bind(self));
        }
        //------------------------
        // example:
        //    baseArray.forEach(function(item, idx, callback) {
        //       ...
        //       callback(value);
        //    }, function(values) {
        //
        //    });
        //------------------------
        _array.forEach(function() {
          results.push(null);
        });
        var _arrayLength = _array.length;
        var finishCnt = 0;
        if (_arrayLength === 0) {
          callback.call(self, []);
          return;
        }
        _array.forEach(function(item, idx) {
          fn.call(self, item, function(value) {
            results[idx] = value;
            finishCnt++;
            if (finishCnt === _arrayLength) {
              callback.call(self, results);
            }
          });
        });
    };

    self.forEach = function(fn, callback) {
        if (typeof fn !== "function") {
            return;
        }
        if (typeof fn === "function" && typeof callback !== "function") {
            //------------------------
            // example:
            //    baseArray.forEach(function(item, idx) {
            //       ...
            //    });
            //------------------------
            _array.forEach(fn.bind(self));
            return;
        }
        //------------------------
        // example:
        //    baseArray.forEach(function(item, callback) {
        //       ...
        //       callback();
        //    }, function() {
        //
        //    });
        //------------------------
        var finishCnt = 0;
        var _arrayLength = _array.length;
        if (_arrayLength === 0) {
          callback.call(self);
          return;
        }

        _array.forEach(function(item, idx) {
          fn.call(self, item, function() {
            finishCnt++;
            if (finishCnt === _arrayLength) {
              callback.call(self);
            }
          });
        });
    };

    self.filter = function(fn, callback) {
        if (typeof fn !== "function") {
            return;
        }
        if (typeof fn === "function" && typeof callback !== "function") {
            //------------------------
            // example:
            //    baseArray.filter(function(item, idx) {
            //       ...
            //       return true or false
            //    });
            //------------------------
            return _array.filter(fn.bind(self));
        }
        //------------------------
        // example:
        //    baseArray.filter(function(item, callback) {
        //       ...
        //       callback(true or false);
        //    }, function(filteredItems) {
        //
        //    });
        //------------------------
        var finishCnt = 0;
        var _arrayLength = _array.length;
        if (_arrayLength === 0) {
          callback.call(self, []);
          return;
        }
        var results = [];
        _array.forEach(function(item, idx) {
          fn.call(self, item, function(isOk) {
            if (isOk) {
              results.push(item);
            }
            finishCnt++;
            if (finishCnt === _arrayLength) {
              callback.call(self, results);
            }
          });
        });
    };

    self.indexOf = function(item) {
        return _array.indexOf(item);
    };
    self.empty = function(noNotify) {
        var cnt = _array.length;
        for (var i = 0; i < cnt; i++) {
          self.removeAt(0, noNotify);
        }
    };

    self.push = function(value, noNotify) {
        _array.push(value);
        if (noNotify !== true) {
          self.trigger("insert_at", _array.length - 1);
        }
        return _array.length;
    };

    self.insertAt = function(index, value, noNotify) {
        if (index > _array.length) {
          for (var i = _array.length; i <= index; i++) {
            _array[i] = undefined;
          }
        }
        _array[index] = value;
        if (noNotify !== true) {
          self.trigger("insert_at", index);
        }
    };

    self.getArray = function() {
        //return _array.slice(0);  <-- Android browser keeps the same instance of original array
        return JSON.parse(JSON.stringify(_array));
    };

    self.getAt = function(index) {
        return _array[index];
    };

    self.setAt = function(index, value, noNotify) {
        var prev = _array[index];
        _array[index] = value;
        if (noNotify !== true) {
          self.trigger("set_at", index, prev);
        }
    };


    self.removeAt = function(index, noNotify) {
        var value = _array[index];
        _array.splice(index, 1);
        if (noNotify !== true) {
          self.trigger("remove_at", index, value);
        }
        return value;
    };

    self.pop = function(noNotify) {
        var index = _array.length - 1;
        var value = _array.pop();
        if (noNotify !== true) {
          self.trigger("remove_at", index, value);
        }
        return value;
    };

    self.getLength = function() {
        return _array.length;
    };
    self.reverse = function() {
        _array = _array.reverse();
    };
    self.sort = function(func) {
        _array = _array.sort(func);
    };
    return self;
}

utils.extend(BaseArrayClass, BaseClass);

module.exports = BaseArrayClass;
