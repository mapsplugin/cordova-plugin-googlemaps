/*jshint esversion: 6 */
const fs = require('fs'),
  path = require('path'),
  xml2js = require('xml2js'),
  errors = require('../../errors');

module.exports = (req, res, next) => {

  let API_KEY = req.params.api_key;
  let manifestPath = path.join(req.shell.settings.ANDROID_ROOT, 'AndroidManifest.xml');

  return new Promise((resolve, reject) => {

    //--------------------------------
    // Is Android platform available?
    //--------------------------------
    let exists = fs.existsSync(manifestPath);

    if (!exists) {
      reject(new errors.AndroidManifestNotFoundError(manifestPath));
      return;
    }

    //---------------------------------
    // Read AndroidManifest.xml file
    //---------------------------------
    fs.readFile(manifestPath, 'utf8', (error, data) => {
      if (error) {
        reject(error);
      } else {
        resolve(data);
      }
    });
  })
  .then(data => {
    //---------------------------------
    // Parse AndroidManifest.xml file
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
  })
  .then(xmlJson => {
    //---------------------------------
    // Does the file have <meta-data android:name="com.google.android.geo.API_KEY"/>?
    //---------------------------------
    return new Promise((resolve, reject) => {
      if (xmlJson.manifest.application) {
        let appNode = xmlJson.manifest.application;
        appNode.forEach(node => {
          if (node['meta-data']) {
            node['meta-data'].forEach(meta => {
              if (meta.$['android:name'] === 'com.google.android.geo.API_KEY') {
                meta.$['android:value'] = API_KEY;
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
    // Update AndroidManifest.xml
    //---------------------------------
    return new Promise((resolve, reject) => {
      fs.writeFile(manifestPath, newXmlTxt, 'utf8', (error) => {
        if (error) {
          reject(error);
        } else {
          res.green("  [OK] ");
          res.white('AndroidManifest.xml -> ');
          res.green("update.\n");
          resolve();
        }
      });
    });
  });

};
