

var LatLng = require('./LatLng');

/*****************************************************************************
 * Projection Class
 *****************************************************************************/
var Projection = function (WORLD_WIDTH) {

  Object.defineProperty(this, 'mWorldWidth', {
    value: WORLD_WIDTH,
    writable: false
  });
};

function toRadians(d) {
  return d * Math.PI / 180;
}

function toDegrees(r) {
  return r * 180 / Math.PI;
}

// https://alastaira.wordpress.com/2011/01/23/the-google-maps-bing-maps-spherical-mercator-projection/
Projection.prototype = {
  'fromLatLngToPoint': function(latLng, noWrap) {
    // var x = latLng.lng * 20037508.34 / 180;
    // var y = Math.log(Math.tan((90 + latLng.lat) * Math.PI / 360)) / (Math.PI / 180);
    // y = y * 20037508.34 / 180;
    // return {
    //   'x': x,
    //   'y': y
    // };
    if (noWrap) {
      latLng.lng = latLng.lng % 360;
    }
    var x = latLng.lng / 360 + 0.5;
    var siny = Math.sin(toRadians(latLng.lat));
    var y = 0.5 * Math.log((1 + siny) / (1 - siny)) / -(2 * Math.PI) + 0.5;

    return {
      'x': x * this.mWorldWidth,
      'y': y * this.mWorldWidth
    };
  },

  'fromPointToLatLng': function(point) {
    // var lng = (x / 20037508.34) * 180;
    // var lat = (y / 20037508.34) * 180;
    //
    // lat = 180/Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI / 2);
    // return new LatLng(lat, lng);

    var x = point.x / this.mWorldWidth - 0.5;
    var lng = x * 360;

    var y = 0.5 - (point.y / this.mWorldWidth);
    var lat = 90 - toDegrees(Math.atan(Math.exp(-y * 2 * Math.PI)) * 2);

    return new LatLng(lat, lng);
  }
};

module.exports = Projection;
