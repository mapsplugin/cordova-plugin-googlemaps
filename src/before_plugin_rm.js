module.exports = function(ctx) {

  var fs = ctx.requireCordovaModule('fs'),
      path = ctx.requireCordovaModule('path'),
      Q = ctx.requireCordovaModule('q');

  var xml2js = require('../node_modules/xml2js');

  var projectRoot = ctx.opts.projectRoot,
    configXmlPath = path.join(projectRoot, "config.xml");

  return Q.Promise(function(resolve, reject, notify) {
    //---------------------------
    // Read the config.xml file
    //---------------------------
    fs.readFile(configXmlPath, function(error, data) {
      if (error) {
        reject(error);
      } else {
        resolve(data + "");
      }
    });
  })
  .then(function(data) {
    //---------------------------
    // Parse the xml data
    //---------------------------
    return Q.Promise(function(resolve, reject, notify) {
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
    return Q.Promise(function(resolve, reject, notify) {
      var hasPluginGoogleMaps = false;
      data.widget.plugin = data.widget.plugin.map(function(plugin) {
        if (plugin.$.name !== "cordova-plugin-googlemaps") {
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
            {"$": {
                "name": "API_KEY_FOR_ANDROID",
                "value": "(API_KEY_FOR_ANDROID)"
              }
            },
            {
              "$": {
                "name": "API_KEY_FOR_IOS",
                "value": "(API_KEY_FOR_IOS)"
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
    return Q.Promise(function(resolve, reject, notify) {
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
  });


};
