module.exports = function(ctx) {

  var fs = require('fs'),
    path = require('path');
  var projectRoot = ctx.opts.projectRoot,
    configXmlPath = path.join(projectRoot, 'config.xml'),
    pluginXmlPath = path.join(__dirname, '..', 'plugin.xml');

  var versions = ctx.opts.cordova.version.split(/\./g);
  if (versions[0] > 6) {
    // If cordova platform version is higher than 6,
    // cordova-cli works well, so skip it.
    return;
  }

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


  return new Promise(function(resolve, reject) {
    var exec = require('child_process').exec;
    exec('npm install xml2js@0.4.19 minimist@1.2.0 --save 2>&1', function(err, stdout) {
      if (err) {
        reject(err);
      } else {
        console.log(stdout);
        resolve();
      }
    });
  })
    .then(function() {
      return new Promise(function(resolve, reject) {
        if (fs.existsSync(pluginXmlPath + '.original')) {
        // Copy the original plugin.xml to the current plugin.xml
          return fs.createReadStream(pluginXmlPath + '.original')
            .pipe(fs.createWriteStream(pluginXmlPath))
            .on('error', reject)
            .on('close', resolve);
        } else {
        // Backup the original plugin.xml file
          return fs.createReadStream(pluginXmlPath)
            .pipe(fs.createWriteStream(pluginXmlPath + '.original'))
            .on('error', reject)
            .on('close', resolve);
        }
      });
    })
    .then(function() {
      return new Promise(function(resolve, reject) {
      //---------------------------
      // Read the config.xml file
      //---------------------------
        fs.readFile(configXmlPath, function(error, data) {
          if (error) {
            reject(error);
          } else {

            //---------------------------
            // Parse the xml data
            //---------------------------
            var xml2js = require('xml2js');
            var xmlParser = new xml2js.Parser();
            xmlParser.parseString(data + '', function(error, configXmlData) {
              if (error) {
                reject(error);
              } else {
                resolve(configXmlData);
              }
            });
          }
        });
      });
    })
    .then(function(configXmlData) {
    //------------------------------------------------------------------------------
    // Check the xml data.
    // If there is no definition of this plugin in the config.xml,
    // then insert some dummy data in order to prevent the API_KEY_FOR_ANDROID error.
    //------------------------------------------------------------------------------
      return new Promise(function(resolve) {
        var hasPluginGoogleMaps = false;
        configXmlData.widget.plugin = configXmlData.widget.plugin || [];
        configXmlData.widget.plugin = configXmlData.widget.plugin.map(function(plugin) {
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
          configXmlData.widget.plugin.push({
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
        resolve(configXmlData);
      });
    })
    .then(function(configXmlData) {
      return new Promise(function(resolve, reject) {
      //---------------------------
      // Read the plugin.xml file
      //---------------------------
        fs.readFile(pluginXmlPath, function(error, data) {
          if (error) {
            reject(error);
          } else {
          //---------------------------
          // Parse the xml data
          //---------------------------
            var xml2js = require('xml2js');
            var xmlParser = new xml2js.Parser();
            xmlParser.parseString(data + '', function(error, pluginXmlData) {
              if (error) {
                reject(error);
              } else {
                resolve({
                  configXmlData: configXmlData,
                  pluginXmlData: pluginXmlData,
                  pluginXmlTxt: data + ''
                });
              }
            });
          }
        });
      });
    })
    .then(function(params) {
      return new Promise(function(resolve, reject) {
      //------------------------------
      // Read the install variables
      //------------------------------
        var mapsPlugin = params.configXmlData.widget.plugin.filter(function(plugin) {
          return (plugin.$.name === 'cordova-plugin-googlemaps');
        })[0];
        var variables = {};
        mapsPlugin.variable.forEach(function(variable) {
          variables[variable.$.name] = variable.$.value;
        });


        //------------------------------
        // Read default preferences
        //------------------------------
        var findPreference = function(xmlData) {
          var results = {};
          var keys = Object.keys(xmlData);
          keys.forEach(function(tagName) {
            switch(tagName) {
            case '$':
            case 'js-module':
            case 'engines':
            case 'config-file':
            case 'info':
              //ignore
              break;

            case 'preference':
              if (Array.isArray(xmlData[tagName])) {
                xmlData[tagName].forEach(function(node) {
                  results[node.$.name] = node.$.default;
                });
              } else {
                results[xmlData[tagName].$.name] = xmlData[tagName].$.default;
              }
              break;

            case 'plugin':
              results = findPreference(xmlData.plugin);
              break;

            default:
              if (Array.isArray(xmlData[tagName])) {
                xmlData[tagName].forEach(function(node) {
                  results = Object.assign(findPreference(node), results);
                });
              }
            }
          });
          return results;
        };
        var pluginDefaults = findPreference(params.pluginXmlData);
        variables = Object.assign(pluginDefaults, variables);

        //----------------------------------
        // Parse the command line variables
        //----------------------------------
        if (ctx.cmdLine.includes('cordova plugin add')) {
          var phrses = require('minimist')(ctx.cmdLine.split(' '));
          if (Array.isArray(phrses.variable)) {
            phrses.variable.forEach(function(line) {
              var tmp = line.split('=');
              variables[tmp[0]] = tmp[1];
            });
          }
        }

        //--------------------------------
        // Override the plugin.xml itself
        //--------------------------------
        params.pluginXmlTxt = params.pluginXmlTxt.replace(/\$([A-Z0-9_]+)/g, function(matchWhole, varName) {
          return variables[varName] || matchWhole;
        });

        fs.writeFile(pluginXmlPath, params.pluginXmlTxt, 'utf8', function(error) {
          if (error) {
            reject(error);
          } else {
            resolve();
          }
        });
      });
    });

};
