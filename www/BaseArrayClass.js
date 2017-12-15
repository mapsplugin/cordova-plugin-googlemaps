var utils = require('cordova/utils'),
  BaseClass = require('./BaseClass');

var ARRAY_FIELD = typeof Symbol === 'undefined' ? '__array' + Date.now() : Symbol.for('array');

var resolvedPromise = typeof Promise == 'undefined' ? null : Promise.resolve();
var nextTick = resolvedPromise ? function(fn) { resolvedPromise.then(fn); } : function(fn) { setTimeout(fn); };

function BaseArrayClass(array) {
  BaseClass.apply(this);
  var self = this;
  self[ARRAY_FIELD] = [];

  if (array && (array instanceof Array || Array.isArray(array))) {
    for (var i = 0; i < array.length; i++) {
      self[ARRAY_FIELD].push(array[i]);
    }
  }
}

utils.extend(BaseArrayClass, BaseClass);

BaseArrayClass.prototype.mapSeries = function(fn, callback) {
  if (typeof fn !== "function" || typeof callback !== "function") {
    return;
  }
  var self = this;

  var results = [];
  var currentIdx = 0;
  var _arrayLength = self[ARRAY_FIELD].length;
  var _looper = function() {
    fn.call(self, self[ARRAY_FIELD][currentIdx], function(value) {
      results[currentIdx] = value;
      currentIdx++;
      if (currentIdx === _arrayLength) {
        callback.call(self, results);
      } else {
        nextTick(_looper);
      }
    });
  };
  nextTick(_looper);
};

BaseArrayClass.prototype.mapAsync = function(fn, callback) {
  if (typeof fn !== "function" || typeof callback !== "function") {
    return;
  }
  var self = this;
  //------------------------
  // example:
  //    baseArray.mapAsync(function(item, idx, callback) {
  //       ...
  //       callback(value);
  //    }, function(values) {
  //
  //    });
  //------------------------
  var results = [];
  self[ARRAY_FIELD].forEach(function() {
    results.push(null);
  });
  var _arrayLength = self[ARRAY_FIELD].length;
  var finishCnt = 0;
  if (_arrayLength === 0) {
    callback.call(self, []);
    return;
  }
  self[ARRAY_FIELD].forEach(function(item, idx) {
    fn.call(self, item, function(value) {
      results[idx] = value;
      finishCnt++;
      if (finishCnt === _arrayLength) {
        callback.call(self, results);
      }
    });
  });
};

BaseArrayClass.prototype.map = function(fn, callback) {
  var self = this;

  if (typeof fn !== "function") {
    return;
  }
  var results = [];
  if (typeof fn === "function" && typeof callback !== "function") {
    //------------------------
    // example:
    //    var values = baseArray.map(function(item, idx) {
    //       ...
    //       return someValue;
    //    });
    //------------------------
    return self[ARRAY_FIELD].map(fn.bind(self));
  }
  self.mapAsync(fn, callback);
};

BaseArrayClass.prototype.forEachAsync = function(fn, callback) {
  if (typeof fn !== "function" || typeof callback !== "function") {
    return;
  }
  var self = this;
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
  var _arrayLength = self[ARRAY_FIELD].length;
  if (_arrayLength === 0) {
    callback.call(self);
    return;
  }

  self[ARRAY_FIELD].forEach(function(item, idx) {
    fn.call(self, item, function() {
      finishCnt++;
      if (finishCnt === _arrayLength) {
        callback.call(self);
      }
    });
  });
};

BaseArrayClass.prototype.forEach = function(fn, callback) {
  var self = this;
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
    self[ARRAY_FIELD].forEach(fn.bind(self));
    return;
  }
  self.forEachAsync(fn, callback);
};

BaseArrayClass.prototype.filterAsync = function(fn, callback) {
  var self = this;
  if (typeof fn !== "function" || typeof callback !== "function") {
    return;
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
  var _arrayLength = self[ARRAY_FIELD].length;
  if (_arrayLength === 0) {
    callback.call(self, []);
    return;
  }
  var results = [];
  self[ARRAY_FIELD].forEach(function(item, idx) {
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

BaseArrayClass.prototype.filter = function(fn, callback) {
  var self = this;
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
    return self[ARRAY_FIELD].filter(fn.bind(self));
  }
  self.filterAsync(fn, callback);
};

BaseArrayClass.prototype.indexOf = function(item) {
  return this[ARRAY_FIELD].indexOf(item);
};

BaseArrayClass.prototype.empty = function(noNotify) {
  var self = this;
  var cnt = self[ARRAY_FIELD].length;
  for (var i = 0; i < cnt; i++) {
    self.removeAt(0, noNotify);
  }
};

BaseArrayClass.prototype.push = function(value, noNotify) {
  var self = this;
  self[ARRAY_FIELD].push(value);
  if (noNotify !== true) {
    self.trigger("insert_at", self[ARRAY_FIELD].length - 1);
  }
  return self[ARRAY_FIELD].length;
};

BaseArrayClass.prototype.insertAt = function(index, value, noNotify) {
  var self = this;
  if (index > self[ARRAY_FIELD].length) {
    for (var i = self[ARRAY_FIELD].length; i <= index; i++) {
      self[ARRAY_FIELD][i] = undefined;
    }
  }
  self[ARRAY_FIELD][index] = value;
  if (noNotify !== true) {
    self.trigger("insert_at", index);
  }
};

BaseArrayClass.prototype.getArray = function() {
  //return _array.slice(0);  <-- Android browser keeps the same instance of original array
  return JSON.parse(JSON.stringify(this[ARRAY_FIELD]));
};

BaseArrayClass.prototype.getAt = function(index) {
  return this[ARRAY_FIELD][index];
};

BaseArrayClass.prototype.setAt = function(index, value, noNotify) {
  var self = this;
  var prev = self[ARRAY_FIELD][index];
  self[ARRAY_FIELD][index] = value;
  if (noNotify !== true) {
    self.trigger("set_at", index, prev);
  }
};

BaseArrayClass.prototype.removeAt = function(index, noNotify) {
  var self = this;
  var value = self[ARRAY_FIELD][index];
  self[ARRAY_FIELD].splice(index, 1);
  if (noNotify !== true) {
    self.trigger("remove_at", index, value);
  }
  return value;
};

BaseArrayClass.prototype.pop = function(noNotify) {
  var self = this;
  var index = self[ARRAY_FIELD].length - 1;
  var value = self[ARRAY_FIELD].pop();
  if (noNotify !== true) {
    self.trigger("remove_at", index, value);
  }
  return value;
};

BaseArrayClass.prototype.getLength = function() {
  return this[ARRAY_FIELD].length;
};

BaseArrayClass.prototype.reverse = function() {
  this[ARRAY_FIELD] = this[ARRAY_FIELD].reverse();
};

BaseArrayClass.prototype.sort = function(func) {
  this[ARRAY_FIELD] = this.sort(func);
};


module.exports = BaseArrayClass;
