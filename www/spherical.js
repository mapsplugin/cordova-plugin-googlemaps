var LatLng = require('./LatLng');
var common = require('./Common');
var EARTH_RADIUS = 6371009;

/**
 * Port from android-maps-utils
 * https://github.com/googlemaps/android-maps-utils/blob/master/library/src/com/google/maps/android/SphericalUtil.java
 */

/**
 * Returns the non-negative remainder of x / m.
 * @param x The operand.
 * @param m The modulus.
 */
function mod(x, m) {
    return ((x % m) + m) % m;
}
/**
 * Wraps the given value into the inclusive-exclusive interval between min and max.
 * @param n   The value to wrap.
 * @param min The minimum.
 * @param max The maximum.
 */
function wrap(n, min, max) {
    return (n >= min && n < max) ? n : (mod(n - min, max - min) + min);
}

/**
 * Returns haversine(angle-in-radians).
 * hav(x) == (1 - cos(x)) / 2 == sin(x / 2)^2.
 */
function hav(x) {
    var sinHalf = Math.sin(x * 0.5);
    return sinHalf * sinHalf;
}

/**
 * Computes inverse haversine. Has good numerical stability around 0.
 * arcHav(x) == acos(1 - 2 * x) == 2 * asin(sqrt(x)).
 * The argument must be in [0, 1], and the result is positive.
 */
function arcHav(x) {
    return 2 * Math.asin(Math.sqrt(x));
}

/**
 * Returns hav() of distance from (lat1, lng1) to (lat2, lng2) on the unit sphere.
 */
function havDistance(lat1, lat2, dLng) {
    return hav(lat1 - lat2) + hav(dLng) * Math.cos(lat1) * Math.cos(lat2);
}

/**
 * Returns distance on the unit sphere; the arguments are in radians.
 */
function distanceRadians(lat1, lng1, lat2, lng2) {
    return arcHav(havDistance(lat1, lat2, lng1 - lng2));
}

/**
 * Returns the angle between two LatLngs, in radians. This is the same as the distance
 * on the unit sphere.
 */
function computeAngleBetween(from, to) {
    return distanceRadians(toRadians(from.lat), toRadians(from.lng),
                           toRadians(to.lat), toRadians(to.lng));
}

/**
 * Returns the distance between two LatLngs, in meters.
 */
function computeDistanceBetween(from, to) {
    return computeAngleBetween(from, to) * EARTH_RADIUS;
}

/**
 * Returns the distance between two LatLngs, in meters.
 */
function computeOffset(from, distance, heading) {
    distance /= EARTH_RADIUS;
    heading = toRadians(heading);

    // http://williams.best.vwh.net/avform.htm#LL
    var fromLat = toRadians(from.lat);
    var fromLng = toRadians(from.lng);
    var cosDistance = Math.cos(distance);
    var sinDistance = Math.sin(distance);
    var sinFromLat = Math.sin(fromLat);
    var cosFromLat = Math.cos(fromLat);
    var sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(heading);
    var dLng = Math.atan2(
                sinDistance * cosFromLat * Math.sin(heading),
                cosDistance - sinFromLat * sinLat);
    return new LatLng(toDegrees(Math.asin(sinLat)), toDegrees(fromLng + dLng));
}


function toRadians(d) {
    return d * Math.PI / 180;
}

function toDegrees(r) {
    return r * 180 / Math.PI;
}

/*
 * Returns the signed area of a closed path on a sphere of given radius.
 */
function computeSignedArea(path) {
    radius = EARTH_RADIUS;
    path = common.convertToPositionArray(path);

    var size = path.length;
    if (size < 3) {
        return 0;
    }
    var total = 0;

    var prev = path[size - 1];
    var prevTanLat = Math.tan((Math.PI / 2 - toRadians(prev.lat)) / 2);
    var prevLng = toRadians(prev.lng);

    // For each edge, accumulate the signed area of the triangle formed by the North Pole
    // and that edge ("polar triangle").
    path.forEach(function(position) {
        var tanLat = Math.tan((Math.PI / 2 - toRadians(position.lat)) / 2);
        var lng = toRadians(position.lng);
        total += polarTriangleArea(tanLat, lng, prevTanLat, prevLng);
        prevTanLat = tanLat;
        prevLng = lng;
    });
    return total * (radius * radius);
}

/*
 * Returns the signed area of a triangle which has North Pole as a vertex.
 * Formula derived from "Area of a spherical triangle given two edges and the included angle"
 * as per "Spherical Trigonometry" by Todhunter, page 71, section 103, point 2.
 * See http://books.google.com/books?id=3uBHAAAAIAAJ&pg=PA71
 * The arguments named "tan" are tan((pi/2 - latitude)/2).
 */
function polarTriangleArea(tan1, lng1, tan2, lng2) {

    var deltaLng = lng1 - lng2;
    var t = tan1 * tan2;
    return 2 * Math.atan2(t * Math.sin(deltaLng), 1 + t * Math.cos(deltaLng));
}
function computeArea(path) {
    return Math.abs(computeSignedArea(path));
}

/**
 * Returns the heading from one LatLng to another LatLng. Headings are
 * expressed in degrees clockwise from North within the range [-180,180).
 * @return The heading in degrees clockwise from north.
 */
function computeHeading(from, to) {
    // http://williams.best.vwh.net/avform.htm#Crs
    var fromLat = toRadians(from.lat);
    var fromLng = toRadians(from.lng);
    var toLat = toRadians(to.lat);
    var toLng = toRadians(to.lng);
    var dLng = toLng - fromLng;
    var heading = Math.atan2(
            Math.sin(dLng) * Math.cos(toLat),
            Math.cos(fromLat) * Math.sin(toLat) - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(dLng));
    return wrap(toDegrees(heading), -180, 180);
}

/**
 * Returns the location of origin when provided with a LatLng destination,
 * meters travelled and original heading. Headings are expressed in degrees
 * clockwise from North. This function returns null when no solution is
 * available.
 * @param to       The destination LatLng.
 * @param distance The distance travelled, in meters.
 * @param heading  The heading in degrees clockwise from north.
 */
function computeOffsetOrigin(to, distance, heading) {
    heading = toRadians(heading);
    distance /= EARTH_RADIUS;
    // http://lists.maptools.org/pipermail/proj/2008-October/003939.html
    var n1 = Math.cos(distance);
    var n2 = Math.sin(distance) * Math.cos(heading);
    var n3 = Math.sin(distance) * Math.sin(heading);
    var n4 = Math.sin(toRadians(to.lat));
    // There are two solutions for b. b = n2 * n4 +/- sqrt(), one solution results
    // in the latitude outside the [-90, 90] range. We first try one solution and
    // back off to the other if we are outside that range.
    var n12 = n1 * n1;
    var discriminant = n2 * n2 * n12 + n12 * n12 - n12 * n4 * n4;
    if (discriminant < 0) {
        // No real solution which would make sense in LatLng-space.
        return null;
    }
    var b = n2 * n4 + Math.sqrt(discriminant);
    b /= n1 * n1 + n2 * n2;
    var a = (n4 - n2 * b) / n1;
    var fromLatRadians = Math.atan2(a, b);
    if (fromLatRadians < - Math.PI / 2 || fromLatRadians > Math.PI / 2) {
        b = n2 * n4 - Math.sqrt(discriminant);
        b /= n1 * n1 + n2 * n2;
        fromLatRadians = Math.atan2(a, b);
    }
    if (fromLatRadians < - Math.PI / 2 || fromLatRadians > Math.PI / 2) {
        // No solution which would make sense in LatLng-space.
        return null;
    }
    var fromLngRadians = toRadians(to.lng) -
            Math.atan2(n3, n1 * Math.cos(fromLatRadians) - n2 * Math.sin(fromLatRadians));
    return new LatLng(toDegrees(fromLatRadians), toDegrees(fromLngRadians));
}


/**
 * Returns the LatLng which lies the given fraction of the way between the
 * origin LatLng and the destination LatLng.
 * @param from     The LatLng from which to start.
 * @param to       The LatLng toward which to travel.
 * @param fraction A fraction of the distance to travel.
 * @return The interpolated LatLng.
 */
function interpolate(from, to, fraction) {
    // http://en.wikipedia.org/wiki/Slerp
    var fromLat = toRadians(from.lat);
    var fromLng = toRadians(from.lng);
    var toLat = toRadians(to.lat);
    var toLng = toRadians(to.lng);
    var cosFromLat = Math.cos(fromLat);
    var cosToLat = Math.cos(toLat);

    // Computes Spherical interpolation coefficients.
    var angle = computeAngleBetween(from, to);
    var sinAngle = Math.sin(angle);
    if (sinAngle < 1E-6) {
        return from;
    }
    var a = Math.sin((1 - fraction) * angle) / sinAngle;
    var b = Math.sin(fraction * angle) / sinAngle;

    // Converts from polar to vector and interpolate.
    var x = a * cosFromLat * Math.cos(fromLng) + b * cosToLat * Math.cos(toLng);
    var y = a * cosFromLat * Math.sin(fromLng) + b * cosToLat * Math.sin(toLng);
    var z = a * Math.sin(fromLat) + b * Math.sin(toLat);

    // Converts interpolated vector back to polar.
    var lat = Math.atan2(z, Math.sqrt(x * x + y * y));
    var lng = Math.atan2(y, x);
    return new LatLng(toDegrees(lat), toDegrees(lng));
}

/**
 * Returns the length of the given path, in meters, on Earth.
 */
function computeLength(path) {
    path = common.convertToPositionArray(path);
    if (path.length < 2) {
        return 0;
    }
    var length = 0;
    var prev = path[0];
    var prevLat = toRadians(prev.lat);
    var prevLng = toRadians(prev.lng);
    path.forEach(function(point) {
        var lat = toRadians(point.lat);
        var lng = toRadians(point.lng);
        length += distanceRadians(prevLat, prevLng, lat, lng);
        prevLat = lat;
        prevLng = lng;
    });
    return length * EARTH_RADIUS;
}
module.exports = {
    computeDistanceBetween: computeDistanceBetween,
    computeOffset: computeOffset,
    computeOffsetOrigin: computeOffsetOrigin,
    computeArea: computeArea,
    computeSignedArea: computeSignedArea,
    computeHeading: computeHeading,
    interpolate: interpolate,
    computeLength: computeLength
};
