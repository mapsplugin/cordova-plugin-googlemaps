package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;

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
    
    AsyncKmlParser kmlParser = new AsyncKmlParser(this.cordova.getActivity(), this.map, callbackContext);
    kmlParser.execute();
  }

  /**
   * Remove this tile layer
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  protected void remove(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    //KmlOverlay kmlOverlay = (KmlOverlay)this.objects.get(id);
    //kmlOverlay.remove();
  }
}
