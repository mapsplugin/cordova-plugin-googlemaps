#!/usr/bin/env node
/*jshint esversion: 6 */

const vorpal = require('vorpal')(),
  path = require('path'),
  fs = require('fs'),
  xml2js = require('xml2js'),
  values = require('./values');

vorpal
  .command('set apiKey android [apiKey]', 'set apiKey for Android')
  .action(require('./routes/android'));

vorpal
  .delimiter('pgm $')
  .show();
//
// app.cmd('init android', 'set API key for Android platform', require('./init/android'));
// app.cmd('init ios', 'set API key for iOS platform', require('./init/ios'));
// app.cmd('test', 'hello question', (req, res, next) => {
//
//   console.log('hello world');
//   next();
//   //
//   // req.question({
//   //   'name': '',
//   //   'value': 'here is value'
//   // },(value) => {
//   //   console.log(value);
//   //   res.prompt();
//   // });
//
// });
//
// app.on('error', (error) => {
//   if (error.getSolution) {
//     console.log("");
//     app.styles.red("Error: \n  ");
//     app.styles.white(error.message);
//     app.styles.white("\n\n");
//     app.styles.cyan("Solution:\n");
//     app.styles.white(error.getSolution(app.settings.PROJECT_PLATFORM));
//     app.styles.white("\n");
//     console.log("");
//   } else {
//     app.styles.red(error.stack);
//     app.styles.white("\n");
//   }
// });
