var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common');

/*****************************************************************************
 * Config Class
 *****************************************************************************/
var Environment = {};

Environment.setBackgroundColor = function(color) {
    cordova.exec(null, null, 'Environment', 'setBackGroundColor', [common.HTMLColor2RGBA(color)]);
};

Environment.setDebuggable = function(debug) {
    debug = common.parseBoolean(debug);
    cordova.exec(null, null, 'Environment', 'setDebuggable', [debug]);
};

Environment.isAvailable = function(callback) {
    cordova.exec(function() {
        if (typeof callback === "function") {
            callback(true);
        }
    }, function(message) {
        if (typeof callback === "function") {
            callback(false, message);
        }
    }, 'Environment', 'isAvailable', ['']);
};


Environment.getLicenseInfo = function(callback) {
    cordova.exec(function(txt) {
        callback(txt);
    }, null, 'Environment', 'getLicenseInfo', []);
};


module.exports = Environment;
