var utils = require('cordova/utils'),
  LatLngBounds = require('./LatLngBounds');

/*****************************************************************************
 * VisibleRegion Class
 *****************************************************************************/
var VisibleRegion = function(southwest, northeast, farLeft, farRight, nearLeft, nearRight) {
  Object.defineProperty(this, 'type', {
    value: 'VisibleRegion',
    writable: false
  });
  this.southwest = southwest;
  this.northeast = northeast;
  this.farLeft = farLeft;
  this.farRight = farRight;
  this.nearLeft = nearLeft;
  this.nearRight = nearRight;
};

utils.extend(VisibleRegion, LatLngBounds);

delete VisibleRegion.prototype.extend;
delete VisibleRegion.prototype.getCenter;

VisibleRegion.prototype.contains = function(latLng) {
  if (!latLng || !('lat' in latLng) || !('lng' in latLng)) {
    return false;
  }
  var y = latLng.lat,
    x = latLng.lng;

  var y90 = y + 90;
  var south = this.southwest.lat,
    north = this.northeast.lat,
    west = this.southwest.lng,
    east = this.northeast.lng;
  var south90 = south + 90,
    north90 = north + 90;

  var containX = false,
    containY = false;

  if (east >= 0 && west >= east) {
    if (x <= 0 && x >= -180) {
      containX = true;
    } else {
      containX = (west <= x && x <= east);
    }
  } else if (west <= 0 && east <= west) {
    containX = (west <= x && x <= east);
    if (x >= 0 && x <= 180) {
      containX = true;
    } else {
      containX = (x <= 0 && x <= west || x <= east && x>= -180);
    }
  } else {
    return LatLngBounds.prototype.contains.call(this, latLng);
  }

  containY = (south90 <= y90 && y90 <= north90) ||  //#a
    (south >= 0 && north <= 0 && ((south <= y && y <= 90) || (y >= -90 && y<= north))); // #d

  return containX && containY;
};

module.exports = VisibleRegion;
