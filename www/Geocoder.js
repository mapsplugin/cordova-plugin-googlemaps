/*****************************************************************************
 * Geocoder class
 *****************************************************************************/
 var argscheck = require('cordova/argscheck'),
     utils = require('cordova/utils'),
     exec = require('cordova/exec'),
     common = require('./Common'),
     BaseClass = require('./BaseClass');

var Geocoder = {};

Geocoder.geocode = function(geocoderRequest, callback) {
    var mvcResults = null;
    var requestCnt = 0;
    if (typeof callback !== "function") {
      return;
    }

    var requests = common.createMvcArray();
    requests.on('insert_at', function(idx) {
        var request = requests.getAt(idx);
        request.idx = idx;
        exec(function(result) {
            if (!mvcResults) {
                callback(result.results);
            } else {
                mvcResults.insertAt(result.idx, result.results);
                if (mvcResults.getLength() === requestCnt) {
                    var tmp = mvcResults.getArray();
                    var tmp2 = tmp.filter(function(ele) {
                      return ele === -1;
                    });
                    if (tmp2.length === 0) {
                        mvcResults.trigger("finish");
                    }
                }
            }
        }, function(error) {
            if (!mvcResults) {
                callback([], error);
            } else {
                mvcResults.trigger("error", error);
            }
        }, "Geocoder", 'geocode', [request]);
    });

    geocoderRequest = geocoderRequest || {};
    var requestProperty = null;
    if ("position" in geocoderRequest && !("address" in geocoderRequest)) {
      requestProperty = "position";
    } else if ("address" in geocoderRequest && !("position" in geocoderRequest)) {
      requestProperty = "address";
    }
    if (!requestProperty) {
      callback([], new Error("Invalid request"));
      return;
    }
    if (geocoderRequest[requestProperty] instanceof Array || Array.isArray(geocoderRequest[requestProperty])) {
        //-------------------------
        // Geocoder.geocode({
        //   address: [
        //    "Kyoto, Japan",
        //    "Tokyo, Japan"
        //   ]
        // })
        //-------------------------
        requestCnt = geocoderRequest[requestProperty].length;
        mvcResults = common.createMvcArray();
        for (i = 0; i < requestCnt; i++) {
          mvcResults.push(-1, true);
        }
        var request;
        var baseRequest = utils.clone(geocoderRequest);
        delete baseRequest[requestProperty];

        for (i = 0; i < requestCnt; i++) {
            request = utils.clone(baseRequest);
            request[requestProperty] = geocoderRequest[requestProperty][i];
            request.keepCallback = (i < requestCnt - 1);
            request.idx = i;
            if (requestProperty === "position") {
                request.position.lat = request.position.lat || 0.0;
                request.position.lng = request.position.lng || 0.0;
            }
            requests.push(request);
        }
        callback(mvcResults);
    } else {
        //-------------------------
        // Geocoder.geocode({
        //   address: "Kyoto, Japan"
        // })
        //-------------------------
        if (requestProperty === "position") {
            geocoderRequest.position.lat = geocoderRequest.position.lat || 0.0;
            geocoderRequest.position.lng = geocoderRequest.position.lng || 0.0;
        }
        geocoderRequest.keepCallback = false;
        geocoderRequest.idx = -1;
        requests.push(geocoderRequest);
    }
};

module.exports = Geocoder;
