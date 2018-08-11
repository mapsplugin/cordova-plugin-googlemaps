module.exports = function(ctx) {

  var versions = ctx.opts.cordova.version.split(/\./g);
  if (versions[0] > 6) {
    // If cordova platform version is higher than 6,
    // cordova-cli works well, so skip it.
    return;
  }

  var fs = ctx.requireCordovaModule('fs'),
      path = ctx.requireCordovaModule('path'),
      Q = ctx.requireCordovaModule('q');
  var pluginXmlPath = path.join(__dirname, '..', 'plugin.xml');

  return Q.Promise(function(resolve, reject, notify) {
    // Copy the original plugin.xml to the current plugin.xml
    return fs.createReadStream(pluginXmlPath + '.original')
        .pipe(fs.createWriteStream(pluginXmlPath))
        .on("error", reject)
        .on("close", resolve);
  });

};
