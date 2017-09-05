/*****************************************************************************
 * Location Class
 *****************************************************************************/
var Location = function(params) {
    var self = this;
    self.latLng = params.latLng || new LatLng(params.lat || 0, params.lng || 0);
    self.elapsedRealtimeNanos = params.elapsedRealtimeNanos;
    self.time = params.time;
    self.accuracy = params.accuracy || null;
    self.bearing = params.bearing || null;
    self.altitude = params.altitude || null;
    self.speed = params.speed || null;
    self.provider = params.provider;
    self.hashCode = params.hashCode;
};

module.exports = Location;
