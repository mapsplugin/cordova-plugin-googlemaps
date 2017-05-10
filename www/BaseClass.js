var BaseClass = function() {
   var self = this;
   var _vars = {};
   var _listeners = {};

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
       if (!eventName) {
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
       document.dispatchEvent(event);
   };
   self.on = function(eventName, callback) {
      if (!eventName || !callback || typeof callback !== "function") {
        return;
      }
       _listeners[eventName] = _listeners[eventName] || [];

       var listener = function(e) {
          if (!e.myself || e.myself !== self) {
            return;
          }
          var mydata = e.mydata;
          delete e.mydata;
          delete e.myself;
          self.event = e;
          callback.apply(self, mydata);
       };
       document.addEventListener(eventName, listener, false);
       _listeners[eventName].push({
           'callback': callback,
           'listener': listener
       });
   };
   self.addEventListener = self.on;

   self.off = function(eventName, callback) {
       var i;
       if (typeof eventName === "string") {
           if (eventName in _listeners) {

               if (typeof callback === "function") {
                   for (i = 0; i < _listeners[eventName].length; i++) {
                       if (_listeners[eventName][i].callback === callback) {
                           document.removeEventListener(eventName, _listeners[eventName][i].listener);
                           _listeners[eventName].splice(i, 1);
                           break;
                       }
                   }
               } else {
                   for (i = 0; i < _listeners[eventName].length; i++) {
                       document.removeEventListener(eventName, _listeners[eventName][i].listener);
                   }
                   delete _listeners[eventName];
               }
           }
       } else {
           //Remove all event listeners
           var eventNames = Object.keys(_listeners);
           for (i = 0; i < eventNames.length; i++) {
               eventName = eventNames[i];
               for (var j = 0; j < _listeners[eventName].length; j++) {
                   document.removeEventListener(eventName, _listeners[eventName][j].listener);
               }
               delete _listeners[eventName];
           }
           _listeners = {};
       }
   };

   self.removeEventListener = self.off;


   self.one = function(eventName, callback) {
       _listeners[eventName] = _listeners[eventName] || [];

       var listener = function(e) {
           if (!e.myself || e.myself !== self) {
               return;
           }
           var mydata = e.mydata;
           delete e.mydata;
           delete e.myself;
           self.event = e;
           callback.apply(self, mydata);
           self.off(eventName, callback);
       };
       document.addEventListener(eventName, listener, false);
       _listeners[eventName].push({
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
