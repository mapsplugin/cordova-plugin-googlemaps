/**
#
# Copyright 2010 Alexandre Gellibert
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
 */

var LatLng = require('./LatLng');
var LatLngBounds = require('./LatLngBounds');
var common = require('./Common');

var GEOCELL_GRID_SIZE = 4;
var GEOCELL_ALPHABET = "0123456789abcdef";

function computeBox(geocell) {
  var geoChar;
  var pos;

  var subcell_lng_span, subcell_lat_span;
  var xy, x, y, i;
  var bbox = _createBoundingBox(90.0, 180.0, -90.0, -180.0);
  while(geocell.length > 0) {
    geoChar = geocell.charAt(i);
    pos = GEOCELL_ALPHABET.indexOf(geoChar);

    subcell_lng_span = (bbox.getEast() - bbox.getWest()) / GEOCELL_GRID_SIZE;
    subcell_lat_span = (bbox.getNorth() - bbox.getSouth()) / GEOCELL_GRID_SIZE;

    xy = _subdiv_xy(geocell.charAt(0));
    x = xy[0];
    y = xy[1];

    bbox = _createBoundingBox(bbox.getSouth() + subcell_lat_span * (y + 1),
              bbox.getWest()  + subcell_lng_span * (x + 1),
              bbox.getSouth() + subcell_lat_span * y,
              bbox.getWest()  + subcell_lng_span * x);

    geocell = geocell.substring(1);
  }
  var sw = new LatLng(bbox.getSouth(), bbox.getWest());
  var ne = new LatLng(bbox.getNorth(), bbox.getEast());
  var bounds = new LatLngBounds(sw, ne);
  return bounds;
}

function _createBoundingBox(north, east, south, west) {
  var north_,south_;

  if(south > north) {
    south_ = north;
    north_ = south;
  } else {
    south_ = south;
    north_ = north;
  }
  return {
    northEast: {lat: north_, lng: east},
    southWest: {lat: south_, lng: west},
    getNorth: function() {
      return this.northEast.lat;
    },
    getSouth: function() {
      return this.southWest.lat;
    },
    getWest: function() {
      return this.southWest.lng;
    },
    getEast: function() {
      return this.northEast.lng;
    }
  };
}

/**
 * https://code.google.com/p/geomodel/source/browse/trunk/geo/geocell.py#370
 * @param latLng
 * @param resolution
 * @return
 */
function getGeocell(lat, lng, resolution) {
  var cell = "";
  var north = 90.0;
  var south = -90.0;
  var east = 180.0;
  var west = -180.0;

  var subcell_lng_span, subcell_lat_span;
  var x, y;
  while(cell.length < resolution + 1) {
    subcell_lng_span = (east - west) / GEOCELL_GRID_SIZE;
    subcell_lat_span = (north - south) / GEOCELL_GRID_SIZE;

    x = Math.min(Math.floor(GEOCELL_GRID_SIZE * (lng - west) / (east - west)), GEOCELL_GRID_SIZE - 1);
    y = Math.min(Math.floor(GEOCELL_GRID_SIZE * (lat - south) / (north - south)),GEOCELL_GRID_SIZE - 1);
    cell += _subdiv_char(x, y);


    south += subcell_lat_span * y;
    north = south + subcell_lat_span;

    west += subcell_lng_span * x;
    east = west + subcell_lng_span;
  }
  return cell;
}


/*
 * Returns the (x, y) of the geocell character in the 4x4 alphabet grid.
 */
function _subdiv_xy(char) {
  // NOTE: This only works for grid size 4.
  var charI = GEOCELL_ALPHABET.indexOf(char);
  return [(charI & 4) >> 1 | (charI & 1) >> 0,
            (charI & 8) >> 2 | (charI & 2) >> 1];
}

/**
 * Returns the geocell character in the 4x4 alphabet grid at pos. (x, y).
 * @return
 */
function _subdiv_char(posX, posY) {
  // NOTE: This only works for grid size 4.
  return GEOCELL_ALPHABET.charAt(
      (posY & 2) << 2 |
      (posX & 2) << 1 |
      (posY & 1) << 1 |
      (posX & 1) << 0);
}

module.exports = {
    computeBox: computeBox,
    getGeocell: getGeocell
};
