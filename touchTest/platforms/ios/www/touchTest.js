
module.exports = {
  click : function() {
    alert("click");
  }
};
cordova.addConstructor(function() {
  if (!window.Cordova) {
    window.Cordova = cordova;
  };
  window.plugin = window.plugin || {};
  window.plugin.touchTest = window.plugin.touchTest || module.exports;
});
