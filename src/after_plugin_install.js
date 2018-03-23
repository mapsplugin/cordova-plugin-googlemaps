module.exports = function(ctx) {

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
