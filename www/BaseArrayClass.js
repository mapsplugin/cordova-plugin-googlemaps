var utils = require('cordova/utils'),
  BaseClass = require('./BaseClass');

var ARRAY_FIELD = typeof Symbol === 'undefined' ? '__array' + Date.now() : Symbol('array');

var nextTick = function(fn) { Promise.resolve().then(fn); };
/**
 * Create a BaseArrayClass.
 * @class
 */
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

/**
 * The same as `Array.map` but runs a single async operation at a time.
 *
 * @name mapSeries
 * @param {Function} iteratee - An async function to apply to each item in array.
 * @param {Function} [callback] - A callback which is called when all `iteratee` functions
 * have finished. Results is an array of the transformed items from holding array.
 * Invoked with (results);
 * @return {Promise} a promise, if no calback if passed.
 */
BaseArrayClass.prototype.mapSeries = function(iteratee, callback) {
  if (typeof iteratee !== 'function') {
    var error = new Error('iteratee must be a function');
    if (typeof callback === 'function') {
      throw error;
    } else {
      return Promise.reject(error);
    }
  }

  var self = this;

  var results = [];
  var _arrayLength = self[ARRAY_FIELD].length;
  if (_arrayLength === 0) {
    if (typeof callback === 'function') {
      callback.call(self, []);
      return;
    } else {
      return Promise.resolve([]);
    }
  }
  var _looper = function(currentIdx, resolve) {

    iteratee.call(self, self[ARRAY_FIELD][currentIdx], function(value) {
      results[currentIdx] = value;
      if (_arrayLength === results.length) {
        resolve(results);
      } else {
        nextTick(function() {
          _looper(currentIdx + 1, resolve);
        });
      }
    });
  };

  return new Promise(function(resolve) {
    nextTick(function() {
      _looper(0, resolve);
    });
  }).then(function(results) {
    if (typeof callback === 'function') {
      return callback.call(self, results);
    } else {
      return Promise.resolve(results);
    }
  });
};


/**
 * The same as `Array.map` but runs async all `iteratee` function at the same time.
 *
 * ```
 * baseArray.mapAsync(function(item, idx, callback) {
 *    ...
 *    callback(value);
 * }).then(function(values) {
 *
 * });
 * ```
 *
 * @name mapAsync
 * @param {Function} iteratee - An async function to apply to each item in array.
 * @param {Function} [callback] - A callback which is called when all `iteratee` functions
 * have finished. Results is an array of the transformed items from holding array.
 * Invoked with (results);
 * @return {Promise} a promise, if no calback if passed.
 */
BaseArrayClass.prototype.mapAsync = function(iteratee, callback) {
  if (typeof iteratee !== 'function') {
    var error = new Error('iteratee must be a function');
    if (typeof callback === 'function') {
      throw error;
    } else {
      return Promise.reject(error);
    }
  }
  var self = this;
  var results = [];
  for (var i = 0; i < self[ARRAY_FIELD].length; i++) {
    results.push(null);
  }
  var _arrayLength = self[ARRAY_FIELD].length;
  var finishCnt = 0;
  if (_arrayLength === 0) {
    if (typeof callback === 'function') {
      callback.call(self, []);
      return;
    } else {
      return Promise.resolve([]);
    }
  }

  return new Promise(function(resolve) {
    for (i = 0; i < self[ARRAY_FIELD].length; i++) {
      (function(item, idx) {
        nextTick(function() {
          iteratee.call(self, item, function(value) {
            results[idx] = value;
            finishCnt++;
            if (finishCnt === _arrayLength) {
              resolve(results);
            }
          });
        });
      })(self[ARRAY_FIELD][i], i);
    }
  }).then(function(results) {
    if (typeof callback === 'function') {
      return callback.call(self, results);
    } else {
      return Promise.resolve(results);
    }
  });

};


/**
 * If you provide `iteratee` and `callback`, work as `mapAsync()`.
 * If you provide `iteratee`, but omit `callback`, work as `Array.map()`.
 *
 * @deprecated
 * @name map
 * @param {Function} iteratee - An async function to apply to each item in array.
 * @param {Function} [callback] - A callback which is called when all `iteratee` functions
 * have finished. Results is an array of the transformed items from holding array.
 * Invoked with (results);
 * @return {Promise|any[]} a promise, if no calback if passed. If you omit `callback`, return transformed items.
 */
BaseArrayClass.prototype.map = function(iteratee, callback) {
  var self = this;

  if (typeof iteratee !== 'function') {
    var error = new Error('iteratee must be a function');
    if (typeof callback === 'function') {
      throw error;
    } else {
      return Promise.reject(error);
    }
  }
  if (typeof callback !== 'function') {
    //------------------------
    // example:
    //    var values = baseArray.map(function(item, idx) {
    //       ...
    //       return someValue;
    //    });
    //------------------------
    return self[ARRAY_FIELD].map(iteratee.bind(self));
  }
  return self.mapAsync(iteratee, callback);
};

/**
 * The same as `Array.forEach` but runs async all `iteratee` function at the same time.
 *
 * @name forEachAsync
 * @param {Function} iteratee - An async function to apply to each item in array.
 * @param {Function} [callback] - A callback which is called when all `iteratee` functions
 * have finished.
 * @return {Promise} a promise, if no calback if passed.
 */
BaseArrayClass.prototype.forEachAsync = function(iteratee, callback) {
  if (typeof iteratee !== 'function') {
    var error = new Error('iteratee must be a function');
    if (typeof callback === 'function') {
      throw error;
    } else {
      return Promise.reject(error);
    }
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
    if (typeof callback === 'function') {
      callback.call(self);
      return;
    } else {
      return Promise.resolve();
    }
  }

  return new Promise(function(resolve) {

    self[ARRAY_FIELD].forEach(function(item) {
      iteratee.call(self, item, function() {
        finishCnt++;
        if (finishCnt === _arrayLength) {
          resolve(self);
        }
      });
    });

  })
    .then(function() {
      if (typeof callback === 'function') {
        callback.call(self);
        return;
      } else {
        return Promise.resolve();
      }
    });
};

/**
 * If you provide `iteratee` and `callback`, work as `forEachAsync()`.
 * If you provide `iteratee`, but you omit `callback`, work as `Array.forEach()`
 *
 * @deprecated
 * @name forEach
 * @param {Function} iteratee - An async function to apply to each item in array.
 * @param {Function} [callback] - A callback which is called when all `iteratee` functions
 * have finished.
 * Invoked with (results);
 * @return {Promise} a promise, if no calback if passed.
 */
BaseArrayClass.prototype.forEach = function(iteratee, callback) {
  var self = this;
  if (typeof iteratee !== 'function') {
    var error = new Error('iteratee must be a function');
    if (typeof callback === 'function') {
      throw error;
    } else {
      return Promise.reject(error);
    }
  }

  if (typeof callback !== 'function') {
    //------------------------
    // example:
    //    baseArray.forEach(function(item, idx) {
    //       ...
    //    });
    //------------------------
    self[ARRAY_FIELD].forEach(iteratee.bind(self));
    return Promise.resolve();
  }
  return self.forEachAsync(iteratee, callback);
};


/**
 * The same as `Array.filter` but runs async all `iteratee` function at the same time.
 *
 * @name filterAsync
 * @param {Function} iteratee - An async function to apply to each item in array.
 * @param {Function} [callback] - A callback which is called when all `iteratee` functions
 * have finished. Results is an array of the filtered items from holding array.
 * Invoked with (results);
 * @return {Promise} a promise, if no calback if passed.
 */
BaseArrayClass.prototype.filterAsync = function(iteratee, callback) {
  var self = this;
  if (typeof iteratee !== 'function') {
    var error = new Error('iteratee must be a function');
    if (typeof callback === 'function') {
      throw error;
    } else {
      return Promise.reject(error);
    }
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
    if (typeof callback === 'function') {
      callback.call(self, []);
      return;
    } else {
      return Promise.resolve([]);
    }
  }
  return (new Promise(function(resolve) {
    var results = [];
    self[ARRAY_FIELD].forEach(function(item) {
      iteratee.call(self, item, function(isOk) {
        if (isOk) {
          results.push(item);
        }
        finishCnt++;
        if (finishCnt === _arrayLength) {
          resolve(results);
        }
      });
    });
  }))
    .then(function(results) {
      if (typeof callback === 'function') {
        callback.call(self, results);
        return;
      } else {
        return Promise.resolve(results);
      }
    });
};

/**
 * If you provide `iteratee` and `callback`, work as `filterAsync()`.
 * If you provide `iteratee`, but you omit `callback`, work as `Array.filter()`
 *
 * @deprecated
 * @name filter
 * @param {Function} iteratee - An async function to apply to each item in array.
 * @param {Function} [callback] - A callback which is called when all `iteratee` functions
 * have finished.
 * @return {Promise} a promise, if no calback if passed.
 */
BaseArrayClass.prototype.filter = function(iteratee, callback) {
  var self = this;
  if (typeof iteratee !== 'function') {
    var error = new Error('iteratee must be a function');
    if (typeof callback === 'function') {
      throw error;
    } else {
      return Promise.reject(error);
    }
  }
  if (typeof callback !== 'function') {
    //------------------------
    // example:
    //    baseArray.filter(function(item, idx) {
    //       ...
    //       return true or false
    //    });
    //------------------------
    return self[ARRAY_FIELD].filter(iteratee);
  }
  return self.filterAsync(iteratee, callback);
};

/**
 * Returns the first index at which a given element can be found in the array, or -1 if it is not present.
 *
 * @name indexOf
 * @param {any} searchElement - Element to locate in the array.
 * @param {number} [searchElement] - The index to start the search at.
 * If the index is greater than or equal to the array's length, -1 is returned,
 * which means the array will not be searched.
 * If the provided index value is a negative number,
 * it is taken as the offset from the end of the array.
 * Note: if the provided index is negative, the array is still searched from front to back.
 * If the provided index is 0, then the whole array will be searched. Default: 0 (entire array is searched).
 * @return The first index of the element in the array; -1 if not found.
 */
BaseArrayClass.prototype.indexOf = function(item, searchElement) {
  searchElement = searchElement === undefined || searchElement === null ? 0 : searchElement;
  if (typeof searchElement !== 'number') {
    throw new Error('searchElement must be a number');
  }
  if (searchElement < 0) {
    throw new Error('searchElement must be over number than 0');
  }

  return this[ARRAY_FIELD].indexOf(item, searchElement);
};

/**
 * Removes all elements. Fire `remove_at` event for each element.
 *
 * @name empty
 * @param {boolean} [noNotify] - Sets `true` if you don't want to fire `remove_at` event.
 */
BaseArrayClass.prototype.empty = function(noNotify) {
  var self = this;
  var cnt = self[ARRAY_FIELD].length;
  for (var i = cnt - 1; i >= 0; i--) {
    self.removeAt(i, noNotify);
  }
};

/**
 * Adds one element to the end of an array and returns the new length of the array.
 * Fire `insert_at` event if `noNotify` is `false`.
 *
 * @name push
 * @param {any} value - The element to add to the end of the array.
 * @param {boolean} [noNotify] - Sets `true` if you don't want to fire `insert_at` event.
 * @return {number} The new length property of the object upon which the method was called.
 */
BaseArrayClass.prototype.push = function(value, noNotify) {
  var self = this;
  self[ARRAY_FIELD].push(value);
  if (noNotify !== true) {
    self.trigger('insert_at', self[ARRAY_FIELD].length - 1);
  }
  return self[ARRAY_FIELD].length;
};

/**
 * Adds one element to the end of an array and returns the new length of the array.
 * Fire `insert_at` event if `noNotify` is `false`.
 *
 * @name insertAt
 * @param {number} index - The position of the array you want to insert new element.
 * @param {any} value - The element to add to the end of the array.
 * @param {boolean} [noNotify] - Sets `true` if you don't want to fire `insert_at` event.
 * @return {number} The new length property of the object upon which the method was called.
 */
BaseArrayClass.prototype.insertAt = function(index, value, noNotify) {
  var self = this;
  if (typeof index !== 'number') {
    throw new Error('index must be a number');
  }
  if (index < 0) {
    throw new Error('index must be over number than 0');
  }
  self[ARRAY_FIELD].splice(index, 0, value);
  if (noNotify !== true) {
    self.trigger('insert_at', index);
  }
  return self[ARRAY_FIELD].length;
};

/**
 * Returns a new array that is the clone of internal array.
 *
 * @name getArray
 * @return {Array<any>} New array
 */
BaseArrayClass.prototype.getArray = function() {
  //return _array.slice(0);  <-- Android browser keeps the same instance of original array
  return JSON.parse(JSON.stringify(this[ARRAY_FIELD]));
};

/**
 * Returns item of specified position.
 *
 * @name getAt
 * @param {number} index - The position of the array you want to get.
 * @return {any} item
 */
BaseArrayClass.prototype.getAt = function(index) {
  var self = this;
  if (typeof index !== 'number') {
    throw new Error('index must be a number');
  }
  if (index < 0) {
    throw new Error('index must be over number than 0');
  }
  if (index >= self[ARRAY_FIELD].length) {
    throw new Error('index must be lower number than ' + self[ARRAY_FIELD].length);
  }
  return this[ARRAY_FIELD][index];
};

/**
 * Replaces item of specified position.
 *
 * @name setAt
 * @param {number} index - The position of the array you want to get.
 * @param {any} value - New element
 * @param {boolean} [noNotify] - Sets `true` if you don't want to fire `set_at` event.
 * @return {any} previous item
 */
BaseArrayClass.prototype.setAt = function(index, value, noNotify) {
  var self = this;
  if (typeof index !== 'number') {
    throw new Error('index must be a number');
  }
  if (index < 0) {
    throw new Error('index must be over number than 0');
  }
  if (index > self[ARRAY_FIELD].length) {
    for (var i = self[ARRAY_FIELD].length; i <= index; i++) {
      self[ARRAY_FIELD][i] = undefined;
    }
  }
  var prev = self[ARRAY_FIELD][index];
  self[ARRAY_FIELD][index] = value;
  if (noNotify !== true) {
    self.trigger('set_at', index, prev);
  }
  return prev;
};

/**
 * Removes item of specified position.
 *
 * @name removeAt
 * @param {number} index - The position of the array you want to get.
 * @param {boolean} [noNotify] - Sets `true` if you don't want to fire `remove_at` event.
 * @return {any} removed item
 */
BaseArrayClass.prototype.removeAt = function(index, noNotify) {
  var self = this;
  if (typeof index !== 'number') {
    throw new Error('index must be a number');
  }
  if (index < 0) {
    throw new Error('index must be over number than 0');
  }
  if (index >= self[ARRAY_FIELD].length) {
    throw new Error('index must be lower number than ' + self[ARRAY_FIELD].length);
  }
  var value = self[ARRAY_FIELD][index];
  self[ARRAY_FIELD].splice(index, 1);
  if (noNotify !== true) {
    self.trigger('remove_at', index, value);
  }
  return value;
};

/**
 * Removes item of the last array item.
 *
 * @name pop
 * @param {boolean} [noNotify] - Sets `true` if you don't want to fire `remove_at` event.
 * @return {any} removed item
 */
BaseArrayClass.prototype.pop = function(noNotify) {
  var self = this;
  var index = self[ARRAY_FIELD].length - 1;
  var value = self[ARRAY_FIELD].pop();
  if (noNotify !== true) {
    self.trigger('remove_at', index, value);
  }
  return value;
};

/**
 * Returns the length of array.
 *
 * @name getLength
 * @return {any} Number of items
 */
BaseArrayClass.prototype.getLength = function() {
  return parseInt(this[ARRAY_FIELD].length, 10); // In order to prevent manupulating through `length` property, conver to number mandatory using `parseInt`.
};

/**
 * Reverses an array in place. The first array element becomes the last, and the last array element becomes the first.
 *
 * @name reverse
 */
BaseArrayClass.prototype.reverse = function() {
  this[ARRAY_FIELD] = this[ARRAY_FIELD].reverse();
};

/**
 * The `sort()` method sorts the elements of an array in place and returns the array.
 * The same as `array.sort()`.
 *
 * @name sort
 * @param {Function} [compareFunction] - Specifies a function that defines the sort order.
 *  If omitted, the array is sorted according to each character's Unicode code point value,
 *  according to the string conversion of each element.
 */
BaseArrayClass.prototype.sort = function(compareFunction) {
  if (typeof compareFunction === 'function') {
    this[ARRAY_FIELD].sort(compareFunction);
  } else {
    this[ARRAY_FIELD].sort();
  }
};


module.exports = BaseArrayClass;
