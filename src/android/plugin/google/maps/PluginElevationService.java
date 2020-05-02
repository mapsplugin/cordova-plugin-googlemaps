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

public class PluginElevationService extends CordovaPlugin {
  private Activity activity;
  private final String TAG = "PluginElevationService";
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

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        try {
          if ("getElevationAlongPath".equals(action)) {
            PluginElevationService.this.getElevationAlongPath(args, callbackContext);
          } else if ("getElevationForLocations".equals(action)) {
            PluginElevationService.this.getElevationForLocations(args, callbackContext);
          }

        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
    return true;

  }



  @SuppressWarnings("unused")
  public void getElevationAlongPath(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    HashMap<String, String> params = new HashMap<String, String>();

    JSONObject opts = args.getJSONObject(0);

    JSONArray points = opts.getJSONArray("path");
    List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
    params.put("path", "enc:" + PluginUtil.encodePath(path));
    params.put("samples", opts.getString("samples"));
    params.put("key", this.API_KEY);

    AsyncGetJsonWithURL httpGet = new AsyncGetJsonWithURL("https://maps.googleapis.com/maps/api/elevation/json", new AsyncHttpGetInterface() {
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
  }


  @SuppressWarnings("unused")
  public void getElevationForLocations(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    HashMap<String, String> params = new HashMap<String, String>();

    JSONObject opts = args.getJSONObject(0);

    JSONArray points = opts.getJSONArray("locations");
    List<LatLng> path = PluginUtil.JSONArray2LatLngList(points);
    params.put("locations", "enc:" + PluginUtil.encodePath(path));
    params.put("key", this.API_KEY);

    AsyncGetJsonWithURL httpGet = new AsyncGetJsonWithURL("https://maps.googleapis.com/maps/api/elevation/json", new AsyncHttpGetInterface() {
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
  }

}
