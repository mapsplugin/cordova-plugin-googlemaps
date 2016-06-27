var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    BaseClass = require('./BaseClass');

/*****************************************************************************
 * Config Class
 *****************************************************************************/
var Config = function() {
    BaseClass.apply(this);
};

utils.extend(Config, BaseClass);

Config.prototype.getPosition = function(callback) {
    var self = this;
    cordova.exec(function(latlng) {
        if (typeof callback === "function") {
            callback.call(self, new LatLng(latlng.lat, latlng.lng));
        }
    }, self.errorHandler, PLUGIN_NAME, 'getPosition', [this.getId()]);
};


module.exports = Config;
