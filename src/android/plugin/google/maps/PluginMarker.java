package plugin.google.maps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.libraries.maps.Projection;
import com.google.android.libraries.maps.model.BitmapDescriptor;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class PluginMarker extends MyPlugin implements IOverlayPlugin {

  private enum Animation {
    DROP,
    BOUNCE
  }

  public final HashMap<Integer, AsyncTask> iconLoadingTasks = new HashMap<Integer, AsyncTask>();
  public final HashMap<String, Bitmap> icons = new HashMap<String, Bitmap>();
  public final HashMap<String, Integer> iconCacheKeys = new HashMap<String, Integer>();
  private static final Paint paint = new Paint();
  private boolean _clearDone = false;
  protected final ConcurrentHashMap<String, MetaMarker> objects = new ConcurrentHashMap<String, MetaMarker>();
  protected PluginMap pluginMap;

  protected interface ICreateMarkerCallback {
    void onSuccess(MetaMarker meta);

    void onError(String message);
  }

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  @Override
  public void setPluginMap(PluginMap pluginMap) {
    this.pluginMap = pluginMap;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    for (String markerId : objects.keySet()) {
      objects.get(markerId).marker.remove();
    }
    this.objects.clear();

    for (String id: icons.keySet()) {
      icons.get(id).recycle();
    }
    icons.clear();

    for (AsyncTask task : iconLoadingTasks.values()) {
      task.cancel(true);
    }
    iconLoadingTasks.clear();

    iconCacheKeys.clear();
  }

  public PluginMap getMapInstance(String mapId) {
    return (PluginMap) CordovaGoogleMaps.viewPlugins.get(mapId);
  }
  public PluginMarker getInstance(String mapId, String markerId) {
    PluginMap mapInstance = getMapInstance(mapId);
    String pluginId;
    if (markerId.contains("markercluster")) {
      pluginId = String.format("%s-markercluster", mapId);
    } else {
      pluginId = String.format("%s-marker", mapId);
    }
    return (PluginMarker) mapInstance.plugins.get(pluginId);
  }

  /**
   * Create a marker
   */
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    // Create an instance of Marker class
    JSONObject opts = args.getJSONObject(2);
    final String markerId = "marker_" + args.getString(3);
    final JSONObject result = new JSONObject();
    result.put("__pgmId", markerId);


    _create(markerId, opts, new ICreateMarkerCallback() {
      @Override
      public void onSuccess(MetaMarker meta) {

        objects.put(markerId, meta);

        if (icons.containsKey(markerId)) {
          Bitmap icon = icons.get(markerId);
          try {
            result.put("width", icon.getWidth() / density);
            result.put("height", icon.getHeight() / density);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          try {
            result.put("width", 24);
            result.put("height", 42);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        callbackContext.success(result);
      }

      @Override
      public void onError(String message) {
        objects.remove(markerId);
        callbackContext.error(message);
      }
    });
  }

  public void _create(final String markerId, final JSONObject opts, final ICreateMarkerCallback callback) throws JSONException {
    final JSONObject properties = new JSONObject();
    final MarkerOptions markerOptions = new MarkerOptions();
    if (opts.has("position")) {
      JSONObject position = opts.getJSONObject("position");
      markerOptions.position(new LatLng(position.getDouble("lat"), position.getDouble("lng")));
    }
    if (opts.has("title")) {
      markerOptions.title(opts.getString("title"));
    }
    if (opts.has("snippet")) {
      markerOptions.snippet(opts.getString("snippet"));
    }
    if (opts.has("visible")) {
      if (opts.has("icon") && !"".equals(opts.getString("icon"))) {
        markerOptions.visible(false);
        properties.put("isVisible", false);
      } else {
        markerOptions.visible(opts.getBoolean("visible"));
        properties.put("isVisible", markerOptions.isVisible());
      }
    } else {
      markerOptions.visible(true);
    }
    if (opts.has("draggable")) {
      markerOptions.draggable(opts.getBoolean("draggable"));
    }
    if (opts.has("rotation")) {
      markerOptions.rotation((float) opts.getDouble("rotation"));
    }
    if (opts.has("flat")) {
      markerOptions.flat(opts.getBoolean("flat"));
    }
    if (opts.has("opacity")) {
      markerOptions.alpha((float) opts.getDouble("opacity"));
    }
    if (opts.has("zIndex")) {
      markerOptions.zIndex((float) opts.getDouble("zIndex"));
    }

    if (opts.has("styles")) {
      properties.put("styles", opts.getJSONObject("styles"));
    }
    if (opts.has("disableAutoPan")) {
      properties.put("disableAutoPan", opts.getBoolean("disableAutoPan"));
    } else {
      properties.put("disableAutoPan", false);
    }
    if (opts.has("noCache")) {
      properties.put("noCache", opts.getBoolean("noCache"));
    } else {
      properties.put("noCache", false);
    }
    if (opts.has("useHtmlInfoWnd")) {
      properties.put("useHtmlInfoWnd", opts.getBoolean("useHtmlInfoWnd"));
    } else {
      properties.put("useHtmlInfoWnd", false);
    }

    final MetaMarker meta = new MetaMarker(markerId);
    meta.properties = properties;

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Marker marker = pluginMap.getGoogleMap().addMarker(markerOptions);
        marker.setTag(markerId);
        marker.hideInfoWindow();
        meta.marker = marker;

        MyPlugin.executorService.submit(new Runnable() {
          @Override
          public void run() {

            try {
              // Prepare the result
              final JSONObject result = new JSONObject();
              result.put("__pgmId", markerId);

              // Load icon
              if (opts.has("icon")) {
                //------------------------------
                // Case: have the icon property
                //------------------------------
                Bundle bundle = null;
                Object value = opts.get("icon");
                if (JSONObject.class.isInstance(value)) {
                  JSONObject iconProperty = (JSONObject) value;
                  if (iconProperty.has("url") && JSONArray.class.isInstance(iconProperty.get("url"))) {

                    float[] hsv = new float[3];
                    JSONArray arrayRGBA = iconProperty.getJSONArray("url");
                    Color.RGBToHSV(arrayRGBA.getInt(0), arrayRGBA.getInt(1), arrayRGBA.getInt(2), hsv);
                    bundle = new Bundle();
                    bundle.putFloat("iconHue", hsv[0]);

                  } else {
                    if (iconProperty.has("label")) {
                      JSONObject label = iconProperty.getJSONObject("label");
                      if (label != null && label.get("color") instanceof JSONArray) {
                        label.put("color", PluginUtil.parsePluginColor(label.getJSONArray("color")));
                        iconProperty.put("label", label);
                      }
                    }
                    bundle = PluginUtil.Json2Bundle(iconProperty);

                    // The `anchor` of the `icon` property
                    if (iconProperty.has("anchor")) {
                      value = iconProperty.get("anchor");
                      if (JSONArray.class.isInstance(value)) {
                        JSONArray points = (JSONArray) value;
                        double[] anchorPoints = new double[points.length()];
                        for (int i = 0; i < points.length(); i++) {
                          anchorPoints[i] = points.getDouble(i);
                        }
                        bundle.putDoubleArray("anchor", anchorPoints);
                      } else if (value instanceof JSONObject && ((JSONObject) value).has("x") && ((JSONObject) value).has("y")) {
                        double[] anchorPoints = new double[2];
                        anchorPoints[0] = ((JSONObject) value).getDouble("x");
                        anchorPoints[1] = ((JSONObject) value).getDouble("y");
                        bundle.putDoubleArray("anchor", anchorPoints);
                      }
                    }

                    // The `infoWindowAnchor` property for infowindow
                    if (opts.has("infoWindowAnchor")) {
                      value = opts.get("infoWindowAnchor");
                      if (JSONArray.class.isInstance(value)) {
                        JSONArray points = (JSONArray) value;
                        double[] anchorPoints = new double[points.length()];
                        for (int i = 0; i < points.length(); i++) {
                          anchorPoints[i] = points.getDouble(i);
                        }
                        bundle.putDoubleArray("infoWindowAnchor", anchorPoints);
                      } else if (value instanceof JSONObject && ((JSONObject) value).has("x") && ((JSONObject) value).has("y")) {
                        double[] anchorPoints = new double[2];
                        anchorPoints[0] = ((JSONObject) value).getDouble("x");
                        anchorPoints[1] = ((JSONObject) value).getDouble("y");
                        bundle.putDoubleArray("infoWindowAnchor", anchorPoints);
                      }
                    }
                  }

                } else if (JSONArray.class.isInstance(value)) {
                  float[] hsv = new float[3];
                  JSONArray arrayRGBA = (JSONArray) value;
                  Color.RGBToHSV(arrayRGBA.getInt(0), arrayRGBA.getInt(1), arrayRGBA.getInt(2), hsv);
                  bundle = new Bundle();
                  bundle.putFloat("iconHue", hsv[0]);
                } else {
                  bundle = new Bundle();
                  bundle.putString("url", (String) value);
                }

                if (opts.has("animation")) {
                  bundle.putString("animation", opts.getString("animation"));
                }
                PluginMarker.this._setIcon(PluginMarker.this, meta, bundle, new PluginAsyncInterface() {

                  @Override
                  public void onPostExecute(final Object object) {
                    activity.runOnUiThread(new Runnable() {
                      @Override
                      public void run() {

                        Marker marker = (Marker) object;
                        if (opts.has("visible")) {
                          try {
                            marker.setVisible(opts.getBoolean("visible"));
                          } catch (JSONException e) {
                          }
                        } else {
                          marker.setVisible(true);
                        }


                        // Animation
                        String markerAnimation = null;
                        if (opts.has("animation")) {
                          try {
                            markerAnimation = opts.getString("animation");
                          } catch (JSONException e) {
                            e.printStackTrace();
                          }
                        }
                        if (markerAnimation != null) {
                          PluginMarker.this._setMarkerAnimation(PluginMarker.this ,marker, markerAnimation, new PluginAsyncInterface() {

                            @Override
                            public void onPostExecute(Object object) {
                              Marker marker = (Marker) object;
                              callback.onSuccess(meta);
                            }

                            @Override
                            public void onError(String errorMsg) {
                              callback.onError(errorMsg);
                            }
                          });
                        } else {
                          callback.onSuccess(meta);
                        }
                      }
                    });
                  }

                  @Override
                  public void onError(String errorMsg) {
                    callback.onError(errorMsg);
                  }

                });
              } else {
                //--------------------------
                // Case: no icon property
                //--------------------------
                String markerAnimation = null;
                if (opts.has("animation")) {
                  markerAnimation = opts.getString("animation");
                }
                if (markerAnimation != null) {
                  // Execute animation
                  PluginMarker.this._setMarkerAnimation(PluginMarker.this, marker, markerAnimation, new PluginAsyncInterface() {

                    @Override
                    public void onPostExecute(Object object) {
                      callback.onSuccess(meta);
                    }

                    @Override
                    public void onError(String errorMsg) {
                      callback.onError(errorMsg);
                    }

                  });
                } else {
                  // Return the result if does not specify the icon property.
                  callback.onSuccess(meta);
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
              callback.onError("" + e.getMessage());
            }
          }
        });
      }
    });

  }

  private void _setDropAnimation(final PluginMarker instance, final Marker marker, final PluginAsyncInterface callback) {
    final long startTime = SystemClock.uptimeMillis();
    final long duration = 100;

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Handler handler = new Handler();
        final Projection proj = instance.pluginMap.getGoogleMap().getProjection();
        final LatLng markerLatLng = marker.getPosition();
        final Point markerPoint = proj.toScreenLocation(markerLatLng);
        final Point startPoint = new Point(markerPoint.x, 0);

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
          @Override
          public void run() {
            LatLng startLatLng = proj.fromScreenLocation(startPoint);
            long elapsed = SystemClock.uptimeMillis() - startTime;
            float t = interpolator.getInterpolation((float) elapsed / duration);

            if(startLatLng != null) {
              double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
              double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
              marker.setPosition(new LatLng(lat, lng));
            }
            
            if (t < 1.0) {
              // Post again 16ms later.
              handler.postDelayed(this, 16);
            } else {
              marker.setPosition(markerLatLng);
              callback.onPostExecute(marker);
            }
          }
        });
      }
    });
  }

  /**
   * Bounce animation
   * http://android-er.blogspot.com/2013/01/implement-bouncing-marker-for-google.html
   */
  private void _setBounceAnimation(final PluginMarker instance, final Marker marker, final PluginAsyncInterface callback) {
    final long startTime = SystemClock.uptimeMillis();
    final long duration = 2000;
    final Interpolator interpolator = new BounceInterpolator();

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {

        final Handler handler = new Handler();
        final Projection projection = instance.pluginMap.getGoogleMap().getProjection();
        final LatLng markerLatLng = marker.getPosition();
        final Point startPoint = projection.toScreenLocation(markerLatLng);
        startPoint.offset(0, -200);


        handler.post(new Runnable() {
          @Override
          public void run() {
            LatLng startLatLng = projection.fromScreenLocation(startPoint);
            long elapsed = SystemClock.uptimeMillis() - startTime;
            float t = interpolator.getInterpolation((float) elapsed / duration);

            if(startLatLng != null) {
              double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
              double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
              marker.setPosition(new LatLng(lat, lng));
            }

            if (t < 1.0) {
              // Post again 16ms later.
              handler.postDelayed(this, 16);
            } else {
              marker.setPosition(markerLatLng);
              callback.onPostExecute(marker);
            }
          }
        });
      }
    });

  }

  private void _setMarkerAnimation(PluginMarker instance, Marker marker, String animationType, PluginAsyncInterface callback) {
    Animation animation = null;
    try {
      animation = Animation.valueOf(animationType.toUpperCase(Locale.US));
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (animation == null) {
      callback.onPostExecute(marker);
      return;
    }
    switch (animation) {
      case DROP:
        this._setDropAnimation(instance, marker, callback);
        break;

      case BOUNCE:
        this._setBounceAnimation(instance, marker, callback);
        break;

      default:
        break;
    }
  }

  /**
   * Set marker animation
   * http://android-er.blogspot.com/2013/01/implement-bouncing-marker-for-google.html
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setAnimation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    String animation = args.getString(2);

    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);

    this._setMarkerAnimation(instance, meta.marker, animation, new PluginAsyncInterface() {

      @Override
      public void onPostExecute(Object object) {
        callbackContext.success();
      }

      @Override
      public void onError(String errorMsg) {
        callbackContext.error(errorMsg);
      }

    });
  }

  /**
   * Show the InfoWindow bound with the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void showInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    instance.objects.get(markerId).marker.showInfoWindow();
    callbackContext.success();
  }

  /**
   * Set rotation for the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setRotation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    Marker marker = instance.objects.get(markerId).marker;
    marker.setRotation((float) args.getDouble(2));
    callbackContext.success();
  }

  /**
   * Set opacity for the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    Marker marker = instance.objects.get(markerId).marker;
    marker.setAlpha((float) args.getDouble(2));
    callbackContext.success();
  }

  /**
   * Set zIndex for the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    Marker marker = instance.objects.get(markerId).marker;
    marker.setZIndex((float) args.getDouble(2));
    callbackContext.success();
  }

  /**
   * set position
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    Marker marker = instance.objects.get(markerId).marker;

    JSONObject params = args.getJSONObject(2);
    LatLng position = new LatLng(params.getDouble("lat"), params.getDouble("lng"));
    marker.setPosition(position);
    callbackContext.success();
  }

  /**
   * Set flat for the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setFlat(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    Marker marker = instance.objects.get(markerId).marker;
    boolean isFlat = args.getBoolean(2);
    marker.setFlat(isFlat);
    callbackContext.success();
  }

  /**
   * Set visibility for the object
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);

    boolean isVisible = args.getBoolean(2);

    MetaMarker meta = instance.objects.get(markerId);
    meta.properties.put("isVisible", isVisible);
    Marker marker = meta.marker;
    marker.setVisible(isVisible);
    callbackContext.success();
  }

  /**
   * Set the flag of the disableAutoPan
   */
  @PgmPluginMethod
  public void setDisableAutoPan(JSONArray args, CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    boolean disableAutoPan = args.getBoolean(2);
    MetaMarker meta = instance.objects.get(markerId);
    meta.properties.put("disableAutoPan", disableAutoPan);
    callbackContext.success();
  }

  /**
   * Set title for the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setTitle(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);
    meta.marker.setTitle(args.getString(2));
    callbackContext.success();
  }

  /**
   * Set the snippet for the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setSnippet(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);
    meta.marker.setSnippet(args.getString(2));
    callbackContext.success();
  }

  /**
   * Hide the InfoWindow bound with the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void hideInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);
    meta.marker.hideInfoWindow();
    callbackContext.success();
  }

  /**
   * Remove the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    _removeMarker(mapId, markerId);
    callbackContext.success();
  }


  protected void _removeMarker(String mapId, String markerId) {
    PluginMarker instance = getInstance(mapId, markerId);
    if (!instance.objects.contains(markerId)) {
      return;
    }

    MetaMarker meta = instance.objects.remove(markerId);
    _removeMarker(instance, meta);
  }

  protected void _removeMarker(PluginMarker instance, MetaMarker meta) {
    //---------------------------------------------
    // Removes marker safely
    // (prevent the `un-managed object exception`)
    //---------------------------------------------
    String clusterId_markerId = (String)meta.marker.getTag();
    String iconCacheKey = "marker_icon_" + meta.getId();
    meta.marker.setTag(null);
    meta.marker.remove();
    meta.marker = null;
    objects.remove(clusterId_markerId);
    Log.d(TAG, String.format("---->remove / %s",clusterId_markerId));

    //---------------------------------------------------------------------------------
    // If no marker uses the icon image used be specified this marker, release it
    //---------------------------------------------------------------------------------
    if (instance.objects.containsKey(iconCacheKey)) {
      if (iconCacheKeys.containsKey(meta.iconCacheKey)) {
        int count = iconCacheKeys.get(meta.iconCacheKey);
        count--;
        if (count < 1) {
          AsyncLoadImage.removeBitmapFromMemCahce(meta.iconCacheKey);
          iconCacheKeys.remove(meta.iconCacheKey);
        } else {
          iconCacheKeys.put(meta.iconCacheKey, count);
        }
      }
    }
  }

  /**
   * Set anchor for the icon of the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setIconAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);

    JSONObject params = args.getJSONObject(2);
    float anchorX = (float) params.getDouble("x");
    float anchorY = (float) params.getDouble("y");

    if (meta.iconSize != null) {
      this._setIconAnchor(meta.marker, anchorX, anchorY,
              meta.iconSize.getWidth(), meta.iconSize.getHeight());
    }
    callbackContext.success();
  }


  /**
   * Set anchor for the InfoWindow of the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setInfoWindowAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);

    JSONObject params = args.getJSONObject(2);
    float anchorX = (float) params.getDouble("x");
    float anchorY = (float) params.getDouble("y");

    if (meta.iconSize != null) {
      this._setInfoWindowAnchor(meta.marker, anchorX, anchorY,
              meta.iconSize.getWidth(), meta.iconSize.getHeight());
    }
    callbackContext.success();
  }

  /**
   * Set draggable for the marker
   */
  @PgmPluginMethod(runOnUiThread = true)
  public void setDraggable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);

    boolean draggable = args.getBoolean(2);
    meta.marker.setDraggable(draggable);
    callbackContext.success();
  }

  /**
   * Set icon of the marker
   */
  @PgmPluginMethod
  public void setIcon(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String mapId = args.getString(0);
    String markerId = args.getString(1);
    PluginMarker instance = getInstance(mapId, markerId);
    MetaMarker meta = instance.objects.get(markerId);

    Object value = args.get(2);
    Bundle bundle = null;
    if (JSONObject.class.isInstance(value)) {
      JSONObject iconProperty = (JSONObject) value;
      bundle = PluginUtil.Json2Bundle(iconProperty);

      // The `anchor` for icon
      if (iconProperty.has("anchor")) {
        value = iconProperty.get("anchor");
        if (JSONArray.class.isInstance(value)) {
          JSONArray points = (JSONArray) value;
          double[] anchorPoints = new double[points.length()];
          for (int i = 0; i < points.length(); i++) {
            anchorPoints[i] = points.getDouble(i);
          }
          bundle.putDoubleArray("anchor", anchorPoints);
        } else if (value instanceof JSONObject && ((JSONObject) value).has("x") && ((JSONObject) value).has("y")) {
          double[] anchorPoints = new double[2];
          anchorPoints[0] = ((JSONObject) value).getDouble("x");
          anchorPoints[1] = ((JSONObject) value).getDouble("y");
          bundle.putDoubleArray("anchor", anchorPoints);
        }
      }
    } else if (JSONArray.class.isInstance(value)) {
      float[] hsv = new float[3];
      JSONArray arrayRGBA = (JSONArray) value;
      Color.RGBToHSV(arrayRGBA.getInt(0), arrayRGBA.getInt(1), arrayRGBA.getInt(2), hsv);
      bundle = new Bundle();
      bundle.putFloat("iconHue", hsv[0]);
    } else if (String.class.isInstance(value)) {
      bundle = new Bundle();
      bundle.putString("url", (String) value);
    }
    if (bundle != null) {
      this._setIcon(instance, meta, bundle, new PluginAsyncInterface() {

        @Override
        public void onPostExecute(Object object) {
          callbackContext.success();
        }

        @Override
        public void onError(String errorMsg) {
          callbackContext.error(errorMsg);
        }
      });
    } else {
      callbackContext.success();
    }
  }

  protected void _setIcon(final PluginMarker instance, final MetaMarker meta, final Bundle iconProperty, final PluginAsyncInterface callback) {
    boolean noCaching = false;
    if (iconProperty.containsKey("noCache")) {
      noCaching = iconProperty.getBoolean("noCache");
    }
    if (iconProperty.containsKey("iconHue")) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          float hue = iconProperty.getFloat("iconHue");
          meta.marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue));
        }
      });
      callback.onPostExecute(meta.marker);
      return;
    }

    String iconUrl = iconProperty.getString("url");
    if (iconUrl == null) {
      callback.onPostExecute(meta.marker);
      return;
    }

    int width = -1;
    int height = -1;
    if (iconProperty.containsKey("size")) {
      Bundle sizeInfo = (Bundle) iconProperty.get("size");
      width = sizeInfo.getInt("width", width);
      height = sizeInfo.getInt("height", height);
    }


    final AsyncLoadImage.AsyncLoadImageOptions options = new AsyncLoadImage.AsyncLoadImageOptions();
    options.url = iconUrl;
    options.width = width;
    options.height = height;
    options.noCaching = noCaching;
    final int taskId = options.hashCode();

    final AsyncLoadImageInterface onComplete = new AsyncLoadImageInterface() {
      @Override
      public void onPostExecute(AsyncLoadImage.AsyncLoadImageResult result) {
        instance.iconLoadingTasks.remove(taskId);

        if (result == null || result.image == null) {
          callback.onPostExecute(meta.marker);
          return;
        }
        if (meta.marker == null) {
          callback.onError("marker is removed");
          return;
        }
        if (result.image.isRecycled()) {
          //Maybe the task was canceled by map.clean()?
          callback.onError("Can not get image for marker. Maybe the task was canceled by map.clean()?");
          return;
        }

        synchronized (meta.marker) {
          String markerIconTag = "marker_icon_" + meta.getId();
          String markerImgSizeTag = "marker_imageSize_" + meta.getId();

          if (result.cacheKey != null && result.cacheKey.equals(meta.iconCacheKey)) {
            synchronized (instance.iconCacheKeys) {
              if (instance.iconCacheKeys.containsKey(meta.iconCacheKey)) {
                int count = instance.iconCacheKeys.get(meta.iconCacheKey);
                count--;
                if (count < 1) {
                  AsyncLoadImage.removeBitmapFromMemCahce(meta.iconCacheKey);
                  instance.iconCacheKeys.remove(meta.iconCacheKey);
                } else {
                  instance.iconCacheKeys.put(meta.iconCacheKey, count);
                }
              }
            }
          }

          if (instance.icons.containsKey(markerIconTag)) {
            Bitmap icon = instance.icons.remove(markerIconTag);
            if (icon != null && !icon.isRecycled()) {
              icon.recycle();
            }
            icon = null;
          }
          instance.icons.put(meta.getId(), result.image);

          //-------------------------------------------------------
          // Counts up the markers that use the same icon image.
          //-------------------------------------------------------
          if (result.cacheHit) {
            if (meta.marker == null || meta.marker.getTag() == null) {
              callback.onPostExecute(meta.marker);
              return;
            }
            meta.iconCacheKey = result.cacheKey;

            if (!iconCacheKeys.containsKey(meta.iconCacheKey)) {
              iconCacheKeys.put(meta.iconCacheKey, 1);
            } else {
              int count = iconCacheKeys.get(meta.iconCacheKey);
              iconCacheKeys.put(meta.iconCacheKey, count + 1);
            }
            //Log.d(TAG, "----> " + result.cacheKey + " = " + iconCacheKeys.get(result.cacheKey));
          }

          //------------------------
          // Draw label on icon
          //------------------------
          if (iconProperty.containsKey("label")) {
            result.image = drawLabel(result.image, iconProperty.getBundle("label"));
          }
          BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(result.image);
          if (bitmapDescriptor == null || meta.marker == null || meta.marker.getTag() == null) {
            callback.onPostExecute(meta.marker);
            return;
          }

          //------------------------
          // Sets image as icon
          //------------------------
          meta.marker.setIcon(bitmapDescriptor);
          bitmapDescriptor = null;

          //---------------------------------------------
          // Save the information for the anchor property
          //---------------------------------------------
          meta.iconSize = new Size(result.image.getWidth(), result.image.getHeight());

//          result.image.recycle();   // cause crash on maps-sdk-3.1.0-beta
//          result.image = null;

          // The `anchor` of the `icon` property
          if (iconProperty.containsKey("anchor")) {
            double[] anchor = iconProperty.getDoubleArray("anchor");
            if (anchor != null && anchor.length == 2) {
              _setIconAnchor(meta.marker, anchor[0], anchor[1], meta.iconSize.getWidth(), meta.iconSize.getHeight());
            }
          }


          // The `anchor` property for the infoWindow
          if (iconProperty.containsKey("infoWindowAnchor")) {
            double[] anchor = iconProperty.getDoubleArray("infoWindowAnchor");
            if (anchor != null && anchor.length == 2) {
              _setInfoWindowAnchor(meta.marker, anchor[0], anchor[1], meta.iconSize.getWidth(), meta.iconSize.getHeight());
            }
          }

          callback.onPostExecute(meta.marker);
        }
      }
    };

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AsyncLoadImage task = new AsyncLoadImage(activity, getCurrentUrl(), options, onComplete);
        task.execute();
        iconLoadingTasks.put(taskId, task);
      }
    });
  }


  private void _setIconAnchor(final Marker marker, double anchorX, double anchorY, final int imageWidth, final int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * density;
    anchorY = anchorY * density;
    marker.setAnchor((float) (anchorX / imageWidth), (float) (anchorY / imageHeight));
  }

  private void _setInfoWindowAnchor(final Marker marker, double anchorX, double anchorY, final int imageWidth, final int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * density;
    anchorY = anchorY * density;
    marker.setInfoWindowAnchor((float) (anchorX / imageWidth), (float) (anchorY / imageHeight));
  }

  protected Bitmap drawLabel(Bitmap image, Bundle labelOptions) {
    String text = labelOptions.getString("text");
    if (text == null || text.length() == 0) {
      return image;
    }
    Bitmap newIcon = Bitmap.createBitmap(image);
    Canvas canvas = new Canvas(newIcon);
    image.recycle();
    image = null;

    int fontSize = 10;
    if (labelOptions.containsKey("fontSize")) {
      fontSize = labelOptions.getInt("fontSize");
    }
    paint.setTextSize(fontSize * density);

    int color = Color.BLACK;
    if (labelOptions.containsKey("color")) {
      color = labelOptions.getInt("color");
    }
    boolean bold = false;
    if (labelOptions.containsKey("bold")) {
      bold = labelOptions.getBoolean("bold");
    }
    paint.setFakeBoldText(bold);
    boolean italic = false;
    if (labelOptions.containsKey("italic")) {
      italic = labelOptions.getBoolean("italic");
    }
    if (italic && bold) {
      paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
    } else if (italic) {
      paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
    } else if (bold) {
      paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    } else {
      paint.setTypeface(Typeface.DEFAULT);
    }
    paint.setColor(color);


    Rect rect = new Rect();
    canvas.getClipBounds(rect);
    int cHeight = rect.height();
    int cWidth = rect.width();
    paint.setTextAlign(Paint.Align.LEFT);
    paint.getTextBounds(text, 0, text.length(), rect);
    float x = cWidth / 2f - rect.width() / 2f - rect.left;
    float y = cHeight / 2f + rect.height() / 2f - rect.bottom;
    canvas.drawText(text, x, y, paint);
    return newIcon;
  }



}
