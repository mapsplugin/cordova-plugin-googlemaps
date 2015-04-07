var screenOrientation = {};

screenOrientation.setOrientation = function(orientation) {
    if (blackberry.app) {
        if (orientation === 'unlocked') {
            blackberry.app.unlockOrientation();
        } else {
            blackberry.app.lockOrientation(orientation);
        }
    }
};

module.exports = screenOrientation;