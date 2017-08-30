
(function() {
  var utils = require('cordova/utils');
  var BaseClass = require('cordova-plugin-googlemaps.BaseClass');

  function PluginMap(args) {
    BaseClass.call(this);
    var self = this;
    this.on("googleready", function() {
      console.log(args.length);
      if (args.length === 3) {
        pluginId = args[2];
        var mapDiv = document.querySelector("[__plugindomid='" + pluginId + "']");
        self.set("map", new google.maps.Map(mapDiv, {
          mapTypeId: google.maps.MapTypeId.ROADMAP,
          center: {lat: 0, lng: 0},
          zoom: 0
        }));
      }
    });

  }

  utils.extend(PluginMap, BaseClass);


  module.exports = PluginMap;
})();
