var exec = require('cordova/exec'),
    screenOrientation = {},
    iosOrientation = 'unlocked',
    orientationMap  = {
        'portrait': [0,180],
        'portrait-primary': [0],
        'portrait-secondary': [180],
        'landscape': [-90,90],
        'landscape-primary': [-90],
        'landscape-secondary': [90],
        'default': [-90,90,0]
    };

screenOrientation.setOrientation = function(orientation) {
    iosOrientation = orientation;

    var success = function(res) {
        if (orientation === 'unlocked' && res.device) {
            iosOrientation = res.device;
            setTimeout(function() {
                iosOrientation = 'unlocked';
            },300);
        }
    };

    exec(success, null, "YoikScreenOrientation", "screenOrientation", ['set', orientation]);
};

module.exports = screenOrientation;

// ios orientation callback/hook
window.shouldRotateToOrientation = function(orientation) {
    var map = orientationMap[iosOrientation] || orientationMap['default'];
    return map.indexOf(orientation) >= 0;
};
