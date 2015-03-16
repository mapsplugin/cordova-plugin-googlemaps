package plugin.google.maps;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

public class PluginGroundOverlay extends MyPlugin {

  /**
   * Create ground overlay
   * 
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void createGroundOverlay(JSONArray args, CallbackContext callbackContext) throws JSONException {
    JSONObject opts = args.getJSONObject(1);
    _createGroundOverlay(opts, callbackContext);
  }
  
  private void _createGroundOverlay(final JSONObject opts, final CallbackContext callbackContext) throws JSONException {
    GroundOverlayOptions options = new GroundOverlayOptions();

    Log.d("Marker", "---->anchor");
    if (opts.has("anchor")) {
      JSONArray anchor = opts.getJSONArray("anchor");
      options.anchor((float)anchor.getDouble(0), (float)anchor.getDouble(1));
    }
    Log.d("Marker", "---->bearing");
    if (opts.has("bearing")) {
      options.bearing((float)opts.getDouble("bearing"));
    }
    Log.d("Marker", "---->opacity");
    if (opts.has("opacity")) {
      options.transparency(1 - (float)opts.getDouble("opacity"));
    }
    Log.d("Marker", "---->zIndex");
    if (opts.has("zIndex")) {
      options.zIndex((float)opts.getDouble("zIndex"));
    }
    Log.d("Marker", "---->visible");
    if (opts.has("visible")) {
      options.visible(opts.getBoolean("visible"));
    }

    Log.d("Marker", "---->bounds");
    if (opts.has("bounds") == true) {
      JSONArray points = opts.getJSONArray("bounds");
      LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
      options.positionFromBounds(bounds);
    }

    Log.d("Marker", "---->url");
    // Load image
    String url = opts.getString("url");
    _setImage(url, options, new PluginAsyncInterface() {

      @Override
      public void onPostExecute(Object object) {
        GroundOverlay groundOverlay = (GroundOverlay)object;

        String id = "groundOverlay_" + groundOverlay.getId();
        PluginGroundOverlay.this.objects.put(id, groundOverlay);

        JSONObject result = new JSONObject();
        try {
          result.put("hashCode", groundOverlay.hashCode());
          result.put("id", id);
          
          PluginGroundOverlay.this.objects.put("gOverlay_property_" + groundOverlay.getId(), opts);
        } catch (Exception e) {}
        callbackContext.success(result);
      }

      @Override
      public void onError(String errorMsg) {
        callbackContext.error(errorMsg);
      }
      
    });
  }
  @SuppressWarnings("resource")
  private void _setImage(final String url, final GroundOverlayOptions options, final PluginAsyncInterface callback) {
    Log.d("GroundOverlay", "---->_setImage");
    if (url == null || url.length() == 0) {
      callback.onError("The url property is empty");
      return;
    }

    String filePath = url;
    if (filePath.indexOf("://") == -1 && 
        filePath.startsWith("/") == false && 
        filePath.startsWith("www/") == false) {
      filePath = "./" + filePath;
    }
    if (filePath.indexOf("./") == 0) {
      String currentPage = this.webView.getUrl();
      currentPage = currentPage.replaceAll("[^\\/]*$", "");
      filePath = filePath.replace("./", currentPage);
    }
    
    
    //=================================
    // Load the image from the Internet
    //=================================
    if (filePath.indexOf("http") == 0) {
      Log.d("GroundOverlay", "---->http");
      
      AsyncLoadImage task = new AsyncLoadImage(new AsyncLoadImageInterface() {

        @Override
        public void onPostExecute(Bitmap image) {
          if (image == null) {
            callback.onError("Can not load image from " + url);
            return;
          }
          BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
          if (bitmapDescriptor != null) {
            options.image(bitmapDescriptor);
            GroundOverlay groundOverlay = PluginGroundOverlay.this.map.addGroundOverlay(options);
            callback.onPostExecute(groundOverlay);
          } else {
            callback.onError("Can not load image from " + url);
          }
        }
      
      });
      task.execute(filePath);
      return;
    }
    
    InputStream inputStream;
    if (filePath.indexOf("/") == 0 ||
        (filePath.indexOf("file://") == 0 && filePath.indexOf("file:///android_asset/") == -1) ||
        filePath.indexOf("cdvfile://") == 0) {
      if (filePath.indexOf("cdvfile://") == 0) {
        filePath = PluginUtil.getAbsolutePathFromCDVFilePath(webView.getResourceApi(), filePath);
      }
      if (filePath.indexOf("file://") == 0) {
        filePath = filePath.replace("file://", "");
      }
      
      try {
        inputStream = new FileInputStream(filePath);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        callback.onError("Can not load image from " + url);
        return;
      }
    } else {
      if (filePath.indexOf("file:///android_asset/") == 0) {
        filePath = filePath.replace("file:///android_asset/", "");
      }
      AssetManager assetManager = PluginGroundOverlay.this.cordova.getActivity().getAssets();
      try {
        inputStream = assetManager.open(filePath);
      } catch (IOException e) {
        e.printStackTrace();
        callback.onError("Can not load image from " + url);
        return;
      }
    }
    
    
    try {
      Bitmap image = null;
      image = BitmapFactory.decodeStream(inputStream);
      
      BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
      if (bitmapDescriptor != null) {
        options.image(bitmapDescriptor);
        GroundOverlay groundOverlay = PluginGroundOverlay.this.map.addGroundOverlay(options);
        callback.onPostExecute(groundOverlay);
      } else {
        callback.onError("Can not load image from " + url);
      }
      image.recycle();
      inputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
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
      this.sendNoResult(callbackContext);
      return;
    }

    String propertyId = "gOverlay_property_" + id;
    this.objects.remove(propertyId);
    groundOverlay.remove();
    this.sendNoResult(callbackContext);
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
      this.sendNoResult(callbackContext);
      return;
    }
    groundOverlay.setVisible(visible);
    this.sendNoResult(callbackContext);
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
    
    String propertyId = "gOverlay_property_" + id;
    JSONObject opts = (JSONObject) this.objects.get(propertyId);
    opts.put("url", url);
    
    _createGroundOverlay(opts, callbackContext);
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

    this.sendNoResult(callbackContext);
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
    this.setFloat("setTransparency", id, 1 - alpha, callbackContext);
  }
  /**
   * Set bearing
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  @SuppressWarnings("unused")
  private void setBearing(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float bearing = (float)args.getDouble(2);
    String id = args.getString(1);
    this.setFloat("setBearing", id, bearing, callbackContext);
  }
  /**
   * set z-index
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  private void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(1);
    float zIndex = (float) args.getDouble(2);
    this.setFloat("setZIndex", id, zIndex, callbackContext);
  }
}
