
var utils = require('cordova/utils'),
  common = require('./Common'),
  Overlay = require('./Overlay'),
  BaseClass = require('./BaseClass'),
  event = require('./event'),
  BaseArrayClass = require('./BaseArrayClass');

/*****************************************************************************
 * FusionTableOverlay Class
 *****************************************************************************/
var FusionTableOverlay = function (map, FusionTableOverlayOptions, _exec) {
  Overlay.call(this, map, FusionTableOverlayOptions, 'FusionTableOverlay', _exec);

  var self = this;

  //-----------------------------------------------
  // Sets event listeners
  //-----------------------------------------------
  self.on('visible_changed', function () {
    var visible = self.get('visible');
    self.exec.call(self, null, self.errorHandler, self.getPluginName(), 'setVisible', [self.getId(), visible]);
  });

  delete self._privateInitialize;

  var onOverlayClick = function() {
    var overlay = this;
    self.trigger('fusion_click', overlay);
  };

  var _privateInitialize = function(kmlData) {
    if (!kmlData) {
      //-----------------------------------------------
      // (browser)Trigger internal command queue
      //-----------------------------------------------
      Object.defineProperty(self, '_isReady', {
        value: true,
        writable: false
      });
      self.exec('nop');
      return;
    }

    //--------------------------
    // Listen XXX_CLICK events
    //--------------------------
    var eventNames = {
      'Marker': event.MARKER_CLICK,
      'Polyline': event.POLYLINE_CLICK,
      'Polygon': event.POLYGON_CLICK,
      'GroundOverlay': event.GROUND_OVERLAY_CLICK
    };
    var seekOverlays = function (overlay) {
      if (overlay instanceof BaseArrayClass) {
        overlay.forEach(seekOverlays);
      } else if (Array.isArray(overlay)) {
        (new BaseArrayClass(overlay)).forEach(seekOverlays);
      } else if (overlay instanceof BaseClass && overlay.type in eventNames) {
        overlay.on(eventNames[overlay.type], onOverlayClick);
      }
    };

    kmlData.forEach(seekOverlays);



    //-----------------------------------------------
    // Trigger internal command queue
    //-----------------------------------------------
    Object.defineProperty(self, '_isReady', {
      value: true,
      writable: false
    });
    self.exec('nop');
  };
  Object.defineProperty(FusionTableOverlay.prototype, '_privateInitialize', {
    enumerable: false,
    value: _privateInitialize,
    writable: false
  });

};

utils.extend(FusionTableOverlay, Overlay);


FusionTableOverlay.prototype.getPluginName = function () {
  return this.map.getId() + '-fusiontableoverlay';
};

FusionTableOverlay.prototype.getHashCode = function () {
  return this.hashCode;
};

FusionTableOverlay.prototype.getMap = function () {
  return this.map;
};
FusionTableOverlay.prototype.getId = function () {
  return this.__pgmId;
};
FusionTableOverlay.prototype.setVisible = function (visible) {
  visible = common.parseBoolean(visible);
  this.set('visible', visible);
};
FusionTableOverlay.prototype.getVisible = function () {
  return this.get('visible');
};

FusionTableOverlay.prototype.setOptions = function (options) {
  var currentOptions = this.get('options');
  this.set('options', {
    clickable: 'clickable' in options ? common.defaultTrueOption(options.clickable) : currentOptions.clickable,
    suppressInfoWindows: 'suppressInfoWindows' in options ? options.suppressInfoWindows === true : currentOptions.suppressInfoWindows,
    query: {
      from: options.query && 'from' in options.query ? options.query.from : currentOptions.query.from,
      limit: options.query && 'from' in options.query ? options.query.limit : currentOptions.query.limit,
      offset: options.query && 'from' in options.query ? options.query.offset : currentOptions.query.offset,
      orderBy: options.query && 'from' in options.query ? options.query.orderBy : currentOptions.query.orderBy,
      select: options.query && 'from' in options.query ? options.query.select : currentOptions.query.select,
      where: options.query && 'from' in options.where ? options.query.where : currentOptions.query.where
    }
  });
};
FusionTableOverlay.prototype.remove = function (callback) {
  var self = this;
  if (self._isRemoved) {
    if (typeof callback === 'function') {
      return;
    } else {
      return Promise.resolve();
    }
  }
  Object.defineProperty(self, '_isRemoved', {
    value: true,
    writable: false
  });
  self.trigger(self.__pgmId + '_remove');

  var resolver = function(resolve, reject) {
    self.exec.call(self,
      function() {
        self.destroy();
        resolve.call(self);
      },
      reject.bind(self),
      self.getPluginName(), 'remove', [self.getId()], {
        remove: true
      });
  };

  if (typeof callback === 'function') {
    resolver(callback, self.errorHandler);
  } else {
    return new Promise(resolver);
  }

};

module.exports = FusionTableOverlay;
