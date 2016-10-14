package plugin.google.maps;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PluginGroundOverlay extends MyPlugin implements MyPluginInterface  {

  private String userAgent = "Mozilla";
  @Override
  public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  /**
   * Create ground overlay
   * 
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void create(JSONArray args, CallbackContext callbackContext) throws JSONException {
    JSONObject opts = args.getJSONObject(1);
    _createGroundOverlay(opts, callbackContext);
  }
  
  public void _createGroundOverlay(final JSONObject opts, final CallbackContext callbackContext) throws JSONException {
    final GroundOverlayOptions options = new GroundOverlayOptions();
    final JSONObject properties = new JSONObject();

    if (opts.has("anchor")) {
      JSONArray anchor = opts.getJSONArray("anchor");
      options.anchor((float)anchor.getDouble(0), (float)anchor.getDouble(1));
    }
    if (opts.has("bearing")) {
      options.bearing((float)opts.getDouble("bearing"));
    }
    if (opts.has("opacity")) {
      options.transparency(1 - (float)opts.getDouble("opacity"));
    }
    if (opts.has("zIndex")) {
      options.zIndex((float)opts.getDouble("zIndex"));
    }
    if (opts.has("visible")) {
      options.visible(opts.getBoolean("visible"));
    }
    if (opts.has("bounds")) {
      JSONArray points = opts.getJSONArray("bounds");
      LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
      options.positionFromBounds(bounds);
    }
    if (opts.has("clickable")) {
      properties.put("isClickable", opts.getBoolean("clickable"));
    } else {
      properties.put("isClickable", true);
    }
    properties.put("isVisible", options.isVisible());

    // Since this plugin provide own click detection,
    // disable default clickable feature.
    options.clickable(false);

    // Load image
    final String imageUrl = opts.getString("url");

    setImage_(options, imageUrl, new PluginAsyncInterface() {

      @Override
      public void onPostExecute(Object object) {
        if (object == null) {
          callbackContext.error("Cannot create a ground overlay");
          return;
        }
        GroundOverlay groundOverlay = (GroundOverlay)object;
        String id = groundOverlay.getId();

        self.objects.put("groundoverlay_" + id, groundOverlay);

        self.objects.put("groundoverlay_bounds_" + id, groundOverlay.getBounds());

        self.objects.put("groundoverlay_property_" + id, properties);

        self.objects.put("groundoverlay_initOpts_" + id, opts);

        JSONObject result = new JSONObject();
        try {
          result.put("hashCode", groundOverlay.hashCode());
          result.put("id", "groundoverlay_" + id);
        } catch (Exception e) {
          e.printStackTrace();
        }
        callbackContext.success(result);
      }

      @Override
      public void onError(String errorMsg) {
        callbackContext.error(errorMsg);
      }

    });
  }
  

  /**
   * Remove this tile layer
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void remove(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    final GroundOverlay groundOverlay = (GroundOverlay)self.objects.get(id);
    if (groundOverlay == null) {
      this.sendNoResult(callbackContext);
      return;
    }

    self.objects.remove(id.replace("groundoverlay_", "groundoverlay_property_"));
    self.objects.remove(id.replace("groundoverlay_", "groundoverlay_initOpts_"));
    self.objects.remove(id.replace("groundoverlay_", "groundoverlay_bounds_"));
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        GroundOverlay groundOverlay = (GroundOverlay)self.objects.get(id);
        groundOverlay.remove();
        self.objects.remove(id);
        sendNoResult(callbackContext);
      }
    });
  }


  /**
   * Set image of the ground-overlay
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setImage(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    GroundOverlay groundOverlay = (GroundOverlay)self.objects.get(id);
    String url = args.getString(1);
    
    String propertyId = "groundoverlay_initOpts_" + groundOverlay.getId();
    JSONObject opts = (JSONObject) self.objects.get(propertyId);
    opts.put("url", url);
    self.objects.put(propertyId, opts);
    
    _createGroundOverlay(opts, callbackContext);
  }
  

  /**
   * Set bounds
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setBounds(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final GroundOverlay groundOverlay = (GroundOverlay)self.objects.get(id);

    String propertyId = "groundoverlay_initOpts_" + groundOverlay.getId();
    JSONObject opts = (JSONObject) self.objects.get(propertyId);

    JSONArray points = args.getJSONArray(1);
    opts.put("bounds", points);
    self.objects.put(propertyId, opts);

    final LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        groundOverlay.setPositionFromBounds(bounds);
      }
    });

    String boundsId = "groundoverlay_bounds_" + groundOverlay.getId();
    self.objects.put(boundsId, bounds);

    this.sendNoResult(callbackContext);
  }

  /**
   * Set opacity
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float opacity = (float)args.getDouble(1);
    String id = args.getString(0);

    String propertyId = id.replace("groundoverlay_", "groundoverlay_initOpts_");
    JSONObject opts = (JSONObject) self.objects.get(propertyId);
    opts.put("opacity", opacity);
    self.objects.put(propertyId, opts);

    this.setFloat("setTransparency", id, 1 - opacity, callbackContext);
  }
  /**
   * Set bearing
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setBearing(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float bearing = (float)args.getDouble(1);
    String id = args.getString(0);

    String propertyId = id.replace("groundoverlay_", "groundoverlay_initOpts_");
    JSONObject opts = (JSONObject) self.objects.get(propertyId);
    opts.put("bearing", bearing);
    self.objects.put(propertyId, opts);

    this.setFloat("setBearing", id, bearing, callbackContext);
  }
  /**
   * set z-index
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    float zIndex = (float) args.getDouble(1);

    String propertyId = id.replace("groundoverlay_", "groundoverlay_initOpts_");
    JSONObject opts = (JSONObject) self.objects.get(propertyId);
    opts.put("zIndex", zIndex);
    self.objects.put(propertyId, opts);

    this.setFloat("setZIndex", id, zIndex, callbackContext);
  }


  /**
   * Set visibility for the object
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final boolean isVisible = args.getBoolean(1);

    final GroundOverlay groundOverlay = this.getGroundOverlay(id);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        groundOverlay.setVisible(isVisible);
      }
    });
    String propertyId = "groundoverlay_property_" + groundOverlay.getId();
    JSONObject properties = (JSONObject)self.objects.get(propertyId);
    properties.put("isVisible", isVisible);
    self.objects.put(propertyId, properties);

    propertyId = id.replace("groundoverlay_", "groundoverlay_initOpts_");
    JSONObject opts = (JSONObject) self.objects.get(propertyId);
    opts.put("visible", isVisible);
    self.objects.put(propertyId, opts);


    this.sendNoResult(callbackContext);
  }

  /**
   * Set clickable for the object
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final boolean clickable = args.getBoolean(1);
    String propertyId = id.replace("groundoverlay_", "groundoverlay_property_");
    JSONObject properties = (JSONObject)self.objects.get(propertyId);
    properties.put("isClickable", clickable);
    self.objects.put(propertyId, properties);
    this.sendNoResult(callbackContext);
  }

  private void setImage_(final GroundOverlayOptions options, String imgUrl, final PluginAsyncInterface callback) {

    if (imgUrl == null) {
      callback.onPostExecute(null);
      return;
    }

    if (!imgUrl.contains("://") &&
        !imgUrl.startsWith("/") &&
        !imgUrl.startsWith("www/") &&
        !imgUrl.startsWith("data:image") &&
        !imgUrl.startsWith("./") &&
        !imgUrl.startsWith("../")) {
      imgUrl = "./" + imgUrl;
    }
    if (imgUrl.startsWith("./")  || imgUrl.startsWith("../")) {
      imgUrl = imgUrl.replace("././", "./");
      String currentPage = CURRENT_PAGE_URL;
      currentPage = currentPage.replaceAll("[^\\/]*$", "");
      imgUrl = currentPage + "/" + imgUrl;
    }

    if (imgUrl == null) {
      callback.onPostExecute(null);
      return;
    }

    final String imageUrl = imgUrl;
    if (imageUrl.indexOf("http") != 0) {
      //----------------------------------
      // Load img from local file
      //----------------------------------
      AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {

        @Override
        protected Bitmap doInBackground(Void... params) {
          String imgUrl = imageUrl;
          if (imgUrl == null) {
            return null;
          }

          Bitmap image = null;
          if (imgUrl.indexOf("cdvfile://") == 0) {
            CordovaResourceApi resourceApi = webView.getResourceApi();
            imgUrl = PluginUtil.getAbsolutePathFromCDVFilePath(resourceApi, imgUrl);
          }
          if (imgUrl == null) {
            return null;
          }

          if (imgUrl.indexOf("data:image/") == 0 && imgUrl.contains(";base64,")) {
            String[] tmp = imgUrl.split(",");
            image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
          } else if (imgUrl.indexOf("file://") == 0 &&
              !imgUrl.contains("file:///android_asset/")) {
            imgUrl = imgUrl.replace("file://", "");
            File tmp = new File(imgUrl);
            if (tmp.exists()) {
              image = BitmapFactory.decodeFile(imgUrl);
            } else {
              Log.w(TAG, "image is not found (" + imgUrl + ")");
            }
          } else {
            //Log.d(TAG, "imgUrl = " + imgUrl);
            if (imgUrl.indexOf("file:///android_asset/") == 0) {
              imgUrl = imgUrl.replace("file:///android_asset/", "");
            }
            //Log.d(TAG, "imgUrl = " + imgUrl);
            if (imgUrl.contains("./")) {
              try {
                boolean isAbsolutePath = imgUrl.startsWith("/");
                File relativePath = new File(imgUrl);
                imgUrl = relativePath.getCanonicalPath();
                //Log.d(TAG, "imgUrl = " + imgUrl);
                if (!isAbsolutePath) {
                  imgUrl = imgUrl.substring(1);
                }
                //Log.d(TAG, "imgUrl = " + imgUrl);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
            AssetManager assetManager = PluginGroundOverlay.this.cordova.getActivity().getAssets();
            InputStream inputStream;
            try {
              inputStream = assetManager.open(imgUrl);
              image = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
              e.printStackTrace();
              return null;
            }
          }
          if (image == null) {
            return null;
          }

          return image;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
          if (image == null) {
            callback.onPostExecute(null);
            return;
          }

          GroundOverlay groundOverlay = null;
          try {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
            options.image(bitmapDescriptor);
            groundOverlay = self.map.addGroundOverlay(options);

            callback.onPostExecute(groundOverlay);

          } catch (Exception e) {
            Log.e(TAG,"PluginGroundOverlay: Warning - ground overlay method is called when ground overlay method has been disposed, wait for addGroundOverlay callback before calling more methods on the groundOverlay.");
            //e.printStackTrace();
            try {
              if (groundOverlay != null) {
                groundOverlay.remove();
              }
            } catch (Exception ignore) {
              ignore = null;
            }
            callback.onError(e.getMessage() + "");
          }
        }
      };
      task.execute();


      return;
    }

    if (imageUrl.indexOf("http") == 0) {
      //----------------------------------
      // Load img from on the internet
      //----------------------------------
      int width = -1;
      int height = -1;
      AsyncLoadImage task = new AsyncLoadImage(userAgent, width, height, true, new AsyncLoadImageInterface() {

        @Override
        public void onPostExecute(Bitmap image) {

          if (image == null) {
            callback.onPostExecute(null);
            return;
          }
          GroundOverlay groundOverlay = null;
          try {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(image);
            options.image(bitmapDescriptor);
            groundOverlay = self.map.addGroundOverlay(options);

            image.recycle();
            callback.onPostExecute(groundOverlay);
          } catch (Exception e) {
            //e.printStackTrace();
            try {
              if (groundOverlay != null) {
                groundOverlay.remove();
              }
            } catch (Exception ignore) {
              ignore = null;
            }
            callback.onError(e.getMessage() + "");
          }

        }

      });
      task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageUrl);
    }
  }
}
