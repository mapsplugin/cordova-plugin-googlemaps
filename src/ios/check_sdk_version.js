module.exports = function(ctx) {

  var PluginInfoProvider = ctx.requireCordovaModule('cordova-common').PluginInfoProvider;

  var Q = ctx.requireCordovaModule('q'),
      path = ctx.requireCordovaModule('path');

  var projectRoot = ctx.opts.projectRoot;
  return Q.Promise(function(resolve, reject, notify) {

    var pluginsDir = path.join(projectRoot, 'plugins');
    // TODO: This should list based off of platform.json, not directories within plugins/
    var pluginInfoProvider = new PluginInfoProvider();
    var plugins = pluginInfoProvider.getAllWithinSearchPath(pluginsDir);
    var pluginInfo;
    for (var i = 0; i < plugins.length; i++) {
      pluginInfo = plugins[i];
      if (pluginInfo.id === "com.googlemaps.ios") {
        var version = parseInt(pluginInfo.version.replace(/[^\d]/g, ""), 10);
        if (version < 260) {
          var errorMsg = [];
          errorMsg.push("-------[cordova googlemaps plugin error]----------");
          errorMsg.push("   This version requires 'com.googlemaps.ios@2.6.0'.");
          errorMsg.push("   Please reinstall the iOS SDK with following steps:");
          errorMsg.push("");
          errorMsg.push("   $> cordova plugin rm com.googlemaps.ios -f ");
          errorMsg.push("   $> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps-sdk#2.6.0");
          errorMsg.push("-------------------------------------------------");
          reject(errorMsg.join("\n"));
          return;
        }
      }
    }

    resolve();
  });

};
