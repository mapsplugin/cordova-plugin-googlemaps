module.exports = function(ctx) {

  var versions = ctx.opts.cordova.version.split(/\./g);
  if (versions[0] > 6) {
    // If cordova platform version is higher than 6,
    // cordova-cli works well, so skip it.
    return;
  }

  var fs = require('fs'),
    path = require('path');
  var projectRoot = ctx.opts.projectRoot,
    configXmlPath = path.join(projectRoot, 'config.xml');

  var Module = require('module').Module;
  var NODE_MODULES_DIR = path.join(__dirname, '..', 'node_modules');
  if (fs.existsSync(NODE_MODULES_DIR)) {
    var old_nodeModulePaths = Module._nodeModulePaths;
    var oldPaths = Module._nodeModulePaths(projectRoot);
    if (oldPaths.indexOf(NODE_MODULES_DIR) === -1) {
      Module._nodeModulePaths = function(from) {
        var paths = old_nodeModulePaths.call(this, from);
        paths.push(NODE_MODULES_DIR);
        return paths;
      };
    }
  }

  var xml2js = require('xml2js');

  var rmdir = function(dir_path) {
    if (fs.existsSync(dir_path)) {
      fs.readdirSync(dir_path).forEach(function(entry) {
        var entry_path = path.join(dir_path, entry);
        if (fs.lstatSync(entry_path).isDirectory()) {
          rmdir(entry_path);
        } else {
          fs.unlinkSync(entry_path);
        }
      });
      fs.rmdirSync(dir_path);
    }
  };

  return new Promise(function(resolve, reject) {
    //---------------------------
    // Read the config.xml file
    //---------------------------
    fs.readFile(configXmlPath, function(error, data) {
      if (error) {
        reject(error);
      } else {
        resolve(data + '');
      }
    });
  })
    .then(function(data) {
    //---------------------------
    // Parse the xml data
    //---------------------------
      return new Promise(function(resolve, reject) {
        var xmlParser = new xml2js.Parser();
        xmlParser.parseString(data, function(error, data) {
          if (error) {
            reject(error);
          } else {
            resolve(data);
          }
        });

      });
    })
    .then(function(data) {
    //------------------------------------------------------------------------------
    // Check the xml data.
    // If there is no definition of this plugin in the config.xml,
    // then insert some dummy data in order to prevent the API_KEY_FOR_ANDROID error.
    //------------------------------------------------------------------------------
      return new Promise(function(resolve) {
        var hasPluginGoogleMaps = false;
        data.widget.plugin = data.widget.plugin || [];
        data.widget.plugin = data.widget.plugin.map(function(plugin) {
          if (plugin.$.name !== 'cordova-plugin-googlemaps') {
            return plugin;
          }

          hasPluginGoogleMaps = true;
          var variables = {};
          plugin.variable = plugin.variable || [];
          plugin.variable.forEach(function(variable) {
            variables[variable.$.name] = variable.$.value;
          });
          if (!('API_KEY_FOR_ANDROID' in variables)) {
            plugin.variable.push({
              '$' : {
                'name': 'API_KEY_FOR_ANDROID',
                'value': '(API_KEY_FOR_ANDROID)'
              }
            });
          }
          if (!('API_KEY_FOR_IOS' in variables)) {
            plugin.variable.push({
              '$' : {
                'name': 'API_KEY_FOR_IOS',
                'value': '(API_KEY_FOR_IOS)'
              }
            });
          }
          return plugin;
        });

        if (!hasPluginGoogleMaps) {
          data.widget.plugin.push({
            '$' : {
              'name': 'cordova-plugin-googlemaps',
              'spec': 'dummy'
            },
            'variable' : [
              {'$': {
                'name': 'API_KEY_FOR_ANDROID',
                'value': '(API_KEY_FOR_ANDROID)'
              }
              },
              {
                '$': {
                  'name': 'API_KEY_FOR_IOS',
                  'value': '(API_KEY_FOR_IOS)'
                }
              }
            ]
          });
        }
        resolve(data);
      });
    })
    .then(function(data) {
    //---------------------------
    // Override the config.xml
    //---------------------------
      return new Promise(function(resolve, reject) {
        var builder = new xml2js.Builder();
        var xml = builder.buildObject(data);
        fs.writeFile(configXmlPath, xml, 'utf8', function(error) {
          if (error) {
            reject(error);
          } else {
            resolve();
          }
        });
      });
    })
    .then(function() {
      ctx.opts.cordova.platforms.forEach(function(platformName) {
        rmdir(path.join(projectRoot, 'platforms', platformName, 'platform_www', 'plugins', 'cordova-plugin-googlemaps'));
        rmdir(path.join(projectRoot, 'platforms', platformName, 'www', 'plugins', 'cordova-plugin-googlemaps'));
      });
    });


};
