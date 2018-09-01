/*jshint esversion: 6 */
const fs = require('fs'),
  path = require('path'),
  xml2js = require('xml2js'),
  errors = require('../../errors'),
  values = require('../../values');

module.exports = (req, res, next) => {

  let API_KEY = req.params.api_key;


  require('./InfoPlist')(req, res, next)
  .then(() => {
      resolve();
    // if (req.shell.settings.PROJECT_PLATFORM === values.PLATFORM_CORDOVA ||
    //   req.shell.settings.PROJECT_PLATFORM === values.PLATFORM_CORDOVA_OLD) {
    //
    //   return require('./cordova')(req, res, next);
    // } else {
    //   return new Promise((resolve, reject) => {
    //     // stub
    //     resolve();
    //   });
    // }
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
