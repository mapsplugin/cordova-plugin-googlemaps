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
        if (typeof fn !== "function" || _array.length === 0) {
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
            results = _array.map(fn.bind(self));
            return;
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
        if (typeof fn !== "function" || _array.length === 0) {
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
        _array.forEach(function(item, idx) {
          fn.call(self, item, function() {
            finishCnt++;
            if (finishCnt === _arrayLength) {
              callback.call(self);
            }
          });
        });
    };

    self.empty = function() {
        var tmp = [];
        for (var i = 0; i < _array.length; i++) {
            tmp.push(1);
        }
        tmp.forEach(function() {
          self.removeAt(0);
        });
    };

    self.push = function(value, noNotify) {
        _array.push(value);
        if (noNotify !== true) {
          self.trigger("insert_at", _array.length - 1);
        }
        return _array.length;
    };

    self.insertAt = function(index, value) {
        if (index > _array.length) {
          for (var i = _array.length; i <= index; i++) {
            _array[i] = undefined;
          }
        }
        _array[index] = value;
        self.trigger("insert_at", index);
    };

    self.getArray = function() {
        return _array.slice(0);
    };

    self.getAt = function(index) {
        return _array[index];
    };

    self.setAt = function(index, value) {
        var prev = _array[index];
        _array[index] = value;
        self.trigger("set_at", index, prev);
    };


    self.removeAt = function(index) {
        var value = _array[index];
        _array.splice(index, 1);
        self.trigger("remove_at", index, value);
        return value;
    };

    self.pop = function() {
        var index = _array.length - 1;
        var value = _array.pop();
        self.trigger("remove_at", index, value);
        return value;
    };

    self.getLength = function() {
        return _array.length;
    };
    return self;
}

utils.extend(BaseArrayClass, BaseClass);

module.exports = BaseArrayClass;
