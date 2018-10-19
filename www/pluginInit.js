var cordova_exec = require('cordova/exec');

function pluginInit() {
  //-------------------------------------------------------------
  // In some older browsers do not implement these methods.
  // For example, Android 4.4 does not have Array.map()
  //
  // But this plugin needs them.
  // That's why if the browser does not have it, implement it.
  //-------------------------------------------------------------
  if (typeof Array.prototype.forEach !== 'function') {
    (function () {
      Array.prototype.forEach = function (fn, thisArg) {
        thisArg = thisArg || this;
        for (var i = 0; i < this.length; i++) {
          fn.call(thisArg, this[i], i, this);
        }
      };
    })();
  }
  if (typeof Array.prototype.filter !== 'function') {
    (function () {
      Array.prototype.filter = function (fn, thisArg) {
        thisArg = thisArg || this;
        var results = [];
        for (var i = 0; i < this.length; i++) {
          if (fn.call(thisArg, this[i], i, this) === true) {
            results.push(this[i]);
          }
        }
        return results;
      };
    })();
  }
  if (typeof Array.prototype.map !== 'function') {
    (function () {
      Array.prototype.map = function (fn, thisArg) {
        thisArg = thisArg || this;
        var results = [];
        for (var i = 0; i < this.length; i++) {
          results.push(fn.call(thisArg, this[i], i, this));
        }
        return results;
      };
    })();
  }
  /*****************************************************************************
   * To prevent strange things happen,
   * disable the changing of viewport zoom level by double clicking.
   * This code has to run before the device ready event.
   *****************************************************************************/
  var viewportTag = null;
  var metaTags = document.getElementsByTagName('meta');
  for (var i = 0; i < metaTags.length; i++) {
    if (metaTags[i].getAttribute('name') === 'viewport') {
      viewportTag = metaTags[i];
      break;
    }
  }
  if (!viewportTag) {
    viewportTag = document.createElement('meta');
    viewportTag.setAttribute('name', 'viewport');
  }

  var viewportTagContent = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no';

  //
  // Detect support for CSS env() variable
  //
  var envTestDiv = '<div id="envTest" style="margin-top:-99px;margin-top:env(safe-area-inset-top);position:absolute;z-index:-1;"></div>';

  document.head.insertAdjacentHTML('afterbegin', envTestDiv);

  var testElement = document.getElementById('envTest');
  var computedStyles = window.getComputedStyle(testElement);
  var testResult = computedStyles.getPropertyValue('margin-top');

  document.head.removeChild(testElement);

  // if browser supports env(), returns a pixel value as string, even if 0px
  if (testResult != '-99px') {
    viewportTagContent += ', viewport-fit=cover';
  }

  // Update viewport tag attribute
  viewportTag.setAttribute('content', viewportTagContent);

  /*****************************************************************************
   * Prevent background, background-color, background-image properties
   *****************************************************************************/
  var cssAdjuster = document.createElement('style');
  cssAdjuster.setAttribute('type', 'text/css');
  cssAdjuster.innerText = [
    'html, body, ._gmaps_cdv_ {',
    '   background-image: url() !important;',
    '   background: rgba(0,0,0,0) url() !important;',
    '   background-color: rgba(0,0,0,0) !important;',
    '}',
    '._gmaps_cdv_ .nav-decor {',
    '   background-color: rgba(0,0,0,0) !important;',
    '   background: rgba(0,0,0,0) !important;',
    '   display:none !important;',
    '}',
    '.framework7-root .page-previous {',
    '   display:none !important;',
    '}'
  ].join('');
  document.head.appendChild(cssAdjuster);

  // I guess no longer necessary this code at 2018/March
  // //----------------------------------------------
  // // Set transparent mandatory for older browser
  // // http://stackoverflow.com/a/3485654/697856
  // //----------------------------------------------
  // if(document.body){
  //   document.body.style.backgroundColor = 'rgba(0,0,0,0)';
  //   //document.body.style.display='none';
  //   document.body.offsetHeight;
  //   //document.body.style.display='';
  // }

  //--------------------------------------------
  // Hook the backbutton of Android action
  //--------------------------------------------
  var anotherBackbuttonHandler = null;

  function onBackButton(e) {

    // Check DOM tree for new page
    cordova.fireDocumentEvent('plugin_touch', {
      force: true
    });

    if (anotherBackbuttonHandler) {
      // anotherBackbuttonHandler must handle the page moving transaction.
      // The plugin does not take care anymore if another callback is registered.
      anotherBackbuttonHandler(e);
    } else {
      cordova_exec(null, null, 'CordovaGoogleMaps', 'backHistory', []);
    }
  }

  document.addEventListener('backbutton', onBackButton);

  var _org_addEventListener = document.addEventListener;
  var _org_removeEventListener = document.removeEventListener;
  document.addEventListener = function (eventName, callback) {
    var args = Array.prototype.slice.call(arguments, 0);
    if (eventName.toLowerCase() !== 'backbutton') {
      _org_addEventListener.apply(this, args);
      return;
    }
    if (!anotherBackbuttonHandler) {
      anotherBackbuttonHandler = callback;
    }
  };
  document.removeEventListener = function (eventName, callback) {
    var args = Array.prototype.slice.call(arguments, 0);
    if (eventName.toLowerCase() !== 'backbutton') {
      _org_removeEventListener.apply(this, args);
      return;
    }
    if (anotherBackbuttonHandler === callback) {
      anotherBackbuttonHandler = null;
    }
  };

}

module.exports = pluginInit;
