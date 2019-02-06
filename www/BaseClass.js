var VARS_FIELD = typeof Symbol === 'undefined' ? '__vars' + Date.now() : Symbol('vars');
var SUBSCRIPTIONS_FIELD = typeof Symbol === 'undefined' ? '__subs' + Date.now() : Symbol('subscriptions');

function BaseClass() {
  this[VARS_FIELD] = {};
  this[SUBSCRIPTIONS_FIELD] = {};
  this.errorHandler = this.errorHandler.bind(this);

  Object.defineProperty(this, 'hashCode', {
    value: Math.floor(Date.now() * Math.random())
  });
}

BaseClass.prototype = {
  empty: function () {
    var vars = this[VARS_FIELD];

    Object.keys(vars).forEach(function (name) {
      vars[name] = null;
      delete vars[name];
    });
  },

  get: function (key) {
    return this[VARS_FIELD].hasOwnProperty(key) ? this[VARS_FIELD][key] : undefined;
  },

  set: function (key, value, noNotify) {
    if (key === "__pgmId") return;
    var prev = this.get(key);

    this[VARS_FIELD][key] = value;

    if (!noNotify && prev !== value) {
      this.trigger(key + '_changed', prev, value, key);
    }

    return this;
  },

  bindTo: function (key, target, targetKey, noNotify) {
    if (key === "__pgmId") return;
    targetKey = targetKey || key;

    // If `noNotify` is true, prevent `(targetKey)_changed` event occurrs,
    // when bind the value for the first time only.
    // (Same behaviour as Google Maps JavaScript v3)
    target.set(targetKey, this[VARS_FIELD][key], noNotify);

    this.on(key + '_changed', function (oldValue, value) {
      target.set(targetKey, value);
    });
  },

  trigger: function (eventName) {
    if (!eventName) {
      return this;
    }

    if (!this[SUBSCRIPTIONS_FIELD][eventName]) {
      return this;
    }

    var listenerHashMap = this[SUBSCRIPTIONS_FIELD][eventName];
    var keys = Object.keys(listenerHashMap);
    var args = Array.prototype.slice.call(arguments, 1);
    var self = this;

    keys.forEach(function (_hashCode) {
      if (self[SUBSCRIPTIONS_FIELD] &&
        self[SUBSCRIPTIONS_FIELD][eventName] &&
        _hashCode in self[SUBSCRIPTIONS_FIELD][eventName]) {
        var info = self[SUBSCRIPTIONS_FIELD][eventName][_hashCode];

        switch (info.kind) {
        case 'on':
          info.listener.apply(self, args);
          break;
        case 'onThrottled':
          info.args = args;
          if (!info.timer) {
            info.timer = setTimeout(function () {
              info.listener.apply(this, info.args);
              info.timer = null;
            }.bind(self), info.interval);
          }
          break;
        }
      }
    });

    return this;
  },

  on: function (eventName, listener) {
    if (!listener || typeof listener !== 'function') {
      throw Error('Listener for on()/addEventListener() method is not a function');
    }
    if (!listener._hashCode) {
      Object.defineProperty(listener, '_hashCode', {
        value: Math.floor(Date.now() * Math.random()),
        writable: false,
        enumerable: false
      });
    }
    this[SUBSCRIPTIONS_FIELD][eventName] = this[SUBSCRIPTIONS_FIELD][eventName] || {};
    this[SUBSCRIPTIONS_FIELD][eventName][listener._hashCode] = {
      'kind': 'on',
      'listener': listener
    };
    return this;
  },

  onThrottled: function (eventName, listener, interval) {
    if (!listener || typeof listener !== 'function') {
      throw Error('Listener for on()/addEventListener() method is not a function');
    }
    if (typeof interval !== 'number' || interval < 1) {
      throw Error('interval argument must be bigger number than 0');
    }
    if (!listener._hashCode) {
      Object.defineProperty(listener, '_hashCode', {
        value: Math.floor(Date.now() * Math.random()),
        writable: false,
        enumerable: false
      });
    }
    this[SUBSCRIPTIONS_FIELD][eventName] = this[SUBSCRIPTIONS_FIELD][eventName] || {};
    this[SUBSCRIPTIONS_FIELD][eventName][listener._hashCode] = {
      'kind': 'onThrottled',
      'interval': interval,
      'timer': null,
      'listener': listener
    };
    return this;
  },

  off: function (eventName, listener) {
    if (!eventName && !listener) {
      this[SUBSCRIPTIONS_FIELD] = {};
      return this;
    }

    if (eventName && !listener) {
      this[SUBSCRIPTIONS_FIELD][eventName] = null;
      delete this[SUBSCRIPTIONS_FIELD][eventName];
    } else if (this[SUBSCRIPTIONS_FIELD][eventName] && listener._hashCode) {
      delete this[SUBSCRIPTIONS_FIELD][eventName][listener._hashCode];
    }

    return this;
  },

  one: function (eventName, listener) {
    if (!listener || typeof listener !== 'function') {
      throw Error('Listener for one()/addEventListenerOnce() method is not a function');
    }

    var self = this;

    var callback = function () {
      self.off(eventName, arguments.callee);
      listener.apply(self, arguments);
      callback = undefined;
    };
    this.on(eventName, callback);

    return this;
  },

  hasEventListener: function (eventName) {
    return (eventName in this[SUBSCRIPTIONS_FIELD] && Object.keys(this[SUBSCRIPTIONS_FIELD][eventName]).length > 0);
  },

  destroy: function () {
    this.off();
    this.empty();
  },

  errorHandler: function (error) {
    if (error) {
      if (typeof console.error === 'function') {
        if (this.__pgmId) {
          console.error(this.__pgmId, error);
        } else {
          console.error(error);
        }
      } else {
        if (this.__pgmId) {
          console.log(this.__pgmId, error);
        } else {
          console.log(error);
        }
      }
      this.trigger('error', error instanceof Error ? error : createError(error));
    }

    return false;
  }
};

BaseClass.prototype.addEventListener = BaseClass.prototype.on;
BaseClass.prototype.addThrottledEventListener = BaseClass.prototype.onThrottled;
BaseClass.prototype.addEventListenerOnce = BaseClass.prototype.one;
BaseClass.prototype.removeEventListener = BaseClass.prototype.off;

function createError(message, methodName, args) {
  var error = new Error(methodName ? [
    'Got error with message: "', message, '" ',
    'after calling "', methodName, '"'
  ].join('') : message);

  Object.defineProperties(error, {
    methodName: {
      value: methodName
    },
    args: {
      value: args
    }
  });

  return error;
}

module.exports = BaseClass;
