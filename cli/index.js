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

});


app.cmd('init android', 'set API key for Android platform', require('./init/android'));
app.cmd('init ios', 'set API key for iOS platform', require('./init/ios'));
app.cmd('test', 'hello question', (req, res, next) => {

  req.question({
    'name': '',
    'value': 'here is value'
  },(value) => {
    console.log(value);
    res.prompt();
  });

});

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
