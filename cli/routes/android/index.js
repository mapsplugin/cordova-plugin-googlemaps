/*jshint esversion: 6 */
const fs = require('fs'),
  path = require('path'),
  xml2js = require('xml2js'),
  errors = require('../../errors'),
  values = require('../../values');

module.exports = (args, callback) => {
  console.log(args);
  callback();
  return;

  require('./prepare')(req, res, next)
  .then(() => {
    return require('./AndroidManifest')(req, res, next);
  })
  .then(() => {
    if (req.shell.settings.PROJECT_PLATFORM === values.PLATFORM_CORDOVA ||
      req.shell.settings.PROJECT_PLATFORM === values.PLATFORM_CORDOVA_OLD) {

      return require('./cordova')(req, res, next);
    } else {
      return new Promise((resolve, reject) => {
        // stub
        resolve();
      });
    }
  })
  .then(()=>{
    res.prompt();
  })
  .catch(err => {
console.log('error', err);
    next(err);
  });


  //require(`./${req.shell.settings.PROJECT_PLATFORM}`)(req, res, next);
};
