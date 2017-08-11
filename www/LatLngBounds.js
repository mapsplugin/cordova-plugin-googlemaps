var LatLng = require('./LatLng');

/*****************************************************************************
 * LatLngBounds Class
 *****************************************************************************/
var LatLngBounds = function() {
    Object.defineProperty(this, "type", {
        value: "LatLngBounds",
        writable: false
    });

    var args = [];
    if (arguments.length === 1 &&
        typeof arguments[0] === "object" &&
        "push" in arguments[0]) {
        args = arguments[0];
    } else {
        args = Array.prototype.slice.call(arguments, 0);
    }

    for (var i = 0; i < args.length; i++) {
        if (args[i] && "lat" in args[i] && "lng" in args[i]) {
            this.extend(args[i]);
        }
    }
};

LatLngBounds.prototype.northeast = null;
LatLngBounds.prototype.southwest = null;

LatLngBounds.prototype.toString = function() {
    return '{"southwest":' + this.southwest.toString() + ', "northeast":' + this.northeast.toString() + '}';
};
LatLngBounds.prototype.toUrlValue = function(precision) {
    precision = precision || 6;
    return "[" + this.southwest.toUrlValue(precision) + "," + this.northeast.toUrlValue(precision) + "]";
};

LatLngBounds.prototype.extend = function(latLng) {
    if (latLng && "lat" in latLng && "lng" in latLng) {
        if (!this.southwest && !this.northeast) {
            this.southwest = latLng;
            this.northeast = latLng;
        } else {
            var south = Math.min(latLng.lat, this.southwest.lat);
            var west = Math.min(latLng.lng, this.southwest.lng);
            var north = Math.max(latLng.lat, this.northeast.lat);
            var east = Math.max(latLng.lng, this.northeast.lng);

            if (east === 180 & west < 0) {
              east = west;
              west = -180;
            }
            if (east > 0 && west < 0) {
              var tmp = west;
              west = east;
              east = tmp;
            }

            delete this.southwest;
            delete this.northeast;
            this.southwest = new LatLng(south, west);
            this.northeast = new LatLng(north, east);
        }
    }
};

LatLngBounds.prototype.getCenter = function() {
    var centerLat = (this.southwest.lat + this.northeast.lat) / 2;

    var swLng = this.southwest.lng;
    var neLng = this.northeast.lng;
    var sumLng = swLng + neLng;
    var centerLng = sumLng / 2;

    if ((swLng > 0 && neLng < 0 && sumLng < 180)) {
        centerLng += sumLng > 0 ? -180 : 180;
    }
    return new LatLng(centerLat, centerLng);
};
LatLngBounds.prototype.contains = function(latLng) {
    if (!latLng || !("lat" in latLng) || !("lng" in latLng)) {
        return false;
    }
    var y = latLng.lat,
      x = latLng.lng;
/*
    if (y > 90) {
      y = y - (y % 90) * 90;
    } else if (y < -90) {
      y = y + Math.abs(y % 90) * 90;
    }
    if (x > 90) {
      x = x - (x % 180) * 180;
    } else if (x < -180) {
      x = x + Math.abs(x % 180) * 180;
    }*/
    var x180 = x + 180,
      y90 = y + 90;
    var south = this.southwest.lat,
      north = this.northeast.lat,
      west = this.southwest.lng,
      east = this.northeast.lng;
      east = east === 180 & west < 0 ? -180 : east;
    var south90 = south + 90,
      north90 = north + 90,
      west180 = west + 180,
      east180 = east + 180;

    var containX = false,
      containY = false;

    containX = (west180 <= x180 && x180 <= east180) ||  // #1
              (west >= 0 && east <= 0 && ((west <= x && x <= 180) || (x >= -180 && x <= east)));  // 4

    containY = (south90 <= y90 && y90 <= north90) ||  //#a
              (south >= 0 && north <= 0 && ((south <= y && y <= 90) || (y >= -90 && y<= north))); // #d
/*
console.log("x = " + x + ", y = " + y);
console.log("s = " + south + ", w = " + west + ", n = " + north + ", e = " + east);
console.log("containX = " + containX + ", containY = " + containY);
*/
    return containX && containY;
};

module.exports = LatLngBounds;
