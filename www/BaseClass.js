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

   self.deleteFromObject = function(object, type) {
       if (object === null) return object;
       for(var index in Object.keys(object)) {
           var key = Object.keys(object)[index];
           if (typeof object[key] === 'object') {
              object[key] = self.deleteFromObject(object[key], type);
           } else if (typeof object[key] === type) {
              delete object[key];
           }
       }
       return object;
   };

   self.get = function(key) {
       return key in _vars ? _vars[key] : null;
   };
   self.set = function(key, value) {
       if (_vars[key] !== value) {
           self.trigger(key + "_changed", _vars[key], value);
       }
       _vars[key] = value;
   };

   self.trigger = function(eventName) {
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
       _listeners[eventName] = _listeners[eventName] || [];

       var listener = function(e) {
           if (!e.myself || e.myself !== self) {
               return;
           }
           callback.apply(self, e.mydata);
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
           //Remove all event listeners except 'keepWatching_changed'
           var eventNames = Object.keys(_listeners);
           for (i = 0; i < eventNames.length; i++) {
               eventName = eventNames[i];
               if ( eventName !== 'keepWatching_changed' ) {
                   for (var j = 0; j < _listeners[eventName].length; j++) {
                       document.removeEventListener(eventName, _listeners[eventName][j].listener);
                   }
                   delete _listeners[eventName];
               }
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
           callback.apply(self, e.mydata);
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
