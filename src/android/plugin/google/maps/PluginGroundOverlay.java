package plugin.google.maps;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.BitmapDescriptor;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.GroundOverlay;
import com.google.android.libraries.maps.model.GroundOverlayOptions;
import com.google.android.libraries.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PluginGroundOverlay extends MyPlugin implements IOverlayPlugin {

  public final HashMap<Integer, AsyncTask> imageLoadingTasks = new HashMap<Integer, AsyncTask>();
  public final HashMap<String, Bitmap> overlayImage = new HashMap<String, Bitmap>();
  private boolean _clearDone = false;
  private PluginMap pluginMap;
  public final ConcurrentHashMap<String, MetaGroundOverlay> objects = new ConcurrentHashMap<String, MetaGroundOverlay>();


  public PluginMap getMapInstance(String mapId) {
    return (PluginMap) CordovaGoogleMaps.viewPlugins.get(mapId);
  }
  public PluginGroundOverlay getInstance(String mapId) {
    PluginMap mapInstance = getMapInstance(mapId);
    return (PluginGroundOverlay) mapInstance.plugins.get(String.format("%s-groundoverlay", mapId));
  }

  @Override
  public void setPluginMap(PluginMap pluginMap) {
    this.pluginMap = pluginMap;
  }

  /**
   * Create ground overlay
   */
  public void create(JSONArray args, CallbackContext callbackContext) throws JSONException {
    JSONObject opts = args.getJSONObject(2);
    String hashCode = args.getString(3);
    _createGroundOverlay(hashCode, opts, callbackContext);
  }

  public void _createGroundOverlay(final String idBase, final JSONObject opts, final CallbackContext callbackContext) throws JSONException {
    final GroundOverlayOptions options = new GroundOverlayOptions();
    final JSONObject properties = new JSONObject();
    options.anchor(0.5f, 0.5f);

    String groundOverlyId = "groundoverlay_" + idBase;
    final MetaGroundOverlay meta = new MetaGroundOverlay(groundOverlyId);
    meta.properties = properties;
    meta.initOpts = opts;

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
      meta.isVisible = opts.getBoolean("visible");
      options.visible(meta.isVisible);
    }
    if (opts.has("bounds")) {
      JSONArray points = opts.getJSONArray("bounds");
      LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
      options.positionFromBounds(bounds);
    }
    if (opts.has("clickable")) {
      meta.isClickable = opts.getBoolean("clickable");
      properties.put("isClickable", meta.isClickable);
    } else {
      meta.isClickable = true;
      properties.put("isClickable", true);
    }
    properties.put("isVisible", options.isVisible());

    // Since this plugin provide own click detection,
    // disable default clickable feature.
    options.clickable(false);

    // Load image
    final String imageUrl = opts.getString("url");


    _setImage(imageUrl, new PluginAsyncInterface() {

      @Override
      public void onPostExecute(Object object) {
        if (object == null) {
          callbackContext.error("Cannot create a ground overlay");
          return;
        }

        AsyncLoadImage.AsyncLoadImageResult result = (AsyncLoadImage.AsyncLoadImageResult)object;
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(result.image);
        options.image(bitmapDescriptor);

        GroundOverlay groundOverlay = pluginMap.getGoogleMap().addGroundOverlay(options);
        meta.groundOverlay = groundOverlay;
        overlayImage.put(groundOverlyId, result.image);

        groundOverlay.setTag(groundOverlyId);
        meta.bounds = groundOverlay.getBounds();
        objects.put(groundOverlyId, meta);


        JSONObject resultJSON = new JSONObject();
        try {
          resultJSON.put("hashCode", idBase);
          resultJSON.put("__pgmId", groundOverlyId);
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


  /**
   * Remove this tile layer
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void remove(JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);

    PluginGroundOverlay instance = getInstance(mapId);
    MetaGroundOverlay meta = instance.objects.remove(groundOverlayId);

    if (meta.groundOverlay != null) {
      Bitmap image = instance.overlayImage.remove(groundOverlayId);
      if (image != null && !image.isRecycled()) {
        image.recycle();
      }
      meta.groundOverlay.remove();
    }
    callbackContext.success();
  }


  /**
   * Set image of the ground-overlay
   */
  @PgmPluginMethod
  public void setImage(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);
    PluginGroundOverlay instance = getInstance(mapId);

    final String url = args.getString(2);

    MetaGroundOverlay meta = instance.objects.get(groundOverlayId);
    meta.initOpts.put("url", url);

    _setImage(url, new PluginAsyncInterface() {
      @Override
      public void onPostExecute(Object object) {
        if (object == null) {
          callbackContext.error("[error]groundoverlay.setImage(" + url + ")");
          return;
        }
        AsyncLoadImage.AsyncLoadImageResult result = (AsyncLoadImage.AsyncLoadImageResult) object;
        if (meta.groundOverlay != null) {
          Bitmap currentBmp = instance.overlayImage.remove(groundOverlayId);
          if (currentBmp != null) {
            currentBmp.recycle();
          }
        }
        if (result.image != null) {
          instance.overlayImage.put(groundOverlayId, result.image);
          meta.groundOverlay.setImage(BitmapDescriptorFactory.fromBitmap(result.image));
          callbackContext.success();
        } else {
          callbackContext.error("[error]groundoverlay.setImage(" + url + ")");
        }
      }

      @Override
      public void onError(String errorMsg) {
        callbackContext.error(errorMsg);
      }
    });
  }


  /**
   * Set bounds
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setBounds(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);
    PluginGroundOverlay instance = getInstance(mapId);
    MetaGroundOverlay meta = instance.objects.get(groundOverlayId);

    JSONArray points = args.getJSONArray(2);
    meta.initOpts.put("bounds", points);

    LatLngBounds bounds = PluginUtil.JSONArray2LatLngBounds(points);
    meta.groundOverlay.setPositionFromBounds(bounds);
    meta.bounds = bounds;

    callbackContext.success();
  }

  /**
   * Set opacity
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);
    PluginGroundOverlay instance = getInstance(mapId);
    float opacity = (float)args.getDouble(2);

    MetaGroundOverlay meta = instance.objects.get(groundOverlayId);
    meta.initOpts.put("opacity", opacity);
    meta.groundOverlay.setTransparency(1 - opacity);
    callbackContext.success();
  }
  /**
   * Set bearing
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setBearing(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);
    PluginGroundOverlay instance = getInstance(mapId);
    MetaGroundOverlay meta = instance.objects.get(groundOverlayId);

    float bearing = (float)args.getDouble(2);
    meta.initOpts.put("bearing", bearing);
    meta.groundOverlay.setBearing(bearing);
    callbackContext.success();
  }
  /**
   * set z-index
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);
    PluginGroundOverlay instance = getInstance(mapId);
    float zIndex = (float)args.getDouble(2);

    MetaGroundOverlay meta = instance.objects.get(groundOverlayId);
    meta.initOpts.put("zIndex", zIndex);
    meta.groundOverlay.setZIndex(zIndex);
    callbackContext.success();
  }


  /**
   * Set visibility for the object
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);
    PluginGroundOverlay instance = getInstance(mapId);
    boolean isVisible = args.getBoolean(2);

    MetaGroundOverlay meta = instance.objects.get(groundOverlayId);
    meta.groundOverlay.setVisible(isVisible);
    meta.isVisible = isVisible;
    meta.properties.put("isVisible", isVisible);
    meta.initOpts.put("visible", isVisible);

    callbackContext.success();
  }

  /**
   * Set clickable for the object
   */
  @PgmPluginMethod
  public void setClickable(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String groundOverlayId = args.getString(1);
    PluginGroundOverlay instance = getInstance(mapId);
    boolean clickable = args.getBoolean(2);
    MetaGroundOverlay meta = instance.objects.get(groundOverlayId);
    meta.isClickable = clickable;
    meta.properties.put("isClickable", clickable);
    callbackContext.success();
  }


  private void _setImage(final String imgUrl, final PluginAsyncInterface callback) {
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

    final AsyncLoadImageInterface onComplete = new AsyncLoadImageInterface() {

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
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AsyncLoadImage task = new AsyncLoadImage(activity, getCurrentUrl(), imageOptions, onComplete);
        task.execute();
        imageLoadingTasks.put(taskId, task);
      }
    });
  }


  @Override
  public void onDestroy() {
    super.onDestroy();

    //--------------------------------------
    // Cancel tasks
    //--------------------------------------

    int i, n = imageLoadingTasks.size();
    for (i = n - 1; i >= 0; i--) {
      imageLoadingTasks.remove(i).cancel(true);
    }

  }

}
