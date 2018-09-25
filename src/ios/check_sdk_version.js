module.exports = function(ctx) {

  var PluginInfoProvider = ctx.requireCordovaModule('cordova-common').PluginInfoProvider;

  var Q = ctx.requireCordovaModule('q'),
      path = ctx.requireCordovaModule('path');

  var projectRoot = ctx.opts.projectRoot;
  return Q.Promise(function(resolve, reject, notify) {

    var pluginsDir = path.join(projectRoot, 'plugins');
    var pluginInfoProvider = new PluginInfoProvider();
    var plugins = pluginInfoProvider.getAllWithinSearchPath(pluginsDir);
    var pluginInfo;
    var needToUninstall = false;
    for (var i = 0; i < plugins.length; i++) {
      pluginInfo = plugins[i];
      if (pluginInfo.id === "com.googlemaps.ios") {
        needToUninstall = true;
        break;
      }
    }

    if (needToUninstall) {
      console.info("--[cordova-plugin-googlemaps]------------------------");
      console.info("From version 2.4.5, the cordova-plugin-googlemaps uses CocoaPod.");
      console.info("No longer necessary com.googlemaps.ios plugin.");
      console.info("Automatic uninstalling com.googlemaps.ios plugin...");
      console.info("-----------------------------------------------------");

      var exec = require('child_process').exec;
      exec('cordova plugin rm com.googlemaps.ios 2>&1', function(err, stdout) {
        if (err) {
          reject(err);
        } else {
          console.log(stdout);
          exec('npm uninstall cordova-plugin-googlemaps-sdk --save 2>&1', function() {
            resolve();
          });
        }
      });
    } else {
      resolve();
    }
  });

};
