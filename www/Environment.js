var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * Config Class
 *****************************************************************************/
var Environment = function() {
    BaseClass.apply(this);
};

utils.extend(Environment, BaseClass);

Environment.prototype.setBackgroundColor = function(color) {
    this.set('strokeColor', color);
    cordova.exec(null, this.errorHandler, 'Environment', 'setBackGroundColor', [common.HTMLColor2RGBA(color)]);
};

Environment.prototype.setDebuggable = function(debug) {
    var self = this;
    debug = common.parseBoolean(debug);
    cordova.exec(null, self.errorHandler, 'Environment', 'setDebuggable', [debug]);
};

Environment.prototype.isAvailable = function(callback) {
    var self = this;

    cordova.exec(function() {
        if (typeof callback === "function") {
            callback.call(self, true);
        }
    }, function(message) {
        if (typeof callback === "function") {
            callback.call(self, false, message);
        }
    }, 'Environment', 'isAvailable', ['']);
};


Environment.prototype.getLicenseInfo = function(callback) {
    var self = this;
    cordova.exec(function(txt) {
        callback.call(self, txt);
    }, self.errorHandler, 'Environment', 'getLicenseInfo', []);
};


module.exports = Environment;
