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
    
    String layerId = groundOverlay.getId();
    this.objects.put(layerId, groundOverlay);
    
    callbackContext.success(layerId);
  }

  /**
   * set visibility
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setVisible(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    boolean visible = args.getBoolean(2);
    GroundOverlay groundOverlay = this.getGroundOverlay(id);
    groundOverlay.setVisible(visible);
    callbackContext.success();
  }
}
