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
    
    String urlStr = opts.getString("url");
    if (urlStr.indexOf("://") == -1 && 
        urlStr.startsWith("/") == false && 
        urlStr.startsWith("www/") == false) {
      urlStr = "./" + urlStr;
    }
    if (urlStr.indexOf("./") == 0) {
      String currentPage = this.webView.getUrl();
      currentPage = currentPage.replaceAll("[^\\/]*$", "");
      urlStr = urlStr.replace("./", currentPage);
    }
    if (urlStr.indexOf("cdvfile://") == 0 ) {
      urlStr = PluginUtil.getAbsolutePathFromCDVFilePath(this.webView.getResourceApi(), urlStr);
    }
    
    AsyncKmlParser kmlParser = new AsyncKmlParser(this.cordova.getActivity(), this.mapCtrl, callbackContext, params);
    kmlParser.execute(urlStr);
  }

}
