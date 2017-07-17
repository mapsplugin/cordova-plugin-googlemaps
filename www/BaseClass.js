
var _globalListeners = {};

function globalEventListener(e) {
  var eventName = e.type;
  if (!_globalListeners[eventName]) {
    return;
  }

  var hashCode = e.myself.hashCode;
  if (!hashCode || !_globalListeners[eventName][hashCode]) {
    return;
  }
  var callbacks = _globalListeners[eventName][hashCode];
  for (var i = 0; i < callbacks.length; i++) {
    callbacks[i].listener(e);
  }
}

var BaseClass = function() {
  var self = this;
  var _vars = {};

  var hashCode = Math.floor(Date.now() * Math.random());

  Object.defineProperty(self, "hashCode", {
      value: hashCode,
      writable: false
  });
  self.empty = function() {
    for (var key in Object.keys(_vars)) {
      _vars[key] = null;
      delete _vars[key];
    }
  };

  self.get = function(key) {
    return key in _vars ? _vars[key] : null;
  };
  self.set = function(key, value, noNotify) {
    var prev = _vars[key];
    _vars[key] = value;
    if (!noNotify && prev !== value) {
      self.trigger(key + "_changed", prev, value);
    }
  };
  self.bindTo = function(key, target, targetKey, noNotify) {
    targetKey = targetKey === undefined || targetKey === null ? key : targetKey;
    self.on(key + "_changed", function(prevValue, newValue) {
      target.set(targetKey, newValue, noNotify === true);
    });
  };

  self.trigger = function(eventName) {
    if (!eventName || !_globalListeners[eventName] || !_globalListeners[eventName][hashCode]) {
      return;
    }

    var args = [];
    for (var i = 1; i < arguments.length; i++) {
      args.push(arguments[i]);
    }
    var event = document.createEvent('Event');
    event.initEvent(eventName, false, false);
    event.mydata = args;
    event.myself = self;

    var callbacks = _globalListeners[eventName][hashCode];
    for (var i = 0; i < callbacks.length; i++) {
      callbacks[i].listener(event);
    }
  };
  self.on = function(eventName, callback) {
    if (!eventName || !callback || typeof callback !== "function") {
      return;
    }

    _globalListeners[eventName] = _globalListeners[eventName] || {};

    var listener = function(e) {
      if (!e.myself || e.myself !== self) {
        return;
      }
      var evt = Object.create(e);
      var mydata = evt.mydata;
      delete evt.mydata;
      delete evt.myself;
      self.event = evt;
      callback.apply(self, mydata);
    };
    if (!(hashCode in _globalListeners[eventName])) {
      document.addEventListener(eventName, globalEventListener, false);
      _globalListeners[eventName][hashCode] = [];
    }
    _globalListeners[eventName][hashCode].push({
      'callback': callback,
      'listener': listener
    });
  };
  self.addEventListener = self.on;

  self.off = function(eventName, callback) {
    var i, j, callbacks;
    if (typeof eventName === "string") {
      if (eventName in _globalListeners) {

        if (typeof callback === "function") {
          callbacks = _globalListeners[eventName][hashCode] ||[];
          for (i = 0; i < callbacks.length; i++) {
            if (callbacks[i].callback === callback) {
              callbacks.splice(i, 1);
              break;
            }
          }
          if (callbacks.length === 0) {
            delete _globalListeners[eventName][hashCode];
          }
        } else {
          delete _globalListeners[eventName][hashCode];
        }
        if (Object.keys(_globalListeners[eventName]) === 0) {
          document.removeEventListener(eventName, globalEventListener);
        }
      }
    } else {
      //Remove all event listeners
      var eventNames = Object.keys(_globalListeners);
      for (j = 0; j < eventNames.length; j++) {
        eventName = eventNames[j];
        delete _globalListeners[eventName][hashCode];
        if (Object.keys(_globalListeners[eventName]) === 0) {
          document.removeEventListener(eventName, globalEventListener);
        }
      }
    }
  };

  self.removeEventListener = self.off;


  self.one = function(eventName, callback) {

    var listener = function(e) {
      if (!e.myself || e.myself !== self) {
        return;
      }
      var evt = Object.create(e);
      var mydata = evt.mydata;
      delete evt.mydata;
      delete evt.myself;
      self.event = evt;
      callback.apply(self, mydata);
      self.off(eventName, callback);
    };

    _globalListeners[eventName] = _globalListeners[eventName] || {};

    if (!(hashCode in _globalListeners[eventName])) {
      document.addEventListener(eventName, globalEventListener, false);
      _globalListeners[eventName][hashCode] = [];
    }
    _globalListeners[eventName][hashCode].push({
      'callback': callback,
      'listener': listener
    });
  };
  self.addEventListenerOnce = self.one;

  self.errorHandler = function(msg) {
    if (msg) {
      console.log(msg);
      self.trigger('error', msg);
    }
    return false;
  };

  return self;
};


module.exports = BaseClass;
