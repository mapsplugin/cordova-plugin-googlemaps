
module.exports = {
  'isAvailable': function(onSuccess, onError, args) {
    onSuccess();
  },
  'setBackGroundColor': function(onSuccess, onError, args) {
    // stub
    onSuccess();
  },
  'getLicenseInfo': function(onSuccess, onError, args) {
    // stub
    onSuccess("cordova-plugin-googlemaps for browser does not need to display any open source lincenses. But for iOS, you still need to display the lincense.");
  },
};


require('cordova/exec/proxy').add('PluginEnvironment', module.exports);
