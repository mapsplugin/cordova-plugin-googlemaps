var BaseClass = require('./BaseClass'),
    utils = require('cordova/utils'),
    BaseArrayClass = require('./BaseArrayClass');


/*****************************************************************************
 * Overlay Class
 *****************************************************************************/
var Overlay = function(map, options, className, _exec, extras) {
  extras = extras || {};
  BaseClass.apply(this);

  var self = this;

  //-----------------------------------------------
  // Sets the initialize option to each property
  //-----------------------------------------------
  var ignores = ["map", "id", "hashCode", "type"];
  for (var key in options) {
      if (ignores.indexOf(key) === -1) {
          self.set(key, options[key]);
      }
  }

  //-------------------------------------------------------------------------------
  // If app code wants to execute some method before `_isReady = true`,
  // just stack in to the internal queue.
  // If this overlay is ready, execute it.
  //-------------------------------------------------------------------------------
  var cmdQueue = new BaseArrayClass();
  cmdQueue.on('insert_at', function() {
    if (!self._isReady) {
      return;
    }
    var cmd;
    while(cmdQueue.getLength() > 0) {
      cmd = cmdQueue.removeAt(0, true);
      if (cmd && cmd.target && cmd.args && cmd.args[0] !== "nop") {
        _exec.apply(cmd.target, cmd.args);
      }
    }
  });


  Object.defineProperty(self, "_cmdQueue", {
    enumerable: false,
    value: cmdQueue,
    writable: false
  });

  className = className.toLowerCase();
  Object.defineProperty(self, "_isReady", {
    value: false,
    writable: true
  });
  Object.defineProperty(self, "map", {
    value: map,
    writable: false
  });
  Object.defineProperty(self, "id", {
    value: extras.id || className + "_" + this.hashCode,
    writable: false
  });
  Object.defineProperty(self, "type", {
    value: className,
    writable: false
  });

  Object.defineProperty(self, "getPluginName", {
    writable: false,
    value: function() {
      return this.map.getId() + "-" + className.toLowerCase();
    }
  });
};

utils.extend(Overlay, BaseClass);

Overlay.prototype._privateInitialize = function(options) {
  var self = this;
  //-----------------------------------------------
  // Sets the initialize option to each property
  //-----------------------------------------------
  if (options) {
    var ignores = ["map", "id", "hashCode", "type"];
    for (var key in options) {
      if (ignores.indexOf(key) === -1) {
        self.set(key, options[key], true);
      }
    }
  }

  //-----------------------------------------------
  // Trigger internal command queue
  //-----------------------------------------------
  Object.defineProperty(self, "_isReady", {
    value: true,
    writable: false
  });
  self.exec("nop");
};


Overlay.prototype.exec = function() {
  this._cmdQueue.push.call(this._cmdQueue, {
    target: this,
    args: Array.prototype.slice.call(arguments, 0)
  });
};
Overlay.prototype.getId = function() {
  return this.id;
};
Overlay.prototype.getMap = function() {
  return this.map;
};
Overlay.prototype.getHashCode = function() {
  return this.hashCode;
};

module.exports = Overlay;
