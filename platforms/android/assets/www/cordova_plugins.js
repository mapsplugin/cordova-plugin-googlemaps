cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "file": "plugins/plugin.google.maps/www/googlemaps-cdv-plugin.js",
        "id": "plugin.google.maps.phonegap-googlemaps-plugin",
        "clobbers": [
            "window.plugins.phonegap-googlemaps-plugin"
        ]
    },
    {
        "file": "plugins/plugin.http.request/www/http-request.js",
        "id": "plugin.http.request.phonegap-http-requst",
        "clobbers": [
            "cordova.plugins.http-request"
        ]
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
    "plugin.google.maps": "1.0.11",
    "plugin.http.request": "1.0.0",
    "org.apache.cordova.console": "0.2.11",
    "com.googlemaps.ios": "1.9.1",
    "com.google.playservices": "23.0.0"
}
// BOTTOM OF METADATA
});