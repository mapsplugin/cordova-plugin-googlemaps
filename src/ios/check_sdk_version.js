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
          console.error('\x1b[36m%s\x1b[0m', "-------[cordova googlemaps plugin error]----------");
          console.error('\x1b[36m%s\x1b[0m', "   This version requires 'com.googlemaps.ios@2.6.0'.");
          console.error('\x1b[36m%s\x1b[0m', "   Please reinstall the iOS SDK with following steps:");
          console.error('\x1b[36m%s\x1b[0m', "");
          console.error('\x1b[36m%s\x1b[0m', "   $> cordova plugin rm com.googlemaps.ios -f ");
          console.error('\x1b[36m%s\x1b[0m', "   $> cordova plugin add https://github.com/mapsplugin/cordova-plugin-googlemaps-sdk#2.6.0");
          console.error('\x1b[36m%s\x1b[0m', "-------------------------------------------------");
          reject();
          return;
        }
      }
    }

    resolve();
  });

  return ctx.cordova.plugin.list(projectRoot, {
    fire: function() {
      return Q.Promise(function(resolve, reject, notify) {
        resolve();
      });
    }
  })
  .then(function(pluginList) {
    console.log(pluginList);
  });

return;
  if (ctx.opts.cordova.platforms.indexOf('ios') < 0) {
      return;
  }

  var fs = ctx.requireCordovaModule('fs'),
      path = ctx.requireCordovaModule('path'),
      Q = ctx.requireCordovaModule('q');

  var projectRoot = ctx.opts.projectRoot;
  var configXml = fs.readFileSync(path.join(projectRoot, "config.xml")) + "";
  var matches = configXml.match(/engine name=\"android\" spec=\"(.+?)\"/gi);
  if (!matches) {
    return;
  }
  matches[0] = matches[0].replace(/engine name=\"android\" spec=\"~?([\d\.]+)\"/g, "$1");
  var androidLibVersion = parseInt(matches[0].replace(/\./g, ""), 10);
  var androidPlatformDir = path.join(projectRoot, "platforms", "android");
  var androidProjDir = (androidLibVersion >= 700) ? path.join("app", "src", "main") : "";
  androidProjDir = path.join(androidPlatformDir, androidProjDir);
  var dstLibsDir = path.join(androidProjDir, "libs");
  var tbxmlSource = path.join(__dirname, "tbxml-android", "libs");

  var copyFile = function(filename, srcDir, dstDir) {
    return Q.promise(function(resolve, reject, notify) {
      fs.stat(dstDir, function(error, stat) {
        if (error || !stat) {
          fs.mkdirSync(dstDir);
        }
        var srcFile = path.join(srcDir, filename);
        var dstFile = path.join(dstDir, filename);

        // for debug
        // console.log(" [copy] " + srcFile + " -> " + dstFile);
        fs.writeFileSync(dstFile, fs.readFileSync(srcFile));
        resolve();
      });
    });
  };

  return Q.Promise(function(resolve, reject, notify) {
    fs.stat(dstLibsDir, function(error, stat) {
      if (error || !stat) {
        fs.mkdirSync(dstLibsDir);
      }

      var files = [];
      files.push(copyFile("libtbxml.so", path.join(tbxmlSource, "arm64-v8a"), path.join(dstLibsDir, "arm64-v8a")));
      files.push(copyFile("libtbxml.so", path.join(tbxmlSource, "armeabi"), path.join(dstLibsDir, "armeabi")));
      files.push(copyFile("libtbxml.so", path.join(tbxmlSource, "armeabi-v7a"), path.join(dstLibsDir, "armeabi-v7a")));
      files.push(copyFile("libtbxml.so", path.join(tbxmlSource, "mips"), path.join(dstLibsDir, "mips")));
      files.push(copyFile("libtbxml.so", path.join(tbxmlSource, "mips64"), path.join(dstLibsDir, "mips64")));
      files.push(copyFile("libtbxml.so", path.join(tbxmlSource, "x86"), path.join(dstLibsDir, "x86")));
      files.push(copyFile("libtbxml.so", path.join(tbxmlSource, "x86_64"), path.join(dstLibsDir, "x86_64")));

      resolve(files);
    });
  })
  .then(function(files) {
    return Q.all(files);
  });

};
