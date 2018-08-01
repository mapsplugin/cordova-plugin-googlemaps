/*jshint esversion: 6 */
const fs = require('fs'),
  path = require('path'),
  xml2js = require('xml2js');

module.exports = (req, res, next) => {
  let API_KEY = req.params.api_key;
  let configXmlPath = path.resolve(req.shell.settings.workspace, 'config.xml');

  return new Promise((resolve, reject) => {
    //---------------------------------
    // update plugins/android.json
    //---------------------------------
    let jsonFilePath = path.resolve(req.shell.settings.workspace, 'plugins', 'android.json');
    try {
      let plugins = require(jsonFilePath);
      let plugin = plugins.installed_plugins['cordova-plugin-googlemaps'];
      plugin.API_KEY_FOR_ANDROID = API_KEY;

      fs.writeFile(jsonFilePath, JSON.stringify(plugins, null, 2), 'utf8', (error) => {
        if (error) {
          reject(error);
        } else {
          res.green("  [OK] ");
          res.white('plugins/android.json -> ');
          res.green("update.\n");
          resolve();
        }
      });
    } catch (error) {
      reject(error);
    }
  }).then(() => {
    return new Promise((resolve, reject) => {
      //---------------------------------
      // update platforms/android/android.json
      //---------------------------------
      let jsonFilePath = path.resolve(req.shell.settings.workspace, 'platforms', 'android', 'android.json');
      try {
        let plugins = require(jsonFilePath);
        let plugin = plugins.installed_plugins['cordova-plugin-googlemaps'];
        plugin.API_KEY_FOR_ANDROID = API_KEY;

        fs.writeFile(jsonFilePath, JSON.stringify(plugins, null, 2), 'utf8', (error) => {
          if (error) {
            reject(error);
          } else {
            res.green("  [OK] ");
            res.white('platforms/android/android.json -> ');
            res.green("update.\n");
            resolve();
          }
        });
      } catch (error) {
        reject(error);
      }
    });
  }).then(() => {
    return new Promise((resolve, reject) => {
      //---------------------------------
      // update package.json
      //---------------------------------
      let jsonFilePath = path.resolve(req.shell.settings.workspace, 'package.json');
      try {
        let plugins = require(jsonFilePath);
        let plugin = plugins.cordova.plugins["cordova-plugin-googlemaps"];
        plugin.API_KEY_FOR_ANDROID = API_KEY;

        fs.writeFile(jsonFilePath, JSON.stringify(plugins, null, 2), 'utf8', (error) => {
          if (error) {
            reject(error);
          } else {
            res.green("  [OK] ");
            res.white('platforms/android/android.json -> ');
            res.green("update.\n");
            resolve();
          }
        });
      } catch (error) {
        reject(error);
      }
    });
  }).then(() => {
    return new Promise((resolve, reject) => {
      //---------------------------------
      // update plugins/fetch.json
      //---------------------------------
      let jsonFilePath = path.resolve(req.shell.settings.workspace, 'plugins', 'fetch.json');
      try {
        let plugins = require(jsonFilePath);
        plugins['cordova-plugin-googlemaps'].variables.API_KEY_FOR_ANDROID = API_KEY;

        fs.writeFile(jsonFilePath, JSON.stringify(plugins, null, 2), 'utf8', (error) => {
          if (error) {
            reject(error);
          } else {
            res.green("  [OK] ");
            res.white('plugins/fetch.json -> ');
            res.green("update.\n");
            resolve();
          }
        });
      } catch (error) {
        reject(error);
      }
    });
  }).then(() => {
    return new Promise((resolve, reject) => {

      //--------------------------------
      // Is Android platform available?
      //--------------------------------
      let exists = fs.existsSync(configXmlPath);

      if (!exists) {
        reject(new errors.ConfigXmlNotFoundError(configXmlPath));
        return;
      }

      //---------------------------------
      // Read config.xml file
      //---------------------------------
      fs.readFile(configXmlPath, 'utf8', (error, data) => {
        if (error) {
          reject(error);
        } else {
          resolve(data);
        }
      });
    });
  }).then((data) => {
    //---------------------------------
    // Parse config.xml file
    //---------------------------------
    return new Promise((resolve, reject) => {
      let xmlParser = new xml2js.Parser();
      xmlParser.parseString(data + "", (error, configXmlData) => {
        if (error) {
          reject(error);
        } else {
          resolve(configXmlData);
        }
      });
    });
  }).then(xmlJson => {
    //---------------------------------
    // update
    //---------------------------------
    return new Promise((resolve, reject) => {
      if (xmlJson.widget.plugin) {
        xmlJson.widget.plugin.forEach(node => {
          if (node.$.name === 'cordova-plugin-googlemaps') {
            node.variable.forEach(variable => {
              if (variable.$.name === 'API_KEY_FOR_ANDROID') {
                variable.$.value = API_KEY;
              }
            });
          }
        });
      }
      let xmlBuilder = new xml2js.Builder();
      resolve(xmlBuilder.buildObject(xmlJson));
    });
  })
  .then(newXmlTxt => {
    //---------------------------------
    // Update config.xml
    //---------------------------------
    return new Promise((resolve, reject) => {
      fs.writeFile(configXmlPath, newXmlTxt, 'utf8', (error) => {
        if (error) {
          reject(error);
        } else {
          res.green("  [OK] ");
          res.white('config.xml -> ');
          res.green("update.\n");
          resolve();
        }
      });
    });
  });
};
