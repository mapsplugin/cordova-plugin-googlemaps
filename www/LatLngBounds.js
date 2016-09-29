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
        if ("lat" in args[i] && "lng" in args[i]) {
            this.extend(args[i]);
        }
    }
};

LatLngBounds.prototype.northeast = null;
LatLngBounds.prototype.southwest = null;

LatLngBounds.prototype.toString = function() {
    return "{\"southwest\": : " + this.southwest.toString() + ", \"northeast\": : " + this.northeast.toString() + "}";
};
LatLngBounds.prototype.toUrlValue = function(precision) {
    return "[" + this.southwest.toUrlValue(precision) + "," + this.northeast.toUrlValue(precision) + "]";
};

LatLngBounds.prototype.extend = function(latLng) {
    if ("lat" in latLng && "lng" in latLng) {
        if (!this.southwest && !this.northeast) {
            this.southwest = latLng;
            this.northeast = latLng;
        } else {
            var swLat = Math.min(latLng.lat, this.southwest.lat);
            var swLng = Math.min(latLng.lng, this.southwest.lng);
            var neLat = Math.max(latLng.lat, this.northeast.lat);
            var neLng = Math.max(latLng.lng, this.northeast.lng);

            delete this.southwest;
            delete this.northeast;
            this.southwest = new LatLng(swLat, swLng);
            this.northeast = new LatLng(neLat, neLng);
        }
        this[0] = this.southwest;
        this[1] = this.northeast;
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
    if (!("lat" in latLng) || !("lng" in latLng)) {
        return false;
    }
    var SWLat = this.southwest.lat,
        NELat = this.northeast.lat,
        SWLng = this.southwest.lng,
        NELng = this.northeast.lng;

    if (SWLng > NELng) {
        return (latLng.lat >= SWLat) && (latLng.lat <= NELat) &&
            (((SWLng < latLng.lng) && (latLng.lng < 180)) || ((-180 < latLng.lng) && (latLng.lng < NELng)));
    }
    return (latLng.lat >= SWLat) && (latLng.lat <= NELat) &&
        (latLng.lng >= SWLng) && (latLng.lng <= NELng);
};

module.exports = LatLngBounds;
