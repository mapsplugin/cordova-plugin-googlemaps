package plugin.google.maps;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Method;

public class Environment extends CordovaPlugin {
  public PluginManager pluginManager;

  @Override
  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    pluginManager = webView.getPluginManager();
  }

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
    try {
      Method method = this.getClass().getDeclaredMethod(action, JSONArray.class, CallbackContext.class);
      if (!method.isAccessible()) {
        method.setAccessible(true);
      }
      method.invoke(this, args, callbackContext);
      return true;
    } catch (Exception e) {
      Log.e("CordovaLog", "An error occurred", e);
      callbackContext.error(e.toString());
      return false;
    }
  }

  @SuppressWarnings("unused")
  public void isAvailable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Log.d("Environment", "--->isAvailable");

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        // ------------------------------
        // Check of Google Play Services
        // ------------------------------
        int checkGooglePlayServices =
          GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(cordova.getActivity());
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
          String errorMsg = GoogleApiAvailability.getInstance().getErrorString(checkGooglePlayServices);
          callbackContext.error(errorMsg);
          return;
        }


        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            // ------------------------------
            // Check of Google Maps Android API v2
            // ------------------------------
            try {
              @SuppressWarnings({ "rawtypes" })
              Class GoogleMapsClass = Class.forName("com.google.android.gms.maps.GoogleMap");
            } catch (Exception e) {
              Log.e("GoogleMaps", "Error", e);
              callbackContext.error(e.getMessage());
              return;
            }

            callbackContext.success();
          }
        });
      }
    });

  }



  /**
   * Set the app background
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setBackGroundColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    final JSONArray rgba = args.getJSONArray(0);

    cordova.getActivity().runOnUiThread(new Runnable() {
      public void run() {
        GoogleMaps googleMaps = (GoogleMaps) pluginManager.getPlugin("GoogleMaps");

        int backgroundColor = Color.WHITE;
        if (rgba != null && rgba.length() == 4) {
          try {
            backgroundColor = PluginUtil.parsePluginColor(rgba);
            googleMaps.mPluginLayout.setBackgroundColor(backgroundColor);
          } catch (JSONException e) {}
        }
        sendNoResult(callbackContext);
      }
    });
  }

  @SuppressWarnings("unused")
  public Boolean getLicenseInfo(JSONArray args, final CallbackContext callbackContext) {
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        String msg = GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(cordova.getActivity());
        callbackContext.success(msg);
      }
    });
    return true;
  }


  /**
   * Set the debug flag of myPluginLayer
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setDebuggable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    /*
    Log.d("GoogleMaps", "pluginLayer_setDebuggable ");
    boolean debuggable = args.getBoolean(0);
    this.mPluginLayout.setDebug(debuggable);
    this.isDebug = debuggable;
    this.sendNoResult(callbackContext);
    */
  }

  protected void sendNoResult(CallbackContext callbackContext) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
    pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);
  }

}
