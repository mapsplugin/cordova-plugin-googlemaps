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
import org.apache.cordova.CordovaResourceApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PluginGroundOverlay extends MyPlugin implements MyPluginInterface  {

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
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        setImage_(options, imageUrl, new PluginAsyncInterface() {

          @Override
          public void onPostExecute(Object object) {
            if (object == null) {
              callbackContext.error("Cannot create a ground overlay");
              return;
            }
            GroundOverlay groundOverlay = (GroundOverlay)object;

            String id = "groundoverlay_" + groundOverlay.getId();
            self.objects.put(id, groundOverlay);

            String boundsId = "groundoverlay_bounds_" + groundOverlay.getId();
            self.objects.put(boundsId, groundOverlay.getBounds());

            String propertyId = "groundoverlay_property_" + groundOverlay.getId();
            self.objects.put(propertyId, properties);

            JSONObject result = new JSONObject();
            try {
              result.put("hashCode", groundOverlay.hashCode());
              result.put("id", id);
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
    });

  }
  

  /**
   * Remove this tile layer
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void remove(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    GroundOverlay groundOverlay = (GroundOverlay)self.objects.get(id);
    if (groundOverlay == null) {
      this.sendNoResult(callbackContext);
      return;
    }

    String propertyId = "groundoverlay_property_" + id;
    self.objects.remove(propertyId);
    groundOverlay.remove();
    this.sendNoResult(callbackContext);
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
    
    String propertyId = "groundoverlay_property_" + id;
    JSONObject opts = (JSONObject) self.objects.get(propertyId);
    opts.put("url", url);
    
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
    GroundOverlay groundOverlay = (GroundOverlay)self.objects.get(id);
    
    JSONArray points = args.getJSONArray(1);
    LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
    groundOverlay.setPositionFromBounds(bounds);

    String boundsId = "groundoverlay_bounds_" + groundOverlay.getId();
    self.objects.put(boundsId, groundOverlay.getBounds());

    this.sendNoResult(callbackContext);
  }

  /**
   * Set opacity
   * @param args
   * @param callbackContext
   * @throws JSONException 
   */
  public void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float alpha = (float)args.getDouble(1);
    String id = args.getString(0);
    this.setFloat("setTransparency", id, 1 - alpha, callbackContext);
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

  private void setImage_(final GroundOverlayOptions options, String iconUrl, final PluginAsyncInterface callback) {

    if (iconUrl == null) {
      callback.onPostExecute(null);
      return;
    }

    if (!iconUrl.contains("://") &&
        !iconUrl.startsWith("/") &&
        !iconUrl.startsWith("www/") &&
        !iconUrl.startsWith("data:image") &&
        !iconUrl.startsWith("./") &&
        !iconUrl.startsWith("../")) {
      iconUrl = "./" + iconUrl;
    }
    if (iconUrl.startsWith("./")  || iconUrl.startsWith("../")) {
      iconUrl = iconUrl.replace("././", "./");
      String currentPage = PluginGroundOverlay.this.webView.getUrl();
      currentPage = currentPage.replaceAll("[^\\/]*$", "");
      iconUrl = currentPage + "/" + iconUrl;
    }

    if (iconUrl == null) {
      callback.onPostExecute(null);
      return;
    }

    final String imageUrl = iconUrl;

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {

        if (imageUrl.indexOf("http") != 0) {
          //----------------------------------
          // Load icon from local file
          //----------------------------------
          AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Void... params) {
              String iconUrl = imageUrl;
              if (iconUrl == null) {
                return null;
              }

              Bitmap image = null;
              if (iconUrl.indexOf("cdvfile://") == 0) {
                CordovaResourceApi resourceApi = webView.getResourceApi();
                iconUrl = PluginUtil.getAbsolutePathFromCDVFilePath(resourceApi, iconUrl);
              }
              if (iconUrl == null) {
                return null;
              }

              if (iconUrl.indexOf("data:image/") == 0 && iconUrl.contains(";base64,")) {
                String[] tmp = iconUrl.split(",");
                image = PluginUtil.getBitmapFromBase64encodedImage(tmp[1]);
              } else if (iconUrl.indexOf("file://") == 0 &&
                  !iconUrl.contains("file:///android_asset/")) {
                iconUrl = iconUrl.replace("file://", "");
                File tmp = new File(iconUrl);
                if (tmp.exists()) {
                  image = BitmapFactory.decodeFile(iconUrl);
                } else {
                  //if (PluginMarker.this.mapCtrl.mPluginLayout.isDebug) {
                  Log.w(TAG, "icon is not found (" + iconUrl + ")");
                  //}
                }
              } else {
                //Log.d(TAG, "iconUrl = " + iconUrl);
                if (iconUrl.indexOf("file:///android_asset/") == 0) {
                  iconUrl = iconUrl.replace("file:///android_asset/", "");
                }
                //Log.d(TAG, "iconUrl = " + iconUrl);
                if (iconUrl.contains("./")) {
                  try {
                    boolean isAbsolutePath = iconUrl.startsWith("/");
                    File relativePath = new File(iconUrl);
                    iconUrl = relativePath.getCanonicalPath();
                    //Log.d(TAG, "iconUrl = " + iconUrl);
                    if (!isAbsolutePath) {
                      iconUrl = iconUrl.substring(1);
                    }
                    //Log.d(TAG, "iconUrl = " + iconUrl);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
                AssetManager assetManager = PluginGroundOverlay.this.cordova.getActivity().getAssets();
                InputStream inputStream;
                try {
                  inputStream = assetManager.open(iconUrl);
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
                Log.e(TAG,"PluginMarker: Warning - marker method called when marker has been disposed, wait for addMarker callback before calling more methods on the marker (setIcon etc).");
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
          // Load icon from on the internet
          //----------------------------------
          int width = -1;
          int height = -1;
          AsyncLoadImage task = new AsyncLoadImage(width, height, new AsyncLoadImageInterface() {

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
          task.execute(imageUrl);
        }
      }
    });
  }
}
