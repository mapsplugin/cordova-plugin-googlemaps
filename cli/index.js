#!/usr/bin/env node
/*jshint esversion: 6 */

const shell = require('shell'),
  path = require('path'),
  fs = require('fs'),
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
  var PROJECT_ROOT = app.settings.workspace;
  var PROJECT_PLATFORM = values.PLATFORM_CORDOVA;

  // Does this project use @capacitor?
  if (fs.existsSync(path.join(PROJECT_ROOT, 'capacitor.config.json'))) {
    PROJECT_PLATFORM = values.PLATFORM_CAPACITOR;
  }
  app.set('PROJECT_PLATFORM', PROJECT_PLATFORM);

  var ANDROID_PLUGIN_PROJ_ROOT;
  switch (PROJECT_PLATFORM) {
    //-----------------
    // @capacitor
    //-----------------
    case values.PLATFORM_CAPACITOR:
      ANDROID_PLUGIN_PROJ_ROOT = path.resolve(
        PROJECT_ROOT, 'node_modules', '@capacitor', 'cli', 'assets',
        'capacitor-android-plugins', 'src', 'main');
      break;

    //-----------------
    // cordova
    //-----------------
    case values.PLATFORM_CORDOVA:
      break;
  }

  app.set('ANDROID_ROOT', ANDROID_PLUGIN_PROJ_ROOT);

});


app.cmd('variable API_KEY_FOR_ANDROID :api_key', 'set API key for Android platform', require('./variable/android'));

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
