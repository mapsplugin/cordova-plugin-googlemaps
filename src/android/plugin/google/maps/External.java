package plugin.google.maps;

import java.lang.reflect.Method;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class External extends CordovaPlugin {

  private enum TRAVEL_MODE {
    driving,
    transit,
    bicycling,
    walking
  }
  
  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
    try {
      Method method = this.getClass().getDeclaredMethod(action, JSONArray.class, CallbackContext.class);
      if (method.isAccessible() == false) {
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
  
  /**
   * Send an intent to the navigation applications
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void launchNavigation(JSONArray args, CallbackContext callbackContext) throws JSONException {
    JSONObject params = args.getJSONObject(0);
    String from = params.getString("from");
    String to = params.getString("to");
        
    Uri.Builder builder = new Uri.Builder();
    builder.scheme("http")
           .authority("maps.google.com")
           .appendPath("maps")
           .appendQueryParameter("saddr", from)
           .appendQueryParameter("daddr", to);
    
    if (params.has("travelMode")) {
      TRAVEL_MODE mode = null;
      try {
        mode = TRAVEL_MODE.valueOf(params.getString("travelMode"));
      } catch (Exception e){}
      //travel mode
      if (mode != null) {
        String dirFlag = "d";
        switch (mode) {
        case walking:
          dirFlag="w";
          break;

        case transit:
          dirFlag="r";
          break;

        case bicycling:
          dirFlag="b";
          break;
        default:
          break;
          
        }
        builder.appendQueryParameter("dirflg", dirFlag);
      }
    }

    Intent intent = new Intent(android.content.Intent.ACTION_VIEW, builder.build());
    this.cordova.getActivity().startActivity(intent);
    
    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
    callbackContext.sendPluginResult(result);
  }

}
