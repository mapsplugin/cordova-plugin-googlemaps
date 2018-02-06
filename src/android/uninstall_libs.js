module.exports = function(ctx) {
  if (ctx.opts.cordova.platforms.indexOf('android') < 0) {
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

  var removeFile = function(filepath) {
    return Q.promise(function(resolve, reject, notify) {
      fs.stat(filepath, function(error, stat) {
        if (error || !stat) {
          resolve();
          return;
        }
        // for debug
        // console.log("[remove] " + filepath);
        fs.unlink(filepath, function() {
          resolve();
        });
      });
    });
  };

  return Q.Promise(function(resolve, reject, notify) {
    fs.stat(dstLibsDir, function(error, stat) {
      if (error || !stat) {
        resolve();
        return;
      }

      var files = [];
      files.push(removeFile(path.join(dstLibsDir, "arm64-v8a", "libtbxml.so")));
      files.push(removeFile(path.join(dstLibsDir, "armeabi", "libtbxml.so")));
      files.push(removeFile(path.join(dstLibsDir, "armeabi-v7a", "libtbxml.so")));
      files.push(removeFile(path.join(dstLibsDir, "mips", "libtbxml.so")));
      files.push(removeFile(path.join(dstLibsDir, "mips64", "libtbxml.so")));
      files.push(removeFile(path.join(dstLibsDir, "x86", "libtbxml.so")));
      files.push(removeFile(path.join(dstLibsDir, "x86_64", "libtbxml.so")));

      Q.all(files).then(function() {
        resolve();
      });
    });
  });

};
