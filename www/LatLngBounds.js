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
        if (args[i] && args[i] && "lat" in args[i] && "lng" in args[i]) {
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
LatLngBounds.prototype.contains2 = function(latLng) {
    if (!("lat" in latLng) || !("lng" in latLng)) {
        return false;
    }
    var positionLat = latLng.lat + 90,
        positionLng = latLng.lng + 180;
    var SWLat = this.southwest.lat + 90,
        NELat = this.northeast.lat + 90,
        SWLng = this.southwest.lng + 180,
        NELng = this.northeast.lng + 180;

    var containY = (positionLat >= SWLat) && (positionLat <= NELat);

    var containX = false;
    if (SWLng <= NELng) {
      containX = (positionLng >= SWLng) && (positionLat <= NELng);
    } else {
      containX = (positionLng >= SWLng) && (positionLng <= 360) ||
                 (positionLng >= 0) && (positionLng <= NELng);
    }
    return containX && containY;
/*
      if (NELng > 0) {
        containY = (positionLat >= SWLat) && (positionLat <= NELat);
      }


    if (SWLng < 0) {
      if (NELng > 0) {
        //-------------
        // SWLng < 0 && NELng > 0
        //-------------
        if (SWLat < 0) {
          if (NELat < 0) {
          // sw: {"lat": -4.298503, "lng": -104.208062}
          // ne: {"lat": 74.012460, "lng": 176.724306}
          return ((positionLat >= SWLat) && (positionLat <= 0) ||
                  (positionLat >= 0) && (positionLat <= NELat)) &&
                 ((positionLng >= -180) && (positionLng <= SWLng) ||
                  (positionLng >= NELng) && (positionLng <= 180));
        } else {
          // sw: {"lat": 20.598322, "lng": -143.294849}
          // ne: {"lat": 68.092027, "lng": -70.906448}
          return (() &&
                 ((positionLng >= NELng) && (positionLng <= SWLng));
        }
      } else {
        //-------------
        // SWLng < 0 && NELng < 0
        //-------------
        if (SWLat < 0) {
          if (NELat < 0) {
            // sw: {"lat": -68.598322, "lng": -143.294849}
            // ne: {"lat": -20.092027, "lng": -70.906448}
            return ((positionLat >= SWLat) && (positionLat <= NELat)) &&
                   (positionLng >= NELng) && (positionLng <= SWLng);
          } else {
            // sw: {"lat": -68.598322, "lng": -143.294849}
            // ne: {"lat": 20.092027, "lng": -70.906448}
            return ((positionLat >= SWLat) && (positionLat <= 0) ||
                    (positionLat >= 0) && (positionLat <= NELat)) &&
                   (positionLng >= NELng) && (positionLng <= SWLng);
          }
        } else {

        }
      }
    } else {
      if (NELng > 0) {
        // sw: {"lat": -68.598322, "lng": 70.906448}
        // ne: {"lat": 20.092027, "lng": 143.294849}
        // position: {"lat": -12.360865, "lng": 130.891349}
        return ((positionLat >= SWLat) && (positionLat <= 0) ||
                (positionLat >= 0) && (positionLat <= NELat)) &&
               ((positionLng >= SWLng) && (positionLng <= NELng));
      } else {
        // sw: {"lat": -68.598322, "lng": -70.906448}
        // ne: {"lat": 20.092027, "lng": -143.294849}
        return ((positionLat >= SWLat) && (positionLat <= 0) ||
                (positionLat >= 0) && (positionLat <= NELat)) &&
               ((positionLng >= NELng) && (positionLng <= SWLng));
      }
    }
/*
    if (SWLng < 0 && NELng > 0) {
      // sw: {"lat": -58.135933, "lng": -150.782451}
      // ne: {"lat": -3.656541, "lng": 89.016723}]"
      // position: {"lat": -12.360865, "lng": 130.891349}
      return ((positionLng >= NELng) && (positionLng <= 180) ||
              (positionLng >= -180) && (positionLng <= SWLng)) &&
             (positionLat >= SWLat) && (positionLat <= NELat);
    }
      // sw: {"lat": -58.135933, "lng": -150.782451}
      // ne: {"lat": -3.656541, "lng": 89.016723}
      // position: {"lat": -12.360865, "lng": 130.891349}
    return (positionLat >= SWLat) && (positionLat <= NELat) &&
        (positionLng >= SWLng) && (positionLng <= NELng);
*/
};

module.exports = LatLngBounds;
