const octokit = require('@octokit/rest')(),
  fs = require('mz/fs'),
  path = require('path'),
  xml2json = require('xml2json');

module.exports = function(args, callback) {
  const self = this,
    ROOT_DIR = process.cwd();

  fs.readFile(path.join(ROOT_DIR, "plugins", "cordova-plugin-googlemaps", "plugin.xml"), 'utf8').then(function(data) {
    var json = JSON.parse(xml2json.toJson(data));
    var version = json.plugin.version;
    console.log(version);
    callback();
  })
  .catch(callback);
return;

  octokit.repos.getTags({
    owner: 'mapsplugin',
    repo: 'cordova-plugin-googlemaps'
  }).then(function(data, headers, status) {

    self.log(data);
    // handle data
    callback();
  });
};
