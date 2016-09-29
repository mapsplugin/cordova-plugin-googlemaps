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

    /**
     * Comparison function.
     * @method
     * @return {Boolean}
     */
    self.equals = function(other) {
        other = other || {};
        return other.lat === self.lat &&
            other.lng === self.lng;
    };

    /**
     * @method
     * @return {String} latitude,lontitude
     */
    self.toString = function() {
        return "{\"lat\": " + self.lat + ", \"lng\": " + self.lng + "}";
    };

    /**
     * @method
     * @param {Number}
     * @return {String} latitude,lontitude
     */
    self.toUrlValue = function(precision) {
        precision = precision || 6;
        return "{\"lat\": " + self.lat.toFixed(precision) + ", \"lng\": " + self.lng.toFixed(precision) + "}";
    };
}

module.exports = LatLng;
