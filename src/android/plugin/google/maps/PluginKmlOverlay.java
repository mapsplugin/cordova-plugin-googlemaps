package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class PluginKmlOverlay extends MyPlugin {
  
  //@SuppressLint("UseSparseArrays")
  //@Override
  //public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
  //  super.initialize(cordova, webView);
  //}

  /**
   * Create kml overlay
   * 
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    final JSONObject opts = args.getJSONObject(0);


    AsyncTask<Void, Void, Bundle> task = new AsyncTask<Void, Void, Bundle>() {
      @Override
      protected Bundle doInBackground(Void... voids) {
        return PluginUtil.Json2Bundle(opts);
      }

      protected void onPostExecute(Bundle params)  {
        String urlStr = null;
        try {
          urlStr = opts.getString("url");
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
          return;
        }
        if (!urlStr.contains("://") &&
            !urlStr.startsWith("/")  &&
            !urlStr.startsWith("www/")) {
          urlStr = "./" + urlStr;
        }
        if (urlStr.startsWith("./")) {
          String currentPage = webView.getUrl();
          currentPage = currentPage.replaceAll("[^\\/]*$", "");
          urlStr = urlStr.replace("./", currentPage);
        }
        if (urlStr.startsWith("cdvfile://")) {
          urlStr = PluginUtil.getAbsolutePathFromCDVFilePath(webView.getResourceApi(), urlStr);
        }

        String kmlId = null;
        try {
          kmlId = opts.getString("kmlId");
        } catch (JSONException e) {
          e.printStackTrace();
          callbackContext.error(e.getMessage() + "");
        }
        AsyncKmlParser kmlParser = new AsyncKmlParser(cordova.getActivity(), mapCtrl, kmlId, callbackContext, params);
        kmlParser.execute(urlStr);
      }
    };
    task.execute();


  }

}
