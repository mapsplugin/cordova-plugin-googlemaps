/*****************************************************************************
 * Geocoder class
 *****************************************************************************/
var Geocoder = {};

Geocoder.geocode = function(geocoderRequest, callback) {
    geocoderRequest = geocoderRequest || {};

    if ("position" in geocoderRequest) {
        geocoderRequest.position.lat = geocoderRequest.position.lat || 0.0;
        geocoderRequest.position.lng = geocoderRequest.position.lng || 0.0;
    }
    var pluginExec = function() {
        cordova.exec(function(results) {
            if (typeof callback === "function") {
                callback(results);
            }
        }, function(error) {
            if (typeof callback === "function") {
                callback([], error);
            }
        }, "Geocoder", 'geocode', [geocoderRequest]);
    };

    pluginExec();
};

module.exports = Geocoder;
