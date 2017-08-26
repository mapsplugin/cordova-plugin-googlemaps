var VARS_FIELD = typeof Symbol === 'undefined' ? '__vars' + Date.now() : Symbol.for('vars');
var SUBSCRIPTIONS_FIELD = typeof Symbol === 'undefined' ? '__subs' + Date.now() : Symbol.for('subscriptions');


function BaseClass() {
  this[VARS_FIELD] = {};
  this[SUBSCRIPTIONS_FIELD] = {};
  this.errorHandler = this.errorHandler.bind(this);

  Object.defineProperty(this, 'hashCode', { value: Math.floor(Date.now() * Math.random()) });
}

BaseClass.prototype = {
  empty: function() {
    var vars = this[VARS_FIELD];

    Object.keys(vars).forEach(function(name) {
      vars[name] = null;
    });
  },

  get: function(key) {
    return this[VARS_FIELD].hasOwnProperty(key) ? this[VARS_FIELD][key] : undefined;
  },

  set: function(key, value, noNotify) {
    var prev = this.get(key);

    this[VARS_FIELD][key] = value;

    if (!noNotify && prev !== value) {
      this.trigger(key + '_changed', prev, value, key);
    }

    return this;
  },

  bindTo: function(key, target, targetKey, noNotify) {
    targetKey = targetKey || key;

    this.on(key + '_changed', function(oldValue, value) {
      target.set(targetKey, value, noNotify);
    });
  },

  trigger: function(eventName) {
    if (!eventName) {
      return this;
    }

    if (!this[SUBSCRIPTIONS_FIELD][eventName]) {
      return this;
    }

    var listeners = this[SUBSCRIPTIONS_FIELD][eventName];
    var i = listeners.length;
    var args = Array.prototype.slice.call(arguments, 1);

    while (i--) {
      listeners[i].apply(this, args);
    }

    return this;
  },

  on: function(eventName, listener) {
    var topic;
    this[SUBSCRIPTIONS_FIELD][eventName] = this[SUBSCRIPTIONS_FIELD][eventName] || [];
    topic = this[SUBSCRIPTIONS_FIELD][eventName];
    topic.push(listener);
    return this;
  },

  off: function(eventName, listener) {
    if (!eventName && !listener) {
      this[SUBSCRIPTIONS_FIELD] = {};
      return this;
    }

    if (eventName && !listener) {
      this[SUBSCRIPTIONS_FIELD][eventName] = null;
    } else if (this[SUBSCRIPTIONS_FIELD][eventName]) {
      var index = this[SUBSCRIPTIONS_FIELD][eventName].indexOf(listener);

      if (index !== -1) {
        this[SUBSCRIPTIONS_FIELD][eventName].splice(index, 1);
      }
    }

    return this;
  },

  one: function(eventName, listener) {

    var self = this;

    var callback = function() {
      self.off(eventName, arguments.callee);
      listener.apply(self, arguments);
    };
    this.on(eventName, callback);

    return this;
  },

  destroy: function() {
    this.off();
    this.empty();
  },

  errorHandler: function(error) {
    if (error) {
      if (typeof console.error === "function") {
        console.error(error);
      } else {
        console.log(error);
      }
      this.trigger('error', error instanceof Error ? error : createError(error));
    }

    return false;
  }
};

BaseClass.prototype.addEventListener = BaseClass.prototype.on;
BaseClass.prototype.addEventListenerOnce = BaseClass.prototype.one;
BaseClass.prototype.removeEventListener = BaseClass.prototype.off;

function createError(message, methodName, args) {
  var error = new Error(methodName ? [
    'Got error with message: "', message, '" ',
    'after calling "', methodName, '"'
  ].join('') : message);

  Object.defineProperties(error, {
    methodName: { value: methodName },
    args: { value: args }
  });

  return error;
}

module.exports = BaseClass;
