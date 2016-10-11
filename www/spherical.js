var LatLng = require('./LatLng');

/**
 * Compute distance between two positions
 * http://www.htmlgoodies.com/beyond/javascript/calculate-the-distance-between-two-points-in-your-web-apps.html
 */
function computeDistanceBetween(latLng1, latLng2) {
    var radlat1 = Math.PI * latLng1.lat / 180
    var radlat2 = Math.PI * latLng2.lat / 180
    var radlon1 = Math.PI * latLng1.lng / 180
    var radlon2 = Math.PI * latLng2.lng / 180
    var theta = latLng1.lng - latLng2.lng;
    var radtheta = Math.PI * theta / 180;
    var dist = Math.sin(radlat1) * Math.sin(radlat2) + Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
    dist = Math.acos(dist);
    dist = dist * 180 / Math.PI;
    dist = dist * 60 * 1.1515;
    dist = dist * 1609.344;
    return dist; // meter
}

function computeMidpointBetween(latLng1, latLng2) {
    var lat1 = latLng1.lat * 0.017453292519943295;
    var lat2 = latLng2.lat * 0.017453292519943295;
    var lng1 = latLng1.lng * 0.017453292519943295;
    var lng2 = latLng2.lng * 0.017453292519943295;

    var dlng = lng2 - lng1;
    var Bx = Math.cos(lat2) * Math.cos(dlng);
    var By = Math.cos(lat2) * Math.sin(dlng);
    var lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2),
        Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
    var lng3 = lng1 + Math.atan2(By, (Math.cos(lat1) + Bx));

    var lat = (lat3 * 180) / Math.PI;
    var lng = (lng3 * 180) / Math.PI;
    return new LatLng(lat, lng);
}

function computeOffset(fromLatLng, distance, heading) {
  distance = distance * 0.000621371192;
  var earthsradius = 3963.189;
  var d2r = Math.PI / 180;
  var r2d = 180 / Math.PI;
  var rlat = (distance / earthsradius) * r2d;
  var rlng = rlat / Math.cos(fromLatLng.lat * d2r);
  var ey = fromLatLng.lng + (rlng * Math.cos(heading * d2r));
  var ex = fromLatLng.lat + (rlat * Math.sin(heading * d2r));
  return new LatLng(ex, ey);
}


function d2r(d) {
    return d * Math.PI / 180;
}

function r2d(r) {
    return r2d * 180 / Math.PI;
}

var wgs84 = {};
wgs84.CRADIUS = 6378137;
wgs84.CFLATTENING_DENOM = 298.257223563;
wgs84.CFLATTENING = 1 / wgs84.FLATTENING_DENOM;
wgs84.CPOLAR_RADIUS = wgs84.RADIUS * (1 - wgs84.FLATTENING);

/**
 * compute geometry area
 * https://github.com/mapbox/geojson-area
 *
 * License : BSD 2-Clause
 * https://github.com/mapbox/geojson-area/blob/master/LICENSE
 */
function computeArea(coords) {
   var area = 0;
   if (coords && coords.length > 0) {
       area += Math.abs(ringArea(coords[0]));
       for (var i = 1; i < coords.length; i++) {
           area -= Math.abs(ringArea(coords[i]));
       }
   }
   return area;
}
function ringArea(coords) {
    var p1, p2, p3, lowerIndex, middleIndex, upperIndex,
    area = 0,
    coordsLength = coords.length;

    if (coordsLength > 2) {
        for (i = 0; i < coordsLength; i++) {
            if (i === coordsLength - 2) {// i = N-2
                lowerIndex = coordsLength - 2;
                middleIndex = coordsLength -1;
                upperIndex = 0;
            } else if (i === coordsLength - 1) {// i = N-1
                lowerIndex = coordsLength - 1;
                middleIndex = 0;
                upperIndex = 1;
            } else { // i = 0 to N-3
                lowerIndex = i;
                middleIndex = i + 1;
                upperIndex = i + 2;
            }
            p1 = coords[lowerIndex];
            p2 = coords[middleIndex];
            p3 = coords[upperIndex];
            area += ( r2d(p3[0]) - r2d(p1[0]) ) * Math.sin( r2d(p2[1]));
        }

        area = area * wgs84.RADIUS * wgs84.RADIUS / 2;
    }

    return area;
}

module.exports = {
    computeDistanceBetween: computeDistanceBetween,
    computeMidpointBetween: computeMidpointBetween,
    computeOffset: computeOffset,
    computeArea: computeArea
};
