package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Bundle;

public class PluginKmlOverlay extends MyPlugin {
  
  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  /**
   * Create kml overlay
   * 
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void createKmlOverlay(JSONArray args, CallbackContext callbackContext) throws JSONException {
    JSONObject opts = args.getJSONObject(1);
    Bundle params = PluginUtil.Json2Bundle(opts);
    
    AsyncKmlParser kmlParser = new AsyncKmlParser(this.cordova.getActivity(), this.mapCtrl, callbackContext, params);
    kmlParser.execute(opts.getString("url"));
  }

}
