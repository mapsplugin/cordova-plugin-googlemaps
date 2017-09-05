/*******************************************************************************
 * @name LatLng
 * @class This class represents new camera position
 * @param {Number} latitude
 * @param {Number} longitude
 ******************************************************************************/
function LatLng(latitude, longitude) {
  var self = this;
  /**
   * @property {Number} latitude
   */
  self.lat = parseFloat(latitude || 0, 10);

  /**
   * @property {Number} longitude
   */
  self.lng = parseFloat(longitude || 0, 10);
}

LatLng.prototype = {
  /**
   * Comparison function.
   * @method
   * @return {Boolean}
   */
  equals: function(other) {
    other = other || {};
    return other.lat === this.lat &&
        other.lng === this.lng;
  },

  /**
   * @method
   * @return {String} latitude,lontitude
   */
  toString: function() {
    return '{"lat": ' + this.lat + ', "lng": ' + this.lng + '}';
  },

  /**
   * @method
   * @param {Number}
   * @return {String} latitude,lontitude
   */
  toUrlValue: function(precision) {
    precision = precision || 6;
    return '{"lat": ' + this.lat.toFixed(precision) + ', "lng": ' + this.lng.toFixed(precision) + '}';
  }
};

module.exports = LatLng;
