var Resolver = require('jest-resolve');
var path = require('path');
var fs = require('fs');

var pluginModuleRegex = /^cordova-plugin-googlemaps\.(.*)/;
var pathBrowser = './src/browser';
var pathWww = './www';

module.exports = function(moduleId, options) {
  if (pluginModuleRegex.test(moduleId)) {
    var fileName = moduleId.match(pluginModuleRegex)[1] + '.js';
    if (fs.existsSync(path.resolve(pathBrowser, fileName))) {
      return path.resolve(pathBrowser, fileName);
    }
    if (fs.existsSync(path.resolve(pathWww, fileName))) {
      return path.resolve(pathWww, fileName);
    }

    throw new Error(moduleId + 'could not be located');
  }
  return Resolver.findNodeModule(moduleId, options);
};
