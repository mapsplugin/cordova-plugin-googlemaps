/*****************************************************************************
 * External service
 *****************************************************************************/
var ExternalService = {};

ExternalService.launchNavigation = function(params) {
    params = params || {};
    if (!params.from || !params.to) {
        return;
    }
    if (typeof params.from === "object" && "toUrlValue" in params.from) {
        params.from = params.from.toUrlValue();
    }
    if (typeof params.to === "object" && "toUrlValue" in params.to) {
        params.to = params.to.toUrlValue();
    }
    //params.from = params.from.replace(/\s+/g, "%20");
    //params.to = params.to.replace(/\s+/g, "%20");
    cordova.exec(null, null, "External", 'launchNavigation', [params]);
};

module.exports = ExternalService;
