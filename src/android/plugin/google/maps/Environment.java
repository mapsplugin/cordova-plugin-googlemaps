package plugin.google.maps;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

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
  public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) {
    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        try {
          Method method = Environment.this.getClass().getDeclaredMethod(action, JSONArray.class, CallbackContext.class);
          if (!method.isAccessible()) {
            method.setAccessible(true);
          }
          method.invoke(Environment.this, args, callbackContext);
        } catch (Exception e) {
          Log.e("CordovaLog", "An error occurred", e);
          callbackContext.error(e.toString());
        }
      }
    });

    return true;
  }

  @SuppressWarnings("unused")
  public void isAvailable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    // ------------------------------
    // Check of Google Play Services
    // ------------------------------
    int checkGooglePlayServices =
      GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(cordova.getActivity());
    if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
      String errorMsg = GoogleApiAvailability.getInstance().getErrorString(checkGooglePlayServices);
      callbackContext.error(errorMsg);

      try {
        cordova.getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms")));
      } catch (android.content.ActivityNotFoundException anfe) {
        cordova.getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.gms")));
      }

      // End the app (in order to prevent lots of crashes)
      cordova.getActivity().finish();

      return;
    }

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



  /**
   * Set the app background
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback contect for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setBackGroundColor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONArray rgba = args.getJSONArray(0);
    int backgroundColor = Color.WHITE;

    if (rgba != null && rgba.length() == 4) {
      backgroundColor = PluginUtil.parsePluginColor(rgba);
    }
    final int finalBackgroundColor = backgroundColor;


    final CordovaGoogleMaps googleMaps = (CordovaGoogleMaps) pluginManager.getPlugin("CordovaGoogleMaps");

    Handler handler = new Handler(cordova.getActivity().getMainLooper());
    handler.postDelayed(new Runnable() {
      public void run() {
        googleMaps.mPluginLayout.setBackgroundColor(finalBackgroundColor);
        sendNoResult(callbackContext);
      }
    }, googleMaps.initialized ? 0 : 250);
  }

  @SuppressWarnings("unused")
  public Boolean getLicenseInfo(JSONArray args, final CallbackContext callbackContext) {
    callbackContext.success("Google Maps Android API v2 does not need this method anymore. But for iOS, you still need to display the lincense.");
    return true;
  }


  /**
   * Set the debug flag of myPluginLayer
   * @param args Parameters given from JavaScript side
   * @param callbackContext Callback context for sending back the result.
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void setDebuggable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        try {
          boolean debuggable = args.getBoolean(0);
          CordovaGoogleMaps googleMaps = (CordovaGoogleMaps) pluginManager.getPlugin("CordovaGoogleMaps");
          googleMaps.mPluginLayout.isDebug = debuggable;
        } catch (JSONException e) {
          e.printStackTrace();
        } finally {
          callbackContext.success();
        }
      }
    });
  }

  protected void sendNoResult(CallbackContext callbackContext) {
    PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
    pluginResult.setKeepCallback(true);
    callbackContext.sendPluginResult(pluginResult);
  }

}
