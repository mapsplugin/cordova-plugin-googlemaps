var common = require('./Common');

/*****************************************************************************
 * Config Class
 *****************************************************************************/
var Environment = {};

Environment.setBackgroundColor = function (color) {
  cordova.exec(null, null, 'CordovaGoogleMaps', 'setBackGroundColor', [common.HTMLColor2RGBA(color)]);
};

Environment.isAvailable = function (callback) {
  cordova.exec(function () {
    if (typeof callback === 'function') {
      callback(true);
    }
  }, function (message) {
    if (typeof callback === 'function') {
      callback(false, message);
    }
  }, 'CordovaGoogleMaps', 'isAvailable', ['']);
};

Environment.getLicenseInfo = function (callback) {
  cordova.exec(function (txt) {
    callback(txt);
  }, null, 'CordovaGoogleMaps', 'getLicenseInfo', []);
};

Environment.setEnv = function (options) {
  if (options) {
    cordova.exec(null, null, 'CordovaGoogleMaps', 'setEnv', [options]);
  }
};

module.exports = Environment;
