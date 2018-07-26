#!/usr/bin/env node
/*jshint esversion: 6 */

const shell = require('shell'),
  path = require('path'),
  fs = require('fs'),
  xml2js = require('xml2js'),
  values = require('./values');

const app = new shell();


app.configure(() => {
  app.use(shell.history({
    shell: app
  }));
  app.use(shell.completer({
    shell: app
  }));
  app.use(shell.router({
    shell: app
  }));
  app.use(shell.help({
    shell: app,
    introduction: true
  }));

  //-----------------------------------
  // Detect project platform
  // (support: cordova and capacitor)
  //-----------------------------------
  let PROJECT_ROOT = app.settings.workspace;
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
  }
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
  app.set('PROJECT_PLATFORM', PROJECT_PLATFORM);
  app.styles.white("platform: ");
  app.styles.cyan(`${PROJECT_PLATFORM}\n`);

  app.set('ANDROID_ROOT', ANDROID_ROOT);

});


app.cmd('variable API_KEY_FOR_ANDROID :api_key', 'set API key for Android platform', require('./variable/API_KEY_FOR_ANDROID'));

app.cmd('variable API_KEY_FOR_IOS :api_key', 'set API key for iOS platform', require('./variable/ios'));

app.on('error', (error) => {
  if (error.getSolution) {
    console.log("");
    app.styles.red("Error: \n  ");
    app.styles.white(error.message);
    app.styles.white("\n\n");
    app.styles.cyan("Solution:\n");
    app.styles.white(error.getSolution(app.settings.PROJECT_PLATFORM));
    app.styles.white("\n");
    console.log("");
  } else {
    app.styles.red(error.stack);
    app.styles.white("\n");
  }
});
