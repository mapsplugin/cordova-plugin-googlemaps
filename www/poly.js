var LatLngBounds = require('./LatLngBounds'),
  BaseArrayClass = require('./BaseArrayClass');

function containsLocation(latLng, path) {
  if ('lat' in latLng === false ||
    'lng' in latLng === false) {
    return false;
  }
  if (path instanceof BaseArrayClass) {
    path = path.getArray();
  }
  var points = JSON.parse(JSON.stringify(path));

  var first = points[0],
    last = points[points.length - 1];
  if (first.lat !== last.lat || first.lng !== last.lng) {
    points.push({
      lat: first.lat,
      lng: first.lng
    });
  }
  var point = {
    lat: latLng.lat,
    lng: latLng.lng
  };

  var wn = 0,
    bounds = new LatLngBounds(points),
    sw = bounds.southwest,
    ne = bounds.northeast,
    offsetLng360 = sw.lng <= 0 && ne.lng >= 0 && sw.lng < ne.lng ? 360 : 0;

  sw.lng += offsetLng360;
  point.lng += offsetLng360;

  points = points.map(function(vertex) {
    vertex.lng += +offsetLng360;
    return vertex;
  });

  var vt, a, b;

  for (var i = 0; i < points.length - 1; i++) {
    a = points[i];
    b = points[i + 1];

    if ((a.lat <= point.lat) && (b.lat > point.lat)) {
      vt = (point.lat - a.lat) / (b.lat - a.lat);
      if (point.lng < (a.lng + (vt * (b.lng - a.lng)))) {
        wn++;
      }
    } else if ((a.lat > point.lat) && (b.lat <= point.lat)) {
      vt = (point.lat - a.lat) / (b.lat - a.lat);
      if (point.lng < (a.lng + (vt * (b.lng - a.lng)))) {
        wn--;
      }
    }
  }

  return (wn !== 0);
}

function isLocationOnEdge(latLng, path) {
  if ('lat' in latLng === false ||
    'lng' in latLng === false) {
    return false;
  }

  var Sx, Sy;
  var p0, p1;
  var point = {'lat': latLng.lat, 'lng': latLng.lng};

  if (path instanceof BaseArrayClass) {
    path = path.getArray();
  }
  var points = JSON.parse(JSON.stringify(path));

  var bounds = new LatLngBounds(points),
    sw = bounds.southwest,
    ne = bounds.northeast,
    offsetLng360 = sw.lng <= 0 && ne.lng >= 0 && sw.lng < ne.lng ? 360 : 0;

  point.lng += offsetLng360;

  points = points.map(function(vertex) {
    vertex.lng += offsetLng360;
    return vertex;
  });

  p0 = points[0];
  for (var i = 1; i < points.length; i++) {
    p1 = points[i];
    Sx = (point.lng - p0.lng) / (p1.lng - p0.lng);
    Sy = (point.lat - p0.lat) / (p1.lat - p0.lat);
    if (Math.abs(Sx - Sy) < 0.05 && Sx < 1 && Sx > 0) {
      return true;
    }
    p0 = p1;
  }
  return false;
}

module.exports = {
  containsLocation: containsLocation,
  isLocationOnEdge: isLocationOnEdge
};
