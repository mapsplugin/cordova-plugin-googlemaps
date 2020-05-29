package plugin.google.maps;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PluginDirectionsService extends CordovaPlugin {
  private Activity activity;
  private final String TAG = "PluginDirectionsService";
  private String API_KEY = "";

  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    activity = cordova.getActivity();

    try {
      ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
      this.API_KEY = ai.metaData.getString("com.google.android.geo.API_KEY");
    } catch (Exception e) {
      // ignore
    }
  }


  @Override
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    HashMap<String, String> params = new HashMap<String, String>();

    JSONObject opts = args.getJSONObject(0);

    //-------------------
    // required parameters
    //-------------------
    params.put("origin", _decode_DirectionsRequestLocation(opts.getJSONObject("origin")));
    params.put("destination", _decode_DirectionsRequestLocation(opts.getJSONObject("destination")));
    params.put("key", this.API_KEY);

    //-------------------
    // mode parameter
    //-------------------
    if (opts.has("travelMode")) {
      // Default : driving
      String travelMode = opts.getString("travelMode").toLowerCase();
      if (!"driving".equals(travelMode)) {
        params.put("mode", travelMode);

        //-------------------
        // transitOptions parameter
        //-------------------
        if ("transit".equals(travelMode) && opts.has("transitOptions")) {
          JSONObject transitOptions = opts.getJSONObject("transitOptions");
          if (transitOptions.has("arrivalTime")) {
            params.put("arrival_time", String.valueOf(transitOptions.getInt("arrivalTime")));
          }
          if (transitOptions.has("departureTime")) {
            params.put("departure_time", String.valueOf(transitOptions.getInt("departureTime")));
          }
        }
      } else {

        //-------------------
        // DrivingOptions parameter
        //-------------------
        if (opts.has("drivingOptions")) {
          JSONObject drivingOptions = opts.getJSONObject("drivingOptions");
          if (drivingOptions.has("departureTime")) {
            params.put("departure_time", String.valueOf(drivingOptions.getInt("departureTime")));
          }
          if (drivingOptions.has("trafficModel")) {
            String trafficModel = drivingOptions.getString("trafficModel").toLowerCase();
            if (!"best_guess".equals(trafficModel)) {
              params.put("traffic_model", trafficModel);
            }
          }
        }
      }

      //-------------------
      // transitOptions parameter
      //-------------------
      if (opts.has("transitOptions")) {
        JSONObject transitOptions = opts.getJSONObject("transitOptions");
        //-------------------
        // transitOptions.modes parameter
        //-------------------
        if (transitOptions.has("modes")) {
          JSONArray modes = transitOptions.getJSONArray("modes");
          StringBuilder buffer = new StringBuilder();
          int n = modes.length();
          for (int i = 0; i < n; i++) {
            if (i > 0) {
              buffer.append("|");
            }
            buffer.append(modes.getString(i).toLowerCase());
          }
          params.put("transit_mode", buffer.toString());
        }
        //-------------------
        // transitOptions.routingPreference parameter
        //-------------------
        if (transitOptions.has("routingPreference")) {
          String routingPreference = transitOptions.getString("routingPreference").toLowerCase();
          params.put("transit_routing_preference", routingPreference);
        }
      }
    }



    //-------------------
    // waypoints parameter
    //-------------------
    if (opts.has("waypoints")) {
      JSONArray waypoints = opts.getJSONArray("waypoints");
      StringBuilder buffer = new StringBuilder();
      int n = waypoints.length();

      int cnt = 0;
      if (opts.has("optimizeWaypoints") && opts.getBoolean("optimizeWaypoints")) {
        cnt = 1;
        buffer.append("optimize:true");
      }
      for (int i = 0; i < n; i++) {
        JSONObject point = waypoints.getJSONObject(i);
        if (point.has("location")) {
          boolean stopOver = false;
          if (point.has("stopover")) {
            stopOver = point.getBoolean("stopover");
          }
          if (cnt > 0) {
            buffer.append("|");
          }
          if (!stopOver) {
            buffer.append("via:");
          }
          buffer.append(_decode_DirectionsRequestLocation(point.getJSONObject("location")));
          cnt++;
        }
      }
      if (cnt > 0) {
        params.put("waypoints", buffer.toString());
      }
    }
    //-------------------
    // alternatives parameter
    //-------------------
    if (opts.has("provideRouteAlternatives") && opts.getBoolean("provideRouteAlternatives")) {
      params.put("alternatives", "true");
    }

    //-------------------
    // avoid parameter
    //-------------------
    boolean avoidFerries = false;
    boolean avoidHighways = false;
    boolean avoidTolls = false;
    if (opts.has("avoidFerries")) {
      avoidFerries = opts.getBoolean("avoidFerries");
    }
    if (opts.has("avoidHighways")) {
      avoidHighways = opts.getBoolean("avoidHighways");
    }
    if (opts.has("avoidTolls")) {
      avoidTolls = opts.getBoolean("avoidTolls");
    }
    if (avoidFerries || avoidHighways || avoidTolls) {
      StringBuilder buffer = new StringBuilder();
      if (avoidFerries) {
        buffer.append("ferries|");
      }
      if (avoidTolls) {
        buffer.append("tolls|");
      }
      if (avoidHighways) {
        buffer.append("highways|");
      }
      buffer.delete(buffer.length() - 2, buffer.length());
      params.put("avoid", buffer.toString());
    }
    //-------------------
    // language parameter
    //-------------------
    String lCode = Locale.getDefault().getLanguage();
    params.put("language", lCode);

    //-------------------
    // units parameter
    //-------------------
    String localeFull = Locale.getDefault().getDisplayLanguage();
    if (opts.has("unitSystem")) {
      params.put("units", opts.getString("unitSystem").toLowerCase());
    } else if ("en_US".equals(localeFull)) {
      params.put("units", "imperial");
    } else {
      params.put("units", "metric");
    }

    //-------------------
    // region parameter
    //-------------------
    if (opts.has("region")) {
      params.put("region", opts.getString("region").toLowerCase());
    }


    AsyncGetJsonWithURL httpGet = new AsyncGetJsonWithURL("https://maps.googleapis.com/maps/api/directions/json", new AsyncHttpGetInterface() {
      @Override
      public void onPostExecute(JSONObject result) {
        PluginResult pluginResult = null;

        try {
          if (result == null) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, "UNKNOWN_ERROR");
          } else {
            if ("OK".equals(result.getString("status"))) {
              pluginResult = new PluginResult(PluginResult.Status.OK, result);
            } else {
              pluginResult = new PluginResult(PluginResult.Status.ERROR, result.getString("status"));
            }
          }
        } catch (Exception e){
          pluginResult = new PluginResult(PluginResult.Status.ERROR, "UNKNOWN_ERROR");
        } finally {
          if (pluginResult == null) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, "UNKNOWN_ERROR");
          }
          callbackContext.sendPluginResult(pluginResult);
        }
      }
    });

    httpGet.execute(params);
    return true;
  }

  private String _decode_DirectionsRequestLocation(JSONObject position) throws JSONException {
    String positionType = position.getString("type");

    if ("string".equals(positionType)) {
      return position.getString("value");
    }

    JSONObject value = position.getJSONObject("value");
    if ("location".equals(positionType)) {
      return String.format("%f,%f", value.getDouble("lat"), value.getDouble("lng"));
    }
    if (value.has("placeId")) {
      return String.format("place_id:%s", value.getString("placeId"));
    }
    if (value.has("location")) {
      JSONObject location = value.getJSONObject("location");
      return String.format("%f,%f", location.getDouble("lat"), location.getDouble("lng"));
    }
    if (value.has("query")) {
      return value.getString("query");
    }
    return "";
  }
}
