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
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class PluginMarker extends MyPlugin implements MyPluginInterface {

  private enum Animation {
    DROP,
    BOUNCE
  }

  protected HashMap<Integer, AsyncTask> iconLoadingTasks = new HashMap<Integer, AsyncTask>();
  protected HashMap<String, Bitmap> icons = new HashMap<String, Bitmap>();
  protected final HashMap<String, Integer> iconCacheKeys = new HashMap<String, Integer>();
  private static final Paint paint = new Paint();
  private boolean _clearDone = false;

  protected interface ICreateMarkerCallback {
    void onSuccess(Marker marker);

    void onError(String message);
  }

  @Override
  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    this.clear();
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Set<String> keySet = pluginMap.objects.keys;
        if (keySet.size() > 0) {
          String[] objectIdArray = keySet.toArray(new String[keySet.size()]);

          for (String objectId : objectIdArray) {
            if (pluginMap.objects.containsKey(objectId)) {
              if (objectId.startsWith("marker_") &&
                !objectId.startsWith("marker_property_") &&
                !objectId.startsWith("marker_imageSize_") &&
                !objectId.startsWith("marker_icon_")) {
                Marker marker = (Marker) pluginMap.objects.remove(objectId);
                _removeMarker(marker);
                marker = null;
              } else {
                Object object = pluginMap.objects.remove(objectId);
                object = null;
              }
            }
          }
        }

        pluginMap.objects.clear();
      }
    });

  }

  @Override
  protected void clear() {

    Semaphore semaphore = new Semaphore(1);

    _clearDone = false;

    cordova.getThreadPool().submit(new Runnable() {
      @Override
      public void run() {
        //--------------------------------------
        // Cancel tasks
        //--------------------------------------
        AsyncTask task;
        if (iconLoadingTasks != null && iconLoadingTasks.size() > 0) {
          int i, ilen = iconLoadingTasks.size();
          for (i = 0; i < ilen; i++) {
            task = iconLoadingTasks.get(i);
            task.cancel(true);
          }
        }
      }
    });


    //--------------------------------------
    // Recycle bitmaps as much as possible
    //--------------------------------------
    if (iconCacheKeys != null) {
      if (iconCacheKeys.size() > 0) {
        String[] cacheKeys = iconCacheKeys.keySet().toArray(new String[iconCacheKeys.size()]);
        for (int i = 0; i < cacheKeys.length; i++) {
          AsyncLoadImage.removeBitmapFromMemCahce(cacheKeys[i]);
          iconCacheKeys.remove(cacheKeys[i]);
        }
        cacheKeys = null;
      }
    }
    if (icons != null && icons.size() > 0) {
      String[] keys = icons.keySet().toArray(new String[icons.size()]);
      //Bitmap[] cachedBitmaps = icons.toArray(new Bitmap[icons.size()]);
      Bitmap image;
      for (int i = 0; i < keys.length; i++) {
        image = icons.remove(keys[i]);
        if (image != null && !image.isRecycled()) {
          image.recycle();
        }
        image = null;
      }
      icons.clear();
    }

    try {

      semaphore.acquire();
      //--------------------------------------
      // clean up properties as much as possible
      //--------------------------------------
      cordova.getActivity().runOnUiThread(() -> {
        Set<String> keySet = pluginMap.objects.keys;
        if (keySet.size() > 0) {
          String[] objectIdArray = keySet.toArray(new String[keySet.size()]);

          for (String objectId : objectIdArray) {
            if (pluginMap.objects.containsKey(objectId)) {
              if (objectId.startsWith("marker_") &&
                !objectId.startsWith("marker_property_") &&
                !objectId.startsWith("marker_imageSize") &&
                !objectId.startsWith("marker_icon_")) {
                Marker marker = (Marker) pluginMap.objects.remove(objectId);
                marker.setTag(null);
                marker.remove();
                marker = null;
              } else {
                Object object = pluginMap.objects.remove(objectId);
                object = null;
              }
            }
          }
        }

        _clearDone = true;
        semaphore.release();
      });

      if (!_clearDone) {
        semaphore.acquire();
      }
    } catch (InterruptedException ignore) {

    }
  }

  /**
   * Create a marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  @SuppressWarnings("unused")
  public void create(final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    // Create an instance of Marker class

    JSONObject opts = args.getJSONObject(1);
    final String markerId = "marker_" + args.getString(2);
    final JSONObject result = new JSONObject();
    result.put("__pgmId", markerId);

    _create(markerId, opts, new ICreateMarkerCallback() {
      @Override
      public void onSuccess(Marker marker) {
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
        callbackContext.error(message);
      }
    });
  }

  protected void _create(final String markerId, final JSONObject opts, final ICreateMarkerCallback callback) throws JSONException {
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

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Marker marker = map.addMarker(markerOptions);
        marker.setTag(markerId);
        marker.hideInfoWindow();

        cordova.getThreadPool().execute(new Runnable() {
          @Override
          public void run() {

            try {
              // Store the marker
              synchronized (pluginMap.objects) {
                pluginMap.objects.put(markerId, marker);

                pluginMap.objects.put("marker_property_" + markerId, properties);
              }

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
                PluginMarker.this.setIcon_(marker, bundle, new PluginAsyncInterface() {

                  @Override
                  public void onPostExecute(final Object object) {
                    cordova.getActivity().runOnUiThread(new Runnable() {
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
                          PluginMarker.this.setMarkerAnimation_(marker, markerAnimation, new PluginAsyncInterface() {

                            @Override
                            public void onPostExecute(Object object) {
                              Marker marker = (Marker) object;
                              callback.onSuccess(marker);
                            }

                            @Override
                            public void onError(String errorMsg) {
                              callback.onError(errorMsg);
                            }
                          });
                        } else {
                          callback.onSuccess(marker);
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
                  PluginMarker.this.setMarkerAnimation_(marker, markerAnimation, new PluginAsyncInterface() {

                    @Override
                    public void onPostExecute(Object object) {
                      callback.onSuccess(marker);
                    }

                    @Override
                    public void onError(String errorMsg) {
                      callback.onError(errorMsg);
                    }

                  });
                } else {
                  // Return the result if does not specify the icon property.
                  callback.onSuccess(marker);
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

  private void setDropAnimation_(final Marker marker, final PluginAsyncInterface callback) {
    final long startTime = SystemClock.uptimeMillis();
    final long duration = 100;

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final Handler handler = new Handler();
        final Projection proj = map.getProjection();
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
  private void setBounceAnimation_(final Marker marker, final PluginAsyncInterface callback) {
    final long startTime = SystemClock.uptimeMillis();
    final long duration = 2000;
    final Interpolator interpolator = new BounceInterpolator();

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        final Handler handler = new Handler();
        final Projection projection = map.getProjection();
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

  private void setMarkerAnimation_(Marker marker, String animationType, PluginAsyncInterface callback) {
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
        this.setDropAnimation_(marker, callback);
        break;

      case BOUNCE:
        this.setBounceAnimation_(marker, callback);
        break;

      default:
        break;
    }
  }

  /**
   * http://android-er.blogspot.com/2013/01/implement-bouncing-marker-for-google.html
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setAnimation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String markerId = args.getString(0);
    String animation = args.getString(1);
    final Marker marker = this.getMarker(markerId);
    Log.d(TAG, "--->setAnimation: markerId = " + markerId + ", animation = " + animation);
    if (marker == null) {
      callbackContext.error("marker is null");
      return;
    }

    this.setMarkerAnimation_(marker, animation, new PluginAsyncInterface() {

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
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void showInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = getMarker(id);
        if (marker != null) {
          marker.showInfoWindow();
        }
        callbackContext.success();
      }
    });
  }

  /**
   * Set rotation for the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setRotation(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float rotation = (float) args.getDouble(1);
    String id = args.getString(0);
    this.setFloat("setRotation", id, rotation, callbackContext);
  }

  /**
   * Set opacity for the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setOpacity(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float alpha = (float) args.getDouble(1);
    String id = args.getString(0);
    this.setFloat("setAlpha", id, alpha, callbackContext);
  }

  /**
   * Set zIndex for the marker (dummy code, not available on Android V2)
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setZIndex(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float zIndex = (float) args.getDouble(1);
    String id = args.getString(0);
    Marker marker = getMarker(id);
    this.setFloat("setZIndex", id, zIndex, callbackContext);
  }

  /**
   * set position
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setPosition(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    final LatLng position = new LatLng(args.getDouble(1), args.getDouble(2));
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = getMarker(id);
        if (marker != null) {
          marker.setPosition(position);
        }
        callbackContext.success();
      }
    });
  }

  /**
   * Set flat for the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setFlat(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    boolean isFlat = args.getBoolean(1);
    String id = args.getString(0);
    this.setBoolean("setFlat", id, isFlat, callbackContext);
  }

  /**
   * Set visibility for the object
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setVisible(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean isVisible = args.getBoolean(1);
    String id = args.getString(0);

    Marker marker = this.getMarker(id);
    if (marker == null) {
      callbackContext.success();
      return;
    }
    String propertyId = "marker_property_" + id;
    JSONObject properties = null;
    if (self.pluginMap.objects.containsKey(propertyId)) {
      properties = (JSONObject) self.pluginMap.objects.get(propertyId);
    } else {
      properties = new JSONObject();
    }
    properties.put("isVisible", isVisible);
    self.pluginMap.objects.put(propertyId, properties);

    this.setBoolean("setVisible", id, isVisible, callbackContext);
  }

  /**
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setDisableAutoPan(JSONArray args, CallbackContext callbackContext) throws JSONException {
    boolean disableAutoPan = args.getBoolean(1);
    String id = args.getString(0);
    Marker marker = this.getMarker(id);
    if (marker == null) {
      callbackContext.success();
      return;
    }
    String propertyId = "marker_property_" + id;
    JSONObject properties = null;
    if (self.pluginMap.objects.containsKey(propertyId)) {
      properties = (JSONObject) self.pluginMap.objects.get(propertyId);
    } else {
      properties = new JSONObject();
    }
    properties.put("disableAutoPan", disableAutoPan);
    self.pluginMap.objects.put(propertyId, properties);
    callbackContext.success();
  }

  /**
   * Set title for the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setTitle(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String title = args.getString(1);
    String id = args.getString(0);
    this.setString("setTitle", id, title, callbackContext);
  }

  /**
   * Set the snippet for the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setSnippet(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String snippet = args.getString(1);
    String id = args.getString(0);
    this.setString("setSnippet", id, snippet, callbackContext);
  }

  /**
   * Hide the InfoWindow binded with the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void hideInfoWindow(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Marker marker = getMarker(id);
        if (marker != null) {
          marker.hideInfoWindow();
        }
        callbackContext.success();
      }
    });
  }

  /**
   * Remove the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void remove(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    final String id = args.getString(0);
    final Marker marker = this.getMarker(id);
    if (marker == null) {
      callbackContext.success();
      return;
    }

    /*
    String[] cacheKeys = iconCacheKeys.toArray(new String[iconCacheKeys.size()]);
    for (int i = 0; i < cacheKeys.length; i++) {
      AsyncLoadImage.removeBitmapFromMemCahce(cacheKeys[i]);
    }
    */

    String propertyId = "marker_property_" + id;
    pluginMap.objects.remove(propertyId);

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {

        pluginMap.objects.remove(id);
        _removeMarker(marker);

        callbackContext.success();
      }
    });
  }

  protected void _removeMarker(Marker marker) {
    if (marker == null || marker.getTag() == null) {
      return;
    }
    //---------------------------------------------
    // Removes marker safely
    // (prevent the `un-managed object exception`)
    //---------------------------------------------
    String iconCacheKey = "marker_icon_" + marker.getTag();
    marker.setTag(null);
    marker.remove();

    //---------------------------------------------------------------------------------
    // If no marker uses the icon image used be specified this marker, release it
    //---------------------------------------------------------------------------------
    if (pluginMap.objects.containsKey(iconCacheKey)) {
      String cacheKey = (String) pluginMap.objects.remove(iconCacheKey);
      if (iconCacheKeys.containsKey(cacheKey)) {
        int count = iconCacheKeys.get(cacheKey);
        count--;
        if (count < 1) {
          AsyncLoadImage.removeBitmapFromMemCahce(cacheKey);
          iconCacheKeys.remove(cacheKey);
        } else {
          iconCacheKeys.put(cacheKey, count);
        }
      }
      pluginMap.objects.remove(iconCacheKey);
    }
  }

  /**
   * Set anchor for the icon of the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setIconAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float anchorX = (float) args.getDouble(1);
    float anchorY = (float) args.getDouble(2);
    String id = args.getString(0);
    Marker marker = this.getMarker(id);

    Bundle imageSize = (Bundle) self.pluginMap.objects.get("marker_imageSize_" + id);
    if (imageSize != null) {
      this._setIconAnchor(marker, anchorX, anchorY, imageSize.getInt("width"), imageSize.getInt("height"));
    }
    callbackContext.success();
  }


  /**
   * Set anchor for the InfoWindow of the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setInfoWindowAnchor(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    float anchorX = (float) args.getDouble(1);
    float anchorY = (float) args.getDouble(2);
    String id = args.getString(0);
    Marker marker = this.getMarker(id);

    Bundle imageSize = (Bundle) self.pluginMap.objects.get("marker_imageSize_" + id);
    if (imageSize != null) {
      this._setInfoWindowAnchor(marker, anchorX, anchorY, imageSize.getInt("width"), imageSize.getInt("height"));
    }
    callbackContext.success();
  }

  /**
   * Set draggable for the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setDraggable(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Boolean draggable = args.getBoolean(1);
    String id = args.getString(0);
    this.setBoolean("setDraggable", id, draggable, callbackContext);
  }

  /**
   * Set icon of the marker
   *
   * @param args
   * @param callbackContext
   * @throws JSONException
   */
  public void setIcon(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    String id = args.getString(0);
    Marker marker = this.getMarker(id);
    Object value = args.get(1);
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
      this.setIcon_(marker, bundle, new PluginAsyncInterface() {

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

  protected void setIcon_(final Marker marker, final Bundle iconProperty, final PluginAsyncInterface callback) {
    boolean noCaching = false;
    if (iconProperty.containsKey("noCache")) {
      noCaching = iconProperty.getBoolean("noCache");
    }
    if (iconProperty.containsKey("iconHue")) {
      cordova.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          float hue = iconProperty.getFloat("iconHue");
          marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue));
        }
      });
      callback.onPostExecute(marker);
      return;
    }

    String iconUrl = iconProperty.getString("url");
    if (iconUrl == null) {
      callback.onPostExecute(marker);
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
        iconLoadingTasks.remove(taskId);

        if (result == null || result.image == null) {
          callback.onPostExecute(marker);
          return;
        }
        if (marker == null) {
          callback.onError("marker is removed");
          return;
        }
        if (result.image.isRecycled()) {
          //Maybe the task was canceled by map.clean()?
          callback.onError("Can not get image for marker. Maybe the task was canceled by map.clean()?");
          return;
        }

        synchronized (marker) {
          String markerTag = marker.getTag() + "";
          String markerIconTag = "marker_icon_" + markerTag;
          String markerImgSizeTag = "marker_imageSize_" + markerTag;

          String currentCacheKey = (String) pluginMap.objects.get(markerIconTag);
          if (result.cacheKey != null && result.cacheKey.equals(currentCacheKey)) {
            synchronized (iconCacheKeys) {
              if (iconCacheKeys.containsKey(currentCacheKey)) {
                int count = iconCacheKeys.get(currentCacheKey);
                count--;
                if (count < 1) {
                  AsyncLoadImage.removeBitmapFromMemCahce(currentCacheKey);
                  iconCacheKeys.remove(currentCacheKey);
                } else {
                  iconCacheKeys.put(currentCacheKey, count);
                }
              }
            }
          }

          if (icons.containsKey(markerIconTag)) {
            Bitmap icon = icons.remove(markerIconTag);
            if (icon != null && !icon.isRecycled()) {
              icon.recycle();
            }
            icon = null;
          }
          icons.put(markerTag, result.image);

          //-------------------------------------------------------
          // Counts up the markers that use the same icon image.
          //-------------------------------------------------------
          if (result.cacheHit) {
            if (marker == null || marker.getTag() == null) {
              callback.onPostExecute(marker);
              return;
            }
            String hitCountKey = markerIconTag;
            pluginMap.objects.put(hitCountKey, result.cacheKey);
            if (!iconCacheKeys.containsKey(result.cacheKey)) {
              iconCacheKeys.put(result.cacheKey, 1);
            } else {
              int count = iconCacheKeys.get(result.cacheKey);
              iconCacheKeys.put(result.cacheKey, count + 1);
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
          if (bitmapDescriptor == null || marker == null || marker.getTag() == null) {
            callback.onPostExecute(marker);
            return;
          }

          //------------------------
          // Sets image as icon
          //------------------------
          marker.setIcon(bitmapDescriptor);
          bitmapDescriptor = null;

          //---------------------------------------------
          // Save the information for the anchor property
          //---------------------------------------------
          Bundle imageSize = new Bundle();
          imageSize.putInt("width", result.image.getWidth());
          imageSize.putInt("height", result.image.getHeight());
          self.pluginMap.objects.remove(markerImgSizeTag);
          self.pluginMap.objects.put(markerImgSizeTag, imageSize);

          result.image.recycle();
          result.image = null;

          // The `anchor` of the `icon` property
          if (iconProperty.containsKey("anchor")) {
            double[] anchor = iconProperty.getDoubleArray("anchor");
            if (anchor != null && anchor.length == 2) {
              _setIconAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
            }
          }


          // The `anchor` property for the infoWindow
          if (iconProperty.containsKey("infoWindowAnchor")) {
            double[] anchor = iconProperty.getDoubleArray("infoWindowAnchor");
            if (anchor != null && anchor.length == 2) {
              _setInfoWindowAnchor(marker, anchor[0], anchor[1], imageSize.getInt("width"), imageSize.getInt("height"));
            }
          }

          callback.onPostExecute(marker);
        }
      }
    };

    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AsyncLoadImage task = new AsyncLoadImage(cordova, webView, options, onComplete);
        task.execute();
        iconLoadingTasks.put(taskId, task);
      }
    });
  }


  private void _setIconAnchor(final Marker marker, double anchorX, double anchorY, final int imageWidth, final int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * density;
    anchorY = anchorY * density;
    final double fAnchorX = anchorX;
    final double fAnchorY = anchorY;
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        marker.setAnchor((float) (fAnchorX / imageWidth), (float) (fAnchorY / imageHeight));
      }
    });
  }

  private void _setInfoWindowAnchor(final Marker marker, double anchorX, double anchorY, final int imageWidth, final int imageHeight) {
    // The `anchor` of the `icon` property
    anchorX = anchorX * density;
    anchorY = anchorY * density;
    final double fAnchorX = anchorX;
    final double fAnchorY = anchorY;
    cordova.getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        marker.setInfoWindowAnchor((float) (fAnchorX / imageWidth), (float) (fAnchorY / imageHeight));
      }
    });
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
