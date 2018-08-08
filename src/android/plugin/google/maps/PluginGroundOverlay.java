package plugin.google.maps;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;

public class PluginGroundOverlay extends MyPlugin implements MyPluginInterface  {

  private HashMap<Integer, AsyncTask> imageLoadingTasks = new HashMap<Integer, AsyncTask>();
  private final Object semaphore = new Object();
  private HashMap<String, Bitmap> overlayImage = new HashMap<String, Bitmap>();
  private boolean _clearDone = false;

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
    String hashCode = args.getString(2);
    _createGroundOverlay(hashCode, opts, callbackContext);
  }

  public void _createGroundOverlay(final String idBase, final JSONObject opts, final CallbackContext callbackContext) throws JSONException {
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

    setImage_(imageUrl, new PluginAsyncInterface() {

      @Override
      public void onPostExecute(Object object) {
        if (object == null) {
          callbackContext.error("Cannot create a ground overlay");
          return;
        }

        AsyncLoadImage.AsyncLoadImageResult result = (AsyncLoadImage.AsyncLoadImageResult)object;
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(result.image);
        options.image(bitmapDescriptor);
        //options.zIndex(Calendar.getInstance().getTimeInMillis());
        GroundOverlay groundOverlay = self.map.addGroundOverlay(options);
        overlayImage.put("groundoverlay_" + idBase, result.image);

        groundOverlay.setTag(idBase);

        pluginMap.objects.put("groundoverlay_" + idBase, groundOverlay);

        pluginMap.objects.put("groundoverlay_bounds_" + idBase, groundOverlay.getBounds());

        pluginMap.objects.put("groundoverlay_property_" + idBase, properties);

        pluginMap.objects.put("groundoverlay_initOpts_" + idBase, opts);


        JSONObject resultJSON = new JSONObject();
        try {
          resultJSON.put("hashCode", idBase);
          resultJSON.put("id", "groundoverlay_" + idBase);
        } catch (Exception e) {
          e.printStackTrace();
        }
        callbackContext.success(resultJSON);
      }

      @Override
      public void onError(String errorMsg) {
        callbackContext.error(errorMsg);
      }

    });
  }


  @Override
  protected void clear() {
    _clearDone = false;
    synchronized (semaphore) {

      //--------------------------------------
      // clean up properties as much as possible
      //--------------------------------------
      cordova.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Set<String> keySet = pluginMap.objects.keys;
          String[] objectIdArray = keySet.toArray(new String[keySet.size()]);

          synchronized (pluginMap.objects) {
            Bitmap image;
            for (String objectId : objectIdArray) {
              if (pluginMap.objects.containsKey(objectId)) {
                if (objectId.startsWith("groundoverlay_") &&
                    !objectId.startsWith("groundoverlay_property_") &&
                    !objectId.startsWith("groundoverlay_initOpts_") &&
                    !objectId.startsWith("groundoverlay_bounds_")) {
                  GroundOverlay groundOverlay = (GroundOverlay) pluginMap.objects.remove(objectId);
                  image = overlayImage.remove(objectId);
                  if (image != null && !image.isRecycled()) {
                    image.recycle();
                  }
                  groundOverlay.setTag(null);
                  groundOverlay.remove();
                  groundOverlay = null;
                } else {
                  Object object = pluginMap.objects.remove(objectId);
                  object = null;
                }
              }
            }
          }

          synchronized (semaphore) {
            _clearDone = true;
            semaphore.notify();
          }

        }
      });

      try {
        if (!_clearDone) {
          semaphore.wait(1000);
        }
      } catch (InterruptedException e) {
        // ignore
        //e.printStackTrace();
      }
    }
  }

  /**
   * Remove this tile layer
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void remove(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    final GroundOverlay groundOverlay = (GroundOverlay)pluginMap.objects.get(id);
    if (groundOverlay == null) {
      callbackContext.success();
      return;
    }

    synchronized (pluginMap.objects) {
      pluginMap.objects.remove(id.replace("groundoverlay_", "groundoverlay_property_"));
      pluginMap.objects.remove(id.replace("groundoverlay_", "groundoverlay_initOpts_"));
      pluginMap.objects.remove(id.replace("groundoverlay_", "groundoverlay_bounds_"));
    }
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        synchronized (pluginMap.objects) {
          GroundOverlay groundOverlay = (GroundOverlay) pluginMap.objects.remove(id);
          if (groundOverlay != null) {
            Bitmap image = overlayImage.remove(id);
            if (image != null && !image.isRecycled()) {
              image.recycle();
            }
            groundOverlay.remove();
          }
        }
        callbackContext.success();
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
    final String id = args.getString(0);
    final String url = args.getString(1);
    final String idBase = id.replace("groundoverlay_", "");

    String propertyId = "groundoverlay_initOpts_" + idBase;
    final JSONObject opts = (JSONObject) pluginMap.objects.get(propertyId);
    opts.put("url", url);
    pluginMap.objects.put(propertyId, opts);

    setImage_(url, new PluginAsyncInterface() {
      @Override
      public void onPostExecute(Object object) {
        if (object == null) {
          callbackContext.error("[error]groundoverlay.setImage(" + url + ")");
          return;
        }
        AsyncLoadImage.AsyncLoadImageResult result = (AsyncLoadImage.AsyncLoadImageResult) object;
        GroundOverlay groundOverlay = getGroundOverlay(id);
        if (groundOverlay != null) {
          Bitmap currentBmp = overlayImage.remove(id);
          if (currentBmp != null) {
            currentBmp.recycle();
          }
        }
        if (result.image != null) {
          overlayImage.put(id, result.image);
          groundOverlay.setImage(BitmapDescriptorFactory.fromBitmap(result.image));
          callbackContext.success();
        } else {
          callbackContext.error("[error]groundoverlay.setImage(" + url + ")");
        }
      }

      @Override
      public void onError(String errorMsg) {

      }
    });
  }


  /**
   * Set bounds
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setBounds(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    final GroundOverlay groundOverlay = (GroundOverlay)pluginMap.objects.get(id);

    String propertyId = id.replace("groundoverlay_", "groundoverlay_initOpts_");
    JSONObject opts = (JSONObject) pluginMap.objects.get(propertyId);

    JSONArray points = args.getJSONArray(1);
    opts.put("bounds", points);
    pluginMap.objects.put(propertyId, opts);

    final LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        groundOverlay.setPositionFromBounds(bounds);
      }
    });

    String boundsId = id.replace("groundoverlay_", "groundoverlay_bounds_");
    pluginMap.objects.put(boundsId, bounds);

    callbackContext.success();
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
    JSONObject opts = (JSONObject) pluginMap.objects.get(propertyId);
    opts.put("opacity", opacity);
    pluginMap.objects.put(propertyId, opts);

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
    JSONObject opts = (JSONObject) pluginMap.objects.get(propertyId);
    opts.put("bearing", bearing);
    pluginMap.objects.put(propertyId, opts);

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
    JSONObject opts = (JSONObject) pluginMap.objects.get(propertyId);
    opts.put("zIndex", zIndex);
    pluginMap.objects.put(propertyId, opts);

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
    String propertyId = id.replace("groundoverlay_",  "groundoverlay_property_");
    JSONObject properties = (JSONObject)pluginMap.objects.get(propertyId);
    properties.put("isVisible", isVisible);
    pluginMap.objects.put(propertyId, properties);

    propertyId = id.replace("groundoverlay_", "groundoverlay_initOpts_");
    JSONObject opts = (JSONObject) pluginMap.objects.get(propertyId);
    opts.put("visible", isVisible);
    pluginMap.objects.put(propertyId, opts);

    callbackContext.success();
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
    JSONObject properties = (JSONObject)pluginMap.objects.get(propertyId);
    properties.put("isClickable", clickable);
    pluginMap.objects.put(propertyId, properties);
    callbackContext.success();
  }


  private void setImage_(final String imgUrl, final PluginAsyncInterface callback) {
    if (imgUrl == null) {
      callback.onPostExecute(null);
      return;
    }

    final AsyncLoadImage.AsyncLoadImageOptions imageOptions = new AsyncLoadImage.AsyncLoadImageOptions();
    imageOptions.height = -1;
    imageOptions.width = -1;
    imageOptions.noCaching = true;
    imageOptions.url = imgUrl;
    final int taskId = imageOptions.hashCode();

    AsyncLoadImageInterface onComplete = new AsyncLoadImageInterface() {

      @Override
      public void onPostExecute(AsyncLoadImage.AsyncLoadImageResult result) {
        if (result == null || result.image == null) {
          callback.onError("Can not read image from " + imgUrl);
          imageLoadingTasks.remove(taskId).cancel(true);
          return;
        }

        callback.onPostExecute(result);

        imageLoadingTasks.remove(taskId).cancel(true);
      }
    };
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AsyncLoadImage task = new AsyncLoadImage(cordova, webView, imageOptions, onComplete);
        //cordova.getActivity().runOnUiThread(new Runnable() {
        //  @Override
        //  public void run() {
        //    task.execute();
        //  }
        //});
        task.execute();
        imageLoadingTasks.put(taskId, task);
      }
    });


/*
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
      imageLoadingTasks.add(task);


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
      imageLoadingTasks.add(task);
    }
*/
  }


  @Override
  public void onDestroy() {
    super.onDestroy();

    //--------------------------------------
    // Cancel tasks
    //--------------------------------------
    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        AsyncTask task;
        int i, ilen=imageLoadingTasks.size();
        for (i = 0; i < ilen; i++) {
          task = imageLoadingTasks.remove(i);
          task.cancel(true);
          task = null;
        }
        imageLoadingTasks = null;
      }
    });

    this.clear();

  }

}
