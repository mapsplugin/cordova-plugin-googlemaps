var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    common = require('./Common');

/*****************************************************************************
 * Config Class
 *****************************************************************************/
var Environment = {};

Environment.setBackgroundColor = function(color) {
    cordova.exec(null, null, 'PluginEnvironment', 'setBackGroundColor', [common.HTMLColor2RGBA(color)]);
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
    }, 'PluginEnvironment', 'isAvailable', ['']);
};


Environment.getLicenseInfo = function(callback) {
    cordova.exec(function(txt) {
        callback(txt);
    }, null, 'PluginEnvironment', 'getLicenseInfo', []);
};


module.exports = Environment;
