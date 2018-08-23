module.exports = function(ctx) {
console.log("script", ctx.opts.cordova.platforms);
  if (ctx.opts.cordova.platforms.indexOf('browser') < 0) {
      return;
  }

  var fs = ctx.requireCordovaModule('fs'),
      path = ctx.requireCordovaModule('path'),
      deferral = ctx.requireCordovaModule('q').defer();

  var projectRoot = ctx.opts.projectRoot;
  var projectConfigXml = path.join(projectRoot, 'config.xml');
  var platformConfigXml = path.join(projectRoot, 'platforms', 'browser', 'config.xml');
  var platformConfigXml2 = path.join(projectRoot, 'www', 'config.xml');

  // Read (projectDir)/config.xml
  fs.readFile(projectConfigXml, function(err, fileData) {
    if (err) {
      deferral.resolve();
      return;
    }

    var matches = (fileData + "").match(/<variable.+?API_KEY_FOR_BROWSER.+?value=\"(.+?)\".+?\/>/i);
    if (!matches) {
      deferral.resolve();
      return;
    }
    API_KEY_FOR_BROWSER = matches[1];
    console.log("API_KEY_FOR_BROWSER = ", API_KEY_FOR_BROWSER);

    fs.readFile(platformConfigXml, function(err2, fileData2) {
      if (err2) {
        deferral.resolve();
        return;
      }

      fileData2 = fileData2 + "";
      if (fileData2.indexOf("API_KEY_FOR_BROWSER") > -1) {
        deferral.resolve();
        return;
      }
      fileData2 = fileData2.replace(/<\/widget>/, [
        '<preference name="API_KEY_FOR_BROWSER" value="' + API_KEY_FOR_BROWSER + '" />',
        "</widget>"
      ].join("\n"));

console.log(fileData2);

      fs.writeFile(platformConfigXml2, fileData2, function(err) {
        deferral.resolve();
      });

    });

  });


  return deferral.promise;
};
