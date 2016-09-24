var utils = require('cordova/utils'),
    BaseClass = require('./BaseClass');

var BaseArrayClass = function(array) {
    BaseClass.apply(this);
    var self = this;
    var _array = [];

    if (array && (array instanceof Array || Array.isArray(array))) {
       for (var i = 0; i < array.length; i++) {
           _array.push(array[i]);
       }
    }

    BaseArrayClass.prototype.forEach = function(callback) {
        if (typeof callback !== "function" || _array.length === 0) {
            return;
        }
        _array.forEach(callback);
    };

    BaseArrayClass.prototype.empty = function() {
        _array = [];
    };

    BaseArrayClass.prototype.push = function(value, noNotify) {
        _array.push(value);
        if (noNotify !== true) {
          self.trigger("insert_at", _array.length - 1);
        }
        return _array.length;
    };

    BaseArrayClass.prototype.insertAt = function(index, value) {
        _array.splice(index, 0, value);
        self.trigger("insert_at", index);
    };

    BaseArrayClass.prototype.getArray = function() {
        return _array.slice(0);
    };

    BaseArrayClass.prototype.getAt = function(index) {
        return _array[index];
    };

    BaseArrayClass.prototype.setAt = function(index, value) {
        var prev = _array[index];
        _array[index] = value;
        self.trigger("set_at", index, prev);
    };


    BaseArrayClass.prototype.removeAt = function(index) {
        var value = _array.slice(index, 1);
        self.trigger("remove_at", index, value);
        return value;
    };

    BaseArrayClass.prototype.pop = function() {
        var value = _array.pop();
        self.trigger("remove_at", _array.length, value);
        return value;
    };

    BaseArrayClass.prototype.getLength = function() {
        return _array.length;
    };
    return self;
};

utils.extend(BaseArrayClass, BaseClass);

module.exports = BaseArrayClass;
