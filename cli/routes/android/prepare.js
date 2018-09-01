/*jshint esversion: 6 */
const fs = require('fs'),
  path = require('path'),
  errors = require('../../errors'),
  values = require('../../values');

module.exports = (req, res, next) => {

  return new Promise((resolve, reject) => {

    //-----------------------------------
    // Detect project platform
    // (support: cordova and capacitor)
    //-----------------------------------
    let PROJECT_ROOT = req.shell.settings.workspace;
    let PROJECT_PLATFORM = values.PLATFORM_UNKNOWN;
    let ANDROID_ROOT = null;

    if (fs.existsSync(path.join(PROJECT_ROOT, 'capacitor.config.json'))) {
      //-----------------
      // @capacitor
      //-----------------
      PROJECT_PLATFORM = values.PLATFORM_CAPACITOR;
      ANDROID_ROOT = path.resolve(
        PROJECT_ROOT, 'node_modules', '@capacitor', 'cli', 'assets',
        'capacitor-android-plugins', 'src', 'main');
    } else {
      //-----------------
      // cordova Android
      //-----------------
      let manifestPath = path.resolve(
        PROJECT_ROOT, 'platforms', 'android', 'app',
        'src', 'main', 'AndroidManifest.xml');
      if (fs.existsSync(manifestPath)) {
        //-----------------
        // cordova-android@7.x or higher versions
        //-----------------
        PROJECT_PLATFORM = values.PLATFORM_CORDOVA;
        ANDROID_ROOT = path.resolve(
          PROJECT_ROOT, 'platforms', 'android', 'app', 'src', 'main');
      }
      if (!ANDROID_ROOT) {
        manifestPath = path.resolve(
          PROJECT_ROOT, 'platforms', 'android', 'AndroidManifest.xml');
        if (fs.existsSync(manifestPath)) {
          //-----------------
          // cordova-android@6.x or lower versions
          //-----------------
          PROJECT_PLATFORM = values.PLATFORM_CORDOVA_OLD;
          ANDROID_ROOT = path.resolve(
            PROJECT_ROOT, 'platforms', 'android');
        }
      }
    }

    if (PROJECT_PLATFORM === values.PLATFORM_UNKNOWN) {
      return reject('can not detect project platform');
    }

    res.white("platform: ");
    res.cyan(`${PROJECT_PLATFORM}`);
    res.ln();
    res.ln();

    req.shell.settings.ANDROID_ROOT = ANDROID_ROOT;
    req.shell.settings.PROJECT_PLATFORM = PROJECT_PLATFORM;

    // Ask install variables
    req.question({
      'API_KEY_FOR_ANDROID?': '',
      'PLAY_SERVICES_VERSION?': '15.0.1',
      'ANDROID_SUPPORT_V4_VERSION?': '27.1.1'
    },(values) => {
      resolve(values);
    });
  });

};
