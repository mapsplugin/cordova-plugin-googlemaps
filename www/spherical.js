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

module.exports = {
    computeDistanceBetween: computeDistanceBetween,
    computeMidpointBetween: computeMidpointBetween,
    computeOffset: computeOffset
};
