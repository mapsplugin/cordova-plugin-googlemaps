package plugin.google.maps;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

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
    
    if (opts.has("anchor")) {
      JSONArray anchor = opts.getJSONArray("anchor");
      options.anchor((float)anchor.getDouble(0), (float)anchor.getDouble(1));
    }
    if (opts.has("bearing")) {
      options.bearing((float)opts.getDouble("bearing"));
    }
    if (opts.has("opacity")) {
      options.transparency((float)opts.getDouble("opacity"));
    }
    if (opts.has("zIndex")) {
      options.zIndex((float)opts.getDouble("zIndex"));
    }
    if (opts.has("visible")) {
      options.visible(opts.getBoolean("visible"));
    }
    
    if (opts.has("bounds") == true) {
      JSONArray points = opts.getJSONArray("bounds");
      LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
      options.positionFromBounds(bounds);
    }

    // Load a dummy image
    options.image(this.dummyImg);
    
    GroundOverlay groundOverlay = this.map.addGroundOverlay(options);
    
    // Load image
    String url = opts.getString("url");
    _setImage(groundOverlay, url, callbackContext);
  }
  private void _setImage(final GroundOverlay groundOverlay, final String url, final CallbackContext callbackContext) {
    if (url != null && url.length() > 0) {
      if (url.indexOf("http") == 0) {
        AsyncLoadImage task = new AsyncLoadImage(new AsyncLoadImageInterface() {

          @Override
          public void onPostExecute(Bitmap image) {
            if (image == null) {
              callbackContext.error("Can not load image from " + url);
              return;
            }
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
            groundOverlay.setImage(bitmapDescriptor);
            _success(groundOverlay, callbackContext);
          }
        
        });
        task.execute(url);
      } else {
        groundOverlay.setImage(BitmapDescriptorFactory.fromAsset(url));
        _success(groundOverlay, callbackContext);
      }
    }
  }
  
  private void _success(GroundOverlay groundOverlay, CallbackContext callbackContext) {

    String id = "ground_" + groundOverlay.getId();
    this.objects.put(id, groundOverlay);

    JSONObject result = new JSONObject();
    try {
      result.put("hashCode", groundOverlay.hashCode());
      result.put("id", id);
    } catch (Exception e) {}
    callbackContext.success(result);
  }

  /**
   * Remove this tile layer
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  protected void remove(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    GroundOverlay groundOverlay = (GroundOverlay)this.objects.get(id);
    if (groundOverlay == null) {
      callbackContext.success();
      return;
    }
    groundOverlay.remove();
    callbackContext.success();
  }

  /**
   * Set visibility for the object
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  protected void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean visible = args.getBoolean(2);
    
    String id = args.getString(1);
    GroundOverlay groundOverlay = (GroundOverlay)this.objects.get(id);
    if (groundOverlay == null) {
      callbackContext.success();
      return;
    }
    groundOverlay.setVisible(visible);
    callbackContext.success();
  }
  

  /**
   * Set image of the ground-overlay
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setImage(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    GroundOverlay groundOverlay = (GroundOverlay)this.objects.get(id);
    String url = args.getString(2);
    
    // Load image
    _setImage(groundOverlay, url, callbackContext);
  }
  

  /**
   * Set bounds
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setBounds(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    GroundOverlay groundOverlay = (GroundOverlay)this.objects.get(id);
    
    JSONArray points = args.getJSONArray(2);
    LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
    groundOverlay.setPositionFromBounds(bounds);
    
    callbackContext.success();
  }

  /**
   * Set opacity
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float alpha = (float)args.getDouble(2);
    String id = args.getString(1);
    this.setFloat("setTransparency", id, alpha, callbackContext);
  }
}
