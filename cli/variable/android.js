/*jshint esversion: 6 */
const fs = require('fs'),
  path = require('path'),
  errors = require('../errors');

module.exports = (req, res, next) => {

  //--------------------------------
  // Is Android platform available?
  //--------------------------------
  let manifestPath = path.join(req.shell.settings.ANDROID_ROOT, 'AndroidManifest.xml');
  let exists = fs.existsSync(manifestPath);

  if (!exists) {
    next(new errors.AndroidManifestNotFoundError(manifestPath, req.shell.settings.PROJECT_PLATFORM));
    return;
  } else {
    res.green("  [OK] ");
    res.white('AndroidManifest.xml -> ');
    res.green("found.\n");
  }
  res.cyan(`api key -> ${req.params.api_key}\n`);
  res.prompt();
};
