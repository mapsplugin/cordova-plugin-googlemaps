package plugin.google.maps;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;

public class PluginGroundOverlay extends MyPlugin {
  private BitmapDescriptor dummyImg;
  
  @SuppressLint("UseSparseArrays")
  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
    Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    this.dummyImg = BitmapDescriptorFactory.fromBitmap(bitmap);
  }

  /**
   * Create ground overlay
   * 
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void createGroundOverlay(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    JSONObject opts = args.getJSONObject(1);

    GroundOverlayOptions options = new GroundOverlayOptions();
    float width = -1;
    float height = -1;
    if (opts.has("width")) {
      width = (float)opts.getDouble("width");
    }
    if (opts.has("height")) {
      height = (float)opts.getDouble("height");
    }
    if (opts.has("anchor")) {
      JSONArray anchor = opts.getJSONArray("anchor");
      options.anchor((float)anchor.getDouble(0), (float)anchor.getDouble(1));
    }
    if (opts.has("bearing")) {
      options.bearing((float)opts.getDouble("bearing"));
    }
    if (opts.has("transparency")) {
      options.transparency((float)opts.getDouble("transparency"));
    }
    if (opts.has("zIndex")) {
      options.zIndex((float)opts.getDouble("zIndex"));
    }
    if (opts.has("visible")) {
      options.visible(opts.getBoolean("visible"));
    }
    JSONArray points = opts.getJSONArray("points");
    LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
    options.positionFromBounds(bounds);

    // Load a dummy image
    options.image(this.dummyImg);
    
    GroundOverlay groundOverlay = this.map.addGroundOverlay(options);

    // Load image
    String url = opts.getString("url");
    if (url != null && url.length() > 0) {
      if (url.indexOf("http") == 0) {
        AsyncLoadImage task = new AsyncLoadImage(groundOverlay, "setImage");
        task.execute(url);
      } else {
        groundOverlay.setImage(BitmapDescriptorFactory.fromAsset(url));
      }
    }
    
    String id = "ground_" + groundOverlay.getId();
    this.objects.put(id, groundOverlay);
    
    callbackContext.success(id);
  }

}
